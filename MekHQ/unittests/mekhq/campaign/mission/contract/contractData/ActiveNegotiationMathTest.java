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

import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.marginScore;
import static mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath.netMargin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.contract.utilities.ActiveNegotiationMath;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Verifies {@link ActiveNegotiationMath}: the level scoring of a {@link MarginOfSuccess} and the net of an opposed
 * pair.
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
}
