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
package mekhq.campaign.universe.factionHints;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Tests the date-range primitives every diplomacy lookup is built on: inclusive start/end bounds, open (perpetual)
 * bounds, and the starts-today/ends-today checks used for the player-facing war-and-peace notifications.
 */
class FactionHintTest {
    private static final LocalDate START = LocalDate.of(3050, 3, 1);
    private static final LocalDate END = LocalDate.of(3052, 6, 15);

    @Test
    void testDateRangeBoundsAreInclusive() {
        FactionHint hint = new FactionHint("Bounded", START, END);

        assertTrue(hint.isInDateRange(START), "The exact start day is in range");
        assertTrue(hint.isInDateRange(END), "The exact end day is in range");
        assertTrue(hint.isInDateRange(LocalDate.of(3051, 1, 1)));
        assertFalse(hint.isInDateRange(START.minusDays(1)));
        assertFalse(hint.isInDateRange(END.plusDays(1)));
    }

    @Test
    void testNullBoundsArePerpetual() {
        FactionHint perpetual = new FactionHint("Perpetual", null, null);
        FactionHint openStart = new FactionHint("Open start", null, END);
        FactionHint openEnd = new FactionHint("Open end", START, null);

        assertTrue(perpetual.isInDateRange(LocalDate.of(2398, 1, 1)));
        assertTrue(perpetual.isInDateRange(LocalDate.of(3152, 12, 31)));
        assertTrue(openStart.isInDateRange(LocalDate.of(2398, 1, 1)));
        assertFalse(openStart.isInDateRange(END.plusDays(1)));
        assertTrue(openEnd.isInDateRange(LocalDate.of(3152, 12, 31)));
        assertFalse(openEnd.isInDateRange(START.minusDays(1)));
    }

    @Test
    void testHintStartsToday() {
        FactionHint hint = new FactionHint("Bounded", START, END);

        assertTrue(hint.hintStartsToday(START));
        assertFalse(hint.hintStartsToday(START.plusDays(1)));
        assertFalse(new FactionHint("Perpetual", null, null).hintStartsToday(START),
              "A hint without a start date never starts today");
    }

    @Test
    void testHintEndsToday() {
        FactionHint hint = new FactionHint("Bounded", START, END);

        assertTrue(hint.hintEndsToday(END));
        assertFalse(hint.hintEndsToday(END.minusDays(1)));
        assertFalse(new FactionHint("Perpetual", null, null).hintEndsToday(END),
              "A hint without an end date never ends today");
    }
}
