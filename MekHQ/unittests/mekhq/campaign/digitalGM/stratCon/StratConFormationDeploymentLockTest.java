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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link StratConRulesManager#isFormationDeployedToStratCon(Campaign, Formation)}, which freezes the membership
 * of formations deployed to a StratCon track (and everything beneath them) so units can't be pulled home early.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFormationDeploymentLockTest {
    private static final int COMPANY_ID = 10;
    private static final int LANCE_ID = 11;

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private StratConCampaignState stratConState;
    private Formation company;
    private Formation lance;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaignOptions.isUseStratCon()).thenReturn(true);

        stratConState = mock(StratConCampaignState.class);
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(stratConState);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));

        company = mock(Formation.class);
        when(company.getId()).thenReturn(COMPANY_ID);
        when(company.getAllParents()).thenReturn(List.of());

        lance = mock(Formation.class);
        when(lance.getId()).thenReturn(LANCE_ID);
        when(lance.getAllParents()).thenReturn(List.of(company));
    }

    @Test
    void formationDeployedItselfIsDeployed() {
        when(stratConState.isForceDeployedHere(LANCE_ID)).thenReturn(true);

        assertTrue(StratConRulesManager.isFormationDeployedToStratCon(campaign, lance));
    }

    @Test
    void formationInsideDeployedParentIsDeployed() {
        when(stratConState.isForceDeployedHere(COMPANY_ID)).thenReturn(true);

        assertTrue(StratConRulesManager.isFormationDeployedToStratCon(campaign, lance));
    }

    @Test
    void parentOfDeployedFormationIsNotDeployed() {
        when(stratConState.isForceDeployedHere(LANCE_ID)).thenReturn(true);

        assertFalse(StratConRulesManager.isFormationDeployedToStratCon(campaign, company));
    }

    @Test
    void undeployedFormationIsNotDeployed() {
        assertFalse(StratConRulesManager.isFormationDeployedToStratCon(campaign, lance));
    }

    @Test
    void noFormationIsNotDeployed() {
        assertFalse(StratConRulesManager.isFormationDeployedToStratCon(campaign, null));
    }

    @Test
    void stratConDisabledIsNeverDeployed() {
        when(campaignOptions.isUseStratCon()).thenReturn(false);
        when(stratConState.isForceDeployedHere(LANCE_ID)).thenReturn(true);

        assertFalse(StratConRulesManager.isFormationDeployedToStratCon(campaign, lance));
    }
}
