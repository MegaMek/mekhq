/*
 * Copyright (c) 2019 - The Megamek Team. All rights reserved.
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

import megamek.client.generator.RandomSkillsGenerator;
import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;

@AtBScenarioEnabled
public class PirateFreeForAllBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 6410090692095923096L;

    private static final String PIRATE_FORCE_ID = "Pirates";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return PIRATEFREEFORALL;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Pirates Free-for-All";
    }

    @Override
    public String getResourceKey() {
        return "pirateFreeForAll";
    }

    @Override
    public int getMapX() {
        return 50;
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
        setStart(Board.START_CENTER);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer()
                    .add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                            getContract(campaign).getAllyQuality(), UnitType.MEK, UnitMarket.getRandomAeroWeight(), // max
                                                                                                                    // heavy
                            campaign));
        }

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), UnitType.MEK,
                    UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEnemyCode(),
                            campaign.getCampaignOptions().getRegionalMechVariations()),
                    campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_N, enemyEntities));

        ArrayList<Entity> otherForce = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            otherForce.add(getEntity("PIR", RandomSkillsGenerator.L_REG, IUnitRating.DRAGOON_C, UnitType.MEK,
                    UnitMarket.getRandomMechWeight(), campaign));
        }

        addBotForce(new BotForce(PIRATE_FORCE_ID, 3, Board.START_S, otherForce));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        ScenarioObjective destroyPirates = CommonObjectiveFactory.getDestroyEnemies(PIRATE_FORCE_ID, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                50, false);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(destroyPirates);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
