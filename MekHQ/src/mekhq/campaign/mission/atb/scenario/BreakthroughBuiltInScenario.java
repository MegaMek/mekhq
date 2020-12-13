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

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.OffBoardDirection;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class BreakthroughBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = -4258866789358298684L;

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
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        if (isAttacker()) {
            playerHome = Compute.d6() > 3 ? Board.START_S : Board.START_N;
            setStart(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(AtBDynamicScenarioFactory.getOppositeEdge(playerHome));
        } else {
            setStart(Board.START_CENTER);
            playerHome = Board.START_N;
            enemyStart = Compute.d6() > 3 ? Board.START_S : Board.START_N;

            setEnemyHome(AtBDynamicScenarioFactory.getOppositeEdge(enemyStart));
        }

        BotForce allyEntitiesForce = null;

        if (allyEntities.size() > 0) {
            allyEntitiesForce = getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities);
            addBotForce(allyEntitiesForce);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        BotForce botForce = getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities);

        try {
            if (isAttacker()) {
                if (null != allyEntitiesForce) {
                    allyEntitiesForce
                            .setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                    allyEntitiesForce.setDestinationEdge(AtBDynamicScenarioFactory.getOppositeEdge(getStart()));
                }
            } else {
                botForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                botForce.setDestinationEdge(getEnemyHome());
            }
        } catch (PrincessException e) {
            MekHQ.getLogger().error(e);
        }

        addBotForce(botForce);
    }

    @Override
    public boolean canAddDropShips() {
        return !isAttacker() && (Compute.d6() == 1);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = isAttacker()
                ? CommonObjectiveFactory.getBreakthrough(contract, this, campaign, 66,
                        OffBoardDirection.getOpposite(OffBoardDirection.translateBoardStart(getStart())))
                : CommonObjectiveFactory.getPreventEnemyBreakthrough(contract, 50,
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
