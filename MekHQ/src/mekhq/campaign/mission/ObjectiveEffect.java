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
package mekhq.campaign.mission;

/**
 * A data structure containing metadata relevant to the effect that completing or failing an objective can have.
 *
 * @author NickAragua
 */
public class ObjectiveEffect {

    /**
     * The possible type of effect scaling effects, aka what you multiply the effect by.
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
         *  contributes a "Scenario Victory Point" towards the scenario's victory/defeat state
         */
        ScenarioVictory("+%d Scenario VP", true),
        /*
         *  contributes a "negative Scenario Victory Point/s" towards the scenario's victory/defeat state
         */
        ScenarioDefeat("-%d Scenario VP", true),
        /*
         *  changes the contract score
         */
        ContractScoreUpdate("%d Contract Score/Campaign VP", true),
        /* changes the number of support points (not implemented yet)
         *
         */
        SupportPointUpdate("%d Support Points", true),
        /* changes the size of supply cache
         *
         */
        SupplyCache("%d Looted Supplies", true),
        /*
         *  changes the contract morale up or down
         */
        ContractMoraleUpdate("%d Contract Morale", true),
        /*
         *  insta-win the contract (player still has to manually complete it)
         */
        ContractVictory("Early Contract Victory/Temporary Rout", false),
        /*
         *  insta-lose the contract (player still has to manually complete it)
         */
        ContractDefeat("Early Contract Loss", false),
        /*
         *  update the BV budget multiplier for template scenarios (not implemented yet)
         */
        BVBudgetUpdate("%d%% BV budget increase", true),
        /*
         *  roll an AtB-style "bonus"
         */
        AtBBonus("%d AtB bonus roll(s)", true),

        /*
         * In StratCon, relevant if scenario is about a facility, said facility remains in play.
         */
        FacilityRemains("Facility remains with current controller", false),

        /*
         * In StratCon, relevant if scenario is about a facility, said facility is removed from play.
         */
        FacilityRemoved("Facility destroyed and removed from area of operations", false),

        /*
         * In StratCon, relevant if scenario is about a facility, said facility changes ownership.
         */
        FacilityCaptured("Facility captured and changes controller", false);

        private final String descriptiveText;
        private boolean magnitudeIsRelevant;

        /**
         * Whether the scaling is relevant for this particular objective effect type - e.g. it doesn't matter how many
         * times you destroy a facility, it's still destroyed
         */
        public boolean isMagnitudeRelevant() {
            return magnitudeIsRelevant;
        }

        @Override
        public String toString() {
            return descriptiveText;
        }

        ObjectiveEffectType(String description, boolean magnitudeIsRelevant) {
            descriptiveText = description;
            this.magnitudeIsRelevant = magnitudeIsRelevant;
        }
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

        if (effectType.isMagnitudeRelevant()) {
            sb.append(" - ");
            sb.append(effectScaling.toString());
            sb.append(" - ");
            sb.append(howMuch);
        }
        return sb.toString();
    }
}
