/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import org.apache.logging.log4j.LogManager;

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
    //endregion Variable Declarations

    //region Constructors
    UnitMarketType(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isBlackMarket() {
        return this == BLACK_MARKET;
    }
    //endregion Boolean Comparison Methods

    //region File IO
    public static UnitMarketType parseFromString(final String text) {
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

        LogManager.getLogger().error("Failed to parse " + text + " into a UnitMarketType");

        return OPEN;
    }
    //endregion File IO

    @Override
    public String toString() {
        return name;
    }
}
