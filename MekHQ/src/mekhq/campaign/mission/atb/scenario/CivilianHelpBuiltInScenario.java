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
import java.util.List;
import java.util.UUID;

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
import mekhq.campaign.mission.atb.AtBScenarioEnabled;
import mekhq.campaign.unit.Unit;

@AtBScenarioEnabled
public class CivilianHelpBuiltInScenario extends AtBScenario {
    private static final String CIVILIAN_FORCE_ID = "Civilians";

    @Override
    public boolean isSpecialScenario() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return CIVILIANHELP;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Special Scenario: Civilian Help";
    }

    @Override
    public String getResourceKey() {
        return "civilianHelp";
    }

    @Override
    public boolean canDeploy(Unit unit, Campaign campaign) {
        return unit.getCommander().getRank().isOfficer();
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
            enemyEntities = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                        getContract(campaign).getEnemyQuality(), UnitType.MEK, weight, campaign));
            }
            getSpecialScenarioEnemies().add(enemyEntities);
        }

        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getSpecialScenarioEnemies().get(0)), campaign);

        List<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 4, campaign);

        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }

        addBotForce(new BotForce(CIVILIAN_FORCE_ID, 1, getStartingPos(), otherForce), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 66);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                1, 1, true);
        ScenarioObjective keepCiviliansAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(CIVILIAN_FORCE_ID,
                1, 1, true);

        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepCiviliansAlive.addSuccessEffect(bonusEffect);
        keepCiviliansAlive.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"),
                bonusEffect.howMuch));

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepCiviliansAlive);
    }
}
