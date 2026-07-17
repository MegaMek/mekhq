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
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;

/**
 * Paints ocean onto a StratCon track in a shape determined by its {@link HydrologyProfileType}. Every profile produces
 * a visually distinct water layout, and none produce a straight channel that bisects the sector.
 *
 * <p>Water-dominant profiles ({@code ISLAND}, {@code ARCHIPELAGO}, {@code PENINSULA}) are built by growing the
 * <em>land</em> masses and flooding the remainder, so the land reads as a coherent shape surrounded by water. The
 * others grow water regions directly.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConOceanPlacer {
    private StratConOceanPlacer() {}

    private static final int HEX_DIRECTIONS = 6;

    /**
     * Paints roughly {@code oceanTargetHexes} hexes of the given ocean terrain onto the track, in the shape dictated by
     * the profile type.
     *
     * @param track            the track to paint
     * @param type             the hydrology profile whose shape to use
     * @param oceanTargetHexes the desired number of ocean hexes
     * @param oceanTerrain     the ocean terrain type to paint (chosen from the sector's biome)
     */
    public static void placeOceans(StratConTrackState track, HydrologyProfileType type, int oceanTargetHexes,
          String oceanTerrain) {
        int total = track.getWidth() * track.getHeight();
        int oceanTarget = Math.clamp(oceanTargetHexes, 0, total);
        if (oceanTarget == 0) {
            return;
        }

        int landTarget = total - oceanTarget;
        Set<StratConCoords> ocean = switch (type) {
            case INLAND -> scatteredBlobs(track, oceanTarget, 1, 2, true);
            case LAKELANDS -> scatteredBlobs(track, oceanTarget, 4, 8, false);
            case MARSHLANDS -> scatteredBlobs(track, oceanTarget, 8, 16, false);
            case COASTAL -> singleBlob(track, oceanTarget, randomEdgeCoords(track));
            case INLAND_SEA -> singleBlob(track, oceanTarget, centerCoords(track));
            case RIVERLANDS -> river(track, oceanTarget);
            case ISLAND -> complement(track, singleBlob(track, landTarget, centerCoords(track)));
            case ARCHIPELAGO -> complement(track, scatteredBlobs(track, landTarget, 2, 6, false));
            case PENINSULA -> complement(track, singleBlob(track, landTarget, randomEdgeCoords(track)));
        };

        for (StratConCoords coords : ocean) {
            track.setTerrainTile(coords, oceanTerrain);
        }
    }

    /**
     * Grows one or more disjoint water regions from random seeds until roughly {@code target} hexes are covered.
     *
     * @param isolate when {@code true}, later blobs may not touch earlier ones (used for a few clearly separate spots)
     */
    private static Set<StratConCoords> scatteredBlobs(StratConTrackState track, int target, int minBlobs, int maxBlobs,
          boolean isolate) {
        int blobs = max(1, minBlobs + Compute.randomInt((maxBlobs - minBlobs) + 1));
        int perBlob = max(1, target / blobs);

        Set<StratConCoords> region = new HashSet<>();
        int attempts = 0;
        int attemptLimit = blobs * 6;
        while ((region.size() < target) && (attempts < attemptLimit)) {
            attempts++;

            StratConCoords seed = randomCoords(track);
            Set<StratConCoords> blocked = new HashSet<>(region);
            if (isolate) {
                blocked.addAll(neighborsOf(track, region));
            }
            if (blocked.contains(seed)) {
                continue;
            }

            int want = min(perBlob, target - region.size());
            region.addAll(growBlob(track, seed, want, blocked));
        }
        return region;
    }

    /**
     * Grows a single connected water region of roughly {@code target} hexes from the given seed.
     */
    private static Set<StratConCoords> singleBlob(StratConTrackState track, int target, StratConCoords seed) {
        return growBlob(track, seed, target, new HashSet<>());
    }

    /**
     * Grows a connected region from {@code seed} up to {@code size} hexes, never entering a {@code blocked} hex. The
     * region expands from a randomly chosen frontier hex each step, producing an organic (non-circular) blob.
     */
    private static Set<StratConCoords> growBlob(StratConTrackState track, StratConCoords seed, int size,
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

    /**
     * Carves a meandering river (with the occasional bend) from a random edge across the sector, until roughly
     * {@code target} hexes are covered. The path keeps a heading and only turns by one hex-facing at a time, so it
     * winds rather than cutting a straight channel.
     */
    private static Set<StratConCoords> river(StratConTrackState track, int target) {
        Set<StratConCoords> region = new HashSet<>();
        StratConCoords current = randomEdgeCoords(track);
        region.add(current);

        int direction = Compute.randomInt(HEX_DIRECTIONS);
        int guard = 0;
        int guardLimit = (track.getWidth() * track.getHeight() * 4) + 8;
        while ((region.size() < target) && (guard < guardLimit)) {
            guard++;

            // Meander: occasionally turn by a single facing left or right.
            if (Compute.randomInt(3) == 0) {
                direction = (Compute.randomInt(2) == 0) ?
                                  ((direction + 1) % HEX_DIRECTIONS) :
                                  ((direction + HEX_DIRECTIONS - 1) % HEX_DIRECTIONS);
            }

            StratConCoords next = current.translate(direction);
            if (!inBounds(track, next)) {
                // Turn back inward at the edge rather than walking off the map.
                direction = (direction + (HEX_DIRECTIONS / 2)) % HEX_DIRECTIONS;
                continue;
            }

            region.add(next);
            current = next;
        }
        return region;
    }

    /**
     * @return every in-bounds hex not present in {@code land}
     */
    private static Set<StratConCoords> complement(StratConTrackState track, Set<StratConCoords> land) {
        Set<StratConCoords> ocean = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!land.contains(coords)) {
                    ocean.add(coords);
                }
            }
        }
        return ocean;
    }

    private static Set<StratConCoords> neighborsOf(StratConTrackState track, Set<StratConCoords> region) {
        Set<StratConCoords> result = new HashSet<>();
        for (StratConCoords coords : region) {
            result.addAll(neighbors(track, coords));
        }
        return result;
    }

    private static List<StratConCoords> neighbors(StratConTrackState track, StratConCoords coords) {
        List<StratConCoords> result = new ArrayList<>();
        for (int direction = 0; direction < HEX_DIRECTIONS; direction++) {
            StratConCoords neighbor = coords.translate(direction);
            if (inBounds(track, neighbor)) {
                result.add(neighbor);
            }
        }
        return result;
    }

    private static boolean inBounds(StratConTrackState track, Coords coords) {
        return (coords.getX() >= 0) &&
                     (coords.getX() < track.getWidth()) &&
                     (coords.getY() >= 0) &&
                     (coords.getY() < track.getHeight());
    }

    private static StratConCoords randomCoords(StratConTrackState track) {
        return new StratConCoords(Compute.randomInt(track.getWidth()), Compute.randomInt(track.getHeight()));
    }

    private static StratConCoords centerCoords(StratConTrackState track) {
        return new StratConCoords(track.getWidth() / 2, track.getHeight() / 2);
    }

    private static StratConCoords randomEdgeCoords(StratConTrackState track) {
        int width = track.getWidth();
        int height = track.getHeight();

        // Pick one of the four edges, then a random position along it.
        return switch (Compute.randomInt(4)) {
            case 0 -> new StratConCoords(Compute.randomInt(width), 0);
            case 1 -> new StratConCoords(Compute.randomInt(width), height - 1);
            case 2 -> new StratConCoords(0, Compute.randomInt(height));
            default -> new StratConCoords(width - 1, Compute.randomInt(height));
        };
    }
}
