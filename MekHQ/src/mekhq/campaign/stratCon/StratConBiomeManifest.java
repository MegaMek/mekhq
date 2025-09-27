/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratCon;

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
public class StratConBiomeManifest {
    private static final MMLogger logger = MMLogger.create(StratConBiomeManifest.class);

    public static final String FOG_OF_WAR = "FogOfWar";
    public static final String DEFAULT = "Default";
    public static final String HEX_SELECTED = "HexSelected";
    public static final String FACILITY_HOSTILE = "FacilityHostile";
    public static final String FACILITY_ALLIED = "FacilityAllied";
    public static final String FORCE_FRIENDLY = "ForceFriendly";
    public static final String FORCE_HOSTILE = "ForceHostile";

    // these constants will eventually be driven by planetary or track data
    /**
     * The "Terran" default biome bucket, used as one of the possible arguments for calls to getTempMap()
     */
    public static final String TERRAN_BIOME = "Terran";

    /**
     * The "TerranFacility" default biome bucket, used as one of the possible arguments for calls to getTempMap()
     */
    public static final String TERRAN_FACILITY_BIOME = "TerranFacility";

    /**
     * This enum is used to determine whether an image being retrieved is a terrain tile or a facility
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
    private List<StratConBiome> biomes = new ArrayList<>();
    @XmlElement(name = "biomeMapTypes")
    private Map<String, MapTypeList> biomeMapTypes = new HashMap<>();
    @XmlElement(name = "biomeImages")
    private Map<String, String> biomeImages = new HashMap<>();
    @XmlElement(name = "facilityImages")
    private Map<String, String> facilityImages = new HashMap<>();

    // derived fields, populated at load time
    private final Map<String, TreeMap<Integer, StratConBiome>> biomeTempMap = new HashMap<>();
    private final Map<String, List<StratConBiome>> biomeCategoryMap = new HashMap<>();

    public TreeMap<Integer, StratConBiome> getTempMap(String category) {
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

        logger.warn("Biome image not defined in data\\stratconbiomedefinitions\\StratconBiomeManifest.xml: {}",
              biomeType);
        return null;
    }

    /**
     * Get the file path for the facility image corresponding to the given facility type Returns default facility if
     * specific facility type is not defined.
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

    private static StratConBiomeManifest instance;

    /**
     * Gets the singleton biome manifest instance
     */
    public static StratConBiomeManifest getInstance() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    private static StratConBiomeManifest load() {
        StratConBiomeManifest resultingManifest;
        File inputFile = new File(MHQConstants.STRAT_CON_BIOME_MANIFEST_PATH);
        if (!inputFile.exists()) {
            logger.warn("Specified file {} does not exist", MHQConstants.STRAT_CON_BIOME_MANIFEST_PATH);
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(StratConBiomeManifest.class);
            Unmarshaller um = context.createUnmarshaller();
            try (FileInputStream fileStream = new FileInputStream(inputFile)) {
                Source inputSource = MHQXMLUtility.createSafeXmlSource(fileStream);
                JAXBElement<StratConBiomeManifest> manifestElement = um.unmarshal(inputSource,
                      StratConBiomeManifest.class);
                resultingManifest = manifestElement.getValue();
            }
        } catch (Exception e) {
            logger.error("Error Deserializing Facility Manifest", e);
            return null;
        }

        for (StratConBiome biome : resultingManifest.biomes) {
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
