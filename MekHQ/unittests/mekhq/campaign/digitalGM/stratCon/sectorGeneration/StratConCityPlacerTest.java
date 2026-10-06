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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import megamek.common.MMRandom;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConCityPlacer}: population-driven city counts, land-only placement, and the coastal/clustering
 * arrangement.
 */
class StratConCityPlacerTest {

    /**
     * Placement is random, and the two arrangement tests below compare two random samples against each other. Left to
     * the live RNG their tails overlap often enough to fail outright roughly once in a few hundred suite runs, so the
     * generator is pinned here: any failure is then reproducible rather than a coin flip.
     *
     * <p>The seed is not load-bearing. Both assertions were measured to hold for every one of 400 consecutive seeds
     * at the sample counts used below, so this seed is representative rather than a lucky pick.</p>
     */
    private static final long SEED = 20260818L;

    /** A reproducible stand-in for MegaMek's live RNG. */
    private static final class SeededRandom extends MMRandom {
        private final Random random;

        private SeededRandom(long seed) {
            this.random = new Random(seed);
        }

        @Override
        public int randomInt(int maxValue) {
            return random.nextInt(maxValue);
        }

        @Override
        public float randomFloat() {
            return random.nextFloat();
        }
    }

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void seedRandomness() {
        Compute.setRNG(new SeededRandom(SEED));
    }

    @AfterEach
    void restoreRandomness() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    private static final int SIZE = 20;

    /**
     * Sample count for the two arrangement tests. Both effects are directional rather than absolute - a bias shifts the
     * odds of each placement rather than dictating it - so each is measured over enough sectors for the sampling noise
     * to fall well below the effect.
     */
    private static final int ARRANGEMENT_RUNS = 32;

    private static PlanetProfile planet(Long population) {
        return new PlanetProfile(20, PlanetProfile.TERRA_DIAMETER_KM, 50, false, null, "", 1, 1.0, population,
              HPGRating.C);
    }

    private static UrbanProfile urban(double cityCountModifier, double clustering, double coastalBias) {
        return new UrbanProfile(UrbanProfileType.DISPERSED, null, null, null, null, cityCountModifier, clustering,
              coastalBias, null, null);
    }

    @Test
    void primateCity_neverSpreadsOntoWater() {
        // The blob grows outward from its seed rather than picking from the land list, so it needs the ocean handed to
        // it as a barrier. Given an empty barrier it will happily march into the sea, and the existing tests would not
        // notice: the contiguous-blob test uses an all-land sector, and the placesOnlyOnLand test uses a profile that
        // never reaches this path.
        StratConTrackState track = coastTrack();
        UrbanProfile primate = new UrbanProfile(UrbanProfileType.PRIMATE_CITY, null, null, null, null, 1.0, 1.0, 0.0,
              null, null);

        StratConCityPlacer.placeCities(track, planet(2_000_000_000L), primate);

        assertFalse(track.getCities().isEmpty(), "a primate city should place some city hexes");
        for (StratConCoords city : track.getCities()) {
            assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(city)),
                  "the primate city blob spread onto ocean at " + city);
        }
    }

    @Test
    void primateCity_placesOneContiguousBlob() {
        StratConTrackState track = landTrack();
        UrbanProfile primate = new UrbanProfile(UrbanProfileType.PRIMATE_CITY, null, null, null, null, 1.0, 0.0, 0.0,
              null, null);

        StratConCityPlacer.placeCities(track, planet(2_000_000_000L), primate);

        Set<StratConCoords> cities = track.getCities();
        assertTrue(cities.size() > 1, "a primate city should place several city hexes");

        // every city hex must be reachable from any other by stepping between adjacent city hexes (one connected blob)
        Set<StratConCoords> seen = new HashSet<>();
        Deque<StratConCoords> queue = new ArrayDeque<>();
        StratConCoords start = cities.iterator().next();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, queue.poll())) {
                if (cities.contains(neighbor) && seen.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        assertEquals(cities.size(), seen.size(), "primate city hexes should form a single connected blob");
    }

    /** An all-land track. */
    private static StratConTrackState landTrack() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                track.setTerrainTile(new StratConCoords(x, y), "Plains");
            }
        }
        return track;
    }

    /** A track with a wall of water down the west edge, the rest land. */
    private static StratConTrackState coastTrack() {
        StratConTrackState track = landTrack();
        for (int y = 0; y < SIZE; y++) {
            track.setTerrainTile(new StratConCoords(0, y), "Sea");
        }
        return track;
    }

    private static boolean isCoastal(StratConTrackState track, StratConCoords coords) {
        for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, coords)) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(neighbor))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void cityDensity_followsPopulationTiers() {
        assertEquals(0.0, StratConCityPlacer.cityDensity(null));
        assertEquals(0.0, StratConCityPlacer.cityDensity(5_000L));
        assertEquals(0.01, StratConCityPlacer.cityDensity(500_000L));
        assertEquals(0.02, StratConCityPlacer.cityDensity(50_000_000L));
        assertEquals(0.04, StratConCityPlacer.cityDensity(500_000_000L));
        assertEquals(0.06, StratConCityPlacer.cityDensity(2_000_000_000L));
    }

    @Test
    void cityCount_scalesWithDensityAndModifier() {
        assertEquals(0, StratConCityPlacer.cityCount(100, planet(null), urban(1.0, 0.0, 0.0)));
        assertEquals(1, StratConCityPlacer.cityCount(100, planet(500_000L), urban(1.0, 0.0, 0.0)));
        assertEquals(6, StratConCityPlacer.cityCount(100, planet(2_000_000_000L), urban(1.0, 0.0, 0.0)));
        assertEquals(3, StratConCityPlacer.cityCount(100, planet(2_000_000_000L), urban(0.5, 0.0, 0.0)));
    }

    @Test
    void unknownPopulation_placesNoCities() {
        StratConTrackState track = landTrack();
        StratConCityPlacer.placeCities(track, planet(null), urban(1.0, 0.0, 0.0));
        assertTrue(track.getCities().isEmpty());
    }

    @Test
    void placeCities_placesOnlyOnLand() {
        StratConTrackState track = coastTrack();
        StratConCityPlacer.placeCities(track, planet(2_000_000_000L), urban(1.0, 0.0, 0.0));

        assertFalse(track.getCities().isEmpty());
        for (StratConCoords city : track.getCities()) {
            assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(city)),
                  "a city was placed on ocean at " + city);
        }
    }

    @Test
    void coastalBias_pullsCitiesTowardTheShore() {
        // Fewer cities and a mild clustering (as the real COASTAL_PORTS profile uses) so the coastal pull is not
        // swamped by the spread term.
        long coastalWithBias = 0;
        long coastalWithoutBias = 0;
        for (int run = 0; run < ARRANGEMENT_RUNS; run++) {
            StratConTrackState biased = coastTrack();
            StratConCityPlacer.placeCities(biased, planet(50_000_000L), urban(1.0, 0.3, 0.9));
            coastalWithBias += biased.getCities().stream().filter(city -> isCoastal(biased, city)).count();

            StratConTrackState unbiased = coastTrack();
            StratConCityPlacer.placeCities(unbiased, planet(50_000_000L), urban(1.0, 0.3, 0.0));
            coastalWithoutBias += unbiased.getCities().stream().filter(city -> isCoastal(unbiased, city)).count();
        }

        // The shore is a thin slice of this sector, so an unbiased run lands on it only occasionally; a full bias
        // weights those hexes heavily enough to more than double the count. Asserting the size of the gap rather than
        // only its direction keeps a bias that had been reduced to a rounding error from still passing.
        assertTrue(coastalWithBias > (2 * coastalWithoutBias),
              "a coastal bias should place far more cities on the shore, but placed " + coastalWithBias
                    + " against " + coastalWithoutBias + " without it");
    }

    @Test
    void clustering_bringsCitiesCloserTogether() {
        double clusteredTotal = 0;
        double spreadTotal = 0;
        for (int run = 0; run < ARRANGEMENT_RUNS; run++) {
            clusteredTotal += meanNearestNeighborDistance(placeAndReturn(urban(1.0, 0.9, 0.0)));
            spreadTotal += meanNearestNeighborDistance(placeAndReturn(urban(1.0, 0.0, 0.0)));
        }

        double clustered = clusteredTotal / ARRANGEMENT_RUNS;
        double spread = spreadTotal / ARRANGEMENT_RUNS;

        // A modest effect by nature: even fully spread placement puts cities only a few hexes apart on a sector this
        // size, so clustering has little room to pull them closer. Direction is all that can be asserted.
        assertTrue(clustered < spread,
              "high clustering should place cities closer together than spread placement, but averaged " + clustered
                    + " against " + spread);
    }

    private static StratConTrackState placeAndReturn(UrbanProfile urban) {
        StratConTrackState track = landTrack();
        StratConCityPlacer.placeCities(track, planet(2_000_000_000L), urban);
        return track;
    }

    private static double meanNearestNeighborDistance(StratConTrackState track) {
        var cities = track.getCities().stream().toList();
        if (cities.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        for (StratConCoords city : cities) {
            int nearest = Integer.MAX_VALUE;
            for (StratConCoords other : cities) {
                if (!city.equals(other)) {
                    nearest = Math.min(nearest, city.distance(other));
                }
            }
            sum += nearest;
        }
        return sum / cities.size();
    }
}
