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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission.atb.scenario;

import java.util.ArrayList;
import java.util.UUID;

import megamek.common.board.Board;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBStaticWeightGenerator;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ConvoyRescueBuiltInScenario extends AtBScenario {
    private static String CONVOY_FORCE_ID = "Convoy";

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return CONVOYRESCUE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Convoy Rescue";
    }

    @Override
    public String getResourceKey() {
        return "convoyRescue";
    }

    @Override
    public int getMapX() {
        return 65;
    }

    @Override
    public int getMapY() {
        return 45;
    }

    @Override
    public void setMapFile() {
        setMap("Convoy");
        setTerrainType("Forest");
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
        setStartingPos(Board.START_N);
        setDeploymentDelay(7);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                  getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
                  EntityWeightClass.WEIGHT_LIGHT, campaign));
        }

        ArrayList<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 12, campaign);

        for (Entity e : otherForce) {
            getSurvivalBonusIds().add(UUID.fromString(e.getExternalIdAsString()));
        }

        addBotForce(new BotForce(CONVOY_FORCE_ID, 1, Board.START_CENTER, otherForce), campaign);

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                  getContract(campaign).getEnemyQuality(), UnitType.MEK,
                  AtBStaticWeightGenerator.getRandomWeight(campaign, UnitType.MEK, getContract(campaign).getEnemy()),
                  campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_S, enemyEntities), campaign);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
              1, 50, false);
        ScenarioObjective keepConvoyAlive = CommonObjectiveFactory.getPreserveSpecificFriendlies(CONVOY_FORCE_ID, 1,
              1, true);

        // not losing the scenario also gets you a "bonus"
        ObjectiveEffect bonusEffect = new ObjectiveEffect();
        bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
        bonusEffect.effectScaling = EffectScalingType.Linear;
        bonusEffect.howMuch = 1;
        keepConvoyAlive.addSuccessEffect(bonusEffect);
        keepConvoyAlive.addDetail(String.format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"),
              bonusEffect.howMuch));

        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
        getScenarioObjectives().add(keepConvoyAlive);
    }
}
