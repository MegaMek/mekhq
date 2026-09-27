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

/**
 * Implements the Fate Chart used to answer yes/no questions in the solo-roleplay oracle system.
 *
 * <p>The player picks the {@link FateChartOdds} of the answer being yes and supplies the current chaos factor
 * ({@value #MINIMUM_CHAOS_FACTOR}-{@value #MAXIMUM_CHAOS_FACTOR}). Each cell of the chart holds three thresholds; a
 * d100 roll is compared against them:</p>
 * <ul>
 *     <li>roll &lt;= exceptional-yes threshold: {@link FateChartAnswer#EXCEPTIONAL_YES}</li>
 *     <li>roll &lt;= yes threshold: {@link FateChartAnswer#NORMAL_YES}</li>
 *     <li>roll &gt;= exceptional-no threshold: {@link FateChartAnswer#EXCEPTIONAL_NO}</li>
 *     <li>otherwise: {@link FateChartAnswer#NORMAL_NO}</li>
 * </ul>
 *
 * <p>Every cell of the chart is one of thirteen threshold triples. Moving one step up in odds, or one step up in chaos,
 * moves one step along that sequence, so a cell's triple is found by adding the odds and chaos indices together.</p>
 *
 * <p>A roll that is a multiple of 11, or a roll of 100, also triggers a random event: a follow-up d100 is rolled on
 * the {@link RandomEventFocus} table.</p>
 */
public final class FateChart {
    public static final int MINIMUM_CHAOS_FACTOR = 1;
    public static final int MAXIMUM_CHAOS_FACTOR = 9;

    /** Stands in for an 'x' on the chart where no exceptional yes is possible. */
    static final int NO_EXCEPTIONAL_YES = 0;

    /** Stands in for an 'x' on the chart where no exceptional no is possible. */
    static final int NO_EXCEPTIONAL_NO = 101;

    /** The chart's cell values as {@code {exceptional yes, yes, exceptional no}}, from least to most likely. */
    private static final int[][] THRESHOLDS = {
          { NO_EXCEPTIONAL_YES, 1, 81 },
          { 1, 5, 82 },
          { 2, 10, 83 },
          { 3, 15, 84 },
          { 5, 25, 86 },
          { 7, 35, 88 },
          { 10, 50, 91 },
          { 13, 65, 94 },
          { 15, 75, 96 },
          { 17, 85, 98 },
          { 18, 90, 99 },
          { 19, 95, 100 },
          { 20, 99, NO_EXCEPTIONAL_NO }
    };

    /**
     * The offset that aligns {@code odds + chaos} with {@link #THRESHOLDS}: 50/50 odds at chaos factor 5 is the 50%
     * cell.
     */
    private static final int INDEX_OFFSET = 3;

    private FateChart() {}

    /**
     * Consults the Fate Chart using given rolls.
     *
     * @param odds            the player's chosen odds of the answer being yes
     * @param chaosFactor     the current chaos factor; values outside the valid range are clamped
     * @param roll            the d100 roll (1-100)
     * @param randomEventRoll the follow-up d100 roll (1-100) used only if {@code roll} triggers a random event
     *
     * @return the result, including any random event
     */
    public static FateChartResult consult(final FateChartOdds odds, final int chaosFactor, final int roll,
          final int randomEventRoll) {
        final FateChartAnswer answer = resolve(odds, chaosFactor, roll);
        if (!isRandomEvent(roll)) {
            return new FateChartResult(answer, roll, null, 0);
        }
        return new FateChartResult(answer, roll, RandomEventFocus.fromRoll(randomEventRoll), randomEventRoll);
    }

    /**
     * @param roll a d100 roll
     *
     * @return {@code true} if the roll is a multiple of 11 or is 100, triggering a random event
     */
    public static boolean isRandomEvent(final int roll) {
        return roll == 100 || (roll > 0 && roll % 11 == 0);
    }

    /**
     * Resolves a Fate Chart question against a given d100 roll.
     *
     * @param odds        the player's chosen odds of the answer being yes
     * @param chaosFactor the current chaos factor; values outside the valid range are clamped
     * @param roll        the d100 roll (1-100)
     *
     * @return the answer
     */
    public static FateChartAnswer resolve(final FateChartOdds odds, final int chaosFactor, final int roll) {
        final int[] thresholds = getThresholds(odds, chaosFactor);

        if (roll <= thresholds[0]) {
            return FateChartAnswer.EXCEPTIONAL_YES;
        } else if (roll <= thresholds[1]) {
            return FateChartAnswer.NORMAL_YES;
        } else if (roll >= thresholds[2]) {
            return FateChartAnswer.EXCEPTIONAL_NO;
        }
        return FateChartAnswer.NORMAL_NO;
    }

    /**
     * Returns the chart cell for the given odds and chaos factor.
     *
     * @param odds        the player's chosen odds of the answer being yes
     * @param chaosFactor the current chaos factor; values outside the valid range are clamped
     *
     * @return a copy of the cell as {@code {exceptional yes, yes, exceptional no}}; {@link #NO_EXCEPTIONAL_YES} and
     *       {@link #NO_EXCEPTIONAL_NO} represent the chart's 'x' entries
     */
    public static int[] getThresholds(final FateChartOdds odds, final int chaosFactor) {
        final int clampedChaos = clampChaosFactor(chaosFactor);
        final int index = Math.clamp(odds.ordinal() + clampedChaos - INDEX_OFFSET, 0, THRESHOLDS.length - 1);
        return THRESHOLDS[index].clone();
    }

    /**
     * @param chaosFactor a chaos factor
     *
     * @return the chaos factor clamped to {@value #MINIMUM_CHAOS_FACTOR}-{@value #MAXIMUM_CHAOS_FACTOR}
     */
    public static int clampChaosFactor(final int chaosFactor) {
        return Math.clamp(chaosFactor, MINIMUM_CHAOS_FACTOR, MAXIMUM_CHAOS_FACTOR);
    }
}
