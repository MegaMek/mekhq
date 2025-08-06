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
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
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
public class BreakthroughBuiltInScenario extends AtBScenario {
    private static final MMLogger logger = MMLogger.create(BreakthroughBuiltInScenario.class);

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
        return 18 + this.getForceCount();
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

        CombatTeam combatTeam = getCombatTeamById(campaign);
        int weightClass = combatTeam != null ? combatTeam.getWeightClass(campaign) : EntityWeightClass.WEIGHT_LIGHT;
        addEnemyForce(enemyEntities, weightClass, campaign);
        BotForce botForce = getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities);

        try {
            if (isAttacker()) {
                if (null != allyEntitiesForce) {
                    allyEntitiesForce
                          .setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                    allyEntitiesForce.setDestinationEdge(AtBDynamicScenarioFactory.getOppositeEdge(getStartingPos()));
                }
            } else {
                botForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                botForce.setDestinationEdge(getEnemyHome());
            }
        } catch (Exception ex) {
            logger.error("", ex);
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
                                                  ?
                                                  CommonObjectiveFactory.getBreakthrough(contract,
                                                        this,
                                                        campaign,
                                                        1,
                                                        66,
                                                        OffBoardDirection.getOpposite(OffBoardDirection.translateBoardStart(
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
