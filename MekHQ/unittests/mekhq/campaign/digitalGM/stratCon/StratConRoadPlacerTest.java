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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConRoadPlacer}: connecting cities, avoiding ocean and (where cheaper) mountains, keeping
 * landmasses independent, and branching off the map.
 */
class StratConRoadPlacerTest {

    private static StratConTrackState landTrack(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                track.setTerrainTile(new StratConCoords(x, y), "Plains");
            }
        }
        return track;
    }

    private static Set<StratConCoords> roadComponent(StratConTrackState track, StratConCoords start) {
        Set<StratConCoords> component = new HashSet<>();
        if (!track.getRoads().contains(start)) {
            return component;
        }
        Deque<StratConCoords> queue = new ArrayDeque<>();
        queue.add(start);
        component.add(start);
        while (!queue.isEmpty()) {
            StratConCoords current = queue.poll();
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, current)) {
                if (track.getRoads().contains(neighbor) && component.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return component;
    }

    @Test
    void noCities_producesNoRoads() {
        StratConTrackState track = landTrack(8, 8);
        StratConRoadPlacer.recalculateRoads(track);
        assertTrue(track.getRoads().isEmpty());
        assertTrue(track.getRoadExits().isEmpty());
    }

    @Test
    void connectsAllCitiesOnOneLandmass() {
        StratConTrackState track = landTrack(12, 12);
        StratConCoords a = new StratConCoords(1, 1);
        StratConCoords b = new StratConCoords(9, 9);
        StratConCoords c = new StratConCoords(4, 8);
        track.addCity(a);
        track.addCity(b);
        track.addCity(c);

        StratConRoadPlacer.recalculateRoads(track);

        Set<StratConCoords> component = roadComponent(track, a);
        assertTrue(component.contains(a));
        assertTrue(component.contains(b), "second city not reachable by road");
        assertTrue(component.contains(c), "third city not reachable by road");
    }

    @Test
    void roadsNeverCrossOcean() {
        StratConTrackState track = landTrack(9, 5);
        // an ocean strait down the middle splits the map
        for (int y = 0; y < 5; y++) {
            track.setTerrainTile(new StratConCoords(4, y), "Sea");
        }
        track.addCity(new StratConCoords(1, 2));
        track.addCity(new StratConCoords(7, 2));

        StratConRoadPlacer.recalculateRoads(track);

        for (StratConCoords road : track.getRoads()) {
            assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(road)),
                  "a road crossed ocean at " + road);
        }
    }

    @Test
    void citiesOnSeparateLandmassesFormSeparateNetworks() {
        StratConTrackState track = landTrack(9, 5);
        for (int y = 0; y < 5; y++) {
            track.setTerrainTile(new StratConCoords(4, y), "Sea");
        }
        StratConCoords west = new StratConCoords(1, 2);
        StratConCoords east = new StratConCoords(7, 2);
        track.addCity(west);
        track.addCity(east);

        StratConRoadPlacer.recalculateRoads(track);

        assertFalse(roadComponent(track, west).contains(east), "cities across an ocean should not be road-connected");
    }

    @Test
    void networkBranchesOffTheMap() {
        StratConTrackState track = landTrack(12, 12);
        track.addCity(new StratConCoords(5, 5));
        track.addCity(new StratConCoords(6, 6));

        StratConRoadPlacer.recalculateRoads(track);

        assertFalse(track.getRoadExits().isEmpty(), "a land-connected network should branch off the map");
        for (StratConCoords exit : track.getRoadExits()) {
            boolean onBorder = (exit.getX() == 0) ||
                                     (exit.getX() == (track.getWidth() - 1)) ||
                                     (exit.getY() == 0) ||
                                     (exit.getY() == (track.getHeight() - 1));
            assertTrue(onBorder, "road exit " + exit + " is not on the border");
            assertTrue(track.getRoads().contains(exit), "road exit " + exit + " is not itself a road");
        }
    }

    @Test
    void routesAroundMountainsWhenADetourIsCheaper() {
        // Cities two hexes apart with a lone mountain directly between them; the short detour over plains is cheaper
        // than crossing the (cost-5) mountain, so the road should avoid it.
        StratConTrackState track = landTrack(4, 3);
        StratConCoords a = new StratConCoords(0, 0);
        StratConCoords b = new StratConCoords(2, 0);
        track.setTerrainTile(new StratConCoords(1, 0), "Mountain");
        track.addCity(a);
        track.addCity(b);

        StratConRoadPlacer.recalculateRoads(track);

        assertTrue(roadComponent(track, a).contains(b), "cities should still be connected");
        assertFalse(track.getRoads().contains(new StratConCoords(1, 0)), "road should detour around the mountain");
    }
}
