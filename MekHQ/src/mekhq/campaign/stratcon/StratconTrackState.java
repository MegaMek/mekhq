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
 */
package mekhq.campaign.stratcon;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;
import mekhq.utilities.MHQXMLUtility;

import java.time.LocalDate;
import java.util.*;

/**
 * Track-level state object for a StratCon campaign.
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
    private List<StratconStrategicObjective> strategicObjectives;

    private Map<StratconCoords, String> terrainTypes;

    // don't serialize this
    private transient Map<Integer, StratconScenario> backingScenarioMap;
    private transient Map<StratconCoords, StratconStrategicObjective> specificStrategicObjectives;

    private int scenarioOdds;
    private int deploymentTime;
    private int requiredLanceCount;

    private int temperature;

    public StratconTrackState() {
        facilities = new HashMap<>();
        scenarios = new HashMap<>();
        assignedForceCoords = new HashMap<>();
        assignedForceReturnDates = new HashMap<>();
        assignedCoordForces = new HashMap<>();
        setAssignedForceReturnDatesForStorage(new HashMap<>());
        revealedCoords = new HashSet<>();
        stickyForces = new HashSet<>();
        strategicObjectives = new ArrayList<>();
        terrainTypes = new HashMap<>();
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

    /**
     * @return The size of the track derived by multiplying width and height.
     */
    public int getSize() {
        return width * height;
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

    public void removeScenario(int campaignScenarioID) {
        if (getBackingScenariosMap().containsKey(campaignScenarioID)) {
            removeScenario(getBackingScenariosMap().get(campaignScenarioID));
        }
    }

    /**
     * Removes a StratconScenario from this track.
     */
    public void removeScenario(StratconScenario scenario) {
        scenarios.remove(scenario.getCoords());
        getBackingScenariosMap().remove(scenario.getBackingScenarioID());
        Map<StratconCoords, StratconStrategicObjective> objectives = getObjectivesByCoords();
        if (objectives.containsKey(scenario.getCoords())) {
            StrategicObjectiveType objectiveType = objectives.get(scenario.getCoords()).getObjectiveType();

            switch (objectiveType) {
                case RequiredScenarioVictory:
                case SpecificScenarioVictory:
                    objectives.remove(scenario.getCoords());
                    break;
                default:
                    break;
            }
        }

        // any assigned forces get cleared out here as well.
        for (int forceID : scenario.getAssignedForces()) {
            unassignForce(forceID);

            // scenario book-keeping
            scenario.getPrimaryForceIDs().clear();
        }
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
     * Convenience function that determines if there are any forces deployed to the given
     * coordinates.
     */
    public boolean areAnyForceDeployedTo(StratconCoords coords) {
        return getAssignedCoordForces().containsKey(coords) &&
                !getAssignedCoordForces().get(coords).isEmpty();
    }

    /**
     * Handles the assignment of a force to the given coordinates on this track on the given date.
     */
    public void assignForce(int forceID, StratconCoords coords, LocalDate date, boolean sticky) {
        assignedForceCoords.put(forceID, coords);
        assignedCoordForces.putIfAbsent(coords, new HashSet<>());
        assignedCoordForces.get(coords).add(forceID);

        if (sticky) {
            addStickyForce(forceID);
        }

        LocalDate returnDate;

        // if we're assigning the force to a scenario, then
        // the return date should be the scenario's return date;
        // otherwise, just deploy it for the minimum amount for the track
        if (getScenarios().containsKey(coords)) {
            returnDate = getScenarios().get(coords).getReturnDate();
        } else {
            returnDate = date.plusDays(deploymentTime);
        }

        getAssignedForceReturnDates().put(forceID, returnDate);
        getAssignedForceReturnDatesForStorage().put(forceID, returnDate.toString());
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
     * Handles the unassignment of a force from this track.
     */
    public void unassignUnit(int forceID) {
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
            assignedForceReturnDates.put(forceID, MHQXMLUtility.parseDate(getAssignedForceReturnDatesForStorage().get(forceID)));
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
     * Returns (and possibly initializes, if necessary) a map between
     * coordinates and strategic objectives
     */
    public Map<StratconCoords, StratconStrategicObjective> getObjectivesByCoords() {
        if (specificStrategicObjectives == null) {
            specificStrategicObjectives = new HashMap<>();
            for (StratconStrategicObjective objective : strategicObjectives) {
                specificStrategicObjectives.put(objective.getObjectiveCoords(), objective);
            }
        }

        return specificStrategicObjectives;
    }

    /**
     * Moves a strategic objectives from the source to the destination coordinates.
     * @return True if the operation succeeded, false if it failed
     */
    public boolean moveObjective(StratconCoords source, StratconCoords destination) {
        // safety: don't move it if it's not there; logic prevents two objectives in the same coords
        if (getObjectivesByCoords().containsKey(source) &&
                !getObjectivesByCoords().containsKey(destination)) {
            StratconStrategicObjective objective = getObjectivesByCoords().get(source);
            // gotta get the cache
            getObjectivesByCoords().remove(source);
            getObjectivesByCoords().put(destination, objective);
            objective.setObjectiveCoords(destination);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Convenience method to fail an objective at the given coordinates.
     */
    public void failObjective(StratconCoords coords) {
        if (getObjectivesByCoords().containsKey(coords)) {
            getObjectivesByCoords().get(coords).setCurrentObjectiveCount(StratconStrategicObjective.OBJECTIVE_FAILED);
        }
    }

    /**
     * @return Whether or not this track has a facility on it that reveals the track.
     */
    public boolean hasActiveTrackReveal() {
        return getFacilities().values().stream().anyMatch(StratconFacility::getRevealTrack);
    }

    /**
     * Determines the number of facilities on this track that actively reveal the track.
     *
     * <p>This method iterates through all facilities associated with the track and counts
     * how many of them have the ability to reveal the track, as determined by the facility's
     * {@link StratconFacility#getIncreaseScanRange()} method.</p>
     *
     * @return an integer representing the total number of facilities on this track
     *         that are actively revealing it.
     */
    public int getScanRangeIncrease() {
        int scanRange = 0;
        for (StratconFacility facility : getFacilities().values()) {
            if (facility.getIncreaseScanRange()) {
                scanRange++;
            }
        }
        return scanRange;
    }

    /**
     * Count of all the scenario odds adjustments from facilities
     * (and potentially other sources) on this track.
     */
    public int getScenarioOddsAdjustment() {
        int accumulator = 0;
        for (StratconFacility facility : getFacilities().values()) {
            accumulator += facility.getScenarioOddsModifier();
        }

        return accumulator;
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

    @XmlElementWrapper(name = "instantiatedObjectives")
    @XmlElement(name = "instantiatedObjective")
    public List<StratconStrategicObjective> getStrategicObjectives() {
        return strategicObjectives;
    }

    public void setStrategicObjectives(List<StratconStrategicObjective> strategicObjectives) {
        this.strategicObjectives = strategicObjectives;
    }

    public void addStrategicObjective(StratconStrategicObjective strategicObjective) {
        getStrategicObjectives().add(strategicObjective);
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temp) {
        temperature = temp;
    }

    public void setTerrainTile(StratconCoords coords, String terrainTypeName) {
        terrainTypes.put(coords, terrainTypeName);
    }

    public String getTerrainTile(StratconCoords coords) {
        return terrainTypes.getOrDefault(coords, "");
    }

    @XmlElementWrapper(name = "terrainTypes")
    @XmlElement(name = "terrainType")
    public Map<StratconCoords, String> getTerrainTypes() {
        return terrainTypes;
    }

    public void setStrategicObjectives(Map<StratconCoords, String> terrainTypes) {
        this.terrainTypes = terrainTypes;
    }
}
