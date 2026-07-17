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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConMountainPlacer}: gravity-driven range counts, volcanic terrain, and ocean avoidance.
 */
class StratConMountainPlacerTest {
    private static final String MOUNTAIN = "Mountain";
    private static final String VOLCANO = "Volcano";
    private static final String OCEAN = "Sea";
    private static final int SIZE = 20;

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        return track;
    }

    private static long count(StratConTrackState track, Predicate<String> predicate) {
        long total = 0;
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                if (predicate.test(track.getTerrainTile(new StratConCoords(x, y)))) {
                    total++;
                }
            }
        }
        return total;
    }

    private static void fillOcean(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                track.setTerrainTile(new StratConCoords(x, y), OCEAN);
            }
        }
    }

    @Test
    void zeroGravity_placesNoMountains() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track, MOUNTAIN, 0.0);

        assertEquals(0, count(track, terrain -> terrain.equals(MOUNTAIN) || terrain.equals(VOLCANO)));
    }

    @Test
    void noMountainTerrain_placesNothing() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track, null, 2.0);

        assertEquals(0, count(track, terrain -> !terrain.isEmpty()));
    }

    @Test
    void highGravity_placesMountainsAcrossRuns() {
        long totalMountainHexes = 0;
        for (int run = 0; run < 40; run++) {
            StratConTrackState track = track();
            StratConMountainPlacer.placeMountains(track, MOUNTAIN, 2.0);
            totalMountainHexes += count(track, terrain -> terrain.equals(MOUNTAIN) || terrain.equals(VOLCANO));
        }
        assertTrue(totalMountainHexes > 0, "high gravity should produce mountains across many runs");
    }

    @Test
    void ranges_neverOverwriteOcean() {
        StratConTrackState track = track();
        fillOcean(track);

        for (int run = 0; run < 10; run++) {
            StratConMountainPlacer.placeMountains(track, MOUNTAIN, 2.0);
        }

        assertEquals(SIZE * SIZE, count(track, OCEAN::equals), "ocean hexes must be untouched");
        assertEquals(0, count(track, terrain -> terrain.equals(MOUNTAIN) || terrain.equals(VOLCANO)));
    }

    @Test
    void onlyMountainOrVolcanicTerrainIsPlaced() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track, MOUNTAIN, 2.0);

        long stray = count(track,
              terrain -> !terrain.isEmpty() && !terrain.equals(MOUNTAIN) && !terrain.equals(VOLCANO));
        assertEquals(0, stray, "only mountain or volcanic terrain should be placed");
    }
}
