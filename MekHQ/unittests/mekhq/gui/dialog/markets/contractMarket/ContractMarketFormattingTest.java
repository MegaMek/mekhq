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
package mekhq.gui.dialog.markets.contractMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Locale;

import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.gui.baseComponents.hud.HudStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Tests the compact money figures and the difficulty colours of {@link ContractMarketFormatting}.
 *
 * <p>Compact figures fill the board's tiles and the Compare page's tight columns, so they must stay short: millions
 * to two decimals, thousands rounded to whole, smaller amounts whole. Losses carry a true minus sign, and only the
 * signed variant marks a gain with a plus.</p>
 */
class ContractMarketFormattingTest {
    private static final String MINUS = "−";

    private MockedStatic<MekHQ> mekHQ;

    /**
     * Supplies the number-formatting locale, which would otherwise need the application's user preferences.
     */
    @BeforeEach
    void setUp() {
        MHQOptions mhqOptions = mock(MHQOptions.class);
        when(mhqOptions.getLocale()).thenReturn(Locale.ENGLISH);
        mekHQ = mockStatic(MekHQ.class);
        mekHQ.when(MekHQ::getMHQOptions).thenReturn(mhqOptions);
    }

    @AfterEach
    void tearDown() {
        mekHQ.close();
    }

    @ParameterizedTest
    @CsvSource({
          "4850000, 4.85M",
          "1000000, 1.00M",
          "12345678, 12.35M",
          "1234567890, '1,234.57M'",
          "712400, 712K",
          "712600, 713K",
          "1000, 1K",
          "999, 999",
          "450, 450",
          "0, 0"
    })
    void compactMoneyShortensPositiveAmounts(double amount, String expected) {
        assertEquals(expected, ContractMarketFormatting.compactMoney(Money.of(amount)));
    }

    @ParameterizedTest
    @CsvSource({
          "-4850000, 4.85M",
          "-712000, 712K",
          "-450, 450"
    })
    void compactMoneyPrefixesLossesWithTheMinusSign(double amount, String magnitude) {
        assertEquals(MINUS + magnitude, ContractMarketFormatting.compactMoney(Money.of(amount)));
    }

    @ParameterizedTest
    @CsvSource({
          "4850000, +4.85M",
          "712000, +712K",
          "450, +450"
    })
    void signedCompactMoneyPrefixesGainsWithPlus(double amount, String expected) {
        assertEquals(expected, ContractMarketFormatting.signedCompactMoney(Money.of(amount)));
    }

    @ParameterizedTest
    @CsvSource({
          "-4850000, 4.85M",
          "-712000, 712K",
          "-450, 450"
    })
    void signedCompactMoneyPrefixesLossesWithTheMinusSignOnly(double amount, String magnitude) {
        assertEquals(MINUS + magnitude, ContractMarketFormatting.signedCompactMoney(Money.of(amount)));
    }

    @Test
    void signedCompactMoneyLeavesZeroUnsigned() {
        assertEquals("0", ContractMarketFormatting.signedCompactMoney(Money.zero()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void favourableDifficultyIsReady(int difficulty) {
        assertEquals(HudStyle.READY, ContractMarketFormatting.severityColor(difficulty));
    }

    @ParameterizedTest
    @ValueSource(ints = { 4, 5 })
    void evenDifficultyIsAccent(int difficulty) {
        assertEquals(HudStyle.ACCENT, ContractMarketFormatting.severityColor(difficulty));
    }

    @ParameterizedTest
    @ValueSource(ints = { 6, 7 })
    void hardDifficultyIsAmber(int difficulty) {
        assertEquals(HudStyle.AMBER, ContractMarketFormatting.severityColor(difficulty));
    }

    @ParameterizedTest
    @ValueSource(ints = { 8, 9, 10 })
    void veryHardDifficultyIsDanger(int difficulty) {
        assertEquals(HudStyle.DANGER, ContractMarketFormatting.severityColor(difficulty));
    }
}
