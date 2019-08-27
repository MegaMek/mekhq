package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.OffBoardDirection;

/**
 * Contains metadata used to describe a scenario objective
 * @author NickAragua
 */
public class ScenarioObjective {
    private static Map<ObjectiveCriterion, String> objectiveTypeMapping;
    
    static {
        objectiveTypeMapping = new HashMap<>();
        objectiveTypeMapping.put(ObjectiveCriterion.Destroy, "Destroy");
        objectiveTypeMapping.put(ObjectiveCriterion.ForceWithdraw, "Force Withdrawal");
        objectiveTypeMapping.put(ObjectiveCriterion.Capture, "Capture");
        objectiveTypeMapping.put(ObjectiveCriterion.PreventReachMapEdge, "Prevent From Reaching");
        objectiveTypeMapping.put(ObjectiveCriterion.Preserve, "Preserve");
        objectiveTypeMapping.put(ObjectiveCriterion.ReachMapEdge, "Reach");
        objectiveTypeMapping.put(ObjectiveCriterion.Custom, "Custom");
    }
    
    private ObjectiveCriterion objectiveCriterion;
    private String description;
    private OffBoardDirection destinationEdge;
    private int percentage;
    private Set<String> associatedForceNames = new HashSet<>();
    private Set<String> associatedUnitIDs = new HashSet<>();
    private List<ObjectiveEffect> successEffects = new ArrayList<>();
    private List<ObjectiveEffect> failureEffects = new ArrayList<>();
    
    /**
     * Types of automatically tracked scenario objectives
     */
    public enum ObjectiveCriterion {
        // entity must be destroyed:
        // center torso/structure gone, crew killed, immobilized + battlefield control
        Destroy,
        // entity must be crippled, destroyed or withdrawn off the wrong edge of the map        
        ForceWithdraw,
        // entity must be immobilized but not destroyed
        Capture,
        // entity must be prevented from reaching a particular map edge
        PreventReachMapEdge,
        // entity must be intact (can be crippled, immobilized, crew-killed)
        Preserve,
        // if an entity crossed a particular map edge without getting messed up en route
        ReachMapEdge,
        // this must be tracked manually by the player
        Custom;
        
        @Override
        public String toString() {
            return objectiveTypeMapping.get(this);
        }
    }

    public ObjectiveCriterion getObjectiveCriterion() {
        return objectiveCriterion;
    }

    public void setObjectiveCriterion(ObjectiveCriterion objectiveCriterion) {
        this.objectiveCriterion = objectiveCriterion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void addForce(String name) {
        associatedForceNames.add(name);
    }
    
    public void removeForce(String name) {
        associatedForceNames.remove(name);
    }
    
    public Set<String> getAssociatedForceNames() {
        return new HashSet<String>(associatedForceNames);
    }
    
    public void addUnit(String id) {
        associatedUnitIDs.add(id);
    }
    
    public void removeUnit(String id) {
        associatedUnitIDs.remove(id);
    }
    
    public Set<String> getAssociatedUnitIDs() {
        return associatedUnitIDs;
    }

    public void addSuccessEffect(ObjectiveEffect successEffect) {
        successEffects.add(successEffect);
    }
    
    public List<ObjectiveEffect> getSuccessEffects() {
        return successEffects;
    }
    
    public void addFailureEffect(ObjectiveEffect failureEffect) {
        failureEffects.add(failureEffect);
    }
    
    public List<ObjectiveEffect> getFailureEffects() {
        return failureEffects;
    }

    public OffBoardDirection getDestinationEdge() {
        return destinationEdge;
    }

    public void setDestinationEdge(OffBoardDirection destinationEdge) {
        this.destinationEdge = destinationEdge;
    }
    
    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        sb.append("\nObjective Type: ");
        sb.append(objectiveCriterion.toString());
        
        if(objectiveCriterion == ObjectiveCriterion.ReachMapEdge || 
                objectiveCriterion == ObjectiveCriterion.PreventReachMapEdge) {
            sb.append("\n");
            sb.append(destinationEdge.toString());
            sb.append(" edge");
        }
        
        sb.append(percentage);
        sb.append("%% ");
        
        if(associatedForceNames.size() > 0) {
            sb.append("\nForces:");        
            for(String forceName : associatedForceNames) {
                sb.append("\n");
                sb.append(forceName);
            }
        }
        
        if(associatedUnitIDs.size() > 0) {
            for(String unitID : associatedUnitIDs) {
                sb.append("\n");
                sb.append(unitID.toString());
            }
        }
        
        if(successEffects.size() > 0) {
            for(ObjectiveEffect effect : successEffects) {
                sb.append("\n");
                sb.append(effect.toString());
            }
        }
        
        if(failureEffects.size() > 0) {
            for(ObjectiveEffect effect : failureEffects) {
                sb.append("\n");
                sb.append(effect.toString());
            }
        }
        
        return sb.toString();
    }
}