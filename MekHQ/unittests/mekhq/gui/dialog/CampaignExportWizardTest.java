/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CampaignExportWizard#parseExportMoney(String, Locale)}.
 *
 * <p>This is a regression suite for issue #5939 ("Export Campaign Subset doesn't export C-bills"),
 * where the previous implementation called {@code Integer.parseInt} inside a {@code catch (Exception
 * ignored)} block, causing any input with a thousands separator (or any value larger than
 * {@code Integer.MAX_VALUE}) to be silently dropped.
 */
class CampaignExportWizardTest {

    private static final double DELTA = 1e-6;

    // ---------- Empty / null input -----------------------------------------------------------

    @Test
    void parseExportMoneyReturnsZeroForNull() {
        assertEquals(0d, CampaignExportWizard.parseExportMoney(null, Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyReturnsZeroForEmptyString() {
        assertEquals(0d, CampaignExportWizard.parseExportMoney("", Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyReturnsZeroForWhitespaceOnly() {
        assertEquals(0d, CampaignExportWizard.parseExportMoney("   ", Locale.US), DELTA);
    }

    // ---------- Plain digits work in any locale ----------------------------------------------

    @Test
    void parseExportMoneyParsesPlainDigitsInUsLocale() {
        assertEquals(1234567d, CampaignExportWizard.parseExportMoney("1234567", Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyParsesPlainDigitsInSpanishLocale() {
        assertEquals(1234567d, CampaignExportWizard.parseExportMoney("1234567", Locale.of("es", "ES")), DELTA);
    }

    @Test
    void parseExportMoneyTrimsSurroundingWhitespace() {
        assertEquals(1000d, CampaignExportWizard.parseExportMoney("  1000  ", Locale.US), DELTA);
    }

    // ---------- en-US grouping / decimal -----------------------------------------------------

    @Test
    void parseExportMoneyParsesUsGroupingSeparator() {
        // Regression for #5939 — this is the exact case the bug report describes.
        assertEquals(1_000_000d, CampaignExportWizard.parseExportMoney("1,000,000", Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyParsesUsDecimalSeparator() {
        assertEquals(1234.56d, CampaignExportWizard.parseExportMoney("1,234.56", Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyAcceptsValuesLargerThanIntegerMaxValue() {
        // The original Integer.parseInt-based implementation silently failed for any value above ~2.1B.
        double expected = 5_000_000_000d;
        assertEquals(expected, CampaignExportWizard.parseExportMoney("5,000,000,000", Locale.US), DELTA);
    }

    // ---------- es-ES grouping / decimal (the case the user called out) ----------------------

    @Test
    void parseExportMoneyParsesSpanishGroupingSeparator() {
        // In es-ES the grouping separator is '.', so "1.000.000" is one million, not 1.0.
        assertEquals(1_000_000d,
              CampaignExportWizard.parseExportMoney("1.000.000", Locale.of("es", "ES")),
              DELTA);
    }

    @Test
    void parseExportMoneyParsesSpanishDecimalSeparator() {
        assertEquals(1234.56d,
              CampaignExportWizard.parseExportMoney("1.234,56", Locale.of("es", "ES")),
              DELTA);
    }

    // ---------- Other locales ----------------------------------------------------------------

    @Test
    void parseExportMoneyParsesGermanGroupingSeparator() {
        assertEquals(1_000_000d,
              CampaignExportWizard.parseExportMoney("1.000.000", Locale.GERMANY),
              DELTA);
    }

    // ---------- Invalid input throws -----------------------------------------------------

    @Test
    void parseExportMoneyThrowsOnNonNumericInput() {
        assertThrows(NumberFormatException.class,
              () -> CampaignExportWizard.parseExportMoney("not a number", Locale.US));
    }

    @Test
    void parseExportMoneyThrowsOnTrailingGarbage() {
        // The whole string must be consumed; a partial parse is rejected so we don't silently
        // truncate user input.
        assertThrows(NumberFormatException.class,
              () -> CampaignExportWizard.parseExportMoney("1000abc", Locale.US));
    }

    @Test
    void parseExportMoneyThrowsOnLeadingGarbage() {
        assertThrows(NumberFormatException.class,
              () -> CampaignExportWizard.parseExportMoney("abc1000", Locale.US));
    }

    @Test
    void parseExportMoneyRejectsSpanishFormatInUsLocale() {
        // "1.000.000" in en-US is ambiguous and would parse as 1.0 with trailing ".000.000" left
        // over; the function rejects that to avoid silently transferring the wrong amount.
        assertThrows(NumberFormatException.class,
              () -> CampaignExportWizard.parseExportMoney("1.000.000,50", Locale.US));
    }

    // ---------- Negative and fractional values ----------------------------------------------

    @Test
    void parseExportMoneyParsesNegativeValues() {
        // The wizard itself only acts when money > 0, but the parser should still round-trip
        // negatives correctly so it's not the parser swallowing the sign.
        assertEquals(-1500d, CampaignExportWizard.parseExportMoney("-1,500", Locale.US), DELTA);
    }

    @Test
    void parseExportMoneyParsesFractionalCBills() {
        // C-bills are stored as BigDecimal inside Money, so fractional amounts are valid.
        assertEquals(0.5d, CampaignExportWizard.parseExportMoney("0.5", Locale.US), DELTA);
    }
}
