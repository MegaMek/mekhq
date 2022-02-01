/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ResourceBundle;

public enum FinancialTerm {
    //region Enum Declarations
    BIWEEKLY("FinancialTerm.BIWEEKLY.text", "FinancialTerm.BIWEEKLY.toolTipText"),
    MONTHLY("FinancialTerm.MONTHLY.text", "FinancialTerm.MONTHLY.toolTipText"),
    QUARTERLY("FinancialTerm.QUARTERLY.text", "FinancialTerm.QUARTERLY.toolTipText"),
    ANNUALLY("FinancialTerm.ANNUALLY.text", "FinancialTerm.ANNUALLY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    FinancialTerm(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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
    public boolean isBiweekly() {
        return this == BIWEEKLY;
    }

    public boolean isMonthly() {
        return this == MONTHLY;
    }

    public boolean isQuarterly() {
        return this == QUARTERLY;
    }

    public boolean isAnnually() {
        return this == ANNUALLY;
    }
    //endregion Boolean Comparison Methods

    /**
     * @param origin the origin date, which will normally (but not always) be the current date
     * @return the next valid date for the financial term, with a built-in grace period to line up
     * everything to the first day of a financial setup (Monday, First of the Month, First of the Year)
     */
    public LocalDate nextValidDate(final LocalDate origin) {
        return nextValidDate(origin.minusDays(1), origin);
    }

    /**
     * @param yesterday the day before the origin
     * @param origin the origin date, which will normally (but not always) be the current date
     * @return the next valid date for the financial term, with a built-in grace period to line up
     * everything to the first day of a financial setup (Monday, First of the Month, First of the Year)
     */
    public LocalDate nextValidDate(final LocalDate yesterday, final LocalDate origin) {
        switch (this) {
            case BIWEEKLY:
                // First, find the next or same Monday
                var date = origin.with(WeekFields.of(MekHQ.getMekHQOptions().getDateLocale()).dayOfWeek(), 1);
                return date.plusWeeks(
                        (date.get(WeekFields.of(MekHQ.getMekHQOptions().getDateLocale()).weekOfYear()) % 2 == 0)
                                ? 2 : 3);
            case MONTHLY:
                return ((origin.getDayOfMonth() == 1) ? origin : origin.with(TemporalAdjusters.firstDayOfNextMonth()))
                        .plusMonths(1);
            case QUARTERLY:
                //
                // Finally, adjust to the first day of the current quarter
                return (origin.get(IsoFields.QUARTER_OF_YEAR) != yesterday.get(IsoFields.QUARTER_OF_YEAR))
                        ? origin.plusMonths(3) : origin.with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(6);
            case ANNUALLY:
            default:
                return ((origin.getDayOfYear() == 1) ? origin : origin.with(TemporalAdjusters.firstDayOfNextYear()))
                        .plusYears(1);
        }
    }

    public boolean endsToday(final LocalDate today) {
        switch (this) {
            case BIWEEKLY:
                return (today.getDayOfWeek() == DayOfWeek.MONDAY)
                        && (today.get(WeekFields.of(MekHQ.getMekHQOptions().getDateLocale()).weekOfYear()) % 2 == 0);
            case MONTHLY:
                return ((origin.getDayOfMonth() == 1) ? origin : origin.with(TemporalAdjusters.firstDayOfNextMonth()))
                        .plusMonths(1);
            case QUARTERLY:
                return (((origin.getDayOfMonth() == 1) && (((origin.getMonth().ordinal() - 1) % 3) == 0))
                        ? origin : origin.with(TemporalAdjusters.firstDayOfNextMonth())).plusMonths(3);
            case ANNUALLY:
            default:
                return ((origin.getDayOfYear() == 1) ? origin : origin.with(TemporalAdjusters.firstDayOfNextYear()))
                        .plusYears(1);
        }
    }

    public double determineYearlyDenominator() {
        switch (this) {
            case BIWEEKLY:
                return 26;
            case MONTHLY:
                return 12;
            case QUARTERLY:
                return 4;
            case ANNUALLY:
            default:
                return 1;
        }
    }

    //region File I/O
    public static FinancialTerm parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return BIWEEKLY;
                case 1:
                    return MONTHLY;
                case 2:
                    return QUARTERLY;
                case 3:
                    return ANNUALLY;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        LogManager.getLogger().error("Failed to parse the FinancialTerm from text " + text + ", returning ANNUALLY.");

        return ANNUALLY;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
