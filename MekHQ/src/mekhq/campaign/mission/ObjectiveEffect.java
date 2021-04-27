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

/**
 * A data structure containing metadata relevant to the effect that completing or failing an objective 
 * can have. 
 * @author NickAragua
 *
 */
public class ObjectiveEffect {
    
    /**
     * The possible type of effect scaling effects, 
     * aka what you multiply the effect by.
     */
    public enum EffectScalingType {
        /*
         *  no scaling, effect is just applied "howMuch" times
         */
        Fixed,
        /*
         *  linear scaling, effect is applied based on how many units qualified for the objective
         */
        Linear,
        /*
         *  inverted scaling, effect is applied based on how many units did not qualify for the objective
         */
        Inverted
    }
    
    /**
     * The behavior of the application when the objective effect is applied
     */
    public enum ObjectiveEffectType {
        /*
         *  contributes a "victory point" towards the scenario's victory/defeat state
         */
        ScenarioVictory,
        /*
         *  contributes a "negative victory point" towards the scenario's victory/defeat state
         */
        ScenarioDefeat,
        /*
         *  changes the contract score
         */
        ContractScoreUpdate,
        /* changes the number of support points (not implemented yet)
         * 
         */
        SupportPointUpdate,
        /*
         *  changes the contract morale up or down
         */
        ContractMoraleUpdate,
        /*
         *  insta-win the contract (player still has to manually complete it)
         */
        ContractVictory,
        /*
         *  insta-lose the contract (player still has to manually complete it)
         */
        ContractDefeat,
        /*
         *  update the BV budget multiplier for template scenarios (not implemented yet)
         */
        BVBudgetUpdate,
        /*
         *  roll an AtB-style "bonus"
         */
        AtBBonus,
        
        /*
         * In StratCon, relevant if scenario is about a facility, said facility remains in play.
         */
        FacilityRemains,
        
        /*
         * In StratCon, relevant if scenario is about a facility, said facility is removed from play.
         */
        FacilityRemoved,
        
        /*
         * In StratCon, relevant if scenario is about a facility, said facility changes ownership.
         */
        FacilityCaptured
    }        
    
    /**
     * Possible conditions under which an objective effect may be triggered
     */
    public enum ObjectiveEffectConditionType {
        /**
         * An effect triggered when the associated objective is fulfilled
         */
        ObjectiveSuccess,
        
        /**
         * An effect triggered when the associated objective is not fulfilled
         */
        ObjectiveFailure
    }
    
    public ObjectiveEffectType effectType;
    // whether the effect is scaled to the # of units or fixed in nature
    public EffectScalingType effectScaling = EffectScalingType.Fixed;
    // how much of the effect per unit, or how much of the effect fixed
    public int howMuch;
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(effectType.toString());
        sb.append(" - ");
        sb.append(effectScaling.toString());
        sb.append(" - ");
        sb.append(howMuch);
        return sb.toString();
    }
}
