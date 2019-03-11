package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;

public class StratconTrackState {
    
    // a track has the following characteristics:
    // width/height
    // [future]: terrain information by coordinates
    // scenario information by coordinates
    // active facilities by coordinates
    private String displayableName;
    private int width;
    private int height;
    private Map<Coords, StratconFacility> facilities;
    private Map<Coords, StratconScenario> scenarios;
    private List<Integer> assignedForceIDs;
    private int scenarioOdds;
    
    public StratconTrackState() {
        facilities = new HashMap<>();
        scenarios = new HashMap<>();
        assignedForceIDs = new ArrayList<>();
    }
    
    public String getDisplayableName() {
        return displayableName;
    }
    
    public void setDisplayableName(String name) {
        displayableName = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Map<Coords, StratconFacility> getFacilities() {
        return facilities;
    }

    public void setFacilities(Map<Coords, StratconFacility> facilities) {
        this.facilities = facilities;
    }

    public Map<Coords, StratconScenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<Coords, StratconScenario> scenarios) {
        this.scenarios = scenarios;
    }
    
    public StratconScenario getScenario(Coords coords) {
        return scenarios.get(coords);
    }
    
    public void generateScenarios(Campaign campaign, AtBContract contract) {
        switch(contract.getCommandRights()) {
        case AtBContract.COM_INTEGRATED:
            generateFixedScenarios(campaign, contract, true);
            break;
        case AtBContract.COM_HOUSE:
            generateFixedScenarios(campaign, contract, false);
            break;
        }
    }
    
    private void generateFixedScenarios(Campaign campaign, AtBContract contract, boolean autoAssignLances) {
        // get coordinates
        int x = Compute.randomInt(width);
        int y = Compute.randomInt(height);
        
        // create scenario for each force if we roll higher than the track's scenario odds
        // if we already have a scenario in the given coords, let's make it a larger battle instead
        // otherwise, generate an appropriate scenario for the unit type of the force being examined
        for(int forceID : assignedForceIDs) {
            if(Compute.randomInt(100) > scenarioOdds) {
                Coords scenarioCoords = new Coords(x, y);
                
                if(scenarios.containsKey(scenarioCoords)) {
                    scenarios.get(scenarioCoords).incrementRequiredPlayerLances();
                    continue;
                }
                
                StratconScenario scenario = new StratconScenario();
                scenario.initializeScenario(campaign, contract, campaign.getForce(forceID).getPrimaryUnitType(campaign));
                
                scenarios.put(scenarioCoords, scenario);
            }
        }
    }
}
