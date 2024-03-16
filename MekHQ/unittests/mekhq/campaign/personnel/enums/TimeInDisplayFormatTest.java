/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeInDisplayFormatTest {
    //region Variable Declarations
    private static final TimeInDisplayFormat[] formats = TimeInDisplayFormat.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsDays() {
        for (final TimeInDisplayFormat format : formats) {
            if (format == TimeInDisplayFormat.DAYS) {
                assertTrue(format.isDays());
            } else {
                assertFalse(format.isDays());
            }
        }
    }

    @Test
    public void testIsWeeks() {
        for (final TimeInDisplayFormat format : formats) {
            if (format == TimeInDisplayFormat.WEEKS) {
                assertTrue(format.isWeeks());
            } else {
                assertFalse(format.isWeeks());
            }
        }
    }

    @Test
    public void testIsMonths() {
        for (final TimeInDisplayFormat format : formats) {
            if (format == TimeInDisplayFormat.MONTHS) {
                assertTrue(format.isMonths());
            } else {
                assertFalse(format.isMonths());
            }
        }
    }

    @Test
    public void testIsMonthsYears() {
        for (final TimeInDisplayFormat format : formats) {
            if (format == TimeInDisplayFormat.MONTHS_YEARS) {
                assertTrue(format.isMonthsYears());
            } else {
                assertFalse(format.isMonthsYears());
            }
        }
    }

    @Test
    public void testIsYears() {
        for (final TimeInDisplayFormat format : formats) {
            if (format == TimeInDisplayFormat.YEARS) {
                assertTrue(format.isYears());
            } else {
                assertFalse(format.isYears());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetDisplayFormattedOutput() {
        // Days
        assertEquals(String.format(TimeInDisplayFormat.DAYS.getDisplayFormat(), -1),
                TimeInDisplayFormat.DAYS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 2), LocalDate.of(3025, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.DAYS.getDisplayFormat(), 0),
                TimeInDisplayFormat.DAYS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.DAYS.getDisplayFormat(), 1),
                TimeInDisplayFormat.DAYS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 2)));
        assertEquals(String.format(TimeInDisplayFormat.DAYS.getDisplayFormat(), 31),
                TimeInDisplayFormat.DAYS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 2, 1)));
        assertEquals(String.format(TimeInDisplayFormat.DAYS.getDisplayFormat(), 365),
                TimeInDisplayFormat.DAYS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3026, 1, 1)));

        // Weeks
        assertEquals(String.format(TimeInDisplayFormat.WEEKS.getDisplayFormat(), -1),
                TimeInDisplayFormat.WEEKS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 8), LocalDate.of(3025, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.WEEKS.getDisplayFormat(), 0),
                TimeInDisplayFormat.WEEKS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 3)));
        assertEquals(String.format(TimeInDisplayFormat.WEEKS.getDisplayFormat(), 1),
                TimeInDisplayFormat.WEEKS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 9)));
        assertEquals(String.format(TimeInDisplayFormat.WEEKS.getDisplayFormat(), 3),
                TimeInDisplayFormat.WEEKS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 28)));
        assertEquals(String.format(TimeInDisplayFormat.WEEKS.getDisplayFormat(), 4),
                TimeInDisplayFormat.WEEKS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 29)));

        // Months
        assertEquals(String.format(TimeInDisplayFormat.MONTHS.getDisplayFormat(), -1),
                TimeInDisplayFormat.MONTHS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3024, 12, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS.getDisplayFormat(), 0),
                TimeInDisplayFormat.MONTHS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS.getDisplayFormat(), 1),
                TimeInDisplayFormat.MONTHS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 2, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS.getDisplayFormat(), 12),
                TimeInDisplayFormat.MONTHS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3026, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS.getDisplayFormat(), 60),
                TimeInDisplayFormat.MONTHS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3030, 1, 1)));

        // Months and Years
        assertEquals(String.format(TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormat(), -1, 0),
                TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3024, 12, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormat(), 2, 1),
                TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3026, 3, 1)));
        assertEquals(String.format(TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormat(), 0, 5),
                TimeInDisplayFormat.MONTHS_YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3030, 1, 1)));

        // Years
        assertEquals(String.format(TimeInDisplayFormat.YEARS.getDisplayFormat(), -1),
                TimeInDisplayFormat.YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3024, 1, 1)));
        assertEquals(String.format(TimeInDisplayFormat.YEARS.getDisplayFormat(), 0),
                TimeInDisplayFormat.YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3025, 5, 1)));
        assertEquals(String.format(TimeInDisplayFormat.YEARS.getDisplayFormat(), 1),
                TimeInDisplayFormat.YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3026, 1, 2)));
        assertEquals(String.format(TimeInDisplayFormat.YEARS.getDisplayFormat(), 11),
                TimeInDisplayFormat.YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3036, 12, 31)));
        assertEquals(String.format(TimeInDisplayFormat.YEARS.getDisplayFormat(), 12),
                TimeInDisplayFormat.YEARS.getDisplayFormattedOutput(
                        LocalDate.of(3025, 1, 1), LocalDate.of(3037, 1, 1)));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("TimeInDisplayFormat.DAYS.text"), TimeInDisplayFormat.DAYS.toString());
        assertEquals(resources.getString("TimeInDisplayFormat.YEARS.text"), TimeInDisplayFormat.YEARS.toString());
    }
}
