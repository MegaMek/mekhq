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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * Fills the dry (empty) hexes of a track with terrain, choosing each hex's terrain from the biome by weight and then
 * smoothing the result into coherent patches. Weights combine planet-wide biases (breathability, water, composition)
 * with the per-hex geographic {@link StratConTerrainFields} (coastal moisture, rain shadow, latitudinal coldness), so
 * terrain follows geography rather than pure noise.
 *
 * <p>Because the biome is already temperature-filtered, the geographic fields only need to bias along the
 * vegetation&harr;barren axis: wetter, windward, warmer hexes favor vegetation; drier, leeward, colder hexes favor
 * barren ground.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConTerrainFiller {
    private StratConTerrainFiller() {}

    private static final int BASE_WEIGHT = 10;

    // Planet-wide biases (§7).
    private static final int BREATHABLE_VEGETATION = 8;
    private static final int NON_BREATHABLE_VEGETATION = -7;
    private static final int NON_BREATHABLE_BARREN = 8;
    private static final int WET_VEGETATION = 6;
    private static final int DRY_BARREN = 6;
    private static final int DRY_VEGETATION = -4;
    private static final int ICY_BARREN = 6;
    private static final int ROCKY_BARREN = 6;
    private static final int SWAMP_LANDMASS = 4;

    private static final int WET_WATER_THRESHOLD = 60;
    private static final int DRY_WATER_THRESHOLD = 20;
    private static final int MANY_LANDMASSES = 3;

    // Per-hex geographic biases.
    private static final int MOISTURE_STRENGTH = 8;
    private static final int RAIN_SHADOW_BARREN = 8;
    private static final int RAIN_SHADOW_VEGETATION = -6;
    private static final int COLDNESS_STRENGTH = 8;

    private static final int SMOOTHING_PASSES = 2;

    // Post-fill geographic overrides.
    private static final int PIEDMONT_CHANCE = 55;
    private static final int RIPARIAN_CHANCE = 60;

    private static final String SWAMP_TERRAIN = "Swamp";
    private static final String FALLBACK_TERRAIN = "Badlands";

    /**
     * Fills every empty (dry) hex on the track with terrain, then smooths the fill into coherent patches. Ocean and
     * mountain hexes, already placed, are left untouched.
     *
     * @param track  the track to fill
     * @param biome  the sector's biome
     * @param planet the destination planet's resolved data
     * @param fields the geographic influence fields for this track
     */
    public static void fill(StratConTrackState track, StratConBiome biome, PlanetProfile planet,
          StratConTerrainFields fields) {
        List<String> candidates = candidateTerrains(biome, planet);
        Set<String> candidateSet = Set.copyOf(candidates);

        Map<String, Integer> baseWeights = new HashMap<>();
        for (String terrain : candidates) {
            baseWeights.put(terrain, baseWeight(terrain, planet));
        }

        // 1. Weighted, geography-aware sample per hex.
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!track.getTerrainTile(coords).isEmpty()) {
                    continue;
                }
                track.setTerrainTile(coords, chooseTerrain(candidates, baseWeights, coords, fields));
            }
        }

        // 2. Smooth the fill into coherent patches without disturbing ocean or mountains.
        for (int pass = 0; pass < SMOOTHING_PASSES; pass++) {
            smooth(track, candidateSet);
        }

        // 3. Geographic overrides. Airless worlds have neither foothills nor riverbanks.
        if (!planet.airless()) {
            placePiedmont(track, biome, fields);
            if (!planet.taintedOrToxic()) {
                placeRiparian(track, biome);
            }
        }
    }

    /**
     * Builds the candidate dry-terrain set: the biome's dry terrains excluding water, mountains, and urban, with
     * atmosphere gating (no vegetation on tainted/toxic worlds). Airless worlds draw from the dedicated {@code Airless}
     * biome (its lunar/volcanic terrains) rather than the temperature-selected Terran biome.
     */
    static List<String> candidateTerrains(StratConBiome biome, PlanetProfile planet) {
        StratConBiome sourceBiome = planet.airless() ? airlessBiome(biome, planet) : biome;

        List<String> candidates = new ArrayList<>();
        for (String terrain : sourceBiome.allowedTerrainTypes) {
            if (StratConBiomeManifest.isOceanTerrain(terrain) ||
                      StratConBiomeManifest.isMountainTerrain(terrain) ||
                      StratConBiomeManifest.isUrbanTerrain(terrain)) {
                continue;
            }
            if (planet.taintedOrToxic() && StratConBiomeManifest.isVegetationTerrain(terrain)) {
                continue;
            }
            candidates.add(terrain);
        }

        if (candidates.isEmpty()) {
            candidates.add(FALLBACK_TERRAIN);
        }
        return candidates;
    }

    /**
     * Resolves the {@code Airless} biome for the planet's temperature. Falls back to the given biome only in the
     * degraded case where no airless biome is authored (e.g., a failed manifest load).
     */
    private static StratConBiome airlessBiome(StratConBiome fallback, PlanetProfile planet) {
        var tempMap = StratConBiomeManifest.getInstance().getTempMap(StratConBiomeManifest.AIRLESS_BIOME);
        if ((tempMap == null) || tempMap.isEmpty()) {
            return fallback;
        }

        int kelvin = planet.temperatureCelsius() + StratConContractInitializer.ZERO_CELSIUS_IN_KELVIN;
        var entry = tempMap.floorEntry(kelvin);
        if (entry == null) {
            entry = tempMap.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * The planet-wide (hex-independent) base weight for a terrain type.
     */
    private static int baseWeight(String terrain, PlanetProfile planet) {
        boolean vegetation = StratConBiomeManifest.isVegetationTerrain(terrain);
        boolean barren = StratConBiomeManifest.isBarrenTerrain(terrain);
        int weight = BASE_WEIGHT;

        if (planet.breathable()) {
            if (vegetation) {
                weight += BREATHABLE_VEGETATION;
            }
        } else if (!planet.airless()) {
            if (vegetation) {
                weight += NON_BREATHABLE_VEGETATION;
            }
            if (barren) {
                weight += NON_BREATHABLE_BARREN;
            }
        }

        if (planet.waterPercent() >= WET_WATER_THRESHOLD) {
            if (vegetation) {
                weight += WET_VEGETATION;
            }
        }
        if (planet.waterPercent() <= DRY_WATER_THRESHOLD) {
            if (barren) {
                weight += DRY_BARREN;
            }
            if (vegetation) {
                weight += DRY_VEGETATION;
            }
        }

        if (planet.hasIcyComposition() && barren) {
            weight += ICY_BARREN;
        }
        if (planet.hasRockyComposition() && barren) {
            weight += ROCKY_BARREN;
        }
        if ((planet.landmassCount() >= MANY_LANDMASSES) && terrain.equals(SWAMP_TERRAIN)) {
            weight += SWAMP_LANDMASS;
        }

        return weight;
    }

    /**
     * The per-hex geographic adjustment to a terrain's weight, from moisture, rain shadow, and coldness.
     */
    private static int fieldDelta(String terrain, StratConCoords coords, StratConTerrainFields fields) {
        boolean vegetation = StratConBiomeManifest.isVegetationTerrain(terrain);
        boolean barren = StratConBiomeManifest.isBarrenTerrain(terrain);
        if (!vegetation && !barren) {
            return 0;
        }

        int delta = 0;

        double moistureBias = (fields.moistureAt(coords) - 0.5) * 2.0; // -1 (dry) .. +1 (wet)
        int moistureDelta = (int) round(moistureBias * MOISTURE_STRENGTH);
        if (vegetation) {
            delta += moistureDelta;
        }
        if (barren) {
            delta -= moistureDelta;
        }

        if (fields.inRainShadow(coords)) {
            if (vegetation) {
                delta += RAIN_SHADOW_VEGETATION;
            }
            if (barren) {
                delta += RAIN_SHADOW_BARREN;
            }
        }

        int coldnessDelta = (int) round(fields.coldnessAt(coords) * COLDNESS_STRENGTH);
        if (vegetation) {
            delta -= coldnessDelta;
        }
        if (barren) {
            delta += coldnessDelta;
        }

        return delta;
    }

    /**
     * Picks a terrain for one hex by weight, combining the base weight with the hex's geographic field delta.
     */
    private static String chooseTerrain(List<String> candidates, Map<String, Integer> baseWeights,
          StratConCoords coords, StratConTerrainFields fields) {
        int totalWeight = 0;
        int[] weights = new int[candidates.size()];
        for (int index = 0; index < candidates.size(); index++) {
            String terrain = candidates.get(index);
            weights[index] = max(1, baseWeights.get(terrain) + fieldDelta(terrain, coords, fields));
            totalWeight += weights[index];
        }

        int roll = Compute.randomInt(totalWeight);
        int cumulative = 0;
        for (int index = 0; index < candidates.size(); index++) {
            cumulative += weights[index];
            if (roll < cumulative) {
                return candidates.get(index);
            }
        }
        return candidates.getLast();
    }

    /**
     * One cellular-automata smoothing pass: each fill hex adopts the most common fill terrain among itself and its fill
     * neighbors (ties keep the current terrain), coalescing the fill into coherent patches. Ocean and mountain hexes
     * are left alone and do not vote.
     */
    private static void smooth(StratConTrackState track, Set<String> candidateSet) {
        Map<StratConCoords, String> updates = new HashMap<>();

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                String current = track.getTerrainTile(coords);
                if (!candidateSet.contains(current)) {
                    continue;
                }

                Map<String, Integer> counts = new HashMap<>();
                counts.merge(current, 1, Integer::sum);
                for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, coords)) {
                    String neighborTerrain = track.getTerrainTile(neighbor);
                    if (candidateSet.contains(neighborTerrain)) {
                        counts.merge(neighborTerrain, 1, Integer::sum);
                    }
                }

                String best = current;
                int bestCount = counts.get(current);
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > bestCount) {
                        best = entry.getKey();
                        bestCount = entry.getValue();
                    }
                }
                if (!best.equals(current)) {
                    updates.put(coords, best);
                }
            }
        }

        updates.forEach(track::setTerrainTile);
    }

    /**
     * Piedmont: rings mountains with foothills by converting some fill hexes immediately adjacent to relief into the
     * biome's hills terrain, so peaks blend into the lowlands instead of sitting straight on other terrain.
     */
    private static void placePiedmont(StratConTrackState track, StratConBiome biome, StratConTerrainFields fields) {
        String hills = firstTerrainMatching(biome, StratConBiomeManifest::isHillsTerrain);
        if (hills == null) {
            return;
        }

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if ((fields.reliefDistanceAt(coords) == 1) &&
                          isConvertibleFillHex(track, coords) &&
                          (Compute.randomInt(100) < PIEDMONT_CHANCE)) {
                    track.setTerrainTile(coords, hills);
                }
            }
        }
    }

    /**
     * Riparian: greens the banks by converting some fill hexes adjacent to open water into the biome's vegetation
     * terrain, so forest hugs coasts and rivers.
     */
    private static void placeRiparian(StratConTrackState track, StratConBiome biome) {
        String vegetation = firstTerrainMatching(biome, StratConBiomeManifest::isVegetationTerrain);
        if (vegetation == null) {
            return;
        }

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (isConvertibleFillHex(track, coords) &&
                          hasOceanNeighbor(track, coords) &&
                          (Compute.randomInt(100) < RIPARIAN_CHANCE)) {
                    track.setTerrainTile(coords, vegetation);
                }
            }
        }
    }

    /**
     * @return {@code true} if the hex holds fill terrain that an override may replace (not ocean, mountain, volcanic,
     *       or urban)
     */
    private static boolean isConvertibleFillHex(StratConTrackState track, StratConCoords coords) {
        String terrain = track.getTerrainTile(coords);
        return !StratConBiomeManifest.isOceanTerrain(terrain) &&
                     !StratConBiomeManifest.isMountainTerrain(terrain) &&
                     !StratConBiomeManifest.isVolcanicTerrain(terrain) &&
                     !StratConBiomeManifest.isUrbanTerrain(terrain);
    }

    private static boolean hasOceanNeighbor(StratConTrackState track, StratConCoords coords) {
        for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, coords)) {
            if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(neighbor))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the first terrain in the biome matching the predicate, or {@code null} if the biome offers none
     */
    private static @Nullable String firstTerrainMatching(StratConBiome biome, Predicate<String> predicate) {
        for (String terrain : biome.allowedTerrainTypes) {
            if (predicate.test(terrain)) {
                return terrain;
            }
        }
        return null;
    }
}
