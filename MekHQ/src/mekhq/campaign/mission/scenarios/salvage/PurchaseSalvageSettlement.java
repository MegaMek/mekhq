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
package mekhq.campaign.mission.scenarios.salvage;

import mekhq.campaign.finances.Money;

/**
 * A {@link SalvageSettlement} for salvage purchases: there is no cap on salvage. For each wreck, the player is paid
 * their share of its value in cash, unless they buy the unit by paying the employer their share.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PurchaseSalvageSettlement extends SalvageSettlement {
    PurchaseSalvageSettlement(double playerShare) {
        super(playerShare);
    }

    @Override
    public boolean canKeepSalvage() {
        return true;
    }

    @Override
    public boolean isKeptSalvageBought() {
        return true;
    }

    @Override
    public Money getPurchaseCost(Money keptSalvageValue) {
        // Subtracting the player's share, rather than multiplying by (1 - share), keeps the player's part of bought
        // salvage exactly equal to their salvage rights allowance; e.g. 1 - 0.07 is 0.9299999999999999 as a double
        return keptSalvageValue.minus(keptSalvageValue.multipliedBy(getPlayerShare()));
    }

    @Override
    public Money getCashShare(Money employerSalvageValue) {
        return employerSalvageValue.multipliedBy(getPlayerShare());
    }
}
