/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum FinancialTerm {
    // region Enum Declarations
    BIWEEKLY("FinancialTerm.BIWEEKLY.text", "FinancialTerm.BIWEEKLY.toolTipText"),
    MONTHLY("FinancialTerm.MONTHLY.text", "FinancialTerm.MONTHLY.toolTipText"),
    QUARTERLY("FinancialTerm.QUARTERLY.text", "FinancialTerm.QUARTERLY.toolTipText"),
    SEMIANNUALLY("FinancialTerm.SEMIANNUALLY.text", "FinancialTerm.SEMIANNUALLY.toolTipText"),
    ANNUALLY("FinancialTerm.ANNUALLY.text", "FinancialTerm.ANNUALLY.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    FinancialTerm(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isBiweekly() {
        return this == BIWEEKLY;
    }

    public boolean isMonthly() {
        return this == MONTHLY;
    }

    public boolean isQuarterly() {
        return this == QUARTERLY;
    }

    public boolean isSemiannually() {
        return this == SEMIANNUALLY;
    }

    public boolean isAnnually() {
        return this == ANNUALLY;
    }
    // endregion Boolean Comparison Methods

    /**
     * @param today the origin date, which will normally (but not always) be the
     *              current date
     * @return the next valid date for the financial term, with a built-in grace
     *         period to line up
     *         everything to the first day of a financial setup (Monday, First of
     *         the Month, First of the Year)
     */
    public LocalDate nextValidDate(final LocalDate today) {
        return nextValidDate(today.minusDays(1), today);
    }

    /**
     * @param yesterday the day before today
     * @param today     the origin date, which will normally (but not always) be the
     *                  current date
     * @return the next valid date for the financial term, with a built-in grace
     *         period to line up
     *         everything to the first day of a financial setup (Monday, First of
     *         the Month, First of the Year)
     */
    public LocalDate nextValidDate(final LocalDate yesterday, final LocalDate today) {
        switch (this) {
            case BIWEEKLY:
                // First, find the first day of the current week
                final LocalDate date = today.with(WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek(), 1);
                // Second, add two weeks to the date if this is an even week, or three if it is
                // not
                return date.plusWeeks(
                        ((date.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()) % 2) == 0)
                                ? 2
                                : 3);
            case MONTHLY:
                // First, determine if the term would end today. If it would, use today or
                // otherwise
                // adjust to the first day of the next month.
                // Second, add a month to the determined date
                return (endsToday(yesterday, today) ? today : today.with(TemporalAdjusters.firstDayOfNextMonth()))
                        .plusMonths(1);
            case QUARTERLY:
                // Determine if the term would end today
                // If it is, return today plus three months
                // Otherwise, then adjust to the first day of the current quarter before adding
                // six
                // months
                return endsToday(yesterday, today) ? today.plusMonths(3)
                        : today.with(IsoFields.DAY_OF_QUARTER, 1).plusMonths(6);
            case SEMIANNUALLY:
                // Determine if the term would end today
                // If it would, return today plus six months
                // Otherwise, then adjust to the first day of the current quarter before adding
                // twelve months if today is in an even quarter or nine months otherwise
                return endsToday(yesterday, today) ? today.plusMonths(6)
                        : today.with(IsoFields.DAY_OF_QUARTER, 1)
                                .plusMonths(((today.get(IsoFields.QUARTER_OF_YEAR) % 2) == 1) ? 12 : 9);
            case ANNUALLY:
            default:
                // First, use today if the term would end today or otherwise adjust to the first
                // day
                // of the next year.
                // Second, add a year to the determined date
                return (endsToday(yesterday, today) ? today : today.with(TemporalAdjusters.firstDayOfNextYear()))
                        .plusYears(1);
        }
    }

    /**
     * @param yesterday the day before today
     * @param today     the origin date, which will normally (but not always) be the
     *                  current date
     * @return if the current financial term ends today
     */
    public boolean endsToday(final LocalDate yesterday, final LocalDate today) {
        switch (this) {
            case BIWEEKLY:
                // Start of the week, on an even week
                return (today.get(WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek()) == 1)
                        && ((today.get(WeekFields.of(DayOfWeek.MONDAY, 7).weekOfYear()) % 2) == 0);
            case MONTHLY:
                return today.getMonth() != yesterday.getMonth();
            case QUARTERLY:
                return today.get(IsoFields.QUARTER_OF_YEAR) != yesterday.get(IsoFields.QUARTER_OF_YEAR);
            case SEMIANNUALLY:
                return (today.get(IsoFields.QUARTER_OF_YEAR) != yesterday.get(IsoFields.QUARTER_OF_YEAR))
                        && ((today.get(IsoFields.QUARTER_OF_YEAR) % 2) == 1);
            case ANNUALLY:
            default:
                return today.getYear() != yesterday.getYear();
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
            case SEMIANNUALLY:
                return 2;
            case ANNUALLY:
            default:
                return 1;
        }
    }

    // region File I/O
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

        MMLogger.create(FinancialTerm.class)
                .error("Unable to parse " + text + " into a FinancialTerm. Returning ANNUALLY.");
        return ANNUALLY;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
