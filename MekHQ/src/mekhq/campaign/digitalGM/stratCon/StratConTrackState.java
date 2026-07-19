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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.utilities.MHQXMLUtility;

/**
 * Track-level state object for a StratCon campaign.
 *
 * @author NickAragua
 */
@XmlRootElement(name = "campaignTrack")
public class StratConTrackState {
    public static final String ROOT_XML_ELEMENT_NAME = "StratConTrackState";

    // a track has the following characteristics:
    // width/height
    // [future]: terrain information by coordinates
    // scenario information by coordinates
    // active facilities by coordinates
    private String displayableName;
    private int width;
    private int height;
    private boolean gmRevealed;

    private Map<StratConCoords, StratConFacility> facilities;
    private Map<StratConCoords, StratConScenario> scenarios;
    private Map<StratConCoords, Set<Integer>> assignedCoordForces;
    private Map<Integer, StratConCoords> assignedForceCoords;
    private Map<Integer, LocalDate> assignedForceReturnDates;
    private Set<Integer> stickyForces;
    private Map<Integer, String> assignedForceReturnDatesForStorage;
    private Set<StratConCoords> revealedCoords;
    private List<StratConStrategicObjective> strategicObjectives;

    private Map<StratConCoords, String> terrainTypes;

    // city overlay: a city sits on top of the base terrain of its hex; rendered with the generic urban sprite
    private Set<StratConCoords> cities;

    // road overlay: hexes carrying a road, plus the border hexes whose road branches off the map into a neighbor sector
    private Set<StratConCoords> roads;
    private Set<StratConCoords> roadExits;

    // don't serialize this
    private transient Map<Integer, StratConScenario> backingScenarioMap;
    private transient Map<StratConCoords, StratConStrategicObjective> specificStrategicObjectives;

    private int scenarioOdds;
    private int deploymentTime;
    private int requiredLanceCount;

    private int temperature;

    // how built-up this sector's cities are, 0.0 (hamlets) to 1.0 (dense metropolis), from the planet's population;
    // carried onto city-hex scenarios to scale the urban area laid onto their battle maps
    private double urbanizationLevel;

    // Environment summary recorded by the improved generator, for the sector info panel (all null on legacy tracks).
    // Stored as the profile/band enum names (prettified for display) to keep this core class off the generation package.
    private String latitudeBand;
    private String hydrologyProfile;
    private String orogenyProfile;
    private String urbanProfile;

    public StratConTrackState() {
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
        cities = new HashSet<>();
        roads = new HashSet<>();
        roadExits = new HashSet<>();
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
    public Map<StratConCoords, StratConFacility> getFacilities() {
        return facilities;
    }

    public void setFacilities(Map<StratConCoords, StratConFacility> facilities) {
        this.facilities = facilities;
    }

    public StratConFacility getFacility(StratConCoords coords) {
        return facilities.get(coords);
    }

    /**
     * Used for serialization/deserialization. Do not manipulate directly, or things get unpleasant.
     */
    @XmlElementWrapper(name = "trackScenarios")
    @XmlElement(name = "scenario")
    public Map<StratConCoords, StratConScenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<StratConCoords, StratConScenario> scenarios) {
        this.scenarios = scenarios;
    }

    /**
     * Adds a StratConScenario to this track. Assumes it already has some coordinates assigned, and a valid campaign
     * scenario ID for its backing AtB scenario
     */
    public void addScenario(StratConScenario scenario) {
        scenarios.put(scenario.getCoords(), scenario);

        updateScenario(scenario);
    }

    /**
     * Updates an existing scenario on this track.
     */
    public void updateScenario(StratConScenario scenario) {
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
     * Removes a StratConScenario from this track.
     */
    public void removeScenario(StratConScenario scenario) {
        scenarios.remove(scenario.getCoords());
        getBackingScenariosMap().remove(scenario.getBackingScenarioID());

        // any assigned forces get cleared out here as well.
        for (int forceID : scenario.getAssignedForces()) {
            unassignFormation(forceID);

            // scenario bookkeeping
            scenario.getPrimaryForceIDs().clear();
        }
    }

    public StratConScenario getScenario(StratConCoords coords) {
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
     * Convenience function that determines if there are any forces deployed to the given coordinates.
     */
    public boolean areAnyForceDeployedTo(StratConCoords coords) {
        return getAssignedCoordForces().containsKey(coords) &&
                     !getAssignedCoordForces().get(coords).isEmpty();
    }

    /**
     * Handles the assignment of a force to the given coordinates on this track on the given date.
     */
    public void assignForce(int forceID, StratConCoords coords, LocalDate date, boolean sticky) {
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
    public void unassignFormation(int forceID) {
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
    @Deprecated(since = "0.51.0", forRemoval = true)
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
     * Restores the look-up table of force IDs to return dates
     */
    public void restoreReturnDates() {
        for (int forceID : getAssignedForceReturnDatesForStorage().keySet()) {
            assignedForceReturnDates.put(forceID,
                  MHQXMLUtility.parseDate(getAssignedForceReturnDatesForStorage().get(forceID)));
        }
    }

    public Map<Integer, StratConCoords> getAssignedForceCoords() {
        return assignedForceCoords;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setAssignedForceCoords(Map<Integer, StratConCoords> assignedForceCoords) {
        this.assignedForceCoords = assignedForceCoords;
    }

    @XmlTransient
    public Map<StratConCoords, Set<Integer>> getAssignedCoordForces() {
        return assignedCoordForces;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setAssignedCoordForces(Map<StratConCoords, Set<Integer>> assignedCoordForces) {
        this.assignedCoordForces = assignedCoordForces;
    }

    /**
     * Restores the look-up table of coordinates to force lists
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

    @Deprecated(since = "0.51.0", forRemoval = true)
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
        return revealedCoords.contains(new StratConCoords(x, y));
    }

    public Set<StratConCoords> getRevealedCoords() {
        return revealedCoords;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setRevealedCoords(Set<StratConCoords> revealedCoords) {
        this.revealedCoords = revealedCoords;
    }

    public void addFacility(StratConCoords coords, StratConFacility facility) {
        facilities.put(coords, facility);
    }

    public void removeFacility(StratConCoords coords) {
        facilities.remove(coords);
    }

    /**
     * Returns the allied facility coordinates closest to the given coordinates. Null if no allied facilities on the
     * board.
     */
    @Nullable
    @Deprecated(since = "0.51.0", forRemoval = true)
    public StratConCoords findClosestAlliedFacilityCoords(StratConCoords coords) {
        int minDistance = Integer.MAX_VALUE;
        StratConCoords closestFacilityCoords = null;

        for (StratConCoords facilityCoords : facilities.keySet()) {
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
     * Returns (and possibly initializes, if necessary) a map between scenario IDs and StratCon scenario pointers
     */
    public Map<Integer, StratConScenario> getBackingScenariosMap() {
        if (backingScenarioMap == null) {
            backingScenarioMap = new HashMap<>();
            for (StratConScenario scenario : getScenarios().values()) {
                backingScenarioMap.put(scenario.getBackingScenarioID(), scenario);
            }
        }

        return backingScenarioMap;
    }

    /**
     * Returns (and possibly initializes, if necessary) a map between coordinates and strategic objectives
     */
    public Map<StratConCoords, StratConStrategicObjective> getObjectivesByCoords() {
        if (specificStrategicObjectives == null) {
            specificStrategicObjectives = new HashMap<>();
            for (StratConStrategicObjective objective : strategicObjectives) {
                StratConCoords coords = objective.getObjectiveCoords();
                if (coords != null) {
                    specificStrategicObjectives.put(coords, objective);
                }
            }
        }

        return specificStrategicObjectives;
    }

    /**
     * Moves a strategic objectives from the source to the destination coordinates.
     *
     * @return True if the operation succeeded, false if it failed
     */
    public boolean moveObjective(StratConCoords source, StratConCoords destination) {
        // safety: don't move it if it's not there; logic prevents two objectives in the same coords
        if (getObjectivesByCoords().containsKey(source) &&
                  !getObjectivesByCoords().containsKey(destination)) {
            StratConStrategicObjective objective = getObjectivesByCoords().get(source);
            // I've to get the cache
            getObjectivesByCoords().remove(source);
            getObjectivesByCoords().put(destination, objective);
            objective.setObjectiveCoords(destination);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Convenience method to fail an objective at the given coordinates. Does nothing if the objective has already been
     * completed.
     */
    public void failObjective(StratConCoords coords) {
        if (getObjectivesByCoords().containsKey(coords)) {
            StratConStrategicObjective objective = getObjectivesByCoords().get(coords);
            if (!objective.isObjectiveCompleted(this)) {
                objective.setCurrentObjectiveCount(StratConStrategicObjective.OBJECTIVE_FAILED);
            }
        }
    }

    /**
     * @return Whether this track has a facility on it that reveals the track.
     */
    public boolean hasActiveTrackReveal() {
        return getFacilities().values().stream().anyMatch(StratConFacility::getRevealTrack);
    }

    /**
     * Determines the number of facilities on this track that actively reveal the track.
     *
     * <p>This method iterates through all facilities associated with the track and counts
     * how many of them have the ability to reveal the track, as determined by the facility's
     * {@link StratConFacility#getIncreaseScanRange()} method.</p>
     *
     * @return an integer representing the total number of facilities on this track that are actively revealing it.
     */
    public int getScanRangeIncrease() {
        int scanRange = 0;
        for (StratConFacility facility : getFacilities().values()) {
            if (facility.getIncreaseScanRange()) {
                scanRange++;
            }
        }
        return scanRange;
    }

    /**
     * Count of all the scenario odds adjustments from facilities (and potentially other sources) on this track.
     */
    public int getScenarioOddsAdjustment() {
        int accumulator = 0;
        for (StratConFacility facility : getFacilities().values()) {
            accumulator += facility.getScenarioOddsModifier();
        }

        return accumulator;
    }

    /**
     * Convenience method - returns true if the force with the given ID is currently deployed to this track
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
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

    @Deprecated(since = "0.51.0", forRemoval = true)
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
    public List<StratConStrategicObjective> getStrategicObjectives() {
        return strategicObjectives;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setStrategicObjectives(List<StratConStrategicObjective> strategicObjectives) {
        this.strategicObjectives = strategicObjectives;
        specificStrategicObjectives = null;
    }

    public void addStrategicObjective(StratConStrategicObjective strategicObjective) {
        getStrategicObjectives().add(strategicObjective);
        if (specificStrategicObjectives != null) {
            StratConCoords coords = strategicObjective.getObjectiveCoords();
            if (coords != null) {
                specificStrategicObjectives.put(coords, strategicObjective);
            }
        }
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temp) {
        temperature = temp;
    }

    /**
     * @return how built-up this sector's cities are, 0.0 (hamlets) to 1.0 (dense metropolis)
     */
    public double getUrbanizationLevel() {
        return urbanizationLevel;
    }

    public void setUrbanizationLevel(double urbanizationLevel) {
        this.urbanizationLevel = urbanizationLevel;
    }

    public String getLatitudeBand() {
        return latitudeBand;
    }

    public void setLatitudeBand(String latitudeBand) {
        this.latitudeBand = latitudeBand;
    }

    public String getHydrologyProfile() {
        return hydrologyProfile;
    }

    public void setHydrologyProfile(String hydrologyProfile) {
        this.hydrologyProfile = hydrologyProfile;
    }

    public String getOrogenyProfile() {
        return orogenyProfile;
    }

    public void setOrogenyProfile(String orogenyProfile) {
        this.orogenyProfile = orogenyProfile;
    }

    public String getUrbanProfile() {
        return urbanProfile;
    }

    public void setUrbanProfile(String urbanProfile) {
        this.urbanProfile = urbanProfile;
    }

    public void setTerrainTile(StratConCoords coords, String terrainTypeName) {
        terrainTypes.put(coords, terrainTypeName);
    }

    /**
     * Check to see if specified coordinates would be placed off the StratCon board
     */
    public boolean isOffTrack(StratConCoords coords) {
        int width = getWidth() - 1;
        int height = getHeight() - 1;
        return (coords.getX() < 0) ||
                     (coords.getX() > width) ||
                     (coords.getY() < 0) ||
                     (coords.getY() > height);
    }

    public String getTerrainTile(StratConCoords coords) {
        return terrainTypes.getOrDefault(coords, "");
    }

    @XmlElementWrapper(name = "terrainTypes")
    @XmlElement(name = "terrainType")
    @Deprecated(since = "0.51.0", forRemoval = true)
    public Map<StratConCoords, String> getTerrainTypes() {
        return terrainTypes;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void setStrategicObjectives(Map<StratConCoords, String> terrainTypes) {
        this.terrainTypes = terrainTypes;
    }

    /**
     * @param coords the hex to test
     *
     * @return {@code true} if a city sits on the given hex
     */
    public boolean isCity(StratConCoords coords) {
        return cities.contains(coords);
    }

    /**
     * Marks the given hex as holding a city. Cities are an overlay: the hex keeps its base terrain.
     *
     * @param coords the hex to make a city
     */
    public void addCity(StratConCoords coords) {
        cities.add(coords);
    }

    @XmlElementWrapper(name = "cities")
    @XmlElement(name = "city")
    public Set<StratConCoords> getCities() {
        return cities;
    }

    public void setCities(Set<StratConCoords> cities) {
        this.cities = cities;
    }

    /**
     * @param coords the hex to test
     *
     * @return {@code true} if a road runs through the given hex
     */
    public boolean isRoad(StratConCoords coords) {
        return roads.contains(coords);
    }

    @XmlElementWrapper(name = "roads")
    @XmlElement(name = "road")
    public Set<StratConCoords> getRoads() {
        return roads;
    }

    public void setRoads(Set<StratConCoords> roads) {
        this.roads = roads;
    }

    /**
     * @return the border hexes whose road branches off the map into a neighboring sector
     */
    @XmlElementWrapper(name = "roadExits")
    @XmlElement(name = "roadExit")
    public Set<StratConCoords> getRoadExits() {
        return roadExits;
    }

    public void setRoadExits(Set<StratConCoords> roadExits) {
        this.roadExits = roadExits;
    }

    /**
     * Clears all generated terrain, cities, roads, and hex reveals so the sector can be regenerated from scratch.
     * Scenarios, facilities, and assigned forces are left untouched.
     */
    public void clearForRegeneration() {
        terrainTypes.clear();
        cities.clear();
        roads.clear();
        roadExits.clear();
        revealedCoords.clear();
    }
}
