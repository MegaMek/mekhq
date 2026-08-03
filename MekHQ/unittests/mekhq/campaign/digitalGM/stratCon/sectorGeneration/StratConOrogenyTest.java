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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConOrogeny.DEFAULT_GRAVITY_SIGMA;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConOrogeny.DEFAULT_TEMPERATURE_SIGMA;
import static mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConOrogeny.DEFAULT_WATER_SIGMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConOrogeny}: YAML loading, multi-factor weighting, and condition-driven selection. Backed by
 * the authored {@code OrogenyProfiles.yaml}.
 */
class StratConOrogenyTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConOrogeny orogeny() {
        return StratConOrogeny.getInstance();
    }

    private static PlanetProfile planet(int temperatureCelsius, int waterPercent, double gravity, String composition,
          boolean airless) {
        return new PlanetProfile(temperatureCelsius,
              PlanetProfile.TERRA_DIAMETER_KM,
              waterPercent,
              airless,
              null,
              composition,
              1,
              gravity,
              null,
              mekhq.campaign.universe.enums.HPGRating.X);
    }

    private static OrogenyProfileType highestWeightType(PlanetProfile planet) {
        StratConOrogeny orogeny = orogeny();
        OrogenyProfile best = null;
        double bestWeight = -1.0;
        for (OrogenyProfile profile : orogeny.getProfiles()) {
            double weight = StratConOrogeny.weight(profile,
                  planet,
                  orogeny.getGravitySigma(),
                  orogeny.getTemperatureSigma(),
                  orogeny.getWaterSigma());
            if (weight > bestWeight) {
                bestWeight = weight;
                best = profile;
            }
        }
        assertNotNull(best);
        return best.type();
    }

    @Test
    void yaml_loadsAllEightProfilesAndSigmas() {
        StratConOrogeny orogeny = orogeny();

        assertEquals(8, orogeny.getProfiles().size());
        assertEquals(0.4, orogeny.getGravitySigma());
        assertEquals(25.0, orogeny.getTemperatureSigma());
        assertEquals(25.0, orogeny.getWaterSigma());
        orogeny.getProfiles().forEach(profile -> assertNotNull(profile.type()));
    }

    @Test
    void airlessWorld_favorsShieldCratered() {
        assertEquals(OrogenyProfileType.SHIELD_CRATERED, highestWeightType(planet(30, 20, 1.0, "rock", true)));
    }

    @Test
    void aridHighGravityWorld_favorsBasinAndRange() {
        assertEquals(OrogenyProfileType.BASIN_AND_RANGE, highestWeightType(planet(20, 10, 1.4, "arid/rock", false)));
    }

    @Test
    void indifferentProfile_hasNeutralWeightRegardlessOfPlanet() {
        OrogenyProfile indifferent = new OrogenyProfile(OrogenyProfileType.CORDILLERA,
              null, null, null, null, null, null, null, null);

        double dry = StratConOrogeny.weight(indifferent,
              planet(-40, 5, 0.5, "ice", true),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);
        double wet = StratConOrogeny.weight(indifferent,
              planet(50, 90, 2.0, "rock", false),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);

        assertEquals(1.0, dry);
        assertEquals(1.0, wet);
    }

    @Test
    void gravityGaussian_peaksAtCenterAndFallsOff() {
        OrogenyProfile gravityOnly = new OrogenyProfile(OrogenyProfileType.MASSIF,
              1.0, null, null, null, null, null, null, null);

        double atCenter = StratConOrogeny.weight(gravityOnly,
              planet(20, 50, 1.0, "", false),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);
        double offCenter = StratConOrogeny.weight(gravityOnly,
              planet(20, 50, 1.6, "", false),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);

        assertEquals(1.0, atCenter, 1.0e-9);
        assertTrue(offCenter < atCenter);
    }

    @Test
    void rockyMultiplier_appliesOnlyOnRockyWorlds() {
        OrogenyProfile rockyBoost = new OrogenyProfile(OrogenyProfileType.PLATEAU,
              null, null, null, 2.0, null, null, null, null);

        double rocky = StratConOrogeny.weight(rockyBoost,
              planet(20, 20, 1.0, "rock", false),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);
        double notRocky = StratConOrogeny.weight(rockyBoost,
              planet(20, 20, 1.0, "", false),
              DEFAULT_GRAVITY_SIGMA,
              DEFAULT_TEMPERATURE_SIGMA,
              DEFAULT_WATER_SIGMA);

        assertEquals(2.0, rocky);
        assertEquals(1.0, notRocky);
    }

    @Test
    void omittedVolcanismChance_usesDefault() {
        OrogenyProfile noVolcanism = new OrogenyProfile(OrogenyProfileType.MASSIF,
              null, null, null, null, null, null, null, null);
        assertEquals(OrogenyProfile.DEFAULT_VOLCANISM_CHANCE, noVolcanism.volcanismChanceOrDefault());
    }

    @Test
    void selectProfile_alwaysReturnsALoadedProfile() {
        StratConOrogeny orogeny = orogeny();
        for (double gravity = 0.5; gravity <= 2.0; gravity += 0.5) {
            for (int water = 0; water <= 100; water += 25) {
                OrogenyProfile selected = orogeny.selectProfile(planet(20, water, gravity, "rock", false));
                assertNotNull(selected);
                assertTrue(orogeny.getProfiles().contains(selected));
            }
        }
    }
}
