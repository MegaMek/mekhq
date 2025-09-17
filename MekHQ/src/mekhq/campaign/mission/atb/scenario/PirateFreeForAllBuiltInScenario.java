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
import java.util.List;

import megamek.common.board.Board;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

@AtBScenarioEnabled
public class PirateFreeForAllBuiltInScenario extends AtBScenario {
    private static final String PIRATE_FORCE_ID = "Pirates";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return PIRATE_FREE_FOR_ALL;
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
                weightClass = AtBStaticWeightGenerator.getRandomWeight(campaign,
                      UnitType.MEK,
                      contract.getEmployerFaction());
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
