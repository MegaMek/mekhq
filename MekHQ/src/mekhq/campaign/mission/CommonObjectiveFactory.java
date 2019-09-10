package mekhq.campaign.mission;

import java.util.UUID;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;

/**
 * This class contains code for the creation of some common objectives for an AtB scenario
 * @author NickAragua
 *
 */
public class CommonObjectiveFactory {
    /**
     * Generates a "keep the attached units alive" objective that applies to 
     * attached liaisons, trainees, house units and integrated units, giving a -1 contract score penalty for each
     * one that gets totaled. Does not include dropships.
     */
    public static ScenarioObjective getKeepAttachedGroundUnitsAlive(AtBContract contract, AtBScenario scenario) {
        ScenarioObjective keepAttachedUnitsAlive = new ScenarioObjective();
        keepAttachedUnitsAlive.setDescription("The following unit(s) deployed by your employer must survive. Each one destroyed results in a 1 point penalty to your contract score:");
        keepAttachedUnitsAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        keepAttachedUnitsAlive.setPercentage(100);

        addEmployerUnitsToObjective(scenario, contract, keepAttachedUnitsAlive);
        
        if(keepAttachedUnitsAlive.getAssociatedForceNames().size() == 0 && 
                keepAttachedUnitsAlive.getAssociatedUnitIDs().size() == 0) {
            return null;
        }
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ContractScoreUpdate;
        failureEffect.scaledEffect = true;
        failureEffect.howMuch = -1;
        
        keepAttachedUnitsAlive.addFailureEffect(failureEffect);
        
        return keepAttachedUnitsAlive;
    }

    /**
     * Generates a "keep at least X% or X of [force] units" objective from the bot force with the specified name
     */
    public static ScenarioObjective getPreserveSpecificFriendlies(String forceName, int number, boolean fixedAmount) {
        ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
        if(fixedAmount) {
            keepFriendliesAlive.setDescription(String.format("Ensure that at least %d unit of the following force(s) and unit(s) survive:", number));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format("Ensure that at least %d%% of the following force(s) and unit(s) survive:", number));
            keepFriendliesAlive.setPercentage(number);
        }
        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        keepFriendliesAlive.addForce(forceName);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        keepFriendliesAlive.addSuccessEffect(successEffect);
        
        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        keepFriendliesAlive.addFailureEffect(friendlyFailureEffect);  
        
        return keepFriendliesAlive;
    }
    
    /**
     * Generates a "keep at least X% of all units" objective from the primary player force,
     * as well as any attached allies, alive
     * @param campaign
     * @param contract
     * @param scenario
     * @return
     */
    public static ScenarioObjective getKeepFriendliesAlive(Campaign campaign, AtBContract contract, AtBScenario scenario, 
            int number, boolean fixedAmount) {
        ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
        if(fixedAmount) {
            keepFriendliesAlive.setDescription(String.format("Ensure that at least %d unit of the following force(s) and unit(s) survive:", number));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format("Ensure that at least %d%% of the following force(s) and unit(s) survive:", number));
            keepFriendliesAlive.setPercentage(number);
        }
        
        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);

        addAssignedPlayerUnitsToObjective(scenario, campaign, keepFriendliesAlive);
        addEmployerUnitsToObjective(scenario, contract, keepFriendliesAlive);

        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        keepFriendliesAlive.addSuccessEffect(successEffect);
        
        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        keepFriendliesAlive.addFailureEffect(friendlyFailureEffect);  
        
        return keepFriendliesAlive;
    }

    /**
     * Generates a "destroy x% of all units" from the given force name objective
     * @param forcename Explicit enemy force name
     * @return
     */
    public static ScenarioObjective getDestroyEnemies(String forcename, int percentage) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(String.format("Destroy, cripple or force the withdrawal of at least %d%% of the following enemy force(s):", percentage));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.ForceWithdraw);
        destroyHostiles.setPercentage(percentage);
        destroyHostiles.addForce(forcename);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        destroyHostiles.addSuccessEffect(successEffect);
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        destroyHostiles.addFailureEffect(failureEffect);
        
        return destroyHostiles;
    }
    
    /** 
     * Generates a "destroy x% of all units" from the primary opposing force objective
     * @param contract Contract to examine for enemy force name.
     */
    public static ScenarioObjective getDestroyEnemies(AtBContract contract, int percentage) {
        return getDestroyEnemies(contract.getEnemyBotName(), percentage);
    }
    
    /** 
     * Generates a "destroy x% of all units" from the primary opposing force objective
     * @param contract
     * @param percentage
     * @return
     */
    public static ScenarioObjective getPreventEnemyBreakthrough(AtBContract contract, int percentage, OffBoardDirection direction) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(
                String.format("Prevent at least %d%% of the following enemy force(s) from reaching the %s edge:", percentage, direction));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.PreventReachMapEdge);
        destroyHostiles.setPercentage(percentage);
        destroyHostiles.setDestinationEdge(direction);
        destroyHostiles.addForce(contract.getEnemyBotName());
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        destroyHostiles.addSuccessEffect(successEffect);
        
        return destroyHostiles;
    }
    
    /** 
     * Generates a "reach X edge with x% of all allied + player units" objective
     */
    public static ScenarioObjective getBreakthrough(AtBContract contract, AtBScenario scenario, Campaign campaign, 
            int percentage, OffBoardDirection direction) {
        ScenarioObjective breakthrough = new ScenarioObjective();
        breakthrough.setDescription(
                String.format("Reach the %s edge with at least %d%% of the following player and attached units and allied force(s):", direction, percentage));
        breakthrough.setObjectiveCriterion(ObjectiveCriterion.ReachMapEdge);
        breakthrough.setPercentage(percentage);
        breakthrough.setDestinationEdge(direction);
     
        addAssignedPlayerUnitsToObjective(scenario, campaign, breakthrough);
        addEmployerUnitsToObjective(scenario, contract, breakthrough);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        breakthrough.addSuccessEffect(successEffect);
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        breakthrough.addFailureEffect(failureEffect);
        
        return breakthrough;
    }
    
    /**
     * Worker function - adds designated lance or currently assigned player units to objective
     */
    private static void addAssignedPlayerUnitsToObjective(AtBScenario scenario, Campaign campaign, ScenarioObjective objective) {
        int expectedNumUnits = AtBDynamicScenarioFactory.getLanceSize(campaign.getFactionCode());
        if(scenario.isBigBattle()) {
            expectedNumUnits *= 2;
        } else if(scenario.isSpecialMission()) {
            expectedNumUnits = 1;
        }
        
        // some scenarios have a lance assigned
        // some scenarios have individual units assigned
        if(scenario.getLanceForceId() != AtBScenario.NO_LANCE) {
            objective.addForce(campaign.getForce(scenario.getLanceForceId()).getName());
        } else {
            int unitCount = 0;
            
            // mildly hack-ish:
            // just take the first X number of assigned units
            for(UUID unitID : scenario.getForces(campaign).getAllUnits()) {
                objective.addUnit(unitID.toString());
                unitCount++;
                
                if(unitCount > expectedNumUnits) {
                    break;
                }
            }
        }
    }
    
    private static void addEmployerUnitsToObjective(AtBScenario scenario, AtBContract contract, ScenarioObjective objective) {
        for(int botForceID = 0; botForceID < scenario.getNumBots(); botForceID++) {
            // kind of hack-ish:
            // if there's an allied bot that shares employer name, then add it to the survival objective
            // we know there's only one of those, so break out of the loop when we see it
            if(scenario.getBotForce(botForceID).getName().equals(contract.getAllyBotName())) {
                objective.addForce(contract.getAllyBotName());
                break;
            }
        }

        for(Entity attachedAlly : scenario.getAlliesPlayer()) {
            // only attach non-dropship units
            if(!(attachedAlly instanceof Dropship)) {
                objective.addUnit(attachedAlly.getExternalIdAsString());
            }
        }
    }
}
