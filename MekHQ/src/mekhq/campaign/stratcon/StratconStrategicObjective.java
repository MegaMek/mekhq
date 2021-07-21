/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.stratcon;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

/**
 * This class is a data structure storing data relating to StratCon strategic objectives
 * and also handles some small amount of "business logic"
 */
public class StratconStrategicObjective {
    public static final int OBJECTIVE_FAILED = -1;
    
    private StratconCoords objectiveCoords;
    private StrategicObjectiveType objectiveType;
    private int currentObjectiveCount;
    private int desiredObjectiveCount;
    
    public StratconCoords getObjectiveCoords() {
        return objectiveCoords;
    }
    
    public void setObjectiveCoords(StratconCoords objectiveCoords) {
        this.objectiveCoords = objectiveCoords;
    }
    
    public StrategicObjectiveType getObjectiveType() {
        return objectiveType;
    }
    
    public void setObjectiveType(StrategicObjectiveType objectiveType) {
        this.objectiveType = objectiveType;
    }
    
    public int getCurrentObjectiveCount() {
        return currentObjectiveCount;
    }

    public void setCurrentObjectiveCount(int currentObjectiveCount) {
        this.currentObjectiveCount = currentObjectiveCount;
    }
    
    public void incrementCurrentObjectiveCount() {
        currentObjectiveCount++;
    }

    public int getDesiredObjectiveCount() {
        return desiredObjectiveCount;
    }

    public void setDesiredObjectiveCount(int desiredObjectiveCount) {
        this.desiredObjectiveCount = desiredObjectiveCount;
    }

    public boolean isObjectiveFailed(StratconTrackState trackState) {
        switch (getObjectiveType()) {
            case AnyScenarioVictory:
            case SpecificScenarioVictory:
                // you can fail this if the scenario goes away somehow
                return getCurrentObjectiveCount() == OBJECTIVE_FAILED;
            case AlliedFacilityControl:
                // you can fail this by having the facility destroyed
                StratconFacility alliedFacility = trackState.getFacility(getObjectiveCoords());
                return alliedFacility == null;
            case HostileFacilityControl:
                // you can fail this by having the facility destroyed
                StratconFacility hostileFacility = trackState.getFacility(getObjectiveCoords());
                return hostileFacility == null;
            case FacilityDestruction:
                // you can't really permanently fail this
                return false;
            default:
                // we shouldn't be here, but just in case
                return false;
        }
    }
    
    /**
     * Given the track that this objective is on, is it complete?
     */
    public boolean isObjectiveCompleted(StratconTrackState trackState) {
        switch (getObjectiveType()) {
            case AnyScenarioVictory:
            case SpecificScenarioVictory:
                // this is set once qualifying scenarios are completed
                return getCurrentObjectiveCount() >= getDesiredObjectiveCount();
            case AlliedFacilityControl:
                // this is "ok" if the facility exists and is under allied control
                StratconFacility alliedFacility = trackState.getFacility(getObjectiveCoords());
                return (alliedFacility != null) && (alliedFacility.getOwner() == ForceAlignment.Allied);
            case HostileFacilityControl:
            case FacilityDestruction:
                // these are "ok" if the facility no longer exists or is under allied control
                // we assume that we can slag a facility at any time if we control it
                StratconFacility hostileFacility = trackState.getFacility(getObjectiveCoords());
                return (hostileFacility == null) || (hostileFacility.getOwner() == ForceAlignment.Allied);
            default:
                // we shouldn't be here, but just in case
                return false;
        }
    }
}
