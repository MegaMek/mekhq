/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import megamek.common.Compute;
import megamek.common.planetaryconditions.*;
import mekhq.MHQConstants;
import mekhq.campaign.stratcon.StratconBiomeManifest;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@XmlRootElement(name = "TerrainConditionsOddsManifest")
@XmlAccessorType(XmlAccessType.NONE)
public class TerrainConditionsOddsManifest  {
    @XmlElement(name="TerrainConditionsOdds")
    private static List<TerrainConditionsOdds>  TCO = new ArrayList<>();
    
    private static TerrainConditionsOddsManifest instance;

    public static TerrainConditionsOddsManifest getInstance() {
        if (instance == null) {
            instance = load();
            validations();
        }

        return instance;
    }

    private static TerrainConditionsOddsManifest load() {
        TerrainConditionsOddsManifest result = new TerrainConditionsOddsManifest();

        File inputFile = new File(MHQConstants.TERRAIN_CONDITIONS_ODDS_MANIFEST_PATH);
        if (!inputFile.exists()) {
            result.TCO.addAll(initLight());
            result.TCO.addAll(initWind());
            result.TCO.addAll(initWeather());
            result.TCO.addAll(initFog());
            result.TCO.addAll(initBlowingSand());
            result.TCO.addAll(initEMI());

            try {
                JAXBContext context = JAXBContext.newInstance(TerrainConditionsOddsManifest.class);
                JAXBElement<TerrainConditionsOddsManifest> element = new JAXBElement<>(new QName("TerrainConditionsOddsManifest"),
                        TerrainConditionsOddsManifest.class, result);
                StringWriter writer = new StringWriter();

                Marshaller m = context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                String comment = "<!--\n";
                comment += "for a given key the chance is value / sum(odds values)\n";
                comment += "example 10 / sum(100) = 0.1 or 10%\n";
                comment += "mekhq.log contains TerrainConditionsOddsManifest validations to help find errors. only runs once on startup\n";
                comment += "can delete this file to reload defaults\n";
                comment +=  "-->\n\n";
                m.setProperty("org.glassfish.jaxb.xmlHeaders", comment);
                m.marshal(element, writer);
                FileWriter fw = new FileWriter(inputFile);
                fw.append(writer.toString());
                fw.close();
            } catch (Exception ex) {
                LogManager.getLogger().error("Error Serializing TerrainConditionsOddsManifest", ex);
            }
        } else {
            try {
                JAXBContext context = JAXBContext.newInstance(TerrainConditionsOddsManifest.class);
                Unmarshaller um = context.createUnmarshaller();
                try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                    Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                    JAXBElement<TerrainConditionsOddsManifest> element = um.unmarshal(inputSource, TerrainConditionsOddsManifest.class);
                    result = element.getValue();
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("Error Deserializing TerrainConditionsOddsManifest", ex);
            }
        }

        return result;
    }

    private static void validations() {
        Set<String> mapTypes = StratconBiomeManifest.getInstance().getBiomeMapTypes().keySet();
        List<String> types = List.of(Light.class.getSimpleName(), Wind.class.getSimpleName(),
                Weather.class.getSimpleName(), Fog.class.getSimpleName(),
                BlowingSand.class.getSimpleName(), EMI.class.getSimpleName());
        List<String> enumTypes = new ArrayList<>();
        enumTypes.addAll(Arrays.stream(Light.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));
        enumTypes.addAll(Arrays.stream(Wind.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));
        enumTypes.addAll(Arrays.stream(Weather.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));
        enumTypes.addAll(Arrays.stream(Fog.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));
        enumTypes.addAll(Arrays.stream(BlowingSand.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));
        enumTypes.addAll(Arrays.stream(EMI.values()).map(e -> e.getExternalId()).collect(Collectors.toList()));

        Map<String, Integer> dupTerrain = new HashMap<>();
        List<String> unknownTerrain = new ArrayList<>();
        List<String> unknownTypes = new ArrayList<>();
        List<String> unknownEnums = new ArrayList<>();
        Map<String, Set<String>> conditionTerrain = new HashMap<>();
        Set<String> terrainSet;

        for (TerrainConditionsOdds tco : instance.TCO) {
            String msg = tco.type + " " + tco.name + " odds sum: " + tco.odds.values().stream().mapToInt(i -> i).sum();
            LogManager.getLogger().info(msg);

            for (String terrain : tco.terrain) {
                String key = tco.type + " " + terrain;
                dupTerrain.put(key, dupTerrain.getOrDefault(key, 0) + 1);
                terrainSet = conditionTerrain.getOrDefault(tco.type, new HashSet<>());
                terrainSet.add(terrain);
                conditionTerrain.put(tco.type, terrainSet);
                if (!mapTypes.contains(terrain)) {
                    unknownTerrain.add(tco.type + " " + tco.name + " " +  terrain);
                }
                if (!types.contains(tco.type) && !unknownTypes.contains(tco.type)) {
                    unknownTypes.add(tco.type);
                }
            }
            for (Map.Entry<String, Integer> entry: tco.odds.entrySet()) {
                if (!enumTypes.contains(entry.getKey()) && !unknownEnums.contains(entry.getKey())) {
                    unknownEnums.add(entry.getKey());
                }
            }
        }

        if (!unknownTerrain.isEmpty()) {
            LogManager.getLogger().info("unknown terrain: " + unknownTerrain.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        if (!unknownTypes.isEmpty()) {
            LogManager.getLogger().info("unknown type: " + unknownTypes.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        if (!unknownEnums.isEmpty()) {
            LogManager.getLogger().info("unknown odds key: " + unknownEnums.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        for (Map.Entry<String, Integer> entry : dupTerrain.entrySet()) {
            if (entry.getValue() > 1) {
                LogManager.getLogger().info("duplicate terrain: " + entry.getKey() + " " + entry.getValue());
            }
        }
        for (Map.Entry<String, Set<String>> entry : conditionTerrain.entrySet()) {
            Set<String> missing = new HashSet<>();
            missing.addAll(mapTypes);
            missing.removeAll(entry.getValue());
            if (!missing.isEmpty()) {
                LogManager.getLogger().info("missing terrain " + entry.getKey() + ": " + missing.stream().map(Object::toString).collect(Collectors.joining(", ")));
            }
        }
    }

    private static List<TerrainConditionsOdds> initLight() {
        List<String> terrain;
        Map<String, Integer> odds;
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds = Map.of(Light.DAY.getExternalId(), 600,
                Light.DUSK.getExternalId(),200,
                Light.FULL_MOON.getExternalId(),70,
                Light.GLARE.getExternalId(),30,
                Light.MOONLESS.getExternalId(),69,
                Light.SOLAR_FLARE.getExternalId(),30,
                Light.PITCH_BLACK.getExternalId(),1);
        terrain = List.of("ArcticDesert", "Badlands", "ColdFacility", "ColdForest", "ColdHills",
                "ColdSea", "ColdUrban", "Desert", "Forest", "FrozenFacility", "FrozenSea",
                "Hills", "HotFacility", "HotForest", "HotHillsDry", "HotHillsWet", "HotSea",
                "HotUrban", "Jungle", "Plains", "Savannah", "Sea", "SnowField", "Steppe",
                "Swamp", "TemperateFacility", "Tundra", "Urban");
        t = new TerrainConditionsOdds();
        t.type = Light.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(Light.DAY.getExternalId(), 50,
                Light.DUSK.getExternalId(),20,
                Light.FULL_MOON.getExternalId(),9,
                Light.GLARE.getExternalId(),1,
                Light.MOONLESS.getExternalId(),9,
                Light.SOLAR_FLARE.getExternalId(), 1,
                Light.PITCH_BLACK.getExternalId(),10);
        terrain = List.of("ColdMountain", "Glacier", "HotMountainsDry", "HotMountainsWet", "Mountain");
        t = new TerrainConditionsOdds();
        t.type = Light.class.getSimpleName();
        t.name = "dark";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private static List<TerrainConditionsOdds> initWind() {
        List<String> terrain;
        Map<String, Integer> odds;
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds = Map.of(Wind.CALM.getExternalId(), 70,
                Wind.LIGHT_GALE.getExternalId(),10,
                Wind.MOD_GALE.getExternalId(),10,
                Wind.STRONG_GALE.getExternalId(),4,
                Wind.STORM.getExternalId(),3,
                Wind.TORNADO_F1_TO_F3.getExternalId(),2,
                Wind.TORNADO_F4.getExternalId(),1);
        terrain = List.of("ColdFacility", "ColdForest", "ColdHills", "ColdMountain", "ColdUrban",
                "Forest", "FrozenFacility", "Hills", "HotFacility", "HotForest", "HotHillsWet", "HotMountainsWet",
                "HotUrban", "Jungle", "Mountain", "Swamp", "TemperateFacility", "Urban");
        t = new TerrainConditionsOdds();
        t.type = Wind.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(Wind.CALM.getExternalId(), 50,
                Wind.LIGHT_GALE.getExternalId(),15,
                Wind.MOD_GALE.getExternalId(),14,
                Wind.STRONG_GALE.getExternalId(),10,
                Wind.STORM.getExternalId(),5,
                Wind.TORNADO_F1_TO_F3.getExternalId(),4,
                Wind.TORNADO_F4.getExternalId(),2);
        terrain = List.of("ArcticDesert", "Badlands", "ColdSea", "Desert", "FrozenSea",
                "Glacier", "HotHillsDry", "HotMountainsDry", "HotSea", "Plains", "Savannah", "Sea",
                "SnowField", "Steppe", "Tundra");
        t = new TerrainConditionsOdds();
        t.type = Wind.class.getSimpleName();
        t.name = "high";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private static List<TerrainConditionsOdds> initWeather() {
        List<String> terrain;
        Map<String, Integer> odds = new HashMap<>();
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds.put(Weather.CLEAR.getExternalId(), 67);
        odds.put(Weather.LIGHT_RAIN.getExternalId(), 7);
        odds.put(Weather.MOD_RAIN.getExternalId(), 4);
        odds.put(Weather.HEAVY_RAIN.getExternalId(), 4);
        odds.put(Weather.GUSTING_RAIN.getExternalId(), 2);
        odds.put(Weather.DOWNPOUR.getExternalId(), 2);
        odds.put(Weather.LIGHT_SNOW.getExternalId(), 6);
        odds.put(Weather.MOD_SNOW.getExternalId(), 2);
        odds.put(Weather.HEAVY_SNOW.getExternalId(), 2);
        odds.put(Weather.SLEET.getExternalId(), 3);
        odds.put(Weather.ICE_STORM.getExternalId(), 1);
        odds.put(Weather.LIGHT_HAIL.getExternalId(), 0);
        odds.put(Weather.HEAVY_HAIL.getExternalId(), 0);
        odds.put(Weather.LIGHTNING_STORM.getExternalId(), 0);
        terrain = List.of("Forest", "Hills", "HotFacility", "HotForest", "HotUrban", "Mountain",
                "Plains", "Savannah", "Steppe", "TemperateFacility", "Urban");
        t = new TerrainConditionsOdds();
        t.type = Weather.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = new HashMap<>();
        odds.put(Weather.CLEAR.getExternalId(), 47);
        odds.put(Weather.LIGHT_RAIN.getExternalId(), 16);
        odds.put(Weather.MOD_RAIN.getExternalId(), 12);
        odds.put(Weather.HEAVY_RAIN.getExternalId(), 10);
        odds.put(Weather.GUSTING_RAIN.getExternalId(), 8);
        odds.put(Weather.DOWNPOUR.getExternalId(), 5);
        odds.put(Weather.LIGHT_SNOW.getExternalId(), 1);
        odds.put(Weather.MOD_SNOW.getExternalId(), 0);
        odds.put(Weather.HEAVY_SNOW.getExternalId(), 0);
        odds.put(Weather.SLEET.getExternalId(), 1);
        odds.put(Weather.ICE_STORM.getExternalId(), 0);
        odds.put(Weather.LIGHT_HAIL.getExternalId(), 0);
        odds.put(Weather.HEAVY_HAIL.getExternalId(), 0);
        odds.put(Weather.LIGHTNING_STORM.getExternalId(), 0);
        terrain = List.of("HotHillsWet", "HotMountainsWet", "HotSea", "Jungle");
        t = new TerrainConditionsOdds();
        t.type = Weather.class.getSimpleName();
        t.name = "hot wet";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = new HashMap<>();
        odds.put(Weather.CLEAR.getExternalId(), 46);
        odds.put(Weather.LIGHT_RAIN.getExternalId(), 12);
        odds.put(Weather.MOD_RAIN.getExternalId(), 10);
        odds.put(Weather.HEAVY_RAIN.getExternalId(), 8);
        odds.put(Weather.GUSTING_RAIN.getExternalId(), 6);
        odds.put(Weather.DOWNPOUR.getExternalId(), 6);
        odds.put(Weather.LIGHT_SNOW.getExternalId(), 6);
        odds.put(Weather.MOD_SNOW.getExternalId(), 2);
        odds.put(Weather.HEAVY_SNOW.getExternalId(), 1);
        odds.put(Weather.SLEET.getExternalId(), 2);
        odds.put(Weather.ICE_STORM.getExternalId(), 1);
        odds.put(Weather.LIGHT_HAIL.getExternalId(), 0);
        odds.put(Weather.HEAVY_HAIL.getExternalId(), 0);
        odds.put(Weather.LIGHTNING_STORM.getExternalId(), 0);
        terrain = List.of("Sea", "Swamp");
        t = new TerrainConditionsOdds();
        t.type = Weather.class.getSimpleName();
        t.name = "wet";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = new HashMap<>();
        odds.put(Weather.CLEAR.getExternalId(), 46);
        odds.put(Weather.LIGHT_RAIN.getExternalId(), 6);
        odds.put(Weather.MOD_RAIN.getExternalId(), 3);
        odds.put(Weather.HEAVY_RAIN.getExternalId(), 3);
        odds.put(Weather.GUSTING_RAIN.getExternalId(), 1);
        odds.put(Weather.DOWNPOUR.getExternalId(), 1);
        odds.put(Weather.LIGHT_SNOW.getExternalId(), 12);
        odds.put(Weather.MOD_SNOW.getExternalId(), 10);
        odds.put(Weather.HEAVY_SNOW.getExternalId(), 6);
        odds.put(Weather.SLEET.getExternalId(), 8);
        odds.put(Weather.ICE_STORM.getExternalId(), 4);
        odds.put(Weather.LIGHT_HAIL.getExternalId(), 0);
        odds.put(Weather.HEAVY_HAIL.getExternalId(), 0);
        odds.put(Weather.LIGHTNING_STORM.getExternalId(), 0);;
        terrain = List.of("ColdFacility", "ColdForest", "ColdHills", "ColdMountain", "ColdSea", "ColdUrban",
                "FrozenFacility", "FrozenSea", "Glacier" , "SnowField", "Tundra");
        t = new TerrainConditionsOdds();
        t.type = Weather.class.getSimpleName();
        t.name = "snowy";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = new HashMap<>();
        odds.put(Weather.CLEAR.getExternalId(), 95);
        odds.put(Weather.LIGHT_RAIN.getExternalId(), 2);
        odds.put(Weather.MOD_RAIN.getExternalId(), 1);
        odds.put(Weather.HEAVY_RAIN.getExternalId(), 0);
        odds.put(Weather.GUSTING_RAIN.getExternalId(), 0);
        odds.put(Weather.DOWNPOUR.getExternalId(), 0);
        odds.put(Weather.LIGHT_SNOW.getExternalId(), 1);
        odds.put(Weather.MOD_SNOW.getExternalId(), 0);
        odds.put(Weather.HEAVY_SNOW.getExternalId(), 0);
        odds.put(Weather.SLEET.getExternalId(), 1);
        odds.put(Weather.ICE_STORM.getExternalId(), 0);
        odds.put(Weather.LIGHT_HAIL.getExternalId(), 0);
        odds.put(Weather.HEAVY_HAIL.getExternalId(), 0);
        odds.put(Weather.LIGHTNING_STORM.getExternalId(), 0);
        terrain = List.of("ArcticDesert", "Badlands", "Desert", "HotHillsDry", "HotMountainsDry");
        t = new TerrainConditionsOdds();
        t.type = Weather.class.getSimpleName();
        t.name = "dry";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private static List<TerrainConditionsOdds> initFog() {
        List<String> terrain;
        Map<String, Integer> odds;
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds = Map.of(Fog.FOG_NONE.getExternalId(), 90,
                Fog.FOG_LIGHT.getExternalId(),5,
                Fog.FOG_HEAVY.getExternalId(),5);
        terrain = List.of("ArcticDesert", "Forest", "Hills", "Jungle", "Plains", "Savannah", "Steppe", "TemperateFacility", "Urban");
        t = new TerrainConditionsOdds();
        t.type = Fog.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(Fog.FOG_NONE.getExternalId(), 60,
                Fog.FOG_LIGHT.getExternalId(),20,
                Fog.FOG_HEAVY.getExternalId(),20);
        terrain = List.of("ColdFacility", "ColdForest", "ColdHills", "ColdMountain", "ColdSea",
                "ColdUrban", "FrozenFacility", "FrozenSea", "Glacier", "Mountain", "Sea",
                "SnowField", "Swamp", "Tundra");
        t = new TerrainConditionsOdds();
        t.type = Fog.class.getSimpleName();
        t.name = "heavy";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(Fog.FOG_NONE.getExternalId(), 60,
                Fog.FOG_LIGHT.getExternalId(),20,
                Fog.FOG_HEAVY.getExternalId(),20);
        terrain = List.of("Badlands", "Desert", "HotFacility", "HotForest", "HotHillsDry", "HotHillsWet",
                "HotMountainsDry", "HotMountainsWet", "HotSea", "HotUrban");
        t = new TerrainConditionsOdds();
        t.type = Fog.class.getSimpleName();
        t.name = "none";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private static List<TerrainConditionsOdds> initBlowingSand() {
        List<String> terrain;
        Map<String, Integer> odds;
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds = Map.of(BlowingSand.BLOWING_SAND_NONE.getExternalId(), 90,
                BlowingSand.BLOWING_SAND.getExternalId(),10);
        terrain = List.of("ColdFacility", "ColdForest", "ColdHills", "ColdMountain", "ColdSea", "ColdUrban",
                "Forest", "FrozenFacility", "FrozenSea", "Hills", "HotFacility", "HotForest",
                "HotHillsWet", "HotMountainsWet", "HotSea", "HotUrban", "Jungle", "Mountain", "Plains", "Savannah",
                "Sea", "SnowField", "Steppe", "Swamp", "TemperateFacility", "Urban");
        t = new TerrainConditionsOdds();
        t.type = BlowingSand.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(BlowingSand.BLOWING_SAND_NONE.getExternalId(), 40,
                BlowingSand.BLOWING_SAND.getExternalId(),60);
        terrain = List.of("ArcticDesert", "Badlands", "Desert", "Glacier", "HotHillsDry", "HotMountainsDry", "Tundra");
        t = new TerrainConditionsOdds();
        t.type = BlowingSand.class.getSimpleName();
        t.name = "heavy";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private static List<TerrainConditionsOdds> initEMI() {
        List<String> terrain;
        Map<String, Integer> odds;
        List<TerrainConditionsOdds> result = new ArrayList<>();
        TerrainConditionsOdds t;

        odds = Map.of(EMI.EMI_NONE.getExternalId(), 999,
                EMI.EMI.getExternalId(),1);
        terrain = List.of("ColdFacility", "ColdForest", "ColdHills", "ColdMountain", "ColdSea", "ColdUrban",
                "Forest", "FrozenFacility", "FrozenSea", "Glacier", "Hills", "HotFacility", "HotForest",
                "HotHillsWet", "HotMountainsWet", "HotSea", "HotUrban", "Jungle", "Mountain",
                "Plains", "Savannah", "Sea", "SnowField", "Steppe", "Swamp", "TemperateFacility", "Urban");
        t = new TerrainConditionsOdds();
        t.type = EMI.class.getSimpleName();
        t.name = "standard";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        odds = Map.of(EMI.EMI_NONE.getExternalId(), 90,
                EMI.EMI.getExternalId(),10);
        terrain = List.of("ArcticDesert", "Badlands", "Desert", "HotHillsDry", "HotMountainsDry", "Tundra");
        t = new TerrainConditionsOdds();
        t.type = EMI.class.getSimpleName();
        t.name = "high";
        t.terrain = terrain;
        t.odds = odds;
        result.add(t);

        return result;
    }

    private Map<String, Integer> oddsForTerrain(String type, String terrainType) {
        terrainType = terrainType == null ? "Hills" : terrainType;

        for (TerrainConditionsOdds entry : TCO) {
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
