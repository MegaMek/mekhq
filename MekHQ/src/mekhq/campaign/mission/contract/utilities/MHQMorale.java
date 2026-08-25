/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.contract.utilities;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.LEGENDARY;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.getContractDefinition;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.POLITICS;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.STALEMATE;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.EnemyData;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationEnemy;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerEventManager;
import mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.campaign.universe.factionStanding.PerformBatchall;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.view.MoraleBar;

/**
 * Handles contract morale checks and morale/report calculation for campaign missions.
 *
 * <p>Provides utility methods to evaluate contract performance, calculate morale outcomes, and generate result
 * reports based on recent scenarios and modifiers.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class MHQMorale {
    private static final MMLogger LOGGER = MMLogger.create(MHQMorale.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MHQMorale";

    public static final int RALLYING_TARGET_NUMBER = 6;
    public static final int NO_CHANGE_TARGET_NUMBER = 7;
    public static final int WAVERING_TARGET_NUMBER = 8;

    /**
     * Returns the localized title used for the morale check report UI.
     *
     * <p>The value is loaded from {@link #RESOURCE_BUNDLE} under the key {@code MHQMorale.check.title} and is
     * intended for use in dialogs or notifications that initiate a morale check.</p>
     *
     * @return the localized morale check title string
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static String getFormattedTitle() {
        return getTextAt(RESOURCE_BUNDLE, "MHQMorale.check.title");
    }

    /**
     * Represents possible performance outcomes for a contract's recent scenarios.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public enum PerformanceOutcome {
        DECISIVE_VICTORY,
        VICTORY,
        DRAW,
        DEFEAT,
        DECISIVE_DEFEAT
    }

    /**
     * Represents possible morale check results for a contract.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public enum MoraleOutcome {
        RALLYING,
        WAVERING,
        UNCHANGED
    }

    /**
     * Performs a morale check for the given contract, using the current date and modifier values.
     *
     * <p>Evaluates all relevant scenarios within the past month, calculates performance and morale outcomes, and
     * returns a formatted status report.</p>
     *
     * @param today                   The current date of the check.
     * @param contract                The contract to check morale for.
     * @param decisiveVictoryModifier The modifier to apply for decisive victories.
     * @param victoryModifier         The modifier to apply for normal victories.
     * @param decisiveDefeatModifier  The modifier to apply for decisive defeats.
     * @param defeatModifier          The modifier to apply for normal defeats.
     *
     * @return A formatted string containing the morale check report.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static String performMoraleCheck(final LocalDate today, final AbstractContract contract,
          final int decisiveVictoryModifier, final int victoryModifier, final int decisiveDefeatModifier,
          final int defeatModifier) {
        final TargetRoll targetNumber = new TargetRoll();

        // Add modifiers to the target number
        int reliability = getReliability(contract.getEnemyForceSkill().getAdjustedValue(), contract.getEnemyFaction());
        targetNumber.addModifier(reliability, getTextAt(RESOURCE_BUNDLE, "MHQMorale.modifier.reliability"));

        int performanceModifier = getPerformanceModifier(today, contract, decisiveVictoryModifier, victoryModifier,
              decisiveDefeatModifier, defeatModifier);
        targetNumber.addModifier(performanceModifier, getTextAt(RESOURCE_BUNDLE, "MHQMorale.modifier.performance"));

        // The actual check
        int roll = d6(2);
        int modifiedRoll = roll + targetNumber.getValue();
        MoraleOutcome moraleOutcome = getMoraleOutcome(contract, modifiedRoll);

        // Generate and return the report
        PerformanceOutcome performanceOutcome = getOutcome(decisiveVictoryModifier, victoryModifier,
              decisiveDefeatModifier, defeatModifier, performanceModifier);
        return getReport(reliability, performanceOutcome, performanceModifier, moraleOutcome, roll, modifiedRoll);
    }

    /**
     * Creates a formatted morale report string based on the performance and morale outcomes.
     *
     * @param reliability         The reliability modifier for the OpFor
     * @param performanceOutcome  The result of performance evaluation.
     * @param performanceModifier The performance modifier
     * @param moraleOutcome       The result of the morale check.
     * @param roll                The 2d6 roll without modifiers
     * @param modifiedRoll        The final morale check roll value.
     *
     * @return The formatted morale check report.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getReport(int reliability, PerformanceOutcome performanceOutcome, int performanceModifier,
          MoraleOutcome moraleOutcome, int roll, int modifiedRoll) {
        String reliabilityColor = getWarningColor();
        if (reliability < -1) {
            reliabilityColor = getPositiveColor();
        } else if (reliability > 1) {
            reliabilityColor = getNegativeColor();
        }

        String performanceColor = switch (performanceOutcome) {
            case DECISIVE_VICTORY -> getAmazingColor();
            case VICTORY -> getPositiveColor();
            case DRAW -> getWarningColor();
            case DEFEAT, DECISIVE_DEFEAT -> getNegativeColor();
        };

        String performanceText = getTextAt(RESOURCE_BUNDLE,
              "MHQMorale.performanceOutcome." + performanceOutcome.name());

        String moraleColor = switch (moraleOutcome) {
            case RALLYING -> getNegativeColor();
            case WAVERING -> getPositiveColor();
            case UNCHANGED -> getWarningColor();
        };
        String outcome = getTextAt(RESOURCE_BUNDLE, "MHQMorale.moraleOutcome." + moraleOutcome.name());

        return getFormattedTextAt(RESOURCE_BUNDLE, "MHQMorale.check.report",
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG,
              spanOpeningWithCustomColor(reliabilityColor),
              reliability >= 0 ? "+" + reliability : reliability,
              spanOpeningWithCustomColor(performanceColor),
              performanceText,
              performanceModifier >= 0 ? "+" + performanceModifier : performanceModifier,
              modifiedRoll,
              roll,
              modifiedRoll - roll,
              spanOpeningWithCustomColor(moraleColor),
              outcome
        );
    }


    /**
     * Determines the morale outcome based on the contract's current morale level and roll value. Updates the morale
     * level in the contract if it changes.
     *
     * <p>A lower roll is better for the OpFor.</p>
     *
     * @param contract The contract to check and update morale for.
     * @param roll     The result of the morale check roll.
     *
     * @return The resulting MoraleOutcome after the check.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static MoraleOutcome getMoraleOutcome(AbstractContract contract, int roll) {
        ContractMoraleLevel currentMoraleLevel = contract.getMoraleLevel();
        ContractMoraleLevel updatedMoraleLevel = currentMoraleLevel;
        MoraleOutcome moraleOutcome;

        if (roll <= RALLYING_TARGET_NUMBER) {
            moraleOutcome = MoraleOutcome.RALLYING;
            updatedMoraleLevel = switch (currentMoraleLevel) {
                case ROUTED -> ContractMoraleLevel.CRITICAL;
                case CRITICAL -> ContractMoraleLevel.WEAKENED;
                case WEAKENED -> ContractMoraleLevel.STALEMATE;
                case STALEMATE -> ContractMoraleLevel.ADVANCING;
                case ADVANCING -> ContractMoraleLevel.DOMINATING;
                case DOMINATING, OVERWHELMING -> ContractMoraleLevel.OVERWHELMING;
            };
        } else if (roll >= WAVERING_TARGET_NUMBER) {
            moraleOutcome = MoraleOutcome.WAVERING;
            updatedMoraleLevel = switch (currentMoraleLevel) {
                case ROUTED, CRITICAL -> ContractMoraleLevel.ROUTED;
                case WEAKENED -> ContractMoraleLevel.CRITICAL;
                case STALEMATE -> ContractMoraleLevel.WEAKENED;
                case ADVANCING -> ContractMoraleLevel.STALEMATE;
                case DOMINATING -> ContractMoraleLevel.ADVANCING;
                case OVERWHELMING -> ContractMoraleLevel.DOMINATING;
            };
        } else {
            moraleOutcome = MoraleOutcome.UNCHANGED;
            // Level remains unchanged
        }

        // Only update if the morale level actually changed
        if (updatedMoraleLevel != currentMoraleLevel) {
            contract.changeMorale(updatedMoraleLevel);
        }

        return moraleOutcome;
    }

    /**
     * Maps the calculated performance modifier to a {@link PerformanceOutcome} value.
     *
     * @param decisiveVictoryModifier Modifier value for decisive victories.
     * @param victoryModifier         Modifier value for normal victories.
     * @param decisiveDefeatModifier  Modifier value for decisive defeats.
     * @param defeatModifier          Modifier value for normal defeats.
     * @param performanceModifier     The final calculated performance modifier.
     *
     * @return The corresponding PerformanceOutcome.
     *
     * @author Illiani
     * @since 0.50.10
     */
    static PerformanceOutcome getOutcome(int decisiveVictoryModifier, int victoryModifier,
          int decisiveDefeatModifier, int defeatModifier, int performanceModifier) {
        if (performanceModifier == decisiveVictoryModifier) {
            return PerformanceOutcome.DECISIVE_VICTORY;
        } else if (performanceModifier == victoryModifier) {
            return PerformanceOutcome.VICTORY;
        } else if (performanceModifier == decisiveDefeatModifier) {
            return PerformanceOutcome.DECISIVE_DEFEAT;
        } else if (performanceModifier == defeatModifier) {
            return PerformanceOutcome.DEFEAT;
        } else {
            return PerformanceOutcome.DRAW;
        }
    }

    /**
     * Calculates the overall reliability modifier for an enemy force based on the contract's parameters and the
     * characteristics of the opposing faction.
     *
     * <p>This value determines how susceptible the OpFor is to morale loss or reliability degradation during AtB
     * campaign events. A <b>lower</b> reliability modifier means the enemy is <b>less likely</b> to lose morale.</p>
     *
     * <p>The calculation proceeds in three stages:</p>
     * <ol>
     *     <li>Start with the enemy's adjusted skill level from the contract.</li>
     *     <li>Apply faction-specific adjustments:
     *     <ul>
     *         <li><strong>Clan factions:</strong> receive a +1 effective skill boost, capped at {@code LEGENDARY}.
     *         This increases their effective reliability before the base modifier is applied.</li>
     *     </ul>
     *     </li>
     *     <li>Convert the resulting skill level into a base reliability modifier using
     *     {@link #getReliabilityModifier(int)}.</li>
     *     <li>Apply faction-type trait adjustments:
     *     <ul>
     *         <li><strong>Rebels, minor powers, mercenaries, pirates:</strong> {@code +1} modifier (better for the
     *         player).</li>
     *         <li><strong>Clans:</strong> an additional {@code -1} modifier ("double-dip"), making Clan morale
     *         harder to break.</li>
     *     </ul>
     *     </li>
     * </ol>
     *
     * @param adjustedSkillLevel the skill rating to evaluate (higher skilled OpFors are harder to break)
     * @param enemyFaction       the faction the player is facing (some factions are inherently easier or harder to
     *                           break)
     *
     * @return the final reliability modifier after applying skill, faction, and trait adjustments
     *
     * @author Illiani
     * @since 0.50.10
     */
    static int getReliability(int adjustedSkillLevel, Faction enemyFaction) {
        int reliabilityModifier;

        // Clan enemies get a reliability boost: set at Legendary or +1
        boolean enemyIsClan = enemyFaction.isClan();
        if (enemyIsClan) {
            // It's important to note that here a positive modifier is bad for the player, as when fed into
            // getReliabilityModifier() it will make the OpFor harder to rout (not easier, as is the case with all
            // other positive modifiers)
            adjustedSkillLevel = Math.min(LEGENDARY.getAdjustedValue(), adjustedSkillLevel + 1);
        }

        // Adjust for adjusted skill level
        reliabilityModifier = getReliabilityModifier(adjustedSkillLevel);

        // Adjust for special enemy traits
        if (enemyFaction.isRebel() ||
                  enemyFaction.isMinorPower() ||
                  enemyFaction.isMercenary() ||
                  enemyFaction.isPirate()) {
            reliabilityModifier++; // Good for the player
        } else if (enemyIsClan) { // Clan forces get to double-dip
            reliabilityModifier--; // Bad for the player
        }

        return reliabilityModifier;
    }

    /**
     * Calculates the reliability modifier based on the provided skill value.
     *
     * <p>This modifier influences how likely the opposing force is to suffer morale loss or reliability degradation
     * during campaign events. Lower skills improve reliability for the player, while extremely high skills reduce it to
     * reflect elite forces being more difficult to destabilize.</p>
     *
     * <ul>
     *     <li>If {@code skill <= GREEN.getAdjustedValue()}, the modifier is {@code +1}, improving reliability for
     *     the player.</li>
     *     <li>If {@code skill >= ELITE.getAdjustedValue()}, the modifier is {@code -1}, making reliability checks
     *     harder for the player.</li>
     *     <li>All values between those thresholds return {@code 0}.</li>
     * </ul>
     *
     * @param adjustedSkillLevel the skill rating to evaluate, compared against the adjusted GREEN and ELITE thresholds
     *
     * @return {@code +1} for low-skill forces (GREEN or worse), {@code -1} for elite forces (ELITE or better),
     *       and {@code 0} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static int getReliabilityModifier(int adjustedSkillLevel) {
        int reliabilityModifier = 0;

        if (adjustedSkillLevel <= GREEN.getAdjustedValue()) {
            reliabilityModifier = 1;
        } else if (adjustedSkillLevel >= ELITE.getAdjustedValue()) {
            reliabilityModifier = -1;
        }

        return reliabilityModifier;
    }

    /**
     * Calculates the performance modifier for recent scenario outcomes within the contract. Considers victories,
     * defeats, and special outcome scenarios within the last month.
     *
     * <p>A higher modifier means the OpFor is more likely to lose morale.</p>
     *
     * @param today                   The current date for modifier evaluation.
     * @param contract                The contract to evaluate scenarios for.
     * @param decisiveVictoryModifier Modifier value for decisive victories.
     * @param victoryModifier         Modifier value for normal victories.
     * @param decisiveDefeatModifier  Modifier value for decisive defeats.
     * @param defeatModifier          Modifier value for normal defeats.
     *
     * @return The computed performance modifier to be used in morale calculation.
     *
     * @author Illiani
     * @since 0.50.10
     */
    static int getPerformanceModifier(LocalDate today, AbstractContract contract, int decisiveVictoryModifier,
          int victoryModifier, int decisiveDefeatModifier, int defeatModifier) {
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        for (Scenario scenario : contract.getScenarios()) {
            LocalDate scenarioDate = scenario.getDate();
            // Scenario date can be null if the scenario hasn't been found in StratCon
            if (scenarioDate == null) {
                LOGGER.info("{} has not been found yet. Skipping", scenario.getName());
                continue;
            }
            // Skip scenarios that occurred more than a month ago
            if (lastMonth.isAfter(scenarioDate)) {
                LOGGER.info("{} occurred more than a month ago. Skipping", scenario.getName());
                continue;
            }

            ScenarioStatus scenarioStatus = scenario.getStatus();
            // We're trying to track down an instance where a scenario can be both resolved and current. This is
            // logging that will be de-escalated to debug once we've got enough information to track the cause.
            LOGGER.info("Processing scenario {} ({}) date={} resolved={} ovlVic={} ovlDef={} decVic={} decDef={} " +
                              "pyrrhic={} fib={}",
                  scenario.getName(),
                  scenario.getId(),
                  scenario.getDate(),
                  scenario.getStatus(),
                  scenario.getStatus().isOverallVictory(),
                  scenario.getStatus().isOverallDefeat(),
                  scenario.getStatus().isDecisiveVictory(),
                  scenario.getStatus().isDecisiveDefeat(),
                  scenario.getStatus().isPyrrhicVictory(),
                  scenario.getStatus().isFleetInBeing());
            LOGGER.info("Read in status for {} this should match the block above: {}", scenario.getName(),
                  scenarioStatus);

            // Decisive Defeat or Refused Engagement count as 2 defeats
            if (scenarioStatus.isDecisiveDefeat() || scenarioStatus.isRefusedEngagement()) {
                defeats += 2;
                continue;
            }

            // Decisive Victory counts as 2 victories
            if (scenarioStatus.isDecisiveVictory()) {
                victories += 2;
                continue;
            }

            // Pyrrhic Victory counts as both a victory and a defeat (neutral consequence)
            if (scenarioStatus.isPyrrhicVictory()) {
                defeats++;
                victories++;
                continue;
            }

            // Fleet in being counts as a single defeat
            if (scenarioStatus.isFleetInBeing()) {
                defeats++;
                continue;
            }

            // Overall Victory/Defeat, if not otherwise noted above
            if (scenarioStatus.isOverallVictory()) {
                victories++;
            } else if (scenarioStatus.isOverallDefeat()) {
                defeats++;
            }
        }

        LOGGER.info("Total victory points for Morale: {}", victories);
        LOGGER.info("Total defeat points for Morale: {}", defeats);

        // Compute performance modifier using concise math logic
        if (victories > defeats) {
            return (victories >= defeats * 2) ? decisiveVictoryModifier : victoryModifier;
        } else if (defeats > victories) {
            return (defeats >= victories * 2) ? decisiveDefeatModifier : defeatModifier;
        }
        return 0;
    }

    /**
     * Applies a simplified morale check as the result of a combat challenge, updating the contract's morale level and
     * triggering routed behavior if necessary.
     *
     * <p>This method does <em>not</em> use the full morale calculation. Instead, it forces a fixed roll based on the
     * overall outcome of the most recent scenario:</p>
     * <ul>
     *     <li>Overall victory: roll is treated as {@link #WAVERING_TARGET_NUMBER} (OpFor morale declines).</li>
     *     <li>Overall defeat: roll is treated as {@link #RALLYING_TARGET_NUMBER} (OpFor morale improves).</li>
     *     <li>Otherwise: roll defaults to {@link #NO_CHANGE_TARGET_NUMBER}.</li>
     * </ul>
     *
     * <p>After applying the morale outcome, if the contract's morale level is routed,
     * {@link #routedMoraleUpdate(Campaign, AbstractContract)} is invoked to handle follow-up effects such as early
     * contract end or prisoner handling.</p>
     *
     * @param campaign       the active campaign containing contract and prisoner state
     * @param contract       the contract whose morale is being updated
     * @param scenarioStatus the outcome of the combat challenge scenario used to determine the forced roll
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processCombatChallengeResults(Campaign campaign, AbstractContract contract,
          ScenarioStatus scenarioStatus) {
        int forcedRoll = NO_CHANGE_TARGET_NUMBER;

        if (scenarioStatus.isOverallVictory()) {
            forcedRoll = WAVERING_TARGET_NUMBER;
        } else if (scenarioStatus.isOverallDefeat()) {
            forcedRoll = RALLYING_TARGET_NUMBER;
        }

        getMoraleOutcome(contract, forcedRoll);

        if (contract.getMoraleLevel().isRouted()) {
            routedMoraleUpdate(campaign, contract);
        }
    }

    /**
     * Applies follow-up effects when a contract's morale has reached a routed state.
     *
     * <p>If the contract is not in a routed morale state, this method performs no action.</p>
     *
     * <p>When the contract <b>is</b> routed, one of two behaviors is applied, depending on whether the contract can
     * end early and whether it is a garrison type:</p>
     *
     * <ul>
     *     <li><b>Garrison or non-early finish contracts</b>:
     *     <ul>
     *         <li>Sets a {@code routEnd} date a short time in the future, based on a random {@code d6} roll,
     *         resulting in a delay of roughly one to three months from the current in-game date.</li>
     *         <li>Processes friendly and enemy prisoners via {@link PrisonerMissionEndEvent}, if any are present.</li>
     *         <li>Resets the temporary prisoner capacity to {@link PrisonerEventManager#DEFAULT_TEMPORARY_CAPACITY}.</li>
     *     </ul>
     *     </li>
     *
     *     <li><b>Non-garrison contracts that can end early</b>:
     *     <ul>
     *         <li>Displays an immersive notification indicating that the contract is ending early due to routed
     *         morale.</li>
     *         <li>Calculates the remaining payout for the routed contract by multiplying the monthly payout by the
     *         number of months remaining after the next in-game day and stores the result on the contract as the
     *         routed payout.</li>
     *         <li>Sets the contract end date to the next in-game day.</li>
     *     </ul>
     *     </li>
     * </ul>
     *
     * <p>This method should be called only after {@link AbstractContract#getMoraleLevel()} has been updated to a routed
     * state, and it assumes that the supplied {@link Campaign} and {@link AbstractContract} are consistent with that
     * state.</p>
     *
     * @param campaign the campaign providing time, contract, and prisoner state
     * @param contract the routed contract whose end behavior and prisoner state are being updated
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void routedMoraleUpdate(Campaign campaign, AbstractContract contract) {
        if (contract.getMoraleLevel().isRouted()) {
            LocalDate today = campaign.getLocalDate();
            StratConCampaignState campaignState = contract.getStratConCampaignState();
            boolean canEarlyFinish = campaignState == null || campaignState.allowEarlyVictory();

            // Additional morale updates if morale level is set to 'Routed' and the contract type is either a garrison
            // type or doesn't allow early contract completion
            if (!canEarlyFinish || contract.getObjectiveType().isGarrisonType()) {
                contract.changeMorale(today.plusMonths(max(1, d6() - 3)).minusDays(1));

                PrisonerMissionEndEvent prisoners = new PrisonerMissionEndEvent(campaign, contract);
                if (!campaign.getPlayerForce().getHumanResources().getFriendlyPrisoners().isEmpty()) {
                    prisoners.handlePrisoners(true, true);
                }

                if (!campaign.getPlayerForce().getHumanResources().getCurrentPrisoners().isEmpty()) {
                    prisoners.handlePrisoners(true, false);
                }

                campaign.getPlayerForce().setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
            } else {
                new ImmersiveDialogNotification(campaign, getFormattedTextAt(RESOURCE_BUNDLE,
                      "stratCon.earlyContractEnd.objectives", contract.getName()), true);
                int remainingMonths = (int) contract.getMonthsLeft(campaign.getLocalDate().plusDays(1));
                contract.changeMorale(today.plusDays(1), contract.getMonthlyPayOut().multipliedBy(remainingMonths));
            }
        }
    }

    /**
     * Checks and updates the morale which depends on various conditions such as the rout end date, skill levels,
     * victories, defeats, etc. This method also updates the enemy status based on the morale level.
     */
    public static void checkMorale(Campaign campaign, AbstractContract contract, boolean showDialog) {
        LocalDate today = campaign.getLocalDate();
        // If there is a rout end date, and it's past today, update morale and enemy state accordingly
        if (contract.getRoutEndDate() != null) {
            // Check whether any current rout continues beyond its expected date. This is only applicable for
            // Garrison Type contracts. For all other types we reinforce immediately
            boolean routContinue = contract.getObjectiveType().isGarrisonType() && randomInt(4) == 0;
            if (routContinue) {
                return;
            }

            if (today.isAfter(contract.getRoutEndDate())) {
                int roll = randomInt(8);

                // We use variable morale levels to spike morale up to a value above Stalemate. This works with the
                // regenerated Scenario Odds to create very high intensity spikes in otherwise low-key Garrison-type
                // contracts.
                ContractMoraleLevel newMoraleLevel = switch (roll) {
                    case 2, 3, 4, 5 -> ADVANCING;
                    case 6, 7 -> DOMINATING;
                    case 8 -> OVERWHELMING;
                    default -> STALEMATE; // 0-1
                };

                // If we have a StratCon enabled contract, regenerate Scenario Odds
                if (contract.getStratConCampaignState() != null) {
                    StratConContractDefinition contractDefinition = getContractDefinition(contract.getObjectiveType());

                    if (contractDefinition != null) {
                        for (StratConTrackState trackState : contract.getStratConCampaignState().getTracks()) {
                            int scenarioOdds = StratConContractInitializer.getScenarioOdds(contractDefinition);

                            trackState.setScenarioOdds(scenarioOdds);
                        }
                    }
                }

                contract.changeMorale(newMoraleLevel, null);

                String key = "routEnded.reinforcements";
                if (contract.getObjectiveType().isGarrisonDuty() || contract.getObjectiveType().isRetainer()) {
                    updateEnemy(campaign, today, contract); // mix it up a little
                    key = "routEnded.aNewChallenger";
                }

                if (showDialog) {
                    new ImmersiveDialogSimple(campaign,
                          contract.getEmployerLiaison(),
                          null,
                          getFormattedTextAt(RESOURCE_BUNDLE,
                                key,
                                campaign.getCommanderAddress(),
                                FactionStandingUtilities.getFactionName(contract.getEnemyFaction(), today.getYear())),
                          null,
                          null,
                          null,
                          false);
                }
            }

            return;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        String moraleReport = MHQMorale.performMoraleCheck(today, contract,
              campaignOptions.get(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_VICTORY_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_DEFEAT_EFFECT));
        String flavorText = MHQMorale.getFormattedTitle()
                                  + "<h2 style='text-align:center;'>" + contract.getName() + "</h2>"
                                  + MoraleBar.getMoraleDisplay(contract).tooltip();
        if (showDialog) {
            new ImmersiveDialogNotification(campaign, flavorText, moraleReport, MoraleBar.createDialogPanel(contract),
                  true);
        }

        MHQMorale.routedMoraleUpdate(campaign, contract);
    }

    public static void updateEnemy(Campaign campaign, LocalDate today, AbstractContract contract) {
        updateEnemy(campaign, today, null, contract);
    }

    /**
     * Updates the enemy faction and enemy bot name for this contract.
     *
     * @param campaign  The current campaign.
     * @param today     The current LocalDate object.
     * @param enemyCode {@code Nullable} the code for the new faction, if {@code null} an appropriate random faction
     *                  will be used
     */
    public static void updateEnemy(Campaign campaign, LocalDate today, @Nullable String enemyCode,
          AbstractContract contract) {
        EnemyData newEnemyData = ChaosContractDeterminationEnemy.generateEnemyFactionForObjective(campaign,
              campaign.getPlayerForce().getForceDetachment().getCurrentLocation(),
              campaign.getLocalDate(),
              contract.getEmployerFaction(),
              contract.getObjectiveType()
        );

        // The Batchall, standing update, and salvage clause below all concern the newly generated enemy, so read its
        // faction here rather than the outgoing one still on the contract.
        Faction newEnemyFaction = newEnemyData.getFaction();

        boolean tracksStanding = campaign.getCampaignOptions().get(CampaignOption.TRACK_FACTION_STANDING);
        FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();
        double regardMultiplier = campaign.getCampaignOptions().get(CampaignOption.REGARD_MULTIPLIER);
        String campaignFactionCode = campaign.getPlayerForce().getFaction().getShortName();

        // A Clan challenger issues a Batchall before battle.
        if (campaign.getCampaignOptions().get(CampaignOption.USE_GENERIC_BATTLE_VALUE) &&
                  newEnemyFaction.performsBatchalls()) {
            boolean allowBatchalls = true;
            if (campaign.getCampaignOptions().isUseFactionStandingBatchallRestrictionsSafe()) {
                double regard = factionStandings.getRegardForFaction(newEnemyFaction.getShortName(), true);
                allowBatchalls = FactionStandingUtilities.isBatchallAllowed(regard);
            }

            if (allowBatchalls) {
                PerformBatchall batchallDialog = new PerformBatchall(campaign,
                      newEnemyData.opposingCommander(),
                      enemyCode);

                boolean acceptedBatchall = batchallDialog.isBatchallAccepted();

                if (!acceptedBatchall && tracksStanding) {
                    List<String> reports = factionStandings.processRefusedBatchall(campaignFactionCode, enemyCode,
                          today.getYear(), regardMultiplier);

                    for (String report : reports) {
                        campaign.addReport(GENERAL, report);
                    }
                }
                newEnemyData = new EnemyData(newEnemyData, acceptedBatchall);
            }
        }

        // Commit the new enemy before deriving anything from the contract below.
        contract.setEnemyData(newEnemyData);

        if (tracksStanding) {
            // Whenever we dynamically change the enemy faction, we update standing accordingly. A covert sponsor, if
            // any, takes the standing change in the visible enemy's place.
            Faction standingEnemyFaction = contract.getStandingEnemyFaction();
            String report = factionStandings.processContractAccept(campaignFactionCode, standingEnemyFaction, today,
                  regardMultiplier, contract.getLengthInMonths());
            if (report != null) {
                campaign.addReport(POLITICS, report);
            }
        }

        // Check for emergency clause (this can trigger multiple times if the enemy faction keeps changing to a Clan
        // faction. This can be seen as the employer getting increasingly desperate and wanting to keep the player on
        // side.
        checkForSpecialClanSalvageClause(campaign, today, newEnemyFaction, contract);
    }

    /**
     * Checks for and applies a special emergency salvage clause when fighting Clan forces prior to or during the Battle
     * of Tukayyid.
     *
     * <p>If the employer is non-Clan and the enemy is Clan, and the current date is on or before the Battle of
     * Tukayyid, the salvage percentage is increased by 25% (up to a minimum of 100%) and salvage exchange is enabled.
     * An immersive dialog is displayed to inform the player of the contract adjustment.</p>
     *
     * @param campaign     The current campaign instance.
     * @param today        The current game date to check against the Tukayyid threshold.
     * @param enemyFaction The faction being fought in the current contract or mission.
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static void checkForSpecialClanSalvageClause(Campaign campaign, LocalDate today, Faction enemyFaction,
          AbstractContract contract) {
        if (!contract.getEmployerFaction().isClan() && enemyFaction.isClan()) {
            if (!today.isAfter(BATTLE_OF_TUKAYYID)) {
                contract.setContractTerms(
                      contract.getContractTerms().withSalvageRights(ChaosContractStepsTable.STEP_FIVE));

                String message = getTextAt(RESOURCE_BUNDLE, "emergencySalvageClause.message");
                new ImmersiveDialogSimple(campaign,
                      contract.getEmployerLiaison(),
                      null,
                      message,
                      null,
                      null,
                      null,
                      false);
            }
        }
    }

    public static void determineStartingMorale(AbstractContract contract, Campaign campaign) {
        if (contract.getObjectiveType().isGarrisonDuty() || contract.getObjectiveType().isRetainer()) {
            contract.changeMorale(ContractMoraleLevel.ROUTED);

            LocalDate startDate = contract.getStartDate();
            startDate = startDate == null ? campaign.getLocalDate() : startDate;
            LocalDate routEnd = startDate.plusMonths(max(1, Compute.d6() - 3)).minusDays(1);
            contract.changeMorale(routEnd);
        } else {
            MHQMorale.checkMorale(campaign, contract, false);

            if (contract.getMoraleLevel().isRouted()) {
                contract.changeMorale(ContractMoraleLevel.CRITICAL);
            }

            if (contract.getObjectiveType().isReliefDuty()) {
                int currentMoraleLevel = min(6, contract.getMoraleLevel().ordinal() + 1);

                contract.changeMorale(ContractMoraleLevel.parseFromString(String.valueOf(currentMoraleLevel)));
            }
        }
    }
}
