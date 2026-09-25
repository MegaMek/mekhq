/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission.scenarios.salvage;

import static java.lang.Math.max;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public record SalvageFormationData(Formation formation, FormationType formationType, @Nullable Person tech,
      double maximumCargoCapacity,
      double maximumTowCapacity, int salvageCapableUnits, boolean hasTug, boolean isSpaceScenario) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageFormationData";

    /**
     * A single line in a capacity tooltip.
     *
     * @param unitName the name of the unit
     * @param capacity the unit's capacity, in tons
     *
     * @author Illiani
     * @since 0.51.01
     */
    private record CapacityEntry(String unitName, double capacity) {}

    public static SalvageFormationData buildData(Campaign campaign, Formation formation, boolean isSpaceScenario) {
        FormationType formationType = formation.getFormationType();
        UUID techId = formation.getTechID();
        Person tech;
        if (techId == null || !formationType.isSalvage()) {tech = null;} else {tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);}
        if (tech != null && tech.isEngineer()) { // Engineers cannot salvage
            tech = null;
        }

        double maximumCargoCapacity = 0.0;
        double maximumTowCapacity = 0.0;
        int salvageCapableUnits = 0;
        boolean hasTug = false;

        for (Unit unit : getAvailableSalvageUnits(campaign, formation, isSpaceScenario)) {
            Entity entity = unit.getEntity();
            salvageCapableUnits++;

            double cargoCapacity = unit.getCargoCapacityForSalvage();
            maximumCargoCapacity = max(cargoCapacity, maximumCargoCapacity);

            if (isSpaceScenario) {
                maximumTowCapacity = max(entity.getWeight(), maximumTowCapacity);
                hasTug |= CamOpsSalvageUtilities.hasNavalTug(entity);
            } else {
                maximumTowCapacity = max(CamOpsSalvageUtilities.getTowCapacity(unit), maximumTowCapacity);
            }
        }

        return new SalvageFormationData(formation,
              formationType,
              tech,
              maximumCargoCapacity,
              maximumTowCapacity,
              salvageCapableUnits,
              hasTug,
              isSpaceScenario);
    }

    /**
     * Collects the units in a formation that are able to take part in salvage operations.
     *
     * @param campaign        the current campaign
     * @param formation       the formation to check
     * @param isSpaceScenario {@code true} if the salvage operation takes place in space
     *
     * @return the units available for salvage operations
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static List<Unit> getAvailableSalvageUnits(Campaign campaign, Formation formation,
          boolean isSpaceScenario) {
        AbstractSalvage salvageRules = campaign.getCampaignOptions().get(CampaignOption.SALVAGE_SYSTEM).getSalvage();
        List<Unit> availableUnits = new ArrayList<>();
        for (Unit unit : formation.getAllUnitsAsUnits(campaign.getPlayerForce().getHangar(), false)) {
            if (salvageRules.isAvailableForSalvage(unit, isSpaceScenario)) {
                availableUnits.add(unit);
            }
        }
        return availableUnits;
    }

    public String getTechTooltip(Campaign campaign, Person tech) {
        StringBuilder tooltip = new StringBuilder();

        if (tech == null) {
            String noTechLabel = getTextAt(RESOURCE_BUNDLE, "SalvageFormationData.noTech");
            tooltip.append(noTechLabel);
        } else {
            tooltip.append(tech.getFullTitle()).append("<br>");

            boolean isTechSecondary = CamOpsSalvageUtilities.isUseSecondaryTechSkill(tech);
            tooltip.append(tech.getSkillLevel(campaign, isTechSecondary, true)).append("<br>");

            String injuryLabelKey;
            int injuries;
            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                injuryLabelKey = "SalvageFormationData.injuries";
                injuries = tech.getTotalInjurySeverity();
            } else {
                injuryLabelKey = "SalvageFormationData.hits";
                injuries = tech.getHits();
            }
            String injuriesLabel = getFormattedTextAt(RESOURCE_BUNDLE, injuryLabelKey, injuries);
            tooltip.append(injuriesLabel);
        }

        return tooltip.toString();
    }

    public String getAllCrewTechTooltip(Campaign campaign, Formation formation) {
        LocalHangar hangar = campaign.getPlayerForce().getHangar();

        StringBuilder tooltip = new StringBuilder();
        for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
            for (Person crew : unit.getCrew()) {
                if (crew.isTechExpanded() && !crew.isEngineer()) {
                    if (!tooltip.isEmpty()) {
                        tooltip.append("<br><br>");
                    }
                    tooltip.append(getTechTooltip(campaign, crew));
                }
            }
        }

        return tooltip.toString();
    }

    public String getCargoCapacityTooltip(Campaign campaign) {
        List<CapacityEntry> capacityEntries = new ArrayList<>();
        for (Unit unit : getAvailableSalvageUnits(campaign, formation, isSpaceScenario)) {
            capacityEntries.add(new CapacityEntry(unit.getName(), unit.getCargoCapacityForSalvage()));
        }
        return getCapacityTooltip(capacityEntries);
    }

    public String getTowCapacityTooltip(Campaign campaign) {
        List<CapacityEntry> capacityEntries = new ArrayList<>();
        for (Unit unit : getAvailableSalvageUnits(campaign, formation, isSpaceScenario)) {
            double towCapacity = isSpaceScenario ?
                                       unit.getEntity().getWeight() :
                                       CamOpsSalvageUtilities.getTowCapacity(unit);
            capacityEntries.add(new CapacityEntry(unit.getName(), towCapacity));
        }
        return getCapacityTooltip(capacityEntries);
    }

    private static String getCapacityTooltip(List<CapacityEntry> capacityEntries) {
        capacityEntries.sort(Comparator.comparing(CapacityEntry::unitName, String.CASE_INSENSITIVE_ORDER));

        StringBuilder tooltip = new StringBuilder();
        for (CapacityEntry entry : capacityEntries) {
            if (entry.capacity() > 0.0) {
                tooltip.append(getFormattedTextAt(RESOURCE_BUNDLE, "SalvageFormationData.capacity",
                      entry.unitName(), entry.capacity())).append("<br>");
            }
        }
        return tooltip.toString();
    }

    public String getTugTooltip(Campaign campaign) {
        List<String> unitsWithTug = new ArrayList<>();
        for (Unit unit : getAvailableSalvageUnits(campaign, formation, isSpaceScenario)) {
            if (CamOpsSalvageUtilities.hasNavalTug(unit.getEntity())) {
                unitsWithTug.add(unit.getName());
            }
        }
        unitsWithTug.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder tooltip = new StringBuilder();
        for (String unitName : unitsWithTug) {
            tooltip.append(unitName).append(": ✓<br>");
        }

        if (tooltip.isEmpty()) {
            tooltip.append(getTextAt(RESOURCE_BUNDLE, "SalvageFormationData.noTug"));
        }
        return tooltip.toString();
    }
}
