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
import mekhq.campaign.market.*;

import java.util.ResourceBundle;

public enum ContractMarketMethod {
    //region Enum Declarations
    NONE("ContractMarketMethod.NONE.text", "ContractMarketMethod.NONE.toolTipText"),
    ATB_MONTHLY("ContractMarketMethod.ATB_MONTHLY.text", "ContractMarketMethod.ATB_MONTHLY.toolTipText"),
    CAM_OPS("ContractMarketMethod.CAM_OPS.text", "ContractMarketMethod.CAM_OPS.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    ContractMarketMethod(final String name, final String toolTipText) {
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

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isAtBMonthly() {
        return this == ATB_MONTHLY;
    }

    public boolean isCamOps() {
        return this == CAM_OPS;
    }
    //endregion Boolean Comparison Methods

    public AbstractContractMarket getContractMarket() {
        return switch (this) {
            case ATB_MONTHLY -> new AtbMonthlyContractMarket();
            case CAM_OPS -> new CamOpsContractMarket();
            case NONE -> new DisabledContractMarket();
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
