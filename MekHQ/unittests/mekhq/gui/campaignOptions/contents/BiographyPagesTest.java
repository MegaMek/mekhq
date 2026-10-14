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
package mekhq.gui.campaignOptions.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class BiographyPagesTest {
    private Campaign campaign;
    private CampaignOptions liveOptions;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        liveOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(liveOptions);
    }

    @Test
    void applyingLiveOptionsAppliesTheSelectedRankSystem() {
        try (MockedConstruction<RankPage> rankPages = mockConstruction(RankPage.class)) {
            BiographyPages pages = new BiographyPages(campaign, null);

            // The dialog always passes the campaign's own options when applying, never null
            pages.applyCampaignOptionsToCampaign(liveOptions, false);

            assertEquals(1, rankPages.constructed().size());
            verify(rankPages.constructed().get(0)).applyToCampaign();
        }
    }

    @Test
    void savingAPresetLeavesTheCampaignRankSystemUnchanged() {
        try (MockedConstruction<RankPage> rankPages = mockConstruction(RankPage.class)) {
            BiographyPages pages = new BiographyPages(campaign, null);

            pages.applyCampaignOptionsToCampaign(new CampaignOptions(), true);

            assertEquals(1, rankPages.constructed().size());
            verify(rankPages.constructed().get(0), never()).applyToCampaign();
        }
    }
}
