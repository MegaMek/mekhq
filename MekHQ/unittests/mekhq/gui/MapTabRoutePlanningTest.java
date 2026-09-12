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

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.JumpPath;
import mekhq.campaign.NavigationRouteAnalysis.PathAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.RouteAlternativesPlanner.PlanningStatus;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class MapTabRoutePlanningTest {
    @Test
    void whatIfRouteIgnoresFleetProgressAndCannotBeginTransit() {
        PlanetarySystem fleetSystem = new PlanetarySystem("Fleet System");
        PlanetarySystem whatIfOrigin = new PlanetarySystem("What-If Origin");
        JumpPath proposedPath = path(whatIfOrigin, new PlanetarySystem("Destination"));

        assertTrue(MapTab.isWhatIfRoute(proposedPath, fleetSystem));
        assertEquals(0.0, MapTab.getProposedRouteTransitProgress(proposedPath, fleetSystem, 4.5));
        assertFalse(MapTab.canBeginTransit(proposedPath, fleetSystem, null, assessment(Severity.CLEAR)));
    }

    @Test
    void actualOriginUsesFleetProgressAndRestoresTransitEligibility() {
        PlanetarySystem fleetSystem = new PlanetarySystem("Fleet System");
        JumpPath proposedPath = path(fleetSystem, new PlanetarySystem("Destination"));

        assertFalse(MapTab.isWhatIfRoute(proposedPath, fleetSystem));
        assertEquals(4.5, MapTab.getProposedRouteTransitProgress(proposedPath, fleetSystem, 4.5));
        assertTrue(MapTab.canBeginTransit(proposedPath, fleetSystem, null, assessment(Severity.CLEAR)));
    }

    @Test
    void activeTransitPreventsStartingAProposedRoute() {
        PlanetarySystem fleetSystem = new PlanetarySystem("Fleet System");
        JumpPath proposedPath = path(fleetSystem, new PlanetarySystem("Proposed Destination"));
        JumpPath activePath = path(fleetSystem, new PlanetarySystem("Active Destination"));

        assertFalse(MapTab.canBeginTransit(proposedPath, fleetSystem, activePath, assessment(Severity.CLEAR)));
    }

    @Test
    void routeAssessmentBlocksTransitButAllowsCautionOnlyRoutes() {
        PlanetarySystem fleetSystem = new PlanetarySystem("Fleet System");
        JumpPath proposedPath = path(fleetSystem, new PlanetarySystem("Destination"));

        assertFalse(MapTab.canBeginTransit(proposedPath, fleetSystem, null, null));
        assertFalse(MapTab.canBeginTransit(proposedPath, fleetSystem, null, assessment(Severity.BLOCKED)));
        assertTrue(MapTab.canBeginTransit(proposedPath, fleetSystem, null, assessment(Severity.CAUTION)));
    }

    @Test
    void quickPlotRequiresOnlyADifferentSelectedSystem() {
        PlanetarySystem fleetSystem = new PlanetarySystem("Fleet System");
        PlanetarySystem destination = new PlanetarySystem("Destination");

        assertTrue(MapTab.canQuickPlotRoute(destination, fleetSystem));
        assertFalse(MapTab.canQuickPlotRoute(null, fleetSystem));
        assertFalse(MapTab.canQuickPlotRoute(destination, null));
        assertFalse(MapTab.canQuickPlotRoute(fleetSystem, fleetSystem));
    }

    @Test
    void quickPlotLabelChangesWhenReplanningAnExistingCourse() {
        assertEquals("routeStrip.plot.text", MapTab.quickPlotResourceKey(false));
        assertEquals("routeStrip.replot.text", MapTab.quickPlotResourceKey(true));
    }

    @Test
    void failedPlanningDispatchesExplicitFeedbackWithoutNotifyingForSuccess() {
        List<PlanningStatus> feedback = new ArrayList<>();

        MapTab.dispatchRoutePlanningFeedback(RoutePlanningIntent.ChangeResult.CHANGED, feedback::add);
        MapTab.dispatchRoutePlanningFeedback(RoutePlanningIntent.ChangeResult.ACCESS_DENIED, feedback::add);
        MapTab.dispatchRoutePlanningFeedback(RoutePlanningIntent.ChangeResult.NO_ROUTE, feedback::add);

        assertEquals(List.of(PlanningStatus.ACCESS_DENIED, PlanningStatus.NO_ROUTE), feedback);
    }

    private static PathAssessment assessment(Severity severity) {
        return new PathAssessment(List.of(), severity);
    }

    private static JumpPath path(PlanetarySystem... systems) {
        JumpPath path = new JumpPath();
        for (PlanetarySystem system : systems) {
            path.addSystem(system);
        }
        return path;
    }
}
