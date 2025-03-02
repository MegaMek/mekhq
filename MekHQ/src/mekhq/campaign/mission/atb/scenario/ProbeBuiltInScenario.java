/*
 * Copyright (C) 2019-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */

package mekhq.campaign.mission.atb.scenario;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.stratcon.StratconBiomeManifest;
import mekhq.campaign.stratcon.StratconBiomeManifest.MapTypeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AtBScenarioEnabled
public class ProbeBuiltInScenario extends AtBScenario {
    @Override
    public int getScenarioType() {
        return PROBE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Probe";
    }

    @Override
    public String getResourceKey() {
        return "probe";
    }

    @Override
    public void setTerrain() {
        Map<String, MapTypeList> mapTypes = StratconBiomeManifest.getInstance().getBiomeMapTypes();
        List<String> keys = mapTypes.keySet().stream().sorted().collect(Collectors.toList());
        do {
            setTerrainType(keys.get(Compute.randomInt(keys.size())));
        } while (getTerrainType().toUpperCase().contains("URBAN"));
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

        CombatTeam combatTeam = getCombatTeamById(campaign);
        int weightClass = combatTeam != null ? combatTeam.getWeightClass(campaign) : EntityWeightClass.WEIGHT_LIGHT;

        addEnemyForce(enemyEntities, weightClass, EntityWeightClass.WEIGHT_MEDIUM, 0, 0,
                campaign);

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 25);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 75, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
