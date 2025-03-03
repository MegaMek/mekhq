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

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import java.util.ArrayList;
import java.util.List;

@AtBScenarioEnabled
public class PirateFreeForAllBuiltInScenario extends AtBScenario {
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
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        setStartingPos(Board.START_CENTER);

        final AtBContract contract = getContract(campaign);

        for (int i = 0; i < 4; i++) {
            int weightClass;
            do {
                weightClass = AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, contract.getEmployerFaction());
            } while (weightClass >= EntityWeightClass.WEIGHT_ASSAULT);
            getAlliesPlayer().add(getEntity(contract.getEmployerCode(), contract.getAllySkill(),
                    contract.getAllyQuality(), UnitType.MEK, weightClass, campaign));
        }

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(contract.getEnemyCode(), contract.getEnemySkill(),
                    contract.getEnemyQuality(), UnitType.MEK,
                    AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, contract.getEnemy()),
                    campaign));
        }

        addBotForce(getEnemyBotForce(contract, Board.START_N, enemyEntities), campaign);

        final List<Entity> otherForce = new ArrayList<>();
        final Faction faction = Factions.getInstance().getFaction("PIR");
        for (int i = 0; i < 12; i++) {
            otherForce.add(getEntity(faction.getShortName(), SkillLevel.REGULAR,
                    IUnitRating.DRAGOON_C, UnitType.MEK,
                    AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, faction), campaign));
        }

        addBotForce(new BotForce(PIRATE_FORCE_ID, 3, Board.START_S, otherForce), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 50);
        ScenarioObjective destroyPirates = CommonObjectiveFactory.getDestroyEnemies(PIRATE_FORCE_ID, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 50, false);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(destroyPirates);
        getScenarioObjectives().add(keepFriendliesAlive);
    }
}
