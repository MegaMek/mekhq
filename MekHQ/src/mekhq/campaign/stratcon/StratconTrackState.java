package mekhq.campaign.stratcon;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;

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
    
    private Map<StratconCoords, StratconFacility> facilities;   
    private Map<StratconCoords, StratconScenario> scenarios;    
    private Map<Integer, StratconCoords> assignedForceCoords;
    private Set<StratconCoords> revealedCoords;
    
    private int scenarioOdds;
    private int deploymentTime;
    private int requiredLanceCount;
    
    public StratconTrackState() {
        facilities = new HashMap<>();
        scenarios = new HashMap<>();
        assignedForceCoords = new HashMap<>();
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
    
    @XmlElementWrapper(name="trackScenarios")
    @XmlElement(name="scenario")
    public Map<StratconCoords, StratconScenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<StratconCoords, StratconScenario> scenarios) {
        this.scenarios = scenarios;
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

    public void assignForce(int forceID, StratconCoords coords) {
        assignedForceCoords.put(forceID, coords);
    }
    
    public void unassignForce(int forceID) {
        assignedForceCoords.remove(forceID);
    }
    
    public Map<Integer, StratconCoords> getAssignedForceCoords() {
        return assignedForceCoords;
    }

    public void setAssignedForceCoords(Map<Integer, StratconCoords> assignedForceCoords) {
        this.assignedForceCoords = assignedForceCoords;
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
    
    @Override
    public String toString() {
        return getDisplayableName();
    }
}
