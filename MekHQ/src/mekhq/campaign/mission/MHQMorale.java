/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.rating.IUnitRating.DRAGOON_A;
import static mekhq.campaign.rating.IUnitRating.DRAGOON_ASTAR;
import static mekhq.campaign.rating.IUnitRating.DRAGOON_F;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.universe.Faction;

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
    private static final String RESOURCE_BUNDLE = "mekhq.resources.MHQMorale";

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
    public static String performMoraleCheck(final LocalDate today, final AtBContract contract,
          final int decisiveVictoryModifier, final int victoryModifier, final int decisiveDefeatModifier,
          final int defeatModifier) {
        final TargetRoll targetNumber = new TargetRoll();

        // Add modifiers to the target number
        int reliability = getReliability(contract);
        targetNumber.addModifier(reliability, "Enemy Reliability");

        int performanceModifier = getPerformanceModifier(
              today, contract, decisiveVictoryModifier, victoryModifier,
              decisiveDefeatModifier, defeatModifier);
        targetNumber.addModifier(performanceModifier, "Performance Modifier");

        // The actual check
        int roll = d6(2) + targetNumber.getValue();
        MoraleOutcome moraleOutcome = getMoraleOutcome(contract, roll);

        // Generate and return the report
        PerformanceOutcome performanceOutcome = getOutcome(decisiveVictoryModifier, victoryModifier,
              decisiveDefeatModifier, defeatModifier, performanceModifier);
        return getReport(performanceOutcome, moraleOutcome, roll);
    }

    /**
     * Creates a formatted morale report string based on the performance and morale outcomes.
     *
     * @param performanceOutcome The result of performance evaluation.
     * @param moraleOutcome      The result of the morale check.
     * @param roll               The final morale check roll value.
     *
     * @return The formatted morale check report.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getReport(PerformanceOutcome performanceOutcome, MoraleOutcome moraleOutcome, int roll) {
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
        String moraleText = getTextAt(RESOURCE_BUNDLE, "MHQMorale.moraleOutcome." + moraleOutcome.name());

        return getFormattedTextAt(
              RESOURCE_BUNDLE,
              "MHQMorale.check.report",
              spanOpeningWithCustomColor(performanceColor),
              performanceText,
              CLOSING_SPAN_TAG,
              roll,
              spanOpeningWithCustomColor(moraleColor),
              moraleText
        );
    }


    /**
     * Determines the morale outcome based on the contract's current morale level and roll value.
     *
     * <p>Updates the morale level in the contract if it changes.</p>
     *
     * @param contract The contract to check and update morale for.
     * @param roll     The result of the morale check roll.
     *
     * @return The resulting MoraleOutcome after the check.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static MoraleOutcome getMoraleOutcome(AtBContract contract, int roll) {
        AtBMoraleLevel currentMoraleLevel = contract.getMoraleLevel();
        AtBMoraleLevel updatedMoraleLevel = currentMoraleLevel;
        MoraleOutcome moraleOutcome;

        if (roll <= 5) {
            moraleOutcome = MoraleOutcome.RALLYING;
            updatedMoraleLevel = switch (currentMoraleLevel) {
                case ROUTED -> AtBMoraleLevel.CRITICAL;
                case CRITICAL -> AtBMoraleLevel.WEAKENED;
                case WEAKENED -> AtBMoraleLevel.STALEMATE;
                case STALEMATE -> AtBMoraleLevel.ADVANCING;
                case ADVANCING -> AtBMoraleLevel.DOMINATING;
                case DOMINATING, OVERWHELMING -> AtBMoraleLevel.OVERWHELMING;
            };
        } else if (roll >= 9) {
            moraleOutcome = MoraleOutcome.WAVERING;
            updatedMoraleLevel = switch (currentMoraleLevel) {
                case ROUTED, CRITICAL -> AtBMoraleLevel.ROUTED;
                case WEAKENED -> AtBMoraleLevel.CRITICAL;
                case STALEMATE -> AtBMoraleLevel.WEAKENED;
                case ADVANCING -> AtBMoraleLevel.STALEMATE;
                case DOMINATING -> AtBMoraleLevel.ADVANCING;
                case OVERWHELMING -> AtBMoraleLevel.DOMINATING;
            };
        } else {
            moraleOutcome = MoraleOutcome.UNCHANGED;
            // Level remains unchanged
        }

        // Only update if the morale level actually changed
        if (updatedMoraleLevel != currentMoraleLevel) {
            contract.setMoraleLevel(updatedMoraleLevel);
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
    private static PerformanceOutcome getOutcome(int decisiveVictoryModifier, int victoryModifier,
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
     * Calculates the reliability modifier for the enemy based on contract parameters and faction characteristics.
     *
     * @param contract The contract to evaluate.
     *
     * @return The reliability modifier to use for this contract.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getReliability(AtBContract contract) {
        int reliabilityModifier;

        int quality = contract.getEnemyQuality();
        Faction enemy = contract.getEnemy();

        // Clan enemies get a reliability boost: set at A* or +1
        if (enemy.isClan()) {
            quality = Math.min(DRAGOON_ASTAR, quality + 1);
        }

        // Adjust for Dragoon ratings
        reliabilityModifier = switch (quality) {
            case DRAGOON_F -> -1;
            case DRAGOON_A, DRAGOON_ASTAR -> +1;
            default -> 0; // DRAGOON_D, DRAGOON_C, DRAGOON_B
        };

        // Adjust for special enemy traits
        if (enemy.isRebel() || enemy.isMinorPower() || enemy.isMercenary() || enemy.isPirate()) {
            reliabilityModifier--;
        } else if (enemy.isClan()) { // Clan forces get to double-dip
            reliabilityModifier++;
        }

        return reliabilityModifier;
    }

    /**
     * Calculates the performance modifier for recent scenario outcomes within the contract.
     *
     * <p>Considers victories, defeats, and special outcome scenarios within the last month.</p>
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
    private static int getPerformanceModifier(LocalDate today, AtBContract contract, int decisiveVictoryModifier,
          int victoryModifier, int decisiveDefeatModifier, int defeatModifier) {
        int victories = 0;
        int defeats = 0;
        LocalDate lastMonth = today.minusMonths(1);

        for (Scenario scenario : contract.getScenarios()) {
            LocalDate scenarioDate = scenario.getDate();
            if (scenarioDate != null && lastMonth.isAfter(scenarioDate)) {
                continue;
            }

            ScenarioStatus scenarioStatus = scenario.getStatus();

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
            // Pyrrhic Victory reduces victory count (negative consequence)
            if (scenarioStatus.isPyrrhicVictory()) {
                victories--;
                continue;
            }
            // Overall Victory/Defeat, if not otherwise noted above
            if (scenarioStatus.isOverallVictory()) {
                victories++;
            } else if (scenarioStatus.isOverallDefeat()) {
                defeats++;
            }
        }

        // Compute performance modifier using concise math logic
        if (victories > defeats) {
            return (victories >= defeats * 2) ? decisiveVictoryModifier : victoryModifier;
        } else if (defeats > victories) {
            return (defeats >= victories * 2) ? decisiveDefeatModifier : defeatModifier;
        }
        return 0;
    }
}
