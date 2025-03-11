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
 */
package mekhq.campaign.finances.enums;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum FinancialYearDuration {
    // region Enum Declarations
    SEMIANNUAL("FinancialYearDuration.SEMIANNUAL.text", "FinancialYearDuration.SEMIANNUAL.toolTipText"),
    ANNUAL("FinancialYearDuration.ANNUAL.text", "FinancialYearDuration.ANNUAL.toolTipText"),
    BIENNIAL("FinancialYearDuration.BIENNIAL.text", "FinancialYearDuration.BIENNIAL.toolTipText"),
    QUINQUENNIAL("FinancialYearDuration.QUINQUENNIAL.text", "FinancialYearDuration.QUINQUENNIAL.toolTipText"),
    DECENNIAL("FinancialYearDuration.DECENNIAL.text", "FinancialYearDuration.DECENNIAL.toolTipText"),
    FOREVER("FinancialYearDuration.FOREVER.text", "FinancialYearDuration.FOREVER.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    FinancialYearDuration(final String name, final String toolTipText) {
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
    public boolean isSemiannual() {
        return this == SEMIANNUAL;
    }

    public boolean isAnnual() {
        return this == ANNUAL;
    }

    public boolean isBiennial() {
        return this == BIENNIAL;
    }

    public boolean isQuinquennial() {
        return this == QUINQUENNIAL;
    }

    public boolean isDecennial() {
        return this == DECENNIAL;
    }

    public boolean isForever() {
        return this == FOREVER;
    }
    // endregion Boolean Comparison Methods

    public boolean isEndOfFinancialYear(final LocalDate today) {
        switch (this) {
            case SEMIANNUAL:
                return (today.getDayOfYear() == 1)
                        || ((today.getMonthValue() == 7) && (today.getDayOfMonth() == 1));
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
                return today.getDayOfYear() == 1;
        }
    }

    /**
     * This is called to get the export after the financial year has concluded
     * according to the
     * previous check returns true. Because of that, this is called with tomorrow's
     * date.
     *
     * @param tomorrow tomorrow's date. Because of how this is passed in, we are
     *                 provided that
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
                                Month.DECEMBER.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]",
                                        ""))
                        : String.format("%d %s - %s", year + 1,
                                Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replaceAll("[.]",
                                        ""),
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

    // region File I/O
    /**
     * This allows for the legacy parsing method of financial durations, outdated in
     * 0.49.X
     */
    public static FinancialYearDuration parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
            MMLogger.create(FinancialYearDuration.class)
                    .error(ignored, "Unable to parse " + text + " into a FinancialYearDuration. Returning ANNUAL.");
            return ANNUAL;

        }

    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
