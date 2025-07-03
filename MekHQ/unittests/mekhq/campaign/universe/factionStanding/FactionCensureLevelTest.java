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

import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_3;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_4;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.CENSURE_LEVEL_5;
import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.getCensureLevelFromSeverity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionCensureLevelTest {
    @Test
    void test_allSeveritiesAreExclusive() {
        List<Integer> censureLevels = new ArrayList<>();
        for (FactionCensureLevel censureLevel : FactionCensureLevel.values()) {
            int currentLevel = censureLevel.getSeverity();
            assertFalse(censureLevels.contains(currentLevel),
                  "The severity level of " + censureLevel.name() + " is not exclusive.");
            censureLevels.add(currentLevel);
        }
    }

    @Test
    void test_allSeverityLevelsAreSequential() {
        int lastSeverityLevel = CENSURE_LEVEL_0.getSeverity() - 1;
        for (FactionCensureLevel censureLevel : FactionCensureLevel.values()) {
            int currentLevel = censureLevel.getSeverity();
            int expectedLevel = lastSeverityLevel + 1;
            assertEquals(expectedLevel, currentLevel,
                  "The severity level of " + censureLevel.name() + " should be " + expectedLevel + ".");
            lastSeverityLevel = currentLevel;
        }
    }

    @Test
    void test_allSeverityValuesArePossible() {
        int minimumSeverity = CENSURE_LEVEL_0.getSeverity();
        int maximumSeverity = CENSURE_LEVEL_5.getSeverity();
        for (int severity = minimumSeverity; severity <= maximumSeverity; ++severity) {
            FactionCensureLevel censureLevel = getCensureLevelFromSeverity(severity);
            assertNotNull(censureLevel, "Faction Censure Level is null for " + severity + " severity.");
        }
    }

    private static Stream<Arguments> censureStringProvider() {
        return Stream.of(
              // Valid enum name strings
              Arguments.of("NO_CENSURE", CENSURE_LEVEL_0),
              Arguments.of("CENSURE_LEVEL_3", CENSURE_LEVEL_3),
              Arguments.of("CENSURE_LEVEL_4", CENSURE_LEVEL_4),
              // Valid numeric strings
              Arguments.of("0", CENSURE_LEVEL_0),
              Arguments.of("3", CENSURE_LEVEL_3),
              Arguments.of("4", CENSURE_LEVEL_4),
              // Invalid strings
              Arguments.of("INVALID", CENSURE_LEVEL_0),
              Arguments.of("@!#", CENSURE_LEVEL_0),
              // Out-of-range numeric strings
              Arguments.of("-1", CENSURE_LEVEL_0),
              Arguments.of("10", CENSURE_LEVEL_0),
              // Empty and null
              Arguments.of("", CENSURE_LEVEL_0),
              Arguments.of(null, CENSURE_LEVEL_0)
        );
    }

    @ParameterizedTest
    @MethodSource("censureStringProvider")
    void test_getCensureLevelFromCensureString(String input, FactionCensureLevel expected) {
        assertEquals(expected, FactionCensureLevel.getCensureLevelFromCensureString(input));
    }
}
