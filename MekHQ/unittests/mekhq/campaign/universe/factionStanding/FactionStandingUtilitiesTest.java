/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel.STANDING_LEVEL_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingUtilitiesTest {
    @Test
    void test_calculateFactionStandingLevel_veryLowRegard() {
        // Setup
        int regard = Integer.MIN_VALUE + 1; // values must be above Integer#MIN_VALUE

        // Act
        FactionStandingLevel result = FactionStandingUtilities.calculateFactionStandingLevel(regard);

        // Assert
        assertEquals(STANDING_LEVEL_0, result, "Expected default STANDING_LEVEL_0 for very low Regard.");
    }

    @Test
    void test_calculateFactionStandingLevel_veryHighRegard() {
        // Setup
        int regard = Integer.MAX_VALUE;

        // Act
        FactionStandingLevel result = FactionStandingUtilities.calculateFactionStandingLevel(regard);

        // Assert
        assertEquals(STANDING_LEVEL_8, result, "Expected default STANDING_LEVEL_8 for very high Regard.");
    }

    @ParameterizedTest(name = "Regard {0} → Standing {1}")
    @MethodSource(value = "regardAndExpectedStandingLevel")
    void testCalculateFactionStandingLevel_AllLevels(int regard, FactionStandingLevel expectedStanding) {
        // Act
        FactionStandingLevel result = FactionStandingUtilities.calculateFactionStandingLevel(regard);

        // Assert
        assertEquals(expectedStanding, result, "Expected " + expectedStanding.name() + " for " + regard + " regard.");
    }

    private static Stream<Arguments> regardAndExpectedStandingLevel() {
        return Arrays.stream(FactionStandingLevel.values()).flatMap(standing -> {
            // We're casting to an int as we don't need to check every possible decimal value.
            // If all ints pass the test, then all doubles will too.
            int minimumRegard = (int) standing.getMinimumRegard() + 1;
            int maximumRegard = (int) standing.getMaximumRegard();
            // These special handlers stop us iterating for all values between Integer#MIN_VALUE and
            // Integer#MAX_VALUE.
            if (standing == STANDING_LEVEL_0) {
                minimumRegard = -110;
            }
            if (standing == STANDING_LEVEL_8) {
                maximumRegard = 110;
            }
            return IntStream.rangeClosed(minimumRegard, maximumRegard)
                         .mapToObj(regard -> Arguments.of(regard, standing));
        });
    }
}
