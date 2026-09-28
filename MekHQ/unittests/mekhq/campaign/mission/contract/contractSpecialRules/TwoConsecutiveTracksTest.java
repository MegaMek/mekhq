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
package mekhq.campaign.mission.contract.contractSpecialRules;

import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Tests {@link TwoConsecutiveTracks}: the {@code END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS} special rule's running
 * tally, its scale-scaled thresholds, the StratCon Essential-scenario filter, and the early-ending behavior once a
 * threshold is reached.
 */
class TwoConsecutiveTracksTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 15);

    /**
     * A contract mock whose {@code changeConsecutiveTrackResultTally} and {@code getConsecutiveTrackResultTally}
     * cooperate through a backing counter, so the tally actually accumulates across calls the way the real field does.
     */
    private static AbstractContract contractWithTally(int startingTally, int scale, boolean usesRule) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.usesSpecialRule(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS)).thenReturn(usesRule);
        when(contract.getScale()).thenReturn(scale);

        int[] tally = { startingTally };
        when(contract.getConsecutiveTrackResultTally()).thenAnswer(invocation -> tally[0]);
        doAnswer(invocation -> {
            tally[0] += invocation.getArgument(0, Integer.class);
            return null;
        }).when(contract).changeConsecutiveTrackResultTally(anyInt());

        return contract;
    }

    private static AtBScenario scenarioWithStatus(ScenarioStatus status) {
        AtBScenario scenario = mock(AtBScenario.class);
        when(scenario.getStatus()).thenReturn(status);
        return scenario;
    }

    // region processScenarioResolution - guard clauses

    @Test
    void nullContractIsANoOp() {
        Campaign campaign = mock(Campaign.class);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        TwoConsecutiveTracks.processScenarioResolution(campaign, null, scenario);

        verifyNoInteractions(scenario);
    }

    @Test
    void contractWithoutTheSpecialRuleIsIgnored() {
        Campaign campaign = mock(Campaign.class);
        AbstractContract contract = contractWithTally(0, 1, false);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

        verify(contract, never()).changeConsecutiveTrackResultTally(anyInt());
    }

    // endregion processScenarioResolution - guard clauses

    // region processScenarioResolution - StratCon Essential filter

    @Test
    void everyScenarioCountsWhenTheContractHasNoStratConState() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        AbstractContract contract = contractWithTally(0, 5, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

        verify(contract).changeConsecutiveTrackResultTally(1);
    }

    @Test
    void aNonEssentialScenarioIsIgnoredUnderStratCon() {
        Campaign campaign = mock(Campaign.class);
        AbstractContract contract = contractWithTally(0, 5, true);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        StratConScenario stratConScenario = mock(StratConScenario.class);
        when(stratConScenario.isStrategicObjective()).thenReturn(false);

        try (MockedStatic<StratConCampaignState> stratCon = mockStatic(StratConCampaignState.class)) {
            stratCon.when(() -> StratConCampaignState.getStratConScenarioFromAtBScenario(campaign, scenario))
                  .thenReturn(stratConScenario);

            TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);
        }

        verify(contract, never()).changeConsecutiveTrackResultTally(anyInt());
    }

    @Test
    void aScenarioWithNoBackingStratConScenarioIsIgnored() {
        Campaign campaign = mock(Campaign.class);
        AbstractContract contract = contractWithTally(0, 5, true);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        try (MockedStatic<StratConCampaignState> stratCon = mockStatic(StratConCampaignState.class)) {
            stratCon.when(() -> StratConCampaignState.getStratConScenarioFromAtBScenario(campaign, scenario))
                  .thenReturn(null);

            TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);
        }

        verify(contract, never()).changeConsecutiveTrackResultTally(anyInt());
    }

    @Test
    void anEssentialScenarioCountsUnderStratCon() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        AbstractContract contract = contractWithTally(0, 5, true);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.DEFEAT);

        StratConScenario stratConScenario = mock(StratConScenario.class);
        when(stratConScenario.isStrategicObjective()).thenReturn(true);

        try (MockedStatic<StratConCampaignState> stratCon = mockStatic(StratConCampaignState.class)) {
            stratCon.when(() -> StratConCampaignState.getStratConScenarioFromAtBScenario(campaign, scenario))
                  .thenReturn(stratConScenario);

            TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);
        }

        verify(contract).changeConsecutiveTrackResultTally(-1);
    }

    // endregion processScenarioResolution - StratCon Essential filter

    // region processScenarioResolution - tally direction

    @ParameterizedTest
    @CsvSource({
          "DECISIVE_VICTORY, 1",
          "VICTORY, 1",
          "MARGINAL_VICTORY, 1",
          "PYRRHIC_VICTORY, 1",
          "DEFEAT, -1",
          "DECISIVE_DEFEAT, -1",
          "MARGINAL_DEFEAT, -1",
          "DRAW, -1",
          "REFUSED_ENGAGEMENT, -1",
          "FLEET_IN_BEING, -1"
    })
    void everyNonOverallVictoryStatusCountsAsAFailure(ScenarioStatus status, int expectedDelta) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // A high scale keeps this comfortably clear of the end-contract thresholds.
        AbstractContract contract = contractWithTally(0, 10, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        AtBScenario scenario = scenarioWithStatus(status);

        TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

        verify(contract).changeConsecutiveTrackResultTally(expectedDelta);
    }

    // endregion processScenarioResolution - tally direction

    // region processScenarioResolution - ending the contract

    @Test
    void reachingThePlayerSuccessThresholdEndsTheContractWithOutstandingPay() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // Scale 1 -> success threshold of +2; starting at +1, one more victory reaches it.
        AbstractContract contract = contractWithTally(1, 1, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        when(contract.getHyperlinkedName()).thenReturn("<a>Raid on Sudeten</a>");
        when(contract.getMonthsLeft(TODAY.plusDays(1))).thenReturn(3L);
        when(contract.getMonthlyPayOut()).thenReturn(Money.of(1_000));
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        try (MockedConstruction<ImmersiveDialogNotification> notification = mockConstruction(
              ImmersiveDialogNotification.class)) {
            TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

            assertEquals(1, notification.constructed().size());
        }

        assertEquals(2, contract.getConsecutiveTrackResultTally());
        verify(contract).endContractEarly(TODAY.plusDays(1), Money.of(3_000));
    }

    @Test
    void reachingThePlayerFailureThresholdEndsTheContractWithNoOutstandingPay() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // Scale 1 -> failure threshold of -2; starting at -1, one more defeat reaches it.
        AbstractContract contract = contractWithTally(-1, 1, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        when(contract.getHyperlinkedName()).thenReturn("<a>Raid on Sudeten</a>");
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.DEFEAT);

        try (MockedConstruction<ImmersiveDialogNotification> notification = mockConstruction(
              ImmersiveDialogNotification.class)) {
            TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

            assertEquals(1, notification.constructed().size());
        }

        verify(contract).endContractEarly(TODAY.plusDays(1), Money.zero());
        verify(contract, never()).getMonthsLeft(any());
    }

    @Test
    void aSingleVictoryDoesNotYetEndTheContract() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        AbstractContract contract = contractWithTally(0, 1, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

        assertEquals(1, contract.getConsecutiveTrackResultTally());
        verify(contract, never()).endContractEarly(any(LocalDate.class), any(Money.class));
    }

    @Test
    void thresholdScalesWithContractScale() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // Scale 3 -> success threshold of +6; reaching +5 must not yet end the contract.
        AbstractContract contract = contractWithTally(4, 3, true);
        when(contract.getStratConCampaignState()).thenReturn(null);
        AtBScenario scenario = scenarioWithStatus(ScenarioStatus.VICTORY);

        TwoConsecutiveTracks.processScenarioResolution(campaign, contract, scenario);

        assertEquals(5, contract.getConsecutiveTrackResultTally());
        verify(contract, never()).endContractEarly(any(LocalDate.class), any(Money.class));
    }

    // endregion processScenarioResolution - ending the contract

    // region hasReachedPlayerSuccessThreshold / hasReachedPlayerFailureThreshold

    @Test
    void successThresholdIsFalseWithoutTheSpecialRuleEvenAtTheTally() {
        AbstractContract contract = contractWithTally(2, 1, false);

        assertFalse(TwoConsecutiveTracks.hasReachedPlayerSuccessThreshold(contract));
    }

    @Test
    void successThresholdIsFalseBelowTheThreshold() {
        AbstractContract contract = contractWithTally(1, 1, true);

        assertFalse(TwoConsecutiveTracks.hasReachedPlayerSuccessThreshold(contract));
    }

    @Test
    void successThresholdIsTrueAtExactlyTheThreshold() {
        AbstractContract contract = contractWithTally(2, 1, true);

        assertTrue(TwoConsecutiveTracks.hasReachedPlayerSuccessThreshold(contract));
    }

    @Test
    void successThresholdScalesWithScale() {
        AbstractContract belowScaledThreshold = contractWithTally(5, 3, true);
        AbstractContract atScaledThreshold = contractWithTally(6, 3, true);

        assertFalse(TwoConsecutiveTracks.hasReachedPlayerSuccessThreshold(belowScaledThreshold));
        assertTrue(TwoConsecutiveTracks.hasReachedPlayerSuccessThreshold(atScaledThreshold));
    }

    @Test
    void failureThresholdIsFalseWithoutTheSpecialRuleEvenAtTheTally() {
        AbstractContract contract = contractWithTally(-2, 1, false);

        assertFalse(TwoConsecutiveTracks.hasReachedPlayerFailureThreshold(contract));
    }

    @Test
    void failureThresholdIsFalseAboveTheThreshold() {
        AbstractContract contract = contractWithTally(-1, 1, true);

        assertFalse(TwoConsecutiveTracks.hasReachedPlayerFailureThreshold(contract));
    }

    @Test
    void failureThresholdIsTrueAtExactlyTheThreshold() {
        AbstractContract contract = contractWithTally(-2, 1, true);

        assertTrue(TwoConsecutiveTracks.hasReachedPlayerFailureThreshold(contract));
    }

    // endregion hasReachedPlayerSuccessThreshold / hasReachedPlayerFailureThreshold
}
