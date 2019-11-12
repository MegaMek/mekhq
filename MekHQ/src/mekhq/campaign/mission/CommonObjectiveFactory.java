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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.mission;

import java.util.ResourceBundle;
import java.util.UUID;

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;

/**
 * This class contains code for the creation of some common objectives for an AtB scenario
 * @author NickAragua
 *
 */
public class CommonObjectiveFactory {
    private static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AtBScenarioBuiltIn", new EncodeControl());
    
    /**
     * Generates a "keep the attached units alive" objective that applies to 
     * attached liaisons, trainees, house units and integrated units, giving a -1 contract score penalty for each
     * one that gets totaled. Does not include dropships.
     */
    public static ScenarioObjective getKeepAttachedGroundUnitsAlive(AtBContract contract, AtBScenario scenario) {
        ScenarioObjective keepAttachedUnitsAlive = new ScenarioObjective();
        keepAttachedUnitsAlive.setDescription(resourceMap.getString("commonObjectives.preserveEmployerUnits.text"));
        keepAttachedUnitsAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        keepAttachedUnitsAlive.setPercentage(100);

        addEmployerUnitsToObjective(scenario, contract, keepAttachedUnitsAlive);
        
        if(keepAttachedUnitsAlive.getAssociatedForceNames().size() == 0 && 
                keepAttachedUnitsAlive.getAssociatedUnitIDs().size() == 0) {
            return null;
        }
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ContractScoreUpdate;
        failureEffect.effectScaling = EffectScalingType.Inverted;
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
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString("commonObjectives.preserveFriendlyUnits.text"), number, ""));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString("commonObjectives.preserveFriendlyUnits.text"), number, "%"));
            keepFriendliesAlive.setPercentage(number);
        }
        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        keepFriendliesAlive.addForce(forceName);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = 1;
        keepFriendliesAlive.addSuccessEffect(successEffect);
        
        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        friendlyFailureEffect.howMuch = 1;
        keepFriendliesAlive.addFailureEffect(friendlyFailureEffect);  
        
        return keepFriendliesAlive;
    }
    
    /**
     * Generates a "keep at least X% of all units" objective from the primary player force,
     * as well as any attached allies, alive
     */
    public static ScenarioObjective getKeepFriendliesAlive(Campaign campaign, AtBContract contract, AtBScenario scenario, 
            int number, boolean fixedAmount) {
        ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
        if(fixedAmount) {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString("commonObjectives.preserveFriendlyUnits.text"), number, ""));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString("commonObjectives.preserveFriendlyUnits.text"), number, "%"));
            keepFriendliesAlive.setPercentage(number);
        }
        
        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);

        addAssignedPlayerUnitsToObjective(scenario, campaign, keepFriendliesAlive);
        addEmployerUnitsToObjective(scenario, contract, keepFriendliesAlive);

        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = 1;
        keepFriendliesAlive.addSuccessEffect(successEffect);
        
        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        friendlyFailureEffect.howMuch = 1;
        keepFriendliesAlive.addFailureEffect(friendlyFailureEffect);  
        
        return keepFriendliesAlive;
    }

    /**
     * Generates a "destroy x% of all units" from the given force name objective
     * @param forcename Explicit enemy force name
     */
    public static ScenarioObjective getDestroyEnemies(String forcename, int percentage) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(String.format(resourceMap.getString("commonObjectives.forceWithdraw.text"), percentage));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.ForceWithdraw);
        destroyHostiles.setPercentage(percentage);
        destroyHostiles.addForce(forcename);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = 1;
        destroyHostiles.addSuccessEffect(successEffect);
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        failureEffect.howMuch = 1;
        destroyHostiles.addFailureEffect(failureEffect);
        
        return destroyHostiles;
    }
    
    /** 
     * Generates a "destroy x% of all units" objective from the primary opposing force 
     * @param contract Contract to examine for enemy force name.
     */
    public static ScenarioObjective getDestroyEnemies(AtBContract contract, int percentage) {
        return getDestroyEnemies(contract.getEnemyBotName(), percentage);
    }
    
    /** 
     * Generates a "prevent x% of all units from reaching given edge" objective from the primary opposing force 
     */
    public static ScenarioObjective getPreventEnemyBreakthrough(AtBContract contract, int percentage, OffBoardDirection direction) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(
                String.format(resourceMap.getString("commonObjectives.preventBreakthrough.text"), percentage, direction));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.PreventReachMapEdge);
        destroyHostiles.setPercentage(percentage);
        destroyHostiles.setDestinationEdge(direction);
        destroyHostiles.addForce(contract.getEnemyBotName());
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = 1;
        destroyHostiles.addSuccessEffect(successEffect);
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        failureEffect.howMuch = 1;
        destroyHostiles.addFailureEffect(failureEffect);
        
        return destroyHostiles;
    }
    
    /** 
     * Generates a "reach X edge with x% of all allied + player units" objective
     */
    public static ScenarioObjective getBreakthrough(AtBContract contract, AtBScenario scenario, Campaign campaign, 
            int percentage, OffBoardDirection direction) {
        ScenarioObjective breakthrough = new ScenarioObjective();
        breakthrough.setDescription(
                String.format(resourceMap.getString("commonObjectives.breakthrough.text"), direction, percentage));
        breakthrough.setObjectiveCriterion(ObjectiveCriterion.ReachMapEdge);
        breakthrough.setPercentage(percentage);
        breakthrough.setDestinationEdge(direction);
     
        addAssignedPlayerUnitsToObjective(scenario, campaign, breakthrough);
        addEmployerUnitsToObjective(scenario, contract, breakthrough);
        
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = 1;
        breakthrough.addSuccessEffect(successEffect);
        
        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        failureEffect.howMuch = 1;
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
    
    /**
     * Worker function that adds all employer units in the given scenario (as specified in the contract)
     * to the given objective, with the exception of dropships.
     */
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
