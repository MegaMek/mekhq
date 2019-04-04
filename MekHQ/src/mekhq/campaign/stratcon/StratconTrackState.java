package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Set<Integer> assignedForceIDs;
    private int scenarioOdds;
    private int deploymentTime;
    private int requiredLanceCount;
    
    public StratconTrackState() {
        facilities = new HashMap<>();
        scenarios = new HashMap<>();
        setAssignedForceIDs(new HashSet<>());
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

    public StratconFacility getFacility(Coords coords) {
        return facilities.get(coords);
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
    
    public int getRequiredLanceCount() {
        return requiredLanceCount;
    }

    public void setRequiredLanceCount(int requiredLanceCount) {
        this.requiredLanceCount = requiredLanceCount;
    }
    
    public int getDeploymentTime() {
        return deploymentTime;
    }

    public void setDeploymentTime(int deploymentTime) {
        this.deploymentTime = deploymentTime;
    }

    public void assignForce(int forceID) {
        getAssignedForceIDs().add(forceID);
    }
    
    public void unassignForce(int forceID) {
        getAssignedForceIDs().remove(forceID);
    }
    
    /**
     * Returns the set of all force IDs assigned to this track, regardless of scenario deployment status
     */
    public Set<Integer> getAssignedForceIDs() {
        return assignedForceIDs;
    }

    public void setAssignedForceIDs(Set<Integer> assignedForceIDs) {
        this.assignedForceIDs = assignedForceIDs;
    }
    
    public void addFacility(Coords coords, StratconFacility facility) {
        facilities.put(coords, facility);
    }
    
    public void removeFacility(Coords coords) {
        facilities.remove(coords);
    }
    
    /**
     * Returns the set of all force IDs for forces assigned to this track
     * that have not been assigned to a scenario already.
     * @return
     */
    public List<Integer> getAvailableForceIDs() {
        List<Integer> retVal = new ArrayList<>();
        
        for(int forceID : assignedForceIDs) {
            boolean forceAssigned = false;
            
            for(StratconScenario scenario : scenarios.values()) {
                if(scenario.getAssignedForces().contains(forceID)) {
                    forceAssigned = true;
                    break;
                }
            }
            
            if(!forceAssigned) {
                retVal.add(forceID);
            }
        }
        
        return retVal;
    }
    
    public void generateScenarios(Campaign campaign, AtBContract contract) {
        List<StratconScenario> generatedScenarios = new ArrayList<>();
        boolean autoAssignLances = contract.getCommandRights() == AtBContract.COM_INTEGRATED;
        
        //StratconScenarioFactory.reloadScenarios();
        
        // create scenario for each force if we roll higher than the track's scenario odds
        // if we already have a scenario in the given coords, let's make it a larger battle instead
        // otherwise, generate an appropriate scenario for the unit type of the force being examined
        for(int forceID : getAssignedForceIDs()) {
            if(Compute.randomInt(100) > scenarioOdds) {
                // get coordinates
                int x = Compute.randomInt(width);
                int y = Compute.randomInt(height);                
                
                Coords scenarioCoords = new Coords(x, y);
                
                if(scenarios.containsKey(scenarioCoords)) {
                    scenarios.get(scenarioCoords).incrementRequiredPlayerLances();
                    // if under integrated command, automatically assign the lance to the scenario
                    if(autoAssignLances) {
                        scenarios.get(scenarioCoords).addPrimaryForce(forceID);
                    }
                    continue;
                }
                
                StratconScenario scenario = new StratconScenario();
                scenario.initializeScenario(campaign, contract, campaign.getForce(forceID).getPrimaryUnitType(campaign));
                
                scenarios.put(scenarioCoords, scenario);
                generatedScenarios.add(scenario);
                
                // if under integrated command, automatically assign the lance to the scenario
                if(autoAssignLances) {
                    scenario.addPrimaryForce(forceID);
                }
            }
        }
        
        // if under integrated command, automatically assign the lance to the scenario
        if(autoAssignLances) {
            for(StratconScenario scenario : generatedScenarios) {
                scenario.commitPrimaryForces(campaign, contract);
            }
        }
        
        // if under liaison command, pick a random scenario from the ones generated
        // to set as required and attach liaison
        if(contract.getCommandRights() == AtBContract.COM_HOUSE) {
            int scenarioIndex = Compute.randomInt(generatedScenarios.size() - 1);
            generatedScenarios.get(scenarioIndex).setRequiredScenario(true);
            generatedScenarios.get(scenarioIndex).setAttachedUnitsModifier(contract);
        }
    }
}
