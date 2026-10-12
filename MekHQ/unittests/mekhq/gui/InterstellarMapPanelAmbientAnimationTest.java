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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import org.junit.jupiter.api.Test;

class InterstellarMapPanelAmbientAnimationTest {
    private static final List<Point2D.Double> ROUTE = List.of(new Point2D.Double(0, 0),
          new Point2D.Double(30, 0), new Point2D.Double(30, 40));

    @Test
    void routeFlowPointFollowsLegsAndClampsToEnds() {
        assertEquals(new Point2D.Double(0, 0), InterstellarMapPanel.routeFlowPoint(ROUTE, -5));
        assertEquals(new Point2D.Double(15, 0), InterstellarMapPanel.routeFlowPoint(ROUTE, 15));
        assertEquals(new Point2D.Double(30, 10), InterstellarMapPanel.routeFlowPoint(ROUTE, 40));
        assertEquals(new Point2D.Double(30, 40), InterstellarMapPanel.routeFlowPoint(ROUTE, 500));
        assertEquals(70.0, InterstellarMapPanel.routeScreenLength(ROUTE), 1e-9);
    }

    @Test
    void routePulseWrapsAndItsRepaintBoundsCoverThePacket() {
        InterstellarMapPanel.RoutePulse pulse = new InterstellarMapPanel.RoutePulse(ROUTE, 70.0, 2.0, 0.0);

        assertEquals(35.0, pulse.headDistance(1.0), 1e-9);
        assertEquals(pulse.headDistance(0.5), pulse.headDistance(2.5), 1e-9);
        for (double seconds = 0.0; seconds < 2.0; seconds += 0.1) {
            Point2D.Double packet = InterstellarMapPanel.routeFlowPoint(ROUTE, pulse.headDistance(seconds));
            Rectangle bounds = pulse.bounds(seconds);
            assertTrue(bounds.contains(new Rectangle((int) Math.floor(packet.x - 4.2),
                  (int) Math.floor(packet.y - 4.2), 9, 9)), "packet glow escaped repaint bounds at " + seconds);
        }
    }

    @Test
    void routeFlowSpeedIsConstantInMapUnits() {
        double closePeriod = InterstellarMapPanel.routeFlowPeriodSeconds(100.0, 2.0);
        double farPeriod = InterstellarMapPanel.routeFlowPeriodSeconds(400.0, 8.0);
        assertEquals(closePeriod, farPeriod, 1e-9);
    }

    @Test
    void navigationLightsFlashBetweenDimAndBright() {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (double seconds = 0.0; seconds < 2.8; seconds += 0.01) {
            int alpha = InterstellarMapPanel.getNavigationLightAlpha(seconds, 2.8, 0.0);
            minimum = Math.min(minimum, alpha);
            maximum = Math.max(maximum, alpha);
        }
        assertEquals(35, minimum);
        assertTrue(maximum >= 230, "light must reach near full brightness, got " + maximum);
        assertNotEquals(InterstellarMapPanel.getNavigationLightState(0.05),
              InterstellarMapPanel.getNavigationLightState(1.5));
    }

    @Test
    void nextJumpProgressFollowsTheSlowerOfTransitAndRecharge() {
        // 6 days to the jump point, 7.33 days (176 h) of recharge: recharge sets the pace.
        assertEquals(0.0, InterstellarMapPanel.nextJumpProgress(0.0, 6.0, false, 0.0, 176.0, false), 1e-9);
        assertEquals(3.0 / 7.333, InterstellarMapPanel.nextJumpProgress(3.0, 6.0, false, 72.0, 176.0, false), 1e-3);
        assertEquals(1.0, InterstellarMapPanel.nextJumpProgress(6.0, 6.0, true, 176.0, 176.0, false), 1e-9);
        // Transit sets the pace when the drive is already charged.
        assertEquals(0.5, InterstellarMapPanel.nextJumpProgress(3.0, 6.0, false, 176.0, 176.0, false), 1e-9);
        // After a jump only the recharge remains.
        assertEquals(0.25, InterstellarMapPanel.nextJumpProgress(6.0, 6.0, true, 44.0, 176.0, false), 1e-9);
        // A stored battery charge removes the recharge wait.
        assertEquals(1.0, InterstellarMapPanel.nextJumpProgress(6.0, 6.0, true, 0.0, 176.0, true), 1e-9);
    }

    @Test
    void nextJumpRemainingDaysMatchesTheSlowerWait() {
        assertEquals(176.0 / 24.0, InterstellarMapPanel.nextJumpRemainingDays(0.0, 6.0, false, 0.0, 176.0, false),
              1e-9);
        assertEquals(3.0, InterstellarMapPanel.nextJumpRemainingDays(3.0, 6.0, false, 176.0, 176.0, false), 1e-9);
        assertEquals(5.5, InterstellarMapPanel.nextJumpRemainingDays(6.0, 6.0, true, 44.0, 176.0, false), 1e-9);
        assertEquals(0.0, InterstellarMapPanel.nextJumpRemainingDays(6.0, 6.0, true, 0.0, 176.0, true), 1e-9);
    }

    @Test
    void jumpChargeRingStaysClearOfTheSystemLabel() {
        for (double size : new double[] { 2.0, 4.0, 8.0, 14.0 }) {
            InterstellarMapPanel.SystemMarkerLayout layout = InterstellarMapPanel.SystemMarkerLayout.create(0.0,
                  0.0, size, InterstellarMapPanel.RouteMarkerState.NONE, false, false);
            double ringOuterEdge = InterstellarMapPanel.jumpChargeRingRadius(layout) + 1.6;
            assertTrue(ringOuterEdge > layout.ownershipRadius(), "ring hidden by ownership at size " + size);
            assertTrue(ringOuterEdge < layout.labelX(), "ring overlaps label at size " + size);
        }
    }
}
