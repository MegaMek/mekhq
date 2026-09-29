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
package mekhq.campaign.chaosCampaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the anti-snowball limits in {@link ChaosScaleLimits}: the Hiring Hall contract Scale cap, the combat pay and
 * salvage taper, and the Hot Spots upkeep escalation.
 */
class ChaosScaleLimitsTest {
    private static final double DELTA = 1e-9;

    // region getMaximumContractScale

    @ParameterizedTest
    @CsvSource({ "STANDARD, 8", "MINOR, 6", "QUESTIONABLE, 4", "NONE, 2" })
    void cappedHiringHallsLimitContractScale(HiringHallLevel hiringHallLevel, int expectedMaximum) {
        assertEquals(expectedMaximum, ChaosScaleLimits.getMaximumContractScale(hiringHallLevel));
    }

    @Test
    void aGreatHiringHallDoesNotCapContractScale() {
        assertEquals(Integer.MAX_VALUE, ChaosScaleLimits.getMaximumContractScale(HiringHallLevel.GREAT));
    }

    @Test
    void betterHiringHallsNeverBrokerSmallerJobs() {
        HiringHallLevel[] worstToBest = { HiringHallLevel.NONE, HiringHallLevel.QUESTIONABLE, HiringHallLevel.MINOR,
                                          HiringHallLevel.STANDARD, HiringHallLevel.GREAT };
        for (int index = 1; index < worstToBest.length; index++) {
            assertTrue(ChaosScaleLimits.getMaximumContractScale(worstToBest[index])
                             > ChaosScaleLimits.getMaximumContractScale(worstToBest[index - 1]),
                  worstToBest[index] + " should allow a larger Scale than " + worstToBest[index - 1]);
        }
    }

    @ParameterizedTest
    @EnumSource(HiringHallLevel.class)
    void everyHiringHallAllowsAtLeastScaleOne(HiringHallLevel hiringHallLevel) {
        assertTrue(ChaosScaleLimits.getMaximumContractScale(hiringHallLevel) >= 1);
    }

    // endregion getMaximumContractScale

    // region getTaperedScale

    @ParameterizedTest
    @CsvSource({ "0, 0", "1, 1", "2, 2", "3, 3" })
    void scaleUpToThreeIsWorthItsFullValue(int scale, double expected) {
        assertEquals(expected, ChaosScaleLimits.getTaperedScale(scale), DELTA);
    }

    @ParameterizedTest
    @CsvSource({ "4, 3.5", "5, 4", "7, 5", "10, 6.5", "23, 13" })
    void eachPointOfScaleAboveThreeIsWorthHalf(int scale, double expected) {
        assertEquals(expected, ChaosScaleLimits.getTaperedScale(scale), DELTA);
    }

    @Test
    void taperedScaleKeepsRisingWithScale() {
        double previous = ChaosScaleLimits.getTaperedScale(0);
        for (int scale = 1; scale <= 50; scale++) {
            double current = ChaosScaleLimits.getTaperedScale(scale);
            assertTrue(current > previous, "a larger force must never be worth less, Scale " + scale);
            previous = current;
        }
    }

    @Test
    void negativeScaleIsPassedThroughUntapered() {
        assertEquals(-2, ChaosScaleLimits.getTaperedScale(-2), DELTA);
    }

    // endregion getTaperedScale

    // region getTaperMultiplier

    @ParameterizedTest
    @CsvSource({ "-1", "0", "1", "2", "3" })
    void noTaperAtOrBelowScaleThree(int scale) {
        assertEquals(1.0, ChaosScaleLimits.getTaperMultiplier(scale), DELTA);
    }

    @ParameterizedTest
    @CsvSource({ "4, 0.875", "5, 0.8", "7, 0.7142857142857143", "10, 0.65" })
    void taperMultiplierIsTaperedScaleOverScale(int scale, double expected) {
        assertEquals(expected, ChaosScaleLimits.getTaperMultiplier(scale), DELTA);
    }

    @Test
    void taperMultiplierStaysWithinHalfAndOne() {
        for (int scale = 0; scale <= 1_000; scale++) {
            double multiplier = ChaosScaleLimits.getTaperMultiplier(scale);
            assertTrue(multiplier > 0.5 && multiplier <= 1.0, "Scale " + scale + " gave " + multiplier);
        }
    }

    @Test
    void taperMultiplierNeverRisesWithScale() {
        double previous = ChaosScaleLimits.getTaperMultiplier(0);
        for (int scale = 1; scale <= 100; scale++) {
            double current = ChaosScaleLimits.getTaperMultiplier(scale);
            assertTrue(current <= previous, "Scale " + scale);
            previous = current;
        }
    }

    // endregion getTaperMultiplier

    // region getUpkeepEscalationMultiplier

    @ParameterizedTest
    @CsvSource({ "-3", "0", "1", "4" })
    void noEscalationAtOrBelowScaleFour(int scale) {
        assertEquals(1.0, ChaosScaleLimits.getUpkeepEscalationMultiplier(scale), DELTA);
    }

    @ParameterizedTest
    @CsvSource({ "5, 1.05", "6, 1.10", "10, 1.30", "24, 2.0" })
    void upkeepRisesFivePercentPerScaleAboveFour(int scale, double expected) {
        assertEquals(expected, ChaosScaleLimits.getUpkeepEscalationMultiplier(scale), DELTA);
    }

    // endregion getUpkeepEscalationMultiplier
}
