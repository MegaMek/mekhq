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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Tests when {@link ContractBatchall#offerBatchall} offers no Batchall at all. Whenever one is offered, the player is
 * asked in a dialog, so those cases are not covered here.
 */
class ContractBatchallTest {
    private Campaign campaign;
    private CampaignOptions options;
    private AbstractContract contract;
    private Faction enemyFaction;

    private void setUp(boolean isUsingGenericBattleValue, boolean isEnemyPerformingBatchalls) {
        campaign = MHQTestUtilities.mockCampaign();
        options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.USE_GENERIC_BATTLE_VALUE)).thenReturn(isUsingGenericBattleValue);

        enemyFaction = mock(Faction.class);
        when(enemyFaction.performsBatchalls()).thenReturn(isEnemyPerformingBatchalls);

        contract = mock(AbstractContract.class);
        when(contract.getEnemyFaction()).thenReturn(enemyFaction);
        when(contract.getEnemyFactionCode()).thenReturn("CJF");
    }

    @Test
    void noBatchallWithoutGenericBattleValue() {
        setUp(false, true);

        ContractBatchall.offerBatchall(campaign, contract);

        verify(contract, never()).setEnemyData(any());
    }

    @Test
    void noBatchallFromAnEnemyThatDoesNotFollowTheCustom() {
        setUp(true, false);

        ContractBatchall.offerBatchall(campaign, contract);

        verify(contract, never()).setEnemyData(any());
    }

    @Test
    void noBatchallWhileStandingIsTooLowUnderTheRestrictions() {
        setUp(true, true);
        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isPirateOrBanditCaste()).thenReturn(false);
        when(campaign.getPlayerForce().getFaction()).thenReturn(campaignFaction);
        when(options.isUseFactionStandingBatchallRestrictionsSafe()).thenReturn(true);
        when(campaign.getPlayerForce().getFactionStandings().getRegardForFaction(anyString(), anyBoolean()))
              .thenReturn(-100.0);

        ContractBatchall.offerBatchall(campaign, contract);

        verify(contract, never()).setEnemyData(any());
    }
}
