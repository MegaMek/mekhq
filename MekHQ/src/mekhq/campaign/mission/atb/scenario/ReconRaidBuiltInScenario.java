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
import megamek.common.OffBoardDirection;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ReconRaidBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return RECONRAID;
    }

    @Override
    public String getScenarioTypeDescription() {
        return defaultResourceBundle.getString("battleDetails.reconRaid.name");
    }

    @Override
    public String getResourceKey() {
        return "reconRaid";
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        if (isAttacker()) {
            playerHome = startPos[Compute.randomInt(4)];
            setStart(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(playerHome + 4);

            if (getEnemyHome() > 8) {
                setEnemyHome(getEnemyHome() - 8);
            }
        } else {
            setStart(Board.START_CENTER);
            enemyStart = startPos[Compute.randomInt(4)];
            setEnemyHome(enemyStart);
            playerHome = getEnemyHome() + 4;

            if (playerHome > 8) {
                playerHome -= 8;
            }
        }

        if (!allyEntities.isEmpty()) {
            addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities), campaign);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign),
                isAttacker() ? EntityWeightClass.WEIGHT_ASSAULT : EntityWeightClass.WEIGHT_MEDIUM, 0, 0, campaign);

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities), campaign);
    }

    @Override
    public boolean canAddDropShips() {
        return isAttacker() && (Compute.d6() <= 3);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        if (isAttacker()) {
            ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract,
                    this, 75, false);
            getScenarioObjectives().add(keepFriendliesAlive);

            ScenarioObjective raidObjective = new ScenarioObjective();
            raidObjective.setObjectiveCriterion(ObjectiveCriterion.Custom);
            raidObjective.setDescription(
                    String.format("%s:", defaultResourceBundle.getString("battleDetails.reconRaid.name")));
            raidObjective.addDetail(String.format(
                    defaultResourceBundle.getString("battleDetails.reconRaid.instructions.oppositeEdge"),
                    OffBoardDirection.translateBoardStart(AtBDynamicScenarioFactory.getOppositeEdge(getStart()))));
            raidObjective.addDetail(defaultResourceBundle.getString("battleDetails.reconRaid.instructions.stayStill"));
            raidObjective.addDetail(
                    String.format(defaultResourceBundle.getString("battleDetails.reconRaid.instructions.returnEdge"),
                            OffBoardDirection.translateBoardStart(getStart())));
            raidObjective.addDetail(defaultResourceBundle.getString("battleDetails.reconRaid.instructions.reward"));

            ObjectiveEffect victoryEffect = new ObjectiveEffect();
            victoryEffect.effectType = ObjectiveEffectType.AtBBonus;
            victoryEffect.howMuch = Compute.d6() - 2;
            raidObjective.addSuccessEffect(victoryEffect);

            getScenarioObjectives().add(raidObjective);
        } else {
            destroyHostiles.setTimeLimit(10);
            destroyHostiles.setTimeLimitAtMost(true);
            destroyHostiles.setTimeLimitType(TimeLimitType.Fixed);
            getScenarioObjectives().add(destroyHostiles);
        }
    }

    @Override
    public String getBattlefieldControlDescription() {
        return getResourceBundle().getString("battleDetails.common.defenderControlsBattlefield");
    }
}
