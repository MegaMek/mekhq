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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.IFacilityStrategy;
import mekhq.campaign.digitalGM.IScenarioGenerationStrategy;
import mekhq.campaign.digitalGM.IScenarioLifecycleStrategy;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import org.junit.jupiter.api.Test;

/**
 * Behavioural coverage for the shared StratCon daily loop in {@link AbstractStratConGM} &mdash; the logic migrated out
 * of {@code StratConRulesManager.handleNewDay}. Each play type's rules are exercised by injecting mock strategies and
 * asserting which strategy calls the loop makes for a controlled campaign day. (That each concrete GM wires the correct
 * strategies is covered separately by {@code StratConDigitalGMTest}.)
 *
 * @author Illiani
 */
class AbstractStratConGMTest {

    /** 2024-01-01 is a Monday (and the first of the month). */
    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);
    /** 2024-01-02 is a Tuesday. */
    private static final LocalDate TUESDAY = LocalDate.of(2024, 1, 2);

    /** A GM whose strategies are mocks so the loop's calls can be verified. */
    private static class TestGM extends AbstractStratConGM {
        final IScenarioGenerationStrategy generation = mock(IScenarioGenerationStrategy.class);
        final IScenarioLifecycleStrategy lifecycle = mock(IScenarioLifecycleStrategy.class);
        final IFacilityStrategy facility = mock(IFacilityStrategy.class);
        private final boolean singleDrop;

        TestGM(boolean singleDrop) {
            this.singleDrop = singleDrop;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isEnabled(CampaignOptions campaignOptions) {
            return true;
        }

        @Override
        protected IScenarioGenerationStrategy getScenarioGenerationStrategy() {
            return generation;
        }

        @Override
        protected IScenarioLifecycleStrategy getScenarioLifecycleStrategy() {
            return lifecycle;
        }

        @Override
        protected IFacilityStrategy getFacilityStrategy() {
            return facility;
        }

        @Override
        protected boolean isSingleDropMode() {
            return singleDrop;
        }
    }

    private static StratConTrackState trackWith(StratConScenario... scenarios) {
        StratConTrackState track = mock(StratConTrackState.class);
        Map<StratConCoords, StratConScenario> map = new LinkedHashMap<>();
        for (StratConScenario scenario : scenarios) {
            map.put(mock(StratConCoords.class), scenario);
        }
        when(track.getScenarios()).thenReturn(map);
        return track;
    }

    private static StratConScenario scenario(LocalDate deploymentDate, boolean hasCommittedForces) {
        StratConScenario scenario = mock(StratConScenario.class);
        when(scenario.getDeploymentDate()).thenReturn(deploymentDate);
        when(scenario.getPrimaryForceIDs()).thenReturn(
              hasCommittedForces ? new ArrayList<>(List.of(1)) : new ArrayList<>());
        return scenario;
    }

    private static AtBContract contractWith(StratConCampaignState campaignState, AtBMoraleLevel morale) {
        AtBContract contract = mock(AtBContract.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(morale);
        return contract;
    }

    private static StratConCampaignState campaignStateWith(List<StratConTrackState> tracks,
          List<LocalDate> weeklyScenarios) {
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.getTracks()).thenReturn(tracks);
        when(campaignState.getWeeklyScenarios()).thenReturn(weeklyScenarios);
        return campaignState;
    }

    private static NewDayEvent newDay(LocalDate today, AtBContract contract) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(today);
        when(campaign.getActiveAtBContracts()).thenReturn(List.of(contract));

        NewDayEvent event = mock(NewDayEvent.class);
        when(event.getCampaign()).thenReturn(campaign);
        return event;
    }

    @Test
    void mondayGeneratesWeeklyDatesForEveryTrackWhenNotSingleDrop() {
        StratConTrackState track1 = trackWith();
        StratConTrackState track2 = trackWith();
        StratConCampaignState campaignState = campaignStateWith(List.of(track1, track2), new ArrayList<>());
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(MONDAY, contract));

        verify(gm.generation).generateWeeklyScenarioDates(any(),
              eq(campaignState),
              eq(contract),
              eq(track1),
              eq(false));
        verify(gm.generation).generateWeeklyScenarioDates(any(),
              eq(campaignState),
              eq(contract),
              eq(track2),
              eq(false));
    }

    @Test
    void mondayGeneratesWeeklyDatesForOnlyFirstTrackWhenSingleDrop() {
        StratConTrackState track1 = trackWith();
        StratConTrackState track2 = trackWith();
        StratConCampaignState campaignState = campaignStateWith(List.of(track1, track2), new ArrayList<>());
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(true);

        gm.handleNewDay(newDay(MONDAY, contract));

        // Single-drop caps the campaign to one scenario/week: only the first track schedules
        verify(gm.generation).generateWeeklyScenarioDates(any(), any(), any(), eq(track1), eq(true));
        verify(gm.generation, never()).generateWeeklyScenarioDates(any(), any(), any(), eq(track2), anyBoolean());
    }

    @Test
    void nonMondayDoesNotGenerateWeeklyDates() {
        StratConTrackState track = trackWith();
        StratConCampaignState campaignState = campaignStateWith(List.of(track), new ArrayList<>());
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(TUESDAY, contract));

        verify(gm.generation, never()).generateWeeklyScenarioDates(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void getFacilityStrategyEffectsAndForceReturnsRunForEveryTrack() {
        StratConTrackState track1 = trackWith();
        StratConTrackState track2 = trackWith();
        StratConCampaignState campaignState = campaignStateWith(List.of(track1, track2), new ArrayList<>());
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(TUESDAY, contract));

        // The loop always calls facility(); skipping in Mapless/Singles is handled by wiring a NoOpFacilityStrategy
        verify(gm.facility, times(2)).applyPeriodicEffects(any(), eq(campaignState), anyBoolean());
        verify(gm.lifecycle).processForceReturnDates(eq(track1), any());
        verify(gm.lifecycle).processForceReturnDates(eq(track2), any());
    }

    @Test
    void onlyExpiredUncommittedScenariosAreProcessed() {
        StratConScenario expired = scenario(TUESDAY.minusDays(1), false);
        StratConScenario expiredButCommitted = scenario(TUESDAY.minusDays(1), true);
        StratConScenario future = scenario(TUESDAY.plusDays(5), false);
        StratConTrackState track = trackWith(expired, expiredButCommitted, future);
        StratConCampaignState campaignState = campaignStateWith(List.of(track), new ArrayList<>());
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(TUESDAY, contract));

        verify(gm.lifecycle).processExpiredScenario(eq(expired), eq(track), eq(campaignState));
        verify(gm.lifecycle, never()).processExpiredScenario(eq(expiredButCommitted), any(), any());
        verify(gm.lifecycle, never()).processExpiredScenario(eq(future), any(), any());
    }

    @Test
    void dailyScenariosGeneratedWhenScheduledTodayAndNotRouted() {
        StratConTrackState track = trackWith();
        // today scheduled twice -> scenarioCount == 2
        StratConCampaignState campaignState = campaignStateWith(List.of(track),
              new ArrayList<>(List.of(TUESDAY, TUESDAY)));
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(TUESDAY, contract));

        verify(gm.generation).generateDailyScenarios(any(), eq(campaignState), eq(contract), eq(2));
    }

    @Test
    void dailyScenariosSkippedWhenOpForRouted() {
        StratConTrackState track = trackWith();
        StratConCampaignState campaignState = campaignStateWith(List.of(track),
              new ArrayList<>(List.of(TUESDAY)));
        AtBContract contract = contractWith(campaignState, AtBMoraleLevel.ROUTED);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(TUESDAY, contract));

        verify(gm.generation, never()).generateDailyScenarios(any(),
              any(),
              any(),
              org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void contractWithoutCampaignStateIsSkipped() {
        AtBContract contract = contractWith(null, AtBMoraleLevel.STALEMATE);
        TestGM gm = new TestGM(false);

        gm.handleNewDay(newDay(MONDAY, contract));

        verifyNoInteractions(gm.generation);
        verifyNoInteractions(gm.lifecycle);
        verifyNoInteractions(gm.facility);
    }
}
