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

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MapSettings;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.biome.StratConTerrainCategory;
import mekhq.campaign.mission.scenarios.AtBScenario;

/**
 * Tunes a scenario's generated battle map so it reflects the StratCon sector hex it is fought on. Battle-map selection
 * (the pool/theme) already sets the board's base character; this tuner reinforces the hex's defining terrain, sets an
 * appropriate tileset, and layers on the additive features a single theme cannot express - roads, water, and cities -
 * so the tactical map reads as the same place shown on the sector map.
 *
 * <p>The scenario carries the hex context it needs ({@link AtBScenario#getTerrainType()},
 * {@link AtBScenario#getStratConRoadEntryEdges()}, {@link AtBScenario#isStratConWaterAdjacent()},
 * {@link AtBScenario#isStratConUrban()}), recorded when the scenario was set up, so no track lookup is needed at
 * launch. Reinforcement uses absolute parameter values, so it also normalizes boards whose selected theme under-sells
 * the terrain. Only generated (non-fixed) boards are affected; fixed maps are used as authored.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConMapTuner {
    private StratConMapTuner() {}

    /** Forced road probability (percent) for a hex that carries a sector road. */
    private static final int ROAD_PROBABILITY = 100;

    /** Minimum lake spots for a water-adjacent hex, so a coastal/riverside fight actually shows water. */
    private static final int WATER_MIN_SPOTS = 1;
    private static final int WATER_MIN_MAX_SPOTS = 2;

    /** City-block count range for an urban hex, scaled from a sparse frontier town to a dense metropolis. */
    private static final int URBAN_MIN_BLOCKS = 6;
    private static final int URBAN_MAX_BLOCKS = 26;
    /** Urbanization thresholds separating town / hub / metropolis city forms. */
    private static final double URBAN_TOWN_CEILING = 0.33;
    private static final double URBAN_HUB_CEILING = 0.66;

    /**
     * Applies all hex-derived tuning to the given map settings for the given scenario.
     *
     * @param mapSettings the settings the board will be generated from (already loaded from the selected theme)
     * @param scenario    the scenario being launched, carrying its StratCon hex context
     */
    public static void tune(MapSettings mapSettings, AtBScenario scenario) {
        applyTerrainEmphasis(mapSettings, scenario);
        applyRoads(mapSettings, scenario);
        applyWater(mapSettings, scenario);
        applyUrban(mapSettings, scenario);
    }

    /**
     * Reinforces the hex's defining terrain feature and selects a matching tileset, so the board reads unmistakably as
     * the hex's terrain rather than whatever the selected theme happened to lean toward.
     */
    private static void applyTerrainEmphasis(MapSettings mapSettings, AtBScenario scenario) {
        String terrain = groundBeneath(scenario.getTerrainType());
        if ((terrain == null) || terrain.isBlank()) {
            return;
        }

        StratConBiomeManifest manifest = StratConBiomeManifest.getInstance();
        StratConTerrainCategory category = manifest.getTerrainCategory(terrain);

        switch (category) {
            case VEGETATION -> {
                if ("Swamp".equals(terrain)) {
                    mapSettings.setSwampParams(3, 8, 2, 4);
                } else {
                    // dense woods (more, larger, some heavy)
                    mapSettings.setForestParams(4, 10, 4, 12, 40, 0);
                }
            }
            case BARREN -> {
                mapSettings.setRoughParams(4, 12, 2, 4, 0);
                mapSettings.setSandParams(3, 10, 1, 3);
            }
            case HILLS -> mapSettings.setElevationParams(60, 5, 5);
            case MOUNTAIN -> {
                mapSettings.setElevationParams(70, 6, 5);
                mapSettings.setCliffParam(40);
                int style = "snow".equals(tilesetTheme(terrain)) ?
                                  MapSettings.MOUNTAIN_SNOW_CAPPED :
                                  MapSettings.MOUNTAIN_PLAIN;
                mapSettings.setMountainParams(1, 7, 16, 5, 9, style);
            }
            case AGRICULTURE -> mapSettings.setPlantedFieldParams(6, 14, 2, 4);
            default -> {
                // NEUTRAL, LUNAR, VOLCANIC, URBAN, OCEAN: no feature reinforcement here. Lunar and volcanic get their
                // full look (craters, rubble, magma peaks) from dedicated mapgen themes; the rest come from the theme.
            }
        }

        String theme = tilesetTheme(terrain);
        if (theme != null) {
            mapSettings.setTheme(theme);
        }
    }

    /**
     * @return the MegaMek tileset theme for the given terrain, or {@code null} to leave the board's default look alone.
     *       Authored per terrain in the biome manifest, so adding a terrain to mm-data no longer means editing a chain
     *       of name tests here.
     */
    private static @Nullable String tilesetTheme(String terrain) {
        return StratConBiomeManifest.terrainTilesetTheme(terrain);
    }

    /**
     * Resolves a scenario's terrain type to the actual ground it stands on.
     *
     * <p>A facility scenario's terrain type is a map-pool key rather than a terrain: {@code VolcanoFacility} rather
     * than {@code Volcano}. That key has to stay on the scenario, because {@link AtBScenario#getTerrainType()}
     * containing "FACILITY" is what qualifies a scenario for turret placement. But left as-is it resolves to no
     * category and no theme, so a base on a volcano drew volcanic boards while the tileset stayed generic. Stripping
     * the suffix here gives the tuner the real ground without disturbing the scenario.</p>
     *
     * <p>The generic temperature-banded keys ({@code TemperateFacility} and friends) are deliberately left alone -
     * "Temperate" is not a terrain, so there is nothing truer to resolve them to.</p>
     *
     * @param terrainType the scenario's terrain type, possibly a facility map-pool key
     *
     * @return the underlying terrain name, or {@code terrainType} unchanged when it is not a terrain-keyed facility
     *       pool
     */
    private static @Nullable String groundBeneath(@Nullable String terrainType) {
        if ((terrainType == null) || !terrainType.endsWith(StratConBiomeManifest.FACILITY_POOL_SUFFIX)) {
            return terrainType;
        }

        String stripped = terrainType.substring(0,
              terrainType.length() - StratConBiomeManifest.FACILITY_POOL_SUFFIX.length());
        return StratConBiomeManifest.getInstance().getTerrainTypeNames().contains(stripped) ? stripped : terrainType;
    }


    /**
     * When the hex carries a sector road, force the generator to lay a road across the board so it reads as continuing
     * the sector road network.
     */
    private static void applyRoads(MapSettings mapSettings, AtBScenario scenario) {
        if (!scenario.getStratConRoadEntryEdges().isEmpty()) {
            mapSettings.setRoadParam(ROAD_PROBABILITY);
        }
    }

    /**
     * When the hex borders open water, ensure the generator lays down at least one body of water so a coastal or
     * riverside fight reads as such.
     */
    private static void applyWater(MapSettings mapSettings, AtBScenario scenario) {
        if (scenario.isStratConWaterAdjacent()) {
            mapSettings.setWaterParams(Math.max(WATER_MIN_SPOTS, mapSettings.getMinWaterSpots()),
                  Math.max(WATER_MIN_MAX_SPOTS, mapSettings.getMaxWaterSpots()),
                  mapSettings.getMinWaterSize(),
                  mapSettings.getMaxWaterSize(),
                  mapSettings.getProbDeep());
        }
    }

    /**
     * When the hex holds a city, lay an urban area onto the board over whatever base terrain the theme produced (so a
     * city in the mountains or on volcanic ground still reads as a city). The city's form and size scale with the
     * sector's urbanization, from a sparse frontier town up to a dense metropolis.
     */
    private static void applyUrban(MapSettings mapSettings, AtBScenario scenario) {
        if (!scenario.isStratConUrban()) {
            return;
        }

        double urbanization = Math.clamp(scenario.getStratConUrbanization(), 0.0, 1.0);

        String cityType;
        if (urbanization < URBAN_TOWN_CEILING) {
            cityType = "TOWN";
        } else if (urbanization < URBAN_HUB_CEILING) {
            cityType = "HUB";
        } else {
            cityType = "METRO";
        }

        int cityBlocks = (int) Math.round(URBAN_MIN_BLOCKS + (urbanization * (URBAN_MAX_BLOCKS - URBAN_MIN_BLOCKS)));
        int maxFloors = (int) Math.round(2 + (urbanization * 8));       // 2..10 storeys
        int density = (int) Math.round(40 + (urbanization * 60));       // 40..100% built-up footprint
        int townSize = (int) Math.round(40 + (urbanization * 60));      // used by the TOWN form

        mapSettings.setCityParams(cityBlocks, cityType, 10, 100, 1, maxFloors, density, townSize);

        // a little paved ground around the buildings
        mapSettings.setPavementParams(1, 3, 2, 6);
    }
}
