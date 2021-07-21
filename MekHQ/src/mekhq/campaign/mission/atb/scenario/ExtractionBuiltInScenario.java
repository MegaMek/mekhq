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
import java.util.UUID;

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
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

@AtBScenarioEnabled
public class ExtractionBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 2669891555728754709L;

    private static final String CIVILIAN_FORCE_ID = "Civilians";

    @Override
    public int getScenarioType() {
        return EXTRACTION;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Extraction";
    }

    @Override
    public String getResourceKey() {
        return "extraction";
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities,
            ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int otherStart;
        int otherHome;
        int playerHome;

        if (isAttacker()) {
            playerHome = startPos[Compute.randomInt(4)];
            setStart(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(playerHome + 4);

            if (getEnemyHome() > 8) {
                setEnemyHome(getEnemyHome() - 8);
            }

            otherStart = getStart() + 4;
            otherHome = playerHome;
        } else {
            setStart(Board.START_CENTER);
            enemyStart = startPos[Compute.randomInt(4)];

            setEnemyHome(enemyStart);
            playerHome = getEnemyHome() + 4;

            if (playerHome > 8) {
                playerHome -= 8;
            }

            otherStart = enemyStart + 4;
            otherHome = enemyStart;
        }
        if (otherStart > 8) {
            otherStart -= 8;
        }

        if (allyEntities.size() > 0) {
            addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities));
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities));

        ArrayList<Entity> otherForce = new ArrayList<Entity>();
        addCivilianUnits(otherForce, 4, campaign);

        try {
            if (isAttacker()) {
                BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 1, otherStart, playerHome, otherForce);
                bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                bf.setDestinationEdge(otherHome);

                addBotForce(bf);

                for (Entity en : otherForce) {
                    getSurvivalBonusIds().add(UUID.fromString(en.getExternalIdAsString()));
                }
            } else {
                BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 2, otherStart, enemyStart, otherForce);
                bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                bf.setDestinationEdge(otherHome);

                addBotForce(bf);
            }
        } catch (PrincessException e) {
            MekHQ.getLogger().error(e);
        }
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective keepFriendliesAlive = null;
        ScenarioObjective keepAttachedUnitsAlive = CommonObjectiveFactory.getKeepAttachedGroundUnitsAlive(contract,
                this);
        ScenarioObjective destroyHostiles = null;
        ScenarioObjective civilianObjective;

        if (isAttacker()) {
            civilianObjective = CommonObjectiveFactory.getPreserveSpecificFriendlies(CIVILIAN_FORCE_ID, 50, false);
            keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 66, false);

            civilianObjective.setTimeLimit(12);
            civilianObjective.setTimeLimitAtMost(false);
            civilianObjective.setTimeLimitType(TimeLimitType.Fixed);

            keepFriendliesAlive.setTimeLimit(12);
            keepFriendliesAlive.setTimeLimitAtMost(false);
            keepFriendliesAlive.setTimeLimitType(TimeLimitType.Fixed);

            // not losing the scenario also gets you a "bonus"
            ObjectiveEffect bonusEffect = new ObjectiveEffect();
            bonusEffect.effectType = ObjectiveEffectType.AtBBonus;
            bonusEffect.effectScaling = EffectScalingType.Linear;
            bonusEffect.howMuch = 1;
            civilianObjective.addSuccessEffect(bonusEffect);
            civilianObjective.addDetail(String
                    .format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"), bonusEffect.howMuch));
        } else {
            civilianObjective = CommonObjectiveFactory.getDestroyEnemies(CIVILIAN_FORCE_ID, 100);
            civilianObjective.setTimeLimit(10);
            civilianObjective.setTimeLimitAtMost(true);
            civilianObjective.setTimeLimitType(TimeLimitType.Fixed);
            destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 33);
            destroyHostiles.setTimeLimit(10);
            destroyHostiles.setTimeLimitAtMost(true);
            destroyHostiles.setTimeLimitType(TimeLimitType.Fixed);
        }

        if (destroyHostiles != null) {
            getScenarioObjectives().add(destroyHostiles);
        }

        if (keepAttachedUnitsAlive != null) {
            getScenarioObjectives().add(keepAttachedUnitsAlive);
        }

        if (keepFriendliesAlive != null) {
            getScenarioObjectives().add(keepFriendliesAlive);
        }

        getScenarioObjectives().add(civilianObjective);
    }

    @Override
    public String getBattlefieldControlDescription() {
        return getResourceBundle().getString("battleDetails.common.defenderControlsBattlefield");
    }
}
