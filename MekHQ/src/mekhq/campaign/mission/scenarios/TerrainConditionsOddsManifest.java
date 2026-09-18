/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.compute.Compute;
import megamek.common.planetaryConditions.BlowingSand;
import megamek.common.planetaryConditions.EMI;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;

/**
 * The authored table of planetary-condition odds, loaded once from {@code TerrainConditionsOddsManifest.yaml} and
 * reached through {@link #getInstance()}. Each {@link TerrainConditionsOdds} entry pairs a condition {@link
 * TerrainConditionsOdds#type} (Light, Wind, Weather, Fog, BlowingSand or EMI) with the biome map types it applies to and
 * a weighted map of that condition's possible values, so a scenario on a given terrain can roll for its own weather.
 *
 * <p>The YAML file is the single source of truth: it lives in mm-data and is staged into the {@code data} directory when
 * the application launches, so it is absent under test. A failed or missing load is logged and quietly replaced by an
 * empty manifest, in which case every roll returns its calm default (clear skies, daylight, no wind) - the same result a
 * terrain with no matching entry already produces.</p>
 *
 * <p>On first load {@link #validations()} logs a report to help authors keep the table complete: the odds sum per entry,
 * any unknown terrain, condition type or odds key, duplicate terrain, and - the common gap - biome map types that no
 * entry covers for a condition. Every key of {@link StratConBiomeManifest#getBiomeMapTypes()} should appear exactly once
 * per condition type; anything missing rolls the calm default instead of authored odds.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class TerrainConditionsOddsManifest {
    private static final MMLogger LOGGER = MMLogger.create(TerrainConditionsOddsManifest.class);

    private List<TerrainConditionsOdds> terrainConditionsOdds = new ArrayList<>();

    private static TerrainConditionsOddsManifest instance;

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // Field-based binding matches the authored data shape and the other StratCon YAML readers.
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return mapper;
    }

    /**
     * @return the singleton manifest, loading and validating it on first access. A failed load yields an empty manifest
     *       rather than {@code null}, so callers never have to guard against a missing table.
     */
    public static TerrainConditionsOddsManifest getInstance() {
        if (instance == null) {
            instance = load();
            instance.validations();
        }

        return instance;
    }

    private static TerrainConditionsOddsManifest load() {
        File inputFile = new File(MHQConstants.TERRAIN_CONDITIONS_ODDS_MANIFEST_PATH);
        if (!inputFile.exists()) {
            LOGGER.warn("Terrain conditions odds file {} does not exist; scenarios will roll default conditions",
                  MHQConstants.TERRAIN_CONDITIONS_ODDS_MANIFEST_PATH);
            return new TerrainConditionsOddsManifest();
        }

        try {
            return MAPPER.readValue(inputFile, TerrainConditionsOddsManifest.class);
        } catch (Exception ex) {
            LOGGER.error("Error deserializing TerrainConditionsOddsManifest; scenarios will roll default conditions", ex);
            return new TerrainConditionsOddsManifest();
        }
    }

    private void validations() {
        Set<String> mapTypes = StratConBiomeManifest.getInstance().getBiomeMapTypes().keySet();
        List<String> types = List.of(Light.class.getSimpleName(),
              Wind.class.getSimpleName(),
              Weather.class.getSimpleName(),
              Fog.class.getSimpleName(),
              BlowingSand.class.getSimpleName(),
              EMI.class.getSimpleName());
        List<String> enumTypes = new ArrayList<>();
        enumTypes.addAll(Arrays.stream(Light.values()).map(Light::getExternalId).toList());
        enumTypes.addAll(Arrays.stream(Wind.values()).map(Wind::getExternalId).toList());
        enumTypes.addAll(Arrays.stream(Weather.values()).map(Weather::getExternalId).toList());
        enumTypes.addAll(Arrays.stream(Fog.values()).map(Fog::getExternalId).toList());
        enumTypes.addAll(Arrays.stream(BlowingSand.values()).map(BlowingSand::getExternalId).toList());
        enumTypes.addAll(Arrays.stream(EMI.values()).map(EMI::getExternalId).toList());

        Map<String, Integer> dupTerrain = new HashMap<>();
        List<String> unknownTerrain = new ArrayList<>();
        List<String> unknownTypes = new ArrayList<>();
        List<String> unknownEnums = new ArrayList<>();
        Map<String, Set<String>> conditionTerrain = new HashMap<>();
        Set<String> terrainSet;

        for (TerrainConditionsOdds tco : terrainConditionsOdds) {
            String msg = tco.type + " " + tco.name + " odds sum: " + tco.odds.values().stream().mapToInt(i -> i).sum();
            LOGGER.info(msg);

            for (String terrain : tco.terrain) {
                String key = tco.type + " " + terrain;
                dupTerrain.put(key, dupTerrain.getOrDefault(key, 0) + 1);
                terrainSet = conditionTerrain.getOrDefault(tco.type, new HashSet<>());
                terrainSet.add(terrain);
                conditionTerrain.put(tco.type, terrainSet);
                if (!mapTypes.contains(terrain)) {
                    unknownTerrain.add(tco.type + " " + tco.name + " " + terrain);
                }
                if (!types.contains(tco.type) && !unknownTypes.contains(tco.type)) {
                    unknownTypes.add(tco.type);
                }
            }
            for (Map.Entry<String, Integer> entry : tco.odds.entrySet()) {
                if (!enumTypes.contains(entry.getKey()) && !unknownEnums.contains(entry.getKey())) {
                    unknownEnums.add(entry.getKey());
                }
            }
        }

        if (!unknownTerrain.isEmpty()) {
            LOGGER.info("unknown terrain: {}",
                  unknownTerrain.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        if (!unknownTypes.isEmpty()) {
            LOGGER.info("unknown type: {}",
                  unknownTypes.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        if (!unknownEnums.isEmpty()) {
            LOGGER.info("unknown odds key: {}",
                  unknownEnums.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        for (Map.Entry<String, Integer> entry : dupTerrain.entrySet()) {
            if (entry.getValue() > 1) {
                LOGGER.info("duplicate terrain: {}, {}", entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Set<String>> entry : conditionTerrain.entrySet()) {
            Set<String> missing = new HashSet<>(mapTypes);
            missing.removeAll(entry.getValue());
            if (!missing.isEmpty()) {
                LOGGER.info("missing terrain {}: {}",
                      entry.getKey(),
                      missing.stream().map(Object::toString).collect(Collectors.joining(", ")));
            }
        }
    }

    private Map<String, Integer> oddsForTerrain(String type, String terrainType) {
        terrainType = terrainType == null ? "HILLS" : terrainType;

        for (TerrainConditionsOdds entry : terrainConditionsOdds) {
            if (entry.type.equals(type) && entry.terrain.contains(terrainType)) {
                return entry.odds;
            }
        }

        return null;
    }

    private String rollCondition(Map<String, Integer> odds) {
        String condition = "";
        int sum = odds.values().stream().mapToInt(i -> i).sum();
        int rollingSum = 0;
        int roll = Compute.randomInt(sum);
        TreeMap<String, Integer> sorted = new TreeMap<>(odds);

        for (Map.Entry<String, Integer> chance : sorted.entrySet()) {
            if (chance.getValue() > 0) {
                rollingSum += chance.getValue();
                if (roll < rollingSum) {
                    condition = chance.getKey();
                    break;
                }
            }
        }

        return condition;
    }

    public Light rollLightCondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(Light.class.getSimpleName(), terrainType);

        return odds != null ? Light.getLight(rollCondition(odds)) : Light.DAY;
    }

    public Wind rollWindCondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(Wind.class.getSimpleName(), terrainType);

        return odds != null ? Wind.getWind(rollCondition(odds)) : Wind.CALM;
    }

    public Weather rollWeatherCondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(Weather.class.getSimpleName(), terrainType);

        return odds != null ? Weather.getWeather(rollCondition(odds)) : Weather.CLEAR;
    }

    public Fog rollFogCondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(Fog.class.getSimpleName(), terrainType);

        return odds != null ? Fog.getFog(rollCondition(odds)) : Fog.FOG_NONE;
    }

    public BlowingSand rollBlowingSandCondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(BlowingSand.class.getSimpleName(), terrainType);

        return odds != null ? BlowingSand.getBlowingSand(rollCondition(odds)) : BlowingSand.BLOWING_SAND_NONE;
    }

    public EMI rollEMICondition(String terrainType) {
        Map<String, Integer> odds = oddsForTerrain(EMI.class.getSimpleName(), terrainType);

        return odds != null ? EMI.getEMI(rollCondition(odds)) : EMI.EMI_NONE;
    }
}
