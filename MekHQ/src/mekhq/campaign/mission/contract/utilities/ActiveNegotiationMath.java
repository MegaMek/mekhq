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
package mekhq.campaign.mission.contract.utilities;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

/**
 * Pure scoring behind active negotiation: turning the two negotiators' opposed {@link MarginOfSuccess} results into a
 * single net margin. Kept free of dice and GUI so it can be tested in isolation.
 *
 * <p>Each margin is scored on a scale centered on {@link MarginOfSuccess#BARELY_MADE_IT} (zero), with better results
 * positive and worse results negative. The net margin is the player's score minus the employer's: a positive net means
 * the player's negotiator came out ahead by that many margin-of-success levels (improving that many terms), a negative
 * net means the employer did (lowering that many).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ActiveNegotiationMath {
    /** The margin a check can reach either way, matching the clamp skill checks apply. */
    private static final int MARGIN_LIMIT = 10;
    private static final int DIE_FACES = 6;

    private ActiveNegotiationMath() {
    }

    /**
     * How likely each outcome of an opposed negotiation is.
     *
     * @param win       the chance the player comes out ahead (a positive net margin)
     * @param stalemate the chance of a net margin of zero
     * @param loss      the chance the employer comes out ahead
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Odds(double win, double stalemate, double loss) {
    }

    /**
     * The chance of each {@link MarginOfSuccess} on one Negotiation check, as a skill check resolves it: two dice (or
     * three keeping the highest two, with natural aptitude) against the target number, and, when Edge is spent, one
     * reroll of a failed roll that could have succeeded.
     *
     * @param targetNumber      the check's target number
     * @param cannotSucceed     whether the target can never be met (an automatic failure or impossible check)
     * @param countUp           whether the skill succeeds at or under its target rather than at or over it
     * @param naturalAptitude   whether the check rolls three dice and keeps the highest two
     * @param rerollWithEdge    whether a failed roll is rerolled with Edge
     *
     * @return the chance of each margin, indexed by {@link MarginOfSuccess#ordinal()}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double[] marginChances(int targetNumber, boolean cannotSucceed, boolean countUp,
          boolean naturalAptitude, boolean rerollWithEdge) {
        double[] rolls = rollChances(naturalAptitude);
        boolean canSucceed = !cannotSucceed && (countUp ? targetNumber >= 2 : targetNumber <= 12);
        double[] chances = new double[MarginOfSuccess.values().length];
        for (int roll = 2; roll <= 12; roll++) {
            boolean failed = countUp ? roll > targetNumber : roll < targetNumber;
            if (failed && canSucceed && rerollWithEdge) {
                for (int reroll = 2; reroll <= 12; reroll++) {
                    chances[margin(targetNumber, reroll, countUp).ordinal()] += rolls[roll] * rolls[reroll];
                }
            } else {
                chances[margin(targetNumber, roll, countUp).ordinal()] += rolls[roll];
            }
        }
        return chances;
    }

    /**
     * The odds of an opposed negotiation: every pairing of the two checks' margins, scored as
     * {@link #netMargin(MarginOfSuccess, MarginOfSuccess)} plus a flat modifier from the player's traits.
     *
     * @param playerChances   the player's margin chances, from {@link #marginChances}
     * @param employerChances the employer's margin chances, or {@code null} when the employer has no negotiator and
     *                        always barely makes it
     * @param modifier        the flat modifier to the net margin
     *
     * @return the chance of a win, a stalemate and a loss
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Odds opposedOdds(double[] playerChances, @Nullable double[] employerChances, int modifier) {
        MarginOfSuccess[] margins = MarginOfSuccess.values();
        double win = 0;
        double stalemate = 0;
        double loss = 0;
        for (MarginOfSuccess player : margins) {
            for (MarginOfSuccess employer : margins) {
                double employerChance = employerChances == null
                                              ? (employer == MarginOfSuccess.BARELY_MADE_IT ? 1 : 0)
                                              : employerChances[employer.ordinal()];
                double chance = playerChances[player.ordinal()] * employerChance;
                int net = netMargin(player, employer) + modifier;
                if (net > 0) {
                    win += chance;
                } else if (net == 0) {
                    stalemate += chance;
                } else {
                    loss += chance;
                }
            }
        }
        return new Odds(win, stalemate, loss);
    }

    /** The chance of each total from 2 to 12, indexed by the total. */
    private static double[] rollChances(boolean keepHighestTwoOfThree) {
        double[] chances = new double[13];
        if (keepHighestTwoOfThree) {
            double each = 1.0 / (DIE_FACES * DIE_FACES * DIE_FACES);
            for (int first = 1; first <= DIE_FACES; first++) {
                for (int second = 1; second <= DIE_FACES; second++) {
                    for (int third = 1; third <= DIE_FACES; third++) {
                        int lowest = Math.min(first, Math.min(second, third));
                        chances[first + second + third - lowest] += each;
                    }
                }
            }
        } else {
            double each = 1.0 / (DIE_FACES * DIE_FACES);
            for (int first = 1; first <= DIE_FACES; first++) {
                for (int second = 1; second <= DIE_FACES; second++) {
                    chances[first + second] += each;
                }
            }
        }
        return chances;
    }

    private static MarginOfSuccess margin(int targetNumber, int roll, boolean countUp) {
        long difference = (long) targetNumber - roll;
        int margin = (int) Math.clamp(countUp ? difference : -difference, -MARGIN_LIMIT, MARGIN_LIMIT);
        return MarginOfSuccess.getMarginOfSuccessObject(margin);
    }

    /**
     * The signed level score for a margin: {@code 0} at {@link MarginOfSuccess#BARELY_MADE_IT}, positive for better
     * results, negative for worse. Derived from enum position so it stays correct if labels change.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int marginScore(MarginOfSuccess margin) {
        // The enum is ordered best-first (SPECTACULAR at ordinal 0), so a lower ordinal is a better result.
        return MarginOfSuccess.BARELY_MADE_IT.ordinal() - margin.ordinal();
    }

    /**
     * The net margin of an opposed negotiation: the player's level score minus the employer's. Positive favors the
     * player (that many terms improve), negative favors the employer (that many lower), zero is a stalemate.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int netMargin(MarginOfSuccess playerMargin, MarginOfSuccess employerMargin) {
        return marginScore(playerMargin) - marginScore(employerMargin);
    }
}
