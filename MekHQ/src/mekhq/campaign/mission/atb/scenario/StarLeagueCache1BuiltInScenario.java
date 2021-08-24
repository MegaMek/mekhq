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

import megamek.client.generator.RandomUnitGenerator;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import java.util.ArrayList;
import java.util.List;

@AtBScenarioEnabled
public class StarLeagueCache1BuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 1994382390878571793L;

    private static String TECH_FORCE_ID = "Tech";

    @Override
    public boolean isSpecialMission() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return STARLEAGUECACHE1;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Mission: Star League Cache 1";
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
        setTerrainType(TER_LIGHTURBAN);
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
        setStart(Board.START_CENTER);
        int enemyStart = Board.START_N;

        int roll = Compute.d6();
        /* Only has enemy if SL 'Mech is not primitive */
        for (int weight = EntityWeightClass.WEIGHT_LIGHT; weight <= EntityWeightClass.WEIGHT_ASSAULT; weight++) {
            if (roll > 1) {
                enemyEntities = new ArrayList<Entity>();
                for (int i = 0; i < 3; i++) {
                    enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(),
                            getContract(campaign).getEnemySkill(),
                            getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
                }
            }

            getSpecMissionEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecMissionEnemies().get(0)));

        List<Entity> otherForce = new ArrayList<>();
        MechSummary ms = null;

        if (roll == 1) {
            RandomUnitGenerator.getInstance().setChosenRAT("CivilianUnits_PrimMech");
            ArrayList<MechSummary> msl = RandomUnitGenerator.getInstance().generate(1);
            if (msl.size() > 0) {
                ms = msl.get(0);
            }
        } else {
            // TODO : AtB Star League RAT Roll Year Option
            final Faction faction = Factions.getInstance().getFaction("SL");
            ms = campaign.getUnitGenerator().generate(faction.getShortName(), UnitType.MEK,
                    AtBMonthlyUnitMarket.getRandomWeight(campaign, UnitType.MEK, faction), 2750,
                    (roll == 6) ? IUnitRating.DRAGOON_A : IUnitRating.DRAGOON_D);
        }
        Entity en = (ms == null) ? null
                : AtBDynamicScenarioFactory.createEntityWithCrew(campaign.getFactionCode(),
                        SkillLevel.GREEN, campaign, ms);
        otherForce.add(en);

        // TODO: During SW offer a choice between an employer exchange or a contract breach
        Loot loot = new Loot();
        loot.setName(defaultResourceBundle.getString("battleDetails.starLeagueCache.Mek"));
        loot.addUnit(en);
        getLoot().add(loot);
        addBotForce(new BotForce(TECH_FORCE_ID, 1, getStart(), otherForce));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 100);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, true);
        ScenarioObjective keepTechAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(TECH_FORCE_ID, 1, true);

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepTechAlive);
    }
}
