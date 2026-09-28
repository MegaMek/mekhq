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

package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import megamek.common.MMRandom;
import megamek.common.compute.Compute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that {@link CampaignDice}, the default {@link Dice}, draws exactly the same rolls as calling MegaMek's
 * {@link Compute} directly, so switching the parts over to the dice seam changes no outcome.
 */
class CampaignDiceTest {
    private static final long SEED = 20260927L;
    private static final int NUMBER_OF_ROLLS = 50;
    private static final int RANDOM_INT_BOUND = 7;

    /** A reproducible stand-in for MegaMek's live random number generator. */
    private static final class SeededRandom extends MMRandom {
        private final Random random;

        private SeededRandom(long seed) {
            this.random = new Random(seed);
        }

        @Override
        public int randomInt(int maxValue) {
            return random.nextInt(maxValue);
        }

        @Override
        public float randomFloat() {
            return random.nextFloat();
        }
    }

    @AfterEach
    void restoreRandomness() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @Test
    void d6MatchesComputeRollForRoll() {
        Compute.setRNG(new SeededRandom(SEED));
        List<Integer> computeRolls = new ArrayList<>();
        for (int rollIndex = 0; rollIndex < NUMBER_OF_ROLLS; rollIndex++) {
            computeRolls.add(Compute.d6(2));
        }

        Compute.setRNG(new SeededRandom(SEED));
        CampaignDice campaignDice = new CampaignDice();
        List<Integer> campaignDiceRolls = new ArrayList<>();
        for (int rollIndex = 0; rollIndex < NUMBER_OF_ROLLS; rollIndex++) {
            campaignDiceRolls.add(campaignDice.d6(2));
        }

        assertEquals(computeRolls, campaignDiceRolls);
    }

    @Test
    void randomIntMatchesComputeRollForRoll() {
        Compute.setRNG(new SeededRandom(SEED));
        List<Integer> computeRolls = new ArrayList<>();
        for (int rollIndex = 0; rollIndex < NUMBER_OF_ROLLS; rollIndex++) {
            computeRolls.add(Compute.randomInt(RANDOM_INT_BOUND));
        }

        Compute.setRNG(new SeededRandom(SEED));
        CampaignDice campaignDice = new CampaignDice();
        List<Integer> campaignDiceRolls = new ArrayList<>();
        for (int rollIndex = 0; rollIndex < NUMBER_OF_ROLLS; rollIndex++) {
            campaignDiceRolls.add(campaignDice.randomInt(RANDOM_INT_BOUND));
        }

        assertEquals(computeRolls, campaignDiceRolls);
    }
}
