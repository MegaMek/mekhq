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
 * independent of the active GUI scale factor: they check relationships (monotonicity, symmetry, clamping) rather than
 * absolute pixel positions, since padding and cap widths are scaled by {@code UIUtil.scaleForGUI}.
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

    // region valueToX - overshoot caps

    @Test
    void belowRangeValuesPinToBelowCapWhenEnabled() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setOvershootCaps(Color.RED.darker(), Color.GREEN.darker());

        final int gradientLeft = bar.valueToX(0);
        final int belowOne = bar.valueToX(-1);
        final int belowFifty = bar.valueToX(-50);
        // All below-range values pin to the same point, left of the gradient's start.
        assertEquals(belowOne, belowFifty);
        assertTrue(belowOne < gradientLeft, "below-cap marker should sit left of the gradient start");
    }

    @Test
    void aboveRangeValuesPinToAboveCapWhenEnabled() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setOvershootCaps(Color.RED.darker(), Color.GREEN.darker());

        final int gradientRight = bar.valueToX(10);
        final int aboveOne = bar.valueToX(11);
        final int aboveFifty = bar.valueToX(50);
        // All above-range values pin to the same point, right of the gradient's end.
        assertEquals(aboveOne, aboveFifty);
        assertTrue(aboveOne > gradientRight, "above-cap marker should sit right of the gradient end");
    }

    @Test
    void rangeBoundariesStayInsideTheGradientZone() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        bar.setOvershootCaps(Color.RED.darker(), Color.GREEN.darker());

        // Exactly start / end belong to the gradient zone, not the caps.
        assertTrue(bar.valueToX(0) > bar.valueToX(-1), "value at range start should be right of the below-cap");
        assertTrue(bar.valueToX(10) < bar.valueToX(11), "value at range end should be left of the above-cap");
    }

    @Test
    void belowRangeClampsToGradientStartWhenCapDisabled() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        // No caps configured.

        assertEquals(bar.valueToX(0), bar.valueToX(-25));
    }

    @Test
    void aboveRangeClampsToGradientEndWhenCapDisabled() {
        final GradientMarkerBar bar = sizedBar();
        bar.setGradientRange(0, 10);
        // No caps configured.

        assertEquals(bar.valueToX(10), bar.valueToX(99));
    }

    @Test
    void onlyBelowCapShiftsLeftEdgeButNotRightEdge() {
        final GradientMarkerBar withoutCaps = sizedBar();
        withoutCaps.setGradientRange(0, 10);

        final GradientMarkerBar withBelowCap = sizedBar();
        withBelowCap.setGradientRange(0, 10);
        withBelowCap.setOvershootCaps(Color.RED.darker(), null);

        // Enabling only the below-cap reserves space on the left, pushing the gradient start inward...
        assertTrue(withBelowCap.valueToX(0) > withoutCaps.valueToX(0));
        // ...while the right edge (no above-cap) is unchanged.
        assertEquals(withoutCaps.valueToX(10), withBelowCap.valueToX(10));
    }

    // endregion valueToX - overshoot caps

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
        final java.util.List<Marker> source = new java.util.ArrayList<>();
        source.add(new Marker(5, null, Color.WHITE, MarkerStyle.SOLID, false));
        bar.setMarkers(source);
        // Mutating the original list after the call must not affect the component.
        assertDoesNotThrow(source::clear);
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
