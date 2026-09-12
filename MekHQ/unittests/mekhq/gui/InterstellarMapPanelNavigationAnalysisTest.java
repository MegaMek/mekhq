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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import mekhq.campaign.NavigationRouteAnalysis.LegAssessment;
import mekhq.campaign.NavigationRouteAnalysis.LegFacts;
import mekhq.campaign.NavigationRouteAnalysis.PathAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelNavigationAnalysisTest {
    @Test
    void measurementClicksAreConsumedOnlyWhileToolIsActive() {
        PlanetarySystem alpha = system("ALPHA");
        PlanetarySystem beta = system("BETA");
        PlanetarySystem gamma = system("GAMMA");

        InterstellarMapPanel.MeasurementClick inactive =
              InterstellarMapPanel.MeasurementState.inactive().click(alpha);
        InterstellarMapPanel.MeasurementClick first =
              InterstellarMapPanel.MeasurementState.active().click(alpha);
        InterstellarMapPanel.MeasurementClick blank = first.state().click(null);
        InterstellarMapPanel.MeasurementClick second = blank.state().click(beta);
        InterstellarMapPanel.MeasurementClick restart = second.state().click(gamma);

        assertFalse(inactive.consumed());
        assertTrue(first.consumed());
        assertEquals(alpha, blank.state().start());
        assertEquals(null, blank.state().end());
        assertEquals(alpha, second.state().start());
        assertEquals(beta, second.state().end());
        assertEquals(gamma, restart.state().start());
        assertEquals(null, restart.state().end());
    }

    @Test
    void reachabilityStylesCarryShellAndConstraintMeaningByShape() {
        assertEquals(new InterstellarMapPanel.ReachabilityMarkerStyle(
                    InterstellarMapPanel.NavigationMarkerShape.CIRCLE,
                    InterstellarMapPanel.NavigationMarkerTone.IMMEDIATE),
              InterstellarMapPanel.reachabilityMarkerStyle(1, Severity.CLEAR, false));
        assertEquals(InterstellarMapPanel.NavigationMarkerShape.SQUARE,
              InterstellarMapPanel.reachabilityMarkerStyle(2, Severity.CLEAR, false).shape());
        assertEquals(InterstellarMapPanel.NavigationMarkerShape.HEXAGON,
              InterstellarMapPanel.reachabilityMarkerStyle(3, Severity.CLEAR, false).shape());
        assertEquals(InterstellarMapPanel.NavigationMarkerShape.TRIANGLE,
              InterstellarMapPanel.reachabilityMarkerStyle(1, Severity.CAUTION, false).shape());
        assertEquals(InterstellarMapPanel.NavigationMarkerShape.DIAMOND,
              InterstellarMapPanel.reachabilityMarkerStyle(1, Severity.CLEAR, true).shape());
    }

      @Test
      void everyNavigationShapeRespectsItsStrokedInnerClearance() {
            double clearance = 24.0;
            double strokeWidth = 2.2;
            for (InterstellarMapPanel.NavigationMarkerShape shape
                    : InterstellarMapPanel.NavigationMarkerShape.values()) {
                  double pathRadius = InterstellarMapPanel.navigationMarkerPathRadius(
                          shape, clearance, strokeWidth);
                  double pathInradius = switch (shape) {
                        case CIRCLE, SQUARE -> pathRadius;
                        case TRIANGLE -> pathRadius * Math.cos(Math.PI / 3.0);
                        case DIAMOND -> pathRadius * Math.cos(Math.PI / 4.0);
                        case HEXAGON -> pathRadius * Math.cos(Math.PI / 6.0);
                  };

                  assertEquals(clearance, pathInradius - (strokeWidth / 2.0), 0.000_001, shape.name());
            }
      }

      @Test
      void blockedReachabilityStrokeUsesValidMiterLimit() {
            BasicStroke stroke = InterstellarMapPanel.reachabilityMarkerStroke(
                    InterstellarMapPanel.NavigationMarkerTone.BLOCKED);

            assertTrue(stroke.getMiterLimit() >= 1.0f);
      }

    @Test
    void routeWarningsAreAssociatedWithTheirDestinationLegs() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem caution = system("CAUTION");
        PlanetarySystem blocked = system("BLOCKED");
        PathAssessment assessment = new PathAssessment(List.of(
              assessment(origin, caution, Severity.CAUTION),
              assessment(caution, blocked, Severity.BLOCKED)), Severity.BLOCKED);

        List<InterstellarMapPanel.RouteConstraintMarker> markers =
              InterstellarMapPanel.routeConstraintMarkers(List.of(origin, caution, blocked), assessment);

        assertEquals(List.of(caution, blocked), markers.stream()
                                                    .map(InterstellarMapPanel.RouteConstraintMarker::destination)
                                                    .toList());
        assertEquals(List.of(false, true), markers.stream()
                                             .map(InterstellarMapPanel.RouteConstraintMarker::brokenSegment)
                                             .toList());
    }

    @Test
    void measurementLabelClampsInsideViewportAndAboveMapControls() {
        Rectangle viewport = new Rectangle(0, 0, 800, 600);
        Rectangle navigationInstrument = new Rectangle(10, 470, 280, 118);
        Rectangle layerDrawer = new Rectangle(600, 400, 190, 188);

        Rectangle leftLabel = InterstellarMapPanel.clampMeasurementLabel(viewport,
              new Dimension(260, 30), new Point(80, 580), List.of(navigationInstrument, layerDrawer));
        Rectangle rightLabel = InterstellarMapPanel.clampMeasurementLabel(viewport,
              new Dimension(260, 30), new Point(760, 580), List.of(navigationInstrument, layerDrawer));

        assertTrue(viewport.contains(leftLabel));
        assertTrue(viewport.contains(rightLabel));
        assertFalse(leftLabel.intersects(navigationInstrument));
        assertFalse(rightLabel.intersects(layerDrawer));
    }

    private static LegAssessment assessment(PlanetarySystem origin, PlanetarySystem destination,
          Severity severity) {
        return new LegAssessment(origin, destination,
              new LegFacts(20.0, 1, severity != Severity.BLOCKED, severity == Severity.CAUTION,
                    72.0, 0, 168.0, false),
              List.of(), severity);
    }

    private static PlanetarySystem system(String id) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        return system;
    }
}
