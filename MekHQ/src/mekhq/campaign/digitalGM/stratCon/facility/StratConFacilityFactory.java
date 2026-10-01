/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityJson.LoadedFacilityDefinition;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;

/**
 * This class handles loading StratCon facility definitions and creating facilities from them.
 *
 * <p>Definitions are looked up by ID. Old-format files (one side of a type each, as shipped before 0.51.01) are
 * merged in pairs through their captured definitions, and can also be looked up by either file's name.</p>
 *
 * @author NickAragua
 */
public class StratConFacilityFactory {
    private static final MMLogger logger = MMLogger.create(StratConFacilityFactory.class);

    // definition ID -> definition, in load order
    private static final Map<String, StratConFacilityDefinition> definitionsById = new HashMap<>();

    // every loaded definition, in load order
    private static final List<StratConFacilityDefinition> definitions = new ArrayList<>();

    // old-format file name (with and without extension) -> the definition it was merged into
    private static final Map<String, StratConFacilityDefinition> legacyAliases = new HashMap<>();

    static {
        reloadFacilities();
    }

    /**
     * Worker function that reloads all the facilities from disk
     */
    public static void reloadFacilities() {
        reloadFacilities(MHQConstants.STRAT_CON_FACILITY_MANIFEST,
              MHQConstants.STRAT_CON_USER_FACILITY_MANIFEST,
              MHQConstants.STRAT_CON_FACILITY_PATH);
    }

    /**
     * Test seam: reloads the facilities from an explicit manifest and directory.
     *
     * <p>The {@code data} directory is built when the application launches, so under test the default paths resolve to
     * nothing and the facility lists are left empty. That surfaces far downstream as a null facility rather than as a
     * missing file, so tests that place facilities must call this first - see {@code StratConTestData}.</p>
     *
     * @param manifestPath the facility manifest to read
     * @param facilityPath the directory holding the facility files the manifest names
     */
    public static void loadForTest(String manifestPath, String facilityPath) {
        reloadFacilities(manifestPath, null, facilityPath);
    }

    private static void reloadFacilities(String manifestPath, @Nullable String userManifestPath, String facilityPath) {
        definitionsById.clear();
        definitions.clear();
        legacyAliases.clear();

        StratConFacilityManifest facilityManifest = StratConFacilityManifest.deserialize(manifestPath);
        StratConFacilityManifest userManifest = (userManifestPath == null) ?
                                                      null :
                                                      StratConFacilityManifest.deserialize(userManifestPath);

        Map<String, LoadedFacilityDefinition> legacyFiles = new HashMap<>();
        List<String> legacyFileOrder = new ArrayList<>();
        if (facilityManifest != null) {
            loadFacilitiesFromManifest(facilityManifest, facilityPath, legacyFiles, legacyFileOrder);
        }

        if (userManifest != null) {
            loadFacilitiesFromManifest(userManifest, facilityPath, legacyFiles, legacyFileOrder);
        }

        mergeLegacyFiles(legacyFiles, legacyFileOrder);
    }

    /**
     * Loads the definitions a manifest names. Current-format definitions are registered at once; old-format ones are
     * collected so the two sides of each type can be merged once every file is read.
     *
     * @param manifest        The manifest to process
     * @param facilityPath    the directory holding the facility files the manifest names
     * @param legacyFiles     collects old-format files by file name
     * @param legacyFileOrder collects old-format file names in load order
     */
    private static void loadFacilitiesFromManifest(StratConFacilityManifest manifest, String facilityPath,
          Map<String, LoadedFacilityDefinition> legacyFiles, List<String> legacyFileOrder) {
        for (String listedName : manifest.facilityFileNames) {
            if ((listedName == null) || listedName.isBlank()) {
                continue;
            }

            String fileName = listedName.trim();
            File inputFile = Paths.get(facilityPath, fileName).toFile();
            if (!inputFile.exists()) {
                logger.warn("Specified file {} does not exist", inputFile.getPath());
                continue;
            }

            try {
                LoadedFacilityDefinition loaded = StratConFacilityJson.fromFile(inputFile);
                if (loaded.isLegacyFormat()) {
                    legacyFiles.put(fileName, loaded);
                    legacyFileOrder.add(fileName);
                } else {
                    register(loaded.definition());
                }
            } catch (Exception e) {
                logger.error("Error loading file: {}", inputFile.getPath(), e);
            }
        }
    }

    /**
     * Merges old-format files in pairs: an allied file and the hostile file it names as its captured definition (or
     * the other way round) become one definition carrying both profiles, under the allied file's ID. A file with no
     * such partner is registered on its own, with its one profile.
     *
     * @param legacyFiles     old-format files by file name
     * @param legacyFileOrder old-format file names in load order
     */
    private static void mergeLegacyFiles(Map<String, LoadedFacilityDefinition> legacyFiles,
          List<String> legacyFileOrder) {
        List<String> consumedFiles = new ArrayList<>();

        for (String fileName : legacyFileOrder) {
            if (consumedFiles.contains(fileName)) {
                continue;
            }
            consumedFiles.add(fileName);

            LoadedFacilityDefinition loaded = legacyFiles.get(fileName);
            StratConFacilityDefinition definition = loaded.definition();

            String partnerName = (loaded.capturedDefinition() == null) ? null : loaded.capturedDefinition().trim();
            LoadedFacilityDefinition partner = (partnerName == null) ? null : legacyFiles.get(partnerName);
            boolean isPartnerUsable = (partner != null)
                                            && !consumedFiles.contains(partnerName)
                                            && (definition.getAlliedProfile() != null)
                                                     != (partner.definition().getAlliedProfile() != null);

            if (isPartnerUsable) {
                consumedFiles.add(partnerName);
                definition = mergeSides(definition, partner.definition());
                addLegacyAlias(partnerName, definition);
            }

            addLegacyAlias(fileName, definition);
            register(definition);
        }
    }

    /**
     * @return one definition holding the allied profile of one side and the hostile profile of the other, named and
     *       identified after the allied side
     */
    private static StratConFacilityDefinition mergeSides(StratConFacilityDefinition first,
          StratConFacilityDefinition second) {
        StratConFacilityDefinition allied = (first.getAlliedProfile() != null) ? first : second;
        StratConFacilityDefinition hostile = (allied == first) ? second : first;

        StratConFacilityDefinition merged = new StratConFacilityDefinition(allied.getId(),
              allied.getDisplayableName(),
              allied.getFacilityType(),
              allied.getAlliedProfile(),
              hostile.getHostileProfile());
        merged.setBiomes(allied.getBiomes().isEmpty() ? hostile.getBiomes() : allied.getBiomes());
        return merged;
    }

    private static void addLegacyAlias(String fileName, StratConFacilityDefinition definition) {
        legacyAliases.put(fileName, definition);
        legacyAliases.put(StratConFacilityJson.idFromFileName(fileName), definition);
    }

    private static void register(StratConFacilityDefinition definition) {
        StratConFacilityDefinition replaced = definitionsById.put(definition.getId(), definition);
        if (replaced != null) {
            logger.warn("Facility definition {} is defined more than once; the last one loaded is used",
                  definition.getId());
            definitions.remove(replaced);
        }
        definitions.add(definition);
    }

    /**
     * Gets a definition by its ID, or by the file name of an old-format file merged into it.
     *
     * @param id the definition ID or old-format file name
     *
     * @return the definition, or {@code null} if none is loaded
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConFacilityDefinition getDefinition(@Nullable String id) {
        if (id == null) {
            return null;
        }

        StratConFacilityDefinition definition = definitionsById.get(id);
        return (definition != null) ? definition : legacyAliases.get(id);
    }

    /**
     * Finds the definition to use for a facility known only by its type, as in saves from before 0.51.01. A definition
     * with both profiles is preferred over a one-sided one.
     *
     * @param facilityType the facility type
     *
     * @return the definition, or {@code null} if none of that type is loaded
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConFacilityDefinition getDefinitionForType(FacilityType facilityType) {
        StratConFacilityDefinition oneSidedMatch = null;
        for (StratConFacilityDefinition definition : definitions) {
            if (definition.getFacilityType() != facilityType) {
                continue;
            }

            if ((definition.getAlliedProfile() != null) && (definition.getHostileProfile() != null)) {
                return definition;
            }

            if (oneSidedMatch == null) {
                oneSidedMatch = definition;
            }
        }
        return oneSidedMatch;
    }

    /**
     * @return every loaded definition, in load order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConFacilityDefinition> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    /**
     * @param owner the side that would hold the facility
     *
     * @return the loaded definitions with a profile for that side, in load order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConFacilityDefinition> getDefinitionsFor(ForceAlignment owner) {
        List<StratConFacilityDefinition> matchingDefinitions = new ArrayList<>();
        for (StratConFacilityDefinition definition : definitions) {
            if (definition.hasProfileFor(owner)) {
                matchingDefinitions.add(definition);
            }
        }
        return matchingDefinitions;
    }

    /**
     * @return a new facility of a random type, held by the enemy, or {@code null} if no type has a hostile profile
     */
    public static @Nullable StratConFacility getRandomHostileFacility() {
        return createRandomFacility(ForceAlignment.Opposing);
    }

    /**
     * @return a new facility of a random type, held by an ally, or {@code null} if no type has an allied profile
     */
    public static @Nullable StratConFacility getRandomAlliedFacility() {
        return createRandomFacility(ForceAlignment.Allied);
    }

    private static @Nullable StratConFacility createRandomFacility(ForceAlignment owner) {
        StratConFacilityDefinition definition = ObjectUtility.getRandomItem(getDefinitionsFor(owner));
        if (definition == null) {
            logger.error("No facility definition has a profile for {}; is the facility data loaded?", owner);
            return null;
        }
        return new StratConFacility(definition, owner);
    }
}
