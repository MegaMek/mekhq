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

import java.time.LocalDate;
import java.util.ResourceBundle;

public enum FinancialYearDuration {
    //region Enum Declarations
    SEMIANNUAL("FinancialYearDuration.SEMIANNUAL.text", "FinancialYearDuration.SEMIANNUAL.toolTipText"),
    ANNUAL("FinancialYearDuration.ANNUAL.text", "FinancialYearDuration.ANNUAL.toolTipText"),
    BIANNUAL("FinancialYearDuration.BIANNUAL.text", "FinancialYearDuration.BIANNUAL.toolTipText"),
    QUINQUENNIAL("FinancialYearDuration.QUINQUENNIAL.text", "FinancialYearDuration.QUINQUENNIAL.toolTipText"),
    DECENNIAL("FinancialYearDuration.DECENNIAL.text", "FinancialYearDuration.DECENNIAL.toolTipText"),
    FOREVER("FinancialYearDuration.FOREVER.text", "FinancialYearDuration.FOREVER.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String typeName;
    private final String toolTipText;

    public final static FinancialYearDuration DEFAULT_TYPE = ANNUAL;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FinancialYearDuration(String typeName, String toolTipText) {
        this.typeName = resources.getString(typeName);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isEndOfFinancialYear(LocalDate today) {
        switch (this) {
            case SEMIANNUAL:
                return (today.getDayOfYear() == 1) || (today.getDayOfYear() == 183);
            case BIANNUAL:
                return (today.getDayOfYear() == 1) && (today.getYear() % 2 == 0);
            case QUINQUENNIAL:
                return (today.getDayOfYear() == 1) && (today.getYear() % 5 == 0);
            case DECENNIAL:
                return (today.getDayOfYear() == 1) && (today.getYear() % 10 == 0);
            case FOREVER:
                return false;
            case ANNUAL:
            default:
                return (today.getDayOfYear() == 1);
        }
    }

    @Override
    public String toString() {
        return typeName;
    }
}
