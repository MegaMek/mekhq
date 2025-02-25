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
package mekhq.campaign.finances.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class FinancialTermTest {
    // region Variable Declarations
    private static final FinancialTerm[] terms = FinancialTerm.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("FinancialTerm.BIWEEKLY.toolTipText"),
                FinancialTerm.BIWEEKLY.getToolTipText());
        assertEquals(resources.getString("FinancialTerm.ANNUALLY.toolTipText"),
                FinancialTerm.ANNUALLY.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsBiweekly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.BIWEEKLY) {
                assertTrue(financialTerm.isBiweekly());
            } else {
                assertFalse(financialTerm.isBiweekly());
            }
        }
    }

    @Test
    void testIsMonthly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.MONTHLY) {
                assertTrue(financialTerm.isMonthly());
            } else {
                assertFalse(financialTerm.isMonthly());
            }
        }
    }

    @Test
    void testIsQuarterly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.QUARTERLY) {
                assertTrue(financialTerm.isQuarterly());
            } else {
                assertFalse(financialTerm.isQuarterly());
            }
        }
    }

    @Test
    void testIsSemiannually() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.SEMIANNUALLY) {
                assertTrue(financialTerm.isSemiannually());
            } else {
                assertFalse(financialTerm.isSemiannually());
            }
        }
    }

    @Test
    void testIsAnnually() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.ANNUALLY) {
                assertTrue(financialTerm.isAnnually());
            } else {
                assertFalse(financialTerm.isAnnually());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testNextValidDate() {
        assertEquals(LocalDate.of(3025, 1, 10),
                FinancialTerm.BIWEEKLY.nextValidDate(LocalDate.of(3024, 12, 27)));
        assertEquals(LocalDate.of(3025, 1, 10),
                FinancialTerm.BIWEEKLY.nextValidDate(LocalDate.of(3025, 1, 1)));
        assertEquals(LocalDate.of(3025, 1, 24),
                FinancialTerm.BIWEEKLY.nextValidDate(LocalDate.of(3025, 1, 3)));
        assertEquals(LocalDate.of(3025, 1, 24),
                FinancialTerm.BIWEEKLY.nextValidDate(LocalDate.of(3025, 1, 4)));
        assertEquals(LocalDate.of(3025, 1, 24),
                FinancialTerm.BIWEEKLY.nextValidDate(LocalDate.of(3025, 1, 10)));

        assertEquals(LocalDate.of(3025, 2, 1),
                FinancialTerm.MONTHLY.nextValidDate(LocalDate.of(3025, 1, 1)));
        assertEquals(LocalDate.of(3025, 3, 1),
                FinancialTerm.MONTHLY.nextValidDate(LocalDate.of(3025, 1, 2)));
        assertEquals(LocalDate.of(3025, 3, 1),
                FinancialTerm.MONTHLY.nextValidDate(LocalDate.of(3025, 2, 1)));

        assertEquals(LocalDate.of(3025, 4, 1),
                FinancialTerm.QUARTERLY.nextValidDate(LocalDate.of(3025, 1, 1)));
        assertEquals(LocalDate.of(3025, 7, 1),
                FinancialTerm.QUARTERLY.nextValidDate(LocalDate.of(3025, 1, 2)));
        assertEquals(LocalDate.of(3025, 7, 1),
                FinancialTerm.QUARTERLY.nextValidDate(LocalDate.of(3025, 4, 1)));

        assertEquals(LocalDate.of(3025, 7, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 1, 1)));
        assertEquals(LocalDate.of(3026, 1, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 1, 2)));
        assertEquals(LocalDate.of(3026, 1, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 5, 1)));
        assertEquals(LocalDate.of(3026, 1, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 7, 1)));
        assertEquals(LocalDate.of(3026, 7, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 8, 1)));
        assertEquals(LocalDate.of(3026, 7, 1),
                FinancialTerm.SEMIANNUALLY.nextValidDate(LocalDate.of(3025, 12, 1)));

        assertEquals(LocalDate.of(3026, 1, 1),
                FinancialTerm.ANNUALLY.nextValidDate(LocalDate.of(3025, 1, 1)));
        assertEquals(LocalDate.of(3027, 1, 1),
                FinancialTerm.ANNUALLY.nextValidDate(LocalDate.of(3025, 1, 2)));
        assertEquals(LocalDate.of(3027, 1, 1),
                FinancialTerm.ANNUALLY.nextValidDate(LocalDate.of(3026, 1, 1)));
    }

    @Test
    void testEndsToday() {
        assertFalse(FinancialTerm.BIWEEKLY.endsToday(LocalDate.of(3024, 12, 31),
                LocalDate.of(3025, 1, 1)));
        assertFalse(FinancialTerm.BIWEEKLY.endsToday(LocalDate.of(3025, 1, 8),
                LocalDate.of(3025, 1, 9)));
        assertTrue(FinancialTerm.BIWEEKLY.endsToday(LocalDate.of(3025, 1, 9),
                LocalDate.of(3025, 1, 10)));
        assertFalse(FinancialTerm.BIWEEKLY.endsToday(LocalDate.of(3025, 1, 16),
                LocalDate.of(3025, 1, 17)));
        assertTrue(FinancialTerm.BIWEEKLY.endsToday(LocalDate.of(3025, 1, 23),
                LocalDate.of(3025, 1, 24)));

        assertTrue(FinancialTerm.MONTHLY.endsToday(LocalDate.of(3024, 12, 31),
                LocalDate.of(3025, 1, 1)));
        assertFalse(FinancialTerm.MONTHLY.endsToday(LocalDate.of(3025, 1, 1),
                LocalDate.of(3025, 1, 31)));

        assertTrue(FinancialTerm.QUARTERLY.endsToday(LocalDate.of(3025, 3, 31),
                LocalDate.of(3025, 4, 1)));
        assertFalse(FinancialTerm.QUARTERLY.endsToday(LocalDate.of(3025, 4, 1),
                LocalDate.of(3025, 5, 1)));

        assertTrue(FinancialTerm.SEMIANNUALLY.endsToday(LocalDate.of(3024, 12, 31),
                LocalDate.of(3025, 1, 1)));
        assertFalse(FinancialTerm.SEMIANNUALLY.endsToday(LocalDate.of(3025, 1, 1),
                LocalDate.of(3025, 1, 2)));
        assertFalse(FinancialTerm.SEMIANNUALLY.endsToday(LocalDate.of(3025, 3, 31),
                LocalDate.of(3025, 4, 1)));
        assertTrue(FinancialTerm.SEMIANNUALLY.endsToday(LocalDate.of(3025, 6, 30),
                LocalDate.of(3025, 7, 1)));

        assertFalse(FinancialTerm.ANNUALLY.endsToday(LocalDate.ofYearDay(3026, 1),
                LocalDate.ofYearDay(3026, 2)));
        assertFalse(FinancialTerm.ANNUALLY.endsToday(LocalDate.ofYearDay(3029, 364),
                LocalDate.ofYearDay(3029, 365)));
        assertTrue(FinancialTerm.ANNUALLY.endsToday(LocalDate.ofYearDay(3029, 365),
                LocalDate.ofYearDay(3030, 1)));
    }

    @Test
    void testDetermineYearlyDenominator() {
        assertEquals(26, FinancialTerm.BIWEEKLY.determineYearlyDenominator());
        assertEquals(12, FinancialTerm.MONTHLY.determineYearlyDenominator());
        assertEquals(4, FinancialTerm.QUARTERLY.determineYearlyDenominator());
        assertEquals(2, FinancialTerm.SEMIANNUALLY.determineYearlyDenominator());
        assertEquals(1, FinancialTerm.ANNUALLY.determineYearlyDenominator());
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(FinancialTerm.MONTHLY, FinancialTerm.parseFromString("MONTHLY"));
        assertEquals(FinancialTerm.ANNUALLY, FinancialTerm.parseFromString("ANNUALLY"));

        // Failure Testing
        assertEquals(FinancialTerm.ANNUALLY, FinancialTerm.parseFromString("failureFailsFake"));
    }
    // endregion File I/O

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("FinancialTerm.MONTHLY.text"), FinancialTerm.MONTHLY.toString());
        assertEquals(resources.getString("FinancialTerm.QUARTERLY.text"), FinancialTerm.QUARTERLY.toString());
        assertEquals(resources.getString("FinancialTerm.ANNUALLY.text"), FinancialTerm.ANNUALLY.toString());
    }
}
