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
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;

@AtBScenarioEnabled
public class StarLeagueCache2BuiltInScenario extends StarLeagueCache1BuiltInScenario {
    @Override
    public int getScenarioType() {
        return STARLEAGUECACHE2;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Star League Cache 2";
    }

    @Override
    public String getResourceKey() {
        return "starLeagueCache2";
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        setStartingPos(Board.START_N);
        int enemyStart = Board.START_S;

        for (int weight = EntityWeightClass.WEIGHT_ULTRA_LIGHT; weight <= EntityWeightClass.WEIGHT_COLOSSAL; weight++) {
            enemyEntities = new ArrayList<>();
            MechSummary ms = campaign.getUnitGenerator().generate("SL", UnitType.MEK, weight, 2750,
                    (Compute.d6() == 6) ? IUnitRating.DRAGOON_A : IUnitRating.DRAGOON_D);

            if (ms != null) {
                enemyEntities.add(AtBDynamicScenarioFactory.createEntityWithCrew(getContract(campaign).getEnemyCode(),
                        getContract(campaign).getEnemySkill(), campaign, ms));
            } else {
                enemyEntities.add(null);
            }

            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecialScenarioEnemies().get(0)), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        getScenarioObjectives().clear();

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1,100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 100, false);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
