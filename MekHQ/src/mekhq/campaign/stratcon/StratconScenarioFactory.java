package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Compute;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
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
        // load dynamic scenarios
        AtBScenarioManifest scenarioManifest = AtBScenarioManifest.Deserialize("./data/scenariotemplates/LegacyAtB/ScenarioManifest.xml");
        
        // load user-specified scenario list
        AtBScenarioManifest userManifest = AtBScenarioManifest.Deserialize("./data/scenariotemplates/LegacyAtB/UserScenarioManifest.xml");
        
        loadScenariosFromManifest(scenarioManifest);
        loadScenariosFromManifest(userManifest);
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
            String filePath = String.format("./data/ScenarioTemplates/LegacyAtB/%s", manifest.scenarioFileNames.get(key).trim());
            
            ScenarioTemplate template = ScenarioTemplate.Deserialize(filePath);
            
            if(template != null) {
                // sort templates by location
                if(!dynamicScenarioLocationMap.containsKey(template.mapParameters.getMapLocation())) {
                    dynamicScenarioLocationMap.put(template.mapParameters.getMapLocation(), new ArrayList<>());
                }
                
                dynamicScenarioLocationMap.get(key).add(template);
                
                // sort templates by primary force unit type
                int playerForceUnitType = template.getPrimaryPlayerForce().getAllowedUnitType();
                if(!dynamicScenarioUnitTypeMap.containsKey(playerForceUnitType)) {
                    dynamicScenarioUnitTypeMap.put(playerForceUnitType, new ArrayList<>());
                }
                
                dynamicScenarioUnitTypeMap.get(playerForceUnitType).add(template);
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
        return dynamicScenarioLocationMap.get(location).get(scenarioIndex);
    }
    
    /**
     * Retrieves a random scenario template appropriate for the given unit type.
     * @param location The desired unit type.
     * @return Random scenario template.
     */
    public static ScenarioTemplate getRandomScenario(int unitType) {
        int scenarioIndex = Compute.randomInt(dynamicScenarioUnitTypeMap.get(unitType).size());
        return dynamicScenarioUnitTypeMap.get(unitType).get(scenarioIndex);
    }
}
