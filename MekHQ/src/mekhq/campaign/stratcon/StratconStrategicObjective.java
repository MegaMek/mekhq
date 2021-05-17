package mekhq.campaign.stratcon;

import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

public class StratconStrategicObjective {
    private StratconCoords objectiveCoords;
    private StrategicObjectiveType objectiveType;
    //private Boolean objectiveCompleted;
    
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
}
