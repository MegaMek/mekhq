/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum UnitMarketRarity {

    // region Enum Declarations
    VERY_RARE("UnitMarketRarity.VERY_RARE.name", 0),
    RARE("UnitMarketRarity.RARE.name", 1),
    UNCOMMON("UnitMarketRarity.UNCOMMON.name", 2),
    COMMON("UnitMarketRarity.COMMON.name", 3),
    VERY_COMMON("UnitMarketRarity.VERY_COMMON.name", 4),
    UBIQUITOUS("UnitMarketRarity.UBIQUITOUS.name", 10),
    MYTHIC("UnitMarketRarity.MYTHIC.name", -1);
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final int rarityValue;
    // endregion Variable Declarations

    // region Constructors
    UnitMarketRarity(final String name, final int rarityValue) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.rarityValue = rarityValue;
    }
    // endregion Constructors

    // region Getters
    public String getName() {
        return name;
    }

    public int getRarityValue() {
        return rarityValue;
    }
    // endregion Getters

    // region File I/O
    public static UnitMarketRarity parseFromString(final String text) {
        return switch (text) {
            case "0", "Very Rare" -> VERY_RARE;
            case "1", "Rare" -> RARE;
            case "2", "Uncommon" -> UNCOMMON;
            case "3", "Common" -> COMMON;
            case "4", "Very Common" -> VERY_COMMON;
            case "5", "Ubiquitous" -> UBIQUITOUS;
            case "6", "Mythic" -> MYTHIC;
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/market/enums/UnitMarketRarity.java/fromString: " + text);
        };
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
