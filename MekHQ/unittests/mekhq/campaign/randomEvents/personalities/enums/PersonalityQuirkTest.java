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

import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.NONE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import megamek.common.enums.Gender;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class PersonalityQuirkTest {
    @ParameterizedTest
    @CsvSource(value = { "ADJUSTS_CLOTHES,ADJUSTS_CLOTHES", "INVALID_STATUS,NONE", "'',NONE", "'null',NONE",
                         "1,ADJUSTS_CLOTHES" })
    void testFromStringVariousInputs(String input, PersonalityQuirk expected) {
        if ("null".equals(input)) {
            input = null;
        }
        PersonalityQuirk result = PersonalityQuirk.fromString(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @EnumSource(value = PersonalityQuirk.class)
    void testFromString_Ordinal_All(PersonalityQuirk quirk) {
        String ordinalString = String.valueOf(quirk.ordinal());
        assertEquals(quirk, PersonalityQuirk.fromString(ordinalString));
    }

    static Stream<Arguments> provideTraitsAndRoles() {
        Faction originFaction = Factions.getInstance().getFaction("MERC");
        return Arrays.stream(PersonalityQuirk.values())
                     .flatMap(trait -> IntStream.range(0, 3)
                                             .mapToObj(i -> Arguments.of(trait,
                                                   MEKWARRIOR,
                                                   i,
                                                   Gender.MALE,
                                                   originFaction,
                                                   "Barry")));
    }

    @ParameterizedTest
    @MethodSource(value = "provideTraitsAndRoles")
    void testGetDescription_notInvalid(PersonalityQuirk trait, PersonnelRole role, int validIndex, Gender gender,
          Faction faction, String name) {
        String description = trait.getDescription(role, validIndex, gender, faction, name);
        assertTrue(isResourceKeyValid(description));
    }

    @ParameterizedTest
    @EnumSource(value = PersonalityQuirk.class)
    void testGetPersonalityTraitTypeLabel_notInvalid(PersonalityQuirk status) {
        String label = status.getPersonalityTraitTypeLabel();
        assertTrue(isResourceKeyValid(label));
    }

    @ParameterizedTest
    @CsvSource(value = { "-1", "999", "1000000", "2147483647" })
        // example edge cases
    void testGetDescription_InvalidDescriptionIndex(int invalidIndex) {
        Faction originFaction = Factions.getInstance().getFaction("MERC");
        String description = NONE.getDescription(MEKWARRIOR, invalidIndex, Gender.MALE, originFaction, "Barry");
        assertTrue(isResourceKeyValid(description));
    }
}
