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

import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.PrincessException;
import megamek.common.Board;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class ExtractionBuiltInScenario extends AtBScenario {
    private static final MMLogger logger = MMLogger.create(ExtractionBuiltInScenario.class);

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
    public void setExtraScenarioForces(Campaign campaign, ArrayList<Entity> allyEntities,
          ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int otherStart;
        int otherHome;
        int playerHome;

        if (isAttacker()) {
            playerHome = startPos[Compute.randomInt(4)];
            setStartingPos(playerHome);

            enemyStart = Board.START_CENTER;
            setEnemyHome(playerHome + 4);

            if (getEnemyHome() > 8) {
                setEnemyHome(getEnemyHome() - 8);
            }

            otherStart = getStartingPos() + 4;
            otherHome = playerHome;
        } else {
            setStartingPos(Board.START_CENTER);
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

        if (!allyEntities.isEmpty()) {
            addBotForce(getAllyBotForce(getContract(campaign), getStartingPos(), playerHome, allyEntities), campaign);
        }

        CombatTeam combatTeam = getCombatTeamById(campaign);
        int weightClass = combatTeam != null ? combatTeam.getWeightClass(campaign) : EntityWeightClass.WEIGHT_LIGHT;

        addEnemyForce(enemyEntities, weightClass, campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyStart, getEnemyHome(), enemyEntities), campaign);

        ArrayList<Entity> otherForce = new ArrayList<>();
        addCivilianUnits(otherForce, 4, campaign);

        try {
            if (isAttacker()) {
                BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 1, otherStart, playerHome, otherForce);
                bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                bf.setDestinationEdge(otherHome);

                addBotForce(bf, campaign);

                for (Entity en : otherForce) {
                    getSurvivalBonusIds().add(UUID.fromString(en.getExternalIdAsString()));
                }
            } else {
                BotForce bf = new BotForce(CIVILIAN_FORCE_ID, 2, otherStart, enemyStart, otherForce);
                bf.setBehaviorSettings(BehaviorSettingsFactory.getInstance().ESCAPE_BEHAVIOR.getCopy());
                bf.setDestinationEdge(otherHome);

                addBotForce(bf, campaign);
            }
        } catch (PrincessException ex) {
            logger.error("", ex);
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
            civilianObjective = CommonObjectiveFactory.getPreserveSpecificFriendlies(CIVILIAN_FORCE_ID, 1, 50, false);
            keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this, 1, 66, false);

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
                                              .format(defaultResourceBundle.getString("commonObjectives.bonusRolls.text"),
                                                    bonusEffect.howMuch));
        } else {
            civilianObjective = CommonObjectiveFactory.getDestroyEnemies(CIVILIAN_FORCE_ID, 1, 100);
            civilianObjective.setTimeLimit(10);
            civilianObjective.setTimeLimitAtMost(true);
            civilianObjective.setTimeLimitType(TimeLimitType.Fixed);
            destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 1, 33);
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
