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
package mekhq.campaign;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests that pin down today's repair target numbers from {@link Campaign#getTargetFor} and the
 * maintenance target from {@link Maintenance#getTargetForMaintenance}, on a real Locust LCT-1V and real techs.
 *
 * <p>Each case records both the final number and the full modifier text, so a change to any single modifier shows
 * up here. These tests describe current behaviour, not the rules as written: a case that fails after a deliberate
 * rule change should be updated to the new number, not worked around.</p>
 *
 * <p>Unless a case says otherwise the campaign runs on default options (overtime off, destroy-by-margin off), the
 * Locust is quality D at a basic facility, and a full team of six AsTechs is available.</p>
 */
class RepairTargetCharacterizationTest {
    private static final int FULL_ASTECH_TEAM = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
    }

    /**
     * Renders a target as its value and its modifier text, so one assertion covers both.
     */
    private static String describe(TargetRoll target) {
        return target.getValueAsString() + " | " + target.getDesc();
    }

    private Part damagedPart(String partKind) {
        Part part = switch (partKind) {
            case "MEDIUM_LASER" -> mediumLaser();
            case "ACTUATOR" -> PartsScenario.unitParts(locust, MekActuator.class).getFirst();
            case "ENGINE" -> PartsScenario.unitParts(locust, EnginePart.class).getFirst();
            default -> throw new IllegalArgumentException("Unknown part kind " + partKind);
        };
        part.setHits(1);
        return part;
    }

    private EquipmentPart mediumLaser() {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no Medium Laser");
    }

    /**
     * Destroys the Locust's medium laser the way combat does, leaving its missing-part placeholder on the unit.
     */
    private MissingPart missingMediumLaser() {
        mediumLaser().remove(false);
        return PartsScenario.unitParts(locust, MissingPart.class).getFirst();
    }

    /** Experience levels: 1 Green, 2 Regular, 3 Veteran, 4 Elite. */
    @ParameterizedTest(name = "{0} at experience level {1}")
    @CsvSource(delimiter = ';', value = {
          "MEDIUM_LASER; 1; 5 | 8 (Green) - 3 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "MEDIUM_LASER; 2; 3 | 6 (Regular) - 3 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "MEDIUM_LASER; 3; 2 | 5 (Veteran) - 3 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "MEDIUM_LASER; 4; 1 | 4 (Elite) - 3 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ACTUATOR; 1; 8 | 8 (Green) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ACTUATOR; 2; 6 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ACTUATOR; 3; 5 | 5 (Veteran) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ACTUATOR; 4; 4 | 4 (Elite) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ENGINE; 1; 7 | 8 (Green) - 1 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ENGINE; 2; 5 | 6 (Regular) - 1 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ENGINE; 3; 4 | 5 (Veteran) - 1 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "ENGINE; 4; 3 | 4 (Elite) - 1 (difficulty) + 0 (Facility - Basic) + 0 (D)"
    })
    void targetBySkillLevel(String partKind, int experienceLevel, String expectedTarget) {
        Part part = damagedPart(partKind);
        Person tech = scenario.withTech(experienceLevel);

        assertEquals(expectedTarget, describe(campaign.getTargetFor(part, tech)));
    }

    /**
     * Under the default rules a rush job shows no modifier of its own: it drops the tech one experience level, and
     * dropping to Green costs one point more. A Regular tech on a quarter-time rush drops below Green, which the
     * part's minimum skill (Green) refuses, so the job is impossible.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = ';', value = {
          "NORMAL; 6 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "EXTRA_2; 5 | 6 (Regular) + 0 (difficulty) - 1 (Extra time (x2)) + 0 (Facility - Basic) + 0 (D)",
          "EXTRA_4; 3 | 6 (Regular) + 0 (difficulty) - 3 (Extra time (x4)) + 0 (Facility - Basic) + 0 (D)",
          "RUSH_2; 8 | 8 (Green) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
          "RUSH_4; Impossible | Task is beyond this tech's skill level"
    })
    void targetByWorkMode(WorkTime workMode, String expectedTarget) {
        Part actuator = damagedPart("ACTUATOR");
        actuator.setMode(workMode);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals(expectedTarget, describe(campaign.getTargetFor(actuator, tech)));
    }

    /**
     * Under destroy-by-margin the rush modifier is added instead of the level drop, but the target still labels the
     * Regular tech's skill as Green.
     */
    @Test
    void rushJobUnderDestroyByMarginKeepsTheSkillLevelAndAddsTheRushModifier() {
        campaign.getCampaignOptions().set(CampaignOption.DESTROY_BY_MARGIN, true);
        Part actuator = damagedPart("ACTUATOR");
        actuator.setMode(WorkTime.RUSH_2);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("7 | 6 (Green) + 0 (difficulty) + 1 (Rush Job (1/2)) + 0 (Facility - Basic) + 0 (D)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void jobRunningIntoOvertimeAddsOvertimeModifierWhenOvertimeIsAllowed() {
        campaign.setOvertime(true);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);
        tech.setMinutesLeft(60);

        assertEquals("9 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D) + 3 (overtime)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void jobThatFitsTheShiftHasNoOvertimeModifierEvenWhenOvertimeIsAllowed() {
        campaign.setOvertime(true);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("6 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void jobLongerThanTheShiftWithOvertimeOffHasNoOvertimeModifier() {
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);
        tech.setMinutesLeft(60);

        assertEquals("6 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void twoAsTechsAreShorthanded() {
        campaign.getPlayerForce().getHumanResources().decreaseAsTechPool(campaign, FULL_ASTECH_TEAM - 2);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("8 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D) + 2 (shorthanded)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void noAsTechsAtAllIsTheWorstShorthandedModifier() {
        campaign.getPlayerForce().getHumanResources().emptyAsTechPool(campaign);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("10 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D) + 4 (shorthanded)",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @ParameterizedTest(name = "unit quality {0}")
    @CsvSource(delimiter = ';', value = {
          "QUALITY_A; 9 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 3 (A)",
          "QUALITY_F; 4 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) - 2 (F)"
    })
    void targetByUnitQuality(PartQuality quality, String expectedTarget) {
        locust.setQuality(quality);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals(expectedTarget, describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void replacingAMissingPartFromASpare() {
        Part spareLaser = mediumLaser().clone();
        MissingPart placeholder = missingMediumLaser();
        scenario.withSpare(spareLaser, 1);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("6 | 6 (Regular) + 0 (difficulty) + 0 (Facility - Basic) + 0 (D)",
              describe(campaign.getTargetFor(placeholder, tech)));
    }

    @Test
    void replacingAMissingPartWithNoSpareIsImpossible() {
        MissingPart placeholder = missingMediumLaser();
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("Impossible | Replacement part not available.",
              describe(campaign.getTargetFor(placeholder, tech)));
    }

    @Test
    void armorWithNoSpareArmorIsImpossible() {
        Armor armor = PartsScenario.unitParts(locust, Armor.class).getFirst();
        armor.setAmount(armor.getAmount() - 4);
        armor.setAmountNeeded(4);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("Impossible | No spare armor available", describe(campaign.getTargetFor(armor, tech)));
    }

    @Test
    void partAlreadyHeldByAnotherTechIsImpossible() {
        Part actuator = damagedPart("ACTUATOR");
        Person otherTech = scenario.withTech(EXP_REGULAR);
        actuator.setTech(otherTech);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("Impossible | Already being worked on by another team",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void techWithNoTimeLeftIsImpossible() {
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);
        tech.setMinutesLeft(0);

        assertEquals("Impossible | The tech has no time left.", describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void techWithoutAToolKitIsImpossibleWhenToolKitsAreRequired() {
        campaign.getCampaignOptions().set(CampaignOption.TECHS_NEED_TOOL_KIT, true);
        Part actuator = damagedPart("ACTUATOR");
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("Impossible | The tech has no tool kit", describe(campaign.getTargetFor(actuator, tech)));
    }

    @Test
    void partThatAlreadyDefeatedATechOfThisLevelIsImpossible() {
        Part actuator = damagedPart("ACTUATOR");
        actuator.setSkillMin(EXP_REGULAR + 1);
        Person tech = scenario.withTech(EXP_REGULAR);

        assertEquals("Impossible | Task is beyond this tech's skill level",
              describe(campaign.getTargetFor(actuator, tech)));
    }

    /**
     * A unit's default maintenance takes four times the base time, which shows as the extra-time bonus of 3.
     */
    @Test
    void maintenanceTargetWithAFullAsTechTeam() {
        Person tech = scenario.withTech(EXP_REGULAR);

        TargetRoll target = Maintenance.getTargetForMaintenance(campaign, mediumLaser(), tech, FULL_ASTECH_TEAM);

        assertEquals("2 | 6 (Regular) - 1 (maintenance) + 0 (tech rating C) + 0 (Facility - Basic)"
              + " + 0 (D) - 3 (extra time)",
              describe(target));
    }

    @Test
    void maintenanceTargetWithNoAsTechs() {
        Person tech = scenario.withTech(EXP_REGULAR);

        TargetRoll target = Maintenance.getTargetForMaintenance(campaign, mediumLaser(), tech, 0);

        assertEquals("6 | 6 (Regular) - 1 (maintenance) + 0 (tech rating C) + 0 (Facility - Basic) + 0 (D)"
              + " + 4 (shorthanded) - 3 (extra time)",
              describe(target));
    }

    @Test
    void maintenanceTargetWithNoTechIsTheUnmaintainedTarget() {
        TargetRoll target = Maintenance.getTargetForMaintenance(campaign, mediumLaser(), null, 0);

        assertEquals("9 | 10 (Unmaintained) - 1 (maintenance) + 0 (tech rating C) + 0 (Facility - Basic) + 0 (D)",
              describe(target));
    }
}
