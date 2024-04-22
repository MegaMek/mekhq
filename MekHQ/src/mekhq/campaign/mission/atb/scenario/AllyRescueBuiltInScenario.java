/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission.atb.scenario;

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

import java.util.ArrayList;
import java.util.List;

@AtBScenarioEnabled
public class AllyRescueBuiltInScenario extends AtBScenario {
    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return ALLYRESCUE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Ally Rescue";
    }

    @Override
    public String getResourceKey() {
        return "allyRescue";
    }

    @Override
    public int getMapX() {
        return 65;
    }

    @Override
    public int getMapY() {
        return 45;
    }

    @Override
    public void setMapFile() {
        setMap("Ally-rescue");
        setTerrainType("Urban");
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public boolean canRerollMap() {
        return false;
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {

        setStartingPos(Board.START_S);
        setDeploymentDelay(12);

        final AtBContract contract = getContract(campaign);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(contract.getEmployerCode(), contract.getAllySkill(),
                    contract.getAllyQuality(), UnitType.MEK,
                    AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, contract.getEmployerFaction()),
                    campaign));
        }

        List<Entity> otherForce = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            int weightClass;
            do {
                weightClass = AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, contract.getEmployerFaction());
            } while (weightClass >= EntityWeightClass.WEIGHT_ASSAULT);
            otherForce.add(getEntity(contract.getEmployerCode(), contract.getAllySkill(),
                    contract.getAllyQuality(), UnitType.MEK, weightClass, campaign));
        }

        addBotForce(new BotForce(contract.getAllyBotName(), 1, Board.START_CENTER, otherForce), campaign);

        for (int i = 0; i < 12; i++) {
            int weightClass;
            do {
                weightClass = AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, contract.getEnemy());
            } while (weightClass <= EntityWeightClass.WEIGHT_LIGHT);
            enemyEntities.add(getEntity(contract.getEnemyCode(), contract.getEnemySkill(),
                    contract.getEnemyQuality(), UnitType.MEK, weightClass, campaign));
        }

        addBotForce(getEnemyBotForce(contract, Board.START_N, enemyEntities), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 50, false);

        // in addition to the standard destroy 50/preserve 50, you need to keep
        // at least 3/8 of the "allied" units alive.
        ScenarioObjective keepAlliesAlive = new ScenarioObjective();
        keepAlliesAlive.setFixedAmount(3);
        keepAlliesAlive.setDescription(
                String.format(defaultResourceBundle.getString("commonObjectives.preserveFriendlyUnits.text"),
                        keepAlliesAlive.getFixedAmount(), ""));
        keepAlliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);

        keepAlliesAlive.addForce(contract.getAllyBotName());

        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        keepAlliesAlive.addFailureEffect(friendlyFailureEffect);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepAlliesAlive);
    }
}
