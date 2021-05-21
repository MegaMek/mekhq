package mekhq.campaign.stratcon;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

public class StratconStrategicObjective {
    /*public enum StrategicObjectiveState {
        InProgress,
        Failed,
        Succeeded
    }*/
    
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

    public int getDesiredObjectiveCount() {
        return desiredObjectiveCount;
    }

    public void setDesiredObjectiveCount(int desiredObjectiveCount) {
        this.desiredObjectiveCount = desiredObjectiveCount;
    }

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
