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

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;

/**
 * The improved StratCon terrain generator: builds a sector's terrain from planetary data and a hydrology profile,
 * rather than the legacy random-stripe placement. This is the entry point wired behind the alternate-terrain option.
 *
 * <p>The pipeline runs in a strict order: select the biome from temperature, choose a hydrology profile and place
 * oceans, place mountains, fill the remaining dry land, then reveal the open water. Mountains and the weighted dry fill
 * arrive in later phases; until then a single base terrain fills the dry land so the sector is complete.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSectorGenerator {
    private StratConSectorGenerator() {}

    private static final String FALLBACK_OCEAN_TERRAIN = "Sea";
    private static final String FALLBACK_BASE_TERRAIN = "Plains";

    /**
     * Generates terrain for the given track using the improved pipeline.
     *
     * @param track   the track to fill; its width, height, and temperature must already be set
     * @param profile the destination planet's resolved data
     */
    public static void generate(StratConTrackState track, PlanetProfile profile) {
        StratConBiome biome = selectBiome(track.getTemperature());
        String oceanTerrain = oceanTerrainFor(biome);
        String baseTerrain = baseTerrainFor(biome);

        // Hydrology: pick a profile from the planet's water coverage, then place oceans in that profile's shape.
        StratConHydrology hydrology = StratConHydrology.getInstance();
        HydrologyProfile hydrologyProfile = hydrology.selectProfile(profile.waterPercent());
        int oceanPercent = hydrology.rollOceanPercent(hydrologyProfile);
        int oceanTargetHexes = (int) Math.round((oceanPercent / 100.0) * track.getWidth() * track.getHeight());
        StratConOceanPlacer.placeOceans(track, hydrologyProfile.type(), oceanTargetHexes, oceanTerrain);

        // Mountains: an orogeny profile selected from the planet's conditions shapes the ranges; gravity scales their
        // number. Volcanic where the profile calls for it, never over ocean, and only when the biome offers mountains.
        OrogenyProfile orogeny = StratConOrogeny.getInstance().selectProfile(profile);
        StratConMountainPlacer.placeMountains(track, mountainTerrainFor(biome), orogeny, profile.gravity());

        // TODO (phase 3d): replace the base fill below with the weighted dry fill, random variety, and airless set.
        fillEmptyWithBase(track, baseTerrain);

        // Open water carries no fog of war.
        revealOceanHexes(track);
    }

    /**
     * Selects the biome whose temperature band contains the track temperature, falling back to the coldest biome when
     * the temperature is below every band.
     */
    private static StratConBiome selectBiome(int temperatureCelsius) {
        int kelvin = temperatureCelsius + StratConContractInitializer.ZERO_CELSIUS_IN_KELVIN;
        var tempMap = StratConBiomeManifest.getInstance().getTempMap(StratConBiomeManifest.TERRAN_BIOME);
        var entry = tempMap.floorEntry(kelvin);
        if (entry == null) {
            entry = tempMap.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * @return the biome's ocean terrain type (climate-appropriate), or a plain sea when the biome offers no water
     */
    private static String oceanTerrainFor(StratConBiome biome) {
        for (String terrainType : biome.allowedTerrainTypes) {
            if (StratConBiomeManifest.isOceanTerrain(terrainType)) {
                return terrainType;
            }
        }
        return FALLBACK_OCEAN_TERRAIN;
    }

    /**
     * @return the biome's mountain terrain type, or {@code null} when the biome offers no mountains
     */
    private static @Nullable String mountainTerrainFor(StratConBiome biome) {
        for (String terrainType : biome.allowedTerrainTypes) {
            if (StratConBiomeManifest.isMountainTerrain(terrainType)) {
                return terrainType;
            }
        }
        return null;
    }

    /**
     * @return a random dry, non-mountain, non-urban terrain from the biome to serve as the placeholder base fill
     */
    private static String baseTerrainFor(StratConBiome biome) {
        List<String> candidates = new ArrayList<>();
        for (String terrainType : biome.allowedTerrainTypes) {
            if (!StratConBiomeManifest.isOceanTerrain(terrainType) &&
                      !StratConBiomeManifest.isMountainTerrain(terrainType) &&
                      !StratConBiomeManifest.isUrbanTerrain(terrainType)) {
                candidates.add(terrainType);
            }
        }
        if (candidates.isEmpty()) {
            return FALLBACK_BASE_TERRAIN;
        }
        return candidates.get(Compute.randomInt(candidates.size()));
    }

    /**
     * Fills every still-empty (dry) hex with the base terrain, leaving ocean hexes untouched.
     */
    private static void fillEmptyWithBase(StratConTrackState track, String baseTerrain) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (track.getTerrainTile(coords).isEmpty()) {
                    track.setTerrainTile(coords, baseTerrain);
                }
            }
        }
    }

    /**
     * Adds every ocean hex to the track's revealed set, so open water is always visible.
     */
    private static void revealOceanHexes(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords))) {
                    track.getRevealedCoords().add(coords);
                }
            }
        }
    }
}
