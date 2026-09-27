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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;
import org.junit.jupiter.api.Test;

class CheckOddsTest {
    private static final double DELTA = 1e-9;
    /** The chance that two rolls of 2d6 come up equal: the sum of the square of each total's chance. */
    private static final double TIE = 146.0 / 1296;

    @Test
    void sevenOrMoreOnTwoDice() {
        assertEquals(21.0 / 36, CheckOdds.chance(CheckTarget.of(7), false), DELTA);
    }

    @Test
    void targetsOutsideTheDiceAreCertainOrHopeless() {
        assertEquals(1.0, CheckOdds.chance(CheckTarget.of(2), false), DELTA);
        assertEquals(0.0, CheckOdds.chance(CheckTarget.of(13), true), DELTA);
        assertEquals(0.0, CheckOdds.chance(new CheckTarget(7, false, true, RollType.NORMAL), true), DELTA);
    }

    @Test
    void countUpSkillsSucceedAtOrUnderTheTarget() {
        assertEquals(21.0 / 36, CheckOdds.chance(new CheckTarget(7, true, false, RollType.NORMAL), false), DELTA);
    }

    @Test
    void edgeReRollsAFailureOnce() {
        double once = 21.0 / 36;
        assertEquals(once + (1 - once) * once, CheckOdds.chance(CheckTarget.of(7), true), DELTA);
    }

    @Test
    void naturalAptitudeRollsBetter() {
        double normal = CheckOdds.chance(CheckTarget.of(8), false);
        double aptitude = CheckOdds.chance(new CheckTarget(8, false, false, RollType.ADVANTAGE), false);
        assertTrue(aptitude > normal);
        // Keeping the best two of three dice makes 12 as likely as rolling at least two sixes: 16 of 216.
        assertEquals(16.0 / 216, CheckOdds.chance(new CheckTarget(12, false, false, RollType.ADVANTAGE), false),
              DELTA);
    }

    @Test
    void anEvenContestFavoursTheDefender() {
        double acting = CheckOdds.opposedWinChance(CheckTarget.of(7), false, CheckTarget.of(7), false);
        assertEquals((1 - TIE) / 2, acting, DELTA);
        assertTrue(acting < 0.5);
    }

    @Test
    void edgeHelpsWhoeverHasIt() {
        CheckTarget target = CheckTarget.of(7);
        double plain = CheckOdds.opposedWinChance(target, false, target, false);
        assertTrue(CheckOdds.opposedWinChance(target, true, target, false) > plain);
        assertTrue(CheckOdds.opposedWinChance(target, false, target, true) < plain);
    }

    @Test
    void theSidesChancesAddUp() {
        CheckTarget acting = CheckTarget.of(6);
        CheckTarget defending = CheckTarget.of(8);
        double actingWins = CheckOdds.opposedWinChance(acting, false, defending, false);
        double defendingWins = 1 - actingWins;
        assertTrue(actingWins > defendingWins);
        assertFalse(Double.isNaN(actingWins));
    }

    @Test
    void marginsAreCappedAtTen() {
        assertEquals(10, CheckTarget.of(-20).margin(12));
        assertEquals(-10, new CheckTarget(5, false, true, RollType.NORMAL).margin(12));
        assertEquals(3, new CheckTarget(9, true, false, RollType.NORMAL).margin(6));
    }

    // region Brute force

    /** Every roll of the dice for a target, with its chance: two dice, or three keeping the best or worst two. */
    private static double[] totals(final RollType rollType) {
        double[] chances = new double[13];
        int dice = (rollType == RollType.NORMAL) ? 2 : 3;
        int outcomes = (int) Math.pow(6, dice);
        for (int outcome = 0; outcome < outcomes; outcome++) {
            int[] faces = new int[dice];
            int rest = outcome;
            for (int die = 0; die < dice; die++) {
                faces[die] = rest % 6 + 1;
                rest /= 6;
            }
            java.util.Arrays.sort(faces);
            int total = (dice == 2) ? faces[0] + faces[1]
                              : (rollType == RollType.ADVANTAGE) ? faces[1] + faces[2] : faces[0] + faces[1];
            chances[total] += 1.0 / outcomes;
        }
        return chances;
    }

    /**
     * Plays out an opposed check exactly as the rules describe, independently of {@link CheckOdds}: both roll, whoever
     * is losing re-rolls once if they may, and the higher margin wins with ties to the defender.
     */
    private static double bruteForceOpposed(final CheckTarget acting, final boolean actingEdge,
          final CheckTarget defending, final boolean defendingEdge) {
        double[] a = totals(acting.rollType());
        double[] d = totals(defending.rollType());
        double wins = 0;
        for (int first = 2; first <= 12; first++) {
            for (int second = 2; second <= 12; second++) {
                double chance = a[first] * d[second];
                boolean actingAhead = acting.margin(first) > defending.margin(second);
                if (actingAhead && defendingEdge && !defending.impossible()) {
                    for (int reroll = 2; reroll <= 12; reroll++) {
                        if (acting.margin(first) > defending.margin(reroll)) {
                            wins += chance * d[reroll];
                        }
                    }
                } else if (!actingAhead && actingEdge && !acting.impossible()) {
                    for (int reroll = 2; reroll <= 12; reroll++) {
                        if (acting.margin(reroll) > defending.margin(second)) {
                            wins += chance * a[reroll];
                        }
                    }
                } else if (actingAhead) {
                    wins += chance;
                }
            }
        }
        return wins;
    }

    @Test
    void singleChecksMatchEveryRollOfTheDice() {
        for (RollType rollType : RollType.values()) {
            double[] totals = totals(rollType);
            for (int target = 0; target <= 14; target++) {
                double expected = 0;
                for (int total = 2; total <= 12; total++) {
                    expected += (total >= target) ? totals[total] : 0;
                }
                CheckTarget check = new CheckTarget(target, false, false, rollType);
                assertEquals(expected, CheckOdds.chance(check, false), DELTA, rollType + " " + target);
            }
        }
    }

    @Test
    void opposedChecksMatchPlayingEveryCaseOut() {
        CheckTarget[] targets = { CheckTarget.of(4), CheckTarget.of(7), CheckTarget.of(11),
                                  new CheckTarget(8, false, false, RollType.ADVANTAGE),
                                  new CheckTarget(6, true, false, RollType.NORMAL),
                                  new CheckTarget(7, false, true, RollType.NORMAL) };
        boolean[] edges = { false, true };
        for (CheckTarget acting : targets) {
            for (CheckTarget defending : targets) {
                for (boolean actingEdge : edges) {
                    for (boolean defendingEdge : edges) {
                        assertEquals(bruteForceOpposed(acting, actingEdge, defending, defendingEdge),
                              CheckOdds.opposedWinChance(acting, actingEdge, defending, defendingEdge), DELTA,
                              acting + " vs " + defending + " edge " + actingEdge + "/" + defendingEdge);
                    }
                }
            }
        }
    }

    @Test
    void theGuideExampleOddsAreRight() {
        // Rook needs 6+, the Easy Regular dock guard 7+: the Checks guide says Rook wins 56% of the time.
        assertEquals(56, Math.round(CheckOdds.opposedWinChance(CheckTarget.of(6), false, CheckTarget.of(7), false)
                                          * 100));
    }

    // endregion Brute force
}
