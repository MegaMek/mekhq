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

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class StandUpBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return STANDUP;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Stand Up";
    }

    @Override
    public String getResourceKey() {
        return "standup";
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        int playerHome = startPos[Compute.randomInt(4)];
        setStartingPos(playerHome);

        int enemyStart = getStartingPos() + 4;

        if (enemyStart > 8) {
            enemyStart -= 8;
        }

        setEnemyHome(enemyStart);

        if (!allyEntities.isEmpty()) {
            addBotForce(getAllyBotForce(getContract(campaign), getStartingPos(), playerHome, allyEntities), campaign);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), getEnemyHome(), getEnemyHome(), enemyEntities), campaign);
    }

    @Override
    public boolean canAddDropShips() {
        return Compute.d6() <= 2;
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 50, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
