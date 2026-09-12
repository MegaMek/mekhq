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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntPredicate;

import jakarta.annotation.Nullable;
import mekhq.campaign.universe.PlanetarySystem;

/** Deterministic navigation facts and route-constraint assessments. */
public final class NavigationRouteAnalysis {
    public static final int MAXIMUM_REACHABILITY_HOPS = 3;

    private static final Comparator<PlanetarySystem> SYSTEM_ORDER =
          Comparator.comparing(NavigationRouteAnalysis::systemId);

    private NavigationRouteAnalysis() {
    }

    /** Supplies the campaign route graph and the grounded state needed to explain route constraints. */
    public interface Policy extends RouteAlternativesPlanner.RoutePolicy {
        boolean isAbandoned(PlanetarySystem system);

        boolean isAvoidingAbandonedSystems();

        @Override
        default Policy forSegment(PlanetarySystem origin, PlanetarySystem destination) {
            return this;
        }
    }

    public enum Severity {
        CLEAR,
        INFO,
        CAUTION,
        BLOCKED
    }

    public enum FindingKind {
        OUT_OF_STANDARD_JUMP_RANGE,
        ACCESS_DENIED,
        ABANDONED_DESTINATION_AVOIDED,
        ABANDONED_DESTINATION_ALLOWED,
        RECHARGE_IMPOSSIBLE,
        RECHARGE_DURATION,
        RECHARGE_STATION_AVAILABLE,
        SOLAR_RECHARGE_AVAILABLE,
        SOLAR_RECHARGE_IMPOSSIBLE,
        COMMAND_CIRCUIT_ASSUMED
    }

    public record Finding(FindingKind kind, Severity severity) {
        public Finding {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(severity);
        }
    }

    public record LegFacts(double distanceLy, int minimumStandardJumps, boolean accessAllowed,
                          boolean abandonedDestination, double rechargeHours, int rechargeStationCount,
                          double solarRechargeHours, boolean commandCircuitAssumed) {
    }

    public record LegAssessment(PlanetarySystem origin, PlanetarySystem destination, LegFacts facts,
                                List<Finding> findings, Severity severity) {
        public LegAssessment {
            Objects.requireNonNull(origin);
            Objects.requireNonNull(destination);
            Objects.requireNonNull(facts);
            findings = List.copyOf(findings);
            Objects.requireNonNull(severity);
        }

        public @Nullable FindingKind primaryFindingKind() {
            FindingKind firstCaution = null;
            for (Finding finding : findings) {
                if (finding.severity() == Severity.BLOCKED) {
                    return finding.kind();
                }
                if ((firstCaution == null) && (finding.severity() == Severity.CAUTION)) {
                    firstCaution = finding.kind();
                }
            }
            return firstCaution;
        }
    }

    public record PathAssessment(List<LegAssessment> legs, Severity severity) {
        public PathAssessment {
            legs = List.copyOf(legs);
            Objects.requireNonNull(severity);
        }
    }

    public record ReachabilityEntry(PlanetarySystem system, int minimumHops, LegAssessment arrivalAssessment) {
        public ReachabilityEntry {
            Objects.requireNonNull(system);
            Objects.requireNonNull(arrivalAssessment);
        }
    }

    public record Reachability(PlanetarySystem anchor, int maximumHops, List<ReachabilityEntry> reachableSystems,
                               List<ReachabilityEntry> blockedFrontier) {
        public Reachability {
            Objects.requireNonNull(anchor);
            reachableSystems = List.copyOf(reachableSystems);
            blockedFrontier = List.copyOf(blockedFrontier);
        }
    }

    public static double directDistanceLy(PlanetarySystem origin, PlanetarySystem destination) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(destination);
        return origin.getDistanceTo(destination);
    }

    public static int minimumStandardJumps(PlanetarySystem origin, PlanetarySystem destination) {
        return minimumStandardJumps(directDistanceLy(origin, destination));
    }

    public static int minimumStandardJumps(double distanceLy) {
        if (Double.isNaN(distanceLy) || (distanceLy < 0.0)) {
            throw new IllegalArgumentException("distanceLy must be non-negative");
        }
        if (!Double.isFinite(distanceLy)) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(distanceLy / MAX_JUMP_RADIUS);
    }

    public static LegAssessment assessLeg(PlanetarySystem origin, PlanetarySystem destination, LocalDate date,
          boolean useCommandCircuit, Policy policy) {
        return assessLeg(origin, destination, date, useCommandCircuit, policy, false, true);
    }

    private static LegAssessment assessLeg(PlanetarySystem origin, PlanetarySystem destination, LocalDate date,
          boolean useCommandCircuit, Policy policy, boolean requestedDestination, boolean rechargeRequired) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(policy);

        double distanceLy = directDistanceLy(origin, destination);
        int minimumJumps = minimumStandardJumps(distanceLy);
        boolean accessAllowed = policy.canTraverse(origin, destination);
        boolean abandoned = policy.isAbandoned(destination);
        double rechargeHours = destination.getRechargeTime(date, useCommandCircuit);
        int rechargeStations = destination.getNumberRechargeStations(date);
        double solarRechargeHours = destination.getSolarRechargeTime();
        LegFacts facts = new LegFacts(distanceLy, minimumJumps, accessAllowed, abandoned, rechargeHours,
              rechargeStations, solarRechargeHours, useCommandCircuit);

        List<Finding> findings = new ArrayList<>();
        if (!Double.isFinite(distanceLy) || (distanceLy > MAX_JUMP_RADIUS)) {
            findings.add(new Finding(FindingKind.OUT_OF_STANDARD_JUMP_RANGE, Severity.BLOCKED));
        }
        if (!accessAllowed) {
            findings.add(new Finding(FindingKind.ACCESS_DENIED, Severity.BLOCKED));
        }
        if (abandoned) {
            boolean avoided = policy.isAvoidingAbandonedSystems()
                                    && (!requestedDestination
                                              || !policy.isRequestedDestinationAllowed(destination));
            findings.add(new Finding(avoided
                                           ? FindingKind.ABANDONED_DESTINATION_AVOIDED
                                           : FindingKind.ABANDONED_DESTINATION_ALLOWED,
                  avoided ? Severity.BLOCKED : Severity.CAUTION));
        }
        if (Double.isFinite(rechargeHours)) {
            findings.add(new Finding(FindingKind.RECHARGE_DURATION, Severity.INFO));
        } else {
            findings.add(new Finding(FindingKind.RECHARGE_IMPOSSIBLE,
                  rechargeRequired ? Severity.BLOCKED : Severity.INFO));
        }
        if (rechargeStations > 0) {
            findings.add(new Finding(FindingKind.RECHARGE_STATION_AVAILABLE, Severity.INFO));
        }
        findings.add(new Finding(Double.isFinite(solarRechargeHours)
                                       ? FindingKind.SOLAR_RECHARGE_AVAILABLE
                                       : FindingKind.SOLAR_RECHARGE_IMPOSSIBLE,
              Severity.INFO));
        if (useCommandCircuit) {
            findings.add(new Finding(FindingKind.COMMAND_CIRCUIT_ASSUMED, Severity.INFO));
        }

        return new LegAssessment(origin, destination, facts, findings, overallSeverity(findings));
    }

    public static PathAssessment assessPath(List<PlanetarySystem> systems, LocalDate date,
          boolean useCommandCircuit, Policy policy) {
        return assessPath(systems, List.of(), date, destinationIndex -> useCommandCircuit, policy);
    }

    public static PathAssessment assessPath(List<PlanetarySystem> systems, LocalDate date,
          IntPredicate useCommandCircuitAtDestination, Policy policy) {
        return assessPath(systems, List.of(), date, useCommandCircuitAtDestination, policy);
    }

    public static PathAssessment assessPath(List<PlanetarySystem> systems,
          List<PlanetarySystem> requestedStops, LocalDate date,
          IntPredicate useCommandCircuitAtDestination, Policy policy) {
        Objects.requireNonNull(systems);
        Objects.requireNonNull(requestedStops);
        Objects.requireNonNull(date);
        Objects.requireNonNull(useCommandCircuitAtDestination);
        Objects.requireNonNull(policy);
        if (systems.size() < 2) {
            return new PathAssessment(List.of(), Severity.CLEAR);
        }

        List<PlanetarySystem> effectiveStops = requestedStops.isEmpty()
                                                        ? List.of(systems.getLast())
                                                        : List.copyOf(requestedStops);
        int requestedStopIndex = 0;
        PlanetarySystem segmentOrigin = systems.getFirst();
        Policy segmentPolicy = policy.forSegment(segmentOrigin, effectiveStops.getFirst());
        List<LegAssessment> legs = new ArrayList<>(systems.size() - 1);
        for (int index = 1; index < systems.size(); index++) {
            PlanetarySystem destination = systems.get(index);
            boolean requestedDestination = requestedStopIndex < effectiveStops.size()
                                                 && sameSystem(destination,
                                                       effectiveStops.get(requestedStopIndex));
            legs.add(assessLeg(systems.get(index - 1), destination, date,
                useCommandCircuitAtDestination.test(index), segmentPolicy, requestedDestination,
                index < systems.size() - 1));
            if (requestedDestination && (++requestedStopIndex < effectiveStops.size())) {
                segmentOrigin = destination;
                segmentPolicy = policy.forSegment(segmentOrigin, effectiveStops.get(requestedStopIndex));
            }
        }
        return new PathAssessment(legs, overallSeverity(legs.stream().map(LegAssessment::severity).toList()));
    }

    public static Reachability assessReachability(PlanetarySystem anchor, int maximumHops, LocalDate date,
          boolean useCommandCircuit, Policy policy) {
        Objects.requireNonNull(anchor);
        Objects.requireNonNull(date);
        Objects.requireNonNull(policy);
        if ((maximumHops < 1) || (maximumHops > MAXIMUM_REACHABILITY_HOPS)) {
            throw new IllegalArgumentException("maximumHops must be between 1 and " + MAXIMUM_REACHABILITY_HOPS);
        }

        Policy segmentPolicy = policy.forSegment(anchor, anchor);
        Map<String, Integer> reachedHops = new HashMap<>();
        Map<String, ReachabilityEntry> reached = new LinkedHashMap<>();
        Map<String, ReachabilityEntry> blocked = new LinkedHashMap<>();
        Queue<PlanetarySystem> frontier = new ArrayDeque<>();
        reachedHops.put(systemId(anchor), 0);
        frontier.add(anchor);

        while (!frontier.isEmpty()) {
            PlanetarySystem origin = frontier.remove();
            int nextHop = reachedHops.get(systemId(origin)) + 1;
            if (nextHop > maximumHops) {
                continue;
            }

            List<PlanetarySystem> neighbors = sortedSystems(segmentPolicy.getNeighbors(origin));
            for (PlanetarySystem destination : neighbors) {
                String destinationId = systemId(destination);
                if (reachedHops.containsKey(destinationId)) {
                    continue;
                }

                                LegAssessment assessment = assessLeg(origin, destination, date, useCommandCircuit, segmentPolicy,
                                            false, false);
                ReachabilityEntry entry = new ReachabilityEntry(destination, nextHop, assessment);
                if (!segmentPolicy.isSystemAllowed(destination)
                          || (assessment.severity() == Severity.BLOCKED)) {
                    blocked.putIfAbsent(destinationId, entry);
                    continue;
                }

                reachedHops.put(destinationId, nextHop);
                reached.put(destinationId, entry);
                blocked.remove(destinationId);
                if (Double.isFinite(assessment.facts().rechargeHours())) {
                    frontier.add(destination);
                }
            }
        }

        Set<String> reachedIds = new HashSet<>(reached.keySet());
        List<ReachabilityEntry> blockedEntries = blocked.values().stream()
                                                          .filter(entry -> !reachedIds.contains(systemId(entry.system())))
                                                          .sorted(reachabilityOrder())
                                                          .toList();
        return new Reachability(anchor, maximumHops, reached.values().stream().sorted(reachabilityOrder()).toList(),
              blockedEntries);
    }

    private static List<PlanetarySystem> sortedSystems(Collection<PlanetarySystem> systems) {
        if (systems == null) {
            return List.of();
        }
        return systems.stream().filter(Objects::nonNull).sorted(SYSTEM_ORDER).toList();
    }

    private static Comparator<ReachabilityEntry> reachabilityOrder() {
        return Comparator.comparingInt(ReachabilityEntry::minimumHops)
                     .thenComparing(entry -> systemId(entry.system()));
    }

    private static Severity overallSeverity(List<Finding> findings) {
        if (findings.stream().anyMatch(finding -> finding.severity() == Severity.BLOCKED)) {
            return Severity.BLOCKED;
        }
        if (findings.stream().anyMatch(finding -> finding.severity() == Severity.CAUTION)) {
            return Severity.CAUTION;
        }
        return Severity.CLEAR;
    }

    private static Severity overallSeverity(Collection<Severity> severities) {
        if (severities.contains(Severity.BLOCKED)) {
            return Severity.BLOCKED;
        }
        if (severities.contains(Severity.CAUTION)) {
            return Severity.CAUTION;
        }
        return Severity.CLEAR;
    }

    private static String systemId(PlanetarySystem system) {
        return Objects.requireNonNull(system.getId());
    }

    private static boolean sameSystem(PlanetarySystem first, PlanetarySystem second) {
        return Objects.equals(systemId(first), systemId(second));
    }
}
