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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiceExpressionTest {
    /** Dice that come up with the given faces in order, whatever their size. */
    private static IntUnaryOperator faces(final Integer... values) {
        Deque<Integer> queue = new ArrayDeque<>(List.of(values));
        return sides -> queue.pop();
    }

    @Test
    void addsDiceAndNumbers() {
        DiceExpression.Roll roll = DiceExpression.parse("2d6+1").roll(faces(3, 5));
        assertEquals(9, roll.total());
        assertEquals("2d6 + 1", roll.expression());
        assertEquals("[3, 5] + 1", roll.describeWorking());
    }

    @Test
    void subtractsTermsAndMixesDice() {
        DiceExpression.Roll roll = DiceExpression.parse(" 1D20 + 1d4 - 2 ").roll(faces(15, 3));
        assertEquals(16, roll.total());
        assertEquals("d20 + d4 - 2", roll.expression());
        assertEquals("[15] + [3] - 2", roll.describeWorking());
    }

    @Test
    void aPercentDieIsAHundredSided() {
        List<Integer> sides = new java.util.ArrayList<>();
        DiceExpression.parse("d%").roll(size -> {
            sides.add(size);
            return 42;
        });
        assertEquals(List.of(100), sides);
    }

    @Test
    void keepsTheHighestOrLowestDice() {
        DiceExpression.Roll high = DiceExpression.parse("4d6kh3").roll(faces(2, 6, 1, 5));
        assertEquals(13, high.total());
        assertEquals("[2, 6, ~1, 5]", high.describeWorking());

        DiceExpression.Roll low = DiceExpression.parse("2d20kl1").roll(faces(17, 4));
        assertEquals(4, low.total());
        assertEquals("[~17, 4]", low.describeWorking());
        assertEquals("2d20kl1", low.expression());
    }

    @Test
    void droppedDuplicatesAreMarkedOnce() {
        DiceExpression.Roll roll = DiceExpression.parse("3d6kh2").roll(faces(4, 4, 4));
        assertEquals(8, roll.total());
        assertEquals("[4, 4, ~4]", roll.describeWorking());
    }

    @Test
    void aLeadingMinusIsAllowed() {
        DiceExpression.Roll roll = DiceExpression.parse("-1+d6").roll(faces(4));
        assertEquals(3, roll.total());
        assertEquals("-1 + d6", roll.expression());
        assertEquals("-1 + [4]", roll.describeWorking());
    }

    @Test
    void realDiceStayInRange() {
        DiceExpression expression = DiceExpression.parse("3d6");
        for (int i = 0; i < 200; i++) {
            int total = expression.roll(sides -> 1 + (int) (Math.random() * sides)).total();
            assertTrue(total >= 3 && total <= 18, "rolled " + total);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "2d", "d", "d1", "2d0", "101d6", "2d1001", "3d6kh4", "3d6kh0", "2d6 3",
                             "2d6+", "abc", "5+3", "2d6*2", "1d6+1d6+1d6+1d6+1d6+1d6+1d6+1d6+1d6+1d6+1d6",
                             "9999999d6", "d6x" })
    void rejectsWhatItCannotRoll(final String text) {
        assertFalse(DiceExpression.isValid(text), text);
        assertThrows(IllegalArgumentException.class, () -> DiceExpression.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = { "d6", "2d6", "d10", "d20", "d100", "d%", "100d1000", "4d6kh3", "2d20KL1", "1d6+0" })
    void acceptsCommonDice(final String text) {
        assertTrue(DiceExpression.isValid(text), text);
    }
}
