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
 */
package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class CivilianRiotBuiltInScenario extends AtBScenario {
    private static String RIOTER_FORCE_ID = "Rioters";
    private static String REBEL_FORCE_ID = "Rebels";
    private static String LOYALIST_FORCE_ID = "Loyalists";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return CIVILIANRIOT;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Civilian Riot";
    }

    @Override
    public String getResourceKey() {
        return "civilianRiot";
    }

    @Override
    public int getMapX() {
        return 65;
    }

    @Override
    public int getMapY() {
        return 65;
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public boolean canDeploy(Unit unit, Campaign campaign) {
        for (Part p : unit.getParts()) {
            if (p instanceof EquipmentPart) {
                for (String weapon : antiRiotWeapons) {
                    if (((EquipmentPart) p).getType().getInternalName().equals(weapon)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        // north, south, east, west
        int boardEdge = (Compute.randomInt(4) + 1) * 2;
        setStartingPos(boardEdge);

        // TODO: only units with machine guns, flamers, or sm lasers
        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
                    (Compute.randomInt(7) < 3) ? EntityWeightClass.WEIGHT_LIGHT : EntityWeightClass.WEIGHT_MEDIUM,
                    campaign));
        }

        ArrayList<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 8, campaign);

        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }

        addBotForce(new BotForce(LOYALIST_FORCE_ID, 1, Board.START_CENTER, otherForce), campaign);

        otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 12, campaign);
        addBotForce(new BotForce(RIOTER_FORCE_ID, 2, Board.START_CENTER, otherForce), campaign);

        for (int i = 0; i < 3; i++) {
            // 3 mek rebel lance, use employer RAT, enemy skill
            enemyEntities.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getEnemySkill(),
                    IUnitRating.DRAGOON_F, UnitType.MEK,
                    Compute.d6() < 4 ? EntityWeightClass.WEIGHT_LIGHT : EntityWeightClass.WEIGHT_MEDIUM, campaign));
        }

        addBotForce(
                new BotForce(REBEL_FORCE_ID, 2, AtBDynamicScenarioFactory.getOppositeEdge(boardEdge), enemyEntities),
                campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyRioters = CommonObjectiveFactory.getDestroyEnemies(RIOTER_FORCE_ID, 1, 100);
        ScenarioObjective destroyRebels = CommonObjectiveFactory.getDestroyEnemies(REBEL_FORCE_ID, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 50, false);
        ScenarioObjective keepLoyalistsAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(LOYALIST_FORCE_ID,
                1, 1, true);

        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepLoyalistsAlive.addSuccessEffect(bonusEffect);
        keepLoyalistsAlive.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"),
                bonusEffect.howMuch));

        getScenarioObjectives().add(destroyRioters);
        getScenarioObjectives().add(destroyRebels);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepLoyalistsAlive);
    }
}
