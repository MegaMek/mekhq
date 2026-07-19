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
import java.util.List;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConRoadPlacer}: connecting cities, bridging a single ocean hex (but never two in a row),
 * avoiding mountains where cheaper, keeping widely-separated landmasses independent, and branching off the map.
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
    void roadsBridgeASingleOceanHexButNeverTwoInARow() {
        StratConTrackState track = landTrack(9, 5);
        // a one-hex-wide ocean strait down the middle - bridgeable
        for (int y = 0; y < 5; y++) {
            track.setTerrainTile(new StratConCoords(4, y), "Sea");
        }
        StratConCoords west = new StratConCoords(1, 2);
        StratConCoords east = new StratConCoords(7, 2);
        track.addCity(west);
        track.addCity(east);

        StratConRoadPlacer.recalculateRoads(track);

        // the strait is only one hex wide, so a bridge joins the two shores into one network
        assertTrue(roadComponent(track, west).contains(east), "cities across a one-hex strait should be bridged");

        // ...but a bridge is never more than a single hex: no two adjacent road hexes are both ocean
        for (StratConCoords road : track.getRoads()) {
            if (!StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(road))) {
                continue;
            }
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, road)) {
                if (track.getRoads().contains(neighbor)) {
                    assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(neighbor)),
                          "road bridged two ocean hexes in a row at " + road + " and " + neighbor);
                }
            }
        }
    }

    @Test
    void citiesAcrossWideWaterFormSeparateNetworks() {
        StratConTrackState track = landTrack(10, 5);
        // a two-hex-wide ocean strait cannot be bridged
        for (int y = 0; y < 5; y++) {
            track.setTerrainTile(new StratConCoords(4, y), "Sea");
            track.setTerrainTile(new StratConCoords(5, y), "Sea");
        }
        StratConCoords west = new StratConCoords(1, 2);
        StratConCoords east = new StratConCoords(8, 2);
        track.addCity(west);
        track.addCity(east);

        StratConRoadPlacer.recalculateRoads(track);

        assertFalse(roadComponent(track, west).contains(east), "cities across wide water should not be road-connected");
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
    void roadEntryEdges_emptyWhenHexHasNoRoad() {
        StratConTrackState track = landTrack(9, 5);
        assertTrue(StratConRoadPlacer.roadEntryEdges(track, new StratConCoords(4, 2)).isEmpty(),
              "a hex with no road should carry no road edges");
    }

    @Test
    void roadEntryEdges_pointToAdjacentRoadHexes() {
        StratConTrackState track = landTrack(9, 5);
        StratConCoords a = new StratConCoords(4, 2);

        // pick any in-bounds neighbor of a, and note the direction to it
        StratConCoords b = null;
        int directionToB = -1;
        for (int direction = 0; direction < StratConHexGeometry.HEX_DIRECTIONS; direction++) {
            StratConCoords neighbor = a.translate(direction);
            if (StratConHexGeometry.inBounds(track, neighbor)) {
                b = neighbor;
                directionToB = direction;
                break;
            }
        }

        track.setRoads(new HashSet<>(List.of(a, b)));

        assertTrue(StratConRoadPlacer.roadEntryEdges(track, a).contains(directionToB),
              "the road edge toward an adjacent road hex should be reported");
    }

    @Test
    void roadEntryEdges_includeOffMapDirectionForExit() {
        StratConTrackState track = landTrack(9, 5);
        StratConCoords edge = new StratConCoords(0, 2); // on the west border
        track.setRoads(new HashSet<>(List.of(edge)));
        track.setRoadExits(new HashSet<>(List.of(edge)));

        List<Integer> edges = StratConRoadPlacer.roadEntryEdges(track, edge);

        boolean leavesMap = edges.stream()
                                  .anyMatch(direction -> !StratConHexGeometry.inBounds(track,
                                        edge.translate(direction)));
        assertTrue(leavesMap, "a road exit hex should carry a road off the sector edge");
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

    @Test
    void farmlandBlockGetsALaneFromTheNetwork() {
        // Two cities on the west side, and a block of farmland off to the east that no city road would otherwise reach.
        StratConTrackState track = landTrack(12, 6);
        StratConCoords a = new StratConCoords(0, 1);
        StratConCoords b = new StratConCoords(0, 4);
        track.addCity(a);
        track.addCity(b);

        Set<StratConCoords> farmland = Set.of(new StratConCoords(9, 2),
              new StratConCoords(9, 3),
              new StratConCoords(10, 2));
        for (StratConCoords farm : farmland) {
            track.setTerrainTile(farm, StratConBiomeManifest.FARMLAND);
        }

        StratConRoadPlacer.recalculateRoads(track);

        Set<StratConCoords> network = roadComponent(track, a);
        assertTrue(network.contains(b), "cities should be connected");
        assertTrue(farmland.stream().anyMatch(network::contains),
              "the farmland block should be reachable by road from the cities");
    }

    @Test
    void farmlandAlreadyOnTheTrunkGetsNoExtraLane() {
        // A single farm hex sitting directly between two cities is already served by the trunk road, so it should not
        // trigger a spur of its own.
        StratConTrackState track = landTrack(6, 3);
        track.addCity(new StratConCoords(0, 1));
        track.addCity(new StratConCoords(4, 1));
        StratConRoadPlacer.recalculateRoads(track);
        int trunkSize = track.getRoads().size();

        StratConTrackState farmed = landTrack(6, 3);
        farmed.addCity(new StratConCoords(0, 1));
        farmed.addCity(new StratConCoords(4, 1));
        for (StratConCoords road : track.getRoads()) {
            farmed.setTerrainTile(road, StratConBiomeManifest.FARMLAND);
        }

        StratConRoadPlacer.recalculateRoads(farmed);

        assertEquals(trunkSize, farmed.getRoads().size(), "farmland already on the trunk should add no new road hexes");
    }

    @Test
    void spacePortDrawsTheTrunkTowardItself() {
        // Cities at the four corners with a facility in the middle. A space port is a road hub, so the cities should
        // route through the middle to reach each other; an ordinary base is just another endpoint, leaving the cities
        // to link around the perimeter and the base hanging off a spur.
        int hubRoads = roadsNearFacility(FacilityType.SpacePort);
        int plainRoads = roadsNearFacility(FacilityType.MekBase);

        assertTrue(hubRoads > plainRoads,
              "a space port should pull more road through its surroundings than a non-hub facility (hub=" +
                    hubRoads + ", plain=" + plainRoads + ')');
    }

    /**
     * Lays roads between four corner cities with a facility of the given type in the middle, and counts how many road
     * hexes end up close to that facility.
     */
    private static int roadsNearFacility(FacilityType facilityType) {
        StratConTrackState track = landTrack(16, 12);
        track.addCity(new StratConCoords(1, 1));
        track.addCity(new StratConCoords(14, 1));
        track.addCity(new StratConCoords(1, 10));
        track.addCity(new StratConCoords(14, 10));

        StratConCoords facilityCoords = new StratConCoords(8, 6);
        StratConFacility facility = new StratConFacility();
        facility.setFacilityType(facilityType);
        track.addFacility(facilityCoords, facility);

        StratConRoadPlacer.recalculateRoads(track, List.of(facilityCoords));

        // Measured with the map's own adjacency, not StratConCoords.distance(), which is in a different coordinate
        // convention (see StratConHexGeometry.withinRadius).
        Set<StratConCoords> nearFacility = StratConHexGeometry.withinRadius(track, facilityCoords, 2);
        return (int) track.getRoads().stream().filter(nearFacility::contains).count();
    }
}
