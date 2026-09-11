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

import static mekhq.MHQConstants.MAX_JUMP_RADIUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.NavigationRouteAnalysis.FindingKind;
import mekhq.campaign.NavigationRouteAnalysis.LegAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Policy;
import mekhq.campaign.NavigationRouteAnalysis.Reachability;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class NavigationRouteAnalysisTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    @Test
    void directDistanceAndMinimumJumpBoundariesUseStandardJumpRadius() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem destination = system("DESTINATION");
        when(origin.getDistanceTo(destination)).thenReturn(17.25);

        assertEquals(17.25, NavigationRouteAnalysis.directDistanceLy(origin, destination));
        assertEquals(0, NavigationRouteAnalysis.minimumStandardJumps(0.0));
        assertEquals(1, NavigationRouteAnalysis.minimumStandardJumps(MAX_JUMP_RADIUS));
        assertEquals(2, NavigationRouteAnalysis.minimumStandardJumps(Math.nextUp((double) MAX_JUMP_RADIUS)));
        assertEquals(Integer.MAX_VALUE, NavigationRouteAnalysis.minimumStandardJumps(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> NavigationRouteAnalysis.minimumStandardJumps(-0.1));
    }

    @Test
    void outOfRangeAndAccessDenialAreBlockedFindings() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem destination = system("DESTINATION");
        when(origin.getDistanceTo(destination)).thenReturn(Math.nextUp((double) MAX_JUMP_RADIUS));
        Policy policy = policy(Map.of(), Set.of(), false, Set.of(new Leg(origin, destination)));

        LegAssessment assessment = NavigationRouteAnalysis.assessLeg(origin, destination, TEST_DATE, false, policy);

        assertEquals(Severity.BLOCKED, assessment.severity());
        assertEquals(List.of(FindingKind.OUT_OF_STANDARD_JUMP_RANGE, FindingKind.ACCESS_DENIED),
              blockingKinds(assessment));
    }

    @Test
    void allowedAndAvoidedAbandonedDestinationsHaveDistinctSeverity() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem abandoned = system("ABANDONED");

        LegAssessment allowed = NavigationRouteAnalysis.assessLeg(origin, abandoned, TEST_DATE, false,
              policy(Map.of(), Set.of(abandoned), false, Set.of()));
        LegAssessment avoided = NavigationRouteAnalysis.assessLeg(origin, abandoned, TEST_DATE, false,
              policy(Map.of(), Set.of(abandoned), true, Set.of()));

        assertEquals(Severity.CAUTION, allowed.severity());
        assertTrue(allowed.findings().stream()
                         .anyMatch(finding -> finding.kind() == FindingKind.ABANDONED_DESTINATION_ALLOWED));
        assertEquals(Severity.BLOCKED, avoided.severity());
        assertTrue(avoided.findings().stream()
                         .anyMatch(finding -> finding.kind() == FindingKind.ABANDONED_DESTINATION_AVOIDED));
    }

    @Test
    void rechargeFindingsAreGroundedFactsAndImpossibleRechargeBlocks() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem finite = system("FINITE", 72.0, 168.0, 1);
        PlanetarySystem impossible = system("IMPOSSIBLE", Double.POSITIVE_INFINITY,
              Double.POSITIVE_INFINITY, 0);
        Policy policy = policy(Map.of(), Set.of(), false, Set.of());

        LegAssessment finiteAssessment = NavigationRouteAnalysis.assessLeg(origin, finite, TEST_DATE, true, policy);
        LegAssessment impossibleAssessment = NavigationRouteAnalysis.assessLeg(origin, impossible, TEST_DATE,
              false, policy);

        assertEquals(Severity.CLEAR, finiteAssessment.severity());
        assertEquals(72.0, finiteAssessment.facts().rechargeHours());
        assertEquals(List.of(FindingKind.RECHARGE_DURATION, FindingKind.RECHARGE_STATION_AVAILABLE,
              FindingKind.SOLAR_RECHARGE_AVAILABLE, FindingKind.COMMAND_CIRCUIT_ASSUMED),
              finiteAssessment.findings().stream().map(NavigationRouteAnalysis.Finding::kind).toList());
        assertEquals(Severity.BLOCKED, impossibleAssessment.severity());
        assertTrue(impossibleAssessment.findings().stream()
                         .anyMatch(finding -> finding.kind() == FindingKind.RECHARGE_IMPOSSIBLE));
    }

    @Test
    void reachabilityUsesStableMinimumHopShellsHandlesCyclesAndExposesBlockedFrontier() {
        PlanetarySystem anchor = system("ANCHOR");
        PlanetarySystem alpha = system("ALPHA");
        PlanetarySystem beta = system("BETA");
        PlanetarySystem charlie = system("CHARLIE");
        PlanetarySystem abandoned = system("ABANDONED");
        PlanetarySystem denied = system("DENIED");
        Map<PlanetarySystem, List<PlanetarySystem>> graph = Map.of(
              anchor, List.of(beta, denied, alpha, abandoned),
              alpha, List.of(charlie, anchor),
              beta, List.of(charlie),
              charlie, List.of(anchor));
        Policy policy = policy(graph, Set.of(abandoned), true, Set.of(new Leg(anchor, denied)));

        Reachability reachability = NavigationRouteAnalysis.assessReachability(anchor, 3, TEST_DATE, false, policy);

        assertEquals(List.of("ALPHA", "BETA", "CHARLIE"), reachability.reachableSystems().stream()
                                                                  .map(entry -> entry.system().getId())
                                                                  .toList());
        assertEquals(List.of(1, 1, 2), reachability.reachableSystems().stream()
                                             .map(NavigationRouteAnalysis.ReachabilityEntry::minimumHops)
                                             .toList());
        assertEquals(List.of("ABANDONED", "DENIED"), reachability.blockedFrontier().stream()
                                                            .map(entry -> entry.system().getId())
                                                            .toList());
        assertThrows(UnsupportedOperationException.class, () -> reachability.reachableSystems().clear());
    }

    @Test
    void reachabilityHonorsMaximumHopBounds() {
        PlanetarySystem anchor = system("ANCHOR");
        PlanetarySystem alpha = system("ALPHA");
        PlanetarySystem beta = system("BETA");
        Policy policy = policy(Map.of(anchor, List.of(alpha), alpha, List.of(beta)), Set.of(), false, Set.of());

        Reachability oneHop = NavigationRouteAnalysis.assessReachability(anchor, 1, TEST_DATE, false, policy);

        assertEquals(List.of("ALPHA"), oneHop.reachableSystems().stream()
                                             .map(entry -> entry.system().getId())
                                             .toList());
        assertThrows(IllegalArgumentException.class,
              () -> NavigationRouteAnalysis.assessReachability(anchor, 0, TEST_DATE, false, policy));
        assertThrows(IllegalArgumentException.class,
              () -> NavigationRouteAnalysis.assessReachability(anchor, 4, TEST_DATE, false, policy));
    }

    @Test
    void pathAssessmentsRemainInDestinationLegOrder() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem first = system("FIRST");
        PlanetarySystem second = system("SECOND");
        Policy policy = policy(Map.of(), Set.of(second), false, Set.of());

        NavigationRouteAnalysis.PathAssessment assessment = NavigationRouteAnalysis.assessPath(
              List.of(origin, first, second), TEST_DATE, false, policy);

        assertEquals(List.of("FIRST", "SECOND"), assessment.legs().stream()
                                                    .map(leg -> leg.destination().getId())
                                                    .toList());
        assertEquals(List.of(Severity.CLEAR, Severity.CAUTION), assessment.legs().stream()
                                                               .map(LegAssessment::severity)
                                                               .toList());
        assertEquals(Severity.CAUTION, assessment.severity());
    }

    @Test
    void pathAssessmentCapturesPerDestinationCircuitAssumptions() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem first = system("FIRST");
        PlanetarySystem second = system("SECOND");

        NavigationRouteAnalysis.PathAssessment assessment = NavigationRouteAnalysis.assessPath(
              List.of(origin, first, second), TEST_DATE, destinationIndex -> destinationIndex == 1,
              policy(Map.of(), Set.of(), false, Set.of()));

        assertEquals(List.of(true, false), assessment.legs().stream()
                                               .map(leg -> leg.facts().commandCircuitAssumed())
                                               .toList());
    }

    @Test
    void finalArrivalDoesNotRequireRecharge() {
        PlanetarySystem origin = system("ORIGIN");
        PlanetarySystem intermediate = system("INTERMEDIATE", Double.POSITIVE_INFINITY,
              Double.POSITIVE_INFINITY, 0);
        PlanetarySystem finalDestination = system("FINAL", Double.POSITIVE_INFINITY,
              Double.POSITIVE_INFINITY, 0);

        NavigationRouteAnalysis.PathAssessment assessment = NavigationRouteAnalysis.assessPath(
              List.of(origin, intermediate, finalDestination), TEST_DATE, false,
              policy(Map.of(), Set.of(), false, Set.of()));

        LegAssessment finalLeg = assessment.legs().getLast();
        assertEquals(Severity.BLOCKED, assessment.legs().getFirst().severity());
        assertEquals(Severity.CLEAR, finalLeg.severity());
        assertTrue(finalLeg.findings().stream().anyMatch(finding ->
              (finding.kind() == FindingKind.RECHARGE_IMPOSSIBLE) && (finding.severity() == Severity.INFO)));
    }

    @Test
    void reachabilityAllowsArrivalWithoutRechargeButDoesNotContinueThroughIt() {
        PlanetarySystem anchor = system("ANCHOR");
        PlanetarySystem noRecharge = system("NO_RECHARGE", Double.POSITIVE_INFINITY,
              Double.POSITIVE_INFINITY, 0);
        PlanetarySystem beyond = system("BEYOND");
        Policy policy = policy(Map.of(anchor, List.of(noRecharge), noRecharge, List.of(beyond)),
              Set.of(), false, Set.of());

        Reachability reachability = NavigationRouteAnalysis.assessReachability(anchor, 2, TEST_DATE, false, policy);

        assertEquals(List.of("NO_RECHARGE"), reachability.reachableSystems().stream()
                                                   .map(entry -> entry.system().getId())
                                                   .toList());
        assertEquals(Severity.CLEAR, reachability.reachableSystems().getFirst().arrivalAssessment().severity());
        assertTrue(reachability.reachableSystems().getFirst().arrivalAssessment().findings().stream()
                         .anyMatch(finding -> (finding.kind() == FindingKind.RECHARGE_IMPOSSIBLE)
                                                   && (finding.severity() == Severity.INFO)));
        assertTrue(reachability.blockedFrontier().isEmpty());
    }

        @Test
        void requestedEmptyStopsAreCautionsWhileAutomaticEmptyIntermediatesRemainBlocked() {
          PlanetarySystem origin = system("ORIGIN");
          PlanetarySystem automaticEmpty = system("AUTOMATIC_EMPTY");
          PlanetarySystem requestedEmpty = system("REQUESTED_EMPTY");
          Policy policy = policy(Map.of(), Set.of(automaticEmpty, requestedEmpty), true, Set.of(),
              Set.of(requestedEmpty));

          NavigationRouteAnalysis.PathAssessment assessment = NavigationRouteAnalysis.assessPath(
              List.of(origin, automaticEmpty, requestedEmpty), List.of(requestedEmpty), TEST_DATE,
              falseIndex -> false, policy);

          assertEquals(List.of(Severity.BLOCKED, Severity.CAUTION), assessment.legs().stream()
                                                 .map(LegAssessment::severity)
                                                 .toList());
          assertEquals(List.of(FindingKind.ABANDONED_DESTINATION_AVOIDED,
              FindingKind.ABANDONED_DESTINATION_ALLOWED), assessment.legs().stream()
                                            .map(leg -> leg.findings().stream()
                                                        .filter(finding -> finding.kind()
                                                            == FindingKind.ABANDONED_DESTINATION_AVOIDED
                                                            || finding.kind()
                                                                == FindingKind.ABANDONED_DESTINATION_ALLOWED)
                                                        .findFirst()
                                                        .orElseThrow()
                                                        .kind())
                                            .toList());
        }

        @Test
        void explicitlyRequestedEmptyWaypointIsAnAllowedCaution() {
          PlanetarySystem origin = system("ORIGIN");
          PlanetarySystem requestedEmptyWaypoint = system("REQUESTED_EMPTY_WAYPOINT");
          PlanetarySystem destination = system("DESTINATION");
          Policy policy = policy(Map.of(), Set.of(requestedEmptyWaypoint), true, Set.of(),
              Set.of(requestedEmptyWaypoint));

          NavigationRouteAnalysis.PathAssessment assessment = NavigationRouteAnalysis.assessPath(
              List.of(origin, requestedEmptyWaypoint, destination),
              List.of(requestedEmptyWaypoint, destination), TEST_DATE, destinationIndex -> false, policy);

          assertEquals(List.of(Severity.CAUTION, Severity.CLEAR), assessment.legs().stream()
                                                 .map(LegAssessment::severity)
                                                 .toList());
        }

    private static List<FindingKind> blockingKinds(LegAssessment assessment) {
        return assessment.findings().stream()
                     .filter(finding -> finding.severity() == Severity.BLOCKED)
                     .map(NavigationRouteAnalysis.Finding::kind)
                     .toList();
    }

    private static PlanetarySystem system(String id) {
        return system(id, 72.0, 168.0, 0);
    }

    private static PlanetarySystem system(String id, double rechargeHours, double solarRechargeHours,
          int rechargeStations) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        when(system.getRechargeTime(any(LocalDate.class), anyBoolean())).thenReturn(rechargeHours);
        when(system.getSolarRechargeTime()).thenReturn(solarRechargeHours);
        when(system.getNumberRechargeStations(any(LocalDate.class))).thenReturn(rechargeStations);
        return system;
    }

    private static Policy policy(Map<PlanetarySystem, List<PlanetarySystem>> graph,
          Set<PlanetarySystem> abandonedSystems, boolean avoidAbandoned, Set<Leg> deniedLegs) {
          return policy(graph, abandonedSystems, avoidAbandoned, deniedLegs, Set.of());
        }

        private static Policy policy(Map<PlanetarySystem, List<PlanetarySystem>> graph,
            Set<PlanetarySystem> abandonedSystems, boolean avoidAbandoned, Set<Leg> deniedLegs,
            Set<PlanetarySystem> allowedRequestedDestinations) {
        return new Policy() {
            @Override
            public Collection<PlanetarySystem> getNeighbors(PlanetarySystem system) {
                return graph.getOrDefault(system, List.of());
            }

            @Override
            public boolean isSystemAllowed(PlanetarySystem system) {
                return !avoidAbandoned || !abandonedSystems.contains(system);
            }

            @Override
            public boolean isRequestedDestinationAllowed(PlanetarySystem system) {
                return isSystemAllowed(system) || allowedRequestedDestinations.contains(system);
            }

            @Override
            public boolean canTraverse(PlanetarySystem origin, PlanetarySystem destination) {
                return !deniedLegs.contains(new Leg(origin, destination));
            }

            @Override
            public boolean isAbandoned(PlanetarySystem system) {
                return abandonedSystems.contains(system);
            }

            @Override
            public boolean isAvoidingAbandonedSystems() {
                return avoidAbandoned;
            }
        };
    }

    private record Leg(PlanetarySystem origin, PlanetarySystem destination) {
    }
}
