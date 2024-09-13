/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum UnitMarketRarity {

    // region Enum Declarations
    VERY_RARE("UnitMarketRarity.VERY_RARE.name"),
    RARE("UnitMarketRarity.RARE.name"),
    UNCOMMON("UnitMarketRarity.UNCOMMON.name"),
    COMMON("UnitMarketRarity.COMMON.name"),
    VERY_COMMON("UnitMarketRarity.VERY_COMMON.name");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    UnitMarketRarity(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Getters

    public String getName() {
        return name;
    }
    // endregion Getters

    // region Boolean Comparison Methods

    public boolean isVeryRare() {
        return this == VERY_RARE;
    }

    public boolean isRare() {
        return this == RARE;
    }

    public boolean isUncommon() {
        return this == UNCOMMON;
    }

    public boolean isCommon() {
        return this == COMMON;
    }

    public boolean isVeryCommon() {
        return this == VERY_COMMON;
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    public static UnitMarketRarity parseFromString(final String text) {
        return switch (text) {
            case "0", "Very Rare" -> VERY_RARE;
            case "1", "Rare" -> RARE;
            case "2", "Uncommon" -> UNCOMMON;
            case "3", "Common" -> COMMON;
            case "4", "Very Common" -> VERY_COMMON;
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/market/enums/UnitMarketRarity.java/parseFromString: "
                            + text);
        };
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
