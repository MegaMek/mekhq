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
package mekhq.campaign.roleplay;

import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;

/**
 * Works out the exact chance of a check succeeding, or of one side winning an opposed check, by counting every way the
 * dice can fall.
 */
public final class CheckOdds {
    private static final int SUMS = 13;

    // How many ways each total from 2 to 12 can be rolled: 2d6 out of 36, and 3d6 keeping the highest or lowest two
    // out of 216. CheckOddsTest checks these against every possible roll.
    private static final int[] NORMAL_WAYS = { 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1 };
    private static final int[] ADVANTAGE_WAYS = { 1, 3, 7, 12, 19, 27, 34, 36, 34, 27, 16 };
    private static final int[] DISADVANTAGE_WAYS = { 16, 27, 34, 36, 34, 27, 19, 12, 7, 3, 1 };

    private CheckOdds() {}

    /**
     * @param rollType how the dice are rolled
     *
     * @return the chance of each total from 2 to 12, indexed by the total
     */
    static double[] totals(final RollType rollType) {
        final int[] ways = switch (rollType) {
            case NORMAL -> NORMAL_WAYS;
            case ADVANTAGE -> ADVANTAGE_WAYS;
            case DISADVANTAGE -> DISADVANTAGE_WAYS;
        };
        final double outcomes = (rollType == RollType.NORMAL) ? 36 : 216;
        final double[] chances = new double[SUMS];
        for (int total = 2; total < SUMS; total++) {
            chances[total] = ways[total - 2] / outcomes;
        }
        return chances;
    }

    /**
     * @param target  the target to beat
     * @param useEdge {@code true} if a failed roll will be re-rolled once with Edge
     *
     * @return the chance of the check succeeding, from 0 to 1
     */
    public static double chance(final CheckTarget target, final boolean useEdge) {
        double[] totals = totals(target.rollType());
        double chance = 0;
        for (int total = 2; total < SUMS; total++) {
            if (target.succeeds(total)) {
                chance += totals[total];
            }
        }
        return (useEdge && target.canSucceed()) ? chance + (1 - chance) * chance : chance;
    }

    /**
     * The chance that the acting side wins an opposed check. The higher margin of success wins and the defender wins a
     * tie. Whoever is losing may re-roll once with Edge, if they chose to and their check is not impossible; only one
     * re-roll happens per check.
     *
     * @param acting        the acting side's target
     * @param actingEdge    {@code true} if the acting side will re-roll a loss with Edge
     * @param defending     the defending side's target
     * @param defendingEdge {@code true} if the defending side will re-roll a loss with Edge
     *
     * @return the chance of the acting side winning, from 0 to 1
     */
    public static double opposedWinChance(final CheckTarget acting, final boolean actingEdge,
          final CheckTarget defending, final boolean defendingEdge) {
        final boolean actingRerolls = actingEdge && !acting.impossible();
        final boolean defendingRerolls = defendingEdge && !defending.impossible();
        // Rather than pairing every roll with every other, sum over one side's roll at a time: given that roll, the
        // chance of the other side's first roll (and any re-roll, which is independent) follows from one lookup.
        double wins = 0;
        double[] actingTotals = totals(acting.rollType());
        for (int actingRoll = 2; actingRoll < SUMS; actingRoll++) {
            if (actingTotals[actingRoll] == 0) {
                continue;
            }
            // The acting side is ahead when the defender's margin falls short; a losing defender may re-roll, and
            // must fall short again.
            double defenderShort = 1 - chanceToReach(defending, acting.margin(actingRoll), true);
            wins += actingTotals[actingRoll] * defenderShort * (defendingRerolls ? defenderShort : 1);
        }
        if (actingRerolls) {
            double[] defendingTotals = totals(defending.rollType());
            for (int defendingRoll = 2; defendingRoll < SUMS; defendingRoll++) {
                // The acting side is behind when its margin is no better; its re-roll must then beat the defender's.
                double actingBeats = chanceToReach(acting, defending.margin(defendingRoll), false);
                wins += defendingTotals[defendingRoll] * (1 - actingBeats) * actingBeats;
            }
        }
        return wins;
    }

    /**
     * @param target  the target rolled against
     * @param margin  the other side's margin
     * @param orEqual {@code true} to count a tie, as the defender wins ties
     *
     * @return the chance that one roll against {@code target} beats (or, with {@code orEqual}, ties) {@code margin}
     */
    private static double chanceToReach(final CheckTarget target, final int margin, final boolean orEqual) {
        double[] totals = totals(target.rollType());
        double chance = 0;
        for (int total = 2; total < SUMS; total++) {
            int rolled = target.margin(total);
            if (rolled > margin || (orEqual && rolled == margin)) {
                chance += totals[total];
            }
        }
        return chance;
    }
}
