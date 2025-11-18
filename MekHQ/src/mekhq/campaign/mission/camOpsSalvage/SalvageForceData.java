/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.camOpsSalvage;

import static java.lang.Math.max;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.ITransportAssignment;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;

public record SalvageForceData(Force force, ForceType forceType, @Nullable Person tech, double maximumCargoCapacity,
      double maximumTowCapacity, int salvageCapableUnits, boolean hasTug) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageForceData";

    public static SalvageForceData buildData(Campaign campaign, Force force, boolean isSpaceScenario) {
        ForceType forceType = force.getForceType();
        UUID techId = force.getTechID();
        Person tech = techId == null || !forceType.isSalvage() ? null : campaign.getPerson(techId);
        if (tech != null && tech.isEngineer()) { // Engineers cannot salvage
            tech = null;
        }

        double maximumCargoCapacity = 0.0;
        double maximumTowCapacity = 0.0;
        int salvageCapableUnits = 0;
        boolean hasTug = false;

        Hangar hangar = campaign.getHangar();
        for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
            if (!unit.isFullyCrewed()) {
                continue;
            }

            Entity entity = unit.getEntity();
            if (entity != null) {
                boolean canSalvage = isSpaceScenario ? entity.canPerformSpaceSalvageOperations() :
                                           entity.canPerformGroundSalvageOperations();
                if (!canSalvage) {
                    continue;
                }

                if (entity instanceof Tank tank && tank.isTrailer()) {
                    ITransportAssignment transportAssignment = unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT);
                    if (transportAssignment == null || !transportAssignment.hasTransport()) {
                        continue; // If nothing is towing the trailer, it can't reach the salvage operation
                    }
                }

                double cargoCapacity = unit.getCargoCapacityForSalvage();
                maximumCargoCapacity = max(cargoCapacity, maximumCargoCapacity);

                if (isSpaceScenario) {
                    boolean hasNavalTug = CamOpsSalvageUtilities.hasNavalTug(entity);
                    if (cargoCapacity > 0.0 || CamOpsSalvageUtilities.hasNavalTug(entity)) {
                        salvageCapableUnits++;
                    }

                    double towCapacity = entity.getWeight();
                    maximumTowCapacity = max(towCapacity, maximumTowCapacity);

                    hasTug = hasNavalTug;
                } else {
                    boolean isTowCapable = entity instanceof Mek || entity instanceof Tank;
                    if (cargoCapacity > 0.0) {
                        salvageCapableUnits++;
                    } else if (isTowCapable) {
                        salvageCapableUnits++;
                    }

                    if (isTowCapable) {
                        double currentTowWeight = unit.getTotalWeightOfUnitsAssignedToBeTransported(
                              CampaignTransportType.TOW_TRANSPORT,
                              TransporterType.TANK_TRAILER_HITCH);

                        double towCapacity = max(0.0, entity.getWeight() - currentTowWeight);
                        maximumTowCapacity = max(towCapacity, maximumTowCapacity);
                    }
                }
            }
        }

        return new SalvageForceData(force,
              forceType,
              tech,
              maximumCargoCapacity,
              maximumTowCapacity,
              salvageCapableUnits,
              hasTug);
    }

    public String getTechTooltip(Campaign campaign, Person tech) {
        StringBuilder tooltip = new StringBuilder();

        if (tech == null) {
            String noTechLabel = getTextAt(RESOURCE_BUNDLE, "SalvageForceData.noTech");
            tooltip.append(noTechLabel);
        } else {
            tooltip.append(tech.getFullTitle()).append("<br>");

            boolean isTechSecondary = tech.getSecondaryRole().isTechSecondary();
            tooltip.append(tech.getSkillLevel(campaign, isTechSecondary, true)).append("<br>");

            String injuryLabelKey;
            int injuries;
            if (campaign.getCampaignOptions().isUseAdvancedMedical()) {
                injuryLabelKey = "SalvageForceData.injuries";
                injuries = tech.getInjuries().size();
            } else {
                injuryLabelKey = "SalvageForceData.hits";
                injuries = tech.getHits();
            }
            String injuriesLabel = getFormattedTextAt(RESOURCE_BUNDLE, injuryLabelKey, injuries);
            tooltip.append(injuriesLabel);
        }

        return tooltip.toString();
    }

    public String getAllCrewTechTooltip(Campaign campaign, Force force) {
        Hangar hangar = campaign.getHangar();

        StringBuilder tooltip = new StringBuilder();
        for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
            for (Person crew : unit.getCrew()) {
                if (crew.isTechExpanded() && !crew.isEngineer()) {
                    tooltip.append(getTechTooltip(campaign, crew));
                }
            }
        }

        return tooltip.toString();
    }

    public String getCargoCapacityTooltip(Hangar hangar) {
        LinkedHashMap<String, Double> capacityMap = getMap(hangar, false);
        StringBuilder tooltip = getTooltip(capacityMap);
        return tooltip.toString();
    }

    public String getTowCapacityTooltip(Hangar hangar) {
        LinkedHashMap<String, Double> capacityMap = getMap(hangar, true);
        StringBuilder tooltip = getTooltip(capacityMap);
        return tooltip.toString();
    }

    private LinkedHashMap<String, Double> getMap(Hangar hangar, boolean isTow) {
        Map<String, Double> unsortedMap = getUnsortedMap(hangar, isTow);
        return getSortedMap(unsortedMap);
    }

    private Map<String, Double> getUnsortedMap(Hangar hangar, boolean isTow) {
        Map<String, Double> capacityMap = new HashMap<>();
        for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
            Entity entity = unit.getEntity();
            if (entity == null) {
                continue;
            }

            String unitName = unit.getName();
            double weight = isTow ? entity.getWeight() : unit.getCargoCapacity();
            capacityMap.put(unitName, weight);
        }
        return capacityMap;
    }

    private static LinkedHashMap<String, Double> getSortedMap(Map<String, Double> capacityMap) {
        return capacityMap.entrySet()
                     .stream()
                     .sorted(Map.Entry.comparingByKey())
                     .collect(Collectors.toMap(
                           Map.Entry::getKey,
                           Map.Entry::getValue,
                           (e1, e2) -> e1,
                           LinkedHashMap::new
                     ));
    }

    private static StringBuilder getTooltip(LinkedHashMap<String, Double> capacityMap) {
        StringBuilder tooltip = new StringBuilder();
        for (Map.Entry<String, Double> entry : capacityMap.entrySet()) {
            double capacity = entry.getValue();
            if (capacity > 0.0) {
                tooltip.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
            }
        }
        return tooltip;
    }

    public String getTugTooltip(Hangar hangar) {
        Map<String, Boolean> capacityMap = new HashMap<>();
        for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
            Entity entity = unit.getEntity();
            if (entity == null) {
                continue;
            }

            String unitName = unit.getName();
            boolean hasTug = CamOpsSalvageUtilities.hasNavalTug(entity);
            capacityMap.put(unitName, hasTug);
        }

        LinkedHashMap<String, Boolean> sortedMap = capacityMap.entrySet()
                                                         .stream()
                                                         .sorted(Map.Entry.comparingByKey())
                                                         .collect(Collectors.toMap(
                                                               Map.Entry::getKey,
                                                               Map.Entry::getValue,
                                                               (e1, e2) -> e1,
                                                               LinkedHashMap::new
                                                         ));

        StringBuilder tooltip = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : sortedMap.entrySet()) {
            if (entry.getValue()) {
                tooltip.append(entry.getKey()).append(": \u2713<br>");
            }
        }

        if (tooltip.isEmpty()) {
            tooltip.append(getTextAt(RESOURCE_BUNDLE, "SalvageForceData.noTug"));
        }
        return tooltip.toString();
    }
}
