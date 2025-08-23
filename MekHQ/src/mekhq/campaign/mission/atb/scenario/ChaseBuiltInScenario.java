/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.board.Board;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.OffBoardDirection;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ChaseBuiltInScenario extends AtBScenario {
    private static final MMLogger logger = MMLogger.create(ChaseBuiltInScenario.class);

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
        return 18 + getForceCount();
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
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
          ArrayList<Entity> enemyEntities) {
        boolean startNorth = Compute.d6() > 3;

        int destinationEdge = startNorth ? Board.START_S : Board.START_N;
        int startEdge = startNorth ? Board.START_N : Board.START_S;

        setStartingPos(startEdge);
        setEnemyHome(destinationEdge);

        BotForce allyEntitiesForce = null;

        if (!allyEntities.isEmpty()) {
            allyEntitiesForce = getAllyBotForce(getContract(campaign), getStartingPos(), destinationEdge, allyEntities);
            addBotForce(allyEntitiesForce, campaign);
        }

        CombatTeam combatTeam = getCombatTeamById(campaign);
        int weightClass = combatTeam != null ? combatTeam.getWeightClass(campaign) : EntityWeightClass.WEIGHT_LIGHT;
        addEnemyForce(enemyEntities, weightClass, EntityWeightClass.WEIGHT_ASSAULT, 0,
              -1, campaign);
        addEnemyForce(enemyEntities, weightClass, EntityWeightClass.WEIGHT_ASSAULT, 0,
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
            logger.error("", e);
        }

        addBotForce(botForce, campaign);

        /* All forces deploy in 12 - WP turns */
        setDeploymentDelay(12);

        for (Entity en : allyEntities) {
            int speed = en.getWalkMP();

            if (en.getAnyTypeMaxJumpMP() > 0) {
                if (en instanceof Infantry) {
                    speed = en.getJumpMP();
                } else {
                    speed++;
                }
            }

            en.setDeployRound(Math.max(0, 12 - speed));
        }

        for (Entity en : enemyEntities) {
            int speed = en.getWalkMP();

            if (en.getAnyTypeMaxJumpMP() > 0) {
                if (en instanceof Infantry) {
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
                                                  ?
                                                  CommonObjectiveFactory.getBreakthrough(contract,
                                                        this,
                                                        campaign,
                                                        1,
                                                        50,
                                                        OffBoardDirection
                                                              .translateBoardStart(AtBDynamicScenarioFactory.getOppositeEdge(
                                                                    getStartingPos())))
                                                  :
                                                  CommonObjectiveFactory.getPreventEnemyBreakthrough(contract, 1, 50,
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
