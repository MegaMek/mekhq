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

import static mekhq.campaign.force.CombatTeam.getStandardForceSize;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import megamek.common.OffBoardDirection;
import megamek.common.units.Dropship;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;

/**
 * This class contains code for the creation of some common objectives for an AtB scenario
 *
 * @author NickAragua
 */
public class CommonObjectiveFactory {
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AtBScenarioBuiltIn",
          MekHQ.getMHQOptions().getLocale());

    /**
     * Generates a "keep the attached units alive" objective that applies to attached liaisons, trainees, house units
     * and integrated units, giving a -1 contract score penalty for each one that gets totaled. Does not include
     * dropships.
     */
    public static ScenarioObjective getKeepAttachedGroundUnitsAlive(AtBContract contract, AtBScenario scenario) {
        ScenarioObjective keepAttachedUnitsAlive = new ScenarioObjective();
        keepAttachedUnitsAlive.setDescription(resourceMap.getString("commonObjectives.preserveEmployerUnits.text"));
        keepAttachedUnitsAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        keepAttachedUnitsAlive.setPercentage(100);

        addEmployerUnitsToObjective(scenario, contract, keepAttachedUnitsAlive);

        if (keepAttachedUnitsAlive.getAssociatedForceNames().isEmpty()
                  && keepAttachedUnitsAlive.getAssociatedUnitIDs().isEmpty()) {
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
    public static ScenarioObjective getPreserveSpecificFriendlies(String forceName, int OperationalVP, int number,
          boolean fixedAmount) {
        ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
        if (fixedAmount) {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString(
                  "commonObjectives.preserveFriendlyUnits.text"), number, ""));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString(
                  "commonObjectives.preserveFriendlyUnits.text"), number, '%'));
            keepFriendliesAlive.setPercentage(number);
        }
        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);
        return getScenarioObjective(forceName, OperationalVP, keepFriendliesAlive);
    }

    /**
     * Generates a "keep at least X% of all units" objective from the primary player force, as well as any attached
     * allies, alive
     */
    public static ScenarioObjective getKeepFriendliesAlive(Campaign campaign, AtBContract contract,
          AtBScenario scenario, int OperationalVP, int number,
          boolean fixedAmount) {
        ScenarioObjective keepFriendliesAlive = new ScenarioObjective();
        if (fixedAmount) {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString(
                  "commonObjectives.preserveFriendlyUnits.text"), number, ""));
            keepFriendliesAlive.setFixedAmount(number);
        } else {
            keepFriendliesAlive.setDescription(String.format(resourceMap.getString(
                  "commonObjectives.preserveFriendlyUnits.text"), number, '%'));
            keepFriendliesAlive.setPercentage(number);
        }

        keepFriendliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);

        addAssignedPlayerUnitsToObjective(scenario, campaign, keepFriendliesAlive);
        addEmployerUnitsToObjective(scenario, contract, keepFriendliesAlive);

        return getScenarioObjective(OperationalVP, keepFriendliesAlive);
    }

    /**
     * Generates a "destroy x% of all units" from the given force name objective
     *
     * @param forceName Explicit enemy force name
     */
    public static ScenarioObjective getDestroyEnemies(String forceName, int OperationalVP, int percentage) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(String.format(resourceMap.getString("commonObjectives.forceWithdraw.text"),
              percentage));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.ForceWithdraw);
        destroyHostiles.setPercentage(percentage);
        return getScenarioObjective(forceName, OperationalVP, destroyHostiles);
    }

    @Nonnull
    private static ScenarioObjective getScenarioObjective(String forceName, int OperationalVP,
          ScenarioObjective destroyHostiles) {
        destroyHostiles.addForce(forceName);

        return getScenarioObjective(OperationalVP, destroyHostiles);
    }

    @Nonnull
    private static ScenarioObjective getScenarioObjective(int OperationalVP, ScenarioObjective destroyHostiles) {
        ObjectiveEffect successEffect = new ObjectiveEffect();
        successEffect.effectType = ObjectiveEffectType.ScenarioVictory;
        successEffect.howMuch = OperationalVP;
        destroyHostiles.addSuccessEffect(successEffect);

        ObjectiveEffect failureEffect = new ObjectiveEffect();
        failureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        failureEffect.howMuch = OperationalVP;
        destroyHostiles.addFailureEffect(failureEffect);

        return destroyHostiles;
    }

    /**
     * Generates a "destroy x% of all units" objective from the primary opposing force
     *
     * @param contract Contract to examine for enemy force name.
     */
    public static ScenarioObjective getDestroyEnemies(AtBContract contract, int OperationalVP, int percentage) {
        return getDestroyEnemies(contract.getEnemyBotName(), OperationalVP, percentage);
    }

    /**
     * Generates a "prevent x% of all units from reaching given edge" objective from the primary opposing force
     */
    public static ScenarioObjective getPreventEnemyBreakthrough(AtBContract contract, int OperationalVP, int percentage,
          OffBoardDirection direction) {
        ScenarioObjective destroyHostiles = new ScenarioObjective();
        destroyHostiles.setDescription(
              String.format(resourceMap.getString("commonObjectives.preventBreakthrough.text"), percentage, direction));
        destroyHostiles.setObjectiveCriterion(ObjectiveCriterion.PreventReachMapEdge);
        destroyHostiles.setPercentage(percentage);
        destroyHostiles.setDestinationEdge(direction);
        destroyHostiles.addForce(contract.getEnemyBotName());

        return getScenarioObjective(OperationalVP, destroyHostiles);
    }

    /**
     * Generates a "reach X edge with x% of all allied + player units" objective
     */
    public static ScenarioObjective getBreakthrough(AtBContract contract, AtBScenario scenario, Campaign campaign,
          int OperationalVP,
          int percentage, OffBoardDirection direction) {
        ScenarioObjective breakthrough = new ScenarioObjective();
        breakthrough.setDescription(
              String.format(resourceMap.getString("commonObjectives.breakthrough.text"), direction, percentage));
        breakthrough.setObjectiveCriterion(ObjectiveCriterion.ReachMapEdge);
        breakthrough.setPercentage(percentage);
        breakthrough.setDestinationEdge(direction);

        addAssignedPlayerUnitsToObjective(scenario, campaign, breakthrough);
        addEmployerUnitsToObjective(scenario, contract, breakthrough);

        return getScenarioObjective(OperationalVP, breakthrough);
    }

    /**
     * Worker function - adds designated lance or currently assigned player units to objective
     */
    private static void addAssignedPlayerUnitsToObjective(AtBScenario scenario, Campaign campaign,
          ScenarioObjective objective) {
        int expectedNumUnits = getStandardForceSize(campaign.getFaction());
        if (scenario.isBigBattle()) {
            expectedNumUnits *= 2;
        } else if (scenario.isSpecialScenario()) {
            expectedNumUnits = 1;
        }

        // some scenarios have a lance assigned
        // some scenarios have individual units assigned
        if (scenario.getCombatTeamId() != AtBScenario.NO_COMBAT_TEAM) {
            Formation formation = campaign.getForce(scenario.getCombatTeamId());

            if (formation != null) {
                objective.addForce(campaign.getForce(scenario.getCombatTeamId()).getName());
            }
        } else {
            int unitCount = 0;

            // mildly hack-ish:
            // just take the first X number of assigned units
            for (UUID unitID : scenario.getForces(campaign).getAllUnits(true)) {
                objective.addUnit(unitID.toString());
                unitCount++;

                if (unitCount > expectedNumUnits) {
                    break;
                }
            }
        }
    }

    /**
     * Worker function that adds all employer units in the given scenario (as specified in the contract) to the given
     * objective, except DropShips.
     */
    private static void addEmployerUnitsToObjective(AtBScenario scenario, AtBContract contract,
          ScenarioObjective objective) {
        for (int botForceID = 0; botForceID < scenario.getNumBots(); botForceID++) {
            // kind of hack-ish:
            // if there's an allied bot that shares employer name, then add it to the survival objective
            // we know there's only one of those, so break out of the loop when we see it
            if (scenario.getBotForce(botForceID).getName().equals(contract.getAllyBotName())) {
                objective.addForce(contract.getAllyBotName());
                break;
            }
        }

        scenario.getAlliesPlayer().stream()
              .filter(Objects::nonNull)
              .filter(entity -> !(entity instanceof Dropship))
              .forEach(entity -> objective.addUnit(entity.getExternalIdAsString()));
    }
}
