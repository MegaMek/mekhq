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

    private CheckOdds() {}

    /**
     * @param rollType how the dice are rolled
     *
     * @return the chance of each total from 2 to 12, indexed by the total
     */
    static double[] totals(final RollType rollType) {
        double[] chances = new double[SUMS];
        if (rollType == RollType.NORMAL) {
            for (int first = 1; first <= 6; first++) {
                for (int second = 1; second <= 6; second++) {
                    chances[first + second] += 1.0 / 36;
                }
            }
            return chances;
        }
        for (int first = 1; first <= 6; first++) {
            for (int second = 1; second <= 6; second++) {
                for (int third = 1; third <= 6; third++) {
                    int dropped = (rollType == RollType.ADVANTAGE) ? Math.min(first, Math.min(second, third))
                                        : Math.max(first, Math.max(second, third));
                    chances[first + second + third - dropped] += 1.0 / 216;
                }
            }
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
        double[] actingTotals = totals(acting.rollType());
        double[] defendingTotals = totals(defending.rollType());
        double wins = 0;
        for (int actingRoll = 2; actingRoll < SUMS; actingRoll++) {
            for (int defendingRoll = 2; defendingRoll < SUMS; defendingRoll++) {
                double chance = actingTotals[actingRoll] * defendingTotals[defendingRoll];
                if (chance == 0) {
                    continue;
                }
                int actingMargin = acting.margin(actingRoll);
                int defendingMargin = defending.margin(defendingRoll);
                if (actingMargin > defendingMargin) {
                    // The defender is losing, so may re-roll against the acting side's margin.
                    wins += chance * (defendingRerolls ? 1 - chanceToReach(defending, actingMargin, true) : 1);
                } else if (actingRerolls) {
                    wins += chance * chanceToReach(acting, defendingMargin, false);
                }
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
