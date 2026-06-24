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
package mekhq.gui.baseComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;

import mekhq.gui.baseComponents.SegmentedBar.Segment;
import org.junit.jupiter.api.Test;

/**
 * Tests for the pure static factory and color helpers of {@link SegmentedBar}, with an emphasis on the argument
 * validation those public methods are expected to perform.
 */
class SegmentedBarTest {
    private static final Color[] BLACK_TO_WHITE = { Color.BLACK, Color.WHITE };
    private static final String[] THREE_TOOLTIPS = { "a", "b", "c" };

    // region gradientSegments(Color[], String[])

    @Test
    void gradientSegmentsRejectsNullAnchors() {
        assertThrows(NullPointerException.class, () -> SegmentedBar.gradientSegments(null, THREE_TOOLTIPS));
    }

    @Test
    void gradientSegmentsRejectsNullTooltips() {
        assertThrows(NullPointerException.class, () -> SegmentedBar.gradientSegments(BLACK_TO_WHITE, null));
    }

    @Test
    void gradientSegmentsRejectsEmptyAnchors() {
        assertThrows(IllegalArgumentException.class,
              () -> SegmentedBar.gradientSegments(new Color[0], THREE_TOOLTIPS));
    }

    @Test
    void gradientSegmentsWithEmptyTooltipsReturnsEmptyList() {
        final List<Segment> segments = SegmentedBar.gradientSegments(BLACK_TO_WHITE, new String[0]);
        assertTrue(segments.isEmpty());
    }

    @Test
    void gradientSegmentsProducesOneSegmentPerTooltipAtEndpoints() {
        final List<Segment> segments = SegmentedBar.gradientSegments(BLACK_TO_WHITE, THREE_TOOLTIPS);

        assertEquals(THREE_TOOLTIPS.length, segments.size());
        // The first and last segments land exactly on the gradient endpoints.
        assertEquals(Color.BLACK, segments.get(0).color());
        assertEquals(Color.WHITE, segments.get(segments.size() - 1).color());
        // The midpoint of black -> white is mid-gray.
        assertEquals(new Color(128, 128, 128), segments.get(1).color());
    }

    @Test
    void gradientSegmentsPreservesTooltipsIncludingNullEntries() {
        final String[] tooltips = { "first", null, "third" };
        final List<Segment> segments = SegmentedBar.gradientSegments(BLACK_TO_WHITE, tooltips);

        assertEquals("first", segments.get(0).tooltip());
        assertNull(segments.get(1).tooltip());
        assertEquals("third", segments.get(2).tooltip());
    }

    @Test
    void gradientSegmentsWithSingleAnchorUsesThatColorForEverySegment() {
        final List<Segment> segments = SegmentedBar.gradientSegments(new Color[] { Color.RED }, THREE_TOOLTIPS);

        for (final Segment segment : segments) {
            assertEquals(Color.RED, segment.color());
        }
    }

    // endregion gradientSegments(Color[], String[])

    // region gradientSegments(Color, Color, String[])

    @Test
    void gradientSegmentsStartEndRejectsNullStart() {
        assertThrows(NullPointerException.class,
              () -> SegmentedBar.gradientSegments(null, Color.WHITE, THREE_TOOLTIPS));
    }

    @Test
    void gradientSegmentsStartEndRejectsNullEnd() {
        assertThrows(NullPointerException.class,
              () -> SegmentedBar.gradientSegments(Color.BLACK, null, THREE_TOOLTIPS));
    }

    @Test
    void gradientSegmentsStartEndRejectsNullTooltips() {
        assertThrows(NullPointerException.class,
              () -> SegmentedBar.gradientSegments(Color.BLACK, Color.WHITE, null));
    }

    @Test
    void gradientSegmentsStartEndInterpolatesBetweenTheTwoColors() {
        final List<Segment> segments = SegmentedBar.gradientSegments(Color.BLACK, Color.WHITE, THREE_TOOLTIPS);

        assertEquals(Color.BLACK, segments.get(0).color());
        assertEquals(new Color(128, 128, 128), segments.get(1).color());
        assertEquals(Color.WHITE, segments.get(2).color());
    }

    // endregion gradientSegments(Color, Color, String[])

    // region interpolatedColor

    @Test
    void interpolatedColorRejectsNullAnchors() {
        assertThrows(NullPointerException.class, () -> SegmentedBar.interpolatedColor(null, 3, 0));
    }

    @Test
    void interpolatedColorRejectsEmptyAnchors() {
        assertThrows(IllegalArgumentException.class, () -> SegmentedBar.interpolatedColor(new Color[0], 3, 0));
    }

    @Test
    void interpolatedColorRejectsNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 3, -1));
    }

    @Test
    void interpolatedColorRejectsIndexEqualToCount() {
        assertThrows(IllegalArgumentException.class, () -> SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 3, 3));
    }

    @Test
    void interpolatedColorWithSingleSegmentReturnsFirstAnchor() {
        assertEquals(Color.BLACK, SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 1, 0));
    }

    @Test
    void interpolatedColorReturnsEndpointsAndMidpoint() {
        assertEquals(Color.BLACK, SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 3, 0));
        assertEquals(new Color(128, 128, 128), SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 3, 1));
        assertEquals(Color.WHITE, SegmentedBar.interpolatedColor(BLACK_TO_WHITE, 3, 2));
    }

    // endregion interpolatedColor
}
