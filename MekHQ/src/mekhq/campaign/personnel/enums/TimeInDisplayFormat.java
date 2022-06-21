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
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

/**
 * This enum is used to format the Time in Service and Time in Rank outputs
 */
public enum TimeInDisplayFormat {
    //region Enum Declarations
    DAYS("TimeInDisplayFormat.DAYS.text", "TimeInDisplayFormat.DAYS.displayFormat"),
    WEEKS("TimeInDisplayFormat.WEEKS.text", "TimeInDisplayFormat.WEEKS.displayFormat"),
    MONTHS("TimeInDisplayFormat.MONTHS.text", "TimeInDisplayFormat.MONTHS.displayFormat"),
    MONTHS_YEARS("TimeInDisplayFormat.MONTHS_YEARS.text", "TimeInDisplayFormat.MONTHS_YEARS.displayFormat"),
    YEARS("TimeInDisplayFormat.YEARS.text", "TimeInDisplayFormat.YEARS.displayFormat");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String displayFormat;
    //endregion Variable Declarations

    //region Constructors
    TimeInDisplayFormat(final String name, final String displayFormat) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
        this.displayFormat = resources.getString(displayFormat);
    }
    //endregion Constructors

    //region Getters
    private String getDisplayFormat() {
        return displayFormat;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isDays() {
        return this == DAYS;
    }

    public boolean isWeeks() {
        return this == WEEKS;
    }

    public boolean isMonths() {
        return this == MONTHS;
    }

    public boolean isMonthsYears() {
        return this == MONTHS_YEARS;
    }

    public boolean isYears() {
        return this == YEARS;
    }
    //endregion Boolean Comparison Methods

    public String getDisplayFormattedOutput(final LocalDate initialDate, final LocalDate today) {
        switch (this) {
            case DAYS:
                return String.format(getDisplayFormat(),
                        Math.toIntExact(ChronoUnit.DAYS.between(initialDate, today)));
            case WEEKS:
                return String.format(getDisplayFormat(),
                        Math.toIntExact(ChronoUnit.WEEKS.between(initialDate, today)));
            case MONTHS:
                return String.format(getDisplayFormat(),
                        Math.toIntExact(ChronoUnit.MONTHS.between(initialDate, today)));
            case MONTHS_YEARS:
                final Period period = Period.between(initialDate, today);
                return String.format(getDisplayFormat(), period.getMonths(), period.getYears());
            case YEARS:
                return String.format(getDisplayFormat(),
                        Math.toIntExact(ChronoUnit.YEARS.between(initialDate, today)));
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
