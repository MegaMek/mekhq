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

package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class AmbushBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 1223302967912696039L;

    @Override
    public boolean isSpecialMission() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return AMBUSH;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Mission: Ambush";
    }

    @Override
    public String getResourceKey() {
        return "ambush";
    }

    @Override
    public void setMapFile() {
        setMap("Savannah");
        setTerrainType(TER_FLATLANDS);
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        setStart(Board.START_CENTER);
        int enemyStart = Board.START_CENTER;

        for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
            enemyEntities = new ArrayList<Entity>();
            if (weight == EntityWeightClass.WEIGHT_LIGHT) {
                enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                        getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));

                enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                        getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
            } else {
                for (int i = 0; i < 3; i++) {
                    enemyEntities
                            .add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                                    getContract(campaign).getEnemyQuality(), UnitType.MEK, weight - 1, campaign));
                }
            }

            getSpecMissionEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 66);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                100, false);

        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
    }
}
