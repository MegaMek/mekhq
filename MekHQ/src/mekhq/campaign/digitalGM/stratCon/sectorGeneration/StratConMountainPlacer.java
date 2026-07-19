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

import static java.lang.Math.max;
import static java.lang.Math.round;

import java.util.HashSet;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * Places mountains onto a StratCon track in a shape determined by an {@link OrogenyProfile}. The number of mountain
 * features scales with gravity and the profile's range-count modifier; each profile arranges them differently (long
 * parallel ranges, short parallel ridges, a single upland, scattered peaks, a volcanic arc, and so on). A per-profile
 * fraction of features are drawn as volcanic terrain, and mountains never overwrite ocean.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConMountainPlacer {
    private StratConMountainPlacer() {}

    /** The maximum base feature count, reached at high gravity. */
    private static final int MAX_RANGES = 4;

    /** The volcanic terrain type used for a volcanic feature. */
    private static final String VOLCANO_TERRAIN = "Volcano";

    // Ridge lengths as a fraction of the larger sector dimension.
    private static final double LONG_RIDGE_FRACTION = 1.0;
    private static final double SHORT_RIDGE_FRACTION = 0.34;
    private static final double ERODED_RIDGE_FRACTION = 0.2;

    // Upland sizes as a fraction of the sector, multiplied by the feature count and capped.
    private static final double MASSIF_FRACTION = 0.05;
    private static final double PLATEAU_FRACTION = 0.08;
    private static final double MAX_UPLAND_FRACTION = 0.5;

    private static final int CLUSTER_MIN_SIZE = 2;
    private static final int CLUSTER_SIZE_SPREAD = 4;
    private static final int LAND_SEED_ATTEMPTS = 20;

    /**
     * How much of a profile's volcanism a comfortable world sheds. Orogeny is chosen mostly on gravity, temperature,
     * and water, none of which discriminate strongly enough to keep active volcanism off a settled, temperate world -
     * so the amount of volcanic ground is damped by habitability here instead. At 1.0 a perfectly habitable world would
     * have no volcanic ground at all; at 0.0 habitability is ignored. Airless, toxic, and temperature-extreme worlds
     * score low on habitability and so keep nearly all of their volcanism.
     *
     * <p>The per-profile {@code volcanismChance} in {@code OrogenyProfiles.yaml} remains the primary knob; this only
     * scales it.</p>
     */
    private static final double HABITABILITY_VOLCANISM_DAMPING = 0.8;

    /**
     * Places mountains on the track according to the given orogeny profile.
     *
     * @param track           the track to paint
     * @param mountainTerrain the biome's mountain terrain type, or {@code null} when the biome offers no mountains (in
     *                        which case nothing is placed)
     * @param orogeny         the selected orogeny profile, or {@code null} to place nothing
     * @param gravity         the planet's surface gravity in G; higher gravity means more mountains
     * @param habitability    the planet's habitability, 0.0 to 1.0; comfortable worlds shed most of their volcanism
     *                        (see {@link #effectiveVolcanism}). Pass {@code 0.0} to leave the profile's volcanism as
     *                        authored.
     */
    public static void placeMountains(StratConTrackState track, @Nullable String mountainTerrain,
          @Nullable OrogenyProfile orogeny, double gravity, double habitability) {
        if ((mountainTerrain == null) || (orogeny == null)) {
            return;
        }

        int intensity = Math.clamp(round(gravity * 2 * orogeny.rangeCountModifierOrDefault()), 0, MAX_RANGES);
        int features = Compute.randomInt(intensity + 1);
        if (features == 0) {
            return;
        }

        int volcanism = effectiveVolcanism(orogeny, habitability);
        switch (orogeny.type()) {
            case CORDILLERA -> parallelRidges(track, mountainTerrain, volcanism, features, LONG_RIDGE_FRACTION);
            case BASIN_AND_RANGE ->
                  parallelRidges(track, mountainTerrain, volcanism, features * 2, SHORT_RIDGE_FRACTION);
            case ERODED_ROLLING -> parallelRidges(track, mountainTerrain, volcanism, features, ERODED_RIDGE_FRACTION);
            case MASSIF -> upland(track, mountainTerrain, volcanism, features, MASSIF_FRACTION);
            case PLATEAU -> upland(track, mountainTerrain, volcanism, features, PLATEAU_FRACTION);
            case SCATTERED_PEAKS -> scatteredPeaks(track, mountainTerrain, volcanism, features * 4);
            case SHIELD_CRATERED -> clusters(track, mountainTerrain, volcanism, features * 2);
            case VOLCANIC_ARC -> arc(track, mountainTerrain, volcanism, features);
        }
    }

    /**
     * @param orogeny      the selected profile, supplying the authored volcanism chance
     * @param habitability the planet's habitability, 0.0 to 1.0
     *
     * @return the percentage of this profile's features that should be volcanic, reduced on worlds comfortable enough
     *       to settle - a lush, breathable world may still sit on a volcanic arc, but it should not be paved with lava
     */
    static int effectiveVolcanism(@Nullable OrogenyProfile orogeny, double habitability) {
        if (orogeny == null) {
            return 0;
        }

        double damping = 1.0 - (HABITABILITY_VOLCANISM_DAMPING * Math.clamp(habitability, 0.0, 1.0));
        return (int) Math.round(orogeny.volcanismChanceOrDefault() * damping);
    }

    /**
     * Draws {@code ridgeCount} roughly parallel straight ridges (all sharing one heading), each of a length scaled from
     * the sector size.
     */
    private static void parallelRidges(StratConTrackState track, String mountainTerrain, int volcanism, int ridgeCount,
          double lengthFraction) {
        int length = max(2, (int) round(max(track.getWidth(), track.getHeight()) * lengthFraction));
        int heading = Compute.randomInt(StratConHexGeometry.HEX_DIRECTIONS);

        for (int ridge = 0; ridge < ridgeCount; ridge++) {
            drawStrip(track, randomCoords(track), heading, length, terrainFor(mountainTerrain, volcanism));
        }
    }

    /**
     * Grows a single upland region sized from the sector and feature count, painted as one terrain (all mountains, or
     * all volcanic).
     */
    private static void upland(StratConTrackState track, String mountainTerrain, int volcanism, int features,
          double fraction) {
        int total = track.getWidth() * track.getHeight();
        int cap = max(1, (int) round(total * MAX_UPLAND_FRACTION));
        int size = Math.clamp((int) round(total * fraction * features), 3, cap);

        Set<StratConCoords> blob = StratConHexGeometry.growBlob(track,
              randomLandCoords(track),
              size,
              oceanHexes(track));
        String terrain = terrainFor(mountainTerrain, volcanism);
        for (StratConCoords coords : blob) {
            track.setTerrainTile(coords, terrain);
        }
    }

    /**
     * Places several small mountain/volcanic clusters at random land seeds.
     */
    private static void clusters(StratConTrackState track, String mountainTerrain, int volcanism, int clusterCount) {
        Set<StratConCoords> ocean = oceanHexes(track);
        for (int cluster = 0; cluster < clusterCount; cluster++) {
            int size = CLUSTER_MIN_SIZE + Compute.randomInt(CLUSTER_SIZE_SPREAD);
            Set<StratConCoords> blob = StratConHexGeometry.growBlob(track, randomLandCoords(track), size, ocean);
            String terrain = terrainFor(mountainTerrain, volcanism);
            for (StratConCoords coords : blob) {
                track.setTerrainTile(coords, terrain);
            }
        }
    }

    /**
     * Sprinkles isolated single-hex peaks across the land.
     */
    private static void scatteredPeaks(StratConTrackState track, String mountainTerrain, int volcanism, int peakCount) {
        for (int peak = 0; peak < peakCount; peak++) {
            StratConCoords coords = randomLandCoords(track);
            if (!isOcean(track, coords)) {
                track.setTerrainTile(coords, terrainFor(mountainTerrain, volcanism));
            }
        }
    }

    /**
     * Draws a single curved chain by walking from a random edge and turning consistently one way, producing an arc
     * rather than a straight line. Volcanism is rolled per hex, so a high-volcanism profile reads as mostly volcanic.
     */
    private static void arc(StratConTrackState track, String mountainTerrain, int volcanism, int features) {
        int length = max(4, (int) round(max(track.getWidth(), track.getHeight()) * (1.0 + (features * 0.4))));
        StratConCoords current = randomEdgeCoords(track);
        int heading = Compute.randomInt(StratConHexGeometry.HEX_DIRECTIONS);
        int turnBias = (Compute.randomInt(2) == 0) ? 1 : (StratConHexGeometry.HEX_DIRECTIONS - 1);

        for (int step = 0; step < length; step++) {
            if (StratConHexGeometry.inBounds(track, current) && !isOcean(track, current)) {
                track.setTerrainTile(current, terrainFor(mountainTerrain, volcanism));
            }

            if ((step % 3) == 2) {
                heading = (heading + turnBias) % StratConHexGeometry.HEX_DIRECTIONS;
            }

            StratConCoords next = current.translate(heading);
            if (!StratConHexGeometry.inBounds(track, next)) {
                heading = (heading + (StratConHexGeometry.HEX_DIRECTIONS / 2)) % StratConHexGeometry.HEX_DIRECTIONS;
                next = current.translate(heading);
                if (!StratConHexGeometry.inBounds(track, next)) {
                    break;
                }
            }
            current = next;
        }
    }

    /**
     * Walks a straight strip of {@code length} hexes from {@code start} in {@code heading}, painting each in-bounds,
     * non-ocean hex.
     */
    private static void drawStrip(StratConTrackState track, StratConCoords start, int heading, int length,
          String terrain) {
        StratConCoords current = start;
        for (int step = 0; step < length; step++) {
            if (StratConHexGeometry.inBounds(track, current) && !isOcean(track, current)) {
                track.setTerrainTile(current, terrain);
            }
            StratConCoords next = current.translate(heading);
            if (!StratConHexGeometry.inBounds(track, next)) {
                break;
            }
            current = next;
        }
    }

    private static String terrainFor(String mountainTerrain, int volcanism) {
        return (Compute.randomInt(100) < volcanism) ? VOLCANO_TERRAIN : mountainTerrain;
    }

    private static boolean isOcean(StratConTrackState track, StratConCoords coords) {
        return StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords));
    }

    private static Set<StratConCoords> oceanHexes(StratConTrackState track) {
        Set<StratConCoords> ocean = new HashSet<>();
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (isOcean(track, coords)) {
                    ocean.add(coords);
                }
            }
        }
        return ocean;
    }

    private static StratConCoords randomCoords(StratConTrackState track) {
        return new StratConCoords(Compute.randomInt(track.getWidth()), Compute.randomInt(track.getHeight()));
    }

    /**
     * @return a random non-ocean hex, or the center hex if no land hex is found within a few attempts
     */
    private static StratConCoords randomLandCoords(StratConTrackState track) {
        for (int attempt = 0; attempt < LAND_SEED_ATTEMPTS; attempt++) {
            StratConCoords coords = randomCoords(track);
            if (!isOcean(track, coords)) {
                return coords;
            }
        }
        return new StratConCoords(track.getWidth() / 2, track.getHeight() / 2);
    }

    private static StratConCoords randomEdgeCoords(StratConTrackState track) {
        int width = track.getWidth();
        int height = track.getHeight();
        return switch (Compute.randomInt(4)) {
            case 0 -> new StratConCoords(Compute.randomInt(width), 0);
            case 1 -> new StratConCoords(Compute.randomInt(width), height - 1);
            case 2 -> new StratConCoords(0, Compute.randomInt(height));
            default -> new StratConCoords(width - 1, Compute.randomInt(height));
        };
    }
}
