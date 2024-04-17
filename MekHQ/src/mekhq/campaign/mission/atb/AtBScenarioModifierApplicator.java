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
package mekhq.campaign.mission.atb;

import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.TaharqaSkillGenerator;
import megamek.common.*;
import megamek.common.enums.SkillLevel;
import megamek.common.options.OptionsConstants;
import megamek.codeUtilities.MathUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.Skills;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Factions;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;

/**
 * Class that handles the application of scenario modifier actions to AtBDynamicScenarios
 * @author NickAragua
 */
public class AtBScenarioModifierApplicator {
    /**
     * Adds the given force to the given scenario at the appropriate point in time.
     */
    public static void addForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate forceToApply, EventTiming eventTiming) {
        preAddForce(campaign, scenario, forceToApply);

        if (eventTiming == EventTiming.PostForceGeneration) {
            postAddForce(campaign, scenario, forceToApply);
        }
    }

    /**
     * Adds the given force to the scenario after primary forces have been generated.
     */
    private static void postAddForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate templateToApply) {
        int effectiveBV = AtBDynamicScenarioFactory.calculateEffectiveBV(scenario, campaign);
        int effectiveUnitCount = AtBDynamicScenarioFactory.calculateEffectiveUnitCount(scenario, campaign);
        int deploymentZone = AtBDynamicScenarioFactory.calculateDeploymentZone(templateToApply, scenario, templateToApply.getForceName());

        AtBDynamicScenarioFactory.generateForce(scenario, scenario.getContract(campaign), campaign,
                effectiveBV, effectiveUnitCount, EntityWeightClass.WEIGHT_ASSAULT, templateToApply);

        // the most recently added bot force is the one we just generated
        BotForce generatedBotForce = scenario.getBotForce(scenario.getNumBots() - 1);
        generatedBotForce.setStartingPos(deploymentZone);
        AtBDynamicScenarioFactory.setDeploymentTurns(generatedBotForce, templateToApply, scenario, campaign);
        AtBDynamicScenarioFactory.setDestinationZone(generatedBotForce, templateToApply);

        // at this point, we have to re-translate the scenario objectives
        // since we're adding a force that could potentially go into any of them
        AtBDynamicScenarioFactory.translateTemplateObjectives(scenario, campaign);
    }

    /**
     * Adds the given force to the scenario template prior to primary force generation.
     */
    private static void preAddForce(Campaign campaign, AtBDynamicScenario scenario, ScenarioForceTemplate forceToApply) {
        if (scenario.getTemplate() != null) {
            scenario.getTemplate().getScenarioForces().put(forceToApply.getForceName(), forceToApply);
        }
    }

    /**
     * Worker function that removes the number of units from the specified side.
     */
    public static void removeUnits(AtBDynamicScenario scenario, Campaign campaign, ForceAlignment eventRecipient, int unitRemovalCount) {
        // can't do this if we don't have bots
        if (scenario.getNumBots() == 0) {
            return;
        }

        int actualUnitsToRemove = unitRemovalCount;

        if (unitRemovalCount == ScenarioForceTemplate.FIXED_UNIT_SIZE_LANCE) {
            String factionCode;

            if (eventRecipient == ForceAlignment.Allied) {
                factionCode = scenario.getContract(campaign).getEmployerCode();
            } else {
                factionCode = scenario.getContract(campaign).getEnemyCode();
            }

            actualUnitsToRemove = AtBDynamicScenarioFactory.getLanceSize(factionCode);
        }

        for (int x = 0; x < actualUnitsToRemove; x++) {
            int botForceIndex = Compute.randomInt(scenario.getNumBots());
            BotForce bf = scenario.getBotForce(botForceIndex);
            ScenarioForceTemplate template = scenario.getBotForceTemplates().get(bf);
            boolean subjectToRemoval = (template != null) && template.isSubjectToRandomRemoval();

            // only remove units from a bot force if it's on the affected team
            // AND if it has any units to remove
            if ((bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal()))
                    && !bf.getFullEntityList(campaign).isEmpty() && subjectToRemoval) {
                int unitIndexToRemove = Compute.randomInt(bf.getFullEntityList(campaign).size());
                bf.removeEntity(unitIndexToRemove);
            }
        }
    }

    /**
     * Worker function that inflicts battle damage on all units belonging to the side specified in this modifier.
     * May theoretically result in a crippled or destroyed unit (?)
     */
    public static void inflictBattleDamage(AtBDynamicScenario scenario, Campaign campaign,
                                           ForceAlignment eventRecipient, int battleDamageIntensity) {
        // now go through all the entities belonging to the recipient currently in the scenario
        // and apply random battle damage
        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce bf = scenario.getBotForce(botIndex);
            if (bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for (Entity en : bf.getFullEntityList(campaign)) {
                    int numClusters = Compute.randomInt(battleDamageIntensity);

                    for (int clusterCount = 0; clusterCount < numClusters; clusterCount++) {
                        HitData hitData = en.rollHitLocation(ToHitData.HIT_NORMAL, Compute.randomInt(4));
                        int resultingArmor = Math.max(1, en.getArmor(hitData) - 5);

                        en.setArmor(resultingArmor, hitData);
                    }
                }
            }
        }
    }

    /**
     * Worker function that expends ammo from all units belonging to the side specified in this modifier.
     */
    public static void expendAmmo(AtBDynamicScenario scenario, Campaign campaign,
            ForceAlignment eventRecipient, int ammoExpenditureIntensity) {
        // now go through all the entities belonging to the recipient currently in the scenario
        // and remove a random amount of ammo from each bin
        for (int botIndex = 0; botIndex < scenario.getNumBots(); botIndex++) {
            BotForce bf = scenario.getBotForce(botIndex);
            if (bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for (Entity en : bf.getFullEntityList(campaign)) {
                    for (Mounted ammoBin : en.getAmmo()) {
                        int remainingShots = Math.max(0, ammoBin.getUsableShotsLeft() - Compute.randomInt(ammoExpenditureIntensity));
                        ammoBin.setShotsLeft(remainingShots);
                    }
                }
            }
        }
    }

    /**
     * Helper function that re-generates skill levels for all existing units in the scenario
     */
    public static void adjustSkill(AtBDynamicScenario scenario, Campaign campaign,
            ForceAlignment eventRecipient, int skillAdjustment) {
        // We want a non-none Skill Level
        final SkillLevel adjustedSkill = Skills.SKILL_LEVELS[MathUtility.clamp(
                scenario.getEffectiveOpforSkill().ordinal() + skillAdjustment,
                SkillLevel.ULTRA_GREEN.ordinal(), SkillLevel.LEGENDARY.ordinal())];
        // fire up a skill generator set to the appropriate skill model
        final AbstractSkillGenerator abstractSkillGenerator = new TaharqaSkillGenerator();
        abstractSkillGenerator.setLevel(adjustedSkill);

        if (Factions.getInstance().getFaction(scenario.getContract(campaign).getEnemyCode()).isClan()) {
            abstractSkillGenerator.setType(SkillGeneratorType.CLAN);
        }

        // now go through all the opfor entities currently in the scenario
        // and re-generate their
        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce bf = scenario.getBotForce(x);
            if (bf.getTeam() == ScenarioForceTemplate.TEAM_IDS.get(eventRecipient.ordinal())) {
                for (Entity en : bf.getFullEntityList(campaign)) {
                    int[] skills = abstractSkillGenerator.generateRandomSkills(en);
                    en.getCrew().setGunnery(skills[0]);
                    en.getCrew().setPiloting(skills[1]);
                }
            }
        }
    }

    /**
     * Worker function that adjusts the scenario's unit quality by the indicated amount,
     * capped between 0 and 5. Only effective for units generated after the adjustment has taken place.
     * Only capable of being applied to opfor.
     */
    public static void adjustQuality(AtBDynamicScenario scenario, Campaign c, ForceAlignment eventRecipient, int qualityAdjustment) {
        if (eventRecipient != ForceAlignment.Opposing) {
            LogManager.getLogger().warn( "Can only adjust opfor unit quality");
            return;
        }

        int currentQuality = scenario.getContract(c).getEnemyQuality();

        currentQuality += qualityAdjustment;
        currentQuality = Math.min(IUnitRating.DRAGOON_ASTAR, currentQuality);
        currentQuality = Math.max(IUnitRating.DRAGOON_F, currentQuality);
        scenario.setEffectiveOpforQuality(currentQuality);
    }

    /**
     * Helper function that sets up and "ambush", meaning declaring as "hidden" some portion of:
     * all non-airborne units on the specified side
     * that will be on the battlefield at the start of the scenario
     *
     * Also marks any such forces as able to deploy "anywhere".
     */
    public static void setupAmbush(AtBDynamicScenario scenario, Campaign campaign, ForceAlignment eventRecipient) {
        if (eventRecipient == ForceAlignment.Player) {
            for (int forceID : scenario.getForceIDs()) {
                ScenarioForceTemplate forceTemplate = scenario.getPlayerForceTemplates().get(forceID);

                if (forceTemplate.getArrivalTurn() == 0) {
                    forceTemplate.setActualDeploymentZone(Board.START_ANY);

                    // Prevent Hidden Units from Causing Issues if Disabled
                    if (!campaign.getGameOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                        continue;
                    }

                    Force playerForce = campaign.getForce(forceID);

                    // we can hide the "commander tactics skill" number of units, but we must keep at least one visible
                    // as the bot is unable to handle an invisible opfor at the moment.
                    int maxHiddenUnits = Math.min(playerForce.getAllUnits(true).size() - 1, scenario.getLanceCommanderSkill(SkillType.S_TACTICS, campaign));
                    int numHiddenUnits = 0;

                    for (UUID unitID : playerForce.getAllUnits(true)) {
                        if (numHiddenUnits >= maxHiddenUnits) {
                            break;
                        }

                        Unit currentUnit = campaign.getUnit(unitID);
                        // to hide, a unit must exist and not be in mid-air
                        if (currentUnit != null && !currentUnit.getEntity().isAero() && !currentUnit.getEntity().hasETypeFlag(Entity.ETYPE_VTOL)) {
                            currentUnit.getEntity().setHidden(true);
                            numHiddenUnits++;
                        }
                    }
                }
            }
        // logic for bot ambushes is a little different
        } else if (eventRecipient == ForceAlignment.Opposing) {
            for (int x = 0; x < scenario.getNumBots(); x++) {
                BotForce currentBotForce = scenario.getBotForce(x);
                ScenarioForceTemplate forceTemplate = scenario.getBotForceTemplates().get(currentBotForce);

                if (forceTemplate.getArrivalTurn() == 0) {
                    forceTemplate.setActualDeploymentZone(Board.START_ANY);

                    // Prevent Hidden Units from Causing Issues if Disabled
                    if (!campaign.getGameOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                        continue;
                    }

                    int maxHiddenUnits = currentBotForce.getFullEntityList(campaign).size() / 2;
                    int numHiddenUnits = 0;

                    for (Entity entity : currentBotForce.getFullEntityList(campaign)) {
                        if (numHiddenUnits >= maxHiddenUnits) {
                            break;
                        }

                        // to hide, a unit must not be in mid-air
                        if (!entity.isAero() && !entity.hasETypeFlag(Entity.ETYPE_VTOL)) {
                            entity.setHidden(true);
                            numHiddenUnits++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Worker method that turns all your allies into treacherous enemies
     * Look into a variant of this where some hostiles defect or go rogue?
     * @param scenario The scenario to process.
     * @param recipient Who's switching sides. Only valid recipient is Allied currently.
     */
    public static void switchSides(AtBDynamicScenario scenario, ForceAlignment recipient) {
        // this operation is only meaningful for the allied forces currently
        if (recipient != ForceAlignment.Allied) {
            return;
        }

        int team = ScenarioForceTemplate.TEAM_IDS.get(recipient.ordinal());
        int oppositeTeam = ScenarioForceTemplate.TEAM_IDS.get(ForceAlignment.Opposing.ordinal());

        for (int x = 0; x < scenario.getNumBots(); x++) {
            BotForce bf = scenario.getBotForce(x);
            if (bf.getTeam() == team) {
                bf.setTeam(oppositeTeam);
            }
        }
    }

    /**
     * Appends the given text to the scenario briefing.
     * @param scenario The scenario to modify.
     * @param additionalBriefingText The additional briefing text.
     */
    public static void appendScenarioBriefingText(AtBDynamicScenario scenario, String additionalBriefingText) {
        scenario.setDesc(String.format("%s\n\n%s", scenario.getDescription(), additionalBriefingText));
    }

    /**
     * Applies an objective to the scenario.
     */
    public static void applyObjective(AtBDynamicScenario scenario, Campaign campaign, ScenarioObjective objective, EventTiming timing) {
        // Only apply objective if it isn't already added
        if (!scenario.getTemplate().scenarioObjectives.contains(objective)) {
            // if we're doing this before force generation, just add the objective for future translation
            scenario.getTemplate().scenarioObjectives.add(objective);

            // if we're doing it after, we have to translate it individually
            if (timing == EventTiming.PostForceGeneration) {
                ScenarioObjective actualObjective = AtBDynamicScenarioFactory.translateTemplateObjective(scenario, campaign, objective);
                scenario.getScenarioObjectives().add(actualObjective);
            }
        }
    }

    /**
     * Applies an additional event, selected from only modifiers that benefit the player or do not benefit the player
     */
    public static void applyExtraEvent(AtBDynamicScenario scenario, boolean goodEvent) {
        scenario.addScenarioModifier(AtBScenarioModifier.getRandomBattleModifier(scenario.getTemplate().mapParameters.getMapLocation(), goodEvent));
    }

    /**
     * Applies a flat reduction to the reinforcement arrival times, either of player/allied forces or hostile forces.
     */
    public static void applyReinforcementDelayReduction(AtBDynamicScenario scenario, ForceAlignment recipient, int value) {
        if (recipient == ForceAlignment.Allied) {
            scenario.setFriendlyReinforcementDelayReduction(value);
        } else if (recipient == ForceAlignment.Opposing) {
            scenario.setHostileReinforcementDelayReduction(value);
        }
    }
}
