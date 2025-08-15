/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

class FinancialYearDurationTest {
    // region Variable Declarations
    private static final FinancialYearDuration[] durations = FinancialYearDuration.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("FinancialYearDuration.SEMIANNUAL.toolTipText"),
              FinancialYearDuration.SEMIANNUAL.getToolTipText());
        assertEquals(resources.getString("FinancialYearDuration.DECENNIAL.toolTipText"),
              FinancialYearDuration.DECENNIAL.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsSemiannual() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.SEMIANNUAL) {
                assertTrue(duration.isSemiannual());
            } else {
                assertFalse(duration.isSemiannual());
            }
        }
    }

    @Test
    void testIsAnnual() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.ANNUAL) {
                assertTrue(duration.isAnnual());
            } else {
                assertFalse(duration.isAnnual());
            }
        }
    }

    @Test
    void testIsBiennial() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.BIENNIAL) {
                assertTrue(duration.isBiennial());
            } else {
                assertFalse(duration.isBiennial());
            }
        }
    }

    @Test
    void testIsQuinquennial() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.QUINQUENNIAL) {
                assertTrue(duration.isQuinquennial());
            } else {
                assertFalse(duration.isQuinquennial());
            }
        }
    }

    @Test
    void testIsDecennial() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.DECENNIAL) {
                assertTrue(duration.isDecennial());
            } else {
                assertFalse(duration.isDecennial());
            }
        }
    }

    @Test
    void testIsForever() {
        for (final FinancialYearDuration duration : durations) {
            if (duration == FinancialYearDuration.FOREVER) {
                assertTrue(duration.isForever());
            } else {
                assertFalse(duration.isForever());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testIsEndOfFinancialYear() {
        assertTrue(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 45)));
        assertTrue(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.of(3025, 7, 1)));
        assertFalse(FinancialYearDuration.SEMIANNUAL.isEndOfFinancialYear(LocalDate.of(3025, 7, 2)));

        assertTrue(FinancialYearDuration.ANNUAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.ANNUAL.isEndOfFinancialYear(LocalDate.of(3025, 12, 31)));

        assertFalse(FinancialYearDuration.BIENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.BIENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 11)));
        assertTrue(FinancialYearDuration.BIENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 1)));

        assertTrue(FinancialYearDuration.QUINQUENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3025, 1)));
        assertFalse(FinancialYearDuration.QUINQUENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 1)));
        assertFalse(FinancialYearDuration.QUINQUENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 11)));

        assertFalse(FinancialYearDuration.DECENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 1)));
        assertFalse(FinancialYearDuration.DECENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3026, 11)));
        assertTrue(FinancialYearDuration.DECENNIAL.isEndOfFinancialYear(LocalDate.ofYearDay(3030, 1)));

        assertFalse(FinancialYearDuration.FOREVER.isEndOfFinancialYear(LocalDate.ofYearDay(3000, 1)));
        assertFalse(FinancialYearDuration.FOREVER.isEndOfFinancialYear(LocalDate.ofYearDay(3006, 11)));
    }

    /**
     * This is only called when the above test returns true, so the date provided will always be valid
     */
    @Test
    void testGetExportFilenameDateString() {
        assertEquals("3025 Jan - Jun",
              FinancialYearDuration.SEMIANNUAL.getExportFilenameDateString(LocalDate.of(3025, 7, 1)));
        assertEquals("3025 Jul - Dec",
              FinancialYearDuration.SEMIANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));

        assertEquals("3024", FinancialYearDuration.ANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3025, 1)));
        assertEquals("3025", FinancialYearDuration.ANNUAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));

        assertEquals("3024 - 3025",
              FinancialYearDuration.BIENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3026, 1)));
        assertEquals("3026 - 3027",
              FinancialYearDuration.BIENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3028, 1)));

        assertEquals("3020 - 3024",
              FinancialYearDuration.QUINQUENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3025, 1)));
        assertEquals("3025 - 3029",
              FinancialYearDuration.QUINQUENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3030, 1)));

        assertEquals("3020 - 3029",
              FinancialYearDuration.DECENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3030, 1)));
        assertEquals("3030 - 3039",
              FinancialYearDuration.DECENNIAL.getExportFilenameDateString(LocalDate.ofYearDay(3040, 1)));
    }

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("FinancialYearDuration.ANNUAL.text"), FinancialYearDuration.ANNUAL.toString());
        assertEquals(resources.getString("FinancialYearDuration.DECENNIAL.text"),
              FinancialYearDuration.DECENNIAL.toString());
        assertEquals(resources.getString("FinancialYearDuration.FOREVER.text"),
              FinancialYearDuration.FOREVER.toString());
    }
}
