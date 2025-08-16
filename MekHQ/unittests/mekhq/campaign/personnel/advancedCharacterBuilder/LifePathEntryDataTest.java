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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory.FIELD_ARCHAEOLOGIST;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import java.util.stream.Stream;

import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class LifePathEntryDataTest {
    @ParameterizedTest
    @EnumSource(LifePathDataClassLookup.class)
    void testForUniqueLookupName(LifePathDataClassLookup classLookup) {
        for (LifePathDataClassLookup lookup : LifePathDataClassLookup.values()) {
            if (lookup == classLookup) {
                continue;
            }

            assertNotEquals(classLookup.getLookupName(), lookup.getLookupName(), "Lookup names should be unique");
        }
    }

    @Test
    void testFromRawEntry_ValidInput() {
        String rawLifePathEntry = "ATOW_TRAIT::CONNECTIONS::42";
        LifePathEntryData result = LifePathEntryData.fromRawEntry(rawLifePathEntry);

        assertEquals("ATOW_TRAIT", result.classLookupName(), "classLookupName should match the input");
        assertEquals("CONNECTIONS", result.objectLookupName(), "objectLookupName should match the input");
        assertEquals(42, result.value(), "value should match the input");
    }

    @Test
    void testFromRawEntry_NullInput() {
        assertThrows(NullPointerException.class, () -> LifePathEntryData.fromRawEntry(null),
              "Method should throw NullPointerException for null input");
    }

    @Test
    void testFromRawEntry_InvalidFormat_MissingParts() {
        String rawLifePathEntry = "classOnly";
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> LifePathEntryData.fromRawEntry(rawLifePathEntry),
              "Method should throw ArrayIndexOutOfBoundsException when parts are missing");
    }

    @Test
    void testFromRawEntry_InvalidFormat_NonIntegerValue() {
        String rawLifePathEntry = "ATOW_TRAIT::CONNECTIONS::notAnInt";
        int actual = LifePathEntryData.fromRawEntry(rawLifePathEntry).value();
        assertEquals(0, actual,
              "Method should return 0 when value part is not a valid integer");
    }

    @Test
    void testFromRawEntry_EmptyInput() {
        String rawLifePathEntry = "";
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> LifePathEntryData.fromRawEntry(rawLifePathEntry),
              "Method should throw ArrayIndexOutOfBoundsException for empty input as it cannot be split into parts");
    }

    @ParameterizedTest
    @EnumSource(LifePathEntryDataTraitLookup.class)
    void testGetTrait_ValidTrait(LifePathEntryDataTraitLookup trait) {
        LifePathEntryData data = new LifePathEntryData("ATOW_TRAIT", trait.getLookupName(), 3);
        assertEquals(3, data.getTrait(trait));
    }

    @Test
    void testGetTrait_InvalidTrait() {
        LifePathEntryData data = new LifePathEntryData("ATOW_TRAIT", "SOME_NONSENSE", 15);
        assertEquals(0, data.getTrait(LifePathEntryDataTraitLookup.CONNECTIONS),
              "Should return 0 as the objectLookupName does not match the trait");
    }

    @Test
    void testGetTrait_InvalidClassLookupName() {
        LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", "CONNECTIONS", 15);
        assertEquals(0, data.getTrait(LifePathEntryDataTraitLookup.CONNECTIONS),
              "Should return 0 as the classLookupName does not match ATOW_TRAIT");
    }

    @ParameterizedTest
    @EnumSource(LifePathEntryDataTraitLookup.class)
    void testGetFactionCode_ValidTrait(LifePathEntryDataTraitLookup trait) {
        LifePathEntryData data = new LifePathEntryData("FACTION_CODE", "SL", 3);
        assertEquals("SL", data.getFactionCode());
    }

    @Test
    void testGetFactionCode_InvalidClassLookupName() {
        LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", "SL", 15);
        assertNull(data.getFactionCode(), "Should return null as the classLookupName does not match INVALID_CLASS");
    }

    @Test
    void testGetLifePathUUID_ValidUUID() {
        LifePathEntryData data = new LifePathEntryData("LIFE_PATH", "123e4567-e89b-12d3-a456-426614174000", 5);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), data.getLifePathUUID(),
              "Should return the UUID as the objectLookupName is valid");
    }

    @Test
    void testGetLifePathUUID_InvalidClassLookupName() {
        LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", "123e4567-e89b-12d3-a456-426614174000", 5);
        assertNull(data.getLifePathUUID(), "Should return null because the classLookupName is not LIFE_PATH");
    }

    @Test
    void testGetLifePathUUID_InvalidObjectLookupNameFormat() {
        LifePathEntryData data = new LifePathEntryData("LIFE_PATH", "invalid-uuid-format", 5);
        assertNull(data.getLifePathUUID(),
              "Should return null because the objectLookupName is not a valid UUID format");
    }

    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    void testGetLifePathCategory_ValidCategory(LifePathCategory category) {
        LifePathEntryData data = new LifePathEntryData("LIFE_PATH_CATEGORY", category.getLookupName(), 10);
        assertEquals(10, data.getLifePathCategory(category),
              category.getLookupName() + " should be returned when category matches the objectLookupName");
    }

    @Test
    void testGetLifePathCategory_InvalidClassLookupName() {
        LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", FIELD_ARCHAEOLOGIST.getLookupName(), 10);
        assertEquals(0, data.getLifePathCategory(LifePathCategory.FIELD_DOCTOR),
              "Should return 0 because the classLookupName does not match LIFE_PATH_CATEGORY");
    }

    @Test
    void testGetLifePathCategory_InvalidObjectLookupName() {
        LifePathEntryData data = new LifePathEntryData("LIFE_PATH_CATEGORY", "INVALID_CATEGORY", 5);
        assertEquals(0, data.getLifePathCategory(LifePathCategory.FIELD_DOCTOR),
              "Should return 0 because the objectLookupName does not match the provided LifePathCategory");
    }

    @Nested
    class GetSkill_SkillType {
        @BeforeAll
        public static void setup() {
            SkillType.initializeTypes();
        }

        private static Stream<String> skills() {
            return Stream.of(SkillType.skillList);
        }

        @ParameterizedTest
        @MethodSource("skills")
        void testGetSkill_ValidSkill(String skillName) {
            LifePathEntryData data = new LifePathEntryData("SKILL", skillName, 5);
            assertEquals(5, data.getSkill(skillName),
                  "Should return " + skillName + " when classLookupName and objectLookupName match");
        }

        @Test
        void testGetSkill_InvalidClassLookupName() {
            LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", "Piloting/Mek", 5);
            assertEquals(0, data.getSkill("Piloting/Mek"),
                  "Should return 0 because classLookupName does not match SKILL");
        }

        @Test
        void testGetSkill_InvalidSkillName() {
            LifePathEntryData data = new LifePathEntryData("SKILL", "SOME_NONSENSE", 3);
            assertEquals(0, data.getSkill("SOME_NONSENSE"),
                  "Should return 0 because objectLookupName does not match the provided skill name");
        }
    }

    @ParameterizedTest
    @EnumSource(SkillAttribute.class)
    void testGetSkillAttribute_ValidCategory(SkillAttribute attribute) {
        if (attribute == NONE) {
            return;
        }

        LifePathEntryData data = new LifePathEntryData("SKILL_ATTRIBUTE", attribute.getLookupName(), 5);
        assertEquals(5, data.getSkillAttribute(attribute),
              attribute.getLookupName() + " should be returned when attribute matches the objectLookupName");
    }

    @Test
    void testGetSkillAttribute_NoneSpecialHandler() {
        LifePathEntryData data = new LifePathEntryData("SKILL_ATTRIBUTE", NONE.getLookupName(), 5);
        assertEquals(0, data.getSkillAttribute(NONE),
              "NONE should always return 0.");
    }

    @Test
    void testGetSkillAttribute_ValidCategory_BelowMinimum() {
        LifePathEntryData data = new LifePathEntryData("SKILL_ATTRIBUTE", STRENGTH.getLookupName(),
              MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, data.getSkillAttribute(STRENGTH),
              "Should return " + MINIMUM_ATTRIBUTE_SCORE + " because value is below the minimum");
    }

    @Test
    void testGetSkillAttribute_ValidCategory_AboveMaximum() {
        LifePathEntryData data = new LifePathEntryData("SKILL_ATTRIBUTE", STRENGTH.getLookupName(),
              MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, data.getSkillAttribute(STRENGTH),
              "Should return " + MAXIMUM_ATTRIBUTE_SCORE + " because value is above the maximum");
    }

    @Test
    void testGetSkillAttribute_InvalidClassLookupName() {
        LifePathEntryData data = new LifePathEntryData("INVALID_CLASS", STRENGTH.getLookupName(), 5);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, data.getSkillAttribute(STRENGTH),
              "Should return " +
                    MINIMUM_ATTRIBUTE_SCORE +
                    " because the classLookupName does not match SKILL_ATTRIBUTE");
    }

    @Test
    void testGetSkillAttribute_InvalidObjectLookupName() {
        LifePathEntryData data = new LifePathEntryData("SKILL_ATTRIBUTE", "INVALID_CATEGORY", 5);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, data.getSkillAttribute(STRENGTH),
              "Should return " +
                    MINIMUM_ATTRIBUTE_SCORE +
                    " because the objectLookupName does not match the provided SkillAttribute");
    }
}
