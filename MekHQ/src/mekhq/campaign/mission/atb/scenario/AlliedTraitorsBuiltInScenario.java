/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class AlliedTraitorsBuiltInScenario extends AtBScenario {
    @Override
    public boolean isSpecialScenario() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return ALLIEDTRAITORS;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Allied Traitors";
    }

    @Override
    public String getResourceKey() {
        return "alliedTraitors";
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        setStartingPos(Board.START_CENTER);
        int enemyStart = Board.START_CENTER;

        for (int weight = EntityWeightClass.WEIGHT_ULTRA_LIGHT; weight <= EntityWeightClass.WEIGHT_COLOSSAL; weight++) {
            enemyEntities = new ArrayList<>();
            enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                    getContract(campaign).getAllyQuality(), UnitType.MEK, weight, campaign));

            enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                    getContract(campaign).getAllyQuality(), UnitType.MEK, weight, campaign));

            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(
                new BotForce(getContract(campaign).getAllyBotName(), 2, enemyStart, getSpecialScenarioEnemies().get(0)), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        String allyBotName = getContract(campaign).getAllyBotName();

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 100);
        // this is a special case where the target is actually the "allied" bot.
        destroyHostiles.clearForces();
        destroyHostiles.addForce(allyBotName);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 1, 100, false);
        keepFriendliesAlive.removeForce(allyBotName);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
