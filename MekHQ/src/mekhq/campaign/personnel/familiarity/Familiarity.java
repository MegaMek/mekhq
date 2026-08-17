/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.familiarity;

import static mekhq.campaign.personnel.PersonnelOptions.FAMILIARITY_EMOTIONALLY_UNAVAILABLE;
import static mekhq.campaign.personnel.PersonnelOptions.FAMILIARITY_IRON_BOND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.unit.Unit;

public enum Familiarity {
    DISABLED("DISABLED",
          0,
          0,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0)),
    NORMAL("NORMAL",
          200,
          100,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(1, 0),
          new FamiliarityLevel(1, 1),
          new FamiliarityLevel(1, 1)),
    HARD("HARD",
          300,
          100,
          new FamiliarityLevel(-1, -1),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(1, 0),
          new FamiliarityLevel(1, 1)),
    ROLEPLAY("ROLEPLAY",
          300,
          100,
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0),
          new FamiliarityLevel(0, 0));

    private final static String RESOURCE_BUNDLE = "mekhq.resources.FamiliarityMode";

    private final static int FAMILIARITY_ZERO = 0;
    private final static int FAMILIARITY_ONE_HUNDRED = 100;
    private final static int FAMILIARITY_TWO_HUNDRED = 200;
    public final static int FAMILIARITY_THREE_HUNDRED = 300;

    private final String lookUpName;
    private final int familiarityCap;
    private final int trainingCap;
    private final String label;
    private final String tooltip;
    private final FamiliarityLevel zero;
    private final FamiliarityLevel oneHundred;
    private final FamiliarityLevel twoHundred;
    private final FamiliarityLevel threeHundred;

    Familiarity(final String lookUpName, final int familiarityCap, final int trainingCap, final FamiliarityLevel zero,
          final FamiliarityLevel oneHundred, final FamiliarityLevel twoHundred, final FamiliarityLevel threeHundred) {
        this.lookUpName = lookUpName;
        this.familiarityCap = familiarityCap;
        this.trainingCap = trainingCap;
        this.label = getLabel(lookUpName);
        this.tooltip = getTooltip(lookUpName);
        this.zero = zero;
        this.oneHundred = oneHundred;
        this.twoHundred = twoHundred;
        this.threeHundred = threeHundred;
    }

    private static String getLabel(String lookUpName) {
        return getTextAt(RESOURCE_BUNDLE, "FamiliarityMode." + lookUpName + ".label");
    }

    private static String getTooltip(String lookUpName) {
        return getTextAt(RESOURCE_BUNDLE, "FamiliarityMode." + lookUpName + ".tooltip");
    }

    public boolean isEnabled() {
        return this != DISABLED;
    }

    public int getFamiliarityCap() {
        return familiarityCap;
    }

    public int getTrainingCap() {
        return trainingCap;
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getPilotingMaintenanceBonus(final int familiarity) {
        int level = getFamiliarityLevel(familiarity);

        return switch (level) {
            case FAMILIARITY_ZERO -> zero.pilotingMaintenance();
            case FAMILIARITY_ONE_HUNDRED -> oneHundred.pilotingMaintenance();
            case FAMILIARITY_TWO_HUNDRED -> twoHundred.pilotingMaintenance();
            case FAMILIARITY_THREE_HUNDRED -> threeHundred.pilotingMaintenance();
            default -> throw new IllegalStateException("Unexpected value: " + level);
        };
    }

    public int getGunneryRepairBonus(final int familiarity) {
        int level = getFamiliarityLevel(familiarity);

        return switch (level) {
            case FAMILIARITY_ZERO -> zero.gunneryRepairs();
            case FAMILIARITY_ONE_HUNDRED -> oneHundred.gunneryRepairs();
            case FAMILIARITY_TWO_HUNDRED -> twoHundred.gunneryRepairs();
            case FAMILIARITY_THREE_HUNDRED -> threeHundred.gunneryRepairs();
            default -> throw new IllegalStateException("Unexpected value: " + level);
        };
    }

    private static int getFamiliarityLevel(int familiarity) {
        if (familiarity < FAMILIARITY_ONE_HUNDRED) {
            return FAMILIARITY_ZERO;
        }

        if (familiarity < FAMILIARITY_TWO_HUNDRED) {
            return FAMILIARITY_ONE_HUNDRED;
        }

        if (familiarity < FAMILIARITY_THREE_HUNDRED) {
            return FAMILIARITY_TWO_HUNDRED;
        }

        return FAMILIARITY_THREE_HUNDRED;
    }

    @Override
    public String toString() {
        return label;
    }

    public static void assignFamiliarityToCombatTeam(Campaign campaign, CombatTeam combatTeam,
          FamiliarityGainType gainType) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Familiarity mode = campaignOptions.get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        if (!mode.isEnabled()) {
            return;
        }

        Formation formation = combatTeam.getFormation(campaign);
        if (formation == null) {
            return;
        }

        int familiaritySpeed = campaignOptions.get(CampaignOption.CHASSIS_FAMILIARITY_SPEED);
        int familiarityCap = mode.getFamiliarityCap();

        LocalHangar hangar = campaign.getPlayerForce().getHangar();
        List<Unit> units = formation.getUnitsAsUnits(hangar);
        for (Unit unit : units) {
            assignFamiliarity(campaign, unit, familiarityCap, familiaritySpeed, gainType);
        }
    }

    public static void assignFamiliarity(Campaign campaign, Unit unit, int cap, int speed,
          FamiliarityGainType gainType) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Familiarity mode = campaignOptions.get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        if (!mode.isEnabled()) {
            return;
        }

        Entity unitEntity = unit.getEntity();
        if (unitEntity == null || !unitEntity.isChassisFamiliarityEligible()) {
            return;
        }

        int familiarityGain = gainType.rollFamiliarity(speed);

        String chassis = unitEntity.getChassis();
        for (Person crew : unit.getCrew()) {
            addFamiliarity(campaign, crew, chassis, cap, familiarityGain);
        }

        Person unitTech = unit.getTech();
        if (unitTech != null) {
            gateMultipleTechAssignments(campaign, unitTech, chassis, cap, familiarityGain);
        }
    }

    /**
     * Awards familiarity with the chassis of {@code chassisSource} to one named person, and to nobody else.
     *
     * <p>Unlike {@link #assignFamiliarity(Campaign, Unit, int, int, FamiliarityGainType)}, this does not spread the
     * grant across the unit's crew or its assigned tech, and the person need not be aboard the unit at all. It is the
     * right entry point wherever the caller has already decided <i>which</i> character is being rewarded - for example
     * training, where the trainee earns familiarity with their own chassis and, separately, with the educator's
     * chassis.</p>
     *
     * <p>Does nothing if familiarity is disabled, if either argument is {@code null}, or if the unit's entity is not
     * {@link Entity#isChassisFamiliarityEligible() eligible} for familiarity.</p>
     *
     * @param campaign      the current campaign
     * @param person        the single character to award
     * @param chassisSource the unit whose chassis the familiarity is earned with
     * @param cap           the maximum familiarity this grant may raise the character to
     * @param speed         the base familiarity gain, before {@code gainType} and per-character trait modifiers
     * @param gainType      how {@code speed} is converted into the awarded amount
     */
    public static void assignFamiliarityToPerson(Campaign campaign, @Nullable Person person,
          @Nullable Unit chassisSource, int cap, int speed, FamiliarityGainType gainType) {
        Familiarity mode = campaign.getCampaignOptions().get(CampaignOption.CHASSIS_FAMILIARITY_MODE);
        if (!mode.isEnabled() || (person == null) || (chassisSource == null)) {
            return;
        }

        Entity unitEntity = chassisSource.getEntity();
        if (unitEntity == null || !unitEntity.isChassisFamiliarityEligible()) {
            return;
        }

        addFamiliarity(campaign, person, unitEntity.getChassis(), cap, gainType.rollFamiliarity(speed));
    }

    private static void gateMultipleTechAssignments(Campaign campaign, Person unitTech, String chassis, int cap,
          int familiarityGain) {
        boolean singleUnitAssignment = unitTech.getTechUnits().size() == 1;
        if (singleUnitAssignment) {
            addFamiliarity(campaign, unitTech, chassis, cap, familiarityGain);
        }
    }

    private static void addFamiliarity(Campaign campaign, Person crew, String chassis, int cap, int familiarityGain) {
        PersonnelOptions options = crew.getOptions();
        boolean hasIronBond = options.booleanOption(FAMILIARITY_IRON_BOND);
        boolean isEmotionallyUnavailable = options.booleanOption(FAMILIARITY_EMOTIONALLY_UNAVAILABLE);

        int currentFamiliarity = crew.getChassisFamiliarity(chassis);

        double multiplier = 1.0;
        if (hasIronBond) {
            multiplier = 1.5;
        }

        if (isEmotionallyUnavailable && currentFamiliarity == 0) {
            multiplier = 0.0;
        }

        familiarityGain = (int) Math.round(familiarityGain * multiplier);

        boolean alreadyCapped = currentFamiliarity == cap;
        crew.addChassisFamiliarity(chassis, familiarityGain, cap);
        boolean nowCapped = crew.getChassisFamiliarity(chassis) == cap;

        if (!alreadyCapped && nowCapped) {
            String report = getFormattedTextAt(RESOURCE_BUNDLE, "ResolveScenarioTracker.cappedFamiliarity",
                  crew.getHyperlinkedFullTitle(), spanOpeningWithCustomColor(getAmazingColor()),
                  CLOSING_SPAN_TAG,
                  chassis);
            campaign.addReport(DailyReportType.PERSONNEL, report);
        }
    }
}
