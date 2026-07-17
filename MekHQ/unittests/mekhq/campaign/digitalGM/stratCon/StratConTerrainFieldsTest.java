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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConTerrainFields}: the moisture, relief-distance, rain-shadow, and coldness fields.
 */
class StratConTerrainFieldsTest {
    private static final int SIZE = 20;

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        return track;
    }

    @Test
    void moisture_isHighNearWaterAndFallsOffInland() {
        StratConTrackState track = track();
        for (int y = 0; y < SIZE; y++) {
            track.setTerrainTile(new StratConCoords(0, y), "Sea");
        }
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);

        double nearWater = fields.moistureAt(new StratConCoords(1, 10));
        double inland = fields.moistureAt(new StratConCoords(SIZE - 1, 10));

        assertTrue(nearWater > inland, "moisture should be higher near water than inland");
        assertTrue(nearWater > 0.0);
    }

    @Test
    void moisture_isZeroWhenSectorHasNoWater() {
        StratConTerrainFields fields = StratConTerrainFields.compute(track(), LatitudeBand.EQUATORIAL, 0);
        assertEquals(0.0, fields.moistureAt(new StratConCoords(5, 5)));
    }

    @Test
    void reliefDistance_isZeroAtMountainAndGrowsOutward() {
        StratConTrackState track = track();
        StratConCoords mountain = new StratConCoords(10, 10);
        track.setTerrainTile(mountain, "Mountain");
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, 0);

        assertEquals(0, fields.reliefDistanceAt(mountain));
        assertEquals(1, fields.reliefDistanceAt(mountain.translate(0)));
        assertTrue(fields.reliefDistanceAt(new StratConCoords(0, 0)) > 1);
    }

    @Test
    void reliefDistance_isLargeWithoutAnyRelief() {
        StratConTerrainFields fields = StratConTerrainFields.compute(track(), LatitudeBand.EQUATORIAL, 0);
        assertTrue(fields.reliefDistanceAt(new StratConCoords(5, 5)) > SIZE * SIZE);
    }

    @Test
    void rainShadow_marksHexesLeewardOfRelief() {
        StratConTrackState track = track();
        StratConCoords mountain = new StratConCoords(10, 10);
        track.setTerrainTile(mountain, "Mountain");

        int windDirection = 1;
        StratConTerrainFields fields = StratConTerrainFields.compute(track, LatitudeBand.EQUATORIAL, windDirection);

        int upwind = (windDirection + 3) % 6;
        StratConCoords leeward = mountain.translate(windDirection); // downwind of the mountain
        StratConCoords windward = mountain.translate(upwind);       // upwind of the mountain

        assertTrue(fields.inRainShadow(leeward), "the downwind side of relief should be in rain shadow");
        assertFalse(fields.inRainShadow(windward), "the upwind side of relief should not be in rain shadow");
    }

    @Test
    void coldness_risesTowardTheNorthEdgeForNorthernBands() {
        StratConTerrainFields fields = StratConTerrainFields.compute(track(), LatitudeBand.NORTH_POLAR, 0);
        assertTrue(fields.coldnessAt(new StratConCoords(5, 0)) > fields.coldnessAt(new StratConCoords(5, SIZE - 1)));
    }

    @Test
    void coldness_risesTowardTheSouthEdgeForSouthernBands() {
        StratConTerrainFields fields = StratConTerrainFields.compute(track(), LatitudeBand.SOUTH_POLAR, 0);
        assertTrue(fields.coldnessAt(new StratConCoords(5, SIZE - 1)) > fields.coldnessAt(new StratConCoords(5, 0)));
    }

    @Test
    void coldness_isFlatAtTheEquator() {
        StratConTerrainFields fields = StratConTerrainFields.compute(track(), LatitudeBand.EQUATORIAL, 0);
        assertEquals(fields.coldnessAt(new StratConCoords(5, 0)), fields.coldnessAt(new StratConCoords(5, SIZE - 1)));
    }
}
