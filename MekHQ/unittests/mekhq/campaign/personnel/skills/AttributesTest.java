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

import static java.lang.Math.min;
import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_EDGE_SCORE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.BODY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.CHARISMA;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.INTELLIGENCE;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.REFLEXES;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.STRENGTH;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.WILLPOWER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import org.junit.jupiter.api.Test;

public class AttributesTest {
    @Test
    public void testDefaultConstructor() {
        Attributes attributes = new Attributes();
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(STRENGTH));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(BODY));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(REFLEXES));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(DEXTERITY));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(INTELLIGENCE));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(WILLPOWER));
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(CHARISMA));
    }

    @Test
    public void testSetStrength() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(STRENGTH);

        attributes.setAttributeScore(phenotype, options, STRENGTH, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(attributeCap, attributes.getBaseAttributeScore(STRENGTH));

        attributes.setAttributeScore(phenotype, options, STRENGTH, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(STRENGTH));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, STRENGTH, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(STRENGTH));
        }
    }

    @Test
    public void testSetBody() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(BODY);

        attributes.setAttributeScore(phenotype, options, BODY, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(attributeCap, attributes.getBaseAttributeScore(BODY));

        attributes.setAttributeScore(phenotype, options, BODY, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(BODY));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, BODY, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(BODY));
        }
    }

    @Test
    public void testSetReflexes() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(REFLEXES);

        attributes.setAttributeScore(phenotype, options, REFLEXES, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(attributeCap, attributes.getBaseAttributeScore(REFLEXES));

        attributes.setAttributeScore(phenotype, options, REFLEXES, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(REFLEXES));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, REFLEXES, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(REFLEXES));
        }
    }

    @Test
    public void testSetDexterity() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(DEXTERITY);

        attributes.setAttributeScore(phenotype, options, DEXTERITY, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(attributeCap, attributes.getBaseAttributeScore(DEXTERITY));

        attributes.setAttributeScore(phenotype, options, DEXTERITY, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(DEXTERITY));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, DEXTERITY, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(DEXTERITY));
        }
    }

    @Test
    public void testSetIntelligence() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(INTELLIGENCE);

        attributes.setAttributeScore(phenotype, options, INTELLIGENCE, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(attributeCap, attributes.getBaseAttributeScore(INTELLIGENCE));

        attributes.setAttributeScore(phenotype, options, INTELLIGENCE, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(INTELLIGENCE));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, INTELLIGENCE, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(INTELLIGENCE));
        }
    }

    @Test
    public void testSetWillpower() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        final int attributeCap = phenotype.getAttributeCap(WILLPOWER);

        attributes.setAttributeScore(phenotype, options, WILLPOWER, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(phenotype.getAttributeCap(WILLPOWER), attributes.getBaseAttributeScore(WILLPOWER));

        attributes.setAttributeScore(phenotype, options, WILLPOWER, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(WILLPOWER));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, WILLPOWER, i);
            int expected = min(i, attributeCap);
            assertEquals(expected, attributes.getBaseAttributeScore(WILLPOWER));
        }
    }

    @Test
    public void testSetCharisma() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        attributes.setAttributeScore(phenotype, options, CHARISMA, MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(phenotype.getAttributeCap(CHARISMA), attributes.getBaseAttributeScore(CHARISMA));

        attributes.setAttributeScore(phenotype, options, CHARISMA, MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(CHARISMA));

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setAttributeScore(phenotype, options, CHARISMA, i);
            assertEquals(min(i, phenotype.getAttributeCap(CHARISMA)), attributes.getBaseAttributeScore(CHARISMA));
        }
    }

    @Test
    public void testChangeAllAttributes_BelowMinimum() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        attributes.changeAllAttributes(phenotype, options, -999);

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            if (attribute != SkillAttribute.EDGE) {
                assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBaseAttributeScore(attribute));
            } else {
                assertEquals(MINIMUM_EDGE_SCORE, attributes.getBaseAttributeScore(attribute));
            }
        }
    }

    @Test
    public void testChangeAllAttributes_AboveMaximum() {
        final Attributes attributes = new Attributes();
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        attributes.changeAllAttributes(phenotype, options, 999);

        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            assertEquals(phenotype.getAttributeCap(attribute), attributes.getBaseAttributeScore(attribute));
        }
    }

    @Test
    public void testChangeAllAttributes_AllPossibleValues() {
        final Phenotype phenotype = Phenotype.GENERAL;
        final PersonnelOptions options = new PersonnelOptions();

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            // reset attributes
            Attributes attributes = new Attributes(MINIMUM_ATTRIBUTE_SCORE);

            attributes.changeAllAttributes(phenotype, options, i);

            for (SkillAttribute attribute : SkillAttribute.values()) {
                if (attribute.isNone()) {
                    continue;
                }

                int newValue = attributes.getBaseAttributeScore(attribute);
                int expected = MINIMUM_ATTRIBUTE_SCORE + i;
                // Account for attribute caps
                expected = min(expected, phenotype.getAttributeCap(attribute));
                assertEquals(expected, newValue);
            }
        }
    }
}
