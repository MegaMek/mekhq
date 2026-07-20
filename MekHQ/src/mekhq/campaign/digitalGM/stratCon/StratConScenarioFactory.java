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
package mekhq.campaign.digitalGM.stratCon;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioManifest;

/**
 * This class handles functionality related to loading and sorting scenario templates.
 *
 * @author NickAragua
 */
public class StratConScenarioFactory {
    private static final MMLogger logger = MMLogger.create(StratConScenarioFactory.class);
    // loaded dynamic scenario templates, sorted by location (ground, low
    // atmosphere, space)
    private static final Map<MapLocation, List<ScenarioTemplate>> dynamicScenarioLocationMap = new HashMap<>();
    private static final Map<Integer, List<ScenarioTemplate>> dynamicScenarioUnitTypeMap = new HashMap<>();
    private static final Map<String, ScenarioTemplate> dynamicScenarioNameMap = new HashMap<>();

    static {
        reloadScenarios();
    }

    /**
     * Reload the dynamic scenarios.
     */
    public static void reloadScenarios() {
        dynamicScenarioLocationMap.clear();
        dynamicScenarioUnitTypeMap.clear();

        // load dynamic scenarios
        AtBScenarioManifest scenarioManifest = AtBScenarioManifest.Deserialize(MHQConstants.STRAT_CON_SCENARIO_MANIFEST);

        // load user-specified scenario list
        AtBScenarioManifest userManifest = AtBScenarioManifest
                                                 .Deserialize(MHQConstants.STRAT_CON_USER_SCENARIO_MANIFEST);

        if (scenarioManifest != null) {
            loadScenariosFromManifest(scenarioManifest);
        }

        if (userManifest != null) {
            loadScenariosFromManifest(userManifest);
        }
    }

    /**
     * Helper function that loads scenario templates from the given manifest.
     *
     * @param manifest The manifest to process
     */
    private static void loadScenariosFromManifest(AtBScenarioManifest manifest) {
        if (manifest == null) {
            return;
        }

        for (int key : manifest.scenarioFileNames.keySet()) {
            String fileName = manifest.scenarioFileNames.get(key).trim();
            String filePath = Paths.get(MHQConstants.STRAT_CON_SCENARIO_TEMPLATE_PATH,
                  manifest.scenarioFileNames.get(key).trim()).toString();

            try {
                ScenarioTemplate template = ScenarioTemplate.Deserialize(filePath);

                if (template != null) {
                    MapLocation locationKey = template.mapParameters.getMapLocation();

                    // sort templates by location
                    if (!dynamicScenarioLocationMap.containsKey(locationKey)) {
                        dynamicScenarioLocationMap.put(locationKey, new ArrayList<>());
                    }

                    dynamicScenarioLocationMap.get(locationKey).add(template);

                    // sort templates by primary force unit type
                    int playerForceUnitType = template.getPrimaryPlayerForce().getAllowedUnitType();
                    if (!dynamicScenarioUnitTypeMap.containsKey(playerForceUnitType)) {
                        dynamicScenarioUnitTypeMap.put(playerForceUnitType, new ArrayList<>());
                    }

                    dynamicScenarioUnitTypeMap.get(playerForceUnitType).add(template);

                    dynamicScenarioNameMap.put(fileName, template);
                }
            } catch (Exception e) {
                logger.error("Error loading file: {}", filePath, e);
            }
        }
    }

    /**
     * Retrieves a random scenario template in the appropriate location.
     *
     * @param location The location (ground/low atmosphere/space) category of the scenario.
     *
     * @return Random scenario template.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static ScenarioTemplate getRandomScenario(MapLocation location) {
        return ObjectUtility.getRandomItem(dynamicScenarioLocationMap.get(location)).clone();
    }

    /**
     * Retrieves a specific scenario given the key (file name)
     */
    public static @Nullable ScenarioTemplate getSpecificScenario(String name) {
        ScenarioTemplate template = dynamicScenarioNameMap.get(name);
        if (template == null) {
            logger.error("Scenario template {} not found.", name);
            return null;
        }

        return template.clone();
    }

    /**
     * Retrieves a random scenario template based on the given unit type and additional parameters.
     *
     * <p>Filters applicable scenarios and randomly selects a viable option, ensuring that facility scenarios
     * are excluded to prevent facility overpopulation.</p>
     *
     * @param unitType        The specific unit type that the scenario should be associated with.
     * @param isAmbushed      A boolean flag indicating whether the unit is ambushed.
     * @param isBungledPatrol A boolean flag indicating whether the scenario involves a bungled patrol.
     *
     * @return A randomly selected {@code ScenarioTemplate} that fits the specified criteria, or {@code null} if no
     *       suitable scenarios are configured for the unit type.
     */
    public static @Nullable ScenarioTemplate getRandomScenario(int unitType, boolean isAmbushed,
          boolean isBungledPatrol) {
        int generalUnitType = convertSpecificUnitTypeToGeneral(unitType);

        // if the specific unit type doesn't have any scenario templates for it
        // then we can't generate a scenario.
        if (!dynamicScenarioUnitTypeMap.containsKey(unitType) &&
                  !dynamicScenarioUnitTypeMap.containsKey(generalUnitType)) {
            logger.warn("No scenarios configured for unit type {}", unitType);
            return null;
        }

        Set<ScenarioTemplate> jointList = new HashSet<>();

        if (dynamicScenarioUnitTypeMap.containsKey(unitType)) {
            getViableScenarioTemplates(unitType, isAmbushed, isBungledPatrol, jointList);
        }

        if (dynamicScenarioUnitTypeMap.containsKey(generalUnitType)) {
            getViableScenarioTemplates(generalUnitType, isAmbushed, isBungledPatrol, jointList);
        }

        // We don't want facilities spawning mid-contract; this stops facility count getting out of control
        jointList.removeIf(ScenarioTemplate::isFacilityScenario);

        if (jointList.isEmpty()) {
            logger.warn("No scenarios configured for unit type {}, ({}) and ambushed status {}", unitType,
                  generalUnitType, isAmbushed);
            return null;
        }

        return ObjectUtility.getRandomItem(jointList).clone();
    }

    /**
     * Filters and collects viable scenario templates based on the specified unit type and additional parameters, such
     * as ambush or bungled patrol conditions, and adds them to the provided set of scenario templates.
     *
     * @param unitType        The specific unit type that the scenarios should be associated with.
     * @param isAmbushed      A boolean flag indicating whether the scenarios should be suitable for ambushes.
     * @param isBungledPatrol A boolean flag indicating whether the scenarios should be suitable for bungled patrols.
     * @param jointList       A set to which viable scenario templates will be added.
     */
    private static void getViableScenarioTemplates(int unitType, boolean isAmbushed, boolean isBungledPatrol,
          Set<ScenarioTemplate> jointList) {
        if (!isAmbushed && !isBungledPatrol) {
            jointList.addAll(dynamicScenarioUnitTypeMap.get(unitType));
        }

        for (ScenarioTemplate template : dynamicScenarioUnitTypeMap.get(unitType)) {
            // If bungled patrol is true, so is isAmbushed so we need to parse isBungledPatrol first
            if (template.isSuitedForBungledPatrols() && isBungledPatrol) {
                jointList.add(template);
                continue;
            }

            if (template.isSuitedForAmbushes() && isAmbushed) {
                jointList.add(template);
            }
        }
    }

    /**
     * Get an allied or hostile facility scenario, depending on passed on parameter.
     */
    public static @Nullable ScenarioTemplate getFacilityScenario(boolean allied) {
        if (allied) {
            return getSpecificScenario(MHQConstants.ALLIED_FACILITY_SCENARIO);
        } else {
            return getSpecificScenario(MHQConstants.HOSTILE_FACILITY_SCENARIO);
        }
    }

    /**
     * Converts a specific unit type (AERO, MEK, etc.) to a generic unit type (ATB_MIX, ATB_AERO_MIX)
     *
     * @param unitType The unit type to convert.
     *
     * @return Generic unit type.
     */
    public static int convertSpecificUnitTypeToGeneral(int unitType) {
        return switch (unitType) {
            case UnitType.AERO,
                 UnitType.AEROSPACE_FIGHTER,
                 UnitType.CONV_FIGHTER,
                 UnitType.DROPSHIP,
                 UnitType.JUMPSHIP,
                 UnitType.WARSHIP,
                 UnitType.SMALL_CRAFT,
                 UnitType.SPACE_STATION -> ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX;
            default -> ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX;
        };
    }
}
