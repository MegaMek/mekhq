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

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConAssassinationLeadBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Tests for scheduling a StratCon contract's ordinary scenarios up front (see {@link StratConScenarioTempo}), and for
 * the campaign state that rolling the schedule again relies on.
 *
 * <p>With a single track, every row of the Track Intensity Tables' first column holds exactly one item, so each roll
 * schedules exactly one scenario per block of months whatever the die shows.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConScenarioTempoTest {
    private static final LocalDate START_DATE = LocalDate.of(3025, 1, 6);

    // Roll count

    @Test
    void rollsOncePerPointOfScale() {
        assertEquals(4, StratConScenarioTempo.getRollCount(true, false, 4, 1.0));
    }

    @Test
    void rollsThreeTimesPerPointOfScaleWhenSupportPointsAreFactoredIntoScale() {
        assertEquals(12, StratConScenarioTempo.getRollCount(true, true, 4, 1.0));
    }

    @Test
    void rollsOnceWhenTrackIntensityIsNotMultipliedByScale() {
        assertEquals(1, StratConScenarioTempo.getRollCount(false, false, 4, 1.0));
    }

    @Test
    void rollsThreeTimesWhenOnlySupportPointsAreFactoredIntoScale() {
        assertEquals(3, StratConScenarioTempo.getRollCount(false, true, 4, 1.0));
    }

    @Test
    void theTempoMultiplierMultipliesTheRolls() {
        assertEquals(8, StratConScenarioTempo.getRollCount(true, false, 4, 2.0));
    }

    @Test
    void theTempoMultiplierAppliesAfterSupportPoints() {
        assertEquals(24, StratConScenarioTempo.getRollCount(true, true, 4, 2.0));
    }

    @Test
    void theTempoMultiplierRoundsToTheNearestRoll() {
        assertEquals(5, StratConScenarioTempo.getRollCount(true, false, 3, 1.5));
    }

    @Test
    void theTempoMultiplierNeverRollsFewerThanOnce() {
        assertEquals(1, StratConScenarioTempo.getRollCount(true, false, 1, 0.1));
    }

    @Test
    void aScaleBelowOneStillRollsOnce() {
        assertEquals(1, StratConScenarioTempo.getRollCount(true, false, 0, 1.0));
    }

    // Non-objective facilities

    @Test
    void supportPointsInScalePlaceOneFacilityPerPointOfScale() {
        assertEquals(4, StratConContractInitializer.getNonObjectiveFacilityCount(4, true, 0));
    }

    @Test
    void withoutSupportPointsOneFacilityPerThreePointsOfScaleRoundedDown() {
        assertEquals(0, StratConContractInitializer.getNonObjectiveFacilityCount(2, false, 0));
        assertEquals(1, StratConContractInitializer.getNonObjectiveFacilityCount(3, false, 0));
        assertEquals(1, StratConContractInitializer.getNonObjectiveFacilityCount(5, false, 0));
        assertEquals(2, StratConContractInitializer.getNonObjectiveFacilityCount(6, false, 0));
    }

    @Test
    void objectiveFacilitiesCountAgainstTheShare() {
        assertEquals(0, StratConContractInitializer.getNonObjectiveFacilityCount(1, true, 1));
        assertEquals(2, StratConContractInitializer.getNonObjectiveFacilityCount(4, true, 2));
    }

    @Test
    void objectiveFacilitiesBeyondTheShareLeaveNoneOver() {
        assertEquals(0, StratConContractInitializer.getNonObjectiveFacilityCount(3, false, 2));
    }

    @Test
    void supportPointsInScalePlaceOneObjectiveFacilityPerThreePointsOfScale() {
        assertEquals(1, StratConContractInitializer.getObjectiveFacilityCount(5, true));
        assertEquals(2, StratConContractInitializer.getObjectiveFacilityCount(6, true));
    }

    @Test
    void withoutSupportPointsOneObjectiveFacilityPerNinePointsOfScale() {
        assertEquals(1, StratConContractInitializer.getObjectiveFacilityCount(17, false));
        assertEquals(2, StratConContractInitializer.getObjectiveFacilityCount(18, false));
    }

    @Test
    void aFacilityObjectiveAlwaysPlacesAtLeastOne() {
        assertEquals(1, StratConContractInitializer.getObjectiveFacilityCount(1, true));
        assertEquals(1, StratConContractInitializer.getObjectiveFacilityCount(1, false));
    }

    @Test
    void aNegativeScalePlacesNoFacilities() {
        assertEquals(0, StratConContractInitializer.getNonObjectiveFacilityCount(-1, true, 0));
    }

    // Scheduling

    @RepeatedTest(20)
    void theColumnsAreReadAsMonths() {
        LocalDate endDate = START_DATE.plusMonths(3);

        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              3, 1, 3);

        assertEquals(3, scenarioDates.size());
        for (LocalDate scenarioDate : scenarioDates) {
            assertFalse(scenarioDate.isBefore(START_DATE), scenarioDate.toString());
            assertTrue(scenarioDate.isBefore(endDate), scenarioDate.toString());
        }
    }

    @Test
    void theTableIsRolledAgainForALongContract() {
        LocalDate endDate = START_DATE.plusMonths(12);

        // Twelve months overruns the six-month table, so it is rolled a second time rather than leaving months empty.
        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              12, 1, 1);

        assertEquals(2, scenarioDates.size());
    }

    @Test
    void aContractWithNoTracksSchedulesNothingWithoutTheMinimumTrack() {
        LocalDate endDate = START_DATE.plusMonths(3);

        assertTrue(StratConScenarioTempo.rollScenarioDates(START_DATE, endDate, 3, 0, 3, false, false).isEmpty());
    }

    @Test
    void theMinimumTrackGivesAContractWithNoTracksScenarios() {
        LocalDate endDate = START_DATE.plusMonths(3);

        assertEquals(3, StratConScenarioTempo.rollScenarioDates(START_DATE, endDate, 3, 0, 3, true, false).size());
    }

    @RepeatedTest(20)
    void theColumnsAreReadAsWeeksWhenRollingWeekly() {
        LocalDate endDate = START_DATE.plusMonths(3);

        // Ninety days is thirteen weeks: four whole rolls of the three-week table and the first week of a fifth.
        List<LocalDate> scenarioDates = StratConScenarioTempo.rollScenarioDates(START_DATE, endDate,
              3, 1, 1, false, true);

        assertTrue((scenarioDates.size() >= 4) && (scenarioDates.size() <= 5), "count " + scenarioDates.size());
        for (LocalDate scenarioDate : scenarioDates) {
            assertFalse(scenarioDate.isBefore(START_DATE), scenarioDate.toString());
            assertTrue(scenarioDate.isBefore(endDate), scenarioDate.toString());
        }
    }

    @Test
    void aContractEndingOnItsStartSchedulesNothing() {
        assertTrue(StratConScenarioTempo.rollScenarioDates(START_DATE, START_DATE, 3, 1, 1).isEmpty());
    }

    // Scheduling a contract

    @Test
    void acceptingAContractSchedulesItsScenariosAcrossItsRun() {
        LocalDate endDate = START_DATE.plusMonths(3);
        AbstractContract contract = contract(START_DATE, endDate, 3, 2);
        StratConCampaignState campaignState = new StratConCampaignState();

        StratConScenarioTempo.scheduleNormalScenarios(campaign(START_DATE, false, false), contract, campaignState,
              START_DATE);

        assertTrue(campaignState.isNormalTempoScheduled());
        assertEquals(2, campaignState.getScheduledScenarioDates().size());
        assertAllWithin(campaignState.getScheduledScenarioDates(), START_DATE, endDate);
    }

    @Test
    void supportPointsInScaleTripleTheScenarios() {
        LocalDate endDate = START_DATE.plusMonths(3);
        AbstractContract contract = contract(START_DATE, endDate, 3, 2);
        StratConCampaignState campaignState = new StratConCampaignState();
        Campaign campaign = campaign(START_DATE, false, false);
        when(campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION))
              .thenReturn(true);

        StratConScenarioTempo.scheduleNormalScenarios(campaign, contract, campaignState, START_DATE);

        assertEquals(6, campaignState.getScheduledScenarioDates().size());
    }

    // Rolling the schedule again (contract edits, emergency extensions)

    @RepeatedTest(10)
    void anExtendedContractIsScheduledThroughItsNewEnd() {
        LocalDate today = START_DATE.plusMonths(2);
        LocalDate oldEndDate = START_DATE.plusMonths(3);
        LocalDate newEndDate = START_DATE.plusMonths(6);
        // Thirty rolls on the six-month table all but guarantee some land in the three added months.
        AbstractContract contract = contract(START_DATE, newEndDate, 6, 30);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenarioDate(START_DATE.plusMonths(2).plusDays(10));

        StratConScenarioTempo.regenerateSchedules(campaign(today, false, true), contract, campaignState);

        List<LocalDate> scheduledDates = campaignState.getScheduledScenarioDates();
        assertAllWithin(scheduledDates, today, newEndDate);
        assertTrue(scheduledDates.stream().anyMatch(date -> !date.isBefore(oldEndDate)), scheduledDates.toString());
    }

    @Test
    void rollingAgainLeavesDatesAlreadyPastAlone() {
        LocalDate today = START_DATE.plusMonths(1);
        LocalDate pastDate = START_DATE.plusDays(3);
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 1);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenarioDate(pastDate);

        StratConScenarioTempo.regenerateSchedules(campaign(today, false, true), contract, campaignState);

        assertTrue(campaignState.getScheduledScenarioDates().contains(pastDate));
        for (LocalDate scheduledDate : campaignState.getScheduledScenarioDates()) {
            assertTrue(scheduledDate.equals(pastDate) || !scheduledDate.isBefore(today), scheduledDate.toString());
        }
    }

    @Test
    void rollingAgainReplacesTheDatesStillToCome() {
        LocalDate today = START_DATE.plusMonths(1);
        LocalDate staleDate = START_DATE.plusMonths(2);
        // A contract with no tracks rolls nothing, so only a stale date that survived would remain.
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 1);
        when(contract.getTrackCount()).thenReturn(0);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenarioDate(staleDate);

        StratConScenarioTempo.regenerateSchedules(campaign(today, false, true), contract, campaignState);

        assertTrue(campaignState.getScheduledScenarioDates().isEmpty());
    }

    @Test
    void singleDropKeepsItsOwnScenarioDates() {
        LocalDate today = START_DATE.plusMonths(1);
        LocalDate singleDropDate = today.plusDays(2);
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 1);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenarioDate(singleDropDate);

        StratConScenarioTempo.regenerateSchedules(campaign(today, true, true), contract, campaignState);

        assertEquals(List.of(singleDropDate), campaignState.getScheduledScenarioDates());
        assertFalse(campaignState.isNormalTempoScheduled());
    }

    // Campaign state

    @Test
    void movingTheStartDateMovesTheOrdinaryScenariosToo() {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenarioDate(START_DATE);

        campaignState.shiftScheduledDates(3);

        assertEquals(List.of(START_DATE.plusDays(3)), campaignState.getScheduledScenarioDates());
    }

    @Test
    void anOlderSaveLoadsItsWeeklyScenariosAsScheduledScenarioDates() throws Exception {
        String xml = "<StratConCampaignState><weeklyScenarios><weeklyScenario>" + START_DATE
                           + "</weeklyScenario></weeklyScenarios></StratConCampaignState>";
        Document document = DocumentBuilderFactory.newInstance()
                                  .newDocumentBuilder()
                                  .parse(new InputSource(new StringReader(xml)));

        StratConCampaignState campaignState = StratConCampaignState.Deserialize(document.getDocumentElement());

        assertEquals(List.of(START_DATE), campaignState.getScheduledScenarioDates());
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

    private static AbstractContract contract(LocalDate startDate, LocalDate endDate, int lengthInMonths, int scale) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStartDate()).thenReturn(startDate);
        when(contract.getEndingDate()).thenReturn(endDate);
        when(contract.getLengthInMonths()).thenReturn(lengthInMonths);
        when(contract.getScale()).thenReturn(scale);
        when(contract.getTrackCount()).thenReturn(1);
        return contract;
    }

    /**
     * @param today              the campaign date
     * @param isSinglesMode      whether Single Drop play is on
     * @param isMaplessMode      whether mapless play is on, which leaves Essential scenarios and points of interest
     *                           out of a schedule rolled again
     */
    private static Campaign campaign(LocalDate today, boolean isSinglesMode, boolean isMaplessMode) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE)).thenReturn(true);
        when(options.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION)).thenReturn(false);
        when(options.get(CampaignOption.SCENARIO_TEMPO_MULTIPLIER)).thenReturn(1.0);
        when(options.get(CampaignOption.MINIMUM_ONE_TRACK_PER_ROLL)).thenReturn(false);
        when(options.get(CampaignOption.ROLL_TRACKS_WEEKLY)).thenReturn(false);
        when(options.isUseStratConSinglesMode()).thenReturn(isSinglesMode);
        when(options.isUseStratConMaplessMode()).thenReturn(isMaplessMode);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(today);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    private static void assertAllWithin(List<LocalDate> dates, LocalDate firstDate, LocalDate endDate) {
        for (LocalDate date : dates) {
            assertFalse(date.isBefore(firstDate), date.toString());
            assertTrue(date.isBefore(endDate), date.toString());
        }
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
