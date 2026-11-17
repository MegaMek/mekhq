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
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.LEGENDARY;
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

import jakarta.annotation.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics;
import mekhq.campaign.digitalGM.stratCon.StratConEscalation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.mission.scenarios.ScenarioType;
import mekhq.campaign.universe.Faction;
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
     * How many points one side must out-score the other by, over a month, for the result to be decisive (see
     * {@link PerformanceTally#outcome()}).
     */
    public static final int DECISIVE_MARGIN = 3;

    /**
     * A Garrison Duty or Retainer contract's enemy raids on a roll of 0 on a die of this size each month: a 1 in 4
     * chance.
     */
    public static final int RAID_DIE_SIZE = 4;

    /**
     * The share of a contract's Contract Victory Point target a monthly setback costs (see
     * {@link #applyMonthlySetback}).
     */
    public static final double SETBACK_VICTORY_POINT_SHARE = 0.1;

    /** The scenario modifier for an enemy fighting short of ammunition. */
    public static final String LOW_AMMUNITION_MODIFIER = "EnemyLowAmmunition.json";

    /** The scenario modifier for an enemy fighting with damage it has had no chance to repair. */
    public static final String POOR_REPAIR_MODIFIER = "EnemyPoorRepair.json";

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
        DECISIVE_DEFEAT;

        /**
         * @return the localized name of this outcome, as the morale check report shows it
         *
         * @author Illiani
         * @since 0.51.01
         */
        public String getLabel() {
            return getTextAt(RESOURCE_BUNDLE, "MHQMorale.performanceOutcome." + name());
        }

        /**
         * @return the report color for this outcome, from the player's point of view
         *
         * @author Illiani
         * @since 0.51.01
         */
        public String getColor() {
            return switch (this) {
                case DECISIVE_VICTORY -> getAmazingColor();
                case VICTORY -> getPositiveColor();
                case DRAW -> getWarningColor();
                case DEFEAT, DECISIVE_DEFEAT -> getNegativeColor();
            };
        }
    }

    /**
     * The victory and defeat points a contract's recent scenarios count for in the morale check (see
     * {@link #getPerformanceTally(LocalDate, AbstractContract)}).
     *
     * @param victoryPoints the victory points
     * @param defeatPoints  the defeat points
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record PerformanceTally(int victoryPoints, int defeatPoints) {
        /**
         * @return victory points less defeat points: positive when the player is ahead, negative when behind
         */
        public int margin() {
            return victoryPoints - defeatPoints;
        }

        /**
         * Rates the tally by its {@link #margin()}. The side with more points wins, and winning by at least
         * {@link MHQMorale#DECISIVE_MARGIN} points is decisive; an even tally is a draw.
         *
         * <p>The margin, rather than the ratio, is what counts: a month of sustained success against some setbacks
         * (say 9 to 5) is decisive, while a single lucky win in an otherwise quiet month (1 to 0) is not.</p>
         *
         * @return the performance outcome this tally earns
         */
        public PerformanceOutcome outcome() {
            int margin = margin();
            if (margin >= DECISIVE_MARGIN) {
                return PerformanceOutcome.DECISIVE_VICTORY;
            } else if (margin > 0) {
                return PerformanceOutcome.VICTORY;
            } else if (margin <= -DECISIVE_MARGIN) {
                return PerformanceOutcome.DECISIVE_DEFEAT;
            } else if (margin < 0) {
                return PerformanceOutcome.DEFEAT;
            }
            return PerformanceOutcome.DRAW;
        }
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

        // The outcome is settled first and the modifier read from it, so the report names the outcome actually earned
        // even when campaign options give two outcomes the same modifier.
        PerformanceOutcome performanceOutcome = getPerformanceTally(today, contract).outcome();
        int performanceModifier = getPerformanceModifier(performanceOutcome, decisiveVictoryModifier, victoryModifier,
              decisiveDefeatModifier, defeatModifier);
        targetNumber.addModifier(performanceModifier, getTextAt(RESOURCE_BUNDLE, "MHQMorale.modifier.performance"));

        // The actual check
        int roll = d6(2);
        int modifiedRoll = roll + targetNumber.getValue();
        MoraleOutcome moraleOutcome = getMoraleOutcome(contract, modifiedRoll);

        // Generate and return the report
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

        String performanceColor = performanceOutcome.getColor();
        String performanceText = performanceOutcome.getLabel();

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
                case SCATTERED -> ContractMoraleLevel.CRITICAL;
                case CRITICAL -> ContractMoraleLevel.WEAKENED;
                case WEAKENED -> ContractMoraleLevel.STALEMATE;
                case STALEMATE -> ContractMoraleLevel.ADVANCING;
                case ADVANCING -> ContractMoraleLevel.DOMINATING;
                case DOMINATING, OVERWHELMING -> ContractMoraleLevel.OVERWHELMING;
            };
        } else if (roll >= WAVERING_TARGET_NUMBER) {
            moraleOutcome = MoraleOutcome.WAVERING;
            updatedMoraleLevel = switch (currentMoraleLevel) {
                case SCATTERED, CRITICAL -> ContractMoraleLevel.SCATTERED;
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
     * Returns the performance modifier the campaign options assign to a {@link PerformanceOutcome}. A draw is always
     * {@code 0}.
     *
     * <p>A higher modifier means the OpFor is more likely to lose morale.</p>
     *
     * @param performanceOutcome      The outcome to look up.
     * @param decisiveVictoryModifier Modifier value for decisive victories.
     * @param victoryModifier         Modifier value for normal victories.
     * @param decisiveDefeatModifier  Modifier value for decisive defeats.
     * @param defeatModifier          Modifier value for normal defeats.
     *
     * @return The performance modifier for the outcome.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getPerformanceModifier(PerformanceOutcome performanceOutcome, int decisiveVictoryModifier,
          int victoryModifier, int decisiveDefeatModifier, int defeatModifier) {
        return switch (performanceOutcome) {
            case DECISIVE_VICTORY -> decisiveVictoryModifier;
            case VICTORY -> victoryModifier;
            case DRAW -> 0;
            case DEFEAT -> defeatModifier;
            case DECISIVE_DEFEAT -> decisiveDefeatModifier;
        };
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
            // getReliabilityModifier() it will make the OpFor harder to break (not easier, as is the case with all
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
     * Tallies the victory and defeat points the contract's scenarios within the last month count for in the morale
     * check.
     *
     * <ul>
     *     <li>Decisive victory: 2 victory points</li>
     *     <li>Victory: 1 victory point</li>
     *     <li>Pyrrhic victory: 1 victory point and 1 defeat point</li>
     *     <li>Defeat or fleet in being: 1 defeat point</li>
     *     <li>Decisive defeat or refused engagement: 2 defeat points</li>
     * </ul>
     *
     * @param today    The current date for the evaluation.
     * @param contract The contract to evaluate scenarios for.
     *
     * @return The victory and defeat points over the last month.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static PerformanceTally getPerformanceTally(LocalDate today, AbstractContract contract) {
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        for (Scenario scenario : contract.getScenarios()) {
            LocalDate scenarioDate = scenario.getDate();
            // Scenario date can be null if the scenario hasn't been found in StratCon
            if (scenarioDate == null) {
                LOGGER.debug("{} has not been found yet. Skipping", scenario.getName());
                continue;
            }
            // Skip scenarios that occurred more than a month ago
            if (lastMonth.isAfter(scenarioDate)) {
                LOGGER.debug("{} occurred more than a month ago. Skipping", scenario.getName());
                continue;
            }

            ScenarioStatus scenarioStatus = scenario.getStatus();
            LOGGER.debug("Processing scenario {} ({}) date={} status={}", scenario.getName(), scenario.getId(),
                  scenarioDate, scenarioStatus);

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

        LOGGER.debug("Morale performance points: {} victory, {} defeat", victories, defeats);

        return new PerformanceTally(victories, defeats);
    }

    /**
     * Decides whether a scenario's outcome shifts the enemy's morale directly (see
     * {@link #processMoraleChangeFromScenario}). That is up to the scenario's type, with one exception: a riot moves
     * morale only on a Riot Duty contract, where quelling riots is the work. Riots fought elsewhere - such as a scheduled
     * parade disrupted on a Retainer contract - leave morale to the ordinary morale check.
     *
     * @param stratConScenarioType the scenario's type
     * @param contract             the contract the scenario belongs to, or {@code null} if it has none
     *
     * @return {@code true} if the scenario's outcome shifts the enemy's morale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isScenarioOutcomeAffectingMorale(ScenarioType stratConScenarioType,
          @Nullable AbstractContract contract) {
        if (stratConScenarioType.isRiot()) {
            boolean isRiotAffectingMorale = (contract != null)
                                                  && StratConContractMechanics.forContract(contract)
                                                           .isRiotAffectingMorale();
            if (!isRiotAffectingMorale) {
                return false;
            }
        }

        return stratConScenarioType.isScenarioOutcomeAffectsMorale();
    }

    /**
     * Applies a simplified morale check as the result of a scenario whose outcome moves the enemy's morale directly
     * (see {@link #isScenarioOutcomeAffectingMorale}).
     *
     * <p>This method does <em>not</em> use the full morale calculation. Instead, it forces a fixed roll based on the
     * overall outcome of the scenario:</p>
     * <ul>
     *     <li>Overall victory: roll is treated as {@link #WAVERING_TARGET_NUMBER} (OpFor morale declines).</li>
     *     <li>Overall defeat: roll is treated as {@link #RALLYING_TARGET_NUMBER} (OpFor morale improves).</li>
     *     <li>Otherwise: roll defaults to {@link #NO_CHANGE_TARGET_NUMBER}.</li>
     * </ul>
     *
     * @param contract             the contract whose morale is being updated
     * @param scenarioStatus       the outcome of the scenario used to determine the forced roll
     * @param stratConScenarioType the type of scenario.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processMoraleChangeFromScenario(AbstractContract contract, ScenarioStatus scenarioStatus,
          ScenarioType stratConScenarioType) {
        if (!isScenarioOutcomeAffectingMorale(stratConScenarioType, contract)) {
            return;
        }

        boolean moraleCheckOnVictory = stratConScenarioType.isScenarioOutcomeAffectsMoraleOnVictory();
        boolean moraleCheckOnDefeat = stratConScenarioType.isScenarioOutcomeAffectsMoraleOnDefeat();

        int forcedRoll = NO_CHANGE_TARGET_NUMBER;

        if (moraleCheckOnVictory && scenarioStatus.isOverallVictory()) {
            forcedRoll = WAVERING_TARGET_NUMBER;
        } else if (moraleCheckOnDefeat && scenarioStatus.isOverallDefeat()) {
            forcedRoll = RALLYING_TARGET_NUMBER;
        }

        getMoraleOutcome(contract, forcedRoll);
    }

    /**
     * Returns the scenario modifiers a beaten enemy brings into battle at its morale level: a
     * {@link ContractMoraleLevel#SCATTERED} enemy fights short of ammunition and with unrepaired damage, and a
     * {@link ContractMoraleLevel#CRITICAL} one with one of the two, picked at random. Any other enemy brings none.
     *
     * @param moraleLevel the enemy's morale level
     *
     * @return the keys of the scenario modifiers to add, in no particular order; empty if none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getMoraleScenarioModifierKeys(ContractMoraleLevel moraleLevel) {
        return switch (moraleLevel) {
            case SCATTERED -> List.of(LOW_AMMUNITION_MODIFIER, POOR_REPAIR_MODIFIER);
            case CRITICAL -> List.of(randomInt(2) == 0 ? LOW_AMMUNITION_MODIFIER : POOR_REPAIR_MODIFIER);
            default -> List.of();
        };
    }

    /**
     * Runs the monthly morale update for a contract.
     *
     * <p>On most contracts this is the morale check (see {@link #rollMorale}). A Garrison Duty or Retainer contract
     * follows a raid model instead (see
     * {@link mekhq.campaign.mission.contract.contractData.ContractObjectiveType#hasGarrisonRaids()}): the enemy is only
     * tested after a month in which it raided, so between raids its morale holds where it was, and each month there is
     * then a {@link #RAID_DIE_SIZE 1 in 4} chance of a raid (see {@link #applyRaidRoll}). Throughout an emergency
     * extension (see {@link #isInEmergencyExtension}) the enemy raids every month.</p>
     *
     * <p>After the check, an enemy left Dominating or Overwhelming costs the employer a setback (see
     * {@link #applyMonthlySetback}). It is shown with the check, and returned for the daily report, so the caller can
     * place it after its own report of the morale change.</p>
     *
     * @param campaign   the current campaign
     * @param contract   the contract whose morale is updated
     * @param showDialog whether to show the player the morale check and any raid that begins
     *
     * @return the report of the month's setback, or {@code null} if there was none
     */
    public static @Nullable String checkMorale(Campaign campaign, AbstractContract contract, boolean showDialog) {
        String setbackReport = null;
        // A garrison between raids saw no fighting last month, so there is nothing to test its enemy's morale against
        if (!contract.isPeaceful()) {
            String moraleReport = rollMorale(campaign, contract);

            // An enemy left Dominating or Overwhelming by the check costs the employer ground
            setbackReport = applyMonthlySetback(contract, campaign.getCampaignOptions().isUseStratConMaplessMode(),
                  randomInt(2) == 0);
            if (setbackReport != null) {
                moraleReport += "<br><br>" + setbackReport;
            }

            if (showDialog) {
                String flavorText = MHQMorale.getFormattedTitle()
                                          + "<h2 style='text-align:center;'>" + contract.getName() + "</h2>"
                                          + MoraleBar.getMoraleDisplay(contract).tooltip();
                new ImmersiveDialogNotification(campaign, flavorText, moraleReport,
                      MoraleBar.createDialogPanel(contract), true);
            }
        }

        if (contract.getObjectiveType().hasGarrisonRaids()) {
            // The surge that prompted an emergency extension keeps the enemy raiding until the contract ends
            boolean isRaid = isInEmergencyExtension(contract, campaign.getLocalDate())
                                   || (randomInt(RAID_DIE_SIZE) == 0);
            applyRaidRoll(campaign, contract, isRaid, showDialog);
        }

        return setbackReport;
    }

    /**
     * Whether a contract has run past its agreed term into an emergency extension. The extension records its first day
     * when it is granted (see {@link ContractEmergencyExtension#contractExtended}); the schedule cannot tell, since
     * moving the end date also recomputes the contract's length.
     *
     * @param contract the contract
     * @param today    the current date
     *
     * @return {@code true} if the contract is in an emergency extension today
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isInEmergencyExtension(AbstractContract contract, @Nullable LocalDate today) {
        LocalDate extensionStartDate = contract.getEmergencyExtensionStartDate();
        return (today != null) && (extensionStartDate != null) && !today.isBefore(extensionStartDate);
    }

    /**
     * Performs the morale check (see {@link #performMoraleCheck}) with the campaign's morale options, followed by
     * Escalation's roll for morale (see {@link StratConEscalation#rollForMorale}).
     *
     * @param campaign the current campaign
     * @param contract the contract whose morale is checked
     *
     * @return the report of the check
     */
    private static String rollMorale(Campaign campaign, AbstractContract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        String moraleReport = MHQMorale.performMoraleCheck(campaign.getLocalDate(), contract,
              campaignOptions.get(CampaignOption.MORALE_DECISIVE_VICTORY_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_VICTORY_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_DECISIVE_DEFEAT_EFFECT),
              campaignOptions.get(CampaignOption.MORALE_DEFEAT_EFFECT));

        // Escalation may move morale a further level
        String escalationReport = StratConEscalation.rollForMorale(campaign, contract);
        if (escalationReport != null) {
            moraleReport += "<br><br>" + escalationReport;
        }

        return moraleReport;
    }

    /**
     * Applies the monthly setback an employer suffers while the enemy has the upper hand, judged on the enemy's morale
     * once the month's morale check is done:
     *
     * <ul>
     *     <li>{@link ContractMoraleLevel#OVERWHELMING}: the contract loses Contract Victory Points and employer
     *     support.</li>
     *     <li>{@link ContractMoraleLevel#DOMINATING}: the contract loses one or the other, picked at random.</li>
     * </ul>
     *
     * <p>The Contract Victory Points lost are 10% of the contract's target, rounded up, and always at least 1. The
     * support lost is one point per combat team of the contract's scale, but never more than the contract has left. In
     * mapless mode, where Contract Victory Points are worked out from the contract's scenarios rather than kept, only
     * support is lost. A contract run without StratCon has neither, so it suffers no setback.</p>
     *
     * @param contract                   the contract whose enemy's morale was just checked
     * @param isMaplessMode              whether StratCon runs in mapless mode
     * @param isVictoryPointsPickedFirst on a Dominating enemy, whether the setback falls on Contract Victory Points
     *                                   rather than support (ignored in mapless mode)
     *
     * @return the report of the setback, or {@code null} if the contract lost nothing
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String applyMonthlySetback(AbstractContract contract, boolean isMaplessMode,
          boolean isVictoryPointsPickedFirst) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null) {
            return null;
        }

        ContractMoraleLevel moraleLevel = contract.getMoraleLevel();
        boolean isLosingVictoryPoints;
        boolean isLosingSupport;
        if (moraleLevel.isOverwhelming()) {
            isLosingVictoryPoints = !isMaplessMode;
            isLosingSupport = true;
        } else if (moraleLevel.isDominating()) {
            isLosingVictoryPoints = !isMaplessMode && isVictoryPointsPickedFirst;
            isLosingSupport = !isLosingVictoryPoints;
        } else {
            return null;
        }

        int victoryPointsLost = isLosingVictoryPoints ? getSetbackVictoryPoints(contract) : 0;
        int supportLost = isLosingSupport
                                ? Math.clamp(contract.getScale(), 0, max(0, campaignState.getSupportPoints()))
                                : 0;

        campaignState.changeVictoryPoints(-victoryPointsLost);
        campaignState.changeSupportPoints(-supportLost);

        if ((victoryPointsLost == 0) && (supportLost == 0)) {
            return null;
        }

        String key;
        if ((victoryPointsLost > 0) && (supportLost > 0)) {
            key = "MHQMorale.setback.both";
        } else if (victoryPointsLost > 0) {
            key = "MHQMorale.setback.victoryPoints";
        } else {
            key = "MHQMorale.setback.support";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, key, spanOpeningWithCustomColor(getNegativeColor()),
              CLOSING_SPAN_TAG, moraleLevel, victoryPointsLost, supportLost);
    }

    /**
     * Whether the next monthly morale check would bring a setback (see {@link #applyMonthlySetback}) if the enemy's
     * morale stays where it is: the enemy is Dominating or Overwhelming on a contract run with StratCon.
     *
     * @param contract the contract
     *
     * @return {@code true} if a setback is due
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isSetbackDue(AbstractContract contract) {
        ContractMoraleLevel moraleLevel = contract.getMoraleLevel();
        return (contract.getStratConCampaignState() != null)
                     && (moraleLevel.isDominating() || moraleLevel.isOverwhelming());
    }

    /**
     * @param contract the contract
     *
     * @return the Contract Victory Points a monthly setback costs: 10% of the contract's target, rounded up, and always
     *       at least 1
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getSetbackVictoryPoints(AbstractContract contract) {
        return max(1, (int) Math.ceil(contract.getRequiredVictoryPoints() * SETBACK_VICTORY_POINT_SHARE));
    }

    /**
     * Applies a Garrison Duty or Retainer contract's monthly raid roll. A raid ends the peace: the enemy, at whatever
     * morale it held after its last raid, is active in the Area of Operations for the month. Otherwise the contract is
     * peaceful for the month, with no enemy activity at all.
     *
     * <p>Only the start of a raid is announced; a raid carrying on into another month needs no warning. As these
     * contracts get no Batchall when they begin, the enemy's first raid brings one instead (see
     * {@link ContractBatchall#offerBatchall}).</p>
     *
     * @param campaign   the current campaign
     * @param contract   the Garrison Duty or Retainer contract
     * @param isRaid     whether the enemy raids this month
     * @param showDialog whether to show the player a raid that begins, and the Batchall the first one brings
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void applyRaidRoll(Campaign campaign, AbstractContract contract, boolean isRaid, boolean showDialog) {
        boolean wasPeaceful = contract.isPeaceful();
        boolean isFirstRaid = isRaid && !contract.hasBeenRaided();
        contract.setPeaceful(!isRaid);
        if (isRaid) {
            contract.setHasBeenRaided(true);
        }

        if (!showDialog) {
            return;
        }

        if (isRaid && wasPeaceful) {
            new ImmersiveDialogSimple(campaign,
                  contract.getEmployerLiaison(),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "garrisonRaid.started", campaign.getCommanderAddress(),
                        contract.getEnemyDisplayName()),
                  null,
                  null,
                  null,
                  false);
        }

        if (isFirstRaid) {
            ContractBatchall.offerBatchall(campaign, contract);
        }
    }

    /**
     * Sets a newly generated contract's starting morale with a morale check. The enemy never starts
     * {@link ContractMoraleLevel#SCATTERED}, and on a Relief Duty contract it starts a level higher. A Garrison Duty or
     * Retainer contract also starts peaceful (see
     * {@link mekhq.campaign.mission.contract.contractData.ContractObjectiveType#hasGarrisonRaids()}), so its first raid
     * roll comes at the start of the next month.
     *
     * @param contract the newly generated contract
     * @param campaign the current campaign
     */
    public static void determineStartingMorale(AbstractContract contract, Campaign campaign) {
        rollMorale(campaign, contract);

        if (contract.getMoraleLevel().isScattered()) {
            contract.changeMorale(ContractMoraleLevel.CRITICAL);
        }

        if (contract.getObjectiveType().isReliefDuty()) {
            contract.changeMorale(1);
        }

        contract.setPeaceful(contract.getObjectiveType().hasGarrisonRaids());
    }
}
