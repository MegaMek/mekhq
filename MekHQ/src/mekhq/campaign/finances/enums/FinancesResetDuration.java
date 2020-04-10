/*
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum FinancesResetDuration {
    //region Enum Declarations
    DAILY("FinancesResetDuration.DAILY.text", "FinancesResetDuration.DAILY.toolTipText"),
    WEEKLY("FinancesResetDuration.WEEKLY.text", "FinancesResetDuration.WEEKLY.toolTipText"),
    MONTHLY("FinancesResetDuration.MONTHLY.text", "FinancesResetDuration.MONTHLY.toolTipText"),
    SEMIANNUALLY("FinancesResetDuration.SEMIANNUALLY.text", "FinancesResetDuration.SEMIANNUALLY.toolTipText"),
    ANNUALLY("FinancesResetDuration.ANNUALLY.text", "FinancesResetDuration.ANNUALLY.toolTipText"),
    BIANNUALLY("FinancesResetDuration.BIANNUALLY.text", "FinancesResetDuration.BIANNUALLY.toolTipText"),
    QUINQUENNIALLY("FinancesResetDuration.QUINQUENNIALLY.text", "FinancesResetDuration.QUINQUENNIALLY.toolTipText"),
    DECENNIALLY("FinancesResetDuration.DECENNIALLY.text", "FinancesResetDuration.DECENNIALLY.toolTipText"),
    QUADRANSCENTENNIALLY("FinancesResetDuration.QUADRANSCENTENNIALLY.text", "FinancesResetDuration.QUADRANSCENTENNIALLY.toolTipText"),
    CENTENARY("FinancesResetDuration.CENTENARY.text", "FinancesResetDuration.CENTENARY.toolTipText"),
    SESQUICENTENNIALLY("FinancesResetDuration.SESQUICENTENNIALLY.text", "FinancesResetDuration.SESQUICENTENNIALLY.toolTipText"),
    NEVER("FinancesResetDuration.NEVER.text", "FinancesResetDuration.NEVER.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String typeName;
    private final String toolTipText;

    public final static FinancesResetDuration DEFAULT_TYPE = ANNUALLY;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FinancesResetDuration(String typeName, String toolTipText) {
        this.typeName = resources.getString(typeName);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    public String getTypeName() {
        return typeName;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
