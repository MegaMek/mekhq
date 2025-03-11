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
