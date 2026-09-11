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

import static java.lang.Math.sqrt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import mekhq.campaign.JumpPathItinerary.CircuitPlan;
import mekhq.campaign.JumpPathItinerary.Plan;
import mekhq.campaign.JumpPathItinerary.TimelineEntry;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class JumpPathItineraryTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);
    private static final double TOLERANCE = 1.0e-9;

    @Test
    void oneGPlanMatchesJumpPathTotalAndBuildsRechargeChronology() {
        PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
        PlanetarySystem recharge = system("RECHARGE", 3.0, 47.2);
        PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
        JumpPath path = pathOf(origin, recharge, destination);

        Plan plan = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 1.5, false);

        assertEquals(path.getTotalTime(TEST_DATE, 1.5, false), plan.totalDays(), TOLERANCE);
        assertEquals(2.5, plan.startingTransitDays(), TOLERANCE);
        assertEquals(2.0, plan.rechargeDays(), TOLERANCE);
        assertEquals(6.0, plan.endingTransitDays(), TOLERANCE);
        assertEquals(10.5, plan.totalDays(), TOLERANCE);

        TimelineEntry originEntry = plan.entries().get(0);
        TimelineEntry rechargeEntry = plan.entries().get(1);
        TimelineEntry destinationEntry = plan.entries().get(2);
        assertEquals(0.0, originEntry.arrivalElapsedDays(), TOLERANCE);
        assertEquals(2.5, originEntry.departureElapsedDays(), TOLERANCE);
        assertEquals(2.5, rechargeEntry.arrivalElapsedDays(), TOLERANCE);
        assertEquals(48, rechargeEntry.rechargeHours());
        assertEquals(4.5, rechargeEntry.departureElapsedDays(), TOLERANCE);
        assertEquals(4.5, destinationEntry.arrivalElapsedDays(), TOLERANCE);
        assertEquals(10.5, destinationEntry.endpointArrivalElapsedDays(), TOLERANCE);
    }

    @Test
    void accelerationScalesEndpointTransitAndInverseRoundTrips() {
        PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
        PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
        JumpPath path = pathOf(origin, destination);

        Plan fourGPlan = JumpPathItinerary.calculate(path, TEST_DATE, 4.0, origin, 0.0, false);

        assertEquals(2.0, fourGPlan.startingTransitDays(), TOLERANCE);
        assertEquals(3.0, fourGPlan.endingTransitDays(), TOLERANCE);
        assertEquals(5.0, fourGPlan.totalDays(), TOLERANCE);
        assertEquals(4.0, JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, 5.0, origin, 0.0, false)
                                .accelerationG().orElseThrow(), TOLERANCE);

        double oneGTotal = path.getTotalTime(TEST_DATE, 0.0, false);
        assertEquals(1.0, JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, oneGTotal, origin, 0.0, false)
                                .accelerationG().orElseThrow(), TOLERANCE);
    }

    @Test
    void desiredDurationWithoutPositiveTransitBudgetIsImpossible() {
        PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
        PlanetarySystem recharge = system("RECHARGE", 3.0, 48.0);
        PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
        JumpPath path = pathOf(origin, recharge, destination);

        assertFalse(JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, 2.0, origin, 0.0, false)
                          .isPossible());
        assertFalse(JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, 1.0, origin, 0.0, false)
                          .isPossible());
    }

        @Test
        void noneWholeAndCustomPlansApplyCoveragePerIntermediateDeparture() {
          PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
          PlanetarySystem firstRecharge = system("FIRST_RECHARGE", 3.0, 48.0, 12.0);
          PlanetarySystem secondRecharge = system("SECOND_RECHARGE", 3.0, 72.0, 24.0);
          PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
          JumpPath path = pathOf(origin, firstRecharge, secondRecharge, destination);

          Plan none = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0, CircuitPlan.none());
          Plan whole = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0, CircuitPlan.whole());
          Plan custom = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0,
              CircuitPlan.custom(Set.of(1)));

          assertEquals(5.0, none.rechargeDays(), TOLERANCE);
          assertEquals(1.5, whole.rechargeDays(), TOLERANCE);
          assertEquals(3.5, custom.rechargeDays(), TOLERANCE);
          assertEquals(12, custom.entries().get(1).rechargeHours());
          assertEquals(72, custom.entries().get(2).rechargeHours());
          assertEquals(none, JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0, false));
          assertEquals(whole, JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0, true));
        }

        @Test
        void inverseSolverUsesSelectedCustomCoverage() {
          PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
          PlanetarySystem firstRecharge = system("FIRST_RECHARGE", 3.0, 48.0, 12.0);
          PlanetarySystem secondRecharge = system("SECOND_RECHARGE", 3.0, 72.0, 24.0);
          PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
          JumpPath path = pathOf(origin, firstRecharge, secondRecharge, destination);
        CircuitPlan custom = CircuitPlan.custom(Set.of(1));
          double fourGCustomTotal = JumpPathItinerary.calculate(path, TEST_DATE, 4.0, origin, 0.0, custom).totalDays();

          double customRequired = JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, fourGCustomTotal,
              origin, 0.0, custom).accelerationG().orElseThrow();
          double noneRequired = JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, fourGCustomTotal,
              origin, 0.0, CircuitPlan.none()).accelerationG().orElseThrow();

          assertEquals(4.0, customRequired, TOLERANCE);
          assertTrue(noneRequired > customRequired);
        }

        @Test
        void customCoverageIsCoherentWithoutIntermediateSystems() {
          PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
          PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
          JumpPath path = pathOf(origin, destination);

          assertEquals(JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0, CircuitPlan.none()),
              JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0,
                  CircuitPlan.custom(Set.of())));
        }

        @Test
        void customCoverageDistinguishesRepeatedSystemDepartures() {
          PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
          PlanetarySystem repeated = system("REPEATED", 3.0, 48.0, 12.0);
          PlanetarySystem requestedStop = system("REQUESTED_STOP", 3.0, 24.0);
          PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
          JumpPath path = pathOf(origin, repeated, requestedStop, repeated, destination);

          Plan plan = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 0.0,
              CircuitPlan.custom(Set.of(1)));

          assertEquals(12, plan.entries().get(1).rechargeHours());
          assertEquals(48, plan.entries().get(3).rechargeHours());
        }

    @Test
    void destinationTargetPlanetSuppliesEndpointTransit() {
        PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
        PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
        Planet target = mock(Planet.class);
        when(target.getTimeToJumpPoint(anyDouble())).thenAnswer(invocation -> 9.0 / sqrt(invocation.getArgument(0)));
        JumpPath path = pathOf(origin, destination);
        path.setTargetPlanet(target);

        Plan plan = JumpPathItinerary.calculate(path, TEST_DATE, 4.0, origin, 0.0, false);

        assertEquals(4.5, plan.endingTransitDays(), TOLERANCE);
        assertEquals(6.5, plan.totalDays(), TOLERANCE);
        assertEquals(4.5, plan.entries().getLast().endpointTransitDays(), TOLERANCE);
    }

    @Test
    void currentProgressAppliesOnlyWhenRouteStartsAtFleet() {
        PlanetarySystem origin = system("ORIGIN", 4.0, 0.0);
        PlanetarySystem destination = system("DESTINATION", 6.0, 0.0);
        PlanetarySystem fleetElsewhere = system("ELSEWHERE", 2.0, 0.0);
        JumpPath path = pathOf(origin, destination);

        Plan fleetPlan = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, origin, 1.5, false);
        Plan whatIfPlan = JumpPathItinerary.calculate(path, TEST_DATE, 1.0, fleetElsewhere, 1.5, false);

        assertEquals(1.5, fleetPlan.appliedCurrentTransitDays(), TOLERANCE);
        assertEquals(8.5, fleetPlan.totalDays(), TOLERANCE);
        assertEquals(0.0, whatIfPlan.appliedCurrentTransitDays(), TOLERANCE);
        assertEquals(10.0, whatIfPlan.totalDays(), TOLERANCE);
    }

    @Test
    void emptyAndSingleSystemPathsHaveDeterministicPlans() {
        JumpPath emptyPath = new JumpPath();
        Plan emptyPlan = JumpPathItinerary.calculate(emptyPath, TEST_DATE, 1.0, null, 3.0, false);
        assertEquals(0.0, emptyPlan.totalDays(), TOLERANCE);
        assertTrue(emptyPlan.entries().isEmpty());
        assertFalse(JumpPathItinerary.solveRequiredAcceleration(emptyPath, TEST_DATE, 5.0, null, 3.0, false)
                          .isPossible());

        PlanetarySystem onlySystem = system("ONLY", 4.0, 0.0);
        JumpPath singlePath = pathOf(onlySystem);
        Plan singlePlan = JumpPathItinerary.calculate(singlePath, TEST_DATE, 1.0, onlySystem, 1.0, false);
        assertEquals(singlePath.getTotalTime(TEST_DATE, 1.0, false), singlePlan.totalDays(), TOLERANCE);
        assertEquals(7.0, singlePlan.totalDays(), TOLERANCE);
        assertTrue(singlePlan.entries().getFirst().origin());
        assertTrue(singlePlan.entries().getFirst().destination());
        assertEquals(3.0, singlePlan.entries().getFirst().departureElapsedDays(), TOLERANCE);
        assertEquals(7.0, singlePlan.entries().getFirst().endpointArrivalElapsedDays(), TOLERANCE);
    }

    @Test
    void rejectsInvalidNumericInputs() {
        JumpPath path = pathOf(system("ONLY", 4.0, 0.0));

        assertThrows(IllegalArgumentException.class,
              () -> JumpPathItinerary.calculate(path, TEST_DATE, 0.0, null, 0.0, false));
        assertThrows(IllegalArgumentException.class,
              () -> JumpPathItinerary.calculate(path, TEST_DATE, Double.POSITIVE_INFINITY, null, 0.0, false));
        assertThrows(IllegalArgumentException.class,
              () -> JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, Double.NaN, null, 0.0, false));
        assertThrows(IllegalArgumentException.class,
              () -> JumpPathItinerary.solveRequiredAcceleration(path, TEST_DATE, -1.0, null, 0.0, false));
    }

    private static PlanetarySystem system(String id, double oneGTransitDays, double rechargeHours) {
          return system(id, oneGTransitDays, rechargeHours, rechargeHours);
        }

        private static PlanetarySystem system(String id, double oneGTransitDays, double rechargeHours,
            double circuitRechargeHours) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        when(system.getTimeToJumpPoint(anyDouble()))
              .thenAnswer(invocation -> oneGTransitDays / sqrt(invocation.getArgument(0)));
          when(system.getRechargeTime(any(LocalDate.class), anyBoolean()))
              .thenAnswer(invocation -> (boolean) invocation.getArgument(1)
                                       ? circuitRechargeHours
                                       : rechargeHours);
        return system;
    }

    private static JumpPath pathOf(PlanetarySystem... systems) {
        JumpPath path = new JumpPath();
        for (PlanetarySystem system : systems) {
            path.addSystem(system);
        }
        return path;
    }
}
