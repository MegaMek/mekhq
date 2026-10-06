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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChaosPlanetStrategicValueTest {
    private static final LocalDate TEST_DATE = LocalDate.of(3025, 1, 1);

    private static SocioIndustrialData socioIndustrial(final PlanetarySophistication tech,
          final PlanetaryRating industry,
          final PlanetaryRating output) {
        SocioIndustrialData data = new SocioIndustrialData();
        data.tech = tech;
        data.industry = industry;
        data.output = output;
        return data;
    }

    // --- industry / output (PlanetaryRating: A best, F worst) ---

    @ParameterizedTest
    @CsvSource({ "A, 4", "B, 3", "C, 2", "D, 1", "F, 0" })
    void industryPointsMapsRatingHighToLow(final PlanetaryRating industry, final int expected) {
        SocioIndustrialData data = socioIndustrial(PlanetarySophistication.C, industry, PlanetaryRating.C);
        assertEquals(expected, ChaosPlanetStrategicValue.industryPoints(data));
    }

    @ParameterizedTest
    @CsvSource({ "A, 4", "B, 3", "C, 2", "D, 1", "F, 0" })
    void outputPointsMapsRatingHighToLow(final PlanetaryRating output, final int expected) {
        SocioIndustrialData data = socioIndustrial(PlanetarySophistication.C, PlanetaryRating.C, output);
        assertEquals(expected, ChaosPlanetStrategicValue.outputPoints(data));
    }

    @Test
    void industryAndOutputPointsAreZeroWhenSocioIndustrialMissing() {
        assertEquals(0, ChaosPlanetStrategicValue.industryPoints(null));
        assertEquals(0, ChaosPlanetStrategicValue.outputPoints(null));
    }

    // --- tech (PlanetarySophistication: ADVANCED best, REGRESSED worst) ---

    @ParameterizedTest
    @CsvSource({ "ADVANCED, 4", "A, 4", "B, 3", "C, 2", "D, 1", "F, 0", "REGRESSED, 0" })
    void techPointsMapsSophistication(final PlanetarySophistication tech, final int expected) {
        SocioIndustrialData data = socioIndustrial(tech, PlanetaryRating.C, PlanetaryRating.C);
        assertEquals(expected, ChaosPlanetStrategicValue.techPoints(data));
    }

    @Test
    void techPointsIsZeroWhenSocioIndustrialOrTechMissing() {
        assertEquals(0, ChaosPlanetStrategicValue.techPoints(null));
        assertEquals(0,
              ChaosPlanetStrategicValue.techPoints(socioIndustrial(null, PlanetaryRating.A, PlanetaryRating.A)));
    }

    // --- HPG (A best, X worst) ---

    @ParameterizedTest
    @CsvSource({ "A, 4", "B, 3", "C, 2", "D, 1", "X, 0" })
    void hpgPointsMapsRating(final HPGRating hpg, final int expected) {
        assertEquals(expected, ChaosPlanetStrategicValue.hpgPoints(hpg));
    }

    @Test
    void hpgPointsIsZeroWhenMissing() {
        assertEquals(0, ChaosPlanetStrategicValue.hpgPoints(null));
    }

    // --- population brackets ---

    @ParameterizedTest
    @CsvSource({ "999999, 1", "1000000, 2", "99999999, 2", "100000000, 3", "999999999, 3", "1000000000, 4",
                 "50000000000, 4" })
    void populationPointsBracketsInhabitants(final long population, final int expected) {
        assertEquals(expected, ChaosPlanetStrategicValue.populationPoints(population));
    }

    @Test
    void populationPointsIsZeroWhenNullOrNonPositive() {
        assertEquals(0, ChaosPlanetStrategicValue.populationPoints(null));
        assertEquals(0, ChaosPlanetStrategicValue.populationPoints(0L));
        assertEquals(0, ChaosPlanetStrategicValue.populationPoints(-5L));
    }

    // --- contested control ---

    @Test
    void contestedBonusOnlyWhenMoreThanOneFaction() {
        assertEquals(0, ChaosPlanetStrategicValue.contestedBonus(null));
        assertEquals(0, ChaosPlanetStrategicValue.contestedBonus(List.of()));
        assertEquals(0, ChaosPlanetStrategicValue.contestedBonus(List.of("FS")));
        assertEquals(ChaosPlanetStrategicValue.CONTESTED_BONUS,
              ChaosPlanetStrategicValue.contestedBonus(List.of("FS", "DC")));
        assertEquals(ChaosPlanetStrategicValue.CONTESTED_BONUS,
              ChaosPlanetStrategicValue.contestedBonus(List.of("FS", "DC", "LA")));
    }

    // --- aggregate calculate(...) ---

    @Test
    void calculateReturnsMaxForATopTierContestedWorld() {
        Planet planet = mock(Planet.class);
        when(planet.getSocioIndustrial(TEST_DATE)).thenReturn(
              socioIndustrial(PlanetarySophistication.ADVANCED, PlanetaryRating.A, PlanetaryRating.A));
        when(planet.getHPG(TEST_DATE)).thenReturn(HPGRating.A);
        when(planet.getPopulation(TEST_DATE)).thenReturn(5_000_000_000L);
        when(planet.getFactions(TEST_DATE)).thenReturn(List.of("FS", "DC"));

        assertEquals(ChaosPlanetStrategicValue.MAX_STRATEGIC_VALUE,
              ChaosPlanetStrategicValue.calculate(planet, TEST_DATE));
    }

    @Test
    void calculateReturnsZeroForABarrenUncontestedWorld() {
        Planet planet = mock(Planet.class);
        when(planet.getSocioIndustrial(TEST_DATE)).thenReturn(null);
        when(planet.getHPG(TEST_DATE)).thenReturn(HPGRating.X);
        when(planet.getPopulation(TEST_DATE)).thenReturn(null);
        when(planet.getFactions(TEST_DATE)).thenReturn(List.of());

        assertEquals(0, ChaosPlanetStrategicValue.calculate(planet, TEST_DATE));
    }

    @Test
    void calculateSumsComponentsForAModerateWorld() {
        Planet planet = mock(Planet.class);
        // industry C (2) + output D (1) + tech C (2) + HPG C (2) + population tier 2 (2) + uncontested (0) = 9
        when(planet.getSocioIndustrial(TEST_DATE)).thenReturn(
              socioIndustrial(PlanetarySophistication.C, PlanetaryRating.C, PlanetaryRating.D));
        when(planet.getHPG(TEST_DATE)).thenReturn(HPGRating.C);
        when(planet.getPopulation(TEST_DATE)).thenReturn(5_000_000L);
        when(planet.getFactions(TEST_DATE)).thenReturn(List.of("FS"));

        assertEquals(9, ChaosPlanetStrategicValue.calculate(planet, TEST_DATE));
    }
}
