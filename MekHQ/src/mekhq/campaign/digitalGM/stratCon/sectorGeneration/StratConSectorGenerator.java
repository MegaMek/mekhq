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

import jakarta.annotation.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

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

    /**
     * Generates terrain for the given track using the improved pipeline.
     *
     * @param track        the track to fill; its width, height, and temperature must already be set
     * @param profile      the destination planet's resolved data
     * @param latitudeBand the sector's latitude band, which drives the latitudinal terrain gradient
     * @param allowCities  {@code true} to place cities; {@code false} suppresses them entirely (e.g. when both sides
     *                     observe the Ares Conventions)
     */
    public static void generate(StratConTrackState track, PlanetProfile profile, LatitudeBand latitudeBand,
          boolean allowCities) {
        StratConBiome biome = selectBiome(track.getTemperature());
        String oceanTerrain = oceanTerrainFor(biome);

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

        // Dry fill: geography-aware terrain that follows moisture, rain shadow, and coldness, painted in coherent
        // patches from the biome's climate-appropriate terrains.
        int windDirection = Compute.randomInt(StratConHexGeometry.HEX_DIRECTIONS);
        StratConTerrainFields fields = StratConTerrainFields.compute(track, latitudeBand, windDirection);
        StratConTerrainFiller.fill(track, biome, profile, fields);

        // Cities: an overlay whose count comes from population and whose arrangement comes from the urban profile.
        if (allowCities) {
            UrbanProfile urban = StratConUrban.getInstance().selectProfile(profile);
            StratConCityPlacer.placeCities(track, profile, urban);
            // Farmland: a catchment of cultivated hexes radiating out from each city over arable land.
            StratConFarmPlacer.placeFarms(track, profile, urban);
        }

        // Roads: connect the cities and branch each network off the map.
        StratConRoadPlacer.recalculateRoads(track);

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
