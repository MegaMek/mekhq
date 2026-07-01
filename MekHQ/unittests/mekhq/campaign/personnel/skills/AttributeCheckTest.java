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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AttributeCheckTest {

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    @Test
    void testRawConstructor() {
        Person person = mock(Person.class);
        TargetRoll expectedTargetRoll = new TargetRoll(5, "");
        AttributeCheck check = new AttributeCheck(person, expectedTargetRoll, SkillAttribute.BODY, null);

        assertEquals(expectedTargetRoll, check.getTargetNumber());
        assertEquals(SkillAttribute.BODY.getLabel(), check.getActionName());
        assertEquals(check, check.getThis());
    }

    @Test
    void testConstructor_SingleAttribute_CalculatesTargetNumber() {
        Person person = mock(Person.class);
        Attributes attributes = mock(Attributes.class);
        when(person.getATOWAttributes()).thenReturn(attributes);
        when(attributes.getAdjustedAttributeScore(eq(SkillAttribute.BODY), anyList(), any(), anyInt())).thenReturn(5);

        AttributeCheck check = new AttributeCheck(person, SkillAttribute.BODY);

        assertNotNull(check);
        assertEquals(7, check.getTargetNumber().getValue()); // 12 (Base) - 5 (Score) = 7
        assertEquals(SkillAttribute.BODY.getLabel(), check.getActionName());
        assertFalse(check.isCountUp());
    }

    @Test
    void testConstructor_DoubleAttribute_CalculatesTargetNumber() {
        Person person = mock(Person.class);
        Attributes attributes = mock(Attributes.class);
        when(person.getATOWAttributes()).thenReturn(attributes);
        when(attributes.getAdjustedAttributeScore(eq(SkillAttribute.DEXTERITY), any(), any(), anyInt())).thenReturn(6);
        when(attributes.getAdjustedAttributeScore(eq(SkillAttribute.REFLEXES), any(), any(), anyInt())).thenReturn(4);

        AttributeCheck check = new AttributeCheck(person, SkillAttribute.DEXTERITY, SkillAttribute.REFLEXES);

        assertNotNull(check);
        assertEquals(8, check.getTargetNumber().getValue()); // 18 (Base) - 6 - 4 = 8

        String expectedLabel = SkillAttribute.DEXTERITY.getLabel() + "-" + SkillAttribute.REFLEXES.getLabel();
        assertEquals(expectedLabel, check.getActionName());
    }

    @Test
    void testConstructor_NullFirstAttribute() {
        Person person = mock(Person.class);
        assertThrows(IllegalArgumentException.class, () -> new AttributeCheck(person, (SkillAttribute) null));
    }

    @Test
    void testConstructor_NoneFirstAttribute() {
        Person person = mock(Person.class);
        assertThrows(IllegalArgumentException.class, () -> new AttributeCheck(person, SkillAttribute.NO_ATTRIBUTE));
    }

    @Test
    void testIsEasierThan() {
        Person person = mock(Person.class);
        AttributeCheck hardCheck = new AttributeCheck(person, new TargetRoll(9, ""), SkillAttribute.BODY, null);
        AttributeCheck easyCheck = new AttributeCheck(person, new TargetRoll(5, ""), SkillAttribute.BODY, null);

        assertTrue(easyCheck.isEasierThan(hardCheck));
        assertFalse(hardCheck.isEasierThan(easyCheck));
    }

    @Test
    void testIsEasierThanEquals() {
        Person person = mock(Person.class);
        AttributeCheck hardCheck = new AttributeCheck(person, new TargetRoll(3, ""), SkillAttribute.BODY, null);
        AttributeCheck easyCheck = new AttributeCheck(person, new TargetRoll(3, ""), SkillAttribute.BODY, null);

        assertFalse(easyCheck.isEasierThan(hardCheck));
        assertFalse(hardCheck.isEasierThan(easyCheck));
    }

    @Test
    void testHasNaturalAptitude_AlwaysFalse() {
        Person person = mock(Person.class);
        AttributeCheck check = new AttributeCheck(person, new TargetRoll(6, ""), SkillAttribute.BODY, null);
        assertFalse(check.hasNaturalAptitude());
    }

    @Test
    void testIsCountUp_AlwaysFalse() {
        Person person = mock(Person.class);
        AttributeCheck check = new AttributeCheck(person, new TargetRoll(6, ""), SkillAttribute.BODY, null);
        assertFalse(check.isCountUp());
    }
}
