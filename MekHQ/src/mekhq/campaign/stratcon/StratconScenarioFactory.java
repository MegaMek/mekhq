/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.campaign.stratcon;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.UnitType;
import mekhq.MHQConstants;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioManifest;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles functionality related to loading and sorting scenario templates.
 * @author NickAragua
 */
public class StratconScenarioFactory {
    // loaded dynamic scenario templates, sorted by location (ground, low atmosphere, space)
    private static Map<MapLocation, List<ScenarioTemplate>> dynamicScenarioLocationMap = new HashMap<>();
    private static Map<Integer, List<ScenarioTemplate>> dynamicScenarioUnitTypeMap = new HashMap<>();
    private static Map<String, ScenarioTemplate> dynamicScenarioNameMap = new HashMap<>();

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
        AtBScenarioManifest scenarioManifest = AtBScenarioManifest.Deserialize(MHQConstants.STRATCON_SCENARIO_MANIFEST);

        // load user-specified scenario list
        AtBScenarioManifest userManifest = AtBScenarioManifest.Deserialize(MHQConstants.STRATCON_USER_SCENARIO_MANIFEST);

        if (scenarioManifest != null) {
            loadScenariosFromManifest(scenarioManifest);
        }

        if (userManifest != null) {
            loadScenariosFromManifest(userManifest);
        }
    }

    /**
     * Helper function that loads scenario templates from the given manifest.
     * @param manifest The manifest to process
     */
    private static void loadScenariosFromManifest(AtBScenarioManifest manifest) {
        if (manifest == null) {
            return;
        }

        for (int key : manifest.scenarioFileNames.keySet()) {
            String fileName = manifest.scenarioFileNames.get(key).trim();
            String filePath = Paths.get(MHQConstants.STRATCON_SCENARIO_TEMPLATE_PATH,
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
                LogManager.getLogger().error(String.format("Error loading file: %s", filePath), e);
            }
        }
    }

    /**
     * Retrieves a random scenario template in the appropriate location.
     * @param location The location (ground/low atmo/space) category of the scenario.
     * @return Random scenario template.
     */
    public static ScenarioTemplate getRandomScenario(MapLocation location) {
        return ObjectUtility.getRandomItem(dynamicScenarioLocationMap.get(location)).clone();
    }

    /**
     * Retrieves a specific scenario given the key (file name)
     */
    public static ScenarioTemplate getSpecificScenario(String name) {
        return dynamicScenarioNameMap.get(name).clone();
    }

    /**
     * Retrieves a random scenario template appropriate for the given unit type.
     * This includes the more general ATB_MIX and ATB_AERO_MIX where appropriate
     * @param unitType The desired unit type, as per megamek.common.UnitType
     * @return Random scenario template.
     */
    public static ScenarioTemplate getRandomScenario(int unitType) {
        int generalUnitType = convertSpecificUnitTypeToGeneral(unitType);

        // if the specific unit type doesn't have any scenario templates for it
        // then we can't generate a scenario.
        if (!dynamicScenarioUnitTypeMap.containsKey(unitType) &&
                !dynamicScenarioUnitTypeMap.containsKey(generalUnitType)) {
            LogManager.getLogger().warn(String.format("No scenarios configured for unit type %d", unitType));
            return null;
        }

        List<ScenarioTemplate> jointList = new ArrayList<>();

        if (dynamicScenarioUnitTypeMap.containsKey(unitType)) {
            jointList.addAll(dynamicScenarioUnitTypeMap.get(unitType));
        }

        if (dynamicScenarioUnitTypeMap.containsKey(generalUnitType)) {
            jointList.addAll(dynamicScenarioUnitTypeMap.get(generalUnitType));
        }

        return ObjectUtility.getRandomItem(jointList).clone();
    }

    /**
     * Get an allied or hostile facility scenario, depending on passed on parameter.
     */
    public static ScenarioTemplate getFacilityScenario(boolean allied) {
        if (allied) {
            return getSpecificScenario(MHQConstants.ALLIED_FACILITY_SCENARIO);
        } else {
            return getSpecificScenario(MHQConstants.HOSTILE_FACILITY_SCENARIO);
        }
    }

    /**
     * Converts a specific unit type (AERO, MEK, etc) to a generic unit type (ATB_MIX, ATB_AERO_MIX)
     * @param unitType The unit type to convert.
     * @return Generic unit type.
     */
    public static int convertSpecificUnitTypeToGeneral(int unitType) {
        switch (unitType) {
            case UnitType.AERO:
            case UnitType.AEROSPACEFIGHTER:
            case UnitType.CONV_FIGHTER:
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.WARSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.SPACE_STATION:
                return ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX;
            default:
                return ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX;
        }
    }
}
