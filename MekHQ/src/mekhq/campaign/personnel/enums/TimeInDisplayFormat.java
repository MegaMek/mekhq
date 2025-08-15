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
package mekhq.campaign.personnel.enums;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

import mekhq.MekHQ;

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
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.displayFormat = resources.getString(displayFormat);
    }
    //endregion Constructors

    //region Getters
    public String getDisplayFormat() {
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
            default:
                return String.format(getDisplayFormat(),
                      Math.toIntExact(ChronoUnit.YEARS.between(initialDate, today)));
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
