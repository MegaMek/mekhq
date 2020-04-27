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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

/**
 * This enum is used to format the Time in Service and Time in Rank outputs
 */
public enum TimeInDisplayFormat {
    //region Enum Declarations
    DAYS("TimeInDisplayFormat.DAYS.text", "TimeInDisplayFormat.DAYS.displayFormat"),
    MONTHS("TimeInDisplayFormat.MONTHS.text", "TimeInDisplayFormat.MONTHS.displayFormat"),
    MONTHS_YEARS("TimeInDisplayFormat.MONTHS_YEARS.text", "TimeInDisplayFormat.MONTHS_YEARS.displayFormat"),
    YEARS("TimeInDisplayFormat.YEARS.text", "TimeInDisplayFormat.YEARS.displayFormat");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String formatName;
    private final String displayFormat;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    TimeInDisplayFormat(String formatName, String displayFormat) {
        this.formatName = resources.getString(formatName);
        this.displayFormat = resources.getString(displayFormat);
    }
    //endregion Constructors

    public String getDisplayFormattedOutput(LocalDate initialDate, LocalDate today) {
        switch (this) {
            case DAYS:
                break;
            case MONTHS:
                int difference = Math.toIntExact(ChronoUnit.MONTHS.between(initialDate, today));
        }
        return "";
    }

    @Override
    public String toString() {
        return formatName;
    }
}
