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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.Test;

/**
 * Tests the Chaos pay scheme's two scale-derived components: {@link ChaosContractDeterminationPay#getMonthlyPay} (500
 * support points per scale, scaled by the negotiated base-pay multiplier) and
 * {@link ChaosContractDeterminationPay#getCombatPay} (500 support points per scale). Both optionally convert support
 * points to C-bills at 10,000 each; the tests leave conversion off so the raw support-point arithmetic is visible.
 */
class ChaosContractDeterminationPayTest {
    private final ChaosContractDeterminationPay payScheme = new ChaosContractDeterminationPay();

    private static Campaign campaignWithConversion(boolean convertSupportPoints) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION)).thenReturn(convertSupportPoints);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    private static AbstractContract contract(int scale, double basePayMultiplier) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(scale);
        when(contract.getBasePayMultiplier()).thenReturn(basePayMultiplier);
        return contract;
    }

    @Test
    void monthlyPayIsScaleTimesTheMultiplierInSupportPoints() {
        // 500 * scale(2) * basePay(1.0) = 1000 support points, unconverted.
        assertEquals(Money.of(1_000),
              payScheme.getMonthlyPay(campaignWithConversion(false), contract(2, 1.0)));
    }

    @Test
    void monthlyPayAppliesTheNegotiatedBasePayMultiplier() {
        // 500 * 2 = 1000, scaled by 1.5 -> 1500 support points.
        assertEquals(Money.of(1_500),
              payScheme.getMonthlyPay(campaignWithConversion(false), contract(2, 1.5)));
    }

    @Test
    void monthlyPayConvertsSupportPointsToCBillsWhenEnabled() {
        // 1000 support points converted at 10,000 C-bills each.
        assertEquals(Money.of(10_000_000),
              payScheme.getMonthlyPay(campaignWithConversion(true), contract(2, 1.0)));
    }

    @Test
    void combatPayIsScaleInSupportPointsAndIgnoresTheBasePayMultiplier() {
        // 500 * scale(3) = 1500 support points; the base-pay multiplier does not affect combat pay.
        assertEquals(Money.of(1_500),
              payScheme.getCombatPay(campaignWithConversion(false), contract(3, 2.0)));
    }

    @Test
    void combatPayConvertsSupportPointsToCBillsWhenEnabled() {
        assertEquals(Money.of(15_000_000),
              payScheme.getCombatPay(campaignWithConversion(true), contract(3, 1.0)));
    }
}
