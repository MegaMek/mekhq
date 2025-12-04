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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Skill.getIndividualAttributeModifier;
import static mekhq.campaign.personnel.skills.Skill.getTotalAttributeModifier;
import static mekhq.campaign.personnel.skills.SkillModifierData.IGNORE_AGE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SkillTest {
    @Test
    void testGetTotalAttributeModifier_SingleLinkedAttribute() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(REFLEXES);
        testSkillType.setSecondAttribute(NONE);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              7,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        int totalModifier = getTotalAttributeModifier(targetNumber, attributes, testSkillType, new ArrayList<>(),
              new PersonnelOptions(), IGNORE_AGE);

        // Assert
        assertEquals(1, totalModifier);
    }

    @Test
    void testGetTotalAttributeModifier_TwoLinkedAttributes() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(REFLEXES);
        testSkillType.setSecondAttribute(DEXTERITY);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              7,
              8,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        int totalModifier = getTotalAttributeModifier(targetNumber, attributes, testSkillType, new ArrayList<>(),
              new PersonnelOptions(), IGNORE_AGE);

        // Assert
        assertEquals(2, totalModifier);
    }

    @Test
    void testGetTotalAttributeModifier_NoLinkedAttributes() {
        // Setup
        SkillType testSkillType = new SkillType();
        testSkillType.setFirstAttribute(NONE);
        testSkillType.setSecondAttribute(NONE);

        Attributes attributes = new Attributes(DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE,
              DEFAULT_ATTRIBUTE_SCORE);

        TargetRoll targetNumber = new TargetRoll();

        // Act
        int totalModifier = getTotalAttributeModifier(targetNumber, attributes, testSkillType, new ArrayList<>(),
              new PersonnelOptions(), IGNORE_AGE);

        // Assert
        assertEquals(0, totalModifier);
    }

    @ParameterizedTest
    @CsvSource(value = { "-10, -4", // Attribute score is below minimum, testing max()
                         "0, -4",   // Minimum normal attribute score
                         "1, -2",   // Attribute score of 1
                         "2, -1",   // Attribute score of 2
                         "3, -1",   // Attribute score of 3
                         "4, 0",    // Attribute score of 4
                         "5, 0",    // Attribute score of 5
                         "6, 0",    // Attribute score of 6
                         "7, 1",    // Attribute score of 7
                         "8, 1",    // Attribute score of 8
                         "9, 1",    // Attribute score of 9
                         "10, 2",   // Maximum normal attribute score
                         "99, 5"    // High attribute score
    })
    void testGetIndividualAttributeModifier(int attributeScore, int expectedModifier) {
        assertEquals(expectedModifier,
              getIndividualAttributeModifier(attributeScore),
              "Attribute Score: " + attributeScore);
    }
}
