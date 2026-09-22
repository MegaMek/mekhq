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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.pointOfInterest.PointOfInterestDeploymentOutcome;
import org.junit.jupiter.api.Test;

/**
 * Tests for the contract-level rules points of interest feed into: whether a deployment rolls a random scenario, and
 * how a per-month schedule is spread across the calendar.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestContractRulesTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);

    // Odds that always, or never, succeed against randomInt(100), which returns 0 to 99
    private static final int CERTAIN_ODDS = 100;
    private static final int IMPOSSIBLE_ODDS = -1;

    // Random scenario roll

    @Test
    void withoutAPointOfInterestTheUsualRollDecides() {
        assertTrue(StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              false,
              false,
              false,
              CERTAIN_ODDS));
        assertFalse(StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.NO_EFFECT,
              false,
              false,
              false,
              IMPOSSIBLE_ODDS));
    }

    @Test
    void suppressingRulesOutEvenACertainRoll() {
        assertFalse(StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              false,
              false,
              false,
              CERTAIN_ODDS));
    }

    @Test
    void forcingMakesEvenAnImpossibleRollSucceed() {
        assertTrue(StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.FORCE_SCENARIO,
              false,
              false,
              false,
              IMPOSSIBLE_ODDS));
    }

    @Test
    void forcingNeverSpawnsAgainstARoutedEnemy() {
        assertFalse(StratConRulesManager.rollsRandomScenario(PointOfInterestDeploymentOutcome.FORCE_SCENARIO,
              false,
              false,
              true,
              IMPOSSIBLE_ODDS));
    }

    @Test
    void noRandomScenarioUnderEssentialScenariosOnlyOrOnAFacility() {
        for (PointOfInterestDeploymentOutcome outcome : PointOfInterestDeploymentOutcome.values()) {
            assertFalse(StratConRulesManager.rollsRandomScenario(outcome, true, false, false, CERTAIN_ODDS),
                  "Essential Scenarios Only: " + outcome);
            assertFalse(StratConRulesManager.rollsRandomScenario(outcome, false, true, false, CERTAIN_ODDS),
                  "facility hex: " + outcome);
        }
    }

    // Spreading a schedule across the calendar

    @Test
    void eachScheduledItemGetsADayWithinItsMonth() {
        List<Integer> schedule = List.of(2, 0, 3);

        List<LocalDate> spawnDates = StratConContractInitializer.rollSpawnDates(TODAY, schedule, 3);

        assertEquals(5, spawnDates.size());
        for (int index = 0; index < 2; index++) {
            assertWithin(spawnDates.get(index), TODAY, TODAY.plusMonths(1));
        }
        for (int index = 2; index < 5; index++) {
            assertWithin(spawnDates.get(index), TODAY.plusMonths(2), TODAY.plusMonths(3));
        }
    }

    @Test
    void scheduleMonthsPastTheContractFoldIntoItsLastMonth() {
        // A six-month table schedule on a two-month contract: months three to six all land in month two.
        List<Integer> schedule = List.of(0, 0, 1, 1, 1, 1);

        List<LocalDate> spawnDates = StratConContractInitializer.rollSpawnDates(TODAY, schedule, 2);

        assertEquals(4, spawnDates.size(), "nothing is dropped");
        for (LocalDate spawnDate : spawnDates) {
            assertWithin(spawnDate, TODAY.plusMonths(1), TODAY.plusMonths(2));
        }
    }

    private static void assertWithin(LocalDate date, LocalDate windowStart, LocalDate windowEnd) {
        assertFalse(date.isBefore(windowStart), date + " is before " + windowStart);
        assertTrue(date.isBefore(windowEnd), date + " is not before " + windowEnd);
    }
}
