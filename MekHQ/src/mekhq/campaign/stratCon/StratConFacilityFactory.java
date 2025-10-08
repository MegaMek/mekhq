/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;

/**
 * This class handles functionality related to loading and StratCon facility definitions.
 *
 * @author NickAragua
 */
public class StratConFacilityFactory {
    private static final MMLogger logger = MMLogger.create(StratConFacilityFactory.class);

    // loaded facility definitions

    // map of filename -> facility definition, for specific facility retrieval
    private static final Map<String, StratConFacility> stratconFacilityMap = new HashMap<>();

    // list of all loaded facility definitions
    private static final List<StratConFacility> stratConFacilityList = new ArrayList<>();

    // list of all hostile facility defs for convenience
    private static final List<StratConFacility> hostileFacilities = new ArrayList<>();

    // list of all allied facility defs for convenience
    private static final List<StratConFacility> alliedFacilities = new ArrayList<>();

    static {
        reloadFacilities();
    }

    /**
     * Worker function that reloads all the facilities from disk
     */
    public static void reloadFacilities() {
        stratConFacilityList.clear();
        hostileFacilities.clear();
        alliedFacilities.clear();
        stratconFacilityMap.clear();

        // load dynamic scenarios
        StratConFacilityManifest facilityManifest = StratConFacilityManifest
                                                          .deserialize(MHQConstants.STRAT_CON_FACILITY_MANIFEST);

        // load user-specified scenario list
        StratConFacilityManifest userManifest = StratConFacilityManifest
                                                      .deserialize(MHQConstants.STRAT_CON_USER_FACILITY_MANIFEST);

        if (facilityManifest != null) {
            loadFacilitiesFromManifest(facilityManifest);
        }

        if (userManifest != null) {
            loadFacilitiesFromManifest(userManifest);
        }
    }

    /**
     * Helper function that loads scenario templates from the given manifest.
     *
     * @param manifest The manifest to process
     */
    private static void loadFacilitiesFromManifest(StratConFacilityManifest manifest) {
        if (manifest == null) {
            return;
        }

        for (String fileName : manifest.facilityFileNames) {
            String filePath = Paths.get(MHQConstants.STRAT_CON_FACILITY_PATH, fileName.trim()).toString();

            try {
                StratConFacility facility = StratConFacility.deserialize(filePath);

                if (facility != null) {
                    stratConFacilityList.add(facility);
                    stratconFacilityMap.put(fileName.trim(), facility);

                    if (facility.getOwner() == ForceAlignment.Allied) {
                        alliedFacilities.add(facility);
                    } else {
                        hostileFacilities.add(facility);
                    }
                }
            } catch (Exception e) {
                logger.error("Error loading file: {}", filePath, e);
            }
        }
    }

    /**
     * Gets a specific facility given an "ID" (the file name). This method does not clone the facility and should not be
     * used to put one on the board
     */
    public static StratConFacility getFacilityByName(String name) {
        return stratconFacilityMap.get(name);
    }

    /**
     * Gets a clone of a specific facility given the "ID" (file name), null if it doesn't exist.
     */
    @Nullable
    public static StratConFacility getFacilityCloneByName(String name) {
        return stratconFacilityMap.containsKey(name) ? stratconFacilityMap.get(name).clone() : null;
    }

    /**
     * Retrieves a random facility
     */
    public static StratConFacility getRandomFacility() {
        return ObjectUtility.getRandomItem(stratConFacilityList).clone();
    }

    public static StratConFacility getRandomHostileFacility() {
        return ObjectUtility.getRandomItem(hostileFacilities).clone();
    }

    public static StratConFacility getRandomAlliedFacility() {
        return ObjectUtility.getRandomItem(alliedFacilities).clone();
    }

    public static List<StratConFacility> getHostileFacilities() {
        return Collections.unmodifiableList(hostileFacilities);
    }

    public static List<StratConFacility> getAlliedFacilities() {
        return Collections.unmodifiableList(alliedFacilities);
    }
}
