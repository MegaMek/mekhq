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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialTermTest {
    //region Variable Declarations
    private static final FinancialTerm[] terms = FinancialTerm.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("FinancialTerm.BIWEEKLY.toolTipText"),
                FinancialTerm.BIWEEKLY.getToolTipText());
        assertEquals(resources.getString("FinancialTerm.ANNUALLY.toolTipText"),
                FinancialTerm.ANNUALLY.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsBiweekly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.BIWEEKLY) {
                assertTrue(financialTerm.isBiweekly());
            } else {
                assertFalse(financialTerm.isBiweekly());
            }
        }
    }

    @Test
    public void testIsMonthly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.MONTHLY) {
                assertTrue(financialTerm.isMonthly());
            } else {
                assertFalse(financialTerm.isMonthly());
            }
        }
    }

    @Test
    public void testIsQuarterly() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.QUARTERLY) {
                assertTrue(financialTerm.isQuarterly());
            } else {
                assertFalse(financialTerm.isQuarterly());
            }
        }
    }

    @Test
    public void testIsSemiannually() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.SEMIANNUALLY) {
                assertTrue(financialTerm.isSemiannually());
            } else {
                assertFalse(financialTerm.isSemiannually());
            }
        }
    }

    @Test
    public void testIsAnnually() {
        for (final FinancialTerm financialTerm : terms) {
            if (financialTerm == FinancialTerm.ANNUALLY) {
                assertTrue(financialTerm.isAnnually());
            } else {
                assertFalse(financialTerm.isAnnually());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(FinancialTerm.MONTHLY, FinancialTerm.parseFromString("MONTHLY"));
        assertEquals(FinancialTerm.ANNUALLY, FinancialTerm.parseFromString("ANNUALLY"));

        // Parsing Legacy Testing
        assertEquals(FinancialTerm.BIWEEKLY, FinancialTerm.parseFromString("0"));
        assertEquals(FinancialTerm.MONTHLY, FinancialTerm.parseFromString("1"));
        assertEquals(FinancialTerm.QUARTERLY, FinancialTerm.parseFromString("2"));
        assertEquals(FinancialTerm.ANNUALLY, FinancialTerm.parseFromString("3"));

        // Failure Testing
        assertEquals(FinancialTerm.ANNUALLY, FinancialTerm.parseFromString("failureFailsFake"));
    }
    //endregion File I/O

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("FinancialTerm.MONTHLY.text"), FinancialTerm.MONTHLY.toString());
        assertEquals(resources.getString("FinancialTerm.QUARTERLY.text"), FinancialTerm.QUARTERLY.toString());
        assertEquals(resources.getString("FinancialTerm.ANNUALLY.text"), FinancialTerm.ANNUALLY.toString());
    }
}
