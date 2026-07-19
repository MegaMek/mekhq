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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConFarmPlacer}: farmland grows around cities, only over arable land, never past a city's reach,
 * and never on the city hex itself.
 */
class StratConFarmPlacerTest {
    private static final int SIZE = 20;

    /** A fertile, well-settled, high-tech world, so the farming scale is high and farms reliably appear. */
    private static PlanetProfile fertilePlanet() {
        return new PlanetProfile(20, PlanetProfile.TERRA_DIAMETER_KM, 50, false, null, "", 1, 1.0, 2_000_000_000L,
              HPGRating.A);
    }

    private static UrbanProfile farmProfile(int reach, double density) {
        return new UrbanProfile(UrbanProfileType.DISPERSED, null, null, null, null, null, null, null, reach, density);
    }

    private static StratConTrackState terrainTrack(String terrain) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                track.setTerrainTile(new StratConCoords(x, y), terrain);
            }
        }
        return track;
    }

    private static Set<StratConCoords> farmland(StratConTrackState track) {
        Set<StratConCoords> farms = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isAgricultureTerrain(track.getTerrainTile(coords))) {
                    farms.add(coords);
                }
            }
        }
        return farms;
    }

    @Test
    void placesFarmlandAroundCities() {
        StratConTrackState track = terrainTrack("Plains");
        track.addCity(new StratConCoords(10, 10));

        StratConFarmPlacer.placeFarms(track, fertilePlanet(), farmProfile(3, 1.0));

        assertFalse(farmland(track).isEmpty(), "a city on arable land should grow farmland");
    }

    @Test
    void noCities_producesNoFarmland() {
        StratConTrackState track = terrainTrack("Plains");
        StratConFarmPlacer.placeFarms(track, fertilePlanet(), farmProfile(3, 1.0));
        assertTrue(farmland(track).isEmpty(), "with no cities there is nothing to farm around");
    }

    @Test
    void farmsOnlyOnArableLand() {
        // A world of forest (not arable) with a single arable Plains hex next to the city.
        StratConTrackState track = terrainTrack("Forest");
        StratConCoords city = new StratConCoords(10, 10);
        track.addCity(city);
        Set<StratConCoords> arable = new HashSet<>();
        for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, city)) {
            track.setTerrainTile(neighbor, "Plains");
            arable.add(neighbor);
        }

        StratConFarmPlacer.placeFarms(track, fertilePlanet(), farmProfile(3, 1.0));

        for (StratConCoords farm : farmland(track)) {
            assertTrue(arable.contains(farm), "farmland appeared on non-arable terrain at " + farm);
        }
    }

    @Test
    void farmlandStaysWithinReach() {
        StratConTrackState track = terrainTrack("Plains");
        StratConCoords city = new StratConCoords(10, 10);
        track.addCity(city);
        int reach = 2;

        StratConFarmPlacer.placeFarms(track, fertilePlanet(), farmProfile(reach, 1.0));

        for (StratConCoords farm : farmland(track)) {
            assertTrue(city.distance(farm) <= reach, "farmland at " + farm + " is beyond the city's reach");
        }
    }

    @Test
    void doesNotFarmTheCityHexItself() {
        StratConTrackState track = terrainTrack("Plains");
        StratConCoords city = new StratConCoords(10, 10);
        track.addCity(city);

        StratConFarmPlacer.placeFarms(track, fertilePlanet(), farmProfile(3, 1.0));

        assertFalse(StratConBiomeManifest.isAgricultureTerrain(track.getTerrainTile(city)),
              "the city hex itself should not be converted to farmland");
    }
}
