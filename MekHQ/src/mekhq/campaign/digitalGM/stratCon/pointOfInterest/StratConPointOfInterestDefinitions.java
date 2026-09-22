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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;

/**
 * The library of StratCon point of interest definitions, keyed by type ID.
 *
 * <p>Definitions come from two sources:</p>
 * <ul>
 *     <li><b>Data files</b> - the JSON files named by the point of interest manifest and the optional user manifest.
 *     These are cleared and re-read by {@link #reloadDefinitions()}.</li>
 *     <li><b>Code</b> - definitions handed to {@link #registerDefinition} by code outside StratCon. These survive a
 *     reload, and take precedence over a data file definition with the same type ID.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConPointOfInterestDefinitions {
    private static final MMLogger LOGGER = MMLogger.create(StratConPointOfInterestDefinitions.class);

    // insertion-ordered so menus listing the definitions keep the manifest's order
    private static final Map<String, StratConPointOfInterestDefinition> fileDefinitions = new LinkedHashMap<>();
    private static final Map<String, StratConPointOfInterestDefinition> registeredDefinitions = new LinkedHashMap<>();

    static {
        reloadDefinitions();
    }

    private StratConPointOfInterestDefinitions() {
    }

    /**
     * Clears and re-reads every data file definition. Definitions registered from code are kept.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void reloadDefinitions() {
        reloadDefinitions(MHQConstants.STRAT_CON_POINT_OF_INTEREST_MANIFEST,
              MHQConstants.STRAT_CON_USER_POINT_OF_INTEREST_MANIFEST,
              MHQConstants.STRAT_CON_POINT_OF_INTEREST_PATH);
    }

    /**
     * Test seam: reloads the data file definitions from an explicit manifest and directory.
     *
     * <p>The {@code data} directory is built when the application launches, so under test the default paths resolve
     * to nothing and no definitions are loaded. Tests relying on data file definitions must call this first.</p>
     *
     * @param manifestPath       the point of interest manifest to read
     * @param pointOfInterestPath the directory holding the definition files the manifest names
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void loadForTest(String manifestPath, String pointOfInterestPath) {
        reloadDefinitions(manifestPath, null, pointOfInterestPath);
    }

    private static void reloadDefinitions(String manifestPath, @Nullable String userManifestPath,
          String pointOfInterestPath) {
        fileDefinitions.clear();

        StratConPointOfInterestManifest manifest = StratConPointOfInterestManifest.deserialize(manifestPath);
        if (manifest != null) {
            loadDefinitionsFromManifest(manifest, pointOfInterestPath);
        }

        if (userManifestPath != null) {
            StratConPointOfInterestManifest userManifest = StratConPointOfInterestManifest.deserialize(
                  userManifestPath);
            if (userManifest != null) {
                loadDefinitionsFromManifest(userManifest, pointOfInterestPath);
            }
        }
    }

    private static void loadDefinitionsFromManifest(StratConPointOfInterestManifest manifest,
          String pointOfInterestPath) {
        for (String fileName : manifest.pointOfInterestFileNames) {
            String filePath = Paths.get(pointOfInterestPath, fileName.trim()).toString();
            StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinition.deserialize(filePath);

            if (definition == null) {
                continue;
            }

            if (isBlank(definition.getTypeId())) {
                LOGGER.warn("Point of interest definition {} has no type ID; skipping it.", filePath);
                continue;
            }

            if (fileDefinitions.containsKey(definition.getTypeId())) {
                LOGGER.warn("Point of interest definition {} reuses type ID {}; it replaces the earlier definition.",
                      filePath,
                      definition.getTypeId());
            }

            fileDefinitions.put(definition.getTypeId(), definition);
        }
    }

    /**
     * Registers a definition from code, for points of interest defined outside the data files. A registered
     * definition survives {@link #reloadDefinitions()} and takes precedence over a data file definition with the same
     * type ID. Registering a second definition with the same type ID replaces the first.
     *
     * @param definition the definition to register; ignored (and logged) if it has no type ID
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void registerDefinition(StratConPointOfInterestDefinition definition) {
        if ((definition == null) || isBlank(definition.getTypeId())) {
            LOGGER.warn("Ignoring a point of interest definition registered without a type ID.");
            return;
        }

        if (fileDefinitions.containsKey(definition.getTypeId())) {
            LOGGER.warn("Registered point of interest definition {} overrides the data file definition.",
                  definition.getTypeId());
        }

        registeredDefinitions.put(definition.getTypeId(), definition);
    }

    /**
     * Removes a definition previously registered from code. Data file definitions are unaffected.
     *
     * @param typeId the type ID of the registered definition to remove
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void unregisterDefinition(String typeId) {
        registeredDefinitions.remove(typeId);
    }

    /**
     * @param typeId the type ID to look up
     *
     * @return the definition for that type, preferring one registered from code, or {@code null} if there is none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable StratConPointOfInterestDefinition getDefinition(@Nullable String typeId) {
        if (typeId == null) {
            return null;
        }

        StratConPointOfInterestDefinition registeredDefinition = registeredDefinitions.get(typeId);
        if (registeredDefinition != null) {
            return registeredDefinition;
        }

        return fileDefinitions.get(typeId);
    }

    /**
     * @return every known definition, one per type ID: data file definitions in manifest order, then those registered
     *       from code. A registered definition replaces a data file definition with the same type ID.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<StratConPointOfInterestDefinition> getAllDefinitions() {
        List<StratConPointOfInterestDefinition> allDefinitions = new ArrayList<>();

        for (StratConPointOfInterestDefinition fileDefinition : fileDefinitions.values()) {
            if (!registeredDefinitions.containsKey(fileDefinition.getTypeId())) {
                allDefinitions.add(fileDefinition);
            }
        }

        allDefinitions.addAll(registeredDefinitions.values());
        return Collections.unmodifiableList(allDefinitions);
    }

    private static boolean isBlank(@Nullable String value) {
        return (value == null) || value.isBlank();
    }
}
