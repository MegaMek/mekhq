/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

import jakarta.annotation.Nullable;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Produces transient, acceleration-aware plans for a {@link JumpPath} without changing the operational route.
 */
public final class JumpPathItinerary {
    public static final double DEFAULT_ACCELERATION_G = 1.0;

    private JumpPathItinerary() {
    }

    /**
     * Calculates an immutable itinerary using canonical planetary transit and recharge times.
     *
     * @param path              route to calculate
     * @param startDate         campaign date on which planning begins
     * @param accelerationG     acceleration used for both endpoint transits
     * @param fleetSystem       fleet's current system, or {@code null} when unknown
     * @param currentTransit    fleet transit progress in days
     * @param useCommandCircuit whether intermediate recharge uses command circuits
     *
     * @return immutable plan and system chronology
     */
    public static Plan calculate(JumpPath path, LocalDate startDate, double accelerationG,
          @Nullable PlanetarySystem fleetSystem, double currentTransit, boolean useCommandCircuit) {
          return calculate(path, startDate, accelerationG, fleetSystem, currentTransit,
              CircuitPlan.fromBoolean(useCommandCircuit));
        }

        /**
         * Calculates an immutable itinerary with command-circuit coverage selected per intermediate departure.
         *
         * @param path           route to calculate
         * @param startDate      campaign date on which planning begins
         * @param accelerationG  acceleration used for both endpoint transits
         * @param fleetSystem    fleet's current system, or {@code null} when unknown
         * @param currentTransit fleet transit progress in days
         * @param circuitPlan    transient command-circuit planning assumption
         *
         * @return immutable plan and system chronology
         */
        public static Plan calculate(JumpPath path, LocalDate startDate, double accelerationG,
            @Nullable PlanetarySystem fleetSystem, double currentTransit, CircuitPlan circuitPlan) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(startDate);
          Objects.requireNonNull(circuitPlan);
        requirePositiveFinite(accelerationG, "accelerationG");
        requireNonNegativeFinite(currentTransit, "currentTransit");

        List<PlanetarySystem> systems = List.copyOf(path.getSystems());
        if (systems.isEmpty()) {
            return new Plan(startDate, accelerationG, 0.0, 0.0, 0.0, 0.0, 0.0, List.of());
        }

        PlanetarySystem firstSystem = systems.getFirst();
        PlanetarySystem lastSystem = systems.getLast();
        double appliedCurrentTransit = firstSystem.equals(fleetSystem) ? currentTransit : 0.0;
        double startingTransit = firstSystem.getTimeToJumpPoint(accelerationG) - appliedCurrentTransit;
        double endingTransit = (path.getTargetPlanet() == null)
              ? lastSystem.getTimeToJumpPoint(accelerationG)
              : path.getTargetPlanet().getTimeToJumpPoint(accelerationG);

        int[] rechargeHours = new int[systems.size()];
        int totalRechargeHours = 0;
        for (int index = 1; index < systems.size() - 1; index++) {
            PlanetarySystem system = systems.get(index);
            rechargeHours[index] = (int) Math.ceil(system.getRechargeTime(startDate,
                  circuitPlan.usesCircuitAt(index)));
            totalRechargeHours += rechargeHours[index];
        }

        double rechargeDays = totalRechargeHours / 24.0;
        double totalDays = rechargeDays + startingTransit + endingTransit;
        List<TimelineEntry> entries = new ArrayList<>(systems.size());
        double elapsedDays = 0.0;
        for (int index = 0; index < systems.size(); index++) {
            boolean origin = index == 0;
            boolean destination = index == systems.size() - 1;
            double arrivalDays = elapsedDays;
            double departureDays;
            if (origin) {
                departureDays = startingTransit;
            } else {
                departureDays = arrivalDays + (rechargeHours[index] / 24.0);
            }

            if (!destination) {
                elapsedDays = departureDays;
            }
            double endpointArrivalDays = destination ? totalDays : departureDays;
            entries.add(new TimelineEntry(index + 1, systems.get(index), origin, destination, arrivalDays,
                  rechargeHours[index], departureDays, destination ? endingTransit : 0.0, endpointArrivalDays));
        }

        return new Plan(startDate, accelerationG, appliedCurrentTransit, startingTransit, endingTransit, rechargeDays,
              totalDays, entries);
    }

    /**
     * Solves the inverse-square transit relationship for a requested total journey time.
     *
     * @return a required acceleration, or an empty result when no positive endpoint-transit budget exists
     */
    public static RequiredAcceleration solveRequiredAcceleration(JumpPath path, LocalDate startDate,
          double desiredTotalDays, @Nullable PlanetarySystem fleetSystem, double currentTransit,
          boolean useCommandCircuit) {
          return solveRequiredAcceleration(path, startDate, desiredTotalDays, fleetSystem, currentTransit,
              CircuitPlan.fromBoolean(useCommandCircuit));
        }

        /**
         * Solves the inverse-square transit relationship using per-intermediate command-circuit coverage.
         *
         * @return a required acceleration, or an empty result when no positive endpoint-transit budget exists
         */
        public static RequiredAcceleration solveRequiredAcceleration(JumpPath path, LocalDate startDate,
            double desiredTotalDays, @Nullable PlanetarySystem fleetSystem, double currentTransit,
            CircuitPlan circuitPlan) {
        requirePositiveFinite(desiredTotalDays, "desiredTotalDays");
        Plan baseline = calculate(path, startDate, DEFAULT_ACCELERATION_G, fleetSystem, currentTransit,
              circuitPlan);
        double transitAtOneG = baseline.startingTransitDays() + baseline.appliedCurrentTransitDays()
                                   + baseline.endingTransitDays();
        double transitBudget = desiredTotalDays - baseline.rechargeDays() + baseline.appliedCurrentTransitDays();
        if (!(transitAtOneG > 0.0) || !(transitBudget > 0.0) || !Double.isFinite(transitBudget)) {
            return RequiredAcceleration.impossible();
        }

        double requiredAcceleration = Math.pow(transitAtOneG / transitBudget, 2);
        if (!(requiredAcceleration > 0.0) || !Double.isFinite(requiredAcceleration)) {
            return RequiredAcceleration.impossible();
        }
        return new RequiredAcceleration(OptionalDouble.of(requiredAcceleration));
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if ((value < 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    /** Command-circuit coverage mode for transient itinerary planning. */
    public enum CircuitMode {
        NONE,
        WHOLE,
        CUSTOM
    }

    /** Immutable command-circuit coverage selected at intermediate recharge departures. */
    public record CircuitPlan(CircuitMode mode, Set<Integer> coveredDepartureIndexes) {
        public CircuitPlan {
            Objects.requireNonNull(mode);
            Objects.requireNonNull(coveredDepartureIndexes);
            if (coveredDepartureIndexes.stream().anyMatch(index -> index <= 0)) {
                throw new IllegalArgumentException("covered departure indexes must be positive");
            }
            coveredDepartureIndexes = mode == CircuitMode.CUSTOM ? Set.copyOf(coveredDepartureIndexes) : Set.of();
        }

        public static CircuitPlan none() {
            return new CircuitPlan(CircuitMode.NONE, Set.of());
        }

        public static CircuitPlan whole() {
            return new CircuitPlan(CircuitMode.WHOLE, Set.of());
        }

        public static CircuitPlan custom(Set<Integer> coveredDepartureIndexes) {
            return new CircuitPlan(CircuitMode.CUSTOM, coveredDepartureIndexes);
        }

        public boolean usesCircuitAt(int departureIndex) {
            return switch (mode) {
                case NONE -> false;
                case WHOLE -> true;
                case CUSTOM -> coveredDepartureIndexes.contains(departureIndex);
            };
        }

        public CircuitPlan withCoverage(int departureIndex, boolean covered) {
            if (departureIndex <= 0) {
                throw new IllegalArgumentException("departureIndex must be positive");
            }
            Set<Integer> updatedIndexes = new HashSet<>(coveredDepartureIndexes);
            if (covered) {
                updatedIndexes.add(departureIndex);
            } else {
                updatedIndexes.remove(departureIndex);
            }
            return new CircuitPlan(CircuitMode.CUSTOM, updatedIndexes);
        }

        private static CircuitPlan fromBoolean(boolean useCommandCircuit) {
            return useCommandCircuit ? whole() : none();
        }
    }

    /** An acceleration-aware route plan. */
    public record Plan(LocalDate startDate, double accelerationG, double appliedCurrentTransitDays,
                       double startingTransitDays, double endingTransitDays, double rechargeDays, double totalDays,
                       List<TimelineEntry> entries) {
        public Plan {
            entries = List.copyOf(entries);
        }
    }

    /** Chronology for one system in route order. */
    public record TimelineEntry(int sequence, PlanetarySystem system, boolean origin, boolean destination,
                                double arrivalElapsedDays, int rechargeHours, double departureElapsedDays,
                                double endpointTransitDays, double endpointArrivalElapsedDays) {
    }

    /** Result of solving a desired journey duration. */
    public record RequiredAcceleration(OptionalDouble accelerationG) {
        public RequiredAcceleration {
            Objects.requireNonNull(accelerationG);
        }

        public static RequiredAcceleration impossible() {
            return new RequiredAcceleration(OptionalDouble.empty());
        }

        public boolean isPossible() {
            return accelerationG.isPresent();
        }
    }
}
