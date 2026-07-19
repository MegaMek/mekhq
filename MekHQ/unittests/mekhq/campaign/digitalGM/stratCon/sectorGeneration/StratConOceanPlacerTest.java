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
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConOceanPlacer}: that each hydrology shape covers roughly its target ocean area and honors its
 * distinguishing topology.
 */
class StratConOceanPlacerTest {
    private static final String OCEAN = "Sea";
    private static final int SIZE = 30;

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        return track;
    }

    private static Set<StratConCoords> hexesOf(StratConTrackState track, boolean ocean) {
        Set<StratConCoords> result = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                boolean isOcean = OCEAN.equals(track.getTerrainTile(coords));
                if (isOcean == ocean) {
                    result.add(coords);
                }
            }
        }
        return result;
    }

    private static int componentCount(StratConTrackState track, Set<StratConCoords> hexes) {
        Set<StratConCoords> unseen = new HashSet<>(hexes);
        int components = 0;
        while (!unseen.isEmpty()) {
            components++;
            Deque<StratConCoords> queue = new ArrayDeque<>();
            StratConCoords start = unseen.iterator().next();
            unseen.remove(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                StratConCoords current = queue.poll();
                for (int direction = 0; direction < 6; direction++) {
                    StratConCoords neighbor = current.translate(direction);
                    if (unseen.remove(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        return components;
    }

    @Test
    void isWaterAdjacent_trueWhenANeighborIsOcean() {
        StratConTrackState track = track();
        StratConCoords center = new StratConCoords(10, 10);
        StratConCoords neighbor = StratConHexGeometry.neighbors(track, center).get(0);
        track.setTerrainTile(neighbor, OCEAN);

        assertTrue(StratConOceanPlacer.isWaterAdjacent(track, center), "a hex beside ocean should be water-adjacent");
    }

    @Test
    void isWaterAdjacent_falseWhenNoNeighborIsOcean() {
        StratConTrackState track = track();
        assertFalse(StratConOceanPlacer.isWaterAdjacent(track, new StratConCoords(10, 10)),
              "a hex with no ocean neighbors should not be water-adjacent");
    }

    @Test
    void everyProfile_coversRoughlyItsTargetOceanArea() {
        int total = SIZE * SIZE;
        int target = (int) Math.round(0.40 * total); // a percentage every band can plausibly reach

        for (HydrologyProfileType type : HydrologyProfileType.values()) {
            StratConTrackState track = track();
            StratConOceanPlacer.placeOceans(track, type, target, OCEAN);

            int ocean = hexesOf(track, true).size();
            assertTrue(ocean > 0, type + " placed no ocean");
            assertTrue(ocean >= (int) (0.5 * target), type + " placed too little ocean: " + ocean);
            assertTrue(ocean <= (int) (1.5 * target), type + " placed too much ocean: " + ocean);
        }
    }

    @Test
    void placeOceans_neverExceedsTheSector() {
        StratConTrackState track = track();
        int total = SIZE * SIZE;

        // Ask for far more ocean than exists; the placer must clamp to the sector.
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.ARCHIPELAGO, total * 2, OCEAN);

        assertTrue(hexesOf(track, true).size() <= total);
    }

    @Test
    void zeroTarget_placesNoOcean() {
        StratConTrackState track = track();
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.INLAND, 0, OCEAN);
        assertEquals(0, hexesOf(track, true).size());
    }

    @Test
    void island_landIsASingleConnectedMass() {
        StratConTrackState track = track();
        int target = (int) Math.round(0.60 * SIZE * SIZE);
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.ISLAND, target, OCEAN);

        Set<StratConCoords> land = hexesOf(track, false);
        assertTrue(land.size() > 0);
        assertEquals(1, componentCount(track, land), "island should carve exactly one landmass");
    }

    @Test
    void inland_waterFormsAtMostTwoIsolatedSpots() {
        StratConTrackState track = track();
        int target = (int) Math.round(0.08 * SIZE * SIZE);
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.INLAND, target, OCEAN);

        Set<StratConCoords> ocean = hexesOf(track, true);
        assertTrue(ocean.size() > 0);
        assertTrue(componentCount(track, ocean) <= 2, "inland water should be one or two isolated spots");
    }

    @Test
    void archipelago_hasSeveralDistinctLandmasses() {
        StratConTrackState track = track();
        int target = (int) Math.round(0.70 * SIZE * SIZE);
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.ARCHIPELAGO, target, OCEAN);

        Set<StratConCoords> land = hexesOf(track, false);
        assertTrue(land.size() > 0);
        // Seeded with 2..6 land blobs; they can occasionally merge, but there should be more than one.
        assertTrue(componentCount(track, land) >= 2, "archipelago should have multiple landmasses");
    }

    @Test
    void lakelands_scattersMultipleWaterBodies() {
        StratConTrackState track = track();
        int target = (int) Math.round(0.22 * SIZE * SIZE);
        StratConOceanPlacer.placeOceans(track, HydrologyProfileType.LAKELANDS, target, OCEAN);

        assertTrue(componentCount(track, hexesOf(track, true)) >= 2, "lakelands should scatter several lakes");
    }
}
