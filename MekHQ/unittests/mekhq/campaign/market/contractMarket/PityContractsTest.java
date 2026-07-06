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
package mekhq.campaign.market.contractMarket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests {@link PityContracts#createPityContract}, in particular that overwriting a freshly-generated contract's
 * enemy (and, for non-pirate campaigns, its type) correctly triggers a re-resolution of the attacker/defender roles
 * and the target system, rather than leaving them stale from whatever the contract's original (pre-override) enemy
 * happened to be.
 */
class PityContractsTest {

    private static AtBContract mockContract() {
        AtBContract contract = mock(AtBContract.class);
        when(contract.getContractType()).thenReturn(AtBContractType.PIRATE_HUNTING);
        when(contract.getStartDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(contract.getEmployerName()).thenReturn("Some Employer");
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getName(any())).thenReturn("Some System");
        when(contract.getSystem()).thenReturn(system);
        return contract;
    }

    private static Campaign mockCampaign() {
        Campaign campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(campaign.isPirateCampaign()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(mock(CampaignOptions.class));
        when(campaign.getAllCombatEntities()).thenReturn(List.of());
        return campaign;
    }

    /**
     * Regression test: {@code addAtBContract} resolves the attacker/defender roles and target system against the
     * contract's *original* enemy, before the pity-contract overrides run. Once the enemy (and contract type) are
     * overwritten, those roles and that system are stale and must be redone against the actual pity-contract enemy.
     */
    @Test
    void createPityContractRecomputesAttackerAndSystemAfterEnemyOverride() {
        Campaign campaign = mockCampaign();
        AtBContract contract = mockContract();
        AbstractContractMarket contractMarket = mock(AbstractContractMarket.class);
        when(contractMarket.addAtBContract(campaign)).thenReturn(contract);

        PityContracts.createPityContract(campaign, contractMarket);

        InOrder order = inOrder(contract, contractMarket);
        order.verify(contract).setEnemyCode(any());
        order.verify(contractMarket).setAttacker(contract);
        order.verify(contractMarket).setSystemId(contract, campaign);
    }

    /**
     * Regression test: if no valid location can be found for the pity contract's actual enemy, the half-configured
     * contract (already added to the market by {@code addAtBContract}) must be removed rather than left in the
     * market with a mismatched or missing target system.
     */
    @Test
    void createPityContractRemovesContractWhenNoLocationFound() throws Exception {
        Campaign campaign = mockCampaign();
        AtBContract contract = mockContract();
        AbstractContractMarket contractMarket = mock(AbstractContractMarket.class);
        when(contractMarket.addAtBContract(campaign)).thenReturn(contract);
        doThrow(new AbstractContractMarket.NoContractLocationFoundException("no location"))
              .when(contractMarket)
              .setSystemId(eq(contract), eq(campaign));

        PityContracts.createPityContract(campaign, contractMarket);

        verify(contractMarket).removeContract(contract);
        verify(contract, never()).setName(any());
    }
}
