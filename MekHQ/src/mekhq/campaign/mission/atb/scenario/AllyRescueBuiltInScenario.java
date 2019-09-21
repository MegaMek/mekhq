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

import megamek.common.Board;
import megamek.common.Entity;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.CommonObjectiveFactory;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.atb.AtBScenarioEnabled;

@AtBScenarioEnabled
public class AllyRescueBuiltInScenario extends AtBScenario {
    private static final long serialVersionUID = 6993274905243245321L;

    @Override
    public boolean isBigBattle() {
        return true;
    }

    @Override
    public int getScenarioType() {
        return ALLYRESCUE;
    }

    @Override
    public String getScenarioTypeDescription() {
        return "Big Battle: Ally Rescue";
    }

    @Override
    public String getResourceKey() {
        return "allyRescue";
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
        setMap("Ally-rescue");
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

        setStart(Board.START_S);
        setDeploymentDelay(12);

        for (int i = 0; i < 4; i++) {
            getAlliesPlayer().add(getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(), UnitType.MEK,
                    UnitMarket.getRandomWeight(UnitType.MEK, getContract(campaign).getEmployerCode(),
                            campaign.getCampaignOptions().getRegionalMechVariations()),
                    campaign));
        }

        ArrayList<Entity> otherForce = new ArrayList<Entity>();

        for (int i = 0; i < 8; i++) {
            otherForce.add(getEntity(getContract(campaign).getEmployerCode(), getContract(campaign).getAllySkill(),
                    getContract(campaign).getAllyQuality(), UnitType.MEK, UnitMarket.getRandomAeroWeight(), // max
                                                                                                            // heavy
                    campaign));
        }

        addBotForce(new BotForce(getContract(campaign).getAllyBotName(), 1, Board.START_CENTER, otherForce));

        for (int i = 0; i < 12; i++) {
            enemyEntities.add(getEntity(getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), UnitType.MEK, UnitMarket.getRandomAeroWeight() + 1, // no
                                                                                                                 // light
                                                                                                                 // 'Mechs
                    campaign));
        }

        addBotForce(getEnemyBotForce(getContract(campaign), Board.START_N, enemyEntities));
    }

    @Override
    public void setObjectives(Campaign campaign, AtBContract contract) {
        super.setObjectives(campaign, contract);

        ScenarioObjective destroyHostiles = CommonObjectiveFactory.getDestroyEnemies(contract, 50);
        ScenarioObjective keepFriendliesAlive = CommonObjectiveFactory.getKeepFriendliesAlive(campaign, contract, this,
                50, false);

        // in addition to the standard destroy 50/preserve 50, you need to keep
        // at least 3/8 of the "allied" units alive.
        ScenarioObjective keepAlliesAlive = new ScenarioObjective();
        keepAlliesAlive.setFixedAmount(3);
        keepAlliesAlive.setDescription(
                String.format(defaultResourceBundle.getString("commonObjectives.preserveFriendlyUnits.text"),
                        keepAlliesAlive.getFixedAmount(), ""));
        keepAlliesAlive.setObjectiveCriterion(ObjectiveCriterion.Preserve);

        keepAlliesAlive.addForce(contract.getAllyBotName());

        ObjectiveEffect friendlyFailureEffect = new ObjectiveEffect();
        friendlyFailureEffect.effectType = ObjectiveEffectType.ScenarioDefeat;
        keepAlliesAlive.addFailureEffect(friendlyFailureEffect);

        getObjectives().add(destroyHostiles);
        getObjectives().add(keepFriendliesAlive);
        getObjectives().add(keepAlliesAlive);
    }
}
