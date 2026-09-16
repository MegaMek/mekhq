/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.BASE_LEADERSHIP_BUDGET;

import mekhq.campaign.mission.utilities.CombatRole;

/**
 * Pure deployment arithmetic for the StratCon deployment wizard: the buried formulas that decide whether a deployment
 * succeeds and what it costs, pulled out of the wizard UI so the inspector, the board lenses, and the commit path all
 * read one source of truth.
 *
 * <p>Every method here is a total function of its arguments - no campaign, scenario, or UI state - so each is directly
 * unit-testable. Campaign-dependent lookups (eligibility lists, the contract-modified target number) live in the wizard
 * layer that feeds these formulas their inputs.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class DeploymentEvaluator {
    /** Leadership battle-value budget is capped at this many levels of the commander's leadership skill. */
    public static final int LEADERSHIP_SKILL_CAP = 5;

    /** Each support point the player chooses to spend (beyond the base cost) shifts the reinforcement roll by this. */
    public static final int SUPPORT_POINTS_MODIFIER = -2;

    /** Base support-point cost added per force for a normally-arriving reinforcement. */
    public static final int NORMAL_ARRIVAL_BASE_COST = 1;

    /** Base support-point cost added per force for an instantly-arriving reinforcement (double the normal base). */
    public static final int INSTANT_ARRIVAL_BASE_COST = 2;

    /** The 36 equally-likely outcomes of two six-sided dice, indexed by sum (2..12). */
    private static final int[] TWO_D6_WAYS_BY_SUM = { 0, 0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1 };
    private static final int TWO_D6_TOTAL_OUTCOMES = 36;

    private DeploymentEvaluator() {}

    /**
     * @param targetNumber the number the reinforcement roll must meet or beat on two six-sided dice
     *
     * @return the probability, in {@code [0.0, 1.0]}, that a 2d6 roll is greater than or equal to {@code targetNumber}.
     *       A natural 2 always fails, so the result never exceeds {@code 35/36} even for a target of {@code 2} or less.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double successProbabilityTwoD6(int targetNumber) {
        if (targetNumber > 12) {
            return 0.0;
        }

        // A natural 2 is an automatic failure regardless of target number, so the count starts at 3: for any target
        // above 2 that roll already fails, and for a target of 2 or less it is the one outcome that does not succeed.
        int lowestSuccessfulSum = Math.max(targetNumber, 3);
        int favourableOutcomes = 0;
        for (int sum = lowestSuccessfulSum; sum <= 12; sum++) {
            favourableOutcomes += TWO_D6_WAYS_BY_SUM[sum];
        }

        return (double) favourableOutcomes / TWO_D6_TOTAL_OUTCOMES;
    }


    /**
     * @param leadershipSkill the commander's leadership skill; a non-positive skill grants no budget
     *
     * @return the total leadership battle-value budget for a commander of the given skill, capped at
     *       {@link #LEADERSHIP_SKILL_CAP} levels
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int leadershipBudget(int leadershipSkill) {
        int cappedSkill = max(0, min(leadershipSkill, LEADERSHIP_SKILL_CAP));
        return BASE_LEADERSHIP_BUDGET * cappedSkill;
    }

    /**
     * @param leadershipSkill      the commander's leadership skill
     * @param leadershipPointsUsed leadership battle value already committed for the scenario
     *
     * @return the leadership battle value still available to spend, never negative
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int leadershipPointsRemaining(int leadershipSkill, int leadershipPointsUsed) {
        return max(0, leadershipBudget(leadershipSkill) - leadershipPointsUsed);
    }

    /**
     * The utility-page tradeoff: every supporting unit deployed spends a defensive point that would otherwise have been
     * a minefield.
     *
     * @param defensivePoints     the scenario's total defensive points
     * @param utilityUnitsStaged  the number of supporting units staged for deployment
     *
     * @return the minefields remaining after the staged units take their share, never negative
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int minefieldsRemaining(int defensivePoints, int utilityUnitsStaged) {
        return max(0, defensivePoints - utilityUnitsStaged);
    }

    /**
     * @param chosenSupportPoints the support points the player chose to spend per force to improve the roll
     * @param instant             whether the reinforcements arrive instantly (doubles the base cost per force)
     * @param forceCount          the number of forces being committed, each a separate reinforcement attempt
     *
     * @return the {@link ReinforcementCost} for the attempt; a {@code forceCount} of zero costs nothing and applies no
     *       modifier
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ReinforcementCost reinforcementCost(int chosenSupportPoints, boolean instant, int forceCount) {
        int baseCost = instant ? INSTANT_ARRIVAL_BASE_COST : NORMAL_ARRIVAL_BASE_COST;
        int perForce = chosenSupportPoints + baseCost;

        if (forceCount <= 0) {
            return new ReinforcementCost(perForce, 0, 0);
        }

        int total = perForce * forceCount;
        int targetNumberModifier = (chosenSupportPoints * SUPPORT_POINTS_MODIFIER) / forceCount;
        return new ReinforcementCost(perForce, total, targetNumberModifier);
    }

    /**
     * Composes a reinforcement roll from a contract-modified base target number and the modifier bought by spending
     * support points.
     *
     * @param baseTargetNumber     the target number after command-liaison and contract modifiers, before support points
     * @param targetNumberModifier the (non-positive) support-point modifier, from {@link #reinforcementCost}
     * @param isManeuver if the formation is set to {@link CombatRole#MANEUVER}} and therefore enjoys improved reinforcement rolls
     *
     * @return the final target number and its 2d6 success probability
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ReinforcementRoll reinforcementRoll(int baseTargetNumber, int targetNumberModifier, boolean isManeuver) {
        int finalTargetNumber = baseTargetNumber + targetNumberModifier;
        double probability = successProbabilityTwoD6(finalTargetNumber);

        if (isManeuver) {
            probability = 1.0 - Math.pow(1.0 - probability, 2);
        }

        return new ReinforcementRoll(finalTargetNumber, probability);
    }
}
