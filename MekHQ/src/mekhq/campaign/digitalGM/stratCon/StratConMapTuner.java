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

import megamek.common.loaders.MapSettings;
import mekhq.campaign.mission.AtBScenario;

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
        String terrain = scenario.getTerrainType();
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
                int style = isCold(terrain) ? MapSettings.MOUNTAIN_SNOW_CAPPED : MapSettings.MOUNTAIN_PLAIN;
                mapSettings.setMountainParams(1, 7, 16, 5, 9, style);
            }
            case AGRICULTURE -> mapSettings.setPlantedFieldParams(6, 14, 2, 4);
            default -> {
                // NEUTRAL, LUNAR, VOLCANIC, URBAN, OCEAN: no feature reinforcement here; base comes from the theme
                // (and, for volcanic, from a later magma pass).
            }
        }

        String theme = tilesetTheme(terrain, category);
        if (theme != null) {
            mapSettings.setTheme(theme);
        }
    }

    /**
     * @return the tileset theme name that best matches the terrain, or {@code null} to keep the selected theme's own
     *       tileset
     */
    private static String tilesetTheme(String terrain, StratConTerrainCategory category) {
        if ("Mars".equals(terrain)) {
            return "mars";
        }
        if (category == StratConTerrainCategory.LUNAR) {
            return "lunar";
        }
        if (category == StratConTerrainCategory.VOLCANIC) {
            return "volcano";
        }
        if (isCold(terrain)) {
            return "snow";
        }
        if ("Jungle".equals(terrain)) {
            return "jungle";
        }
        if ("HotForest".equals(terrain)) {
            return "tropical";
        }
        if ("Swamp".equals(terrain)) {
            return "swamp";
        }
        if (isHotDry(terrain)) {
            return "desert";
        }
        return null;
    }

    private static boolean isCold(String terrain) {
        return terrain.startsWith("Cold") ||
                     "Glacier".equals(terrain) ||
                     "SnowField".equals(terrain) ||
                     "Tundra".equals(terrain) ||
                     "ArcticDesert".equals(terrain) ||
                     "FrozenSea".equals(terrain);
    }

    private static boolean isHotDry(String terrain) {
        return "Desert".equals(terrain) ||
                     "Badlands".equals(terrain) ||
                     "Steppe".equals(terrain) ||
                     "HotHillsDry".equals(terrain) ||
                     "HotMountainsDry".equals(terrain);
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
}
