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

import static mekhq.campaign.roleplay.FateChart.NO_EXCEPTIONAL_NO;
import static mekhq.campaign.roleplay.FateChart.NO_EXCEPTIONAL_YES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FateChartTest {
    private static final int X_LOW = NO_EXCEPTIONAL_YES;
    private static final int X_HIGH = NO_EXCEPTIONAL_NO;

    /** The published chart, transcribed cell by cell. Rows follow {@link FateChartOdds}; columns are chaos 1-9. */
    private static final int[][][] CHART = {
          // IMPOSSIBLE
          { { X_LOW, 1, 81 }, { X_LOW, 1, 81 }, { X_LOW, 1, 81 }, { 1, 5, 82 }, { 2, 10, 83 }, { 3, 15, 84 },
            { 5, 25, 86 }, { 7, 35, 88 }, { 10, 50, 91 } },
          // NEARLY_IMPOSSIBLE
          { { X_LOW, 1, 81 }, { X_LOW, 1, 81 }, { 1, 5, 82 }, { 2, 10, 83 }, { 3, 15, 84 }, { 5, 25, 86 },
            { 7, 35, 88 }, { 10, 50, 91 }, { 13, 65, 94 } },
          // VERY_UNLIKELY
          { { X_LOW, 1, 81 }, { 1, 5, 82 }, { 2, 10, 83 }, { 3, 15, 84 }, { 5, 25, 86 }, { 7, 35, 88 },
            { 10, 50, 91 }, { 13, 65, 94 }, { 15, 75, 96 } },
          // UNLIKELY
          { { 1, 5, 82 }, { 2, 10, 83 }, { 3, 15, 84 }, { 5, 25, 86 }, { 7, 35, 88 }, { 10, 50, 91 },
            { 13, 65, 94 }, { 15, 75, 96 }, { 17, 85, 98 } },
          // FIFTY_FIFTY
          { { 2, 10, 83 }, { 3, 15, 84 }, { 5, 25, 86 }, { 7, 35, 88 }, { 10, 50, 91 }, { 13, 65, 94 },
            { 15, 75, 96 }, { 17, 85, 98 }, { 18, 90, 99 } },
          // LIKELY
          { { 3, 15, 84 }, { 5, 25, 86 }, { 7, 35, 88 }, { 10, 50, 91 }, { 13, 65, 94 }, { 15, 75, 96 },
            { 17, 85, 98 }, { 18, 90, 99 }, { 19, 95, 100 } },
          // VERY_LIKELY
          { { 5, 25, 86 }, { 7, 35, 88 }, { 10, 50, 91 }, { 13, 65, 94 }, { 15, 75, 96 }, { 17, 85, 98 },
            { 18, 90, 99 }, { 19, 95, 100 }, { 20, 99, X_HIGH } },
          // NEARLY_CERTAIN
          { { 7, 35, 88 }, { 10, 50, 91 }, { 13, 65, 94 }, { 15, 75, 96 }, { 17, 85, 98 }, { 18, 90, 99 },
            { 19, 95, 100 }, { 20, 99, X_HIGH }, { 20, 99, X_HIGH } },
          // CERTAIN
          { { 10, 50, 91 }, { 13, 65, 94 }, { 15, 75, 96 }, { 17, 85, 98 }, { 18, 90, 99 }, { 19, 95, 100 },
            { 20, 99, X_HIGH }, { 20, 99, X_HIGH }, { 20, 99, X_HIGH } }
    };

    @Test
    void everyCellMatchesThePublishedChart() {
        for (FateChartOdds odds : FateChartOdds.values()) {
            for (int chaos = FateChart.MINIMUM_CHAOS_FACTOR; chaos <= FateChart.MAXIMUM_CHAOS_FACTOR; chaos++) {
                assertArrayEquals(CHART[odds.ordinal()][chaos - 1], FateChart.getThresholds(odds, chaos),
                      odds + " at chaos " + chaos);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
          // 50/50 at chaos 5 is 10 / 50 / 91
          "1, EXCEPTIONAL_YES",
          "10, EXCEPTIONAL_YES",
          "11, NORMAL_YES",
          "50, NORMAL_YES",
          "51, NORMAL_NO",
          "90, NORMAL_NO",
          "91, EXCEPTIONAL_NO",
          "100, EXCEPTIONAL_NO"
    })
    void resolveAppliesTheThresholds(final int roll, final FateChartAnswer expected) {
        assertEquals(expected, FateChart.resolve(FateChartOdds.FIFTY_FIFTY, 5, roll));
    }

    @Test
    void xCellsMakeExceptionalAnswersImpossible() {
        // Impossible at chaos 1 (x / 1 / 81): a roll of 1 is only a normal yes
        assertEquals(FateChartAnswer.NORMAL_YES, FateChart.resolve(FateChartOdds.IMPOSSIBLE, 1, 1));
        // Certain at chaos 9 (20 / 99 / x): a roll of 100 is only a normal no
        assertEquals(FateChartAnswer.NORMAL_NO, FateChart.resolve(FateChartOdds.CERTAIN, 9, 100));
    }

    @Test
    void chaosFactorIsClamped() {
        assertArrayEquals(FateChart.getThresholds(FateChartOdds.LIKELY, 1),
              FateChart.getThresholds(FateChartOdds.LIKELY, -4));
        assertArrayEquals(FateChart.getThresholds(FateChartOdds.LIKELY, 9),
              FateChart.getThresholds(FateChartOdds.LIKELY, 15));
    }

    @Test
    void consultAlwaysReturnsAnAnswer() {
        for (int i = 0; i < 1000; i++) {
            FateChartResult result = FateChart.consult(FateChartOdds.FIFTY_FIFTY, 5);
            assertNotNull(result.answer());
            assertEquals(FateChart.isRandomEvent(result.roll()), result.hasRandomEvent());
        }
    }

    @Test
    void randomEventsTriggerOnMultiplesOfElevenAndOneHundred() {
        for (int roll = 1; roll <= 100; roll++) {
            boolean expected = roll == 100 || roll % 11 == 0;
            assertEquals(expected, FateChart.isRandomEvent(roll), "roll " + roll);
        }
    }

    @Test
    void consultIncludesRandomEventOnlyWhenTriggered() {
        FateChartResult withEvent = FateChart.consult(FateChartOdds.FIFTY_FIFTY, 5, 22, 60);
        assertEquals(FateChartAnswer.NORMAL_YES, withEvent.answer());
        assertTrue(withEvent.hasRandomEvent());
        assertEquals(RandomEventFocus.MOVE_AWAY_FROM_A_THREAD, withEvent.randomEventFocus());
        assertEquals(60, withEvent.randomEventRoll());

        FateChartResult withoutEvent = FateChart.consult(FateChartOdds.FIFTY_FIFTY, 5, 23, 60);
        assertFalse(withoutEvent.hasRandomEvent());
        assertNull(withoutEvent.randomEventFocus());
        assertEquals(0, withoutEvent.randomEventRoll());
    }

    @ParameterizedTest
    @CsvSource({
          "1, REMOTE_EVENT", "5, REMOTE_EVENT",
          "6, AMBIGUOUS_EVENT", "10, AMBIGUOUS_EVENT",
          "11, NEW_NPC", "20, NEW_NPC",
          "21, NPC_ACTION", "40, NPC_ACTION",
          "41, NPC_NEGATIVE", "45, NPC_NEGATIVE",
          "46, NPC_POSITIVE", "50, NPC_POSITIVE",
          "51, MOVE_TOWARD_A_THREAD", "55, MOVE_TOWARD_A_THREAD",
          "56, MOVE_AWAY_FROM_A_THREAD", "65, MOVE_AWAY_FROM_A_THREAD",
          "66, MOVE_TOWARD_A_THREAD", "70, MOVE_TOWARD_A_THREAD",
          "71, PC_NEGATIVE", "80, PC_NEGATIVE",
          "81, PC_POSITIVE", "85, PC_POSITIVE",
          "86, CURRENT_CONTEXT", "100, CURRENT_CONTEXT"
    })
    void randomEventFocusMatchesThePublishedTable(final int roll, final RandomEventFocus expected) {
        assertEquals(expected, RandomEventFocus.fromRoll(roll));
    }

    @Test
    void everyLabelIsPresent() {
        for (FateChartOdds odds : FateChartOdds.values()) {
            assertFalse(odds.getLabel().startsWith("!"), odds.name());
        }
        for (FateChartAnswer answer : FateChartAnswer.values()) {
            assertFalse(answer.getLabel().startsWith("!"), answer.name());
        }
        for (RandomEventFocus focus : RandomEventFocus.values()) {
            assertFalse(focus.getLabel().startsWith("!"), focus.name());
        }
    }
}
