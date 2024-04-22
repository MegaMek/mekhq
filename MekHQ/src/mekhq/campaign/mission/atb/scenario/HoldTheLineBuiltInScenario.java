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
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class HoldTheLineBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return HOLDTHELINE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Hold the Line";
    }

    @Override
    public String getResourceKey() {
        return "holdTheLine";
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        if (isAttacker()) {
            playerHome = startPos[Compute.randomInt(4)];
            setStartingPos(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(playerHome + 4);

            if (getEnemyHome() > 8) {
                setEnemyHome(getEnemyHome() - 8);
            }
        } else {
            setStartingPos(Board.START_CENTER);
            enemyStart = startPos[Compute.randomInt(4)];
            setEnemyHome(enemyStart);
            playerHome = getEnemyHome() + 4;

            if (playerHome > 8) {
                playerHome -= 8;
            }
        }

        if (!allyEntities.isEmpty()) {
            addBotForce(getAllyBotForce(getContract(campaign), getStartingPos(), playerHome, allyEntities), campaign);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_ASSAULT,
                isAttacker() ? 0 : 4, 0, campaign);

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities), campaign);
    }

    @Override
    public boolean canAddDropShips() {
        return isAttacker() && (Compute.d6() == 1);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        // Attacker must destroy 50% and keep 66% alive
        // Defender must destroy 33% and keep 50% alive
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1,
                isAttacker() ? 50 : 33);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(
                campaign, contract, this, 1, isAttacker() ? 66 : 50,false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
