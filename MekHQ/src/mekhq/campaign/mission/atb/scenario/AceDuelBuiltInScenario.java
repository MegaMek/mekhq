/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.compute.Compute;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class AceDuelBuiltInScenario extends AtBScenario {
    @Override
    public boolean isSpecialScenario() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return ACE_DUEL;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Ace Duel";
    }

    @Override
    public String getResourceKey() {
        return "aceDuel";
    }

    @Override
    public void setLightConditions() {
        setLight(Light.DAY);
    }

    @Override
    public void setWeatherConditions(boolean isNoTornadoes) {
        setWeather(Weather.CLEAR);
        setWind(Wind.CALM);
        setFog(Fog.FOG_NONE);
    }

    @Override
    public void setMapFile() {
        setMap("Savannah");
        setTerrainType("Savannah");
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
        return !unit.getCommander().getRank().isOfficer();
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
          ArrayList<Entity> enemyEntities) {
        setStartingPos(startPos[Compute.randomInt(4)]);
        int enemyStart = getStartingPos() + 4;

        if (enemyStart > 8) {
            enemyStart -= 8;
        }

        for (int weight = EntityWeightClass.WEIGHT_ULTRA_LIGHT; weight <= EntityWeightClass.WEIGHT_COLOSSAL; weight++) {
            final Entity en;
            if (weight == EntityWeightClass.WEIGHT_COLOSSAL) {
                // Treat Colossal as a unique case, generating at that tier
                en = getEntity(getContract(campaign).getEnemyCode(),
                      getContract(campaign).getEnemySkill(),
                      getContract(campaign).getEnemyQuality(),
                      UnitType.MEK,
                      EntityWeightClass.WEIGHT_COLOSSAL,
                      campaign);
            } else {
                // Generate up to a maximum of Assault
                en = getEntity(getContract(campaign).getEnemyCode(),
                      getContract(campaign).getEnemySkill(),
                      getContract(campaign).getEnemyQuality(),
                      UnitType.MEK,
                      Math.min(weight + 1, EntityWeightClass.WEIGHT_ASSAULT),
                      campaign);
            }

            if (en == null) {
                getSpecialScenarioEnemies().add(new ArrayList<>());
                continue;
            }

            if (weight >= EntityWeightClass.WEIGHT_ASSAULT) {
                en.getCrew().setGunnery(en.getCrew().getGunnery() - 1, en.getCrew().getCrewType().getGunnerPos());
                en.getCrew().setPiloting(en.getCrew().getPiloting() - 1, en.getCrew().getCrewType().getPilotPos());
            }

            enemyEntities = new ArrayList<>();
            enemyEntities.add(en);
            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecialScenarioEnemies().get(0)), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign,
              contract,
              this,
              1,
              100,
              false);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
