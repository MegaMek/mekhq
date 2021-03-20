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

import static org.junit.Assert.*;

import megamek.common.util.EncodeControl;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;

public class FinancialYearDurationTest {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances", new EncodeControl());

    @Test
    public void testIsEndOfFinancialYear() {
        assertTrue(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 45)));
        assertTrue(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.of(3025, 7, 1)));

        assertTrue(FinancialYearDuration.ANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.ANNUAL.isEndOfFinancialYear(LocalDate.of(3025, 12, 31)));

        assertTrue(FinancialYearDuration.BIENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 1)));
        assertFalse(FinancialYearDuration.BIENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 11)));

        assertTrue(FinancialYearDuration.QUINQUENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.QUINQUENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 11)));

        assertTrue(FinancialYearDuration.DECENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3030, 1)));
        assertFalse(FinancialYearDuration.DECENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 11)));

        assertFalse(FinancialYearDuration.FOREVER.isEndOfFinancialYear(LocalDate.ofYearDay(3000, 1)));
        assertFalse(FinancialYearDuration.FOREVER.isEndOfFinancialYear(LocalDate.ofYearDay(3006, 11)));
    }

    /**
     * This is only called when the above test returns true, so the date provided will always be valid
     */
    @Test
    public void testGetExportFilenameDateString() {
        assertEquals("3025 Jan - Jun", FinancialYearDuration.SEMIANNUAL.getExportFilenameDateString(LocalDate.of(3025, 7, 1)));
        assertEquals("3025 Jul - Dec", FinancialYearDuration.SEMIANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));

        assertEquals("3024", FinancialYearDuration.ANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3025, 1)));
        assertEquals("3025", FinancialYearDuration.ANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));

        assertEquals("3024 - 3025", FinancialYearDuration.BIENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));
        assertEquals("3026 - 3027", FinancialYearDuration.BIENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3028, 1)));

        assertEquals("3020 - 3024", FinancialYearDuration.QUINQUENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3025, 1)));
        assertEquals("3025 - 3029", FinancialYearDuration.QUINQUENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3030, 1)));

        assertEquals("3020 - 3029", FinancialYearDuration.DECENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3030, 1)));
        assertEquals("3030 - 3039", FinancialYearDuration.DECENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3040, 1)));
    }

    @Test
    public void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(FinancialYearDuration.ANNUAL, FinancialYearDuration.parseFromString("ANNUAL"));
        assertEquals(FinancialYearDuration.BIENNIAL, FinancialYearDuration.parseFromString("BIENNIAL"));
        assertEquals(FinancialYearDuration.FOREVER, FinancialYearDuration.parseFromString("FOREVER"));

        // Parsing Legacy Testing
        assertEquals(FinancialYearDuration.BIENNIAL, FinancialYearDuration.parseFromString("BIANNUAL"));

        // Failure Testing
        assertEquals(FinancialYearDuration.ANNUAL, FinancialYearDuration.parseFromString("failureFailsFake"));
    }

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("FinancialYearDuration.ANNUAL.text"), FinancialYearDuration.ANNUAL.toString());
        assertEquals(resources.getString("FinancialYearDuration.DECENNIAL.text"), FinancialYearDuration.DECENNIAL.toString());
        assertEquals(resources.getString("FinancialYearDuration.FOREVER.text"), FinancialYearDuration.FOREVER.toString());
    }
}
