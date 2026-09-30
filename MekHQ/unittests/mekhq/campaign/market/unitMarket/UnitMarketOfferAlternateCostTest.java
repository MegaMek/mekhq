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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the unit market's pricing under "Alternate Unit Cost": offers are priced from the unit's catalogue Battle
 * Value in support points rather than its construction cost, with the market percentage and tech-base multipliers
 * still applied.
 */
class UnitMarketOfferAlternateCostTest {
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;

    private CampaignOptions campaignOptions;
    private MekSummary summary;
    private Entity entity;

    @BeforeEach
    void setUp() {
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, true);
        campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
        campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.0);
        campaignOptions.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, 2.0);
        campaignOptions.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, 1.5);

        summary = mock(MekSummary.class);
        when(summary.getBV()).thenReturn(600);
        when(summary.getCost()).thenReturn(4_000_000L);

        entity = mock(Entity.class);
    }

    private UnitMarketOffer offer(int percent) {
        UnitMarketOffer offer = spy(new UnitMarketOffer(UnitMarketType.OPEN, UnitType.MEK, summary, percent, 0,
              campaignOptions));
        doReturn(entity).when(offer).getEntity();
        return offer;
    }

    @Test
    void offerIsPricedFromBattleValue() {
        // BV 600 -> 600 SP -> 6,000,000 C-bills at 100%
        assertEquals(Money.of(600 * SUPPORT_POINTS_TO_C_BILLS), offer(100).getPrice());
    }

    @ParameterizedTest
    @CsvSource({ "85, 5100000", "100, 6000000", "115, 6900000" })
    void marketPercentageStillApplies(int percent, int expected) {
        assertEquals(Money.of(expected), offer(percent).getPrice());
    }

    @Test
    void offerStaysInSupportPointsWithoutTheConversion() {
        campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);

        assertEquals(Money.of(600), offer(100).getPrice());
    }

    @Test
    void clanMultiplierStillApplies() {
        when(entity.isClan()).thenReturn(true);

        assertEquals(Money.of(12_000_000), offer(100).getPrice());
    }

    @Test
    void mixedTechMultiplierStillApplies() {
        when(entity.isClan()).thenReturn(true);
        when(entity.isMixedTech()).thenReturn(true);

        assertEquals(Money.of(9_000_000), offer(100).getPrice());
    }

    @Test
    void constructionCostIsNotUsed() {
        offer(100).getPrice();

        verify(summary, never()).getCost();
    }

    @Test
    void optionOffKeepsConstructionCostPricing() {
        campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);

        assertEquals(Money.of(4_000_000), offer(100).getPrice());
        verify(summary, never()).getBV();
    }

    /** An offer whose unit file can't be loaded is still priced, without a tech-base multiplier. */
    @Test
    void unloadableUnitIsPricedWithoutTheTechMultiplier() {
        UnitMarketOffer offer = offer(100);
        doReturn(null).when(offer).getEntity();

        assertEquals(Money.of(600 * SUPPORT_POINTS_TO_C_BILLS), offer.getPrice());
    }

    @Test
    void largeVesselPricesDoNotOverflow() {
        when(summary.getBV()).thenReturn(1_500_000);

        assertEquals(Money.of(15_000_000_000.0), offer(100).getPrice());
    }
}
