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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ChaosContractDeterminationRequiredVictoryPoints#getRequiredVictoryPoints(AbstractContract)}: the two
 * short-circuits (no StratCon state, and independent command rights), and the expected-turning-points formula
 * {@code ceil(scale * duration * meanScenarioOdds/100 * turningPointChance)} - where garrisons shorten the effective
 * duration to 75% and only integrated command earns the full turning-point chance.
 */
class ChaosContractDeterminationRequiredVictoryPointsTest {

    private static StratConTrackState track(final int scenarioOdds) {
        StratConTrackState track = mock(StratConTrackState.class);
        when(track.getScenarioOdds()).thenReturn(scenarioOdds);
        return track;
    }

    private static StratConCampaignState stateWithTracks(final StratConTrackState... tracks) {
        StratConCampaignState state = mock(StratConCampaignState.class);
        when(state.getTracks()).thenReturn(List.of(tracks));
        return state;
    }

    @Test
    void aContractWithoutStratConStateNeedsNoVictoryPoints() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(null);

        assertEquals(0, ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract));
    }

    @Test
    void independentCommandUsesTheFixedRequirement() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(mock(StratConCampaignState.class));
        when(contract.getCommandRights()).thenReturn(ContractCommandRights.INDEPENDENT);

        assertEquals(INDEPENDENT_COMMAND_RIGHTS_REQUIRED_VICTORY_POINTS,
              ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract));
    }

    @Test
    void integratedCommandTakesTheFullTurningPointChance() {
        StratConCampaignState state = stateWithTracks(track(50));
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getCommandRights()).thenReturn(ContractCommandRights.INTEGRATED);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.OBJECTIVE_RAID);
        when(contract.getScale()).thenReturn(2);
        when(contract.getLengthInMonths()).thenReturn(3);

        // ceil(2 * 3 * (50/100) * 1.0) = ceil(3.0) = 3
        assertEquals(3, ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract));
    }

    @Test
    void nonIntegratedCommandOnlyTakesAThirdOfTheTurningPoints() {
        StratConCampaignState state = stateWithTracks(track(100), track(50));
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getCommandRights()).thenReturn(ContractCommandRights.HOUSE);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.OBJECTIVE_RAID);
        when(contract.getScale()).thenReturn(4);
        when(contract.getLengthInMonths()).thenReturn(4);

        // mean odds = 75 -> 0.75; ceil(4 * 4 * 0.75 * 0.33) = ceil(3.96) = 4
        assertEquals(4, ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract));
    }

    @Test
    void garrisonContractsShortenTheEffectiveDuration() {
        StratConCampaignState state = stateWithTracks(track(100));
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(state);
        when(contract.getCommandRights()).thenReturn(ContractCommandRights.INTEGRATED);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
        when(contract.getScale()).thenReturn(2);
        when(contract.getLengthInMonths()).thenReturn(4);

        // duration -> ceil(4 * 0.75) = 3; ceil(2 * 3 * 1.0 * 1.0) = 6
        assertEquals(6, ChaosContractDeterminationRequiredVictoryPoints.getRequiredVictoryPoints(contract));
    }
}
