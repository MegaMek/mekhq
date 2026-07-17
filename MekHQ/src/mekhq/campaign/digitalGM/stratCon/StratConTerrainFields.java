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
import static java.lang.Math.round;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

/**
 * Per-hex geographic influence fields derived from a track after oceans and mountains are placed, used by the improved
 * dry-terrain fill so terrain follows geography rather than pure noise.
 *
 * <ul>
 *     <li><b>moisture</b> &mdash; closeness to open water; high near coasts and rivers, falling off inland.</li>
 *     <li><b>relief distance</b> &mdash; hex distance to the nearest mountain/volcanic hex; drives foothills.</li>
 *     <li><b>rain shadow</b> &mdash; whether a hex sits leeward of relief along the prevailing wind (so it is dry).</li>
 *     <li><b>coldness</b> &mdash; a latitudinal gradient across the sector, strongest toward the pole-facing edge.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConTerrainFields {
    private static final int UNREACHABLE = Integer.MAX_VALUE / 2;

    private final int width;
    private final int height;
    private final int[][] waterDistance;
    private final int[][] reliefDistance;
    private final boolean[][] rainShadow;
    private final double[][] coldness;
    private final int moistureReach;

    private StratConTerrainFields(int width, int height, int[][] waterDistance, int[][] reliefDistance,
          boolean[][] rainShadow, double[][] coldness, int moistureReach) {
        this.width = width;
        this.height = height;
        this.waterDistance = waterDistance;
        this.reliefDistance = reliefDistance;
        this.rainShadow = rainShadow;
        this.coldness = coldness;
        this.moistureReach = moistureReach;
    }

    /**
     * Computes the influence fields for a track.
     *
     * @param track         the track, with oceans and mountains already placed
     * @param latitudeBand  the sector's latitude band, which sets the coldness gradient
     * @param windDirection the prevailing wind direction (a hex facing 0-5), used for rain shadow
     *
     * @return the computed fields
     */
    public static StratConTerrainFields compute(StratConTrackState track, LatitudeBand latitudeBand,
          int windDirection) {
        int width = track.getWidth();
        int height = track.getHeight();

        int[][] waterDistance = bfsDistance(track, StratConBiomeManifest::isOceanTerrain);
        int[][] reliefDistance = bfsDistance(track, StratConTerrainFields::isRelief);
        boolean[][] rainShadow = computeRainShadow(track, windDirection);
        double[][] coldness = computeColdness(track, latitudeBand);
        int moistureReach = max(3, (int) round(max(width, height) * 0.5));

        return new StratConTerrainFields(width, height, waterDistance, reliefDistance, rainShadow, coldness,
              moistureReach);
    }

    private static boolean isRelief(String terrainType) {
        return StratConBiomeManifest.isMountainTerrain(terrainType) ||
                     StratConBiomeManifest.isVolcanicTerrain(terrainType);
    }

    /**
     * @return {@code moisture} at the hex: {@code 1.0} on/next to water, falling linearly to {@code 0.0} beyond the
     *       moisture reach, and {@code 0.0} everywhere when the sector has no water
     */
    public double moistureAt(StratConCoords coords) {
        int distance = waterDistance[coords.getX()][coords.getY()];
        if (distance >= UNREACHABLE) {
            return 0.0;
        }
        return max(0.0, 1.0 - ((double) distance / moistureReach));
    }

    /**
     * @return the hex distance to the nearest mountain/volcanic hex, or a large value when the sector has no relief
     */
    public int reliefDistanceAt(StratConCoords coords) {
        return reliefDistance[coords.getX()][coords.getY()];
    }

    /**
     * @return {@code true} if the hex is leeward of relief along the prevailing wind, and therefore dry
     */
    public boolean inRainShadow(StratConCoords coords) {
        return rainShadow[coords.getX()][coords.getY()];
    }

    /**
     * @return the latitudinal coldness at the hex, {@code 0.0} (warm) to {@code 1.0} (cold)
     */
    public double coldnessAt(StratConCoords coords) {
        return coldness[coords.getX()][coords.getY()];
    }

    /**
     * Multi-source breadth-first distance from every hex matching {@code isSource} to every other hex.
     */
    private static int[][] bfsDistance(StratConTrackState track, Predicate<String> isSource) {
        int width = track.getWidth();
        int height = track.getHeight();
        int[][] distance = new int[width][height];
        Deque<StratConCoords> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (isSource.test(track.getTerrainTile(coords))) {
                    distance[x][y] = 0;
                    queue.add(coords);
                } else {
                    distance[x][y] = UNREACHABLE;
                }
            }
        }

        while (!queue.isEmpty()) {
            StratConCoords current = queue.poll();
            int nextDistance = distance[current.getX()][current.getY()] + 1;
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, current)) {
                if (distance[neighbor.getX()][neighbor.getY()] > nextDistance) {
                    distance[neighbor.getX()][neighbor.getY()] = nextDistance;
                    queue.add(neighbor);
                }
            }
        }

        return distance;
    }

    /**
     * A hex is in rain shadow when, walking upwind from it, relief is reached within a few hexes before the sector
     * edge.
     */
    private static boolean[][] computeRainShadow(StratConTrackState track, int windDirection) {
        int width = track.getWidth();
        int height = track.getHeight();
        int reach = max(2, (int) round(max(width, height) * 0.3));
        int upwind = (windDirection + (StratConHexGeometry.HEX_DIRECTIONS / 2)) % StratConHexGeometry.HEX_DIRECTIONS;

        boolean[][] shadow = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                StratConCoords probe = new StratConCoords(x, y);
                for (int step = 0; step < reach; step++) {
                    probe = probe.translate(upwind);
                    if (!StratConHexGeometry.inBounds(track, probe)) {
                        break;
                    }
                    if (isRelief(track.getTerrainTile(probe))) {
                        shadow[x][y] = true;
                        break;
                    }
                }
            }
        }
        return shadow;
    }

    /**
     * A latitudinal gradient: strongest for polar bands and zero at the equator, rising toward the pole-facing edge
     * (the north edge for northern bands, the south edge for southern bands).
     */
    private static double[][] computeColdness(StratConTrackState track, LatitudeBand latitudeBand) {
        int width = track.getWidth();
        int height = track.getHeight();
        double strength = coldnessStrength(latitudeBand);
        int hemisphere = hemisphere(latitudeBand); // -1 north, +1 south, 0 equatorial

        double[][] coldness = new double[width][height];
        if ((strength <= 0.0) || (hemisphere == 0) || (height <= 1)) {
            return coldness;
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double poleFraction = (hemisphere < 0) ?
                                            ((double) (height - 1 - y) / (height - 1)) :
                                            ((double) y / (height - 1));
                coldness[x][y] = strength * poleFraction;
            }
        }
        return coldness;
    }

    private static double coldnessStrength(LatitudeBand latitudeBand) {
        return switch (latitudeBand) {
            case EQUATORIAL -> 0.0;
            case NORTH_TROPICAL, SOUTH_TROPICAL -> 0.2;
            case NORTH_TEMPERATE, SOUTH_TEMPERATE -> 0.5;
            case NORTH_POLAR, SOUTH_POLAR -> 0.8;
        };
    }

    private static int hemisphere(LatitudeBand latitudeBand) {
        return switch (latitudeBand) {
            case EQUATORIAL -> 0;
            case NORTH_TROPICAL, NORTH_TEMPERATE, NORTH_POLAR -> -1;
            case SOUTH_TROPICAL, SOUTH_TEMPERATE, SOUTH_POLAR -> 1;
        };
    }
}
