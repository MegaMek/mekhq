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

import java.math.RoundingMode;

import mekhq.campaign.finances.Money;

public class SalvageUtilities {
    /**
     * Computes the player's share of the total salvage value as an integer percentage, using
     * {@link RoundingMode#CEILING} (i.e. any fractional percentage rounds up to the next whole percent).
     *
     * <p>Rounding up is intentional from a gameplay standpoint: the percentage is compared against the contract's
     * salvage cap, and a true value of e.g. 50.001% against a 50% cap is a breach and must be surfaced as such. It also
     * fixes the truncation artifacts that previously could cause the displayed value to shift by a full percentage
     * point after a small change to the salvage assignment (see issue #5683).</p>
     *
     * @param playerShare   the salvage value assigned to the player (mercs)
     * @param employerShare the salvage value assigned to the employer
     *
     * @return integer percentage in the range {@code [0, 100]}, or {@code 0} if there is no salvage to split
     */
    public static int calculateSalvagePercentage(Money playerShare, Money employerShare) {
        Money total = playerShare.plus(employerShare);
        if (!total.isPositive()) {
            return 0;
        }
        return playerShare.multipliedBy(100)
                     .getAmount()
                     .divide(total.getAmount(), 0, RoundingMode.CEILING)
                     .intValue();
    }
}
