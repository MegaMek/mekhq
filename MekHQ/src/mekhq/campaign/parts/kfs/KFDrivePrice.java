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
package mekhq.campaign.parts.kfs;

import megamek.common.units.Jumpship;
import mekhq.campaign.finances.Money;

/**
 * The price of a K-F drive part on its ship. A compact core (WarShips) costs five times as much and a lithium-fusion
 * battery three times as much, so a large WarShip's drive parts run to billions of C-bills; the sums are done in
 * {@code double}, as MegaMek's WarShip cost calculator does, so they cannot overflow.
 */
final class KFDrivePrice {
    private static final double COMPACT_CORE_MULTIPLIER = 5.0;
    private static final double LITHIUM_FUSION_MULTIPLIER = 3.0;

    private KFDrivePrice() {}

    /**
     * @param jumpship  the ship the part is on
     * @param basePrice the part's price for a standard core without a lithium-fusion battery
     *
     * @return the part's price on this ship
     */
    static Money onShip(Jumpship jumpship, double basePrice) {
        double price = basePrice;
        if (jumpship.getDriveCoreType() == Jumpship.DRIVE_CORE_COMPACT) {
            price *= COMPACT_CORE_MULTIPLIER;
        }
        if (jumpship.hasLF()) {
            price *= LITHIUM_FUSION_MULTIPLIER;
        }
        return Money.of(price);
    }
}
