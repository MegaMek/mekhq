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

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

import java.util.ArrayList;
import java.util.UUID;

@AtBScenarioEnabled
public class ConvoyRescueBuiltInScenario extends AtBScenario {
    private static String CONVOY_FORCE_ID = "Convoy";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return CONVOYRESCUE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Convoy Rescue";
    }

    @Override
    public String getResourceKey() {
        return "convoyRescue";
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
        setMap("Convoy");
        setTerrainType("Forest");
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
        setStartingPos(Board.START_N);
        setDeploymentDelay(7);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
                    EntityWeightClass.WEIGHT_LIGHT, campaign));
        }

        ArrayList<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 12, campaign);

        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }

        addBotForce(new BotForce(CONVOY_FORCE_ID, 1, Board.START_CENTER, otherForce), campaign);

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), UnitType.MEK,
                    AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, getContract(campaign).getEnemy()),
                    campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_S, enemyEntities), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 50, false);
        ScenarioObjective keepConvoyAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(CONVOY_FORCE_ID, 1,
                1, true);

        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepConvoyAlive.addSuccessEffect(bonusEffect);
        keepConvoyAlive.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"),
                bonusEffect.howMuch));

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepConvoyAlive);
    }
}
