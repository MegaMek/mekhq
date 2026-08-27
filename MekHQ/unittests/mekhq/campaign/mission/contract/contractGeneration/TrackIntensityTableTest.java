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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import megamek.common.compute.Compute;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link TrackIntensityTable}: the transcription of the printed Track Intensity Tables, the 1D6 row lookup, table
 * selection by contract length, track-count clamping, and the roll-count summing that scales intensity. The 1D6 roll is
 * stubbed so each table row can be pinned exactly.
 */
class TrackIntensityTableTest {

    /** A length that routes to the three-month table, and one that routes to the six-month table. */
    private static final int THREE_MONTH_LENGTH = 3;
    private static final int SIX_MONTH_LENGTH = 6;

    private static List<Integer> rollForRow(final int lengthInMonths, final int trackCount, final int row) {
        return rollForRow(lengthInMonths, trackCount, 1, row);
    }

    private static List<Integer> rollForRow(final int lengthInMonths, final int trackCount, final int rollCount,
          final int row) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(Compute::d6).thenReturn(row);
            return TrackIntensityTable.rollSchedule(lengthInMonths, trackCount, rollCount);
        }
    }

    private static int sum(final List<Integer> schedule) {
        return schedule.stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void threeMonthCellsAreTranscribedExactly() {
        // Spot-check both ends of the printed three-month table (1D6 = 1 and 1D6 = 6).
        assertEquals(List.of(0, 1, 0), rollForRow(THREE_MONTH_LENGTH, 1, 1));
        assertEquals(List.of(0, 1, 1), rollForRow(THREE_MONTH_LENGTH, 2, 1));
        assertEquals(List.of(0, 1, 2), rollForRow(THREE_MONTH_LENGTH, 3, 1));
        assertEquals(List.of(1, 0, 0), rollForRow(THREE_MONTH_LENGTH, 1, 6));
        assertEquals(List.of(0, 2, 0), rollForRow(THREE_MONTH_LENGTH, 2, 6));
        assertEquals(List.of(2, 1, 0), rollForRow(THREE_MONTH_LENGTH, 3, 6));
    }

    @Test
    void sixMonthCellsAreTranscribedExactly() {
        assertEquals(List.of(0, 0, 1, 0, 0, 0), rollForRow(SIX_MONTH_LENGTH, 1, 1));
        assertEquals(List.of(1, 1, 1, 1, 1, 1), rollForRow(SIX_MONTH_LENGTH, 6, 1));
        assertEquals(List.of(0, 0, 2, 1, 1, 2), rollForRow(SIX_MONTH_LENGTH, 6, 3));
        assertEquals(List.of(0, 1, 2, 2, 1, 0), rollForRow(SIX_MONTH_LENGTH, 6, 6));
    }

    @Test
    void everyThreeMonthCellHasThreeMonthsSummingToTrackCount() {
        for (int trackCount = 1; trackCount <= 3; trackCount++) {
            for (int row = 1; row <= 6; row++) {
                List<Integer> schedule = rollForRow(THREE_MONTH_LENGTH, trackCount, row);
                assertEquals(3, schedule.size(), "row " + row + " track count " + trackCount);
                assertEquals(trackCount, sum(schedule), "row " + row + " track count " + trackCount);
            }
        }
    }

    @Test
    void everySixMonthCellHasSixMonthsSummingToTrackCount() {
        for (int trackCount = 1; trackCount <= 6; trackCount++) {
            for (int row = 1; row <= 6; row++) {
                List<Integer> schedule = rollForRow(SIX_MONTH_LENGTH, trackCount, row);
                assertEquals(6, schedule.size(), "row " + row + " track count " + trackCount);
                assertEquals(trackCount, sum(schedule), "row " + row + " track count " + trackCount);
            }
        }
    }

    @Test
    void lengthSelectsTheTable() {
        // Track count 1 avoids clamping, so only the table's native month count varies. The three-month table serves
        // lengths up to SHORT_TABLE_MAX_MONTHS (4); longer contracts use the six-month table.
        assertEquals(3, rollForRow(2, 1, 1).size());
        assertEquals(3, rollForRow(3, 1, 1).size());
        assertEquals(3, rollForRow(TrackIntensityTable.SHORT_TABLE_MAX_MONTHS, 1, 1).size());
        assertEquals(6, rollForRow(TrackIntensityTable.SHORT_TABLE_MAX_MONTHS + 1, 1, 1).size());
        assertEquals(6, rollForRow(6, 1, 1).size());
        assertEquals(6, rollForRow(7, 1, 1).size());
    }

    @Test
    void trackCountAboveWidestColumnIsClamped() {
        // The three-month table has three columns; a track count of 5 clamps to column 3 (so it sums to 3, not 5).
        List<Integer> threeMonth = rollForRow(THREE_MONTH_LENGTH, 5, 1);
        assertEquals(List.of(0, 1, 2), threeMonth);
        assertEquals(3, sum(threeMonth));

        // The six-month table has six columns; a track count of 9 clamps to column 6.
        List<Integer> sixMonth = rollForRow(SIX_MONTH_LENGTH, 9, 1);
        assertEquals(6, sum(sixMonth));
    }

    @Test
    void zeroTrackCountYieldsAnEmptyScheduleOfTheTableLength() {
        assertEquals(List.of(0, 0, 0), TrackIntensityTable.rollSchedule(THREE_MONTH_LENGTH, 0));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), TrackIntensityTable.rollSchedule(SIX_MONTH_LENGTH, 0));
    }

    @Test
    void rollCountSumsThatManyRowsToScaleIntensity() {
        // Every roll is pinned to the same row, so the result is that row's cell multiplied element-wise by the count.
        assertEquals(List.of(0, 3, 6), rollForRow(THREE_MONTH_LENGTH, 3, 3, 1));
        assertEquals(6 * 3, sum(rollForRow(SIX_MONTH_LENGTH, 6, 3, 1)));
    }

    @Test
    void nonPositiveRollCountYieldsAnEmptySchedule() {
        assertEquals(List.of(0, 0, 0), rollForRow(THREE_MONTH_LENGTH, 3, 0, 1));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), rollForRow(SIX_MONTH_LENGTH, 6, -2, 1));
    }

    @Test
    void twoArgRollIsASingleRoll() {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(Compute::d6).thenReturn(1);
            // Same row, one roll: the two-arg overload matches the three-arg overload with a roll count of one.
            assertEquals(TrackIntensityTable.rollSchedule(SIX_MONTH_LENGTH, 6, 1),
                  TrackIntensityTable.rollSchedule(SIX_MONTH_LENGTH, 6));
        }
    }
}
