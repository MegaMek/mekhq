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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nullable;
import mekhq.campaign.JumpPathItinerary.Plan;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Anchors an immutable {@link JumpPathItinerary} plan in time and inserts transient requested-stop dwell holds.
 */
public final class JumpPathSchedule {
    private JumpPathSchedule() {
    }

    /** The moment represented by the selected scheduling anchor. */
    public enum Mode {
        DEPART_AT,
        ARRIVE_BY
    }

    /**
     * Builds a schedule at the same whole-hour precision used by the itinerary timeline.
     *
     * @param itinerary                  immutable route forecast
    * @param destinationPlanet          specific destination planet, or {@code null} when unavailable
     * @param requestedStops             explicit requested stops in route-planning order
     * @param dwellHoursByRequestedStop  dwell hours aligned with {@code requestedStops}; missing values are zero
     * @param mode                       whether {@code anchor} is departure or arrival
     * @param anchor                     selected departure or arrive-by moment
     * @param earliestFeasibleDeparture  earliest moment at which travel can begin
     *
     * @return immutable anchored schedule
     */
        public static Result calculate(Plan itinerary, @Nullable Planet destinationPlanet,
            List<PlanetarySystem> requestedStops,
          List<Integer> dwellHoursByRequestedStop, Mode mode, LocalDateTime anchor,
          LocalDateTime earliestFeasibleDeparture) {
        Objects.requireNonNull(itinerary);
        Objects.requireNonNull(requestedStops);
        Objects.requireNonNull(dwellHoursByRequestedStop);
        Objects.requireNonNull(mode);
        Objects.requireNonNull(anchor);
        Objects.requireNonNull(earliestFeasibleDeparture);
                if ((destinationPlanet != null) && (itinerary.entries().isEmpty()
                            || !Objects.equals(destinationPlanet.getParentSystem(), itinerary.entries().getLast().system()))) {
                        throw new IllegalArgumentException("destination planet must belong to the final itinerary system");
                }
        if (dwellHoursByRequestedStop.stream().anyMatch(hours -> (hours == null) || (hours < 0))) {
            throw new IllegalArgumentException("dwell hours must be non-negative");
        }

        List<Placement> placements = locateIntermediateStops(itinerary, requestedStops,
              dwellHoursByRequestedStop);
        long[] dwellHoursByPathIndex = new long[itinerary.entries().size()];
        long totalDwellHours = 0;
        for (Placement placement : placements) {
            dwellHoursByPathIndex[placement.pathIndex()] += placement.hours();
            totalDwellHours += placement.hours();
        }

        long totalHours = roundedHours(itinerary.totalDays()) + totalDwellHours;
        LocalDateTime departure = mode == Mode.DEPART_AT ? anchor : anchor.minusHours(totalHours);
        LocalDateTime arrival = departure.plusHours(totalHours);
        boolean feasible = !departure.isBefore(earliestFeasibleDeparture);

        List<Entry> entries = new ArrayList<>(itinerary.entries().size());
        List<Dwell> dwells = new ArrayList<>(placements.size());
        long precedingDwellHours = 0;
        int placementIndex = 0;
        for (int pathIndex = 0; pathIndex < itinerary.entries().size(); pathIndex++) {
            JumpPathItinerary.TimelineEntry itineraryEntry = itinerary.entries().get(pathIndex);
            LocalDateTime entryArrival = departure.plusHours(
                  roundedHours(itineraryEntry.arrivalElapsedDays()) + precedingDwellHours);
            LocalDateTime readyForDeparture = departure.plusHours(
                  roundedHours(itineraryEntry.departureElapsedDays()) + precedingDwellHours);

            LocalDateTime holdStart = readyForDeparture;
            while ((placementIndex < placements.size()) && (placements.get(placementIndex).pathIndex() == pathIndex)) {
                Placement placement = placements.get(placementIndex++);
                LocalDateTime holdEnd = holdStart.plusHours(placement.hours());
                dwells.add(new Dwell(placement.requestedStopIndex(), placement.system(), pathIndex,
                      placement.hours(), holdStart, holdEnd));
                holdStart = holdEnd;
            }

            long entryDwellHours = dwellHoursByPathIndex[pathIndex];
            precedingDwellHours += entryDwellHours;
            LocalDateTime endpointArrival = departure.plusHours(
                  roundedHours(itineraryEntry.endpointArrivalElapsedDays()) + precedingDwellHours);
            entries.add(new Entry(itineraryEntry, entryArrival, readyForDeparture,
                  readyForDeparture.plusHours(entryDwellHours), endpointArrival, entryDwellHours));
        }

          return new Result(mode, destinationPlanet, anchor, earliestFeasibleDeparture, departure, arrival,
              totalDwellHours,
              feasible, entries, dwells);
    }

    private static List<Placement> locateIntermediateStops(Plan itinerary, List<PlanetarySystem> requestedStops,
          List<Integer> dwellHoursByRequestedStop) {
        List<Placement> placements = new ArrayList<>();
        int searchFrom = 0;
        int previousPathIndex = -1;
        PlanetarySystem previousStop = null;
        int lastRequestedStop = requestedStops.size() - 1;
        for (int requestedStopIndex = 0; requestedStopIndex < lastRequestedStop; requestedStopIndex++) {
            PlanetarySystem stop = requestedStops.get(requestedStopIndex);
            int pathIndex = indexOf(itinerary, stop, searchFrom);
            if ((pathIndex < 0) && Objects.equals(stop, previousStop)) {
                pathIndex = previousPathIndex;
            }
            if (pathIndex >= 0) {
                searchFrom = pathIndex + 1;
            }
            previousPathIndex = pathIndex;
            previousStop = stop;

            if ((pathIndex <= 0) || (pathIndex >= itinerary.entries().size() - 1)) {
                continue;
            }
            int hours = requestedStopIndex < dwellHoursByRequestedStop.size()
                  ? dwellHoursByRequestedStop.get(requestedStopIndex)
                  : 0;
            placements.add(new Placement(requestedStopIndex, stop, pathIndex, hours));
        }
        return placements;
    }

    private static int indexOf(Plan itinerary, PlanetarySystem target, int fromIndex) {
        for (int index = fromIndex; index < itinerary.entries().size(); index++) {
            if (Objects.equals(itinerary.entries().get(index).system(), target)) {
                return index;
            }
        }
        return -1;
    }

    private static long roundedHours(double elapsedDays) {
        if (!Double.isFinite(elapsedDays)) {
            throw new IllegalArgumentException("itinerary elapsed days must be finite");
        }
        return Math.round(elapsedDays * 24.0);
    }

    private record Placement(int requestedStopIndex, PlanetarySystem system, int pathIndex, int hours) {
    }

    /** An anchored route schedule. */
    public record Result(Mode mode, @Nullable Planet destinationPlanet, LocalDateTime anchor,
                         LocalDateTime earliestFeasibleDeparture,
                         LocalDateTime departure, LocalDateTime arrival, long totalDwellHours, boolean feasible,
                         List<Entry> entries, List<Dwell> dwells) {
        public Result {
            Objects.requireNonNull(mode);
            Objects.requireNonNull(anchor);
            Objects.requireNonNull(earliestFeasibleDeparture);
            Objects.requireNonNull(departure);
            Objects.requireNonNull(arrival);
            entries = List.copyOf(entries);
            dwells = List.copyOf(dwells);
        }
    }

    /** Anchored moments for one itinerary system. */
    public record Entry(JumpPathItinerary.TimelineEntry itineraryEntry, LocalDateTime arrival,
                        LocalDateTime readyForDeparture, LocalDateTime departure, LocalDateTime endpointArrival,
                        long dwellHours) {
    }

    /** One explicit requested-stop hold in request order. */
    public record Dwell(int requestedStopIndex, PlanetarySystem system, int pathIndex, long hours,
                        LocalDateTime start, LocalDateTime end) {
    }
}
