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

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;

@AtBScenarioEnabled
public class BreakthroughBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return BREAKTHROUGH;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Breakthrough";
    }

    @Override
    public String getResourceKey() {
        return "breakthrough";
    }

    @Override
    public int getMapX() {
        // make it a little wider for bigger scenarios
        return 18 + this.getLanceCount();
    }

    @Override
    public int getMapY() {
        return 50;
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        if (isAttacker()) {
            playerHome = Compute.d6() > 3 ? Board.START_S : Board.START_N;
            setStartingPos(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(AtBDynamicScenarioFactory.getOppositeEdge(playerHome));
        } else {
            setStartingPos(Board.START_CENTER);
            playerHome = Board.START_N;
            enemyStart = Compute.d6() > 3 ? Board.START_S : Board.START_N;

            setEnemyHome(AtBDynamicScenarioFactory.getOppositeEdge(enemyStart));
        }

        BotForce allyEntitiesForce = null;

        if (!allyEntities.isEmpty()) {
            allyEntitiesForce = getAllyBotForce(getContract(campaign), getStartingPos(), playerHome, allyEntities);
            addBotForce(allyEntitiesForce, campaign);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        BotForce botForce = getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities);

        try {
            if (isAttacker()) {
                if (null != allyEntitiesForce) {
                    allyEntitiesForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                    allyEntitiesForce.setDestinationEdge(AtBDynamicScenarioFactory.getOppositeEdge(getStartingPos()));
                }
            } else {
                botForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                botForce.setDestinationEdge(getEnemyHome());
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        addBotForce(botForce, campaign);
    }

    @Override
    public boolean canAddDropShips() {
        return !isAttacker() && (Compute.d6() == 1);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = isAttacker()
                ? CommonObjectiveFactory.getBreakthrough(contract, this, campaign, 1, 66,
                        OffBoardDirection.getOpposite(OffBoardDirection.translateBoardStart(getStartingPos())))
                : CommonObjectiveFactory.getPreventEnemyBreakthrough(contract, 1, 50,
                        OffBoardDirection.translateBoardStart(getEnemyHome()));
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        getScenarioObjectives().add(destroyHostiles);
    }

    @Override
    public String getBattlefieldControlDescription() {
        return getResourceBundle().getString("battleDetails.common.defenderControlsBattlefield");
    }
}
