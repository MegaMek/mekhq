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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.MAX_ACCOLADE_RECOGNITION;
import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.MIN_ACCOLADE_RECOGNITION;
import static org.junit.jupiter.api.Assertions.*;

class FactionAccoladeLevelTest {
    @Test
    void test_allAccoladeLevelsAreExclusive() {
        List<Integer> accoladeLevels = new ArrayList<>();
        for (FactionAccoladeLevel accoladeLevel : FactionAccoladeLevel.values()) {
            int currentLevel = accoladeLevel.getRecognition();
            assertFalse(accoladeLevels.contains(currentLevel),
                  "The accolade level of " + accoladeLevel.name() + " is not exclusive.");
            accoladeLevels.add(currentLevel);
        }
    }

    @Test
    void test_allAccoladeLevelsAreSequential() {
        int lastLevel = FactionAccoladeLevel.NO_ACCOLADE.getRecognition() - 1;
        for (FactionAccoladeLevel accoladeLevel : FactionAccoladeLevel.values()) {
            int currentLevel = accoladeLevel.getRecognition();
            int expectedLevel = lastLevel + 1;
            assertEquals(expectedLevel, currentLevel,
                  "The accolade level of " + accoladeLevel.name() + " should be " + expectedLevel + ".");
            lastLevel = currentLevel;
        }
    }

    @Test
    void test_allAccoladeValuesArePossible() {
        for (int level = MIN_ACCOLADE_RECOGNITION; level <= MAX_ACCOLADE_RECOGNITION; ++level) {
            FactionAccoladeLevel accoladeLevel = FactionAccoladeLevel.getAccoladeRecognitionFromRecognition(level);
            assertNotNull(accoladeLevel, "Faction Accolade Level is null for " + level + " level.");
        }
    }

    private static Stream<Arguments> accoladeStringProvider() {
        return Stream.of(
              // Valid enum name strings
              Arguments.of("NONE", FactionAccoladeLevel.NO_ACCOLADE),
              Arguments.of("FIELD_COMMENDATION", FactionAccoladeLevel.FIELD_COMMENDATION),
              Arguments.of("OFFICIAL_COMMENDATION", FactionAccoladeLevel.OFFICIAL_COMMENDATION),
              // Valid numeric strings
              Arguments.of("0", FactionAccoladeLevel.NO_ACCOLADE),
                Arguments.of("1", FactionAccoladeLevel.TAKING_NOTICE),
                Arguments.of("3", FactionAccoladeLevel.OFFICIAL_COMMENDATION),
              // Invalid strings
              Arguments.of("INVALID", FactionAccoladeLevel.NO_ACCOLADE),
              Arguments.of("@!#", FactionAccoladeLevel.NO_ACCOLADE),
              // Out-of-range numeric strings
              Arguments.of("-1", FactionAccoladeLevel.NO_ACCOLADE),
              Arguments.of("10", FactionAccoladeLevel.NO_ACCOLADE),
              // Empty and null
              Arguments.of("", FactionAccoladeLevel.NO_ACCOLADE),
              Arguments.of(null, FactionAccoladeLevel.NO_ACCOLADE)
        );
    }

    @ParameterizedTest
    @MethodSource("accoladeStringProvider")
    void test_getAccoladeLevelFromString(String input, FactionAccoladeLevel expected) {
        assertEquals(expected, FactionAccoladeLevel.getAccoladeRecognitionFromString(input));
    }
}
