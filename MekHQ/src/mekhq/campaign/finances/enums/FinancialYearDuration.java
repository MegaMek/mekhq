/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ResourceBundle;

public enum FinancialYearDuration {
    //region Enum Declarations
    SEMIANNUAL("FinancialYearDuration.SEMIANNUAL.text", "FinancialYearDuration.SEMIANNUAL.toolTipText"),
    ANNUAL("FinancialYearDuration.ANNUAL.text", "FinancialYearDuration.ANNUAL.toolTipText"),
    BIENNIAL("FinancialYearDuration.BIENNIAL.text", "FinancialYearDuration.BIENNIAL.toolTipText"),
    QUINQUENNIAL("FinancialYearDuration.QUINQUENNIAL.text", "FinancialYearDuration.QUINQUENNIAL.toolTipText"),
    DECENNIAL("FinancialYearDuration.DECENNIAL.text", "FinancialYearDuration.DECENNIAL.toolTipText"),
    FOREVER("FinancialYearDuration.FOREVER.text", "FinancialYearDuration.FOREVER.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    FinancialYearDuration(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances", new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    public boolean isEndOfFinancialYear(final LocalDate today) {
        switch (this) {
            case SEMIANNUAL:
                return (today.getDayOfYear() == 1) || ((today.getMonthValue() == 7) && (today.getDayOfMonth() == 1));
            case BIENNIAL:
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

    /**
     * This is called to get the export after the financial year has concluded according to the
     * previous check returns true. Because of that, this is called with tomorrow's date.
     *
     * @param tomorrow tomorrow's date. Because of how this is passed in, we are provided that
     *                 instead of today
     * @return the filename to use for the date during financial export
     */
    public String getExportFilenameDateString(final LocalDate tomorrow) {
        final int year = tomorrow.getYear() - 1;
        switch (this) {
            case SEMIANNUAL:
                final boolean isStartOfYear = tomorrow.getDayOfYear() == 1;
                return isStartOfYear
                        ? String.format("%d %s - %s", year,
                                Month.JULY.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]", ""),
                                Month.DECEMBER.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]", ""))
                        : String.format("%d %s - %s", year + 1,
                                Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]", ""),
                                Month.JUNE.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]", ""));
            case BIENNIAL:
                return String.format("%d - %d", year - 1, year);
            case QUINQUENNIAL:
                return String.format("%d - %d", year - 4, year);
            case DECENNIAL:
                return String.format("%d - %d", year - 9, year);
            case FOREVER:
            case ANNUAL:
            default:
                return Integer.toString(year);
        }
    }

    //region File I/O
    /**
     * This allows for the legacy parsing method of financial durations, outdated in 0.49.X
     */
    public static FinancialYearDuration parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        // Legacy Parsing format
        if ("BIANNUAL".equals(text)) {
            return BIENNIAL;
        }

        MekHQ.getLogger().error("Failed to parse the FinancialYearDuration from text " + text + ", returning ANNUAL.");

        return ANNUAL;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
