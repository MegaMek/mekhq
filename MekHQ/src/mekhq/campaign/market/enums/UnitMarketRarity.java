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

import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum UnitMarketRarity {

    //region Enum Declarations
    VERY_RARE("UnitMarketRarity.VERY_RARE.name"),
    RARE("UnitMarketRarity.RARE.name"),
    UNCOMMON("UnitMarketRarity.UNCOMMON.name"),
    COMMON("UnitMarketRarity.COMMON.name"),
    VERY_COMMON("UnitMarketRarity.VERY_COMMON.name");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    UnitMarketRarity(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Getters
    @SuppressWarnings(value = "unused")
    public String getName() {
        return name;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @SuppressWarnings(value = "unused")
    public boolean isVeryRare() {
        return this == VERY_RARE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isRare() {
        return this == RARE;
    }

    @SuppressWarnings(value = "unused")
    public boolean isUncommon() {
        return this == UNCOMMON;
    }

    @SuppressWarnings(value = "unused")
    public boolean isCommon() {
        return this == COMMON;
    }

    @SuppressWarnings(value = "unused")
    public boolean isVeryCommon() {
        return this == VERY_COMMON;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @SuppressWarnings(value = "unused")
    public static UnitMarketRarity parseFromString(final String text) {
        return switch (text) {
            case "0", "Very Rare" -> VERY_RARE;
            case "1", "Rare" -> RARE;
            case "2", "Uncommon" -> UNCOMMON;
            case "3", "Common" -> COMMON;
            case "4", "Very Common" -> VERY_COMMON;
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/market/enums/UnitMarketRarity.java/parseFromString: "
                    + text);
        };
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
