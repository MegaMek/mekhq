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
import megamek.common.EntityWeightClass;
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
public class ChaseBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 1990640303088391209L;

    @Override
    public int getScenarioType() {
        return CHASE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Chase";
    }

    @Override
    public String getResourceKey() {
        return "chase";
    }

    @Override
    public int getMapX() {
        return 18 + getLanceCount();
    }

    @Override
    public int getMapY() {
        return 70;
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        boolean startNorth = Compute.d6() > 3;

        int destinationEdge = startNorth ? Board.START_S : Board.START_N;
        int startEdge = startNorth ? Board.START_N : Board.START_S;

        setStart(startEdge);
        setEnemyHome(destinationEdge);

        BotForce allyEntitiesForce = null;

        if (allyEntities.size() > 0) {
            allyEntitiesForce = getAllyBotForce(getContract(campaign), getStart(), destinationEdge, allyEntities);
            addBotForce(allyEntitiesForce);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_ASSAULT, 0,
                -1, campaign);
        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), EntityWeightClass.WEIGHT_ASSAULT, 0,
                -1, campaign);

        BotForce botForce = getEnemyBotForce(getContract(campaign), startEdge, getEnemyHome(), enemyEntities);

        try {
            if (isAttacker()) {
                if (null != allyEntitiesForce) {
                    allyEntitiesForce
                            .setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());

                    allyEntitiesForce.setDestinationEdge(destinationEdge);
                }
            } else {
                botForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                botForce.setDestinationEdge(destinationEdge);
            }
        } catch (PrincessException e) {
            MekHQ.getLogger().error(e);
        }

        addBotForce(botForce);

        /* All forces deploy in 12 - WP turns */
        setDeploymentDelay(12);

        for (Entity en : allyEntities) {
            int speed = en.getWalkMP();

            if (en.getJumpMP() > 0) {
                if (en instanceof megamek.common.Infantry) {
                    speed = en.getJumpMP();
                } else {
                    speed++;
                }
            }

            en.setDeployRound(Math.max(0, 12 - speed));
        }

        for (Entity en : enemyEntities) {
            int speed = en.getWalkMP();

            if (en.getJumpMP() > 0) {
                if (en instanceof megamek.common.Infantry) {
                    speed = en.getJumpMP();
                } else {
                    speed++;
                }
            }

            en.setDeployRound(Math.max(0, 12 - speed));
        }
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = isAttacker()
                ? CommonObjectiveFactory.getBreakthrough(contract, this, campaign, 50,
                        OffBoardDirection.translateBoardStart(AtBDynamicScenarioFactory.getOppositeEdge(getStart())))
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
