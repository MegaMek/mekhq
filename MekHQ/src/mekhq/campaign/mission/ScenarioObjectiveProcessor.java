package mekhq.campaign.mission;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;

public class ScenarioObjectiveProcessor {
    
    private Map<ScenarioObjective, Set<String>> qualifyingObjectiveUnits;
    private Map<ScenarioObjective, Set<String>> potentialObjectiveUnits;
    
    public ScenarioObjectiveProcessor() {
        qualifyingObjectiveUnits = new HashMap<>();
        potentialObjectiveUnits = new HashMap<>();
    }
    
    /**
     * Given a ResolveScenarioTracker, evaluate the units contained therein
     * in an effort to determine the units associated and units that meet each objective in the scenario played.
     * @param tracker Tracker to process
     * @param objectiveUnitIDs Map to populate with all units potentially eligible for the objective
     * @return Map containing all units that are eligible for the objective
     */
    public void evaluateScenarioObjectives(ResolveScenarioTracker tracker) {
        for(ScenarioObjective objective : tracker.getScenario().getScenarioObjectives()) {
            
            Set<String> currentObjectiveUnitIDs = new HashSet<>(objective.getAssociatedUnitIDs());
            currentObjectiveUnitIDs.addAll(unrollObjectiveForces(objective, tracker));
            
            potentialObjectiveUnits.put(objective, currentObjectiveUnitIDs);
            
            Set<String> qualifyingUnits = evaluateObjective(objective, currentObjectiveUnitIDs, 
                    tracker.getAllInvolvedUnits().values(), tracker);
            qualifyingObjectiveUnits.put(objective, qualifyingUnits);
        }
    }
    
    /**
     * Given an objective and a resolution tracker, "unroll" the force names specified in the objective 
     * into a set of unit IDs.
     */
    private Set<String> unrollObjectiveForces(ScenarioObjective objective, ResolveScenarioTracker tracker) {
        Set<String> objectiveUnitIDs = new HashSet<>();
        
        // "expand" the player forces involved in the objective
        for(String forceName : objective.getAssociatedForceNames()) {
            boolean forceFound = false;
            
            for(Force force : tracker.getCampaign().getAllForces()) {
                if(force.getName().equals(forceName)) {
                    for(UUID unitID : force.getUnits()) {
                        objectiveUnitIDs.add(tracker.getCampaign().getUnit(unitID).getEntity().getExternalIdAsString());
                    }
                    forceFound = true;
                    break;
                }
            }
            
            // if we've found the objective's force in the campaign's force list, we can move on to the next one
            if(forceFound) {
                continue;
            }
            
            if(tracker.getScenario() instanceof AtBScenario) {
                AtBScenario scenario = (AtBScenario) tracker.getScenario();
                
                for(int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
                    BotForce botForce = scenario.getBotForce(botIndex);
                    
                    if(forceName.equals(botForce.getName())) {
                        for(Entity entity : botForce.getEntityList()) {
                            objectiveUnitIDs.add(entity.getExternalIdAsString());
                        }
                        forceFound = true;
                        break;
                    }
                }
            }
        }
        
        return objectiveUnitIDs;
    }
    
    /**
     * Evaluates whether the given list of units meets the given objective.
     * @return The list of units that qualify for the given objective.
     */
    private Set<String> evaluateObjective(ScenarioObjective objective, Set<String> objectiveUnitIDs, 
            Collection<Entity> units, ResolveScenarioTracker tracker) {
        Set<String> qualifyingUnits = new HashSet<>();
        
        for(Entity unit : units) {
            // the "opponent" depends on whether the given unit was opposed to the player or not
            boolean isFriendlyUnit =
                    tracker.getScenario().isFriendlyUnit(unit, tracker.getCampaign());
            
            boolean opponentHasBattlefieldControl = 
                    isFriendlyUnit ?
                            !tracker.playerHasBattlefieldControl() :
                            tracker.playerHasBattlefieldControl();
            
            if(entityMeetsObjective(unit, objective, objectiveUnitIDs, opponentHasBattlefieldControl)) {
                qualifyingUnits.add(unit.getExternalIdAsString());
            }
        }
        
        return qualifyingUnits;
    }
    
    /**
     * Update the objective qualification status of a given entity based on its current state and
     * on whether or not it has "escaped". Assumes that the entity is potentially eligible for the objective.
     * @param entity
     * @param forceEntityEscape Whether the entity was marked as 'escaped', regardless of entity status
     * @param forceEntityDestruction Whether the entity was marked as 'destroyed', regardless of entity status
     * @param opponentHasBattlefieldControl
     */
    public void updateObjectiveEntityState(Entity entity, boolean forceEntityEscape, 
            boolean forceEntityDestruction, boolean opponentHasBattlefieldControl) {
        for(ScenarioObjective objective : potentialObjectiveUnits.keySet()) {
            boolean entityMeetsObjective = false;
            
            if(potentialObjectiveUnits.get(objective).contains(entity.getExternalIdAsString())) {
                switch(objective.getObjectiveCriterion()) {
                case Destroy:
                    entityMeetsObjective = forceEntityDestruction || 
                        !forceEntityEscape && entityIsDestroyed(entity, opponentHasBattlefieldControl);
                    break;
                case ForceWithdraw:
                    entityMeetsObjective = forceEntityDestruction ||
                        !forceEntityEscape && entityIsForcedWithdrawal(entity);
                    break;
                case Capture:
                    entityMeetsObjective = !forceEntityEscape && entityIsCaptured(entity, opponentHasBattlefieldControl);
                    break;
                case PreventReachMapEdge:
                    entityMeetsObjective = forceEntityDestruction ||
                        !entityHasReachedDestinationEdge(entity, objective, opponentHasBattlefieldControl);
                    break;
                case Preserve:
                    entityMeetsObjective = forceEntityEscape || 
                        !forceEntityDestruction && !entityIsDestroyed(entity, opponentHasBattlefieldControl);
                    break;
                case ReachMapEdge:
                    entityMeetsObjective = forceEntityEscape ||
                        !forceEntityDestruction && entityHasReachedDestinationEdge(entity, objective, opponentHasBattlefieldControl);
                    break;
                // criteria that we have no way of tracking will not be doing any updates
                default:
                    continue;
                }
            }
            
            if(entityMeetsObjective) {
                qualifyingObjectiveUnits.get(objective).add(entity.getExternalIdAsString());
            } else {
                qualifyingObjectiveUnits.get(objective).remove(entity.getExternalIdAsString());
            }
        }
    }
    
    /**
     * Determines if the given entity has met the given objective
     * @param entity Entity to check
     * @param objective Objective to check
     * @param opponentHasBattlefieldControl Whether the entity's opponent has battlefield control
     */
    private boolean entityMeetsObjective(Entity entity, ScenarioObjective objective, 
            Set<String> objectiveUnitIDs, boolean opponentHasBattlefieldControl) {
        if(objectiveUnitIDs.contains(entity.getExternalIdAsString())) {
            switch(objective.getObjectiveCriterion()) {
            case Destroy:
                return entityIsDestroyed(entity, opponentHasBattlefieldControl);
            case ForceWithdraw:
                return entityIsForcedWithdrawal(entity);
            case Capture:
                return entityIsCaptured(entity, !opponentHasBattlefieldControl);
            case PreventReachMapEdge:
                return !entityHasReachedDestinationEdge(entity, objective, opponentHasBattlefieldControl);
            case Preserve:
                return !entityIsDestroyed(entity, opponentHasBattlefieldControl);
            case ReachMapEdge:
                return entityHasReachedDestinationEdge(entity, objective, opponentHasBattlefieldControl);
            default:
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Check whether we should consider an entity as being destroyed for the purposes of a Destroy objective.
     */
    private boolean entityIsDestroyed(Entity entity, boolean opponentHasBattlefieldControl) {
        // "destroy" is a kill
        // it's destroyed if it's destroyed, or if it's been disabled and the enemy has no chance to recover it
        return entity.isDestroyed() || (entity.getCrew().isDead() || entity.isImmobile()) && !opponentHasBattlefieldControl;
    }
    
    /**
     * Check whether we should consider an entity as being forced to withdraw for the purposes of a ForceWithdraw objective
     */
    private boolean entityIsForcedWithdrawal(Entity entity) {
        // we consider an entity force-withdrawn if it's destroyed, crippled, or run off the field
        // note: immobility and having a dead crew are captured within 'crippled'
        return entity.isDestroyed() || entity.isCrippled(true) || entity.getRetreatedDirection() != OffBoardDirection.NONE;
    }
    
    /**
     * Check whether we should consider an entity as being captured for the purposes of a Capture objective.
     */
    private boolean entityIsCaptured(Entity entity, boolean opponentHasBattlefieldControl) {
        // we consider an entity captured if it's been immobilized but not destroyed and hasn't left the field
        // obviously can't capture it if we don't control the battlefield
        return entity.isImmobile() && !entity.isDestroyed() && 
                entity.getRetreatedDirection() == OffBoardDirection.NONE && !opponentHasBattlefieldControl;
    }
    
    /**
     * Check whether or not the entity can be considered as having reached the destination edge in the given objective
     */
    private boolean entityHasReachedDestinationEdge(Entity entity, ScenarioObjective objective, boolean opponentHasBattlefieldControl) {
        // we've reached the edge if it hasn't gotten to the destination edge
        // or the opponent has been routed and the entity hasn't been shot to hell
        return entity.getRetreatedDirection() == objective.getDestinationEdge() ||
                (!entity.isCrippled() && !entity.isDestroyed() && !opponentHasBattlefieldControl);
    }
    
    /**
     * Processes a particular scenario objective, applying its effects to the 
     * current mission and campaign as necessary
     * @param objective The objective to process.
     * @param qualifyingUnitCount How many units qualified for the objective, used to scale the objective effect if necessary
     * @param completionOverride If null, objective completion is calculated dynamically, otherwise a fixed objective completion state.
     */
    public int determineScenarioStatus(Scenario scenario, Map<ScenarioObjective, Boolean> objectiveOverrides, Map<ScenarioObjective, Integer> objectiveUnitCounts) {
        int victoryScore = 0;
        
        for(ScenarioObjective objective : scenario.getScenarioObjectives()) {
            boolean objectiveMet = objectiveOverrides.containsKey(objective) && objectiveOverrides.get(objective) != null ?
                objectiveOverrides.get(objective) : objectiveMet(objective, objectiveUnitCounts.get(objective));
                
            List<ObjectiveEffect> objectiveEffects = objectiveMet ? objective.getSuccessEffects() : objective.getFailureEffects();
            
            for(ObjectiveEffect effect : objectiveEffects) {
                if(effect.effectType == ObjectiveEffectType.ScenarioVictory) {
                    victoryScore++;
                } else if(effect.effectType == ObjectiveEffectType.ScenarioDefeat) {
                    victoryScore--;
                }
            }
        }
        
        if(victoryScore > 0) {
            return Scenario.S_VICTORY;
        } else if(victoryScore < 0) {
            return Scenario.S_DEFEAT;
        } else {
            return Scenario.S_DRAW;
        }
    }
    
    /**
     * Processes a particular scenario objective, applying its effects to the 
     * current mission and campaign as necessary
     * @param objective The objective to process.
     * @param qualifyingUnitCount How many units qualified for the objective, used to scale the objective effect if necessary
     * @param completionOverride If null, objective completion is calculated dynamically, otherwise a fixed objective completion state.
     */
    public void processObjective(ScenarioObjective objective, int qualifyingUnitCount, Boolean completionOverride,
            ResolveScenarioTracker tracker) {
        // if we've overriden the objective completion flag, great, otherwise, calculate it here
        boolean objectiveMet = completionOverride == null ? objectiveMet(objective, qualifyingUnitCount) : completionOverride;
        
        List<ObjectiveEffect> objectiveEffects = objectiveMet ? objective.getSuccessEffects() : objective.getFailureEffects();
        
        int numUnitsMetObjective = qualifyingUnitCount;
        
        // in some cases, the "qualifying unit count" needs to be inverted.
        if(objective.getObjectiveCriterion() == ObjectiveCriterion.Preserve ||
                objective.getObjectiveCriterion() == ObjectiveCriterion.PreventReachMapEdge) {
            numUnitsMetObjective = potentialObjectiveUnits.get(objective).size() - qualifyingUnitCount;
        }
        
        for(ObjectiveEffect effect : objectiveEffects) {
            processObjectiveEffect(effect, numUnitsMetObjective, tracker);
        }
    }
    
    /**
     * Processes an individual objective effect.
     * @param effect
     * @param scaleFactor If it's scaled, how much to scale it by
     * @param tracker
     */
    private void processObjectiveEffect(ObjectiveEffect effect, int scaleFactor, ResolveScenarioTracker tracker) {
        switch(effect.effectType) {
        case ScenarioVictory:
        case ScenarioDefeat:
            // these do not require any additional effects to the campaign
            break;
        case ContractScoreUpdate:
            // if atb contract, update contract score by how many units met criterion * scaling
            if(tracker.getMission() instanceof AtBContract) {
                AtBContract contract = (AtBContract) tracker.getMission();
                
                int scoreEffect = effect.getEffect() * scaleFactor;
                contract.setContractScoreArbitraryModifier(contract.getContractScoreArbitraryModifier() + scoreEffect);
            }
            break;
        case SupportPointUpdate:
            break;
        case ContractMoraleUpdate:
            break;
        case ContractVictory:
            break;
        case ContractDefeat:
            break;
        case BVBudgetUpdate:
            break;
        case AtBBonus:
            if(tracker.getMission() instanceof AtBContract) {
                AtBContract contract = (AtBContract) tracker.getMission();
                
                int numBonuses = effect.getEffect() * scaleFactor;
                for(int x = 0; x < numBonuses; x++) {
                    contract.doBonusRoll(tracker.getCampaign());
                }
            }
        }
    }
    
    public boolean objectiveMet(ScenarioObjective objective, int qualifyingUnitCount) {
        double potentialObjectiveUnitCount = (double) getPotentialObjectiveUnits().get(objective).size();
        
        return qualifyingUnitCount / potentialObjectiveUnitCount>= (double) objective.getPercentage() / 100;
    }
    
    public Map<ScenarioObjective, Set<String>> getQualifyingObjectiveUnits() {
        return qualifyingObjectiveUnits;
    }

    public Map<ScenarioObjective, Set<String>> getPotentialObjectiveUnits() {
        return potentialObjectiveUnits;
    }
}
