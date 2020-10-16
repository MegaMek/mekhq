/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum UnitMarketType {
    //region Enum Declarations
    OPEN("UnitMarketType.OPEN.text"),
    EMPLOYER("UnitMarketType.EMPLOYER.text"),
    MERCENARY("UnitMarketType.MERCENARY.text"),
    FACTORY("UnitMarketType.FACTORY.text"),
    BLACK_MARKET("UnitMarketType.BLACK_MARKET.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    UnitMarketType(String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region File IO
    public static UnitMarketType parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return EMPLOYER;
                case 2:
                    return MERCENARY;
                case 3:
                    return FACTORY;
                case 4:
                    return BLACK_MARKET;
                case 0:
                default:
                    return OPEN;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Failed to parse " + text + " into a UnitMarketType");

        return OPEN;
    }
    //endregion File IO

    @Override
    public String toString() {
        return name;
    }
}
