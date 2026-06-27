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

package mekhq.campaign.personnel.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import megamek.common.TargetRollModifier;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SkillCheckTest {

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    @Test
    void testRawConstructor() {
        Person person = mock(Person.class);
        SkillType skillType = SkillType.getType(SkillType.S_GUN_MEK);
        TargetRoll expectedTargetRoll = new TargetRoll(5, "");
        SkillCheck check = new SkillCheck(person, skillType, expectedTargetRoll);

        assertEquals(expectedTargetRoll, check.getTargetNumber());
        assertEquals(SkillType.S_GUN_MEK, check.getActionName());
        assertEquals(check, check.getThis());
    }

    @Test
    void testConstructor_CalculatesTargetNumber() {
        Person person = mock(Person.class);
        when(person.hasSkill(eq(SkillType.S_PILOT_MEK))).thenReturn(true);
        when(person.getSkill(eq(SkillType.S_PILOT_MEK)))
              .thenReturn(new Skill(SkillType.getType(SkillType.S_PILOT_MEK), 2, 0));
        when(person.getSkillModifierData(anyBoolean(), anyBoolean(), any())).thenReturn(mock(SkillModifierData.class));
        SkillCheck check = new SkillCheck(person, SkillType.S_PILOT_MEK, false, false, LocalDate.of(3151, 1, 1));

        assertEquals(6, check.getTargetNumber().getValue()); // == 8 - 2
        assertEquals(SkillType.S_PILOT_MEK, check.getActionName());
        assertFalse(check.isCountUp());
    }

    @Test
    void testConstructor_CalculatesTargetNumber_Aging() {
        Person person = new Person("GivenName", "Surname", null, "Faction");
        LocalDate dateOfBirth = LocalDate.of(3151, 1, 1);
        int age = 55;
        LocalDate today = dateOfBirth.plusYears(age);
        person.addSkill(SkillType.S_NEGOTIATION, 4, 0);
        person.setDateOfBirth(dateOfBirth);
        person.setAgeForAttributeModifiers(age);
        person.setRank(40);
        SkillCheck check = person.checkSkill(SkillType.S_NEGOTIATION, true, true, today);

        assertNotNull(check);
        assertEquals(2, Aging.getReputationAgeModifier(age, true, false, 40)); // 2
        assertEquals(4, check.getTargetNumber().getValue()); // == 10 - 4 - 2 (aging effect)
    }

    @ParameterizedTest
    @CsvSource({ "true", "false" })
    void testHasNaturalAptitude(boolean hasNaturalAptitude) {
        Person person = mock(Person.class);
        Skill skill = mock(Skill.class);
        when(person.getSkill(SkillType.S_GUN_MEK)).thenReturn(skill);
        when(skill.getHasNaturalAptitude()).thenReturn(hasNaturalAptitude);
        SkillType skillType = SkillType.getType(SkillType.S_GUN_MEK);
        SkillCheck check = new SkillCheck(person, skillType, new TargetRoll(4, ""));

        assertEquals(hasNaturalAptitude, check.hasNaturalAptitude());
    }

    @Test
    void testHasNaturalAptitude_Untrained() {
        Person person = mock(Person.class);
        when(person.getSkill(SkillType.S_GUN_MEK)).thenReturn(null);
        SkillType skillType = SkillType.getType(SkillType.S_GUN_MEK);
        SkillCheck check = new SkillCheck(person, skillType, new TargetRoll(4, ""));

        assertFalse(check.hasNaturalAptitude());
    }

    @Test
    void testIsCountUpTrue() {
        Person person = mock(Person.class);
        SkillType skillType = mock(SkillType.class);
        when(skillType.isCountUp()).thenReturn(true);
        when(skillType.getName()).thenReturn("TestCountUpSkill");
        SkillCheck check = new SkillCheck(person, skillType, new TargetRoll(2, ""));

        assertTrue(check.isCountUp());
    }

    @Test
    void testWithExternalModifiers_AppliesToTargetNumber() {
        Person person = mock(Person.class);
        SkillType skillType = SkillType.getType(SkillType.S_GUN_MEK);
        TargetRoll baseRoll = new TargetRoll(5, "");
        SkillCheck check = new SkillCheck(person, skillType, baseRoll);
        check.withExternalModifiers(List.of(new TargetRollModifier(2, "One"), new TargetRollModifier(-1, "Two")));

        assertEquals(6, check.getTargetNumber().getValue());
    }

    @Test
    void testWithMiscModifier_IsCountUpFalse() {
        Person person = mock(Person.class);
        SkillType skillType = SkillType.getType(SkillType.S_GUN_MEK);
        SkillCheck check = new SkillCheck(person, skillType, new TargetRoll(5, "")).withMiscModifier(2);

        assertEquals(7, check.getTargetNumber().getValue());
    }

    @Test
    void testWithMiscModifier_IsCountUpTrue() {
        Person person = mock(Person.class);
        SkillType skillType = mock(SkillType.class);
        when(skillType.isCountUp()).thenReturn(true);
        when(skillType.getName()).thenReturn("TestCountUpSkill");
        SkillCheck check = new SkillCheck(person, skillType, new TargetRoll(5, "")).withMiscModifier(2);

        assertEquals(3, check.getTargetNumber().getValue());
    }
}
