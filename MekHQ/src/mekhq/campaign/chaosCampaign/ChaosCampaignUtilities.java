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

import java.math.BigDecimal;

import mekhq.campaign.finances.Money;

public class ChaosCampaignUtilities {
    public static int SUPPORT_POINTS_TO_MONEY_CONVERSION = 10_000; // Chaos Campaign pg 28

    public static Money getMoneyFromChaosSupportPoints(int supportPoints) {
        return getMoneyFromChaosSupportPoints(supportPoints, true);
    }

    /**
     * Converts Chaos support points into a {@link Money} value.
     *
     * @param supportPoints  the support-point amount
     * @param convertToMoney when {@code true}, the support points are converted to C-bills at
     *                       {@link #SUPPORT_POINTS_TO_MONEY_CONVERSION} (the standard "BSP to BV" conversion); when
     *                       {@code false}, the support-point amount is taken verbatim as the money value (no
     *                       conversion), so pay is expressed in raw support points.
     *
     * @return the resulting {@link Money}
     */
    public static Money getMoneyFromChaosSupportPoints(int supportPoints, boolean convertToMoney) {
        if (!convertToMoney) {
            return Money.of(supportPoints);
        }
        return Money.of(supportPoints * SUPPORT_POINTS_TO_MONEY_CONVERSION);
    }

    public static int getChaosSupportPointsFromMoney(Money money) {
        Money reducedMoney = money.dividedBy(SUPPORT_POINTS_TO_MONEY_CONVERSION);
        BigDecimal reducedMoneyAmount = reducedMoney.getAmount();

        return reducedMoneyAmount
                     .min(BigDecimal.valueOf(Integer.MAX_VALUE))
                     .max(BigDecimal.valueOf(Integer.MIN_VALUE))
                     .intValue();
    }
}
