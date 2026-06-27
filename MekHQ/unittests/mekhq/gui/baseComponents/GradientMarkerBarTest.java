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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;

import mekhq.gui.baseComponents.GradientMarkerBar.Marker;
import mekhq.gui.baseComponents.GradientMarkerBar.MarkerStyle;
import org.junit.jupiter.api.Test;

/**
 * Tests for the value-to-pixel mapping and gradient math of {@link GradientMarkerBar}. Assertions are written to be
 * independent of the active GUI scale factor: they check relationships (monotonicity, symmetry, proportional ordering)
 * rather than absolute pixel positions, since padding is scaled by {@code UIUtil.scaleForGUI}.
 */
class GradientMarkerBarTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 40;
    private static final Color[] RED_TO_GREEN = { Color.RED, Color.GREEN };

    private static GradientMarkerBar sizedBar() {
        final GradientMarkerBar bar = new GradientMarkerBar();
        bar.setSize(WIDTH, HEIGHT);
        return bar;
    }

    // region gradientFractions

    @Test
    void gradientFractionsForTwoAnchorsAreEndpoints() {
        assertArrayEquals(new float[] { 0.0f, 1.0f }, GradientMarkerBar.gradientFractions(2));
    }

    @Test
    void gradientFractionsForThreeAnchorsIncludeMidpoint() {
        assertArrayEquals(new float[] { 0.0f, 0.5f, 1.0f }, GradientMarkerBar.gradientFractions(3));
    }

    @Test
    void gradientFractionsForFiveAnchorsAreEvenlySpaced() {
        assertArrayEquals(new float[] { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f }, GradientMarkerBar.gradientFractions(5));
    }

    // endregion gradientFractions

    // region valueToX - basic behavior

    @Test
    void valueToXWithoutSizeReturnsZero() {
        final GradientMarkerBar bar = new GradientMarkerBar();
        bar.setGradientRange(0, 10);
        assertEquals(0, bar.valueToX(5));
    }

    @Test
    void valueToXIsMonotonicAcrossRange() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 100);

        int previous = bar.valueToX(0);
        for (int value = 10; value <= 100; value += 10) {
            final int current = bar.valueToX(value);
            assertTrue(current > previous, "valueToX should increase with value (" + value + ")");
            previous = current;
        }
    }

    @Test
    void valueToXMapsMidpointToCenterOfGradientZone() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);

        final int left = bar.valueToX(0);
        final int right = bar.valueToX(10);
        final int mid = bar.valueToX(5);
        // The midpoint value lands at the center of the gradient zone, regardless of scale.
        assertEquals((left + right) / 2.0, mid, 1.0);
    }

    @Test
    void valueToXIsLinearWithinRange() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 100);

        final int origin = bar.valueToX(0);
        final int quarter = bar.valueToX(25) - origin;
        final int half = bar.valueToX(50) - origin;
        // A quarter of the value range covers half the distance of the midpoint, within rounding.
        assertEquals(half / 2.0, quarter, 1.0);
    }

    // endregion valueToX - basic behavior

    // region valueToX - proportional out-of-gradient handling

    @Test
    void displayRangeMatchesGradientRangeWithoutMarkers() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        assertEquals(0.0, bar.displayMin());
        assertEquals(10.0, bar.displayMax());
    }

    @Test
    void markerBelowGradientExpandsDisplayMin() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setMarkers(List.of(new Marker(-4, null, Color.WHITE, MarkerStyle.SOLID, false)));
        assertEquals(-4.0, bar.displayMin());
        assertEquals(10.0, bar.displayMax());
    }

    @Test
    void markerAboveGradientExpandsDisplayMax() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setMarkers(List.of(new Marker(18, null, Color.WHITE, MarkerStyle.SOLID, false)));
        assertEquals(0.0, bar.displayMin());
        assertEquals(18.0, bar.displayMax());
    }

    @Test
    void inRangeMarkersDoNotExpandDisplayRange() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setMarkers(List.of(new Marker(3, null, Color.WHITE, MarkerStyle.SOLID, false),
              new Marker(10, null, Color.WHITE, MarkerStyle.TICK, true)));
        assertEquals(0.0, bar.displayMin());
        assertEquals(10.0, bar.displayMax());
    }

    @Test
    void overflowMarkersArePlacedProportionallyNotPinned() {
        // The whole point of the redesign: a larger overflow sits further right than a smaller one, rather than both
        // pinning to a fixed-width cap.
        final GradientMarkerBar smallOverflow = sizedBar();
        smallOverflow.setGradientRange(0, 10);
        smallOverflow.setMarkers(List.of(new Marker(12, null, Color.WHITE, MarkerStyle.SOLID, false)));

        final GradientMarkerBar largeOverflow = sizedBar();
        largeOverflow.setGradientRange(0, 10);
        largeOverflow.setMarkers(List.of(new Marker(40, null, Color.WHITE, MarkerStyle.SOLID, false)));

        // Within a bar, the gradient end (10) sits left of the overflowing value...
        assertTrue(smallOverflow.valueToX(10) < smallOverflow.valueToX(12));
        // ...and the larger the overflow, the more the gradient end is compressed toward the left.
        assertTrue(largeOverflow.valueToX(10) < smallOverflow.valueToX(10));
    }

    @Test
    void valueToXIsProportionalAcrossAnExpandedRange() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setMarkers(List.of(new Marker(-10, null, Color.WHITE, MarkerStyle.SOLID, false),
              new Marker(10, null, Color.WHITE, MarkerStyle.TICK, true)));
        // The display range is now [-10, 10], so zero should land at the center.
        final int left = bar.valueToX(-10);
        final int right = bar.valueToX(10);
        assertEquals((left + right) / 2.0, bar.valueToX(0), 1.0);
    }

    @Test
    void outOfGradientColorsDoNotChangeGeometry() {
        final GradientMarkerBar withoutColors = sizedBar();
        withoutColors.setGradientRange(0, 10);
        withoutColors.setMarkers(List.of(new Marker(-5, null, Color.WHITE, MarkerStyle.SOLID, false)));

        final GradientMarkerBar withColors = sizedBar();
        withColors.setGradientRange(0, 10);
        withColors.setMarkers(List.of(new Marker(-5, null, Color.WHITE, MarkerStyle.SOLID, false)));
        withColors.setOutOfGradientColors(Color.RED.darker(), Color.GREEN.darker());

        // The flat fills are purely cosmetic; they must not shift where values land.
        assertEquals(withoutColors.valueToX(0), withColors.valueToX(0));
        assertEquals(withoutColors.valueToX(10), withColors.valueToX(10));
        assertEquals(withoutColors.valueToX(-5), withColors.valueToX(-5));
    }

    // endregion valueToX - proportional out-of-gradient handling

    // region valueToX - degenerate ranges

    @Test
    void degenerateRangeDoesNotThrowAndReturnsGradientLeft() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(5, 5);

        final int x = assertDoesNotThrow(() -> bar.valueToX(5));
        assertEquals(bar.valueToX(0), x);
    }

    @Test
    void invertedRangeDoesNotThrow() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(10, 0);

        assertDoesNotThrow(() -> bar.valueToX(5));
    }

    @Test
    void narrowComponentDoesNotInvertMapping() {
        // A layout can size the component narrower than 2 x padding (minimum sizes are advisory). The value-to-x
        // mapping must not invert in that case: a higher value must never map to the left of a lower one.
        final GradientMarkerBar bar = new GradientMarkerBar();
        bar.setSize(4, HEIGHT);
        bar.setGradientRange(0, 10);

        final int low = bar.valueToX(0);
        final int high = bar.valueToX(10);
        assertTrue(high >= low, "narrow component should collapse the mapping, not invert it");
    }

    // endregion valueToX - degenerate ranges

    // region lenient setters

    @Test
    void nullMarkersAndGradientAreTreatedAsEmpty() {
        final GradientMarkerBar bar = sizedBar();
        assertDoesNotThrow(() -> {
            bar.setMarkers(null);
            bar.setGradient(null);
        });
    }

    @Test
    void markersListIsDefensivelyCopied() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        final java.util.List<Marker> source = new java.util.ArrayList<>();
        source.add(new Marker(5, null, Color.WHITE, MarkerStyle.SOLID, false));
        bar.setMarkers(source);

        final double maxBeforeMutation = bar.displayMax();

        // Mutating the original list after the call must not affect the component's derived state. Adding a marker
        // beyond the gradient range would extend displayMax if the bar had retained the same list reference.
        source.add(new Marker(100, null, Color.WHITE, MarkerStyle.SOLID, false));

        assertEquals(maxBeforeMutation, bar.displayMax(),
              "mutating the source list after setMarkers must not change the bar's state");
    }

    @Test
    void markerRecordRetainsItsFields() {
        final Marker marker = new Marker(7.5, "Goal", Color.WHITE, MarkerStyle.TICK, true);
        assertEquals(7.5, marker.value());
        assertEquals("Goal", marker.label());
        assertEquals(Color.WHITE, marker.color());
        assertEquals(MarkerStyle.TICK, marker.style());
        assertTrue(marker.labelAbove());
    }

    // endregion lenient setters

    @Test
    void singleMarkerSetIsAcceptedAndPositionedInRange() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setMarkers(List.of(new Marker(5, null, Color.WHITE, MarkerStyle.SOLID, false)));

        final int mid = bar.valueToX(5);
        assertTrue((mid > bar.valueToX(0)) && (mid < bar.valueToX(10)));
    }
}
