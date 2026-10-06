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
package mekhq.campaign.chaosCampaign;

import static mekhq.campaign.chaosCampaign.ChaosCampaignUtilities.SUPPORT_POINTS_TO_MONEY_CONVERSION;
import static mekhq.campaign.chaosCampaign.ChaosCampaignUtilities.getChaosSupportPointsFromMoney;
import static mekhq.campaign.chaosCampaign.ChaosCampaignUtilities.getMoneyFromChaosSupportPoints;
import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the support-point/money conversions in {@link ChaosCampaignUtilities}.
 */
class ChaosCampaignUtilitiesTest {
    // region getMoneyFromChaosSupportPoints
    @Test
    void getMoneyFromChaosSupportPoints_multipliesByConversionRate() {
        assertEquals(Money.of(5L * SUPPORT_POINTS_TO_MONEY_CONVERSION), getMoneyFromChaosSupportPoints(5));
    }

    @Test
    void getMoneyFromChaosSupportPoints_zeroPointsIsZeroMoney() {
        assertEquals(Money.zero(), getMoneyFromChaosSupportPoints(0));
    }

    @Test
    void getMoneyFromChaosSupportPoints_negativePointsIsNegativeMoney() {
        assertEquals(Money.of(-3L * SUPPORT_POINTS_TO_MONEY_CONVERSION), getMoneyFromChaosSupportPoints(-3));
    }
    // endregion getMoneyFromChaosSupportPoints

    // region getChaosSupportPointsFromMoney
    @Test
    void getChaosSupportPointsFromMoney_exactMultipleDividesCleanly() {
        assertEquals(5, getChaosSupportPointsFromMoney(Money.of(5L * SUPPORT_POINTS_TO_MONEY_CONVERSION)));
    }

    @Test
    void getChaosSupportPointsFromMoney_zeroMoneyIsZeroPoints() {
        assertEquals(0, getChaosSupportPointsFromMoney(Money.zero()));
    }

    @Test
    void getChaosSupportPointsFromMoney_negativeMoneyIsNegativePoints() {
        assertEquals(-4, getChaosSupportPointsFromMoney(Money.of(-4L * SUPPORT_POINTS_TO_MONEY_CONVERSION)));
    }

    @Test
    void getChaosSupportPointsFromMoney_roundsToNearestPoint() {
        // C-Bills have no fractional digits, so the division rounds to a whole support point.
        assertEquals(3, getChaosSupportPointsFromMoney(Money.of(2.6 * SUPPORT_POINTS_TO_MONEY_CONVERSION)));
        assertEquals(2, getChaosSupportPointsFromMoney(Money.of(2.4 * SUPPORT_POINTS_TO_MONEY_CONVERSION)));
    }

    @Test
    void getChaosSupportPointsFromMoney_roundTripPreservesValue() {
        assertEquals(7, getChaosSupportPointsFromMoney(getMoneyFromChaosSupportPoints(7)));
    }

    @Test
    void getChaosSupportPointsFromMoney_clampsAboveIntegerMaxValue() {
        // 1e15 / 10,000 = 1e11, well beyond Integer.MAX_VALUE, so the result is clamped.
        assertEquals(Integer.MAX_VALUE, getChaosSupportPointsFromMoney(Money.of(1e15)));
    }

    @Test
    void getChaosSupportPointsFromMoney_clampsBelowIntegerMinValue() {
        assertEquals(Integer.MIN_VALUE, getChaosSupportPointsFromMoney(Money.of(-1e15)));
    }
    // endregion getChaosSupportPointsFromMoney
}
