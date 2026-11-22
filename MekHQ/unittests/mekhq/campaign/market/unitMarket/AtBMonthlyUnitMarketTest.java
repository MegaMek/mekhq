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
package mekhq.campaign.market.unitMarket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Tests the monthly unit market's offer count and the option that controls its regional 'Mek weights.
 */
class AtBMonthlyUnitMarketTest {
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 5 })
    void testAddOffersAddsOneUnitPerCount(int count) {
        // A count of exactly 1 must still offer a unit
        AtBMonthlyUnitMarket market = spy(new AtBMonthlyUnitMarket());
        doReturn(null).when(market)
              .addSingleUnit(any(Campaign.class), any(UnitMarketType.class), anyInt(), any(Faction.class), anyInt(),
                    anyCollection(), anyCollection(), anyInt());

        market.addOffers(mock(Campaign.class), count, UnitMarketType.OPEN, UnitType.MEK, mock(Faction.class),
              DragoonRating.DRAGOON_F.getRating(), 1);

        verify(market, times(count))
              .addSingleUnit(any(Campaign.class), any(UnitMarketType.class), anyInt(), any(Faction.class), anyInt(),
                    anyCollection(), anyCollection(), anyInt());
    }

    @Test
    void testNegativeCountAddsNoUnits() {
        AtBMonthlyUnitMarket market = spy(new AtBMonthlyUnitMarket());

        market.addOffers(mock(Campaign.class), -2, UnitMarketType.OPEN, UnitType.MEK, mock(Faction.class),
              DragoonRating.DRAGOON_F.getRating(), 1);

        verify(market, times(0))
              .addSingleUnit(any(Campaign.class), any(UnitMarketType.class), anyInt(), any(Faction.class), anyInt(),
                    anyCollection(), anyCollection(), anyInt());
    }

    @ParameterizedTest
    @CsvSource({ "true, false", "false, true" })
    void testWeightUsesUnitMarketRegionalOption(boolean unitMarketOption, boolean scenarioOption) {
        // The Unit Market has its own regional 'Mek weight option, separate from the one used for scenarios
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.UNIT_MARKET_REGIONAL_MEK_VARIATIONS, unitMarketOption);
        campaignOptions.set(CampaignOption.REGIONAL_MEK_VARIATIONS, scenarioOption);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        Faction faction = mock(Faction.class);

        try (MockedStatic<AtBStaticWeightGenerator> generator = mockStatic(AtBStaticWeightGenerator.class)) {
            new AtBMonthlyUnitMarket().generateWeight(campaign, UnitType.MEK, faction);

            generator.verify(() -> AtBStaticWeightGenerator.getRandomWeight(UnitType.MEK, faction, unitMarketOption));
        }
    }
}
