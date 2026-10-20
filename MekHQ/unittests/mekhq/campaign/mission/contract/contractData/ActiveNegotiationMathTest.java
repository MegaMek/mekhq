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
package mekhq.campaign.mission.contract.contractData;

import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.marginChances;
import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.marginScore;
import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.netMargin;
import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.opposedOdds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies {@link ActiveNegotiationMath}: the level scoring of a {@link MarginOfSuccess}, the net of an opposed
 * pair, and the exact odds of each margin and of an opposed negotiation.
 */
class ActiveNegotiationMathTest {

    @Test
    void barelyMadeItScoresZeroAndBetterOutscoresWorse() {
        assertEquals(0, marginScore(MarginOfSuccess.BARELY_MADE_IT));
        assertTrue(marginScore(MarginOfSuccess.SPECTACULAR) > 0, "the best result must score positive");
        assertTrue(marginScore(MarginOfSuccess.DISASTROUS) < 0, "the worst result must score negative");
        assertTrue(marginScore(MarginOfSuccess.GOOD) > marginScore(MarginOfSuccess.IT_WILL_DO),
              "a better margin must score higher");
    }

    @ParameterizedTest
    @EnumSource(MarginOfSuccess.class)
    void marginScoreIsStrictlyBetterForBetterResults(final MarginOfSuccess margin) {
        // Enum is ordered best-first, so a lower ordinal (better result) must never score lower than a worse one.
        for (MarginOfSuccess worse : MarginOfSuccess.values()) {
            if (margin.ordinal() < worse.ordinal()) {
                assertTrue(marginScore(margin) > marginScore(worse),
                      margin + " should outscore " + worse);
            }
        }
    }

    @Test
    void netMarginFavorsTheStrongerNegotiator() {
        // Player clearly better -> positive net (that many terms improve).
        assertTrue(netMargin(MarginOfSuccess.SPECTACULAR, MarginOfSuccess.DISASTROUS) > 0);
        // Employer clearly better -> negative net (that many lower).
        assertTrue(netMargin(MarginOfSuccess.DISASTROUS, MarginOfSuccess.SPECTACULAR) < 0);
        // Equal results -> stalemate.
        assertEquals(0, netMargin(MarginOfSuccess.GOOD, MarginOfSuccess.GOOD));
        // The contest is antisymmetric: swapping the sides negates the net.
        assertEquals(-netMargin(MarginOfSuccess.GOOD, MarginOfSuccess.BAD),
              netMargin(MarginOfSuccess.BAD, MarginOfSuccess.GOOD));
    }

    private static final double TOLERANCE = 1e-9;

    private static double successChance(double[] chances) {
        double success = 0;
        for (MarginOfSuccess margin : MarginOfSuccess.values()) {
            if (margin.ordinal() <= MarginOfSuccess.BARELY_MADE_IT.ordinal()) {
                success += chances[margin.ordinal()];
            }
        }
        return success;
    }

    private static double total(double[] chances) {
        double total = 0;
        for (double chance : chances) {
            total += chance;
        }
        return total;
    }

    @Test
    void marginChancesAlwaysSumToOne() {
        assertEquals(1.0, total(marginChances(7, false, false, false, false)), TOLERANCE);
        assertEquals(1.0, total(marginChances(9, false, false, true, false)), TOLERANCE);
        assertEquals(1.0, total(marginChances(10, false, false, false, true)), TOLERANCE);
        assertEquals(1.0, total(marginChances(4, false, true, true, true)), TOLERANCE);
    }

    @Test
    void twoDiceMeetTheTargetExactlyOnASeven() {
        // 2d6 against 7: exactly 7 (six ways in 36) barely makes it.
        double[] chances = marginChances(7, false, false, false, false);
        assertEquals(6.0 / 36, chances[MarginOfSuccess.BARELY_MADE_IT.ordinal()], TOLERANCE);
        // 7 or more (21 ways in 36) succeeds.
        assertEquals(21.0 / 36, successChance(chances), TOLERANCE);
    }

    @Test
    void naturalAptitudeKeepsTheHighestTwoOfThreeDice() {
        // Three dice keeping the best two make 12 when at least two show a six: 16 ways in 216.
        double[] chances = marginChances(12, false, false, true, false);
        assertEquals(16.0 / 216, chances[MarginOfSuccess.BARELY_MADE_IT.ordinal()], TOLERANCE);
    }

    @Test
    void edgeRerollsAFailure() {
        // 2d6 against 8 fails 21 times in 36; with an Edge reroll it fails only when both rolls do.
        double failure = 21.0 / 36;
        assertEquals(1 - failure, successChance(marginChances(8, false, false, false, false)), TOLERANCE);
        assertEquals(1 - failure * failure, successChance(marginChances(8, false, false, false, true)), TOLERANCE);
    }

    @Test
    void edgeIsNotSpentOnACheckThatCannotSucceed() {
        double[] chances = marginChances(Integer.MAX_VALUE, true, false, false, true);
        assertEquals(1.0, chances[MarginOfSuccess.DISASTROUS.ordinal()], TOLERANCE);
    }

    @Test
    void evenlyMatchedNegotiatorsAreEquallyLikelyToWin() {
        double[] chances = marginChances(7, false, false, false, false);
        ActiveNegotiationMath.Odds odds = opposedOdds(chances, chances, 0);
        assertEquals(odds.win(), odds.loss(), TOLERANCE);
        assertEquals(1.0, odds.win() + odds.stalemate() + odds.loss(), TOLERANCE);
    }

    @Test
    void anEmployerWithNoNegotiatorAlwaysBarelyMakesIt() {
        // Against a fixed "barely made it", the player wins on 8 or more and draws on exactly 7.
        ActiveNegotiationMath.Odds odds = opposedOdds(marginChances(7, false, false, false, false), null, 0);
        assertEquals(15.0 / 36, odds.win(), TOLERANCE);
        assertEquals(6.0 / 36, odds.stalemate(), TOLERANCE);
    }

    @Test
    void aPositiveModifierShiftsTheOddsTowardThePlayer() {
        double[] chances = marginChances(7, false, false, false, false);
        assertTrue(opposedOdds(chances, chances, 1).win() > opposedOdds(chances, chances, 0).win());
        assertTrue(opposedOdds(chances, chances, -1).win() < opposedOdds(chances, chances, 0).win());
    }
}
