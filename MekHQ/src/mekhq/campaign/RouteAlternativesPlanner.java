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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import mekhq.campaign.universe.PlanetarySystem;

/** Computes transient, deterministic route comparisons without changing campaign state. */
public final class RouteAlternativesPlanner {
    private static final int MAXIMUM_SYSTEMS_PER_COURSE = 10001;
    private static final double SCORE_TOLERANCE = 1.0e-9;
    private static final String PATH_SEPARATOR = "\u0000";

    private RouteAlternativesPlanner() {
    }

    /** The operational reason a course is offered. */
    public enum CourseKind {
        FASTEST,
        FEWEST_JUMPS,
        COMMAND_CIRCUIT
    }

    /** Command-circuit coverage assumed while comparing a course. */
    public enum CircuitCoverage {
        NONE,
        WHOLE
    }

    /** Outcome of planning one requested route segment. */
    public enum PlanningStatus {
        ROUTE_FOUND,
        ACCESS_DENIED,
        NO_ROUTE
    }

    /** A route segment or the deterministic reason one could not be returned. */
    public record PlanningResult(JumpPath path, PlanningStatus status) {
        public PlanningResult {
            Objects.requireNonNull(path);
            Objects.requireNonNull(status);
            if ((status == PlanningStatus.ROUTE_FOUND) == path.isEmpty()) {
                throw new IllegalArgumentException("Planning status must match path availability");
            }
        }

        public static PlanningResult found(JumpPath path) {
            return new PlanningResult(path, PlanningStatus.ROUTE_FOUND);
        }

        public static PlanningResult failed(PlanningStatus status) {
            return new PlanningResult(new JumpPath(), status);
        }

        public boolean routeFound() {
            return status == PlanningStatus.ROUTE_FOUND;
        }
    }

    /** Supplies the route graph and campaign-specific filtering without owning mutable campaign options. */
    public interface RoutePolicy {
        Collection<PlanetarySystem> getNeighbors(PlanetarySystem system);

        boolean isSystemAllowed(PlanetarySystem system);

        default boolean isRequestedDestinationAllowed(PlanetarySystem system) {
            return isSystemAllowed(system);
        }

        boolean canTraverse(PlanetarySystem origin, PlanetarySystem destination);

        default RoutePolicy forSegment(PlanetarySystem origin, PlanetarySystem destination) {
            return this;
        }

        default double minimumRechargeHours() {
            return 0.0;
        }
    }

    /** One immutable route comparison. */
    public record Course(CourseKind kind, List<PlanetarySystem> systems, double oneGTotalDays,
                         CircuitCoverage circuitCoverage) {
        public Course {
            Objects.requireNonNull(kind);
            systems = List.copyOf(systems);
            Objects.requireNonNull(circuitCoverage);
        }

        public int jumps() {
            return Math.max(0, systems.size() - 1);
        }

        public JumpPath toJumpPath() {
            JumpPath path = new JumpPath();
            path.addSystems(systems);
            return path;
        }
    }

    static boolean isValidRequest(PlanetarySystem origin, List<PlanetarySystem> requestedStops) {
        if ((origin == null) || (requestedStops == null) || requestedStops.isEmpty()) {
            return false;
        }
        return requestedStops.stream().noneMatch(Objects::isNull);
    }

    /**
     * Plans unique courses through every requested stop in order.
     *
     * @param origin                    first system in every candidate course
     * @param requestedStops             explicit stops, including the final destination
     * @param date                       date used for recharge and elapsed-time comparisons
     * @param currentCircuitAvailability current whole-route operational circuit availability
     * @param policy                     immutable graph and access policy for this calculation
     *
     * @return fastest, fewest-jump, and useful circuit courses in stable presentation order
     */
    public static List<Course> plan(PlanetarySystem origin, List<PlanetarySystem> requestedStops, LocalDate date,
          boolean currentCircuitAvailability, RoutePolicy policy) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(requestedStops);
        Objects.requireNonNull(date);
        Objects.requireNonNull(policy);
        if (requestedStops.isEmpty()) {
            return List.of();
        }

        CircuitCoverage currentCoverage = currentCircuitAvailability ? CircuitCoverage.WHOLE : CircuitCoverage.NONE;
        LinkedHashMap<String, Course> uniqueCourses = new LinkedHashMap<>();
        addCourse(uniqueCourses, createCourse(CourseKind.FASTEST, Objective.FASTEST, currentCoverage, origin,
              requestedStops, date, policy));
        addCourse(uniqueCourses, createCourse(CourseKind.FEWEST_JUMPS, Objective.FEWEST_JUMPS, currentCoverage,
              origin, requestedStops, date, policy));

        if (!currentCircuitAvailability) {
            Course circuitCourse = createCourse(CourseKind.COMMAND_CIRCUIT, Objective.FASTEST,
                  CircuitCoverage.WHOLE, origin, requestedStops, date, policy);
            Course fastestCourse = uniqueCourses.values().stream()
                                         .filter(course -> course.kind() == CourseKind.FASTEST)
                                         .findFirst()
                                         .orElse(null);
            if (isUsefulCircuitCourse(circuitCourse, fastestCourse, date)) {
                addCourse(uniqueCourses, circuitCourse);
            }
        }
        return List.copyOf(uniqueCourses.values());
    }

    private static Course createCourse(CourseKind kind, Objective objective, CircuitCoverage circuitCoverage,
          PlanetarySystem origin, List<PlanetarySystem> requestedStops, LocalDate date, RoutePolicy policy) {
        List<PlanetarySystem> systems = new ArrayList<>();
        PlanetarySystem segmentOrigin = origin;
        boolean useCommandCircuit = circuitCoverage == CircuitCoverage.WHOLE;
        for (PlanetarySystem requestedStop : requestedStops) {
            if (requestedStop == null) {
                return null;
            }
            RoutePolicy segmentPolicy = policy.forSegment(segmentOrigin, requestedStop);
            List<PlanetarySystem> segment = findSegment(segmentOrigin, requestedStop, date, useCommandCircuit,
                objective, segmentPolicy);
            if (segment.isEmpty()) {
                return null;
            }
            systems.addAll(segment.subList(systems.isEmpty() ? 0 : 1, segment.size()));
            segmentOrigin = requestedStop;
        }

        JumpPath path = new JumpPath();
        path.addSystems(systems);
                return new Course(kind, systems, path.getTotalTime(date, 0.0, useCommandCircuit), circuitCoverage);
    }

    static JumpPath planFastestSegment(PlanetarySystem origin, PlanetarySystem destination, LocalDate date,
          boolean useCommandCircuit, RoutePolicy policy) {
        JumpPath path = new JumpPath();
        if (origin == null) {
            return path;
        }
        if ((destination == null) || sameSystem(origin, destination)) {
            path.addSystem(origin);
            return path;
        }

        List<PlanetarySystem> systems = findSegment(origin, destination, date, useCommandCircuit,
              Objective.FASTEST, policy.forSegment(origin, destination));
        path.addSystems(systems);
        return path;
    }

    static PlanningResult planFastestSegmentWithFallback(PlanetarySystem origin, PlanetarySystem destination,
          LocalDate date, boolean useCommandCircuit, RoutePolicy strictPolicy, RoutePolicy fallbackPolicy) {
        JumpPath strictPath = planFastestSegment(origin, destination, date, useCommandCircuit, strictPolicy);
        if (!strictPath.isEmpty()) {
            return PlanningResult.found(strictPath);
        }

        JumpPath fallbackPath = planFastestSegment(origin, destination, date, useCommandCircuit, fallbackPolicy);
        if (!fallbackPath.isEmpty()) {
            return PlanningResult.found(fallbackPath);
        }

        // This path is evidence for failure classification only and is never returned to the caller.
        JumpPath accessIndependentPath = planFastestSegment(origin, destination, date, useCommandCircuit,
              withoutAccessRestrictions(strictPolicy));
        if (accessIndependentPath.isEmpty()) {
            accessIndependentPath = planFastestSegment(origin, destination, date, useCommandCircuit,
                  withoutAccessRestrictions(fallbackPolicy));
        }
        return PlanningResult.failed(accessIndependentPath.isEmpty()
                                           ? PlanningStatus.NO_ROUTE
                                           : PlanningStatus.ACCESS_DENIED);
    }

    private static RoutePolicy withoutAccessRestrictions(RoutePolicy delegate) {
        return new RoutePolicy() {
            @Override
            public Collection<PlanetarySystem> getNeighbors(PlanetarySystem system) {
                return delegate.getNeighbors(system);
            }

            @Override
            public boolean isSystemAllowed(PlanetarySystem system) {
                return delegate.isSystemAllowed(system);
            }

            @Override
            public boolean isRequestedDestinationAllowed(PlanetarySystem system) {
                return delegate.isRequestedDestinationAllowed(system);
            }

            @Override
            public boolean canTraverse(PlanetarySystem origin, PlanetarySystem destination) {
                return true;
            }

            @Override
            public RoutePolicy forSegment(PlanetarySystem origin, PlanetarySystem destination) {
                return withoutAccessRestrictions(delegate.forSegment(origin, destination));
            }

            @Override
            public double minimumRechargeHours() {
                return delegate.minimumRechargeHours();
            }
        };
    }

    private static List<PlanetarySystem> findSegment(PlanetarySystem origin, PlanetarySystem destination,
          LocalDate date, boolean useCommandCircuit, Objective objective, RoutePolicy policy) {
        if (sameSystem(origin, destination)) {
            return List.of(origin);
        }
        if (!policy.isRequestedDestinationAllowed(destination)) {
            return List.of();
        }

        double minimumRechargeHours = policy.minimumRechargeHours();
        if (!Double.isFinite(minimumRechargeHours) || (minimumRechargeHours < 0.0)) {
            throw new IllegalArgumentException("Minimum recharge hours must be finite and nonnegative");
        }
        Comparator<SearchNode> nodeOrder = Comparator.comparingDouble(SearchNode::estimatedTotal)
                                                   .thenComparingDouble(SearchNode::score)
                                                   .thenComparing(node -> systemKey(node.system()));
        PriorityQueue<SearchNode> frontier = new PriorityQueue<>(nodeOrder);
        Map<String, SearchNode> bestNodes = new HashMap<>();
        SearchNode start = new SearchNode(origin, 0.0,
              estimateRemainingCost(origin, destination, origin, objective, minimumRechargeHours), null, 1);
        frontier.add(start);
        bestNodes.put(systemKey(origin), start);

        while (!frontier.isEmpty()) {
            SearchNode current = frontier.remove();
            if (current != bestNodes.get(systemKey(current.system()))) {
                continue;
            }
            if (sameSystem(current.system(), destination)) {
                return reconstructPath(current);
            }
            if (current.depth() >= MAXIMUM_SYSTEMS_PER_COURSE) {
                continue;
            }

            List<PlanetarySystem> neighbors = new ArrayList<>(policy.getNeighbors(current.system()));
            neighbors.sort(Comparator.comparing(RouteAlternativesPlanner::systemKey));
            for (PlanetarySystem neighbor : neighbors) {
                if ((neighbor == null) || containsSystem(current, neighbor)
                                            || (!sameSystem(neighbor, destination) && !policy.isSystemAllowed(neighbor))
                      || !policy.canTraverse(current.system(), neighbor)) {
                    continue;
                }

                double edgeScore = objective == Objective.FEWEST_JUMPS ? 1.0
                                         : getDepartureRecharge(current.system(), origin, date, useCommandCircuit);
                double candidateScore = current.score() + edgeScore;
                SearchNode previous = bestNodes.get(systemKey(neighbor));
                if (!isBetter(candidateScore, current, previous)) {
                    continue;
                }

                double estimatedTotal = candidateScore + estimateRemainingCost(neighbor, destination, origin,
                      objective, minimumRechargeHours);
                SearchNode candidate = new SearchNode(neighbor, candidateScore, estimatedTotal, current,
                      current.depth() + 1);
                bestNodes.put(systemKey(neighbor), candidate);
                frontier.add(candidate);
            }
        }
        return List.of();
    }

    private static double estimateRemainingCost(PlanetarySystem system, PlanetarySystem destination,
          PlanetarySystem segmentOrigin, Objective objective, double minimumRechargeHours) {
        int minimumJumps = (int) Math.ceil(Math.max(0.0,
              system.getDistanceTo(destination) - SCORE_TOLERANCE) / MAX_JUMP_RADIUS);
        if (objective == Objective.FEWEST_JUMPS) {
            return minimumJumps;
        }
        int minimumRechargeStops = sameSystem(system, segmentOrigin)
              ? Math.max(0, minimumJumps - 1)
              : minimumJumps;
        return minimumRechargeStops * minimumRechargeHours;
    }

    private static double getDepartureRecharge(PlanetarySystem system, PlanetarySystem segmentOrigin,
          LocalDate date, boolean useCommandCircuit) {
        return sameSystem(system, segmentOrigin) ? 0.0 : system.getRechargeTime(date, useCommandCircuit);
    }

    private static boolean isBetter(double candidateScore, SearchNode candidateParent, SearchNode previous) {
        if (previous == null) {
            return true;
        }
        if (candidateScore < previous.score() - SCORE_TOLERANCE) {
            return true;
        }
        return (Math.abs(candidateScore - previous.score()) <= SCORE_TOLERANCE)
                     && (comparePaths(candidateParent, previous.parent()) < 0);
    }

    private static int comparePaths(SearchNode first, SearchNode second) {
        List<String> firstPath = pathTo(first);
        List<String> secondPath = pathTo(second);
        int sharedLength = Math.min(firstPath.size(), secondPath.size());
        for (int index = 0; index < sharedLength; index++) {
            int comparison = firstPath.get(index).compareTo(secondPath.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(firstPath.size(), secondPath.size());
    }

    private static List<String> pathTo(SearchNode node) {
        List<String> path = new ArrayList<>(node.depth());
        for (SearchNode current = node; current != null; current = current.parent()) {
            path.add(systemKey(current.system()));
        }
        Collections.reverse(path);
        return path;
    }

    private static List<PlanetarySystem> reconstructPath(SearchNode destination) {
        List<PlanetarySystem> systems = new ArrayList<>(destination.depth());
        for (SearchNode current = destination; current != null; current = current.parent()) {
            systems.add(current.system());
        }
        Collections.reverse(systems);
        return List.copyOf(systems);
    }

    private static boolean isUsefulCircuitCourse(Course circuitCourse, Course fastestCourse, LocalDate date) {
        if ((circuitCourse == null) || (fastestCourse == null)
              || pathKey(circuitCourse.systems()).equals(pathKey(fastestCourse.systems()))) {
            return false;
        }
        double fastestWithCircuit = fastestCourse.toJumpPath().getTotalTime(date, 0.0, true);
        return circuitCourse.oneGTotalDays() < fastestWithCircuit - SCORE_TOLERANCE;
    }

    private static void addCourse(Map<String, Course> courses, Course course) {
        if (course != null) {
            courses.putIfAbsent(pathKey(course.systems()), course);
        }
    }

    private static boolean containsSystem(SearchNode node, PlanetarySystem candidate) {
        for (SearchNode current = node; current != null; current = current.parent()) {
            if (sameSystem(current.system(), candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameSystem(PlanetarySystem first, PlanetarySystem second) {
        return Objects.equals(systemKey(first), systemKey(second));
    }

    private static String pathKey(List<PlanetarySystem> systems) {
        return String.join(PATH_SEPARATOR, systems.stream().map(RouteAlternativesPlanner::systemKey).toList());
    }

    private static String systemKey(PlanetarySystem system) {
        return Objects.requireNonNull(system.getId());
    }

    private enum Objective {
        FASTEST,
        FEWEST_JUMPS
    }

    private record SearchNode(PlanetarySystem system, double score, double estimatedTotal,
                              SearchNode parent, int depth) {
    }
}
