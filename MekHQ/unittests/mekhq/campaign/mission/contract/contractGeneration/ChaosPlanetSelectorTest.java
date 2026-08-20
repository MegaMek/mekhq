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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.mission.contract.contractGeneration.ChaosPlanetSelector.PlanetValuePreference;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChaosPlanetSelectorTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    /**
     * A planet mocked to yield exactly the requested {@link ChaosPlanetStrategicValue} (0..{@code MAX}), by
     * distributing the value across its components in order: industry, output, tech, and HPG (each 0..4), population
     * (0..4), then the contested bonus (0 or 2).
     */
    private static Planet planetWithStrategicValue(final int strategicValue) {
        int remaining = strategicValue;
        int industry = take(remaining, 4);
        remaining -= industry;
        int output = take(remaining, 4);
        remaining -= output;
        int tech = take(remaining, 4);
        remaining -= tech;
        int hpg = take(remaining, 4);
        remaining -= hpg;
        int population = take(remaining, 4);
        remaining -= population;
        boolean contested = remaining >= ChaosPlanetStrategicValue.CONTESTED_BONUS;

        Planet planet = mock(Planet.class);
        when(planet.getSocioIndustrial(TEST_DATE)).thenReturn(
              socioIndustrial(sophisticationForPoints(tech), ratingForPoints(industry), ratingForPoints(output)));
        when(planet.getHPG(TEST_DATE)).thenReturn(hpgForPoints(hpg));
        when(planet.getPopulation(TEST_DATE)).thenReturn(populationForPoints(population));
        when(planet.getFactions(TEST_DATE)).thenReturn(contested ? List.of("FS", "DC") : List.of());
        return planet;
    }

    private static int take(final int remaining, final int cap) {
        return Math.max(0, Math.min(remaining, cap));
    }

    private static SocioIndustrialData socioIndustrial(final PlanetarySophistication tech,
          final PlanetaryRating industry,
          final PlanetaryRating output) {
        SocioIndustrialData data = new SocioIndustrialData();
        data.tech = tech;
        data.industry = industry;
        data.output = output;
        return data;
    }

    /** Inverse of {@link ChaosPlanetStrategicValue}'s {@code 4 - index} rating mapping: points 0..4 -> F,D,C,B,A. */
    private static PlanetaryRating ratingForPoints(final int points) {
        return switch (points) {
            case 4 -> PlanetaryRating.A;
            case 3 -> PlanetaryRating.B;
            case 2 -> PlanetaryRating.C;
            case 1 -> PlanetaryRating.D;
            default -> PlanetaryRating.F;
        };
    }

    private static PlanetarySophistication sophisticationForPoints(final int points) {
        return switch (points) {
            case 4 -> PlanetarySophistication.ADVANCED;
            case 3 -> PlanetarySophistication.B;
            case 2 -> PlanetarySophistication.C;
            case 1 -> PlanetarySophistication.D;
            default -> PlanetarySophistication.REGRESSED;
        };
    }

    private static HPGRating hpgForPoints(final int points) {
        return switch (points) {
            case 4 -> HPGRating.A;
            case 3 -> HPGRating.B;
            case 2 -> HPGRating.C;
            case 1 -> HPGRating.D;
            default -> HPGRating.X;
        };
    }

    private static Long populationForPoints(final int points) {
        return switch (points) {
            case 4 -> 2_000_000_000L;
            case 3 -> 500_000_000L;
            case 2 -> 50_000_000L;
            case 1 -> 500_000L;
            default -> null;
        };
    }

    /** A planet mocked with only its population set &mdash; enough for habitation filtering, which reads nothing else. */
    private static Planet planetWithPopulation(final Long population) {
        Planet planet = mock(Planet.class);
        when(planet.getPopulation(TEST_DATE)).thenReturn(population);
        return planet;
    }

    // --- preferenceFor ---

    @ParameterizedTest
    @CsvSource({ "INVASION, HIGH_VALUE", "GARRISON, HIGH_VALUE", "RAID, HIGH_VALUE", "PIRATE_RAID, HIGH_VALUE",
                 "GUERILLA_OPERATION, HIGH_VALUE", "PIRATE_HUNT, LOW_VALUE", "EXPEDITION, NEUTRAL",
                 "CADRE_DUTY, NEUTRAL" })
    void preferenceForByObjective(final ChaosObjectiveType objectiveType, final PlanetValuePreference expected) {
        assertEquals(expected, ChaosPlanetSelector.preferenceFor(objectiveType));
    }

    // --- planetWeight ---

    @Test
    void planetWeightRewardsValueForHighValueObjectives() {
        // strategicValue 8 -> weight 1 + 8 = 9
        assertEquals(9, ChaosPlanetSelector.planetWeight(planetWithStrategicValue(8), ChaosObjectiveType.INVASION,
              TEST_DATE));
        // A barren world is still pickable at the floor weight of 1.
        assertEquals(1, ChaosPlanetSelector.planetWeight(planetWithStrategicValue(0), ChaosObjectiveType.INVASION,
              TEST_DATE));
    }

    @Test
    void planetWeightRewardsBackwatersForPirateHunts() {
        // strategicValue 8 -> weight 1 + (MAX - 8)
        assertEquals(1 + (ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE - 8),
              ChaosPlanetSelector.planetWeight(planetWithStrategicValue(8), ChaosObjectiveType.PIRATE_HUNT, TEST_DATE));
        // A prize world is least attractive to a pirate hunt, but still pickable at weight 1.
        assertEquals(1, ChaosPlanetSelector.planetWeight(
              planetWithStrategicValue(ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE), ChaosObjectiveType.PIRATE_HUNT,
              TEST_DATE));
    }

    @Test
    void planetWeightIsFlatForNeutralObjectives() {
        assertEquals(1, ChaosPlanetSelector.planetWeight(planetWithStrategicValue(8), ChaosObjectiveType.EXPEDITION,
              TEST_DATE));
        assertEquals(1, ChaosPlanetSelector.planetWeight(planetWithStrategicValue(0), ChaosObjectiveType.EXPEDITION,
              TEST_DATE));
    }

    // --- selectTargetPlanet ---

    @Test
    void selectTargetPlanetReturnsNullWhenNoCandidates() {
        assertNull(ChaosPlanetSelector.selectTargetPlanet(List.of(), ChaosObjectiveType.RAID, TEST_DATE));
    }

    @Test
    void selectTargetPlanetReturnsTheOnlyCandidate() {
        Planet only = planetWithStrategicValue(5);
        assertSame(only,
              ChaosPlanetSelector.selectTargetPlanet(List.of(only), ChaosObjectiveType.RAID, TEST_DATE));
    }

    @Test
    void selectTargetPlanetNeverPicksAnUninhabitedWorldForAHighValueObjectiveWhenAnInhabitedOneExists() {
        Planet prize = planetWithStrategicValue(ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE); // inhabited
        Planet uninhabited = planetWithStrategicValue(0); // no population

        for (int i = 0; i < 500; i++) {
            assertSame(prize, ChaosPlanetSelector.selectTargetPlanet(List.of(prize, uninhabited),
                        ChaosObjectiveType.INVASION, TEST_DATE),
                  "An invasion must never be situated on the uninhabited body when an inhabited world is available");
        }
    }

    // --- habitation filtering ---

    @Test
    void isInhabitedReflectsPopulation() {
        assertFalse(ChaosPlanetSelector.isInhabited(planetWithPopulation(null), TEST_DATE));
        assertFalse(ChaosPlanetSelector.isInhabited(planetWithPopulation(0L), TEST_DATE));
        assertTrue(ChaosPlanetSelector.isInhabited(planetWithPopulation(1L), TEST_DATE));
    }

    @Test
    void eligiblePlanetsKeepsOnlyInhabitedWorldsForValuedObjectives() {
        Planet inhabited = planetWithPopulation(1_000L);
        Planet uninhabited = planetWithPopulation(null);
        List<Planet> candidates = List.of(inhabited, uninhabited);

        assertEquals(List.of(inhabited),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(PlanetValuePreference.HIGH_VALUE,
                    candidates,
                    TEST_DATE)));
        assertEquals(List.of(inhabited),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(PlanetValuePreference.NEUTRAL, candidates, TEST_DATE)));
    }

    @Test
    void eligiblePlanetsFallsBackToAllBodiesWhenNoWorldIsInhabited() {
        Planet uninhabitedA = planetWithPopulation(null);
        Planet uninhabitedB = planetWithPopulation(0L);
        List<Planet> candidates = List.of(uninhabitedA, uninhabitedB);

        assertEquals(candidates.size(),
              ChaosPlanetSelector.eligiblePlanets(PlanetValuePreference.HIGH_VALUE, candidates, TEST_DATE).size());
    }

    @Test
    void eligiblePlanetsKeepsUninhabitedBodiesForAPirateHunt() {
        Planet inhabited = planetWithPopulation(1_000L);
        Planet uninhabited = planetWithPopulation(null);
        List<Planet> candidates = List.of(inhabited, uninhabited);

        assertEquals(candidates.size(),
              ChaosPlanetSelector.eligiblePlanets(PlanetValuePreference.LOW_VALUE, candidates, TEST_DATE).size());
    }
}
