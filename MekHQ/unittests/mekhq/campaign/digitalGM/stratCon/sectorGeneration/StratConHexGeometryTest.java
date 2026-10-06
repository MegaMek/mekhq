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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConHexGeometry#withinRadius}, the "hexes near a hex" helper used by the GM terrain brush.
 */
class StratConHexGeometryTest {

    private static StratConTrackState track(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        return track;
    }

    @Test
    void radiusZero_isJustTheCenterHex() {
        StratConTrackState track = track(12, 12);
        StratConCoords center = new StratConCoords(5, 5);

        assertEquals(Set.of(center), StratConHexGeometry.withinRadius(track, center, 0));
    }

    @Test
    void radiusOne_isExactlyTheCenterAndItsSixNeighbors() {
        // The bug this guards: the brush originally used the inherited Coords.distance(), which measures in MegaMek's
        // coordinate convention rather than the parity-offset one StratCon stores. That painted two hexes a row too
        // high in the neighboring columns and skipped the two below, instead of a ring around the clicked hex.
        StratConTrackState track = track(12, 12);

        for (StratConCoords center : Set.of(new StratConCoords(4, 5), new StratConCoords(5, 5))) {
            Set<StratConCoords> expected = new HashSet<>(StratConHexGeometry.neighbors(track, center));
            expected.add(center);

            assertEquals(expected,
                  StratConHexGeometry.withinRadius(track, center, 1),
                  "radius 1 around " + center + " should be the hex and its own neighbors");
            assertEquals(7, expected.size(), "an inland hex has six neighbors");
        }
    }

    @Test
    void everyHexInRadiusOne_isTrulyAdjacentToTheCenter() {
        StratConTrackState track = track(12, 12);
        StratConCoords center = new StratConCoords(4, 5);
        Set<StratConCoords> neighbors = new HashSet<>(StratConHexGeometry.neighbors(track, center));

        for (StratConCoords coords : StratConHexGeometry.withinRadius(track, center, 1)) {
            assertTrue(coords.equals(center) || neighbors.contains(coords),
                  coords + " is in the radius but does not border the center hex");
        }
    }

    @Test
    void radiusTwo_coversNineteenHexesInOpenGround() {
        StratConTrackState track = track(12, 12);

        assertEquals(19, StratConHexGeometry.withinRadius(track, new StratConCoords(5, 5), 2).size());
    }

    @Test
    void radiusIsClippedAtTheSectorEdge() {
        StratConTrackState track = track(12, 12);

        Set<StratConCoords> corner = StratConHexGeometry.withinRadius(track, new StratConCoords(0, 0), 1);

        assertTrue(corner.size() < 7, "a corner hex has fewer neighbors than an inland one");
        for (StratConCoords coords : corner) {
            assertTrue(StratConHexGeometry.inBounds(track, coords), coords + " should be inside the sector");
        }
    }
}
