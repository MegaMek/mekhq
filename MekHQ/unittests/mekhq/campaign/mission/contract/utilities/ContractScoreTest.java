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
package mekhq.campaign.mission.contract.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link ContractScore#getContractScore(boolean, AbstractContract)}, which reports either the StratCon victory
 * points or - where StratCon is not in play - the tally of the contract's finished scenarios.
 */
class ContractScoreTest {
    private static Scenario scenario(ScenarioStatus status) {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getStatus()).thenReturn(status);
        return scenario;
    }

    private static AbstractContract contractWith(ScenarioStatus... statuses) {
        AbstractContract contract = new ChaosContract();
        Arrays.stream(statuses).map(ContractScoreTest::scenario).forEach(contract::addScenario);
        return contract;
    }

    @ParameterizedTest
    @CsvSource({
          "DECISIVE_VICTORY, 3",
          "VICTORY, 2",
          "MARGINAL_VICTORY, 1",
          "PYRRHIC_VICTORY, 1",
          "DRAW, 0",
          "MARGINAL_DEFEAT, -1",
          "DEFEAT, -2",
          "DECISIVE_DEFEAT, -3",
          "FLEET_IN_BEING, -2",
          "REFUSED_ENGAGEMENT, -3"
    })
    void eachOutcomeContributesItsOwnWeight(ScenarioStatus status, int expectedScore) {
        assertEquals(expectedScore, ContractScore.getContractScore(false, contractWith(status)));
    }

    @Test
    void outcomesAccumulateAcrossScenarios() {
        AbstractContract contract = contractWith(ScenarioStatus.DECISIVE_VICTORY,
              ScenarioStatus.VICTORY,
              ScenarioStatus.DECISIVE_DEFEAT);

        assertEquals(2, ContractScore.getContractScore(false, contract), "3 + 2 - 3");
    }

    @Test
    void scenariosStillBeingFoughtDoNotCountYet() {
        AbstractContract contract = contractWith(ScenarioStatus.VICTORY, ScenarioStatus.CURRENT);

        assertEquals(2, ContractScore.getContractScore(false, contract),
              "an unresolved scenario has no outcome to score");
    }

    @Test
    void aContractWithNoFinishedScenariosScoresZero() {
        assertEquals(0, ContractScore.getContractScore(false, new ChaosContract()));
    }

    @Test
    void stratConContractsReportTheirVictoryPointsInstead() {
        AbstractContract contract = contractWith(ScenarioStatus.DECISIVE_DEFEAT);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.getVictoryPoints()).thenReturn(6);
        contract.setStratConCampaignState(campaignState);

        assertEquals(6, ContractScore.getContractScore(false, contract),
              "under StratCon the campaign state's victory points are the score, not the scenario tally");
    }

    @Test
    void maplessModeFallsBackToTheScenarioTallyEvenUnderStratCon() {
        AbstractContract contract = contractWith(ScenarioStatus.DECISIVE_DEFEAT);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.getVictoryPoints()).thenReturn(6);
        contract.setStratConCampaignState(campaignState);

        assertEquals(-3, ContractScore.getContractScore(true, contract),
              "mapless mode has no tracks to earn victory points on, so scoring falls back to the scenarios");
    }
}
