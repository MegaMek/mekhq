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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A Technician Skill Check always fails on a 2 (Campaign Operations, Extra Time, p. 208).
 */
class TechnicianCheckTest {
    private static final int FULL_ASTECH_TEAM = 6;

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    @ParameterizedTest
    @ValueSource(ints = { -3, 0, 2 })
    void rollOfTwoFailsHoweverLowTheTarget(int targetNumber) {
        assertFalse(TechnicianCheck.isSuccess(2, new TargetRoll(targetNumber, "test")));
    }

    @Test
    void rollOfThreeStillBeatsALowTarget() {
        assertTrue(TechnicianCheck.isSuccess(3, new TargetRoll(-3, "test")));
    }

    @Test
    void rollBelowTheTargetFails() {
        assertFalse(TechnicianCheck.isSuccess(6, new TargetRoll(7, "test")));
    }

    @Test
    void rollMeetingTheTargetSucceeds() {
        assertTrue(TechnicianCheck.isSuccess(7, new TargetRoll(7, "test")));
    }

    @Test
    void automaticSuccessNeedsNoRoll() {
        assertTrue(TechnicianCheck.isSuccess(2, new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "reload")));
    }

    @Test
    void automaticFailureAndImpossibleNeverSucceed() {
        assertFalse(TechnicianCheck.isSuccess(12, new TargetRoll(TargetRoll.AUTOMATIC_FAIL, "test")));
        assertFalse(TechnicianCheck.isSuccess(12, new TargetRoll(TargetRoll.IMPOSSIBLE, "test")));
    }

    @Test
    void legendaryTechTakingExtraTimeFailsTheRepairOnATwo() {
        RepairOnALowTarget repair = new RepairOnALowTarget();

        FixedDieRolls.everyDieShows(1);
        repair.campaign().fixPart(repair.laser(), repair.tech());

        assertEquals(1, repair.laser().getHits());
    }

    @Test
    void legendaryTechTakingExtraTimeFixesThePartOnAFour() {
        RepairOnALowTarget repair = new RepairOnALowTarget();

        FixedDieRolls.everyDieShows(2);
        repair.campaign().fixPart(repair.laser(), repair.tech());

        assertEquals(0, repair.laser().getHits());
    }

    /**
     * A damaged Locust medium laser worked on by a Legendary tech at quadruple time, which brings the target number
     * to 2 or less, so only the always-failing 2 can fail it.
     */
    private record RepairOnALowTarget(Campaign campaign, EquipmentPart laser, Person tech) {
        RepairOnALowTarget() {
            this(PartsScenario.create());
        }

        private RepairOnALowTarget(PartsScenario scenario) {
            this(scenario.getCampaign(), damagedMediumLaser(scenario), legendaryTech(scenario));
            laser.setMode(WorkTime.EXTRA_4);
            int targetNumber = campaign.getTargetFor(laser, tech).getValue();
            assertTrue(targetNumber <= 2, "target number should be 2 or less, was " + targetNumber);
        }

        private static EquipmentPart damagedMediumLaser(PartsScenario scenario) {
            Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
            scenario.withAsTechs(FULL_ASTECH_TEAM);
            EquipmentPart laser = PartsScenario.unitParts(locust, EquipmentPart.class)
                                        .stream()
                                        .filter(equipmentPart -> "Medium Laser".equals(equipmentPart.getName()))
                                        .findFirst()
                                        .orElseThrow();
            laser.setHits(1);
            return laser;
        }

        private static Person legendaryTech(PartsScenario scenario) {
            return scenario.withTech(EXP_LEGENDARY);
        }
    }
}
