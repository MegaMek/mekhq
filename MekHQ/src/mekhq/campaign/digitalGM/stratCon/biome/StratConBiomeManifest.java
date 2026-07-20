/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.biome;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.transform.Source;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.utilities.MHQXMLUtility;

/**
 * The authored catalogue of StratCon terrain, loaded once from {@code StratConBiomeManifest.xml} and reachable through
 * {@link #getInstance()}. Everything the sector generator and the sector map know about terrain comes from here; no
 * terrain name is hard-coded in the generation code.
 *
 * <p>It carries four things:</p>
 * <ul>
 *     <li><b>Biomes</b> &mdash; temperature-keyed buckets ({@link #TERRAN_BIOME}, {@link #AIRLESS_BIOME} and their
 *     facility variants). {@link #getTempMap} returns a bucket as a floor-lookup map, so a sector's temperature
 *     selects the palette of terrain that can appear in it.</li>
 *     <li><b>Terrain types</b> &mdash; each terrain's {@link StratConTerrainCategory}, whether it is arable, and its
 *     temperature offset. The static {@code isOceanTerrain}/{@code isMountainTerrain}/... helpers are category tests
 *     against this list, and are how the placers ask what a hex is without matching on names.</li>
 *     <li><b>Map pools</b> &mdash; the mapgen boards a scenario may draw from. {@link #getMapTypesForTerrain} resolves
 *     a terrain's own pool first and falls back to its category pool, then to {@code NEUTRAL};
 *     {@link #getFacilityPoolKey} gives the separate pool a base on that terrain fights on.</li>
 *     <li><b>Images</b> &mdash; the tile and facility sprites the sector map renders, selected by
 *     {@link ImageType}.</li>
 * </ul>
 *
 * <p>The file lives in mm-data and is staged into {@code data} when the application launches, so it is absent under
 * test; see {@code StratConTestData} for the fixture seam. A failed load is logged and quietly replaced by a stub of
 * one all-temperatures biome holding nothing but grasslands, so the symptom is uniformly bland sectors rather than an
 * exception.</p>
 */
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

    /**
     * The image key for the single generic urban sprite used to render a city overlay, regardless of the hex's base
     * terrain. Distinct from the climate "Urban"/"ColdUrban"/"HotUrban" terrain tiles.
     */
    public static final String CITY = "City";

    /** The terrain type name for cultivated farmland, placed as a catchment around cities over arable land. */
    public static final String FARMLAND = "Farmland";

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is open water (ocean). Ocean hexes are always revealed, are barred
     *       from hosting scenarios or facilities, and are capped so a sector is always at least partly dry land.
     */
    public static boolean isOceanTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.OCEAN;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is mountainous
     */
    public static boolean isMountainTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.MOUNTAIN;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is urban
     */
    public static boolean isUrbanTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.URBAN;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is volcanic
     */
    public static boolean isVolcanicTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.VOLCANIC;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is barren lunar/planetary rock
     */
    public static boolean isLunarTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.LUNAR;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is living vegetation
     */
    public static boolean isVegetationTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.VEGETATION;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is barren, dry, or frozen ground
     */
    public static boolean isBarrenTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.BARREN;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is rolling hills
     */
    public static boolean isHillsTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.HILLS;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain type is cultivated farmland
     */
    public static boolean isAgricultureTerrain(String terrainType) {
        return getInstance().getTerrainCategory(terrainType) == StratConTerrainCategory.AGRICULTURE;
    }

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return {@code true} if the given terrain is arable, i.e. open land a city's agricultural catchment can convert
     *       to farmland. Driven by the {@code arable} flag on the terrain definition in the biome manifest.
     */
    public static boolean isArableTerrain(String terrainType) {
        return getInstance().isArable(terrainType);
    }

    /**
     * The Celsius offset a terrain's local climate applies to a sector's average temperature, used both for the
     * selected-hex readout and for a scenario's board temperature. Volcanic ground bakes; mountains, frozen ground, and
     * "cold" terrain chill; hot/dry terrain warms. Offsets stack (e.g. a cold mountain is both).
     *
     * @param terrainType a StratCon terrain type name
     *
     * @return the temperature offset in Celsius (0 for unknown or temperate terrain)
     */
    public static int terrainTemperatureOffset(String terrainType) {
        if ((terrainType == null) || terrainType.isBlank()) {
            return 0;
        }

        if (isVolcanicTerrain(terrainType)) {
            return 25;
        }

        int offset = 0;
        if (isMountainTerrain(terrainType)) {
            offset -= 6; // elevation cooling
        }

        if (terrainType.startsWith("Cold") ||
                  terrainType.equals("Glacier") ||
                  terrainType.equals("SnowField") ||
                  terrainType.equals("FrozenSea") ||
                  terrainType.equals("ArcticDesert") ||
                  terrainType.equals("Tundra")) {
            offset -= 8;
        } else if (terrainType.startsWith("Hot") || terrainType.equals("Desert") || terrainType.equals("Badlands")) {
            offset += 8;
        }

        return offset;
    }

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
     * The "Airless" biome bucket, used for worlds with no atmosphere: its terrains are the lunar/volcanic set the
     * improved terrain generator fills airless sectors from, replacing what used to be a hardcoded list.
     */
    public static final String AIRLESS_BIOME = "Airless";

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
    @XmlElement(name = "terrainType")
    private List<StratConTerrainType> terrainTypes = new ArrayList<>();
    @XmlElement(name = "biomeMapTypes")
    private Map<String, MapTypeList> biomeMapTypes = new HashMap<>();
    @XmlElement(name = "biomeImages")
    private Map<String, String> biomeImages = new HashMap<>();
    @XmlElement(name = "facilityImages")
    private Map<String, String> facilityImages = new HashMap<>();

    // derived fields, populated at load time
    private final Map<String, TreeMap<Integer, StratConBiome>> biomeTempMap = new HashMap<>();
    private final Map<String, List<StratConBiome>> biomeCategoryMap = new HashMap<>();
    private final Map<String, StratConTerrainType> terrainTypeMap = new HashMap<>();

    /**
     * @param terrainType a StratCon terrain type name (as returned by {@link StratConTrackState#getTerrainTile})
     *
     * @return the terrain's authored {@link StratConTerrainCategory}, or {@link StratConTerrainCategory#NEUTRAL} if the
     *       terrain is unknown or records no category
     */
    public StratConTerrainCategory getTerrainCategory(String terrainType) {
        if (terrainType == null) {
            return StratConTerrainCategory.NEUTRAL;
        }

        StratConTerrainType definition = terrainTypeMap.get(terrainType);
        if ((definition == null) || (definition.category == null)) {
            return StratConTerrainCategory.NEUTRAL;
        }

        return definition.category;
    }

    /**
     * @param terrainType a StratCon terrain type name
     *
     * @return {@code true} if the terrain definition marks it arable; {@code false} for unknown terrain
     */
    public boolean isArable(String terrainType) {
        if (terrainType == null) {
            return false;
        }

        StratConTerrainType definition = terrainTypeMap.get(terrainType);
        return (definition != null) && definition.arable;
    }

    public TreeMap<Integer, StratConBiome> getTempMap(String category) {
        return biomeTempMap.get(category);
    }

    public Map<String, MapTypeList> getBiomeMapTypes() {
        return biomeMapTypes;
    }

    /**
     * @return the names of every declared terrain type, ocean terrains included. Note this covers only real terrain -
     *       not the synthetic map-pool keys (convoy and facility pools, category fallbacks) that also live in
     *       {@link #getBiomeMapTypes()}.
     */
    public Set<String> getTerrainTypeNames() {
        return Collections.unmodifiableSet(terrainTypeMap.keySet());
    }

    /**
     * Resolves the battle-map pool for a terrain type: its own name-keyed pool when one exists, otherwise the pool
     * keyed by the terrain's {@link StratConTerrainCategory} name. This lets terrains without a dedicated pool - and
     * any terrain added later - still resolve to an appropriate set of boards.
     *
     * @param terrainType a StratCon terrain type name
     *
     * @return the resolved map-type pool, or {@code null} if neither an exact nor a category pool is defined
     */
    public MapTypeList getMapTypesForTerrain(String terrainType) {
        MapTypeList exact = biomeMapTypes.get(terrainType);
        if (exact != null) {
            return exact;
        }
        return biomeMapTypes.get(getTerrainCategory(terrainType).name());
    }

    /** Suffix marking a map pool as the facility variant of a terrain. */
    public static final String FACILITY_POOL_SUFFIX = "Facility";

    /**
     * Resolves the map-pool key to use for a facility standing on the given terrain, so a base fights on a board that
     * matches the ground it sits on rather than on climate alone. The key is the terrain's own name plus
     * {@link #FACILITY_POOL_SUFFIX}, e.g. {@code ColdForestFacility}.
     *
     * <p>Matching is by exact terrain name only: unlike {@link #getMapTypesForTerrain} there is deliberately no
     * category-level fallback. Terrain names already carry their climate (ColdForest vs Forest vs HotForest), so a
     * category pool could only be climate-blind - a worse match for an unrecognized terrain than the temperature-banded
     * facility biome the caller falls back to when this returns {@code null}.</p>
     *
     * @param terrainType a StratCon terrain type name
     *
     * @return the key of the declared facility pool, or {@code null} if the terrain declares none and the caller should
     *       fall back to the generic temperature-banded facility biome
     */
    public @Nullable String getFacilityPoolKey(String terrainType) {
        String key = terrainType + FACILITY_POOL_SUFFIX;
        return biomeMapTypes.containsKey(key) ? key : null;
    }

    /**
     * Get the file path for the hex image corresponding to the given terrain type. Prefers an image declared on the
     * terrain definition, falling back to the {@code biomeImages} lookup (which also covers non-terrain sprites such as
     * fog of war and force markers).
     */
    public String getBiomeImage(String biomeType) {
        StratConTerrainType definition = terrainTypeMap.get(biomeType);
        if ((definition != null) && (definition.image != null)) {
            return definition.image;
        }

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
     * Gets the singleton biome manifest instance. If the manifest file cannot be loaded, returns a default instance
     * with minimal biome data.
     */
    public static StratConBiomeManifest getInstance() {
        if (instance == null) {
            instance = load();
            if (instance == null) {
                logger.warn("Failed to load biome manifest, using default instance");
                instance = createDefaultInstance();
            }
        }

        return instance;
    }

    /**
     * Creates a default biome manifest with minimal data for fallback/testing scenarios. This ensures the system can
     * function even when the XML configuration is unavailable.
     */
    private static StratConBiomeManifest createDefaultInstance() {
        StratConBiomeManifest manifest = new StratConBiomeManifest();

        // Create a default biome that covers all temperature ranges
        StratConBiome defaultBiome = new StratConBiome();
        defaultBiome.biomeCategory = TERRAN_BIOME;
        defaultBiome.allowedTemperatureLowerBound = Integer.MIN_VALUE;
        defaultBiome.allowedTemperatureUpperBound = Integer.MAX_VALUE;
        defaultBiome.allowedTerrainTypes = new ArrayList<>();
        defaultBiome.allowedTerrainTypes.add("Grasslands");

        TreeMap<Integer, StratConBiome> defaultTempMap = new TreeMap<>();
        defaultTempMap.put(Integer.MIN_VALUE, defaultBiome);

        manifest.biomeTempMap.put(TERRAN_BIOME, defaultTempMap);
        manifest.biomeTempMap.put(TERRAN_FACILITY_BIOME, defaultTempMap);

        manifest.biomes.add(defaultBiome);

        // Keep ocean detection working in this degraded fallback, since it gates scenario/facility placement and hex
        // reveal. Other categories default to NEUTRAL, which only softens improved terrain generation.
        for (String oceanTerrain : new String[] { "Sea", "ColdSea", "HotSea", "FrozenSea" }) {
            StratConTerrainType ocean = new StratConTerrainType();
            ocean.name = oceanTerrain;
            ocean.category = StratConTerrainCategory.OCEAN;
            manifest.terrainTypeMap.put(oceanTerrain, ocean);
        }

        return manifest;
    }

    /**
     * Test seam: loads the biome manifest from an explicit path and installs the result as the singleton.
     *
     * <p>Production resolves {@link MHQConstants#STRAT_CON_BIOME_MANIFEST_PATH}, which lives under the {@code data}
     * directory that is built when the application launches. That directory does not exist in the test environment, so
     * tests point this at their own copy under {@code testresources} instead.</p>
     *
     * @param path the file to load the biome manifest from
     */
    public static void loadForTest(String path) {
        instance = load(path);
    }

    private static StratConBiomeManifest load() {
        return load(MHQConstants.STRAT_CON_BIOME_MANIFEST_PATH);
    }

    private static StratConBiomeManifest load(String path) {
        StratConBiomeManifest resultingManifest;
        File inputFile = new File(path);
        if (!inputFile.exists()) {
            logger.warn("Specified file {} does not exist", path);
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

        for (StratConTerrainType terrainType : resultingManifest.terrainTypes) {
            if (terrainType.name != null) {
                resultingManifest.terrainTypeMap.put(terrainType.name, terrainType);
            }
        }

        return resultingManifest;
    }
}
