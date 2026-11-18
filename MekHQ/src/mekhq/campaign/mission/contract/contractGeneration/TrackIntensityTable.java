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

import static megamek.common.compute.Compute.d6;

import java.util.Arrays;
import java.util.List;

/**
 * The Hot Spots Draconis Reach Track Intensity Tables (Hot Spots Draconis Reach, "Track Intensity Tables", pg 147),
 * transcribed.
 *
 * <p>Each table distributes a number of items across the months a contract runs. There is one table per canonical
 * contract length - three months and six months - and within a table the column is the number of items, and the row is
 * a 1D6 roll. The chosen cell lists, month by month, how many items fall in that month; the months in a cell always sum
 * to the number that selected the column.</p>
 *
 * <p>The published tables assume contracts of exactly three or six months. When variable contract lengths yield
 * something else, the shorter table serves the shorter contracts and the longer table the rest (see
 * {@link #SHORT_TABLE_MAX_MONTHS}); the returned schedule therefore spans the chosen table's native length (three or
 * six entries) rather than the contract's actual month count. Callers that read the schedule against the calendar must
 * map it onto the real contract duration - a schedule longer than the contract has to fold its tail into the final
 * month rather than dropping it, and a shorter one simply leaves the later months empty (see
 * {@code StratConContractInitializer#rollSpawnDates}).</p>
 */
public final class TrackIntensityTable {
    private TrackIntensityTable() {}

    /**
     * A contract this many months or shorter is scheduled off the three-month table; a longer one off the six-month
     * table. Sits above the three-month family's variable range (2-3 months) and below the six-month family's (5-7).
     */
    static final int SHORT_TABLE_MAX_MONTHS = 4;

    /**
     * Three-month contracts. Indexed {@code [1D6 - 1][trackCount - 1][month]}: six rows (1D6 = 1..6), three columns
     * (1..3 tracks), three months per cell.
     */
    private static final int[][][] THREE_MONTH_TABLE = {
          // 1D6 = 1
          { { 0, 1, 0 }, { 0, 1, 1 }, { 0, 1, 2 } },
          // 1D6 = 2
          { { 0, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } },
          // 1D6 = 3
          { { 0, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } },
          // 1D6 = 4
          { { 0, 0, 1 }, { 1, 0, 1 }, { 0, 2, 1 } },
          // 1D6 = 5
          { { 0, 0, 1 }, { 0, 0, 2 }, { 2, 0, 1 } },
          // 1D6 = 6
          { { 1, 0, 0 }, { 0, 2, 0 }, { 2, 1, 0 } },
          };

    /**
     * Six-month contracts. Indexed {@code [1D6 - 1][trackCount - 1][month]}: six rows (1D6 = 1..6), six columns (1..6
     * tracks), six months per cell.
     */
    private static final int[][][] SIX_MONTH_TABLE = {
          // 1D6 = 1
          { { 0, 0, 1, 0, 0, 0 }, { 0, 1, 0, 1, 0, 0 }, { 0, 1, 0, 1, 0, 1 }, { 0, 1, 0, 1, 1, 1 },
            { 0, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1, 1 } },
          // 1D6 = 2
          { { 0, 0, 0, 0, 1, 0 }, { 0, 0, 1, 0, 1, 0 }, { 0, 1, 0, 1, 1, 0 }, { 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 0, 2 }, { 0, 2, 1, 1, 1, 1 } },
          // 1D6 = 3
          { { 0, 1, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, 1 }, { 0, 1, 1, 1, 0, 0 }, { 0, 0, 1, 1, 1, 1 },
            { 0, 2, 0, 2, 0, 1 }, { 0, 0, 2, 1, 1, 2 } },
          // 1D6 = 4
          { { 1, 0, 0, 0, 0, 0 }, { 0, 1, 1, 0, 0, 0 }, { 0, 0, 0, 1, 1, 1 }, { 0, 2, 0, 1, 0, 1 },
            { 0, 2, 1, 0, 1, 1 }, { 0, 1, 1, 2, 1, 1 } },
          // 1D6 = 5
          { { 0, 0, 0, 0, 0, 1 }, { 0, 0, 0, 1, 1, 0 }, { 0, 2, 0, 1, 0, 0 }, { 0, 0, 2, 0, 1, 1 },
            { 0, 2, 2, 1, 0, 0 }, { 0, 1, 1, 2, 0, 2 } },
          // 1D6 = 6
          { { 0, 0, 0, 1, 0, 0 }, { 0, 0, 2, 0, 0, 0 }, { 0, 0, 0, 2, 1, 0 }, { 0, 2, 1, 0, 1, 0 },
            { 0, 0, 0, 2, 2, 1 }, { 0, 1, 2, 2, 1, 0 } },
          };

    /**
     * Rolls a schedule spreading an exact number of items - such as points of interest - across a contract's months,
     * using the Track Intensity Tables.
     *
     * <p>The contract's length picks the table. A table's widest column caps how many items one roll can place, so the
     * items are split into batches no larger than that column; each batch picks its column, a 1D6 picks the row, and
     * the batches are summed month by month. Because each row sums to its column, the result always sums to exactly
     * {@code count}. It spans the chosen table's native length (three or six months).</p>
     *
     * @param lengthInMonths the contract's length in months, choosing which table applies
     * @param count          how many items to spread
     *
     * @return the schedule as per-month item counts, summing to {@code count}; every month zero if {@code count} is not
     *       positive
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Integer> rollScheduleForCount(int lengthInMonths, int count) {
        final int[][][] table = (lengthInMonths <= SHORT_TABLE_MAX_MONTHS) ? THREE_MONTH_TABLE : SIX_MONTH_TABLE;
        final int nativeMonths = table[0][0].length;
        final int widestColumn = table[0].length;

        final int[] monthlyItems = new int[nativeMonths];
        int remaining = count;
        while (remaining > 0) {
            int batch = Math.min(remaining, widestColumn);
            final int[] cell = table[d6() - 1][batch - 1];
            for (int month = 0; month < nativeMonths; month++) {
                monthlyItems[month] += cell[month];
            }
            remaining -= batch;
        }

        return Arrays.stream(monthlyItems).boxed().toList();
    }
}
