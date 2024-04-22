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

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class PrisonBreakBuiltInScenario extends AtBScenario {
    private static String GUARD_FORCE_ID = "Guards";
    private static String PRISONER_FORCE_ID = "Prisoners";

    @Override
    public boolean isSpecialScenario() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return PRISONBREAK;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Prison Break";
    }

    @Override
    public String getResourceKey() {
        return "prisonBreak";
    }

    @Override
    public int getMapX() {
        return 20;
    }

    @Override
    public int getMapY() {
        return 30;
    }

    @Override
    public boolean canRerollMapSize() {
        return false;
    }

    @Override
    public boolean canDeploy(Unit unit, Campaign campaign) {
        return unit.getEntity().getWeightClass() <= EntityWeightClass.WEIGHT_MEDIUM;
    }

    @Override
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
                                       ArrayList<Entity> enemyEntities) {
        setStartingPos(Board.START_CENTER);
        int enemyStart = startPos[Compute.randomInt(4)];

        for (int weight = EntityWeightClass.WEIGHT_ULTRA_LIGHT; weight <= EntityWeightClass.WEIGHT_COLOSSAL; weight++) {
            enemyEntities = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                        getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
            }
            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(new BotForce(GUARD_FORCE_ID, 2, enemyStart, getSpecialScenarioEnemies().get(0)), campaign);

        ArrayList<Entity> otherForce = new ArrayList<>();

        addCivilianUnits(otherForce, 4, campaign);

        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }

        addBotForce(new BotForce(PRISONER_FORCE_ID, 1, getStartingPos(), otherForce), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 1, true);
        ScenarioObjective keepPrisonersAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(PRISONER_FORCE_ID,
                1, 1, true);
        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(GUARD_FORCE_ID, 1, 100);
        destroyHostiles.getSuccessEffects().clear();
        destroyHostiles.addDetail(getResourceBundle().getString("commonObjectives.battlefieldControl"));
        destroyHostiles.setTimeLimit(8);
        destroyHostiles.setTimeLimitAtMost(true);
        destroyHostiles.setTimeLimitType(TimeLimitType.Fixed);

        keepFriendliesAlive.setTimeLimit(8);
        keepFriendliesAlive.setTimeLimitAtMost(false);
        keepFriendliesAlive.setTimeLimitType(TimeLimitType.Fixed);

        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepPrisonersAlive.setTimeLimit(8);
        keepPrisonersAlive.setTimeLimitAtMost(false);
        keepPrisonersAlive.setTimeLimitType(TimeLimitType.Fixed);
        keepPrisonersAlive.addSuccessEffect(bonusEffect);
        keepPrisonersAlive.addDetail(String.format(getResourceBundle().getString("commonObjectives.bonusRolls.text"),
                bonusEffect.howMuch));

        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepPrisonersAlive);
        getScenarioObjectives().add(destroyHostiles);
    }

    @Override
    public String getBattlefieldControlDescription() {
        return "";
    }
}
