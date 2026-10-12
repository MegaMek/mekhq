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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MarketsPagesTest {
    private Campaign campaign;
    private CampaignOptions liveOptions;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        liveOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(liveOptions);
        when(campaign.getPlayerForce().getHumanResources().getNewPersonnelMarket()).thenReturn(new NewPersonnelMarket());
    }

    @Test
    void applyingLiveOptionsActivatesTheMarketLoadedFromAPreset() {
        MarketsPages pages = new MarketsPages(campaign);
        CampaignOptions presetOptions = new CampaignOptions();
        presetOptions.set(CampaignOption.PERSONNEL_MARKET_STYLE, PersonnelMarketStyle.MEKHQ);
        pages.loadValuesFromCampaignOptions(presetOptions);

        pages.applyCampaignOptionsToCampaign(liveOptions, false);

        ArgumentCaptor<NewPersonnelMarket> replacementMarket = ArgumentCaptor.forClass(NewPersonnelMarket.class);
        verify(campaign).setNewPersonnelMarket(replacementMarket.capture());
        assertEquals(PersonnelMarketStyle.MEKHQ, replacementMarket.getValue().getAssociatedPersonnelMarketStyle());
        assertSame(campaign, replacementMarket.getValue().getCampaign());
        assertEquals(PersonnelMarketStyle.MEKHQ, liveOptions.get(CampaignOption.PERSONNEL_MARKET_STYLE));
    }

    @Test
    void applyingLiveOptionsRepairsAnAlreadyConfiguredButDisabledMarket() {
        liveOptions.set(CampaignOption.PERSONNEL_MARKET_STYLE, PersonnelMarketStyle.MEKHQ);
        MarketsPages pages = new MarketsPages(campaign);

        pages.applyCampaignOptionsToCampaign(liveOptions, false);

        ArgumentCaptor<NewPersonnelMarket> replacementMarket = ArgumentCaptor.forClass(NewPersonnelMarket.class);
        verify(campaign).setNewPersonnelMarket(replacementMarket.capture());
        assertEquals(PersonnelMarketStyle.MEKHQ, replacementMarket.getValue().getAssociatedPersonnelMarketStyle());
        assertEquals(PersonnelMarketStyle.MEKHQ, liveOptions.get(CampaignOption.PERSONNEL_MARKET_STYLE));
    }

    @Test
    void savingAPresetDoesNotChangeTheLiveOptionsOrMarket() {
        MarketsPages pages = new MarketsPages(campaign);
        CampaignOptions presetOptions = new CampaignOptions();
        presetOptions.set(CampaignOption.PERSONNEL_MARKET_STYLE, PersonnelMarketStyle.MEKHQ);
        pages.loadValuesFromCampaignOptions(presetOptions);
        CampaignOptions destination = new CampaignOptions();

        pages.applyCampaignOptionsToCampaign(destination, true);

        assertEquals(PersonnelMarketStyle.MEKHQ, destination.get(CampaignOption.PERSONNEL_MARKET_STYLE));
        assertEquals(PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED,
              liveOptions.get(CampaignOption.PERSONNEL_MARKET_STYLE));
        verify(campaign.getPlayerForce().getHumanResources(), never()).getNewPersonnelMarket();
        verify(campaign, never()).setNewPersonnelMarket(any(NewPersonnelMarket.class));
    }
}
