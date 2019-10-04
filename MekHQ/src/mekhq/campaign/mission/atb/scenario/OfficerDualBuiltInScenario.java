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

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.PlanetaryConditions;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class OfficerDualBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = -1868853521323602877L;

    @Override
    public boolean isSpecialMission() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return OFFICERDUEL;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Mission: Officer Duel";
    }

    @Override
    public String getResourceKey() {
        return "officerDuel";
    }

    @Override
    public void setLightConditions() {
        setLight(PlanetaryConditions.L_DAY);
    }

    @Override
    public void setWeather() {
        setWeather(PlanetaryConditions.WE_NONE);
        setWind(PlanetaryConditions.WI_NONE);
        setFog(PlanetaryConditions.FOG_NONE);
    }

    @Override
    public void setMapFile() {
        setMap("Savannah");
        setTerrainType(TER_FLATLANDS);
    }

    @Override
    public boolean canRerollMap() {
        return false;
    }

    @Override
    public boolean canRerollLight() {
        return false;
    }

    @Override
    public boolean canRerollWeather() {
        return false;
    }

    @Override
    public boolean canDeploy(Unit unit, Campaign campaign) {
        return unit.getCommander().getRank().isOfficer();
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        setStart(startPos[Compute.randomInt(4)]);
        int enemyStart = getStart() + 4;

        if (enemyStart > 8) {
            enemyStart -= 8;
        }

        for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
            enemyEntities = new ArrayList<Entity>();
            Entity en = getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), UnitType.MEK,
                    Math.min(weight + 1, EntityWeightClass.WEIGHT_ASSAULT), campaign);
            if (weight == EntityWeightClass.WEIGHT_ASSAULT) {
                en.getCrew().setGunnery(en.getCrew().getGunnery() - 1);
                en.getCrew().setPiloting(en.getCrew().getPiloting() - 1);
            }

            enemyEntities.add(en);
            getSpecMissionEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                100, false);

        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
    }
}
