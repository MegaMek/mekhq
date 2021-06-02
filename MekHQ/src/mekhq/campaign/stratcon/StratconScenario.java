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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import mekhq.adapter.DateAdapter;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioTemplate;

/**
 * Class that handles scenario metadata and interaction at the StratCon level
 * @author NickAragua
 */
public class StratconScenario implements IStratconDisplayable {
    /**
     * Represents the possible states of a Stratcon scenario
     */
    public enum ScenarioState {
        NONEXISTENT,
        UNRESOLVED,
        PRIMARY_FORCES_COMMITTED,
        REINFORCEMENTS_COMMITTED,
        COMPLETED,
        IGNORED,
        DEFEATED;
    
        private final static Map<ScenarioState, String> scenarioStateNames;
        
        static {
            scenarioStateNames = new HashMap<>();
            scenarioStateNames.put(ScenarioState.NONEXISTENT, "Shouldn't be seen");
            scenarioStateNames.put(ScenarioState.UNRESOLVED, "Unresolved");
            scenarioStateNames.put(ScenarioState.PRIMARY_FORCES_COMMITTED, "Primary forces committed");
            scenarioStateNames.put(ScenarioState.COMPLETED, "Victory");
            scenarioStateNames.put(ScenarioState.IGNORED, "Ignored");
            scenarioStateNames.put(ScenarioState.DEFEATED, "Defeat");
        }
        
        public String getScenarioStateName() {
            return scenarioStateNames.get(this);
        }
    }
    
    private AtBDynamicScenario backingScenario;
    
    private int backingScenarioID; 
    private ScenarioState currentState = ScenarioState.UNRESOLVED;
    private int requiredPlayerLances;
    private boolean requiredScenario;
    private boolean isStrategicObjective;
    private LocalDate deploymentDate;
    private LocalDate actionDate;
    private LocalDate returnDate;
    private StratconCoords coords;
    private int numDefensivePoints;
    private boolean ignoreForceAutoAssignment;
    private int leadershipPointsUsed;
    private Set<Integer> failedReinforcements = new HashSet<>();
    private Set<Integer> primaryForceIDs = new HashSet<>();

    /**
     * Add a force to the backing scenario. Do our best to add the force as a "primary" force, as defined in the scenario template.
     * @param forceID ID of the force to add.
     */
    public void addPrimaryForce(int forceID) {
        backingScenario.addForce(forceID, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        primaryForceIDs.add(forceID);
    }
    
    /**
     * Add a force to the backing scenario, trying to associate it with the given template.
     */
    public void addForce(int forceID, String templateID) {
        backingScenario.addForce(forceID, templateID);
    }
    
    /**
     * Add an individual unit to the backing scenario, trying to associate it with the given template.
     */
    public void addUnit(UUID unitID, String templateID) {
        backingScenario.addUnit(unitID, templateID);
    }

    public List<Integer> getAssignedForces() {
        return backingScenario.getForceIDs();
    }
    
    /**
     * These are all of the force IDs that have been matched up to a template
     * Note: since there's a default Reinforcements template, this is all forces
     * that have been assigned to this scenario
     */
    public List<Integer> getPlayerTemplateForceIDs() {
        return backingScenario.getPlayerTemplateForceIDs();
    }
    
    /**
     * These are all the "primary" force IDs, meaning forces that have been used
     * by the scenario to drive the generation of the OpFor.
     */
    public Set<Integer> getPrimaryForceIDs() {
        return primaryForceIDs;
    }
    
    public void setPrimaryForceIDs(Set<Integer> primaryForceIDs) {
        this.primaryForceIDs = primaryForceIDs;
    }
    
    /**
     * This convenience method sets the scenario's current state to PRIMARY_FORCES_COMMITTED
     * and fixes the forces that were assigned to this scenario prior as "primary".
     */
    public void commitPrimaryForces() {
        currentState = ScenarioState.PRIMARY_FORCES_COMMITTED;
        getPrimaryForceIDs().clear();
        
        for (int forceID : backingScenario.getPlayerTemplateForceIDs()) {
            getPrimaryForceIDs().add(forceID);
        }
    }

    public ScenarioState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ScenarioState state) {
        currentState = state;
    }

    @Override
    public String getInfo() {
        return getInfo(true);
    }
    
    public String getInfo(boolean html) {
        StringBuilder stateBuilder = new StringBuilder();

        if (isStrategicObjective()) {
            stateBuilder.append("<span color='red'>Contract objective located</span>").append(html ? "<br/>" : "");
        }
        
        stateBuilder.append("Scenario: ")
            .append(backingScenario.getName())
            .append(html ? "<br/>" : "");
            
        if (backingScenario.getTemplate() != null) {
            stateBuilder.append(backingScenario.getTemplate().shortBriefing)
                .append(html ? "<br/>" : "");
        }
        
        if (isRequiredScenario()) {
            stateBuilder.append("<span color='red'>Deployment required by contract</span>").append(html ? "<br/>" : "")
                .append("<span color='red'>-1 VP if lost/ignored; +1 VP if won</span>").append(html ? "<br/>" : "");
        }
        
        stateBuilder.append("Status: ")
            .append(currentState.getScenarioStateName())
            .append("<br/>");
        
        stateBuilder.append("Terrain: ");
        if ((backingScenario.getTerrainType() >= 0) && 
                (backingScenario.getTerrainType() < AtBScenario.terrainTypes.length)) {
            stateBuilder.append(AtBScenario.terrainTypes[backingScenario.getTerrainType()])
                .append(" : ")
                .append(backingScenario.getMap());
        }
        stateBuilder.append("<br/>");

        if (deploymentDate != null) {
            stateBuilder.append("Deployment Date: ")
                .append(deploymentDate.toString())
                .append("<br/>");
        }
        
        if (actionDate != null) {
            stateBuilder.append("Battle Date: ")
                .append(actionDate.toString())
                .append("<br/>");
        }
        
        if (returnDate != null) {
            stateBuilder.append("Return Date: ")
                .append(returnDate.toString())
                .append("<br/>");
        }    

        stateBuilder.append("</html>");
        return stateBuilder.toString();
    }
    
    public void updateMinefieldCount(int minefieldType, int number) {
        backingScenario.setNumPlayerMinefields(minefieldType, number);
    }

    public String getName() {
        return backingScenario.getName();
    }
    
    public int getRequiredPlayerLances() {
        return requiredPlayerLances;
    }
    

    public void setRequiredPlayerLances(int requiredPlayerLances) {
        this.requiredPlayerLances = requiredPlayerLances;
    }
    
    public void incrementRequiredPlayerLances() {
        requiredPlayerLances++;
    }

    public boolean isRequiredScenario() {
        return requiredScenario;
    }

    public void setRequiredScenario(boolean requiredScenario) {
        this.requiredScenario = requiredScenario;
    }
    
    @XmlTransient
    public AtBDynamicScenario getBackingScenario() {
        return backingScenario;
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getDeploymentDate() {
        return deploymentDate;
    }

    public void setDeploymentDate(LocalDate deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getActionDate() {
        return actionDate;
    }

    public void setActionDate(LocalDate actionDate) {
        this.actionDate = actionDate;
        backingScenario.setDate(actionDate);
    }

    @XmlJavaTypeAdapter(DateAdapter.class)
    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }
    
    public StratconCoords getCoords() {
        return coords;
    }

    public void setCoords(StratconCoords coords) {
        this.coords = coords;
    }
    
    public boolean isStrategicObjective() {
        return isStrategicObjective;
    }
    
    public void setStrategicObjective(boolean value) {
        isStrategicObjective = value;
    }
    
    public ScenarioTemplate getScenarioTemplate() {
        return backingScenario.getTemplate();
    }

    public int getBackingScenarioID() {
        return backingScenarioID;
    }
    
    public void setBackingScenario(AtBDynamicScenario backingScenario) {
        this.backingScenario = backingScenario;
        
        setBackingScenarioID(backingScenario.getId());
    }

    public void setBackingScenarioID(int backingScenarioID) {
        this.backingScenarioID = backingScenarioID;
    }

    public int getNumDefensivePoints() {
        return numDefensivePoints;
    }

    public void setNumDefensivePoints(int numDefensivePoints) {
        this.numDefensivePoints = numDefensivePoints;
    }
    
    public void useDefensivePoint() {
        numDefensivePoints--;
    }

    public Set<Integer> getFailedReinforcements() {
        return failedReinforcements;
    }

    public void setFailedReinforcements(Set<Integer> failedReinforcements) {
        this.failedReinforcements = failedReinforcements;
    }
    
    public void addFailedReinforcements(int forceID) {
        failedReinforcements.add(forceID);
    }

    public boolean ignoreForceAutoAssignment() {
        return ignoreForceAutoAssignment;
    }

    public void setIgnoreForceAutoAssignment(boolean ignoreForceAutoAssignment) {
        this.ignoreForceAutoAssignment = ignoreForceAutoAssignment;
    }

    public int getLeadershipPointsUsed() {
        return leadershipPointsUsed;
    }

    public void setLeadershipPointsUsed(int leadershipPointsUsed) {
        this.leadershipPointsUsed = leadershipPointsUsed;
    }
    
    public void useLeadershipPoint() {
        leadershipPointsUsed++;
    }
}