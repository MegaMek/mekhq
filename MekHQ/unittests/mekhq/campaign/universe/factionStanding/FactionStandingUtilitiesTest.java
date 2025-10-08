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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingUtilitiesTest {
    static Stream<Arguments> standingLevelProvider() {
        return Stream.of(
              // In-range for all standing levels
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_1.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_1.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_2.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_2.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_2),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_3.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_2),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_3.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_3),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_4.getMinimumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_3),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_4.getMaximumRegard(),
                    FactionStandingLevel.STANDING_LEVEL_4),
              // Typical mid-range values
              Arguments.of((FactionStandingLevel.STANDING_LEVEL_2.getMinimumRegard() +
                                  FactionStandingLevel.STANDING_LEVEL_2.getMaximumRegard()) / 2,
                    FactionStandingLevel.STANDING_LEVEL_2),
              // Out-of-range (below min, above max)
              Arguments.of(Double.NEGATIVE_INFINITY, FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(Double.POSITIVE_INFINITY, FactionStandingLevel.STANDING_LEVEL_8),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard() - 100.0,
                    FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of(FactionStandingLevel.STANDING_LEVEL_8.getMaximumRegard() + 100.0,
                    FactionStandingLevel.STANDING_LEVEL_8)
        );
    }

    @ParameterizedTest
    @MethodSource("standingLevelProvider")
    @DisplayName("Test calculateFactionStandingLevel for in-range and out-of-range values")
    void testCalculateFactionStandingLevel(double inputRegard, FactionStandingLevel expectedLevel) {
        assertEquals(expectedLevel, FactionStandingUtilities.calculateFactionStandingLevel(inputRegard));
    }

    @Test
    @DisplayName("Test calculateFactionStandingLevel always returns a valid standing level")
    void testAlwaysReturnsStandingLevel() {
        for (double regard : new double[] { Double.MIN_VALUE, 0.0, 1.0, -1.0, 100000, -100000, Double.NaN }) {
            assertNotNull(FactionStandingUtilities.calculateFactionStandingLevel(regard));
        }
    }

}
