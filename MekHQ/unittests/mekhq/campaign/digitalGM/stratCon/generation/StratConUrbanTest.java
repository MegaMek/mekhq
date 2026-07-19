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
package mekhq.campaign.digitalGM.stratCon.generation;

import static mekhq.campaign.digitalGM.stratCon.generation.StratConUrban.DEFAULT_HABITABILITY_SIGMA;
import static mekhq.campaign.digitalGM.stratCon.generation.StratConUrban.DEFAULT_POPULATION_SIGMA;
import static mekhq.campaign.digitalGM.stratCon.generation.StratConUrban.DEFAULT_TECH_SIGMA;
import static mekhq.campaign.digitalGM.stratCon.generation.StratConUrban.DEFAULT_WATER_SIGMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConUrban}: YAML loading, multi-factor weighting, and condition-driven selection. Backed by the
 * authored {@code UrbanProfiles.yaml}.
 */
class StratConUrbanTest {

    private static StratConUrban urban() {
        return StratConUrban.getInstance();
    }

    private static PlanetProfile planet(double populationLog, int waterPercent, int temperatureCelsius, HPGRating hpg) {
        long population = (long) Math.pow(10, populationLog);
        return new PlanetProfile(temperatureCelsius,
              PlanetProfile.TERRA_DIAMETER_KM,
              waterPercent,
              false,
              null,
              "",
              1,
              1.0,
              population,
              hpg);
    }

    private static UrbanProfileType highestWeightType(PlanetProfile planet) {
        StratConUrban urban = urban();
        UrbanProfile best = null;
        double bestWeight = -1.0;
        for (UrbanProfile profile : urban.getProfiles()) {
            double weight = StratConUrban.weight(profile,
                  planet,
                  urban.getPopulationSigma(),
                  urban.getWaterSigma(),
                  urban.getHabitabilitySigma(),
                  urban.getTechSigma());
            if (weight > bestWeight) {
                bestWeight = weight;
                best = profile;
            }
        }
        assertNotNull(best);
        return best.type();
    }

    @Test
    void yaml_loadsAllSixProfilesAndSigmas() {
        StratConUrban urban = urban();

        assertEquals(6, urban.getProfiles().size());
        assertEquals(1.5, urban.getPopulationSigma());
        assertEquals(20.0, urban.getWaterSigma());
        assertEquals(0.3, urban.getHabitabilitySigma());
        assertEquals(0.3, urban.getTechSigma());
        urban.getProfiles().forEach(profile -> assertNotNull(profile.type()));
    }

    @Test
    void wetWorld_favorsCoastalPorts() {
        // water exactly at the coastal center; population and habitability off the other profiles' centers
        assertEquals(UrbanProfileType.COASTAL_PORTS, highestWeightType(planet(8, 65, 33, HPGRating.C)));
    }

    @Test
    void teemingIndustrialWorld_favorsConurbation() {
        // water midway between the coastal and riverine centers, so population + tech decide
        assertEquals(UrbanProfileType.CONURBATION, highestWeightType(planet(10, 50, 15, HPGRating.A)));
    }

    @Test
    void sparseHarshWorld_favorsFrontierOutposts() {
        // low population and low habitability (temperature far from comfortable)
        assertEquals(UrbanProfileType.FRONTIER_OUTPOSTS, highestWeightType(planet(4, 20, 63, HPGRating.X)));
    }

    @Test
    void indifferentProfile_hasNeutralWeightRegardlessOfPlanet() {
        UrbanProfile indifferent = new UrbanProfile(UrbanProfileType.DISPERSED, null, null, null, null, null, null,
              null, null, null);

        double dry = StratConUrban.weight(indifferent,
              planet(4, 5, -40, HPGRating.X),
              DEFAULT_POPULATION_SIGMA,
              DEFAULT_WATER_SIGMA,
              DEFAULT_HABITABILITY_SIGMA,
              DEFAULT_TECH_SIGMA);
        double wet = StratConUrban.weight(indifferent,
              planet(10, 90, 15, HPGRating.A),
              DEFAULT_POPULATION_SIGMA,
              DEFAULT_WATER_SIGMA,
              DEFAULT_HABITABILITY_SIGMA,
              DEFAULT_TECH_SIGMA);

        assertEquals(1.0, dry);
        assertEquals(1.0, wet);
    }

    @Test
    void populationGaussian_peaksAtCenter() {
        UrbanProfile populationOnly = new UrbanProfile(UrbanProfileType.PRIMATE_CITY, 8.0, null, null, null, null, null,
              null, null, null);

        double atCenter = StratConUrban.weight(populationOnly,
              planet(8, 50, 15, HPGRating.C),
              DEFAULT_POPULATION_SIGMA,
              DEFAULT_WATER_SIGMA,
              DEFAULT_HABITABILITY_SIGMA,
              DEFAULT_TECH_SIGMA);
        double offCenter = StratConUrban.weight(populationOnly,
              planet(4, 50, 15, HPGRating.C),
              DEFAULT_POPULATION_SIGMA,
              DEFAULT_WATER_SIGMA,
              DEFAULT_HABITABILITY_SIGMA,
              DEFAULT_TECH_SIGMA);

        assertEquals(1.0, atCenter, 1.0e-9);
        assertTrue(offCenter < atCenter);
    }

    @Test
    void placementDefaults_areNeutralWhenOmitted() {
        UrbanProfile bare = new UrbanProfile(UrbanProfileType.DISPERSED, null, null, null, null, null, null, null, null,
              null);
        assertEquals(1.0, bare.cityCountModifierOrDefault());
        assertEquals(0.0, bare.clusteringOrDefault());
        assertEquals(0.0, bare.coastalBiasOrDefault());
        assertEquals(1, bare.farmReachOrDefault());
        assertEquals(0.5, bare.farmDensityOrDefault());
    }

    @Test
    void selectProfile_alwaysReturnsALoadedProfile() {
        StratConUrban urban = urban();
        for (double populationLog = 4; populationLog <= 10; populationLog += 2) {
            for (int water = 0; water <= 100; water += 25) {
                UrbanProfile selected = urban.selectProfile(planet(populationLog, water, 15, HPGRating.C));
                assertNotNull(selected);
                assertTrue(urban.getProfiles().contains(selected));
            }
        }
    }
}
