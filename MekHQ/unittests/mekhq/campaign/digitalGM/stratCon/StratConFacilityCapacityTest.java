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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the capacity rules in {@link StratConContractInitializer#initializeTrackFacilities}.
 *
 * <p>Facility counts scale with a contract's combat teams while a sector's area is capped, so a large contract in few
 * sectors - a Single Sector contract above all - can ask for more facilities than its ground will hold. These pin the
 * two rules that keep that from paving the map: only dry land counts, and only half of it may be built on.</p>
 */
class StratConFacilityCapacityTest {

    private static final String OCEAN = "Sea";
    private static final String LAND = "Grasslands";

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    /** @param wetColumns how many of the track's columns are ocean, counting from the left */
    private static StratConTrackState track(int width, int height, int wetColumns) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        track.setDisplayableName("Sector Test");

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                track.setTerrainTile(new StratConCoords(x, y), (x < wetColumns) ? OCEAN : LAND);
            }
        }

        return track;
    }

    private static void requestFacilities(StratConTrackState track, int count) {
        StratConContractInitializer.initializeTrackFacilities(track,
              count,
              ForceAlignment.Opposing,
              false,
              Collections.emptyList());
    }

    @Test
    void aModestRequestIsPlacedInFull() {
        // The common case must be untouched: an ordinary contract asks for far fewer facilities than it has ground.
        StratConTrackState track = track(20, 20, 0);
        requestFacilities(track, 12);

        assertEquals(12, track.getFacilities().size());
    }

    @Test
    void facilitiesNeverCoverMoreThanHalfTheDryLand() {
        // 400 hexes, all dry: asking for one per hex must still leave half of them free for scenarios to spawn on.
        StratConTrackState track = track(20, 20, 0);
        requestFacilities(track, 400);

        assertEquals(200, track.getFacilities().size());
    }

    @Test
    void oceanDoesNotCountTowardCapacity() {
        // Three quarters of this sector is water, so its capacity is half of the remaining 100 dry hexes - not half of
        // its 400 total. Counting the full area was what let a wet sector be asked for four times what it could hold.
        StratConTrackState track = track(20, 20, 15);
        requestFacilities(track, 400);

        assertEquals(50, track.getFacilities().size());
    }

    @Test
    void noFacilityIsEverPlacedOnWater() {
        StratConTrackState track = track(20, 20, 15);
        requestFacilities(track, 400);

        assertFalse(track.getFacilities().isEmpty());
        for (StratConCoords coords : track.getFacilities().keySet()) {
            assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords)),
                  "facility placed on water at " + coords);
        }
    }

    @Test
    void capacityIsSharedAcrossSuccessiveRequests() {
        // Facilities are seeded in several passes - objective allied, objective hostile, then the non-objective ones.
        // The ceiling applies to the sector, not to each pass, or four passes would each fill half of what was left.
        StratConTrackState track = track(20, 20, 0);
        requestFacilities(track, 400);
        requestFacilities(track, 400);

        assertEquals(200, track.getFacilities().size());
    }

    @Test
    void aTinySectorStillAcceptsOneFacility() {
        // Rounding must not floor a small sector's capacity to zero, or its objectives could never be placed.
        StratConTrackState track = track(1, 1, 0);
        requestFacilities(track, 4);

        assertTrue(track.getFacilities().size() >= 1);
    }
}
