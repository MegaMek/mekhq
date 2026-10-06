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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.JumpPathItinerary.Plan;
import mekhq.campaign.JumpPathItinerary.TimelineEntry;
import mekhq.campaign.JumpPathSchedule.Mode;
import mekhq.campaign.JumpPathSchedule.Result;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class JumpPathScheduleTest {
    private static final LocalDate PLAN_DATE = LocalDate.of(3025, 1, 1);
    private static final LocalDateTime EARLIEST_DEPARTURE = PLAN_DATE.atStartOfDay();

    @Test
    void forwardScheduleAddsOrderedDwellsOnlyAtExplicitIntermediateStops() {
        PlanetarySystem origin = system("Origin");
        PlanetarySystem repeatedStop = system("Repeated");
        PlanetarySystem transit = system("Transit");
        PlanetarySystem destination = system("Destination");
                        Planet destinationPlanet = new Planet("destination-planet");
                        destinationPlanet.setParentSystem(destination);
        Plan itinerary = plan(origin, repeatedStop, transit, repeatedStop, destination);
        LocalDateTime anchor = PLAN_DATE.atTime(8, 0);

        Result result = JumpPathSchedule.calculate(itinerary, destinationPlanet,
              List.of(repeatedStop, repeatedStop, destination), List.of(6, 3, 99),
              Mode.DEPART_AT, anchor, EARLIEST_DEPARTURE);

        assertEquals(anchor, result.departure());
        assertEquals(anchor.plusHours(159), result.arrival());
        assertEquals(9, result.totalDwellHours());
                        assertSame(destinationPlanet, result.destinationPlanet());
        assertTrue(result.feasible());
        assertEquals(List.of(1, 3), result.dwells().stream().map(JumpPathSchedule.Dwell::pathIndex).toList());
        assertEquals(anchor.plusHours(48), result.dwells().get(0).start());
        assertEquals(anchor.plusHours(54), result.dwells().get(0).end());
        assertEquals(anchor.plusHours(102), result.dwells().get(1).start());
        assertEquals(anchor.plusHours(105), result.dwells().get(1).end());
        assertEquals(0, result.entries().get(2).dwellHours());
    }

      @Test
      void destinationPlanetMustBelongToFinalSystem() {
            PlanetarySystem origin = system("Origin");
            PlanetarySystem destination = system("Destination");
            Planet wrongDestination = new Planet("wrong-destination");
            wrongDestination.setParentSystem(system("Another System"));

            assertThrows(IllegalArgumentException.class,
                    () -> JumpPathSchedule.calculate(plan(origin, destination), wrongDestination,
                              List.of(destination), List.of(0), Mode.DEPART_AT, EARLIEST_DEPARTURE, EARLIEST_DEPARTURE));
      }

    @Test
    void arriveByScheduleBackSolvesLatestDepartureAndReportsFeasibilityBoundary() {
        PlanetarySystem origin = system("Origin");
        PlanetarySystem stop = system("Stop");
        PlanetarySystem destination = system("Destination");
        Plan itinerary = plan(origin, stop, destination);
        LocalDateTime deadline = PLAN_DATE.plusDays(10).atTime(12, 0);

        Result result = JumpPathSchedule.calculate(itinerary, null, List.of(stop, destination), List.of(5, 0),
              Mode.ARRIVE_BY, deadline, EARLIEST_DEPARTURE);
        Result impossible = JumpPathSchedule.calculate(itinerary, null, List.of(stop, destination), List.of(5, 0),
              Mode.ARRIVE_BY, deadline, result.departure().plusHours(1));

        assertEquals(deadline, result.arrival());
        assertEquals(deadline.minusHours(155), result.departure());
        assertTrue(result.feasible());
        assertFalse(impossible.feasible());
    }

    @Test
    void resultIsIndependentOfMutableInputsAndExposesImmutableCollections() {
        PlanetarySystem origin = system("Origin");
        PlanetarySystem stop = system("Stop");
        PlanetarySystem destination = system("Destination");
        Plan itinerary = plan(origin, stop, destination);
        List<PlanetarySystem> requestedStops = new ArrayList<>(List.of(stop, destination));
        List<Integer> dwellHours = new ArrayList<>(List.of(4, 0));

      Result result = JumpPathSchedule.calculate(itinerary, null, requestedStops, dwellHours,
              Mode.DEPART_AT, EARLIEST_DEPARTURE, EARLIEST_DEPARTURE);
        requestedStops.clear();
        dwellHours.set(0, 100);

        assertEquals(4, result.totalDwellHours());
        assertThrows(UnsupportedOperationException.class, () -> result.entries().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.dwells().clear());
    }

      @Test
      void duplicateRequestedStopsShareOneAvailableOccurrenceInRequestOrder() {
            PlanetarySystem origin = system("Origin");
            PlanetarySystem stop = system("Stop");
            PlanetarySystem destination = system("Destination");
            LocalDateTime anchor = PLAN_DATE.atStartOfDay();

            Result result = JumpPathSchedule.calculate(plan(origin, stop, destination), null,
                    List.of(stop, stop, destination), List.of(2, 3, 0),
                    Mode.DEPART_AT, anchor, EARLIEST_DEPARTURE);

            assertEquals(5, result.totalDwellHours());
            assertEquals(List.of(0, 1),
                    result.dwells().stream().map(JumpPathSchedule.Dwell::requestedStopIndex).toList());
            assertEquals(result.dwells().get(0).end(), result.dwells().get(1).start());
      }

      @Test
      void replacementRouteWithoutRequestedStopIsIgnoredWithoutChangingDuration() {
            PlanetarySystem origin = system("Origin");
            PlanetarySystem missingStop = system("Missing");
            PlanetarySystem destination = system("Destination");

            Result result = JumpPathSchedule.calculate(plan(origin, destination), null,
                    List.of(missingStop, destination), List.of(12, 0),
                    Mode.DEPART_AT, EARLIEST_DEPARTURE, EARLIEST_DEPARTURE);

            assertEquals(0, result.totalDwellHours());
            assertTrue(result.dwells().isEmpty());
            assertEquals(EARLIEST_DEPARTURE.plusHours(150), result.arrival());
      }

    @Test
    void wholeHourRoundingAndDegeneratePlansAreDeterministic() {
        PlanetarySystem only = system("Only");
        TimelineEntry roundedEntry = new TimelineEntry(1, only, true, true,
              0.0, 0, 0.0, 1.49 / 24.0, 1.49 / 24.0);
        Plan roundedPlan = new Plan(PLAN_DATE, 1.0, 0.0, 0.0, 1.49 / 24.0,
              0.0, 1.49 / 24.0, List.of(roundedEntry));
      Result rounded = JumpPathSchedule.calculate(roundedPlan, null, List.of(only), List.of(20),
              Mode.DEPART_AT, EARLIEST_DEPARTURE, EARLIEST_DEPARTURE);
        Plan emptyPlan = new Plan(PLAN_DATE, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, List.of());
      Result empty = JumpPathSchedule.calculate(emptyPlan, null, List.of(), List.of(),
              Mode.ARRIVE_BY, EARLIEST_DEPARTURE, EARLIEST_DEPARTURE);

        assertEquals(EARLIEST_DEPARTURE.plusHours(1), rounded.arrival());
        assertEquals(0, rounded.totalDwellHours());
        assertEquals(EARLIEST_DEPARTURE, empty.departure());
        assertEquals(EARLIEST_DEPARTURE, empty.arrival());
        assertTrue(empty.entries().isEmpty());
    }

    @Test
    void negativeDwellIsRejected() {
        Plan emptyPlan = new Plan(PLAN_DATE, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, List.of());

        assertThrows(IllegalArgumentException.class,
              () -> JumpPathSchedule.calculate(emptyPlan, null, List.of(), List.of(-1), Mode.DEPART_AT,
                    EARLIEST_DEPARTURE, EARLIEST_DEPARTURE));
    }

    private static Plan plan(PlanetarySystem... systems) {
        List<TimelineEntry> entries = new ArrayList<>();
        for (int index = 0; index < systems.length; index++) {
            boolean origin = index == 0;
            boolean destination = index == systems.length - 1;
            double arrivalDays = index;
            double departureDays = destination ? arrivalDays : arrivalDays + 1.0;
            entries.add(new TimelineEntry(index + 1, systems[index], origin, destination, arrivalDays,
                  origin || destination ? 0 : 24, departureDays, destination ? 2.25 : 0.0,
                  destination ? 6.25 : departureDays));
        }
        return new Plan(PLAN_DATE, 1.0, 0.0, 1.0, 2.25, 3.0, 6.25, entries);
    }

    private static PlanetarySystem system(String name) {
        return new PlanetarySystem(name);
    }
}
