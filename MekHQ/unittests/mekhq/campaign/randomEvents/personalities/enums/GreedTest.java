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
package mekhq.campaign.randomEvents.personalities.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import megamek.common.enums.Gender;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class GreedTest {
    @ParameterizedTest
    @CsvSource(value = { "ASTUTE,ASTUTE", "INVALID_STATUS,NONE", "'',NONE", "'null',NONE", "1,ASTUTE" })
    void testFromStringVariousInputs(String input, Greed expected) {
        if ("null".equals(input)) {
            input = null;
        }
        Greed result = Greed.fromString(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @EnumSource(value = Greed.class)
    void testFromString_Ordinal_All(Greed value) {
        Greed result = Greed.fromString(String.valueOf(value.ordinal()));
        assertEquals(value, result);
    }

    @ParameterizedTest
    @EnumSource(value = Greed.class)
    void testGetLabel_notInvalid(Greed status) {
        String label = status.getLabel();
        assertTrue(isResourceKeyValid(label));
    }

    static Stream<Arguments> provideGreedsAndIndices() {
        return Arrays.stream(Greed.values())
                     .flatMap(trait -> IntStream.range(0, Greed.MAXIMUM_VARIATIONS)
                                             .mapToObj(i -> Arguments.of(trait, i)));
    }

    @ParameterizedTest
    @MethodSource(value = "provideGreedsAndIndices")
    void testGetDescription_notInvalid(Greed trait, int i) {
        String description = trait.getDescription(i, Gender.MALE, "Barry");
        assertTrue(isResourceKeyValid(description));
    }

    @ParameterizedTest
    @CsvSource(value = { "99", "1000", "-1" })
    void testGetDescription_InvalidDescriptionIndex(int invalidIndex) {
        String description = Greed.NONE.getDescription(invalidIndex, Gender.MALE, "Barry");
        assertTrue(isResourceKeyValid(description));
    }

    @ParameterizedTest
    @EnumSource(value = Greed.class)
    void testGetRoninMessage_notInvalid(Greed trait) {
        String description = trait.getRoninMessage("Commander");
        assertTrue(isResourceKeyValid(description));
    }
}
