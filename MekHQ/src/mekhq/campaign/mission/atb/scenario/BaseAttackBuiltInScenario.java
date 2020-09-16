/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.enums.AtBLanceRole;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class BaseAttackBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = -873528230365616996L;

    private static final String BASE_CIVILIAN_FORCE_ID = "Base Civilian Units";
    private static final String BASE_TURRET_FORCE_ID = "Base Turrets";
    private static final String SECOND_ENEMY_FORCE_SUFFIX = " Force #2";

    @Override
    public int getScenarioType() {
        return BASEATTACK;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Base Attack";
    }

    @Override
    public String getResourceKey() {
        return "baseAttack";
    }

    @Override
    public void setTerrain() {
        setTerrainType((Compute.d6() < 4) ? TER_LIGHTURBAN : TER_HEAVYURBAN);
    }

    @Override
    public int getMapX() {
        return getBaseMapX() + 10;
    }

    @Override
    public int getMapY() {
        return getBaseMapY() + 10;
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        int attackerStartIndex = Compute.randomInt(4);
        int attackerStart = startPos[attackerStartIndex];
        int defenderStart = Board.START_CENTER;
        int defenderHome = (attackerStart + 4) % 8; // the defender's "retreat"
                                                    // edge should always be the
                                                    // opposite of the
                                                    // attacker's edge

        int enemyStart;

        // the attacker starts on an edge, the defender starts in the center and
        // flees to the opposite edge of the attacker
        if (isAttacker()) {
            setStart(attackerStart);

            setEnemyHome(defenderHome);
            enemyStart = defenderStart;
        } else {
            setStart(defenderStart);

            setEnemyHome(attackerStart);
            enemyStart = attackerStart;
        }

        /*
         * Ally deploys 2 lances of a lighter weight class than the player,
         * minimum light
         */
        int allyForceWeight = Math.max(getLance(campaign).getWeightClass(campaign) - 1, EntityWeightClass.WEIGHT_LIGHT);
        addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                getContract(campaign).getAllyQuality(), allyForceWeight, campaign);
        addLance(allyEntities, getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                getContract(campaign).getAllyQuality(), allyForceWeight, campaign);

        // the "second" force will be deployed (orthogonally) between 90 degrees
        // clockwise and counterclockwise from the "primary force".
        int angleChange = Compute.randomInt(3) - 1;
        int secondAttackerForceStart = startPos[(attackerStartIndex + angleChange + 4) % 4];

        // the ally is the "second force" and will flee either in the same
        // direction as the player (in case of the player being the defender)
        // or where it came from (in case of the player being the attacker
        addBotForce(getAllyBotForce(getContract(campaign), isAttacker() ? secondAttackerForceStart : getStart(),
                isAttacker() ? secondAttackerForceStart : defenderHome, allyEntities));

        // "base" force gets 8 civilian units and six turrets
        // set the civilians to "cowardly" behavior by default so they don't run
        // out and get killed. As much.
        ArrayList<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 8, campaign);
        BotForce civilianForce = new BotForce(BASE_CIVILIAN_FORCE_ID, isAttacker() ? 2 : 1, defenderStart, defenderHome,
                otherForce);
        civilianForce.setBehaviorSettings(BehaviorSettingsFactory.getInstance().COWARDLY_BEHAVIOR);
        addBotForce(civilianForce);

        ArrayList<Entity> turretForce = new ArrayList<>();
        
        if (isAttacker()) {
            addTurrets(turretForce, 6, getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                    campaign);
        } else {
            addTurrets(turretForce, 6, getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    campaign);
        }
        
        addBotForce(new BotForce(BASE_TURRET_FORCE_ID, isAttacker() ? 2 : 1, defenderStart, defenderHome, turretForce));

        /* Roll 2x on bot lances roll */
        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

        // the "second" enemy force will either flee in the same direction as
        // the first enemy force in case of the player being the attacker
        // or where it came from in case of player being defender
        ArrayList<Entity> secondBotEntities = new ArrayList<>();
        addEnemyForce(secondBotEntities, getLance(campaign).getWeightClass(campaign), campaign);
        BotForce secondBotForce = getEnemyBotForce(getContract(campaign),
                isAttacker() ? enemyStart : secondAttackerForceStart,
                isAttacker() ? getEnemyHome() : secondAttackerForceStart, secondBotEntities);
        secondBotForce.setName(String.format("%s%s", secondBotForce.getName(), SECOND_ENEMY_FORCE_SUFFIX));
        addBotForce(secondBotForce);
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        destroyHostiles.addForce(String.format("%s%s", contract.getEnemyBotName(), SECOND_ENEMY_FORCE_SUFFIX));
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                50, false);
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);

        ScenarioObjective preserveBaseUnits = null;
        if (!isAttacker()) {
            preserveBaseUnits = CommonObjectiveFactory.getPreserveSpecificFriendlies(BASE_CIVILIAN_FORCE_ID, 3, true);
            preserveBaseUnits.addForce(BASE_TURRET_FORCE_ID);

            ObjectiveEffect defeatEffect = new ObjectiveEffect();
            defeatEffect.effectType = ObjectiveEffectType.ContractDefeat;
            preserveBaseUnits.addFailureEffect(defeatEffect);
        } else {
            destroyHostiles.addForce(BASE_CIVILIAN_FORCE_ID);
            destroyHostiles.addForce(BASE_TURRET_FORCE_ID);

            // per AtB rules, completing this scenario on some contracts is an
            // outright victory
            // while completing this scenario on others just puts the morale to
            // Rout for a while
            ObjectiveEffect victoryEffect = new ObjectiveEffect();
            if ((contract.getRequiredLanceType() == AtBLanceRole.FIGHTING)
                    || (contract.getRequiredLanceType() == AtBLanceRole.SCOUTING)) {
                victoryEffect.effectType = ObjectiveEffectType.ContractVictory;
                destroyHostiles.addDetail(getResourceBundle().getString("battleDetails.baseAttack.attacker.details.winnerFightScout"));
            } else {
                victoryEffect.effectType = ObjectiveEffectType.ContractMoraleUpdate;
                victoryEffect.howMuch = -3;
                destroyHostiles.addDetail(getResourceBundle().getString("battleDetails.baseAttack.attacker.details.winnerDefendTraining"));
            }
            destroyHostiles.addSuccessEffect(victoryEffect);
        }

        if (preserveBaseUnits != null) {
            getScenarioObjectives().add(preserveBaseUnits);
        }

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }
        getScenarioObjectives().add(destroyHostiles);
        getScenarioObjectives().add(keepFriendliesAlive);
    }

    @Override
    public String getBattlefieldControlDescription() {
        String retval = super.getBattlefieldControlDescription();

        if(!isAttacker()) {
            retval += "\r\n";
            retval += getResourceBundle().getString("battleDetails.baseAttack.attacker.details.loser");
        }

        return retval;
    }
}
