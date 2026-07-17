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

import static java.lang.Math.max;

import jakarta.annotation.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;

/**
 * Places mountain ranges onto a StratCon track. The number of ranges scales with the planet's gravity (a more rugged
 * world has more relief), each range is drawn as a strip, and ranges never overwrite ocean. A small fraction of ranges
 * are drawn as volcanic terrain instead of ordinary mountains.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConMountainPlacer {
    private StratConMountainPlacer() {}

    private static final int HEX_DIRECTIONS = 6;

    /** The maximum number of mountain ranges, reached at high gravity. */
    private static final int MAX_RANGES = 4;

    /** Percent chance that a given range is drawn as volcanic terrain rather than ordinary mountains. */
    private static final int VOLCANISM_CHANCE = 10;

    /** The volcanic terrain type used for a volcanic range. */
    private static final String VOLCANO_TERRAIN = "Volcano";

    /**
     * Places gravity-driven mountain ranges on the track.
     *
     * @param track           the track to paint
     * @param mountainTerrain the biome's mountain terrain type, or {@code null} when the biome offers no mountains (in
     *                        which case no ranges are placed)
     * @param gravity         the planet's surface gravity in G; higher gravity means more ranges
     */
    public static void placeMountains(StratConTrackState track, @Nullable String mountainTerrain, double gravity) {
        if (mountainTerrain == null) {
            return;
        }

        int maxRanges = Math.clamp(Math.round(gravity * 2), 0, MAX_RANGES);
        int rangeCount = Compute.randomInt(maxRanges + 1);

        for (int range = 0; range < rangeCount; range++) {
            boolean volcanic = Compute.randomInt(100) < VOLCANISM_CHANCE;
            drawRange(track, volcanic ? VOLCANO_TERRAIN : mountainTerrain);
        }
    }

    /**
     * Draws a single range as a strip from a random start hex, running a random length in a random hex direction. Ocean
     * hexes on the strip are skipped, so a range never crosses open water.
     */
    private static void drawRange(StratConTrackState track, String terrain) {
        int width = track.getWidth();
        int height = track.getHeight();

        StratConCoords start = new StratConCoords(Compute.randomInt(width), Compute.randomInt(height));
        int direction = Compute.randomInt(HEX_DIRECTIONS);
        int length = 2 + Compute.randomInt(max(1, max(width, height) - 1));

        StratConCoords end = start;
        for (int step = 0; step < length; step++) {
            StratConCoords next = end.translate(direction);
            if (!inBounds(track, next)) {
                break;
            }
            end = next;
        }

        // intervening() walks the line with plain Coords and stops on dest.equals(current). StratConCoords.equals is
        // class-sensitive, so the endpoints passed here must be plain Coords, or the walk never terminates.
        Coords lineStart = new Coords(start.getX(), start.getY());
        Coords lineEnd = new Coords(end.getX(), end.getY());
        for (Coords coords : Coords.intervening(lineStart, lineEnd)) {
            StratConCoords hex = new StratConCoords(coords.getX(), coords.getY());
            if (!inBounds(track, hex)) {
                continue;
            }
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(hex))) {
                continue;
            }
            track.setTerrainTile(hex, terrain);
        }
    }

    private static boolean inBounds(StratConTrackState track, Coords coords) {
        return (coords.getX() >= 0) &&
                     (coords.getX() < track.getWidth()) &&
                     (coords.getY() >= 0) &&
                     (coords.getY() < track.getHeight());
    }
}
