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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.universe.Atmosphere;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConTerrainFiller}: candidate gating (tainted/toxic, airless), fill invariants, and the coastal
 * moisture effect.
 */
class StratConTerrainFillerTest {
    private static final int SIZE = 30;

    private static StratConBiome biome(String... terrains) {
        StratConBiome biome = new StratConBiome();
        biome.biomeCategory = StratConBiomeManifest.TERRAN_BIOME;
        biome.allowedTemperatureLowerBound = Integer.MIN_VALUE;
        biome.allowedTemperatureUpperBound = Integer.MAX_VALUE;
        biome.allowedTerrainTypes = new ArrayList<>(List.of(terrains));
        return biome;
    }

    private static PlanetProfile planet(int water, boolean airless, Atmosphere atmosphere) {
        return new PlanetProfile(20, PlanetProfile.TERRA_DIAMETER_KM, water, airless, atmosphere, "", 1, 1.0, null);
    }

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        return track;
    }

    private static long countVegetation(StratConTrackState track, int minX, int maxX) {
        long count = 0;
        for (int x = minX; x < maxX; x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                if (StratConBiomeManifest.isVegetationTerrain(track.getTerrainTile(new StratConCoords(x, y)))) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---- Candidate gating ----

    @Test
    void candidates_excludeWaterMountainAndUrban() {
        List<String> candidates = StratConTerrainFiller.candidateTerrains(
              biome("Forest", "Badlands", "Sea", "Mountain", "Urban"),
              planet(50, false, null));

        assertTrue(candidates.contains("Forest"));
        assertTrue(candidates.contains("Badlands"));
        assertFalse(candidates.contains("Sea"));
        assertFalse(candidates.contains("Mountain"));
        assertFalse(candidates.contains("Urban"));
    }

    @Test
    void candidates_taintedWorld_dropVegetation() {
        List<String> candidates = StratConTerrainFiller.candidateTerrains(
              biome("Forest", "Jungle", "Badlands", "Steppe"),
              planet(50, false, Atmosphere.TAINTED_POISON));

        assertFalse(candidates.contains("Forest"));
        assertFalse(candidates.contains("Jungle"));
        assertTrue(candidates.contains("Badlands"));
        assertTrue(candidates.contains("Steppe"));
    }

    @Test
    void candidates_airlessWorld_useLunarVolcanicSet() {
        List<String> candidates = StratConTerrainFiller.candidateTerrains(
              biome("Forest", "Badlands"),
              planet(50, true, null));

        candidates.forEach(terrain -> assertTrue(
              StratConBiomeManifest.isLunarTerrain(terrain) || StratConBiomeManifest.isVolcanicTerrain(terrain),
              terrain + " is not lunar/volcanic"));
        assertTrue(candidates.contains("Volcano"));
    }

    // ---- Fill invariants ----

    @Test
    void fill_leavesNoEmptyHexes() {
        StratConTrackState track = track();
        StratConBiome biome = biome("Forest", "Plains", "Badlands", "Steppe");
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);

        StratConTerrainFiller.fill(track, biome, planet(50, false, null), fields);

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                assertFalse(track.getTerrainTile(new StratConCoords(x, y)).isEmpty());
            }
        }
    }

    @Test
    void fill_doesNotOverwriteOceanOrMountains() {
        StratConTrackState track = track();
        StratConCoords ocean = new StratConCoords(0, 0);
        StratConCoords mountain = new StratConCoords(5, 5);
        track.setTerrainTile(ocean, "Sea");
        track.setTerrainTile(mountain, "Mountain");

        StratConBiome biome = biome("Forest", "Plains", "Badlands");
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);
        StratConTerrainFiller.fill(track, biome, planet(50, false, null), fields);

        assertTrue(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(ocean)));
        assertTrue(StratConBiomeManifest.isMountainTerrain(track.getTerrainTile(mountain)));
    }

    @Test
    void fill_taintedWorld_placesNoVegetation() {
        StratConTrackState track = track();
        StratConBiome biome = biome("Forest", "Jungle", "Badlands", "Steppe");
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);

        StratConTerrainFiller.fill(track, biome, planet(50, false, Atmosphere.TOXIC_POISON), fields);

        assertTrue(countVegetation(track, 0, SIZE) == 0, "a toxic world must have no vegetation");
    }

    @Test
    void fill_airlessWorld_onlyLunarOrVolcanic() {
        StratConTrackState track = track();
        StratConBiome biome = biome("Forest", "Plains", "Badlands");
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);

        StratConTerrainFiller.fill(track, biome, planet(20, true, null), fields);

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                String terrain = track.getTerrainTile(new StratConCoords(x, y));
                assertTrue(StratConBiomeManifest.isLunarTerrain(terrain) ||
                                 StratConBiomeManifest.isVolcanicTerrain(terrain),
                      "airless hex " + x + "," + y + " was " + terrain);
            }
        }
    }

    // ---- Geography ----

    @Test
    void coastalMoisture_vegetationClustersNearWater() {
        StratConBiome biome = biome("Forest", "Plains", "Badlands", "Steppe");
        PlanetProfile breathable = planet(50, false, Atmosphere.BREATHABLE);

        long nearWater = 0;
        long inland = 0;
        for (int run = 0; run < 6; run++) {
            StratConTrackState track = track();
            // A wall of water down the west edge.
            for (int y = 0; y < SIZE; y++) {
                track.setTerrainTile(new StratConCoords(0, y), "Sea");
            }
            StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);
            StratConTerrainFiller.fill(track, biome, breathable, fields);

            nearWater += countVegetation(track, 1, SIZE / 2);
            inland += countVegetation(track, SIZE / 2, SIZE);
        }

        assertTrue(nearWater > inland, "vegetation should cluster nearer the coast than inland");
    }

    @Test
    void piedmont_concentratesHillsAroundMountains() {
        StratConBiome biome = biome("Hills", "Forest", "Plains", "Badlands");
        PlanetProfile breathable = planet(50, false, Atmosphere.BREATHABLE);
        StratConCoords mountain = new StratConCoords(15, 15);

        long adjacentHills = 0;
        long adjacentTotal = 0;
        long farHills = 0;
        long farTotal = 0;
        for (int run = 0; run < 8; run++) {
            StratConTrackState track = track();
            track.setTerrainTile(mountain, "Mountain");
            StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);
            StratConTerrainFiller.fill(track, biome, breathable, fields);

            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, mountain)) {
                adjacentHills += StratConBiomeManifest.isHillsTerrain(track.getTerrainTile(neighbor)) ? 1 : 0;
                adjacentTotal++;
            }
            for (StratConCoords far : List.of(new StratConCoords(0, 0), new StratConCoords(SIZE - 1, SIZE - 1))) {
                farHills += StratConBiomeManifest.isHillsTerrain(track.getTerrainTile(far)) ? 1 : 0;
                farTotal++;
            }
        }

        assertTrue(((double) adjacentHills / adjacentTotal) > ((double) farHills / farTotal),
              "hills should be denser next to mountains than far from them");
    }

    @Test
    void riparian_makesWaterAdjacentHexesMostlyVegetation() {
        StratConBiome biome = biome("Forest", "Plains", "Badlands", "Steppe");
        PlanetProfile breathable = planet(50, false, Atmosphere.BREATHABLE);

        long bankVegetation = 0;
        long bankTotal = 0;
        for (int run = 0; run < 6; run++) {
            StratConTrackState track = track();
            for (int y = 0; y < SIZE; y++) {
                track.setTerrainTile(new StratConCoords(0, y), "Sea");
            }
            StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);
            StratConTerrainFiller.fill(track, biome, breathable, fields);

            for (int y = 0; y < SIZE; y++) {
                bankVegetation += StratConBiomeManifest.isVegetationTerrain(track.getTerrainTile(new StratConCoords(1,
                      y))) ? 1 : 0;
                bankTotal++;
            }
        }

        assertTrue(((double) bankVegetation / bankTotal) > 0.5, "the immediate riverbank should be mostly vegetation");
    }
}
