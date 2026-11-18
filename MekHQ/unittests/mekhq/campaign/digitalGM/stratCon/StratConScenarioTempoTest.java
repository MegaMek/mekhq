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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Tests for rolling a StratCon contract's normal scenarios each week (see {@link StratConScenarioTempo}), laying its
 * schedules out again when it is edited, and the campaign state that relies on.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConScenarioTempoTest {
    private static final LocalDate START_DATE = LocalDate.of(3025, 1, 6);

    // Facility counts

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

    // Weekly rolls

    @Test
    void aSureThreatSchedulesOneScenarioPerPointOfScaleWithinTheWeek() {
        LocalDate monday = START_DATE.minusDays(3);
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 3);
        StratConCampaignState campaignState = campaignStateWithSectors(100, 100);

        int scheduledCount = StratConScenarioTempo.rollWeeklyScenarios(campaign(monday, false, true), contract,
              campaignState, monday);

        assertEquals(3, scheduledCount);
        assertEquals(3, campaignState.getScheduledScenarios().size());
        for (StratConScheduledScenario scheduledScenario : campaignState.getScheduledScenarios()) {
            assertFalse(scheduledScenario.getDate().isBefore(monday));
            assertTrue(scheduledScenario.getDate().isBefore(monday.plusDays(7)));
            assertNotNull(scheduledScenario.getSector(campaignState), "each lands in the sector it was rolled for");
        }
    }

    @Test
    void theRollCountIsRoundedFromTheExactScale() {
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 2);
        when(contract.getExactScale()).thenReturn(2.6);
        StratConCampaignState campaignState = campaignStateWithSectors(100);

        assertEquals(3, StratConScenarioTempo.rollWeeklyScenarios(campaign(START_DATE, false, true), contract,
              campaignState, START_DATE));
    }

    @Test
    void noThreatSchedulesNothing() {
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 5);
        StratConCampaignState campaignState = campaignStateWithSectors(0, 0);

        assertEquals(0, StratConScenarioTempo.rollWeeklyScenarios(campaign(START_DATE, false, true), contract,
              campaignState, START_DATE));
        assertTrue(campaignState.getScheduledScenarios().isEmpty());
    }

    @Test
    void aPeacefulContractSchedulesNothing() {
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 5);
        when(contract.isPeaceful()).thenReturn(true);
        StratConCampaignState campaignState = campaignStateWithSectors(100);

        assertEquals(0, StratConScenarioTempo.rollWeeklyScenarios(campaign(START_DATE, false, true), contract,
              campaignState, START_DATE));
    }

    @RepeatedTest(10)
    void noScenarioIsScheduledOnOrAfterTheContractsEnd() {
        LocalDate endDate = START_DATE.plusDays(2);
        AbstractContract contract = contract(START_DATE, endDate, 1, 10);
        StratConCampaignState campaignState = campaignStateWithSectors(100);

        StratConScenarioTempo.rollWeeklyScenarios(campaign(START_DATE, false, true), contract, campaignState,
              START_DATE);

        for (StratConScheduledScenario scheduledScenario : campaignState.getScheduledScenarios()) {
            assertTrue(scheduledScenario.getDate().isBefore(endDate), scheduledScenario.toString());
        }
    }

    @Test
    void aRollMadeMidweekKeepsToWhatIsLeftOfTheWeek() {
        // START_DATE is a Thursday: four days are left in its week
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 5);
        StratConCampaignState campaignState = campaignStateWithSectors(100);

        StratConScenarioTempo.rollWeeklyScenarios(campaign(START_DATE, false, true), contract, campaignState,
              START_DATE);

        assertEquals(5, campaignState.getScheduledScenarios().size());
        for (StratConScheduledScenario scheduledScenario : campaignState.getScheduledScenarios()) {
            assertFalse(scheduledScenario.getDate().isBefore(START_DATE));
            assertTrue(scheduledScenario.getDate().isBefore(START_DATE.plusDays(4)), scheduledScenario.toString());
        }
    }

    @Test
    void theDaysLeftInAWeekCountTodayThroughSunday() {
        LocalDate monday = START_DATE.minusDays(3);

        assertEquals(7, StratConScenarioTempo.getDaysLeftInWeek(monday));
        assertEquals(4, StratConScenarioTempo.getDaysLeftInWeek(START_DATE));
        assertEquals(1, StratConScenarioTempo.getDaysLeftInWeek(monday.plusDays(6)));
    }

    @Test
    void theWeeklyRollIsDueOncePerWeek() {
        LocalDate monday = START_DATE.minusDays(3);
        StratConCampaignState campaignState = new StratConCampaignState();

        assertTrue(StratConScenarioTempo.isWeeklyRollDue(campaignState, START_DATE), "never rolled");

        campaignState.setLastWeeklyRollDate(monday);
        assertFalse(StratConScenarioTempo.isWeeklyRollDue(campaignState, START_DATE), "already rolled this week");
        assertTrue(StratConScenarioTempo.isWeeklyRollDue(campaignState, monday.plusWeeks(1)), "a new week");

        // A week whose Monday was skipped still rolls on the next day processed
        campaignState.setLastWeeklyRollDate(monday.minusWeeks(1));
        assertTrue(StratConScenarioTempo.isWeeklyRollDue(campaignState, START_DATE));
    }

    @Test
    void exactlyTheThreatInAHundredRollsHit() {
        assertTrue(StratConScenarioTempo.isThreatRollHit(0, 1));
        assertFalse(StratConScenarioTempo.isThreatRollHit(1, 1));
        assertTrue(StratConScenarioTempo.isThreatRollHit(99, 100));
        assertFalse(StratConScenarioTempo.isThreatRollHit(0, 0));
        assertFalse(StratConScenarioTempo.isThreatRollHit(0, -1));
    }

    @Test
    void dueScheduledScenariosAreTakenAndFutureOnesKept() {
        List<StratConScheduledScenario> scheduledScenarios = new ArrayList<>(List.of(
              new StratConScheduledScenario(START_DATE, 0),
              new StratConScheduledScenario(START_DATE.minusDays(2), 1),
              new StratConScheduledScenario(START_DATE.minusDays(30), 0),
              new StratConScheduledScenario(START_DATE.plusDays(1), 0)));

        List<StratConScheduledScenario> due = StratConScenarioTempo.takeDueScheduledScenarios(scheduledScenarios,
              START_DATE);

        assertEquals(2, due.size(), "today's and the one two days late; the month-old one is stale");
        assertEquals(1, scheduledScenarios.size());
        assertEquals(START_DATE.plusDays(1), scheduledScenarios.get(0).getDate());
    }

    @Test
    void aSectorThatNoLongerExistsMeansAnyoneWillDo() {
        StratConCampaignState campaignState = campaignStateWithSectors(50);

        assertNull(new StratConScheduledScenario(START_DATE, 3).getSector(campaignState));
        assertNull(new StratConScheduledScenario(START_DATE, StratConScheduledScenario.ANY_SECTOR)
                         .getSector(campaignState));
        assertNotNull(new StratConScheduledScenario(START_DATE, 0).getSector(campaignState));
    }

    // Laying the schedule out again (contract edits, emergency extensions)

    @Test
    void anUpFrontScheduleMovesOntoTheWeeklyRolls() {
        LocalDate today = START_DATE.plusMonths(1);
        LocalDate pastDate = START_DATE.plusDays(3);
        LocalDate futureDate = today.plusDays(10);
        AbstractContract contract = contract(START_DATE, START_DATE.plusMonths(3), 3, 1);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.setNormalTempoScheduled(true);
        campaignState.addScheduledScenarioDate(pastDate);
        campaignState.addScheduledScenarioDate(futureDate);

        StratConScenarioTempo.regenerateSchedules(campaign(today, false, true), contract, campaignState);

        assertEquals(List.of(pastDate), campaignState.getScheduledScenarioDates());
        assertFalse(campaignState.isNormalTempoScheduled());
    }

    @Test
    void weeklyScenariosPastANewEndAreDropped() {
        LocalDate today = START_DATE.plusMonths(1);
        LocalDate endDate = today.plusDays(3);
        AbstractContract contract = contract(START_DATE, endDate, 1, 1);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addScheduledScenario(new StratConScheduledScenario(today.plusDays(1), 0));
        campaignState.addScheduledScenario(new StratConScheduledScenario(endDate, 0));

        StratConScenarioTempo.regenerateSchedules(campaign(today, false, true), contract, campaignState);

        assertEquals(1, campaignState.getScheduledScenarios().size());
        assertEquals(today.plusDays(1), campaignState.getScheduledScenarios().get(0).getDate());
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

        campaignState.addScheduledScenario(new StratConScheduledScenario(START_DATE, 0));

        campaignState.shiftScheduledDates(3);

        assertEquals(List.of(START_DATE.plusDays(3)), campaignState.getScheduledScenarioDates());
        assertEquals(START_DATE.plusDays(3), campaignState.getScheduledScenarios().get(0).getDate());
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
        when(contract.getExactScale()).thenReturn((double) scale);
        when(contract.getTrackCount()).thenReturn(1);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        return contract;
    }

    /** A campaign state with one sector per threat given, each sector's scenario odds set to that threat. */
    private static StratConCampaignState campaignStateWithSectors(int... threats) {
        StratConCampaignState campaignState = new StratConCampaignState();
        for (int threat : threats) {
            StratConTrackState track = new StratConTrackState();
            track.setScenarioOdds(threat);
            campaignState.addTrack(track);
        }
        return campaignState;
    }

    /**
     * @param today              the campaign date
     * @param isSinglesMode      whether Single Drop play is on
     * @param isMaplessMode      whether mapless play is on, which leaves Essential scenarios and points of interest
     *                           out of a schedule rolled again
     */
    private static Campaign campaign(LocalDate today, boolean isSinglesMode, boolean isMaplessMode) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION)).thenReturn(false);
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
