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
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.market.unitMarket.EmptyUnitMarket;

import java.util.ResourceBundle;

public enum UnitMarketMethod {
    //region Enum Declarations
    NONE("UnitMarketMethod.NONE.text", "UnitMarketMethod.NONE.toolTipText"),
    ATB_MONTHLY("UnitMarketMethod.ATB_MONTHLY.text", "UnitMarketMethod.ATB_MONTHLY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    UnitMarketMethod(final String name, final String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isNone() {
        return this == NONE;
    }
    //endregion Boolean Comparisons

    public AbstractUnitMarket getUnitMarket() {
        switch (this) {
            case ATB_MONTHLY:
                return new AtBMonthlyUnitMarket();
            case NONE:
            default:
                return new EmptyUnitMarket();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
