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
package mekhq.campaign.digitalGM.stratCon.generation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Shared hex-grid helpers used by the improved sector generation placers: bounds checks, neighbor lookup, and growing
 * an organic connected region. Keeping these in one place avoids duplicating the geometry (and the
 * {@link StratConCoords#equals} class-sensitivity pitfall) across the ocean and mountain placers.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConHexGeometry {
    private StratConHexGeometry() {}

    /** The number of directions out of a hex. */
    public static final int HEX_DIRECTIONS = 6;

    /**
     * @return {@code true} if the coordinates fall within the track's bounds
     */
    public static boolean inBounds(StratConTrackState track, Coords coords) {
        return (coords.getX() >= 0) &&
                     (coords.getX() < track.getWidth()) &&
                     (coords.getY() >= 0) &&
                     (coords.getY() < track.getHeight());
    }

    /**
     * @return the in-bounds hex neighbors of the given coordinates
     */
    public static List<StratConCoords> neighbors(StratConTrackState track, StratConCoords coords) {
        List<StratConCoords> result = new ArrayList<>();
        for (int direction = 0; direction < HEX_DIRECTIONS; direction++) {
            StratConCoords neighbor = coords.translate(direction);
            if (inBounds(track, neighbor)) {
                result.add(neighbor);
            }
        }
        return result;
    }

    /**
     * Grows a connected region from {@code seed} up to {@code size} hexes, never entering a {@code blocked} hex. The
     * region expands from a randomly chosen frontier hex each step, producing an organic (non-circular) blob.
     *
     * @param track   the track providing bounds
     * @param seed    the starting hex
     * @param size    the desired region size in hexes
     * @param blocked hexes the region may not enter
     *
     * @return the grown region (possibly smaller than {@code size} if it runs out of room)
     */
    public static Set<StratConCoords> growBlob(StratConTrackState track, StratConCoords seed, int size,
          Set<StratConCoords> blocked) {
        Set<StratConCoords> region = new HashSet<>();
        if ((size <= 0) || !inBounds(track, seed) || blocked.contains(seed)) {
            return region;
        }

        List<StratConCoords> frontier = new ArrayList<>();
        region.add(seed);
        frontier.add(seed);

        while ((region.size() < size) && !frontier.isEmpty()) {
            StratConCoords current = frontier.get(Compute.randomInt(frontier.size()));

            List<StratConCoords> candidates = new ArrayList<>();
            for (StratConCoords neighbor : neighbors(track, current)) {
                if (!region.contains(neighbor) && !blocked.contains(neighbor)) {
                    candidates.add(neighbor);
                }
            }

            if (candidates.isEmpty()) {
                frontier.remove(current);
                continue;
            }

            StratConCoords next = candidates.get(Compute.randomInt(candidates.size()));
            region.add(next);
            frontier.add(next);
        }
        return region;
    }
}
