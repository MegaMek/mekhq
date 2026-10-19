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
package mekhq.campaign.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CombatTeam#assignInactiveTeamsToActiveContract(Campaign)}, the rule that moves combat teams off ended
 * contracts. It used to run only when the Briefing tab redrew, so it is checked here without any GUI.
 */
class CombatTeamContractAssignmentTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 6, 1);

    private Campaign campaign;
    private final List<AbstractContract> activeContracts = new ArrayList<>();
    private final Hashtable<Integer, CombatTeam> combatTeams = new Hashtable<>();

    @BeforeEach
    void beforeEach() {
        activeContracts.clear();
        combatTeams.clear();

        campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getCombatTeamsAsMap(any())).thenReturn(combatTeams);
        when(campaign.getActiveContracts()).thenReturn(activeContracts);
        when(campaign.getLocalDate()).thenReturn(TODAY);
    }

    @Test
    void teamOnEndedContractMovesToFirstActiveContract() {
        AbstractContract endedContract = contract(false);
        AbstractContract firstActiveContract = contract(true);
        AbstractContract secondActiveContract = contract(true);
        activeContracts.add(firstActiveContract);
        activeContracts.add(secondActiveContract);
        CombatTeam combatTeam = teamOn(endedContract);

        CombatTeam.assignInactiveTeamsToActiveContract(campaign);

        assertEquals(firstActiveContract.getId(), combatTeam.getMissionId());
    }

    @Test
    void teamWithoutContractMovesToFirstActiveContract() {
        AbstractContract activeContract = contract(true);
        activeContracts.add(activeContract);
        CombatTeam combatTeam = teamOn(null);

        CombatTeam.assignInactiveTeamsToActiveContract(campaign);

        assertEquals(activeContract.getId(), combatTeam.getMissionId());
    }

    @Test
    void teamOnActiveContractKeepsIt() {
        AbstractContract firstActiveContract = contract(true);
        AbstractContract secondActiveContract = contract(true);
        activeContracts.add(firstActiveContract);
        activeContracts.add(secondActiveContract);
        CombatTeam combatTeam = teamOn(secondActiveContract);

        CombatTeam.assignInactiveTeamsToActiveContract(campaign);

        assertEquals(secondActiveContract.getId(), combatTeam.getMissionId());
    }

    @Test
    void teamOnEndedContractIsClearedWhenNoContractIsActive() {
        AbstractContract endedContract = contract(false);
        CombatTeam combatTeam = teamOn(endedContract);

        CombatTeam.assignInactiveTeamsToActiveContract(campaign);

        assertNull(combatTeam.getMissionId());
    }

    @Test
    void teamPointingAtAMissingContractIsClearedWhenNoContractIsActive() {
        CombatTeam combatTeam = new CombatTeam();
        // The mission id no longer resolves to a contract, so the campaign returns no contract for it
        combatTeam.setMissionId(UUID.randomUUID());
        combatTeams.put(combatTeams.size(), combatTeam);

        CombatTeam.assignInactiveTeamsToActiveContract(campaign);

        assertNull(combatTeam.getMissionId());
    }

    /**
     * @param isActiveToday whether the contract is active on {@link #TODAY}
     *
     * @return a contract the campaign can look up by its id
     */
    private AbstractContract contract(boolean isActiveToday) {
        AbstractContract contract = mock(AbstractContract.class);
        UUID contractId = UUID.randomUUID();
        when(contract.getId()).thenReturn(contractId);
        when(contract.isActiveOn(TODAY)).thenReturn(isActiveToday);
        when(campaign.getContract(contractId)).thenReturn(contract);
        return contract;
    }

    /**
     * @param contract the contract the team starts on, or {@code null} for none
     *
     * @return a combat team registered with the campaign's player force
     */
    private CombatTeam teamOn(@Nullable AbstractContract contract) {
        CombatTeam combatTeam = new CombatTeam();
        combatTeam.setContract(contract);
        combatTeams.put(combatTeams.size(), combatTeam);
        return combatTeam;
    }
}
