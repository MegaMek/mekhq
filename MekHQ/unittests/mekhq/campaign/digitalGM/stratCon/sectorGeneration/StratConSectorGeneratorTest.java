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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConSectorGenerator}: that the improved pipeline produces a fully-terrained sector and reveals
 * exactly its open water.
 */
class StratConSectorGeneratorTest {

    private static StratConTrackState track(int width, int height, int temperatureCelsius) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        track.setTemperature(temperatureCelsius);
        return track;
    }

    private static Set<StratConCoords> oceanHexes(StratConTrackState track) {
        Set<StratConCoords> ocean = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                    ocean.add(coords);
                }
            }
        }
        return ocean;
    }

    @Test
    void generate_leavesNoEmptyHexes() {
        StratConTrackState track = track(20, 20, 25);
        StratConSectorGenerator.generate(track, PlanetProfile.neutral(25), LatitudeBand.EQUATORIAL, true);

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                assertFalse(track.getTerrainTile(new StratConCoords(x, y)).isEmpty(),
                      "hex " + x + "," + y + " was left empty");
            }
        }
    }

    @Test
    void generate_revealsExactlyTheOceanHexes() {
        StratConTrackState track = track(20, 20, 25);
        StratConSectorGenerator.generate(track, PlanetProfile.neutral(25), LatitudeBand.EQUATORIAL, true);

        assertEquals(oceanHexes(track), new HashSet<>(track.getRevealedCoords()));
    }

    @Test
    void generate_onColdWorld_stillCompletesWithoutError() {
        StratConTrackState track = track(16, 16, -60);
        assertDoesNotThrow(() -> StratConSectorGenerator.generate(track,
              PlanetProfile.neutral(-60),
              LatitudeBand.NORTH_POLAR,
              true));

        assertFalse(track.getTerrainTile(new StratConCoords(0, 0)).isEmpty());
    }

    @Test
    void generate_onTinySector_doesNotThrow() {
        StratConTrackState track = track(1, 1, 25);
        assertDoesNotThrow(() -> StratConSectorGenerator.generate(track,
              PlanetProfile.neutral(25),
              LatitudeBand.EQUATORIAL,
              true));
        assertFalse(track.getTerrainTile(new StratConCoords(0, 0)).isEmpty());
    }

    @Test
    void generate_wetWorld_placesSomeOcean() {
        // A very wet planet skews strongly toward high-ocean profiles, so ocean reliably appears. Check across a few
        // generations to stay robust against the rare dry-profile roll.
        PlanetProfile wet = new PlanetProfile(25, PlanetProfile.TERRA_DIAMETER_KM, 80, false, null, "", 1, 1.0, null,
              mekhq.campaign.universe.enums.HPGRating.X);

        boolean anyOcean = false;
        for (int run = 0; run < 3; run++) {
            StratConTrackState track = track(24, 24, 25);
            StratConSectorGenerator.generate(track, wet, LatitudeBand.EQUATORIAL, true);
            if (!oceanHexes(track).isEmpty()) {
                anyOcean = true;
                break;
            }
        }

        assertTrue(anyOcean);
    }
}
