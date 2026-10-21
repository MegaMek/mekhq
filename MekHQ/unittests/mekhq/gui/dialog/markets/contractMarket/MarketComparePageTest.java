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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mekhq.gui.dialog.markets.contractMarket.MarketComparePage.Better;
import mekhq.gui.dialog.markets.contractMarket.MarketComparePage.Cell;
import mekhq.gui.dialog.markets.contractMarket.MarketComparePage.Row;
import org.junit.jupiter.api.Test;

/**
 * Tests the ranking and filtering behind {@link MarketComparePage}.
 *
 * <p>A row marks one value BEST only when higher (or lower) is clearly better and the offers actually differ, so a
 * tie across every offer, or a lone offer that can be ranked, marks nothing. "Only rows that differ" must drop the rows
 * every offer shares, and any section heading left with nothing under it.</p>
 */
class MarketComparePageTest {
    @Test
    void higherIsBetterPicksTheLargest() {
        assertEquals(3.0, MarketComparePage.bestValue(List.of(1.0, 3.0, 2.0), Better.HIGHER));
    }

    @Test
    void lowerIsBetterPicksTheSmallest() {
        assertEquals(1.0, MarketComparePage.bestValue(List.of(2.0, 1.0, 3.0), Better.LOWER));
    }

    @Test
    void higherIsBetterRanksLossesBySize() {
        assertEquals(-10_000.0, MarketComparePage.bestValue(List.of(-50_000.0, -10_000.0), Better.HIGHER));
    }

    @Test
    void rowWithNoBetterDirectionHasNoBest() {
        assertNull(MarketComparePage.bestValue(List.of(1.0, 3.0, 2.0), Better.NEITHER));
    }

    @Test
    void identicalValuesHaveNoBest() {
        assertNull(MarketComparePage.bestValue(List.of(4.0, 4.0, 4.0), Better.HIGHER));
        assertNull(MarketComparePage.bestValue(List.of(4.0, 4.0, 4.0), Better.LOWER));
    }

    @Test
    void tiedBestValueIsStillTheBest() {
        assertEquals(4.0, MarketComparePage.bestValue(List.of(4.0, 4.0, 1.0), Better.HIGHER));
    }

    @Test
    void offersThatCannotBeRankedAreSkipped() {
        assertEquals(4.0, MarketComparePage.bestValue(Arrays.asList(null, 2.0, 4.0), Better.HIGHER));
        assertEquals(2.0, MarketComparePage.bestValue(Arrays.asList(null, 2.0, 4.0), Better.LOWER));
    }

    @Test
    void fewerThanTwoRankableValuesHaveNoBest() {
        assertNull(MarketComparePage.bestValue(List.of(5.0), Better.HIGHER));
        assertNull(MarketComparePage.bestValue(Arrays.asList(5.0, null), Better.HIGHER));
        assertNull(MarketComparePage.bestValue(Arrays.asList(null, null), Better.LOWER));
        assertNull(MarketComparePage.bestValue(List.of(), Better.HIGHER));
    }

    @Test
    void onlyDifferencesDropsSharedRows() {
        List<Row> rows = List.of(section("Money"),
              row("Pay", "1M", "2M"),
              row("Transport", "50%", "50%"),
              row("Net", "3M", "1M"));

        assertEquals(List.of("Money", "Pay", "Net"), labels(MarketComparePage.onlyDifferences(rows)));
    }

    @Test
    void onlyDifferencesDropsSectionsLeftEmpty() {
        List<Row> rows = List.of(section("Money"),
              row("Pay", "1M", "1M"),
              section("Danger"),
              row("Threat", "Green", "Elite"),
              section("Terms"),
              row("Salvage", "50%", "50%"));

        assertEquals(List.of("Danger", "Threat"), labels(MarketComparePage.onlyDifferences(rows)));
    }

    @Test
    void onlyDifferencesOfIdenticalOffersIsEmpty() {
        List<Row> rows = List.of(section("Money"), row("Pay", "1M", "1M"), row("Net", "2M", "2M"));

        assertTrue(MarketComparePage.onlyDifferences(rows).isEmpty());
    }

    private static Row section(String label) {
        return new Row(label, null, true, false, List.of());
    }

    /** A row with one cell per offer, each cell keyed by its value. */
    private static Row row(String label, String... keys) {
        List<Cell> cells = new ArrayList<>();
        for (String key : keys) {
            cells.add(new Cell(key, key, null, false, -1, null, 0, null));
        }
        return new Row(label, null, false, false, cells);
    }

    private static List<String> labels(List<Row> rows) {
        return rows.stream().map(Row::label).toList();
    }
}
