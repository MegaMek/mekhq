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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import megamek.common.annotations.Nullable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;

/**
 * Track-level state object for a stratcon campaign.
 * @author NickAragua
 */
@XmlRootElement(name = "campaignTrack")
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
    private Map<StratconCoords, Set<Integer>> assignedCoordForces; 
    private Map<Integer, StratconCoords> assignedForceCoords;
    private Map<Integer, LocalDate> assignedForceReturnDates;
    private Set<Integer> stickyForces;
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
        stickyForces = new HashSet<>();
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

    @XmlElementWrapper(name = "trackFacilities")
    @XmlElement(name = "facility")
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
     */
    @XmlElementWrapper(name = "trackScenarios")
    @XmlElement(name = "scenario")
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
        if (scenarios.containsKey(scenario.getCoords()) && (scenario.getBackingScenarioID() > 0)) {
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

    /**
     * Handles the assignment of a force to the given coordinates on this track on the given date.
     */
    public void assignForce(int forceID, StratconCoords coords, LocalDate date, boolean sticky) {
        assignedForceCoords.put(forceID, coords);
        assignedForceReturnDates.put(forceID, date.plusDays(deploymentTime));
        
        assignedCoordForces.putIfAbsent(coords, new HashSet<>());
        assignedCoordForces.get(coords).add(forceID);
        
        if (sticky) {
            addStickyForce(forceID);
        }
        
        getAssignedForceReturnDatesForStorage().put(forceID, date.plusDays(deploymentTime).toString());
    }
    
    /**
     * Handles the unassignment of a force from this track.
     */
    public void unassignForce(int forceID) {
        if (assignedForceCoords.containsKey(forceID)) {
            assignedCoordForces.get(assignedForceCoords.get(forceID)).remove(forceID);
            assignedForceCoords.remove(forceID);
            assignedForceReturnDates.remove(forceID);
            removeStickyForce(forceID);
            getAssignedForceReturnDatesForStorage().remove(forceID);
        }
    }
    
    /**
     * Restores the look up table of force IDs to return dates
     */
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
    
    @XmlTransient
    public Map<StratconCoords, Set<Integer>> getAssignedCoordForces() {
        return assignedCoordForces;
    }

    public void setAssignedCoordForces(Map<StratconCoords, Set<Integer>> assignedCoordForces) {
        this.assignedCoordForces = assignedCoordForces;
    }
    
    /**
     * Restores the look up table of coordinates to force lists
     */
    public void restoreAssignedCoordForces() {
        for (int forceID : assignedForceCoords.keySet()) {
            assignedCoordForces.putIfAbsent(assignedForceCoords.get(forceID), new HashSet<>());
            assignedCoordForces.get(assignedForceCoords.get(forceID)).add(forceID);
        }
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
    @Nullable
    public StratconCoords findClosestAlliedFacilityCoords(StratconCoords coords) {
        int minDistance = Integer.MAX_VALUE;
        StratconCoords closestFacilityCoords = null;
        
        for (StratconCoords facilityCoords : facilities.keySet()) {
            if (facilities.get(facilityCoords).getOwner() == ForceAlignment.Allied) {
                int distance = facilityCoords.distance(coords);
                
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

    public Set<Integer> getStickyForces() {
        return stickyForces;
    }

    public void setStickyForces(Set<Integer> stickyForces) {
        this.stickyForces = stickyForces;
    }
    
    public void addStickyForce(int forceID) {
        stickyForces.add(forceID);
    }
    
    public void removeStickyForce(int forceID) {
        stickyForces.remove(forceID);
    }
}
