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
import mekhq.campaign.universe.enums.PlanetaryType;
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
        when(planet.getPlanetType()).thenReturn(PlanetaryType.TERRESTRIAL);
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

    /** A terrestrial planet mocked with only its population set &mdash; enough for eligibility filtering. */
    private static Planet planetWithPopulation(final Long population) {
        return planetOfType(PlanetaryType.TERRESTRIAL, population);
    }

    /** A planet mocked with only its type and population set &mdash; enough for eligibility filtering. */
    private static Planet planetOfType(final PlanetaryType planetType, final Long population) {
        Planet planet = mock(Planet.class);
        when(planet.getPlanetType()).thenReturn(planetType);
        when(planet.getPopulation(TEST_DATE)).thenReturn(population);
        return planet;
    }

    /**
     * A planet mocked with the given strategic value that also has a recorded population. The small population is
     * itself worth one point, so the other components carry {@code strategicValue - 1}; valid for values {@code 1..17},
     * where those components leave population empty.
     */
    private static Planet inhabitedPlanetWithStrategicValue(final int strategicValue) {
        Planet planet = planetWithStrategicValue(strategicValue - 1);
        when(planet.getPopulation(TEST_DATE)).thenReturn(1_000L);
        return planet;
    }

    // --- preferenceFor ---

    @ParameterizedTest
    @CsvSource({ "INVASION, HIGH_VALUE", "GARRISON, HIGH_VALUE", "RAID, HIGH_VALUE",
                 "GUERILLA_OPERATION, HIGH_VALUE", "PIRATE_HUNT, LOW_VALUE", "PIRATE_RAID, LOW_VALUE",
                 "EXPEDITION, NEUTRAL", "CADRE_DUTY, NEUTRAL" })
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
        // An inhabited world with strategicValue 8 -> weight 1 + (MAX - 8)
        assertEquals(1 + (ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE - 8),
              ChaosPlanetSelector.planetWeight(inhabitedPlanetWithStrategicValue(8), ChaosObjectiveType.PIRATE_HUNT,
                    TEST_DATE));
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

    @Test
    void planetWeightKeepsUninhabitedBodiesAtTheFloorForPirateHunts() {
        // Uninhabited bodies have no strategic value, but must not outweigh the inhabited backwaters.
        assertEquals(ChaosPlanetSelector.UNINHABITED_WORLD_WEIGHT,
              ChaosPlanetSelector.planetWeight(planetWithStrategicValue(0), ChaosObjectiveType.PIRATE_HUNT, TEST_DATE));
    }

    // --- selectTargetPlanet ---

    @Test
    void selectTargetPlanetReturnsNullWhenNoCandidates() {
        assertNull(ChaosPlanetSelector.selectTargetPlanet(List.of(), null, ChaosObjectiveType.RAID, TEST_DATE));
    }

    @Test
    void selectTargetPlanetReturnsTheOnlyCandidate() {
        Planet only = planetWithStrategicValue(5);
        assertSame(only,
              ChaosPlanetSelector.selectTargetPlanet(List.of(only), null, ChaosObjectiveType.RAID, TEST_DATE));
    }

    @Test
    void selectTargetPlanetNeverPicksAnUninhabitedWorldForAHighValueObjectiveWhenAnInhabitedOneExists() {
        Planet prize = planetWithStrategicValue(ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE); // inhabited
        Planet uninhabited = planetWithStrategicValue(0); // no population

        for (int i = 0; i < 500; i++) {
            assertSame(prize, ChaosPlanetSelector.selectTargetPlanet(List.of(prize, uninhabited), null,
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

    @ParameterizedTest
    @CsvSource({ "INVASION", "GARRISON", "RAID", "GUERILLA_OPERATION", "PIRATE_RAID", "EXPEDITION", "CADRE_DUTY" })
    void eligiblePlanetsKeepsOnlyInhabitedWorldsWhenOneExists(final ChaosObjectiveType objectiveType) {
        Planet inhabited = planetWithPopulation(1_000L);
        Planet uninhabited = planetWithPopulation(null);

        assertEquals(List.of(inhabited),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(objectiveType, List.of(inhabited, uninhabited), null,
                    TEST_DATE)));
    }

    @Test
    void eligiblePlanetsFallsBackToUninhabitedGroundWhenNoWorldIsInhabited() {
        Planet uninhabitedA = planetWithPopulation(null);
        Planet uninhabitedB = planetOfType(PlanetaryType.DWARF_TERRESTRIAL, 0L);
        Planet gasGiant = planetOfType(PlanetaryType.GAS_GIANT, null);

        assertEquals(List.of(uninhabitedA, uninhabitedB),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(ChaosObjectiveType.INVASION,
                    List.of(uninhabitedA, gasGiant, uninhabitedB), gasGiant, TEST_DATE)));
    }

    @Test
    void eligiblePlanetsKeepsUninhabitedGroundForAPirateHunt() {
        Planet inhabited = planetWithPopulation(1_000L);
        Planet uninhabited = planetWithPopulation(null);

        assertEquals(List.of(inhabited, uninhabited),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(ChaosObjectiveType.PIRATE_HUNT,
                    List.of(inhabited, uninhabited), null, TEST_DATE)));
    }

    @ParameterizedTest
    @CsvSource({ "INVASION", "PIRATE_HUNT", "PIRATE_RAID", "EXPEDITION" })
    void eligiblePlanetsNeverIncludesUninhabitedBodiesWithoutGround(final ChaosObjectiveType objectiveType) {
        Planet ground = planetWithPopulation(null);
        List<Planet> candidates = List.of(ground,
              planetOfType(PlanetaryType.GAS_GIANT, null),
              planetOfType(PlanetaryType.ICE_GIANT, null),
              planetOfType(PlanetaryType.ASTEROID_BELT, null),
              planetOfType(PlanetaryType.GIANT_TERRESTRIAL, null));

        assertEquals(List.of(ground),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(objectiveType, candidates, null, TEST_DATE)));
    }

    @Test
    void eligiblePlanetsKeepsAnInhabitedHabitatWhateverItsType() {
        // Canon settlements such as habitats in asteroid belts remain valid targets.
        Planet habitat = planetOfType(PlanetaryType.ASTEROID_BELT, 50_000L);
        Planet barrenRock = planetWithPopulation(null);

        assertEquals(List.of(habitat),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(ChaosObjectiveType.GARRISON,
                    List.of(habitat, barrenRock), null, TEST_DATE)));
    }

    @Test
    void eligiblePlanetsFallsBackToThePrimaryWhenNoBodyHasGround() {
        Planet primary = planetOfType(PlanetaryType.GAS_GIANT, null);
        Planet belt = planetOfType(PlanetaryType.ASTEROID_BELT, null);

        assertEquals(List.of(primary),
              List.copyOf(ChaosPlanetSelector.eligiblePlanets(ChaosObjectiveType.PIRATE_HUNT,
                    List.of(belt, primary), primary, TEST_DATE)));
    }
}
