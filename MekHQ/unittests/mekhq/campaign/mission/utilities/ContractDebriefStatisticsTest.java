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
package mekhq.campaign.mission.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import org.junit.jupiter.api.Test;

/**
 * Verifies the combat-record roll-up, win-rate calculation, and - the substantive new behaviour - the
 * {@link MissionStatus} recommendation. The recommendation follows the game's own win rule (contract score vs required
 * victory points) and treats an early termination that has not met that target as a breach.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ContractDebriefStatisticsTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 1);
    private static final LocalDate FUTURE_END = TODAY.plusMonths(3);
    private static final LocalDate PAST_END = TODAY.minusDays(1);

    private static Scenario scenarioWith(ScenarioStatus status) {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getStatus()).thenReturn(status);
        return scenario;
    }

    private static StratConStrategicObjective objective(boolean completed, boolean failed) {
        StratConStrategicObjective objective = mock(StratConStrategicObjective.class);
        when(objective.isObjectiveCompleted(any())).thenReturn(completed);
        when(objective.isObjectiveFailed(any())).thenReturn(failed);
        return objective;
    }

    private static AbstractContract baseContract(List<Scenario> scenarios) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScenarios()).thenReturn(scenarios);
        when(contract.getCompletedScenarios()).thenReturn(scenarios);
        when(contract.getStratConCampaignState()).thenReturn(null);
        when(contract.getRequiredVictoryPoints()).thenReturn(0);
        when(contract.getEndingDate()).thenReturn(PAST_END);
        when(contract.getTotalPay()).thenReturn(Money.zero());
        when(contract.getSalvagedByUnitValue()).thenReturn(Money.zero());
        return contract;
    }

    private static StratConCampaignState stratConWith(int victoryPoints, List<StratConStrategicObjective> objectives) {
        StratConTrackState track = mock(StratConTrackState.class);
        when(track.getStrategicObjectives()).thenReturn(objectives);
        List<StratConTrackState> tracks = new ArrayList<>();
        tracks.add(track);

        StratConCampaignState state = mock(StratConCampaignState.class);
        when(state.getVictoryPoints()).thenReturn(victoryPoints);
        when(state.getTracks()).thenReturn(tracks);
        return state;
    }

    private static ContractDebriefStatistics from(AbstractContract contract) {
        return ContractDebriefStatistics.from(contract, false, TODAY);
    }

    @Test
    void countsScenariosAndWinRate() {
        List<Scenario> scenarios = new ArrayList<>();
        scenarios.add(scenarioWith(ScenarioStatus.DECISIVE_VICTORY));
        scenarios.add(scenarioWith(ScenarioStatus.MARGINAL_VICTORY));
        scenarios.add(scenarioWith(ScenarioStatus.DRAW));
        scenarios.add(scenarioWith(ScenarioStatus.DECISIVE_DEFEAT));
        scenarios.add(scenarioWith(ScenarioStatus.CURRENT)); // unresolved: ignored

        ContractDebriefStatistics statistics = from(baseContract(scenarios));

        assertEquals(4, statistics.getResolvedScenarios());
        assertEquals(2, statistics.getVictories());
        assertEquals(1, statistics.getDecisiveVictories());
        assertEquals(1, statistics.getDraws());
        assertEquals(1, statistics.getDefeats());
        assertEquals(1, statistics.getDecisiveDefeats());
        assertEquals(50, statistics.getWinRatePercent());
    }

    @Test
    void winRateIsZeroWithoutResolvedScenarios() {
        assertEquals(0, from(baseContract(new ArrayList<>())).getWinRatePercent());
    }

    @Test
    void recommendsSuccessWhenVictoryPointTargetMet() {
        StratConCampaignState state = stratConWith(5, new ArrayList<>());
        AbstractContract contract = baseContract(new ArrayList<>());
        when(contract.getRequiredVictoryPoints()).thenReturn(3);
        when(contract.getStratConCampaignState()).thenReturn(state);

        ContractDebriefStatistics statistics = from(contract);

        assertEquals(5, statistics.getContractScore());
        assertEquals(MissionStatus.SUCCESS, statistics.getRecommendedStatus());
    }

    @Test
    void recommendsSuccessWhenTargetMetEvenEndingEarly() {
        StratConCampaignState state = stratConWith(3, new ArrayList<>());
        AbstractContract contract = baseContract(new ArrayList<>());
        when(contract.getRequiredVictoryPoints()).thenReturn(1);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getEndingDate()).thenReturn(FUTURE_END);

        assertEquals(MissionStatus.SUCCESS, from(contract).getRecommendedStatus());
    }

    @Test
    void recommendsBreachWhenEndingEarlyBelowTargetDespiteObjectivesSecured() {
        // The reported bug: all objectives completed, but score -3 is below the required 1 and the contract is being
        // ended early. This must not be a success.
        List<StratConStrategicObjective> objectives = new ArrayList<>();
        objectives.add(objective(true, false));
        objectives.add(objective(true, false));

        StratConCampaignState state = stratConWith(-3, objectives);
        AbstractContract contract = baseContract(new ArrayList<>());
        when(contract.getRequiredVictoryPoints()).thenReturn(1);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getEndingDate()).thenReturn(FUTURE_END);

        ContractDebriefStatistics statistics = from(contract);

        assertEquals(2, statistics.getObjectivesCompleted());
        assertEquals(-3, statistics.getContractScore());
        assertEquals(MissionStatus.BREACH, statistics.getRecommendedStatus());
    }

    @Test
    void recommendsPartialWhenRunningToTermBelowTargetWithProgress() {
        StratConCampaignState state = stratConWith(1, new ArrayList<>());
        AbstractContract contract = baseContract(new ArrayList<>());
        when(contract.getRequiredVictoryPoints()).thenReturn(3);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getEndingDate()).thenReturn(PAST_END); // ran to term

        assertEquals(MissionStatus.PARTIAL, from(contract).getRecommendedStatus());
    }

    @Test
    void recommendsFailureWhenRunningToTermWithNoProgress() {
        StratConCampaignState state = stratConWith(-3, new ArrayList<>());
        AbstractContract contract = baseContract(new ArrayList<>());
        when(contract.getRequiredVictoryPoints()).thenReturn(3);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getEndingDate()).thenReturn(PAST_END);

        assertEquals(MissionStatus.FAILED, from(contract).getRecommendedStatus());
    }

    @Test
    void fallsBackToScenarioScoreWithoutVictoryPointTarget() {
        List<Scenario> winning = new ArrayList<>();
        winning.add(scenarioWith(ScenarioStatus.VICTORY));
        winning.add(scenarioWith(ScenarioStatus.VICTORY));
        winning.add(scenarioWith(ScenarioStatus.MARGINAL_DEFEAT));
        assertEquals(MissionStatus.SUCCESS, from(baseContract(winning)).getRecommendedStatus());

        List<Scenario> losing = new ArrayList<>();
        losing.add(scenarioWith(ScenarioStatus.DECISIVE_DEFEAT));
        losing.add(scenarioWith(ScenarioStatus.DEFEAT));
        assertEquals(MissionStatus.FAILED, from(baseContract(losing)).getRecommendedStatus());

        // No scenarios fought at all: nothing was lost, so a success is recommended.
        assertEquals(MissionStatus.SUCCESS, from(baseContract(new ArrayList<>())).getRecommendedStatus());
    }
}
