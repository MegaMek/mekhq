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
import mekhq.campaign.market.contractMarket.AbstractContractMarket;
import mekhq.campaign.market.contractMarket.AtbMonthlyContractMarket;
import mekhq.campaign.market.contractMarket.CamOpsContractMarket;
import mekhq.campaign.market.contractMarket.DisabledContractMarket;

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
