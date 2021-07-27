/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
import java.util.List;

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ConvoyAttackBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 8487647534085152088L;

    private static String CONVOY_FORCE_ID = "Convoy";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return CONVOYATTACK;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Convoy Attack";
    }

    @Override
    public String getResourceKey() {
        return "convoyAttack";
    }

    @Override
    public int getMapX() {
        return 45;
    }

    @Override
    public int getMapY() {
        return 65;
    }

    @Override
    public void setMapFile() {
        setMap("Convoy");
        setTerrainType(TER_WOODED);
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public boolean canRerollMap() {
        return false;
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        setStart(Board.START_S);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
                    EntityWeightClass.WEIGHT_LIGHT, campaign));
        }

        List<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 12, campaign);
        addBotForce(new BotForce(CONVOY_FORCE_ID, 2, Board.START_CENTER, otherForce));

        for (int i = 0; i < 8; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), UnitType.MEK,
                    AtBMonthlyUnitMarket.getRandomWeight(campaign, UnitType.MEK, getContract(campaign).getEnemy()),
                    campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_CENTER, enemyEntities));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyConvoy = CommonObjectiveFactory.getDestroyEnemies(CONVOY_FORCE_ID, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                50, false);

        getScenarioObjectives().add(destroyConvoy);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
