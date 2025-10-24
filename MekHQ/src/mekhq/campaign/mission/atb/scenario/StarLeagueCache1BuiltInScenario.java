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

import megamek.client.generator.RandomUnitGenerator;
import megamek.common.board.Board;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

@AtBScenarioEnabled
public class StarLeagueCache1BuiltInScenario extends AtBScenario {
    private static final String TECH_FORCE_ID = "Tech";

    @Override
    public boolean isSpecialScenario() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return STAR_LEAGUE_CACHE_1;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Star League Cache 1";
    }

    @Override
    public String getResourceKey() {
        return "starLeagueCache1";
    }

    @Override
    public int getMapX() {
        return 20;
    }

    @Override
    public int getMapY() {
        return 35;
    }

    @Override
    public void setMapFile() {
        setMap("Brian-cache");
        setTerrainType("Urban");
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
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
          ArrayList<Entity> enemyEntities) {
        setStartingPos(Board.START_CENTER);
        int enemyStart = Board.START_N;

        int roll = Compute.d6();
        /* Only has enemy if SL 'Mek is not primitive */
        for (int weight = EntityWeightClass.WEIGHT_ULTRA_LIGHT; weight <= EntityWeightClass.WEIGHT_COLOSSAL; weight++) {
            if (roll > 1) {
                enemyEntities = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(),
                          getContract(campaign).getEnemySkill(),
                          getContract(campaign).getEnemyQuality(),
                          UnitType.MEK,
                          weight,
                          campaign));
                }
            }

            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecialScenarioEnemies().get(0)), campaign);

        List<Entity> otherForce = new ArrayList<>();
        MekSummary ms = null;

        if (roll == 1) {
            RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits_PrimMek");
            ArrayList<MekSummary> msl = RandomUnitGenerator.getInstance().generate(1);
            if (!msl.isEmpty()) {
                ms = msl.get(0);
            }
        } else {
            // TODO : AtB Star League RAT Roll Year Option
            final Faction faction = Factions.getInstance().getFaction("SL");
            ms = campaign.getUnitGenerator()
                       .generate(faction.getShortName(),
                             UnitType.MEK,
                             AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, faction),
                             2750,
                             (roll == 6) ? DragoonRating.DRAGOON_A.getRating() : DragoonRating.DRAGOON_D.getRating());
        }
        Entity en = (ms == null) ?
                          null :
                          AtBDynamicScenarioFactory.createEntityWithCrew(campaign.getFaction().getShortName(),
                                SkillLevel.GREEN,
                                campaign,
                                ms);
        otherForce.add(en);

        // TODO: During SW offer a choice between an employer exchange or a contract
        // breach
        Loot loot = new Loot();
        loot.setName(defaultResourceBundle.getString("battleDetails.starLeagueCache.Mek"));
        loot.addUnit(en);
        getLoot().add(loot);
        addBotForce(new BotForce(TECH_FORCE_ID, 1, getStartingPos(), otherForce), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign,
              contract,
              this,
              1,
              1,
              true);
        ScenarioObjective keepTechAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(TECH_FORCE_ID,
              1,
              1,
              true);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepTechAlive);
    }
}
