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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.board.Board;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.stratcon.StratconBiomeManifest;
import mekhq.campaign.stratcon.StratconBiomeManifest.MapTypeList;

@AtBScenarioEnabled
public class HideAndSeekBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return HIDEANDSEEK;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Hide and Seek";
    }

    @Override
    public String getResourceKey() {
        return "hideAndSeek";
    }

    @Override
    public void setTerrain() {
        Map<String, MapTypeList> mapTypes = StratconBiomeManifest.getInstance().getBiomeMapTypes();
        List<String> keys = mapTypes.keySet().stream().sorted().collect(Collectors.toList());
        do {
            setTerrainType(keys.get(Compute.randomInt(keys.size())));
        } while (getTerrainType().equals("ColdSea")
                       || getTerrainType().equals("FrozenSea")
                       || getTerrainType().equals("HotSea")
                       || getTerrainType().equals("Plains")
                       || getTerrainType().equals("Savannah"));
    }

    @Override
    public int getMapX() {
        return getBaseMapX() - 10;
    }

    @Override
    public int getMapY() {
        return getBaseMapY() - 10;
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

        CombatTeam combatTeam = getCombatTeamById(campaign);
        int weightClass = combatTeam != null ? combatTeam.getWeightClass(campaign) : EntityWeightClass.WEIGHT_LIGHT;

        if (isAttacker()) {
            addEnemyForce(enemyEntities, weightClass, EntityWeightClass.WEIGHT_ASSAULT, 2,
                  0, campaign);
        } else {
            addEnemyForce(enemyEntities, weightClass, EntityWeightClass.WEIGHT_HEAVY, 0,
                  0, campaign);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        // Attacker must destroy 50% and keep 66% alive
        // Defender must destroy 33% and keep 50% alive
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1,
              isAttacker() ? 50 : 33);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(
              campaign, contract, this, 1, isAttacker() ? 66 : 50, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
              this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
