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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InterstellarMapPanelMarkerLayoutTest {
    private static final double CENTER_X = 137.25;
    private static final double CENTER_Y = 82.75;
    private static final double DELTA = 0.000_001;

    @ParameterizedTest
    @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
    void hpgStationUsesLeftMiddleSlotClearOfSystemAndName(double markerSize) {
        for (InterstellarMapPanel.RouteMarkerState routeState : InterstellarMapPanel.RouteMarkerState.values()) {
            InterstellarMapPanel.SystemMarkerLayout layout = createLayout(markerSize, routeState, false, false);
            double radius = 7.0;
            Point2D.Double anchor = layout.hpgStationAnchor(radius);

            assertEquals(CENTER_Y, anchor.y, DELTA);
            assertTrue(anchor.x + radius < CENTER_X - layout.externalOrbitRadius());
            assertTrue(anchor.x + radius < layout.labelX());
            assertTrue(anchor.y > layout.operationAnchor().y + radius);
            assertTrue(anchor.y < layout.playerBaseAnchor(radius, 1.0).y - radius);
        }
    }

        @Test
        void systemDiveInterpolatesCameraExactlyAndZoomsMonotonically() {
          InterstellarMapPanel.SystemDiveFrame start = InterstellarMapPanel.calculateSystemDiveFrame(
              120.0, -45.0, 1.5, -300.0, 90.0, 18.0, 0.0);
          InterstellarMapPanel.SystemDiveFrame middle = InterstellarMapPanel.calculateSystemDiveFrame(
              120.0, -45.0, 1.5, -300.0, 90.0, 18.0, 0.5);
          InterstellarMapPanel.SystemDiveFrame end = InterstellarMapPanel.calculateSystemDiveFrame(
              120.0, -45.0, 1.5, -300.0, 90.0, 18.0, 1.0);

          assertEquals(120.0, start.centerX(), DELTA);
          assertEquals(-45.0, start.centerY(), DELTA);
          assertEquals(1.5, start.scale(), DELTA);
          assertTrue(middle.centerX() < start.centerX());
          assertTrue(middle.centerX() > end.centerX());
          assertTrue(middle.centerY() > start.centerY());
          assertTrue(middle.centerY() < end.centerY());
          assertTrue(middle.scale() > start.scale());
          assertTrue(middle.scale() < end.scale());
          assertEquals(-300.0, end.centerX(), DELTA);
          assertEquals(90.0, end.centerY(), DELTA);
          assertEquals(18.0, end.scale(), DELTA);
        }

    @Test
    void systemDiveInterpolationRestoresCameraWhenReversed() {
        InterstellarMapPanel.SystemDiveFrame original = new InterstellarMapPanel.SystemDiveFrame(120, -45, 1.5);
        InterstellarMapPanel.SystemDiveFrame zoomed = new InterstellarMapPanel.SystemDiveFrame(-300, 90, 18);

        InterstellarMapPanel.SystemDiveFrame restored = InterstellarMapPanel.calculateSystemDiveFrame(
              zoomed.centerX(), zoomed.centerY(), zoomed.scale(),
              original.centerX(), original.centerY(), original.scale(), 1.0);

          assertEquals(original.centerX(), restored.centerX(), DELTA);
          assertEquals(original.centerY(), restored.centerY(), DELTA);
          assertEquals(original.scale(), restored.scale(), DELTA);
    }

    @Test
        void factionOwnershipRingCoversFullCircleAndDividesSharedSystemsEqually() {
        Color red = new Color(232, 112, 84);
        Color blue = new Color(88, 170, 230);
        Color green = new Color(114, 196, 126);
          BufferedImage singleFaction = renderOwnershipRing(List.of(red));
          for (double angle : new double[] { 0, 90, 180, 270 }) {
            assertEquals(red.getRGB(), sampleRing(singleFaction, angle));
          }

          BufferedImage shared = renderOwnershipRing(List.of(red, blue, green));
          assertEquals(red.getRGB(), sampleRing(shared, 30));
          assertEquals(blue.getRGB(), sampleRing(shared, -90));
          assertEquals(green.getRGB(), sampleRing(shared, 150));
          for (double angle : new double[] { 0, 60, 120, 180, 240, 300 }) {
            assertTrue((sampleRing(shared, angle) >>> 24) > 0,
                "ownership ring must have no angular gaps");
          }
        }

        @Test
        void analyticalLayerRingCoversFullCircle() {
          Color layerColor = new Color(33, 144, 140);
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
          InterstellarMapPanel.drawAnalyticalOverlay(graphics, new Arc2D.Double(),
              InterstellarMapPanel.SystemMarkerLayout.create(32, 32, 7.2,
                    InterstellarMapPanel.RouteMarkerState.NONE, false, false),
              layerColor, 1.0);
        graphics.dispose();

          for (double angle : new double[] { 0, 45, 90, 135, 180, 225, 270, 315 }) {
            assertEquals(layerColor.getRGB(), sampleRing(image, angle));
          }
        }

        @Test
        void strategicContactUsesOnlyFactionOrLayerCoreWithoutOuterShell() {
          Color contactColor = new Color(190, 66, 160);
          BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
          Graphics2D graphics = image.createGraphics();
          InterstellarMapPanel.drawStrategicContactCore(graphics, new Arc2D.Double(),
              InterstellarMapPanel.SystemMarkerLayout.create(32, 32, 7.2,
                  InterstellarMapPanel.RouteMarkerState.NONE, false, false),
              List.of(contactColor));
          graphics.dispose();

          assertEquals(contactColor.getRGB(), image.getRGB(32, 32));
          assertEquals(0, image.getRGB(36, 32) >>> 24,
              "atlas and navigation contacts must not retain the detail ring");
    }

    @ParameterizedTest
    @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
        void positionedMarkersFollowTheOutermostVisibleEnvelope(double markerSize) {
          InterstellarMapPanel.SystemMarkerLayout plain = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, false, false);
          InterstellarMapPanel.SystemMarkerLayout selected = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, true, false);
          InterstellarMapPanel.SystemMarkerLayout hovered = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, false, true);
          InterstellarMapPanel.SystemMarkerLayout planned = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.PLANNED, false, false);
          InterstellarMapPanel.SystemMarkerLayout active = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.ACTIVE, false, false);

        assertEquals(plain.externalOrbitRadius(), selected.externalOrbitRadius(), DELTA);
        assertEquals(plain.externalOrbitRadius(), hovered.externalOrbitRadius(), DELTA);
        assertEquals(plain.hoveredRadius() + 0.55, plain.orthogonalExternalOrbitRadius(), DELTA);
        assertEquals(plain.orthogonalExternalOrbitRadius(), selected.orthogonalExternalOrbitRadius(), DELTA);
        assertEquals(plain.orthogonalExternalOrbitRadius(), hovered.orthogonalExternalOrbitRadius(), DELTA);
          assertTrue(hovered.externalOrbitRadius() < planned.externalOrbitRadius());
          assertEquals(planned.externalOrbitRadius(), active.externalOrbitRadius(), DELTA);
        assertEquals(planned.externalOrbitRadius(), planned.orthogonalExternalOrbitRadius(), DELTA);
        assertEquals(plain.capitalAnchor(0, 1).y, selected.capitalAnchor(0, 1).y, DELTA);
        assertEquals(plain.capitalAnchor(0, 1).y, hovered.capitalAnchor(0, 1).y, DELTA);
        assertEquals(plain.gmEditedAnchor().y, selected.gmEditedAnchor().y, DELTA);
        assertEquals(plain.gmEditedAnchor().y, hovered.gmEditedAnchor().y, DELTA);
          assertTrue(plain.capitalAnchor(0, 1).y > planned.capitalAnchor(0, 1).y,
              "a capital moves inward when no navigation wrapper is visible");
          assertTrue(plain.gmEditedAnchor().y < planned.gmEditedAnchor().y,
              "the GM pencil moves inward when no navigation wrapper is visible");
    }

        @ParameterizedTest
        @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
        void zoomExpandedMarkersStayAttachedUntilTheyReachDetailedSlots(double markerSize) {
          InterstellarMapPanel.SystemMarkerLayout layout = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, false, false);

          assertAnchorExpansion(layout.capitalAnchor(0, 1, 0.0), layout.capitalAnchor(0, 1, 0.5),
              layout.capitalAnchor(0, 1));
          assertAnchorExpansion(layout.operationAnchor(0.0), layout.operationAnchor(0.5),
              layout.operationAnchor());
          Point2D.Double compactBase = layout.playerBaseAnchor(6.0, 0.0);
          Point2D.Double transitionBase = layout.playerBaseAnchor(6.0, 0.5);
          Point2D.Double detailedBase = layout.playerBaseAnchor(6.0, 1.0);
          assertTrue(compactBase.x < CENTER_X);
          assertTrue(compactBase.y > CENTER_Y);
          assertTrue(transitionBase.x < compactBase.x);
          assertTrue(transitionBase.y > compactBase.y);
          assertTrue(detailedBase.x < transitionBase.x);
          assertTrue(detailedBase.y > transitionBase.y);
          assertAnchorExpansion(layout.shipAnchor(0.0), layout.shipAnchor(0.5), layout.shipAnchor());
          assertAnchorExpansion(layout.gmEditedAnchor(0.0), layout.gmEditedAnchor(0.5),
              layout.gmEditedAnchor());
          assertAnchorExpansion(layout.routeBadgeAnchor(18.0, 0.0), layout.routeBadgeAnchor(18.0, 0.5),
              layout.routeBadgeAnchor(18.0));
          assertAnchorExpansion(layout.routeStatusAnchor(6.0, 0.0), layout.routeStatusAnchor(6.0, 0.5),
              layout.routeStatusAnchor(6.0));
        }

    @ParameterizedTest
    @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
    void restrictedMarkerGrowsMonotonicallyFromAtlasToDetail(double markerSize) {
        InterstellarMapPanel.SystemMarkerLayout layout = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, false, false);
        double atlasRadius = InterstellarMapPanel.restrictedMarkerRadius(layout, 0.0);
        double transitionRadius = InterstellarMapPanel.restrictedMarkerRadius(layout, 0.5);
        double detailRadius = InterstellarMapPanel.restrictedMarkerRadius(layout, 1.0);

        assertTrue(atlasRadius < transitionRadius);
        assertTrue(transitionRadius < detailRadius);
        assertEquals(layout.overrideRadius(), detailRadius, DELTA);
    }

    @ParameterizedTest
    @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
    void selectionHoverAndNavigationUseCompactNonOverlappingOrbits(double markerSize) {
        InterstellarMapPanel.SystemMarkerLayout none = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.NONE, false, false);
        InterstellarMapPanel.SystemMarkerLayout planned = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.PLANNED, false, false);
        InterstellarMapPanel.SystemMarkerLayout active = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.ACTIVE, false, false);

        assertEquals(0.0, none.navigationRadius(), DELTA);
            assertEquals(3.0, planned.selectedRadius() - planned.ownershipRadius(), DELTA);
            assertEquals(3.0, planned.hoveredRadius() - planned.selectedRadius(), DELTA);
            double hoverBracketEnvelope = Math.hypot(planned.hoveredRadius(), planned.hoveredRadius()) + 0.55;
            assertEquals(3.0, planned.navigationRadius() - 1.25 - hoverBracketEnvelope, DELTA);
            assertEquals(planned.navigationRadius(), active.navigationRadius(), DELTA);
    }

    @ParameterizedTest
    @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
    void sharedCapitalBandStaysAtTwelveOClockAndClearOfJumpShip(double markerSize) {
        InterstellarMapPanel.SystemMarkerLayout layout = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.ACTIVE, true, true);
        Point2D.Double ship = layout.shipAnchor();
        Rectangle2D.Double shipBounds = new Rectangle2D.Double(ship.x - 17.0, ship.y - 17.0, 34.0, 34.0);
          assertEquals(CENTER_X, layout.capitalAnchor(0, 1).x, DELTA,
              "a single capital stays horizontally centered over its system");

        for (int markerCount : new int[] { 2, 3 }) {
            Rectangle2D.Double previousBounds = null;
            double capitalY = layout.capitalAnchor(0, markerCount).y;
            for (int markerIndex = 0; markerIndex < markerCount; markerIndex++) {
                Point2D.Double capital = layout.capitalAnchor(markerIndex, markerCount);
                    Rectangle2D.Double capitalBounds = InterstellarMapPanel.capitalBandMarkerBounds(
                      capital, markerSize);

                assertEquals(capitalY, capital.y, DELTA);
                assertTrue(capital.y < CENTER_Y);
                assertFalse(capitalBounds.intersects(shipBounds),
                      "top-center national capitals must not enter the top-right JumpShip slot");
                if (previousBounds != null) {
                    assertFalse(previousBounds.intersects(capitalBounds),
                          "shared national-capital symbols must not overlap");
                }
                previousBounds = capitalBounds;
            }
        }
    }

        @ParameterizedTest
        @ValueSource(doubles = { 3.0, 7.5, 12.0, 18.0, 25.0 })
        void positionedMarkersUseDistinctOuterSlotsAtEveryZoom(double markerSize) {
          InterstellarMapPanel.SystemMarkerLayout layout = createLayout(markerSize,
              InterstellarMapPanel.RouteMarkerState.PLANNED, false, false);
          double waypointRadius = 9.0;
          double routeStatusRadius = Math.max(5.0, markerSize * 0.9);
          Point2D.Double waypoint = layout.routeBadgeAnchor(18.0);
          Point2D.Double routeStatus = layout.routeStatusAnchor(routeStatusRadius);

          assertTrue(layout.capitalAnchor(0, 1).y < CENTER_Y, "capital stays above");
          assertTrue(layout.operationAnchor().x < CENTER_X, "operation stays upper-left");
          assertTrue(layout.operationAnchor().y < CENTER_Y, "operation stays upper-left");
          assertTrue(layout.shipAnchor().x > CENTER_X, "fleet stays upper-right");
          assertTrue(layout.shipAnchor().y < CENTER_Y, "fleet stays upper-right");
          assertTrue(waypoint.x > CENTER_X, "waypoint stays lower-right");
          assertTrue(waypoint.y > CENTER_Y, "waypoint stays lower-right");
          assertTrue(routeStatus.x > CENTER_X, "route status stays right");
          assertEquals(CENTER_Y, routeStatus.y, DELTA, "route status stays at right-middle");
          assertTrue(routeStatus.y < waypoint.y, "route status stays above the waypoint slot");
          assertTrue(Point2D.distance(routeStatus.x, routeStatus.y, waypoint.x, waypoint.y)
              > routeStatusRadius + waypointRadius + 3.0,
              "route status and waypoint slots must not overlap");
          assertEquals(routeStatus.x + routeStatusRadius + 3.0,
              layout.routeStatusLabelX(routeStatusRadius), DELTA,
              "warned destination labels start beyond the route-status marker");
        }

    private static InterstellarMapPanel.SystemMarkerLayout createLayout(double markerSize,
          InterstellarMapPanel.RouteMarkerState routeState, boolean selected, boolean hovered) {
        return InterstellarMapPanel.SystemMarkerLayout.create(CENTER_X, CENTER_Y, markerSize,
              routeState, selected, hovered);
    }

        private static void assertAnchorExpansion(Point2D.Double atlas, Point2D.Double transition,
            Point2D.Double detail) {
          assertEquals(CENTER_X, atlas.x, DELTA);
          assertEquals(CENTER_Y, atlas.y, DELTA);
          assertEquals(Point2D.distance(CENTER_X, CENTER_Y, detail.x, detail.y) / 2.0,
              Point2D.distance(CENTER_X, CENTER_Y, transition.x, transition.y), DELTA);
        }

    @Test
    void waypointGlyphsAreOpticallyCenteredInTheirBadge() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));

        for (String text : List.of("1", "2", "8", "10", "12")) {
            Point2D.Double baseline = InterstellarMapPanel.centeredGlyphBaseline(graphics, text, 32, 32);
            Rectangle2D visualBounds = graphics.getFont()
                  .createGlyphVector(graphics.getFontRenderContext(), text)
                  .getVisualBounds();
            Rectangle2D positionedBounds = new Rectangle2D.Double(
                  baseline.x + visualBounds.getX(), baseline.y + visualBounds.getY(),
                  visualBounds.getWidth(), visualBounds.getHeight());

            assertEquals(32.0, positionedBounds.getCenterX(), DELTA, text + " horizontal center");
            assertEquals(32.0, positionedBounds.getCenterY(), DELTA, text + " vertical center");
        }
        graphics.dispose();
    }

    private static BufferedImage renderOwnershipRing(List<Color> colors) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        InterstellarMapPanel.drawFactionOwnershipRing(graphics, new Arc2D.Double(),
              InterstellarMapPanel.SystemMarkerLayout.create(32, 32, 7.2,
                    InterstellarMapPanel.RouteMarkerState.NONE, false, false), colors);
        graphics.dispose();
        return image;
    }

    private static int sampleRing(BufferedImage image, double angleDegrees) {
        double radians = Math.toRadians(angleDegrees);
        int x = 32 + (int) Math.round(10 * Math.cos(radians));
        int y = 32 - (int) Math.round(10 * Math.sin(radians));
        return image.getRGB(x, y);
    }

}
