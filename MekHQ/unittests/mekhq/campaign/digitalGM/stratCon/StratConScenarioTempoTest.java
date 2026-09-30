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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConAssassinationLeadBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Tests for scheduling a StratCon contract's ordinary scenarios up front (see {@link StratConScenarioTempo}), and for
 * the campaign state that rolling the schedule again relies on.
 *
 * <p>With a single track, every row of the Track Intensity Tables' first column holds exactly one item, so each roll
 * schedules exactly one scenario per block whatever the die shows.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConScenarioTempoTest {
    private static final LocalDate START_DATE = LocalDate.of(3025, 1, 6);

    // Roll count

    @Test
    void rollsOncePerPointOfScale() {
        assertEquals(4, StratConScenarioTempo.getRollCount(true, false, 4));
    }

    @Test
    void rollsThreeTimesPerPointOfScaleWhenSupportPointsAreFactoredIntoScale() {
        assertEquals(12, StratConScenarioTempo.getRollCount(true, true, 4));
    }

    @Test
    void rollsOnceWhenTrackIntensityIsNotMultipliedByScale() {
        assertEquals(1, StratConScenarioTempo.getRollCount(false, false, 4));
    }

    @Test
    void rollsThreeTimesWhenOnlySupportPointsAreFactoredIntoScale() {
        assertEquals(3, StratConScenarioTempo.getRollCount(false, true, 4));
    }

    @Test
    void aScaleBelowOneStillRollsOnce() {
        assertEquals(1, StratConScenarioTempo.getRollCount(true, false, 0));
    }

    // Weekly scheduling

    @RepeatedTest(20)
    void weeklyPlayRollsTheTableAfreshForEachSixWeeks() {
        LocalDate endDate = START_DATE.plusWeeks(12);

        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              3, 1, 2, false);

        assertEquals(4, scenarioDates.size());
        LocalDate secondBlockStart = START_DATE.plusWeeks(6);
        assertEquals(2, countBefore(scenarioDates, secondBlockStart));
    }

    @RepeatedTest(20)
    void weeklyPlayKeepsEveryScenarioWithinTheContract() {
        LocalDate endDate = START_DATE.plusDays(10);

        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              1, 6, 2, false);

        for (LocalDate scenarioDate : scenarioDates) {
            assertFalse(scenarioDate.isBefore(START_DATE), scenarioDate.toString());
            assertTrue(scenarioDate.isBefore(endDate), scenarioDate.toString());
        }
    }

    // Monthly scheduling ("Fewer Weekly Scenarios")

    @RepeatedTest(20)
    void fewerWeeklyScenariosReadsTheColumnsAsMonths() {
        LocalDate endDate = START_DATE.plusMonths(3);

        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              3, 1, 3, true);

        assertEquals(3, scenarioDates.size());
        for (LocalDate scenarioDate : scenarioDates) {
            assertFalse(scenarioDate.isBefore(START_DATE), scenarioDate.toString());
            assertTrue(scenarioDate.isBefore(endDate), scenarioDate.toString());
        }
    }

    @Test
    void fewerWeeklyScenariosRollsTheTableAgainForALongContract() {
        LocalDate endDate = START_DATE.plusMonths(12);

        // Twelve months overruns the six-month table, so it is rolled a second time rather than leaving months empty.
        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              12, 1, 1, true);

        assertEquals(2, scenarioDates.size());
    }

    @Test
    void aContractEndingOnItsStartSchedulesNothing() {
        assertTrue(StratConScenarioTempo.rollScenarioDates(START_DATE, START_DATE, 3, 1, 1, false).isEmpty());
    }

    // Campaign state

    @Test
    void movingTheStartDateMovesTheOrdinaryScenariosToo() {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addWeeklyScenario(START_DATE);

        campaignState.shiftScheduledDates(3);

        assertEquals(List.of(START_DATE.plusDays(3)), campaignState.getWeeklyScenarios());
    }

    @Test
    void placingAPointOfInterestCountsItAndItsMarks() {
        StratConCampaignState campaignState = new StratConCampaignState();
        StratConScheduledPointOfInterest scheduledPointOfInterest = new StratConScheduledPointOfInterest(START_DATE,
              StratConAssassinationLeadBehavior.BEHAVIOR_ID, true);
        scheduledPointOfInterest.getInitialState().put(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY,
              Boolean.TRUE.toString());

        campaignState.recordPlacedPointOfInterest(scheduledPointOfInterest);

        assertEquals(1, campaignState.getPlacedPointOfInterestCount());
        Map<String, Integer> markedCounts = campaignState.getMarkedPointOfInterestCounts();
        assertEquals(1, markedCounts.get(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY));
    }

    @Test
    void seedingTheLedgerHappensOnlyOnce() {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.seedPointOfInterestLedger();
        campaignState.setPlacedPointOfInterestCount(2);

        campaignState.seedPointOfInterestLedger();

        assertTrue(campaignState.isPointOfInterestLedgerSeeded());
        assertEquals(2, campaignState.getPlacedPointOfInterestCount());
    }

    // Marks already made

    @Test
    void realTargetsAlreadyPlacedCountAgainstTheScale() {
        List<StratConScheduledPointOfInterest> scheduled = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            scheduled.add(new StratConScheduledPointOfInterest(START_DATE,
                  StratConAssassinationLeadBehavior.BEHAVIOR_ID, true));
        }
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(3);

        new StratConAssassinationLeadBehavior().onScheduled(scheduled, contract,
              Map.of(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY, 2));

        assertEquals(1, countRealTargets(scheduled));
    }

    @Test
    void noRealTargetsAreMarkedOnceTheScaleIsUsedUp() {
        List<StratConScheduledPointOfInterest> scheduled = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            scheduled.add(new StratConScheduledPointOfInterest(START_DATE,
                  StratConAssassinationLeadBehavior.BEHAVIOR_ID, true));
        }
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(1);

        new StratConAssassinationLeadBehavior().onScheduled(scheduled, contract,
              Map.of(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY, 2));

        assertEquals(0, countRealTargets(scheduled));
    }

    private static int countBefore(List<LocalDate> dates, LocalDate cutoff) {
        int count = 0;
        for (LocalDate date : dates) {
            if (date.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    private static int countRealTargets(List<StratConScheduledPointOfInterest> scheduledPointsOfInterest) {
        int count = 0;
        for (StratConScheduledPointOfInterest scheduledPointOfInterest : scheduledPointsOfInterest) {
            if (Boolean.parseBoolean(scheduledPointOfInterest.getInitialState()
                                           .get(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY))) {
                count++;
            }
        }
        return count;
    }
}
