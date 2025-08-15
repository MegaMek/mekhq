/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.stratcon;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

/**
 * This class is a data structure storing data relating to StratCon strategic objectives and also handles some small
 * amount of "business logic"
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
