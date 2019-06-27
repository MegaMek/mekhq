package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Compute;
import megamek.common.UnitType;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.MekHQ;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioManifest;

/**
 * This class handles functionality related to loading and sorting scenario templates.
 * @author NickAragua
 *
 */
public class StratconScenarioFactory {
    // loaded dynamic scenario templates, sorted by location (ground, low atmosphere, space)
    private static Map<MapLocation, List<ScenarioTemplate>> dynamicScenarioLocationMap = new HashMap<>();
    private static Map<Integer, List<ScenarioTemplate>> dynamicScenarioUnitTypeMap = new HashMap<>();
    
    static {
        reloadScenarios();
    }
    
    public static void reloadScenarios() {
        dynamicScenarioLocationMap.clear();
        dynamicScenarioUnitTypeMap.clear();
        
        // load dynamic scenarios
        AtBScenarioManifest scenarioManifest = AtBScenarioManifest.Deserialize("./data/scenariotemplates/ScenarioManifest.xml");
        
        // load user-specified scenario list
        AtBScenarioManifest userManifest = AtBScenarioManifest.Deserialize("./data/scenariotemplates/UserScenarioManifest.xml");
        
        if(scenarioManifest != null) {
            loadScenariosFromManifest(scenarioManifest);
        }
        
        if(userManifest != null) {
            loadScenariosFromManifest(userManifest);
        }
    }
    
    /**
     * Helper function that loads scenario templates from the given manifest.
     * @param manifest The manifest to process
     */
    private static void loadScenariosFromManifest(AtBScenarioManifest manifest) {
        if(manifest == null) {
            return;
        }
        
        for(int key : manifest.scenarioFileNames.keySet()) {
            String filePath = String.format("./data/ScenarioTemplates/%s", manifest.scenarioFileNames.get(key).trim());
            
            try {
                ScenarioTemplate template = ScenarioTemplate.Deserialize(filePath);
                
                if(template != null) {
                    MapLocation locationKey = template.mapParameters.getMapLocation();
                    
                    // sort templates by location
                    if(!dynamicScenarioLocationMap.containsKey(locationKey)) {
                        dynamicScenarioLocationMap.put(locationKey, new ArrayList<>());
                    }
                    
                    dynamicScenarioLocationMap.get(locationKey).add(template);
                    
                    // sort templates by primary force unit type
                    int playerForceUnitType = template.getPrimaryPlayerForce().getAllowedUnitType();
                    if(!dynamicScenarioUnitTypeMap.containsKey(playerForceUnitType)) {
                        dynamicScenarioUnitTypeMap.put(playerForceUnitType, new ArrayList<>());
                    }
                    
                    dynamicScenarioUnitTypeMap.get(playerForceUnitType).add(template);
                }
            } catch(Exception e) {
                MekHQ.getLogger().error(StratconScenarioFactory.class, "loadScenariosFromManifest", 
                        String.format("Error loading file: %s", filePath), e);
            }
        }
    }
    
    /**
     * Retrieves a random scenario template in the appropriate location.
     * @param location The location (ground/low atmo/space) category of the scenario.
     * @return Random scenario template.
     */
    public static ScenarioTemplate getRandomScenario(MapLocation location) {
        int scenarioIndex = Compute.randomInt(dynamicScenarioLocationMap.get(location).size());
        return (ScenarioTemplate) dynamicScenarioLocationMap.get(location).get(scenarioIndex).clone();
    }
    
    /**
     * Retrieves a random scenario template appropriate for the given unit type.
     * @param location The desired unit type.
     * @return Random scenario template.
     */
    public static ScenarioTemplate getRandomScenario(int unitType) {
        int actualUnitType = unitType;
        
        // if the specific unit type doesn't have any scenario templates for it
        // then try a more generic unit type.
        if(!dynamicScenarioUnitTypeMap.containsKey(unitType)) {
            actualUnitType = convertSpecificUnitTypeToGeneral(unitType);
            
            if(!dynamicScenarioUnitTypeMap.containsKey(actualUnitType)) {            
                MekHQ.getLogger().warning(StratconScenarioFactory.class, "getRandomScenario", 
                        String.format("No scenarios configured for unit type %d", unitType));
                return null;
            }
        }
        
        int scenarioIndex = 2;//Compute.randomInt(dynamicScenarioUnitTypeMap.get(actualUnitType).size());
        return (ScenarioTemplate) dynamicScenarioUnitTypeMap.get(actualUnitType).get(scenarioIndex).clone();
    }
    
    /**
     * Converts a specific unit type (AERO, MEK, etc) to a generic unit type (ATB_MIX, ATB_AERO_MIX)
     * @param unitType The unit type to convert.
     * @return Generic unit type.
     */
    private static int convertSpecificUnitTypeToGeneral(int unitType) {
        switch(unitType)
        {
            case UnitType.AERO:
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
