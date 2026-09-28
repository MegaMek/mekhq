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
package mekhq.campaign.unit;

import static mekhq.campaign.personnel.skills.SkillType.EXP_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests that pin down what one maintenance cycle from {@link Maintenance#doMaintenance} does today
 * to a real Locust LCT-1V: the unit's quality, damaged parts, and the tech's and AsTechs' time.
 *
 * <p>Maintenance checks roll through MegaMek's {@code Compute}, so each case fixes the roll with
 * {@link FixedDieRolls}: every die shows the same face, so a face of 1 is a roll of 2 and a face of 6 a roll of 12.
 * Known bugs are asserted as they behave today and name their audit finding; such a case changes when that finding
 * is fixed.</p>
 *
 * <p>The campaign runs on default options: a seven-day maintenance cycle with no active contract, so each daily call
 * adds a quarter day and the cycle falls due on the 28th call. The Locust starts at quality D and takes 180 minutes
 * to maintain; its tech, when it has one, starts the day with 480 minutes.</p>
 */
class MaintenanceCharacterizationTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_ASTECH_POOL_MINUTES = 2880;
    private static final int FULL_SHIFT_MINUTES = 480;
    private static final int LOCUST_MAINTENANCE_MINUTES = 180;
    private static final int DAILY_CALLS_BEFORE_THE_CYCLE_IS_DUE = 27;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private Person assignTech(int experienceLevel) {
        Person tech = scenario.withTech(experienceLevel);
        locust.setTech(tech);
        return tech;
    }

    /**
     * Runs the daily maintenance call until the cycle is one day short of due, then runs the call that performs the
     * maintenance check with every die showing the given face.
     */
    private void runOneMaintenanceCycle(int face) {
        for (int day = 0; day < DAILY_CALLS_BEFORE_THE_CYCLE_IS_DUE; day++) {
            Maintenance.doMaintenance(campaign, locust);
        }
        assertEquals(6.75, locust.getDaysSinceMaintenance(), "the cycle should not be due yet");

        FixedDieRolls.everyDieShows(face);
        Maintenance.doMaintenance(campaign, locust);
    }

    private int countPartsNeedingRepair() {
        int partsNeedingRepair = 0;
        for (Part part : locust.getParts()) {
            if (part.needsFixing()) {
                partsNeedingRepair++;
            }
        }
        return partsNeedingRepair;
    }

    private int asTechPoolMinutes() {
        return campaign.getPlayerForce().getHumanResources().getAsTechPoolMinutes();
    }

    @Test
    void goodRollRaisesTheQualityAndChargesTechAndAsTechTime() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        Person tech = assignTech(EXP_REGULAR);

        runOneMaintenanceCycle(6);

        assertEquals(PartQuality.QUALITY_E, locust.getQuality());
        assertEquals(0, countPartsNeedingRepair());
        assertEquals(FULL_SHIFT_MINUTES - LOCUST_MAINTENANCE_MINUTES, tech.getMinutesLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES - (LOCUST_MAINTENANCE_MINUTES * FULL_ASTECH_TEAM), asTechPoolMinutes());
        assertEquals(0.0, locust.getDaysSinceMaintenance());
    }

    @Test
    void rollThatJustMeetsTheTargetLeavesTheQualityAlone() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        assignTech(EXP_REGULAR);

        // A roll of 2 against a target of 2 on most parts: a margin of 0 neither improves nor harms them
        runOneMaintenanceCycle(1);

        assertEquals(PartQuality.QUALITY_D, locust.getQuality());
        assertEquals(0, countPartsNeedingRepair());
    }

    @Test
    void badRollByAShorthandedGreenTechLowersTheQualityAndDamagesParts() {
        assignTech(EXP_GREEN);

        // With no AsTechs the target rises by 4, so a roll of 2 misses by 6 or more: every part checked drops a
        // quality step and takes a point of damage
        runOneMaintenanceCycle(1);

        assertEquals(PartQuality.QUALITY_C, locust.getQuality());
        assertEquals(37, countPartsNeedingRepair());
    }

    @Test
    void unitWithNoTechIsCheckedAsUnmaintained() {
        runOneMaintenanceCycle(1);

        assertTrue(locust.getLastMaintenanceReport().contains("Nobody performing maintenance"));
        assertEquals(PartQuality.QUALITY_C, locust.getQuality());
        assertEquals(39, countPartsNeedingRepair());
        assertEquals(0.0, locust.getDaysSinceMaintenance());
    }

    @Test
    void techWithoutEnoughTimeStillMakesTheCheckWithTheirOwnSkill() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        Person tech = assignTech(EXP_REGULAR);
        tech.setMinutesLeft(100);

        runOneMaintenanceCycle(6);

        // Current behaviour, see REP-10: no time or AsTech time is spent, yet the check uses the tech's own skill
        // (with the no-AsTech penalty) instead of the unmaintained target; changes when that is fixed
        assertEquals(100, tech.getMinutesLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES, asTechPoolMinutes());
        assertFalse(locust.getLastMaintenanceReport().contains("Nobody"));
        assertEquals(PartQuality.QUALITY_E, locust.getQuality());
        assertEquals(0.0, locust.getDaysSinceMaintenance());
    }
}
