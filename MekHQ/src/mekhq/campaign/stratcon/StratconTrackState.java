package mekhq.campaign.stratcon;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;


/**
 * Track-level state object for a stratcon campaign.
 * @author NickAragua
 *
 */
@XmlRootElement(name="campaignTrack")
public class StratconTrackState {
    public static final String ROOT_XML_ELEMENT_NAME = "StratconTrackState";
    
    // a track has the following characteristics:
    // width/height
    // [future]: terrain information by coordinates
    // scenario information by coordinates
    // active facilities by coordinates
    private String displayableName;
    private int width;
    private int height;
    private boolean gmRevealed;
    
    private Map<StratconCoords, StratconFacility> facilities;   
    private Map<StratconCoords, StratconScenario> scenarios;    
    private Map<StratconCoords, Integer> assignedCoordForces; 
    private Map<Integer, StratconCoords> assignedForceCoords;
    private Map<Integer, LocalDate> assignedForceReturnDates;
    private Map<Integer, String> assignedForceReturnDatesForStorage;
    private Set<StratconCoords> revealedCoords;

    // don't serialize this
    private transient Map<Integer, StratconScenario> backingScenarioMap;
    
    private int scenarioOdds;
    private int deploymentTime;
    private int requiredLanceCount;
    
    public StratconTrackState() {
        facilities = new HashMap<>();
        scenarios = new HashMap<>();
        assignedForceCoords = new HashMap<>();
        assignedForceReturnDates = new HashMap<>();
        assignedCoordForces = new HashMap<>();
        setAssignedForceReturnDatesForStorage(new HashMap<>());
        revealedCoords = new HashSet<>();
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

    @XmlElementWrapper(name="trackFacilities")
    @XmlElement(name="facility")
    public Map<StratconCoords, StratconFacility> getFacilities() {
        return facilities;
    }

    public void setFacilities(Map<StratconCoords, StratconFacility> facilities) {
        this.facilities = facilities;
    }

    public StratconFacility getFacility(StratconCoords coords) {
        return facilities.get(coords);
    }
    
    /**
     * Used for serialization/deserialization.
     * Do not manipulate directly, or things get unpleasant.
     * @return
     */
    @XmlElementWrapper(name="trackScenarios")
    @XmlElement(name="scenario")
    public Map<StratconCoords, StratconScenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<StratconCoords, StratconScenario> scenarios) {
        this.scenarios = scenarios;
    }
    
    /**
     * Adds a StratconScenario to this track. Assumes it already has some coordinates assigned,
     * and a valid campaign scenario ID for its backing AtB scenario
     */
    public void addScenario(StratconScenario scenario) {
        scenarios.put(scenario.getCoords(), scenario);
        
        updateScenario(scenario);
    }
    
    /**
     * Updates an existing scenario on this track.
     */
    public void updateScenario(StratconScenario scenario) {
        if(scenarios.containsKey(scenario.getCoords()) && (scenario.getBackingScenarioID() > 0)) {
            getBackingScenariosMap().put(scenario.getBackingScenarioID(), scenario);
        }
    }
    
    /**
     * Removes a StratconScenario from this track.
     */
    public void removeScenario(StratconScenario scenario) {
        scenarios.remove(scenario.getCoords());
        getBackingScenariosMap().remove(scenario.getBackingScenarioID());
    }
    
    public StratconScenario getScenario(StratconCoords coords) {
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

    public int getScenarioOdds() {
        return scenarioOdds;
    }

    public void setScenarioOdds(int scenarioOdds) {
        this.scenarioOdds = scenarioOdds;
    }

    public boolean isGmRevealed() {
        return gmRevealed;
    }

    public void setGmRevealed(boolean gmRevealed) {
        this.gmRevealed = gmRevealed;
    }

    public void assignForce(int forceID, StratconCoords coords, LocalDate date) {
        assignedForceCoords.put(forceID, coords);
        assignedForceReturnDates.put(forceID, date.plusDays(deploymentTime));
        assignedCoordForces.put(coords, forceID);
        getAssignedForceReturnDatesForStorage().put(forceID, date.plusDays(deploymentTime).toString());
    }
    
    public void unassignForce(int forceID) {
        assignedCoordForces.remove(assignedForceCoords.get(forceID));
        assignedForceCoords.remove(forceID);
        assignedForceReturnDates.remove(forceID);
        getAssignedForceReturnDatesForStorage().remove(forceID);
    }
    
    public void restoreReturnDates() {
        for (int forceID : getAssignedForceReturnDatesForStorage().keySet()) {
            assignedForceReturnDates.put(forceID, MekHqXmlUtil.parseDate(getAssignedForceReturnDatesForStorage().get(forceID)));
        }
    }
    
    public Map<Integer, StratconCoords> getAssignedForceCoords() {
        return assignedForceCoords;
    }

    public void setAssignedForceCoords(Map<Integer, StratconCoords> assignedForceCoords) {
        this.assignedForceCoords = assignedForceCoords;
    }
    
    public Map<StratconCoords, Integer> getAssignedCoordForces() {
        return assignedCoordForces;
    }

    public void setAssignedCoordForces(Map<StratconCoords, Integer> assignedCoordForces) {
        this.assignedCoordForces = assignedCoordForces;
    }

    @XmlTransient
    public Map<Integer, LocalDate> getAssignedForceReturnDates() {
        return assignedForceReturnDates;
    }

    public void setAssignedForceReturnDates(Map<Integer, LocalDate> assignedForceReturnDates) {
        this.assignedForceReturnDates = assignedForceReturnDates;
    }

    public Map<Integer, String> getAssignedForceReturnDatesForStorage() {
        return assignedForceReturnDatesForStorage;
    }

    public void setAssignedForceReturnDatesForStorage(Map<Integer, String> assignedForceReturnDatesForStorage) {
        this.assignedForceReturnDatesForStorage = assignedForceReturnDatesForStorage;
    }

    public boolean coordsRevealed(int x, int y) {
        return revealedCoords.contains(new StratconCoords(x, y));
    }
    
    public Set<StratconCoords> getRevealedCoords() {
        return revealedCoords;
    }

    public void setRevealedCoords(Set<StratconCoords> revealedCoords) {
        this.revealedCoords = revealedCoords;
    }

    public void addFacility(StratconCoords coords, StratconFacility facility) {
        facilities.put(coords, facility);
    }
    
    public void removeFacility(StratconCoords coords) {
        facilities.remove(coords);
    }
    
    /**
     * Returns the allied facility coordinates closest to the given coordinates. Null if no allied facilities on the board.
     */
    public StratconCoords findClosestAlliedFacilityCoords(StratconCoords coords) {
        int minDistance = Integer.MAX_VALUE;
        StratconCoords closestFacilityCoords = null;
        
        for (StratconCoords facilityCoords : facilities.keySet()) {
            if (facilities.get(facilityCoords).getOwner() == ForceAlignment.Allied) {
                int distance = facilityCoords.direction(coords);
                
                if (distance < minDistance) {
                    minDistance = distance;
                    closestFacilityCoords = facilityCoords;
                }
            }
        }
        
        return closestFacilityCoords;        
    }
    
    /**
     * Returns (and possibly initializes, if necessary) a map between
     * scenario IDs and stratcon scenario pointers
     */
    public Map<Integer, StratconScenario> getBackingScenariosMap() {
        if (backingScenarioMap == null) {
            backingScenarioMap = new HashMap<>();
            for (StratconScenario scenario : getScenarios().values()) {
                backingScenarioMap.put(scenario.getBackingScenarioID(), scenario);
            }
        }
        
        return backingScenarioMap;
    }
    
    /**
     * Convenience method - returns true if the force with the given ID is currently deployed to this track
     */
    public boolean isForceDeployed(int forceID) {
        return assignedForceCoords.containsKey(forceID);
    }
    
    @Override
    public String toString() {
        return getDisplayableName();
    }
}
