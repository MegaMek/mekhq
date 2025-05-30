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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SkillTypeTest {

    static Stream<String> allSkillNames() {
        return Stream.of(SkillType.getSkillList());
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetTarget_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        int target = skillType.getTarget();

        // Assert
        assertTrue(target >= 2 && target <= 12,
              "Invalid target: " + target + " for skill: " + skillType.getName() + " must be >= 2 and <= 12");
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetSubType_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        SkillSubType subType = skillType.getSubType();

        // Assert
        assertNotSame(SkillSubType.NONE,
              subType,
              "Invalid subType for skill: " + skillType.getName() + " cannot be " + "NONE");
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetFirstAttribute_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        SkillAttribute attribute = skillType.getFirstAttribute();

        // Assert
        assertNotSame(SkillAttribute.NONE,
              attribute,
              "Invalid first attribute for skill: " + skillType.getName() + " cannot be NONE");
        assertNotSame(null, attribute, "Invalid first attribute for skill: " + skillType.getName() + " cannot be null");
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetSecondAttribute_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        SkillAttribute attribute = skillType.getFirstAttribute();

        // Assert
        assertNotSame(null,
              attribute,
              "Invalid second attribute for skill: " + skillType.getName() + " cannot be null");
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetGreenLevel_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        int greenLevel = skillType.getGreenLevel();
        int regularLevel = skillType.getRegularLevel();
        int veteranLevel = skillType.getVeteranLevel();
        int eliteLevel = skillType.getEliteLevel();
        int heroicLevel = skillType.getHeroicLevel();
        int legendaryLevel = skillType.getLegendaryLevel();

        // Assert
        assertTrue(greenLevel > 0, "Invalid green level for skill: " + skillType.getName() + " cannot be < 1");

        assertTrue(regularLevel > greenLevel,
              "Invalid regular level for skill: " + skillType.getName() + " cannot be < green level");
        assertTrue(regularLevel < veteranLevel,
              "Invalid regular level for skill: " + skillType.getName() + " cannot be >= veteran level");

        assertTrue(veteranLevel < eliteLevel,
              "Invalid veteran level for skill: " + skillType.getName() + " cannot be >= elite level");

        assertTrue(eliteLevel < heroicLevel,
              "Invalid elite level for skill: " + skillType.getName() + " cannot be >= elite level");

        assertTrue(heroicLevel < legendaryLevel,
              "Invalid heroic level for skill: " + skillType.getName() + " cannot be >= elite level");

        assertTrue(eliteLevel < 11, "Invalid elite level for skill: " + skillType.getName() + " cannot be > 10");
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetCosts_isValid(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        Integer[] costs = skillType.getCosts();

        // Assert
        for (int level = 0; level < 10; level++) {
            assertEquals(11, costs.length, "Invalid costs for skill: " + skillType.getName() + " must be 11 elements");
        }
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetFlavorText_flavorTextExists(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        String flavorText = skillType.getFlavorText(false, false);

        // Assert
        assertTrue(isResourceKeyValid(flavorText), "Invalid resource key: " + skillType.getName());
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetFlavorText_tagsIncludedWhenRequested(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        String flavorText = skillType.getFlavorText(true, false);

        // Assert
        assertTrue(flavorText.contains("<html>"), "Did not include html opening tag: " + skillType.getName());
        assertTrue(flavorText.contains("</html>"), "Did not include html closing tag: " + skillType.getName());
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetFlavorText_allAttributesIncluded(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        String flavorText = skillType.getFlavorText(false, true);

        // Assert
        SkillAttribute firstAttribute = skillType.getFirstAttribute();
        assertNotSame(NONE, firstAttribute, "First Attribute is NONE for Skill: " + skillType.getName());
        if (firstAttribute != NONE) {
            assertTrue(flavorText.contains(firstAttribute.getLabel()),
                  "Did not include first Attribute: " +
                        firstAttribute.toString() +
                        " for Skill: " +
                        skillType.getName());
        }

        SkillAttribute secondAttribute = skillType.getSecondAttribute();
        if (secondAttribute != NONE) {
            assertTrue(flavorText.contains(secondAttribute.getLabel()),
                  "Did not include second Attribute: " +
                        secondAttribute.toString() +
                        " for Skill: " +
                        skillType.getName());
        }
    }

    @ParameterizedTest
    @MethodSource(value = "allSkillNames")
    void testGetFlavorText_containsBothAttributesAndHtmlTags(String skillName) {
        SkillType.initializeTypes();

        // Setup
        SkillType skillType = SkillType.getType(skillName);

        // Act
        String flavorText = skillType.getFlavorText(true, true);

        // Assert
        assertTrue(flavorText.contains("<html>"), "Did not include html opening tag: " + skillType.getName());
        assertTrue(flavorText.contains("</html>"), "Did not include html closing tag: " + skillType.getName());

        SkillAttribute firstAttribute = skillType.getFirstAttribute();
        assertNotSame(NONE, firstAttribute, "First Attribute is NONE for Skill: " + skillType.getName());
        if (firstAttribute != NONE) {
            assertTrue(flavorText.contains(firstAttribute.getLabel()),
                  "Did not include first Attribute: " +
                        firstAttribute.toString() +
                        " for Skill: " +
                        skillType.getName());
        }

        SkillAttribute secondAttribute = skillType.getSecondAttribute();
        if (secondAttribute != NONE) {
            assertTrue(flavorText.contains(secondAttribute.getLabel()),
                  "Did not include second Attribute: " +
                        secondAttribute.toString() +
                        " for Skill: " +
                        skillType.getName());
        }
    }
}
