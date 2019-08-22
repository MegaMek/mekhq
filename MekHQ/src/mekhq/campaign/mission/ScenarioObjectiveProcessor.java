package mekhq.campaign.mission;

import megamek.common.Entity;
import megamek.common.OffBoardDirection;

public class ScenarioObjectiveProcessor {
    /**
     * Determines if the given entity has met the given objective
     * @param entity Entity to check
     * @param objective Objective to check
     * @param opponentHasBattlefieldControl Whether the entity's opponent has battlefield control
     */
    private static boolean entityMeetsObjective(Entity entity, ScenarioObjective objective, boolean opponentHasBattlefieldControl) {
        if(objective.getAssociatedForceNames().contains(entity.getOwner().getName())) {
            switch(objective.getObjectiveCriterion()) {
            case Destroy:
                // we consider an entity destroyed if its crew has been killed or it's... uh... destroyed.
                // also, if it's immobilized and the opponent has battlefield control
                return entity.isDestroyed() || entity.getCrew().isDead() || entity.isImmobile() && opponentHasBattlefieldControl;
            case ForceWithdraw:
                // we consider an entity force-withdrawn if it's destroyed, crippled, or run off the field
                return entity.isDestroyed() || entity.isCrippled(true) || entity.getRetreatedDirection() != OffBoardDirection.NONE;
            case Capture:
                // we consider an entity captured if it's been immobilized but not destroyed and hasn't left the field
                return entity.isImmobile() && !entity.isDestroyed() && entity.getRetreatedDirection() == OffBoardDirection.NONE;
            case PreventReachMapEdge:
                // we've prevented the entity from reaching the edge if... 
                return entity.getRetreatedDirection() != objective.getDestinationEdge();
            case Preserve:
                // the entity is considered preserved if it hasn't been blown up
                // also if it's immobilized but we've retained battlefield control
                return !(entity.isDestroyed() || (entity.isImmobile() && !opponentHasBattlefieldControl));
            case ReachMapEdge:
                return entity.getRetreatedDirection() == objective.getDestinationEdge();
            default:
                return false;                    
            }
        }
        
        return false;
    }
}
