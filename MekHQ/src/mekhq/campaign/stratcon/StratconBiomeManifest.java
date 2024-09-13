/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.stratcon;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;

@XmlAccessorType(XmlAccessType.NONE)
public class StratconBiomeManifest {
    private static final MMLogger logger = MMLogger.create(StratconBiomeManifest.class);

    public static final String FOG_OF_WAR = "FogOfWar";
    public static final String DEFAULT = "Default";
    public static final String HEX_SELECTED = "HexSelected";
    public static final String FACILITY_HOSTILE = "FacilityHostile";
    public static final String FACILITY_ALLIED = "FacilityAllied";
    public static final String FORCE_FRIENDLY = "ForceFriendly";
    public static final String FORCE_HOSTILE = "ForceHostile";

    // these constants will eventually be driven by planetary or track data
    /**
     * The "Terran" default biome bucket, used as one of the possible arguments for
     * calls to getTempMap()
     */
    public static final String TERRAN_BIOME = "Terran";

    /**
     * The "TerranFacility" default biome bucket, used as one of the possible
     * arguments for calls to getTempMap()
     */
    public static final String TERRAN_FACILITY_BIOME = "TerranFacility";

    /**
     * This enum is used to determine whether an image being retrieved is a terrain
     * tile or a facility
     */
    public enum ImageType {
        /**
         * Image name is retrieved using getBiomeImage()
         */
        TerrainTile,
        /**
         * Image name is retrieved using getFacilityImage()
         */
        Facility
    }

    public static class MapTypeList {
        public List<String> mapTypes = new ArrayList<>();
    }

    @XmlElement(name = "biomes")
    private List<StratconBiome> biomes = new ArrayList<>();
    @XmlElement(name = "biomeMapTypes")
    private Map<String, MapTypeList> biomeMapTypes = new HashMap<>();
    @XmlElement(name = "biomeImages")
    private Map<String, String> biomeImages = new HashMap<>();
    @XmlElement(name = "facilityImages")
    private Map<String, String> facilityImages = new HashMap<>();

    // derived fields, populated at load time
    private Map<String, TreeMap<Integer, StratconBiome>> biomeTempMap = new HashMap<>();
    private Map<String, List<StratconBiome>> biomeCategoryMap = new HashMap<>();

    public TreeMap<Integer, StratconBiome> getTempMap(String category) {
        return biomeTempMap.get(category);
    }

    public Map<String, MapTypeList> getBiomeMapTypes() {
        return biomeMapTypes;
    }

    /**
     * Get the file path for the hex image corresponding to the given terrain type
     */
    public String getBiomeImage(String biomeType) {
        if (biomeImages.containsKey(biomeType)) {
            return biomeImages.get(biomeType);
        }

        logger.warn(
                "Biome image not defined in data\\stratconbiomedefinitions\\StratconBiomeManifest.xml: " + biomeType);
        return null;
    }

    /**
     * Get the file path for the facility image corresponding to the given facility
     * type
     * Returns default facility if specific facility type is not defined.
     */
    public String getFacilityImage(String facilityType) {
        if (facilityImages.containsKey(facilityType)) {
            return facilityImages.get(facilityType);
        }

        if (facilityImages.containsKey(DEFAULT)) {
            return facilityImages.get(DEFAULT);
        }

        logger.warn("Default facility image not defined in data\\stratconbiomedefinitions\\StratconBiomeManifest.xml.");

        return null;
    }

    private static StratconBiomeManifest instance;

    /**
     * Gets the singleton biome manifest instance
     */
    public static StratconBiomeManifest getInstance() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    private static StratconBiomeManifest load() {
        StratconBiomeManifest resultingManifest = null;
        File inputFile = new File(MHQConstants.STRATCON_BIOME_MANIFEST_PATH);
        if (!inputFile.exists()) {
            logger.warn(String.format("Specified file %s does not exist", MHQConstants.STRATCON_BIOME_MANIFEST_PATH));
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratconBiomeManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratconBiomeManifest> manifestElement = um.unmarshal(inputSource,
                        StratconBiomeManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            logger.error("Error Deserializing Facility Manifest", e);
            return null;
        }

        for (StratconBiome biome : resultingManifest.biomes) {
            // initialize mapping of biome category to temp map
            if (!resultingManifest.biomeTempMap.containsKey(biome.biomeCategory)) {
                resultingManifest.biomeTempMap.put(biome.biomeCategory, new TreeMap<>());
            }

            resultingManifest.biomeTempMap.get(biome.biomeCategory).put(biome.allowedTemperatureLowerBound, biome);

            // initialize mapping of biome category to list of biomes
            if (!resultingManifest.biomeCategoryMap.containsKey(biome.biomeCategory)) {
                resultingManifest.biomeCategoryMap.put(biome.biomeCategory, new ArrayList<>());
            }

            resultingManifest.biomeCategoryMap.get(biome.biomeCategory).add(biome);
        }

        return resultingManifest;
    }
}
