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

import mekhq.MekHQ;
import mekhq.campaign.market.unitMarket.AbstractUnitMarket;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.market.unitMarket.DisabledUnitMarket;

import java.util.ResourceBundle;

public enum UnitMarketMethod {
    //region Enum Declarations
    NONE("UnitMarketMethod.NONE.text", "UnitMarketMethod.NONE.toolTipText"),
    ATB_MONTHLY("UnitMarketMethod.ATB_MONTHLY.text", "UnitMarketMethod.ATB_MONTHLY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    UnitMarketMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale());
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

    public boolean isAtBMonthly() {
        return this == ATB_MONTHLY;
    }
    //endregion Boolean Comparisons

    public AbstractUnitMarket getUnitMarket() {
        switch (this) {
            case ATB_MONTHLY:
                return new AtBMonthlyUnitMarket();
            case NONE:
            default:
                return new DisabledUnitMarket();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
