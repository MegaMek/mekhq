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
package mekhq.campaign;

import static java.lang.Math.sqrt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mekhq.campaign.RouteAlternativesPlanner.Course;
import mekhq.campaign.RouteAlternativesPlanner.CourseKind;
import mekhq.campaign.RouteAlternativesPlanner.PlanningResult;
import mekhq.campaign.RouteAlternativesPlanner.PlanningStatus;
import mekhq.campaign.RouteAlternativesPlanner.RoutePolicy;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

class RouteAlternativesPlannerTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    @Test
    void immutableRequestedStopsPassValidationAndReachPlanning() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        PlanetarySystem destination = system("DESTINATION", 0.0);
        List<PlanetarySystem> requestedStops = List.of(destination);

        assertTrue(RouteAlternativesPlanner.isValidRequest(origin, requestedStops));
        List<Course> courses = RouteAlternativesPlanner.plan(origin, requestedStops, TEST_DATE, false,
              policy(Map.of(origin, List.of(destination))));

        assertEquals(List.of(origin, destination), courses.getFirst().systems());
    }

    @Test
    void routeRequestValidationRejectsNullInputsAndElements() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        List<PlanetarySystem> requestedStops = new ArrayList<>();
        requestedStops.add(null);

        assertFalse(RouteAlternativesPlanner.isValidRequest(null, List.of(origin)));
        assertFalse(RouteAlternativesPlanner.isValidRequest(origin, null));
        assertFalse(RouteAlternativesPlanner.isValidRequest(origin, List.of()));
        assertFalse(RouteAlternativesPlanner.isValidRequest(origin, requestedStops));
    }

    @Test
    void fastestAndFewestJumpsCanProduceDistinctCourses() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        PlanetarySystem shortRecharge = system("SHORT_RECHARGE", 100.0);
        PlanetarySystem quickOne = system("QUICK_ONE", 1.0);
        PlanetarySystem quickTwo = system("QUICK_TWO", 1.0);
        PlanetarySystem destination = system("DESTINATION", 0.0);
        RoutePolicy policy = policy(Map.of(
              origin, List.of(shortRecharge, quickOne),
              shortRecharge, List.of(destination),
              quickOne, List.of(quickTwo),
              quickTwo, List.of(destination)));

        List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false, policy);

        assertEquals(2, courses.size());
        assertEquals(CourseKind.FASTEST, courses.get(0).kind());
        assertEquals(List.of(origin, quickOne, quickTwo, destination), courses.get(0).systems());
        assertEquals(CourseKind.FEWEST_JUMPS, courses.get(1).kind());
        assertEquals(List.of(origin, shortRecharge, destination), courses.get(1).systems());
    }

        @Test
        void identicalCoursesAreDeduplicated() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);

          List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false,
              policy(Map.of(origin, List.of(destination))));

          assertEquals(1, courses.size());
          assertEquals(CourseKind.FASTEST, courses.getFirst().kind());
          assertEquals(List.of(origin, destination), courses.getFirst().systems());
        }

        @Test
        void equalScoresUseStableSystemIdPathTieBreaking() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem laterId = system("B_ROUTE", 5.0);
          PlanetarySystem earlierId = system("A_ROUTE", 5.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);
          RoutePolicy policy = policy(Map.of(
              origin, List.of(laterId, earlierId),
              laterId, List.of(destination),
              earlierId, List.of(destination)));

          for (int iteration = 0; iteration < 5; iteration++) {
            List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false,
                policy);
            assertEquals(List.of(origin, earlierId, destination), courses.getFirst().systems());
          }
        }

        @Test
        void requestedStopsAreConcatenatedWithoutDuplicateSegmentOrigins() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem firstIntermediate = system("FIRST_INTERMEDIATE", 1.0);
          PlanetarySystem requestedStop = system("REQUESTED_STOP", 1.0);
          PlanetarySystem secondIntermediate = system("SECOND_INTERMEDIATE", 1.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);
          RoutePolicy policy = policy(Map.of(
              origin, List.of(firstIntermediate),
              firstIntermediate, List.of(requestedStop),
              requestedStop, List.of(secondIntermediate),
              secondIntermediate, List.of(destination)));

          List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(requestedStop, destination), TEST_DATE,
              false, policy);

          assertEquals(List.of(origin, firstIntermediate, requestedStop, secondIntermediate, destination),
              courses.getFirst().systems());
        }

        @Test
        void disallowedAndInaccessibleSystemsAreFiltered() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem empty = system("EMPTY", 0.0);
          PlanetarySystem inaccessible = system("INACCESSIBLE", 0.0);
          PlanetarySystem clear = system("CLEAR", 0.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);
          Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
              origin, List.of(empty, inaccessible, clear),
              empty, List.of(destination),
              inaccessible, List.of(destination),
              clear, List.of(destination));
          RoutePolicy policy = policy(neighbors, Set.of(empty), Set.of(new Leg(origin, inaccessible)));

          List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false, policy);

          assertEquals(1, courses.size());
          assertEquals(List.of(origin, clear, destination), courses.getFirst().systems());
        }

    @Test
    void fastestPlanningUsesAStarEstimateToAvoidDeadEndBranches() {
        PlanetarySystem origin = positionedSystem("ORIGIN", 10.0, 0.0);
        PlanetarySystem firstStep = positionedSystem("FIRST_STEP", 10.0, 30.0);
        PlanetarySystem secondStep = positionedSystem("SECOND_STEP", 10.0, 60.0);
        PlanetarySystem destination = positionedSystem("DESTINATION", 0.0, 90.0);
        PlanetarySystem firstDeadEnd = positionedSystem("FIRST_DEAD_END", 10.0, -30.0);
        PlanetarySystem secondDeadEnd = positionedSystem("SECOND_DEAD_END", 10.0, -29.0);
        Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
              origin, List.of(firstDeadEnd, secondDeadEnd, firstStep),
              firstStep, List.of(secondStep),
              secondStep, List.of(destination));
        AtomicInteger expandedSystems = new AtomicInteger();
        RoutePolicy policy = new RoutePolicy() {
            @Override
            public Collection<PlanetarySystem> getNeighbors(PlanetarySystem system) {
                expandedSystems.incrementAndGet();
                return neighbors.getOrDefault(system, List.of());
            }

            @Override
            public boolean isSystemAllowed(PlanetarySystem system) {
                return true;
            }

            @Override
            public boolean canTraverse(PlanetarySystem legOrigin, PlanetarySystem legDestination) {
                return true;
            }

            @Override
            public double minimumRechargeHours() {
                return PlanetarySystem.MINIMUM_RECHARGE_TIME_HOURS;
            }
        };

        JumpPath path = RouteAlternativesPlanner.planFastestSegment(origin, destination, TEST_DATE, false, policy);

        assertEquals(List.of(origin, firstStep, secondStep, destination), path.getSystems());
        assertEquals(3, expandedSystems.get());
    }

                @Test
                void requestedEmptyDestinationIsAllowedWhileEmptyIntermediatesRemainFiltered() {
                    PlanetarySystem origin = system("ORIGIN", 0.0);
                    PlanetarySystem emptyIntermediate = system("EMPTY_INTERMEDIATE", 0.0);
                    PlanetarySystem clearIntermediate = system("CLEAR_INTERMEDIATE", 0.0);
                    PlanetarySystem emptyDestination = system("EMPTY_DESTINATION", 0.0);
                    Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
                            origin, List.of(emptyIntermediate, clearIntermediate),
                            emptyIntermediate, List.of(emptyDestination),
                            clearIntermediate, List.of(emptyDestination));
                    RoutePolicy policy = policy(neighbors, Set.of(emptyIntermediate, emptyDestination), Set.of(),
                            Set.of(emptyDestination));

                    List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(emptyDestination), TEST_DATE,
                            false, policy);

                    assertEquals(List.of(origin, clearIntermediate, emptyDestination), courses.getFirst().systems());
                }

    @Test
    void planningPlotReachesAbandonedCressonThroughRequiredHerrmazFallback() throws IOException {
        Ranks.initializeRankSystems();
        Systems systems = Systems.loadDefault();
        PlanetarySystem origin = systems.getSystemByName("Lindenmarle", TEST_DATE);
        PlanetarySystem herrmaz = systems.getSystemByName("Herrmaz", TEST_DATE);
        PlanetarySystem destination = systems.getSystemByName("Cresson", TEST_DATE);
        CampaignConfiguration configuration = MHQTestUtilities.buildTestConfigWithSystems(systems);
        configuration.setCurrentDay(TEST_DATE);
        configuration.setLocation(new CurrentLocation(origin, 0));
        Campaign campaign = new Campaign(configuration);
        campaign.getPlayerForce().setIsAvoidingEmptySystems(true);

        JumpPath path = campaign.calculateJumpPathForPlanning(origin, destination).path();
        NavigationRouteAnalysis.PathAssessment assessment = campaign.assessNavigationPath(path.getSystems(),
              List.of(destination), index -> false);

        assertFalse(path.getSystems().isEmpty());
        assertEquals(origin, path.getSystems().getFirst());
        assertEquals(destination, path.getSystems().getLast());
        int herrmazIndex = path.getSystems().indexOf(herrmaz);
        assertTrue(herrmazIndex > 0);
        assertTrue(herrmazIndex < path.getSystems().size() - 1);
        assertTrue(path.getSystems().stream().anyMatch(PlanetarySystem::isConnector));
                assessment.legs().stream()
              .filter(leg -> leg.destination().isConnector())
              .forEach(leg -> {
                                    assertNotEquals(NavigationRouteAnalysis.Severity.BLOCKED, leg.severity());
                                    assertFalse(leg.findings().stream()
                                                                        .anyMatch(finding -> finding.kind()
                                                                                                                             == NavigationRouteAnalysis.FindingKind.ABANDONED_DESTINATION_AVOIDED));
              });
        for (int systemIndex = 1; systemIndex < path.getSystems().size() - 1; systemIndex++) {
            PlanetarySystem connector = path.getSystems().get(systemIndex);
            if (connector.isConnector()) {
                JumpPath strictConnectorPath = campaign.calculateJumpPath(path.getSystems().get(systemIndex - 1),
                      path.getSystems().get(systemIndex + 1), false, false);
                assertFalse(strictConnectorPath.isEmpty());
                assertTrue(strictConnectorPath.getSystems().contains(connector));
            }
        }
        NavigationRouteAnalysis.LegAssessment herrmazLeg = assessment.legs().get(herrmazIndex - 1);
        assertEquals(NavigationRouteAnalysis.Severity.BLOCKED, herrmazLeg.severity());
        assertTrue(herrmazLeg.findings().stream()
                         .anyMatch(finding -> finding.kind()
                                                    == NavigationRouteAnalysis.FindingKind.ABANDONED_DESTINATION_AVOIDED));
        assertEquals(NavigationRouteAnalysis.Severity.CAUTION, assessment.legs().getLast().severity());
        assertTrue(assessment.legs().getLast().findings().stream()
                         .anyMatch(finding -> finding.kind()
                                                    == NavigationRouteAnalysis.FindingKind.ABANDONED_DESTINATION_ALLOWED));
        assertEquals(NavigationRouteAnalysis.Severity.BLOCKED, assessment.severity());
    }

    @Test
    void planningUsesStrictPopulatedRouteBeforeFasterAbandonedFallback() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        PlanetarySystem populated = system("POPULATED", 10.0);
        PlanetarySystem abandoned = system("ABANDONED", 1.0);
        PlanetarySystem destination = system("DESTINATION", 0.0);
        Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
              origin, List.of(populated, abandoned),
              populated, List.of(destination),
              abandoned, List.of(destination));
        RoutePolicy strictPolicy = policy(neighbors, Set.of(abandoned), Set.of());
        RoutePolicy fallbackPolicy = policy(neighbors);

        JumpPath fallbackPath = RouteAlternativesPlanner.planFastestSegment(origin, destination, TEST_DATE,
              false, fallbackPolicy);
                PlanningResult planningResult = RouteAlternativesPlanner.planFastestSegmentWithFallback(origin, destination,
              TEST_DATE, false, strictPolicy, fallbackPolicy);

        assertEquals(List.of(origin, abandoned, destination), fallbackPath.getSystems());
                assertEquals(PlanningStatus.ROUTE_FOUND, planningResult.status());
                assertEquals(List.of(origin, populated, destination), planningResult.path().getSystems());
    }

    @Test
    void planningFallbackStillRejectsAccessDeniedLegs() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        PlanetarySystem abandoned = system("ABANDONED", 1.0);
        PlanetarySystem destination = system("DESTINATION", 0.0);
        Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
              origin, List.of(abandoned),
              abandoned, List.of(destination));
        RoutePolicy strictPolicy = policy(neighbors, Set.of(abandoned), Set.of());
        RoutePolicy deniedFallbackPolicy = policy(neighbors, Set.of(), Set.of(new Leg(origin, abandoned)));

                PlanningResult planningResult = RouteAlternativesPlanner.planFastestSegmentWithFallback(origin, destination,
              TEST_DATE, false, strictPolicy, deniedFallbackPolicy);

                assertEquals(PlanningStatus.ACCESS_DENIED, planningResult.status());
                assertTrue(planningResult.path().isEmpty());
        }

        @Test
        void planningReportsNoRouteWhenAccessDoesNotExplainFailure() {
                PlanetarySystem origin = system("ORIGIN", 0.0);
                PlanetarySystem destination = system("DESTINATION", 0.0);
                RoutePolicy policy = policy(Map.of());

                PlanningResult planningResult = RouteAlternativesPlanner.planFastestSegmentWithFallback(origin, destination,
              TEST_DATE, false, policy, policy);

                assertEquals(PlanningStatus.NO_ROUTE, planningResult.status());
                assertTrue(planningResult.path().isEmpty());
    }

    @Test
    void directSegmentAllowsRequestedEmptyDestinationButRejectsOtherEmptyAndDeniedSystems() {
        PlanetarySystem origin = system("ORIGIN", 0.0);
        PlanetarySystem emptyIntermediate = system("EMPTY_INTERMEDIATE", 0.0);
        PlanetarySystem deniedIntermediate = system("DENIED_INTERMEDIATE", 0.0);
        PlanetarySystem clearIntermediate = system("CLEAR_INTERMEDIATE", 10.0);
        PlanetarySystem emptyDestination = system("EMPTY_DESTINATION", 0.0);
        Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
              origin, List.of(emptyIntermediate, deniedIntermediate, clearIntermediate),
              emptyIntermediate, List.of(emptyDestination),
              deniedIntermediate, List.of(emptyDestination),
              clearIntermediate, List.of(emptyDestination));
        RoutePolicy policy = policy(neighbors, Set.of(emptyIntermediate, emptyDestination),
              Set.of(new Leg(origin, deniedIntermediate)), Set.of(emptyDestination));

        JumpPath path = RouteAlternativesPlanner.planFastestSegment(origin, emptyDestination, TEST_DATE,
              false, policy);

        assertEquals(List.of(origin, clearIntermediate, emptyDestination), path.getSystems());
    }

                @Test
                void requestedDestinationAllowanceDoesNotOverrideAccessDenial() {
                    PlanetarySystem origin = system("ORIGIN", 0.0);
                    PlanetarySystem emptyDestination = system("EMPTY_DESTINATION", 0.0);
                    RoutePolicy policy = policy(Map.of(origin, List.of(emptyDestination)), Set.of(emptyDestination),
                            Set.of(new Leg(origin, emptyDestination)), Set.of(emptyDestination));

                    assertTrue(RouteAlternativesPlanner.plan(origin, List.of(emptyDestination), TEST_DATE, false,
                            policy).isEmpty());
                }

                        @Test
                        void requestedEmptyWaypointDoesNotAdmitUnrelatedEmptyIntermediates() {
                          PlanetarySystem origin = system("ORIGIN", 0.0);
                          PlanetarySystem requestedEmptyWaypoint = system("REQUESTED_EMPTY_WAYPOINT", 0.0);
                          PlanetarySystem unrelatedEmpty = system("UNRELATED_EMPTY", 0.0);
                          PlanetarySystem clearIntermediate = system("CLEAR_INTERMEDIATE", 0.0);
                          PlanetarySystem destination = system("DESTINATION", 0.0);
                          Map<PlanetarySystem, List<PlanetarySystem>> neighbors = Map.of(
                              origin, List.of(requestedEmptyWaypoint),
                              requestedEmptyWaypoint, List.of(unrelatedEmpty, clearIntermediate),
                              unrelatedEmpty, List.of(destination),
                              clearIntermediate, List.of(destination));
                          RoutePolicy policy = policy(neighbors, Set.of(requestedEmptyWaypoint, unrelatedEmpty), Set.of(),
                              Set.of(requestedEmptyWaypoint));

                          List<Course> courses = RouteAlternativesPlanner.plan(origin,
                              List.of(requestedEmptyWaypoint, destination), TEST_DATE, false, policy);

                          assertEquals(List.of(origin, requestedEmptyWaypoint, clearIntermediate, destination),
                              courses.getFirst().systems());
                        }

        @Test
        void unavailableRequestedSegmentProducesNoCourses() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem blocked = system("BLOCKED", 0.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);
          RoutePolicy policy = policy(Map.of(origin, List.of(blocked), blocked, List.of(destination)),
              Set.of(blocked), Set.of());

          assertTrue(RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false, policy).isEmpty());
        }

        @Test
        void wholeCircuitCourseIsOfferedOnlyWhenItImprovesTheRoute() {
          PlanetarySystem origin = system("ORIGIN", 0.0);
          PlanetarySystem normalRoute = system("NORMAL_ROUTE", 5.0, 5.0);
          PlanetarySystem circuitOne = system("CIRCUIT_ONE", 100.0, 1.0);
          PlanetarySystem circuitTwo = system("CIRCUIT_TWO", 100.0, 1.0);
          PlanetarySystem destination = system("DESTINATION", 0.0);
          RoutePolicy policy = policy(Map.of(
              origin, List.of(normalRoute, circuitOne),
              normalRoute, List.of(destination),
              circuitOne, List.of(circuitTwo),
              circuitTwo, List.of(destination)));

          List<Course> courses = RouteAlternativesPlanner.plan(origin, List.of(destination), TEST_DATE, false, policy);

          assertEquals(2, courses.size());
          assertEquals(List.of(origin, normalRoute, destination), courses.getFirst().systems());
          assertEquals(CourseKind.COMMAND_CIRCUIT, courses.getLast().kind());
          assertEquals(List.of(origin, circuitOne, circuitTwo, destination), courses.getLast().systems());
          assertEquals(RouteAlternativesPlanner.CircuitCoverage.WHOLE, courses.getLast().circuitCoverage());
        }

    private static PlanetarySystem system(String id, double rechargeHours) {
        return system(id, rechargeHours, rechargeHours);
    }

    private static PlanetarySystem system(String id, double rechargeHours, double circuitRechargeHours) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        when(system.getRechargeTime(any(LocalDate.class), anyBoolean()))
              .thenAnswer(invocation -> (boolean) invocation.getArgument(1)
                                       ? circuitRechargeHours
                                       : rechargeHours);
        when(system.getTimeToJumpPoint(anyDouble()))
              .thenAnswer(invocation -> 1.0 / sqrt(invocation.getArgument(0)));
        return system;
    }

    private static PlanetarySystem positionedSystem(String id, double rechargeHours, double x) {
        PlanetarySystem system = system(id, rechargeHours);
        when(system.getX()).thenReturn(x);
        when(system.getDistanceTo(any(PlanetarySystem.class)))
              .thenAnswer(invocation -> Math.abs(x - ((PlanetarySystem) invocation.getArgument(0)).getX()));
        return system;
    }

    private static RoutePolicy policy(Map<PlanetarySystem, List<PlanetarySystem>> neighbors) {
        return policy(neighbors, Set.of(), Set.of());
    }

    private static RoutePolicy policy(Map<PlanetarySystem, List<PlanetarySystem>> neighbors,
          Set<PlanetarySystem> disallowedSystems, Set<Leg> inaccessibleLegs) {
          return policy(neighbors, disallowedSystems, inaccessibleLegs, Set.of());
        }

        private static RoutePolicy policy(Map<PlanetarySystem, List<PlanetarySystem>> neighbors,
            Set<PlanetarySystem> disallowedSystems, Set<Leg> inaccessibleLegs,
            Set<PlanetarySystem> allowedRequestedDestinations) {
        return new RoutePolicy() {
            @Override
            public Collection<PlanetarySystem> getNeighbors(PlanetarySystem system) {
                return neighbors.getOrDefault(system, List.of());
            }

            @Override
            public boolean isSystemAllowed(PlanetarySystem system) {
                return !disallowedSystems.contains(system);
            }

            @Override
            public boolean isRequestedDestinationAllowed(PlanetarySystem system) {
                return isSystemAllowed(system) || allowedRequestedDestinations.contains(system);
            }

            @Override
            public boolean canTraverse(PlanetarySystem origin, PlanetarySystem destination) {
                return !inaccessibleLegs.contains(new Leg(origin, destination));
            }
        };
    }

    private record Leg(PlanetarySystem origin, PlanetarySystem destination) {
    }
}
