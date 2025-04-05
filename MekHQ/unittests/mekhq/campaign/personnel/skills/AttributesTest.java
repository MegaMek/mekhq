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
 */
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AttributesTest {
    @Test
    public void testDefaultConstructor() {
        Attributes attributes = new Attributes();
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getStrength());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getBody());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getReflexes());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getDexterity());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getIntelligence());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getWillpower());
        assertEquals(DEFAULT_ATTRIBUTE_SCORE, attributes.getCharisma());
    }

    @Test
    public void testSetStrength() {
        Attributes attributes = new Attributes();

        attributes.setStrength(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getStrength());

        attributes.setStrength(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getStrength());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setStrength(i);
            assertEquals(i, attributes.getStrength());
        }
    }

    @Test
    public void testSetBody() {
        Attributes attributes = new Attributes();

        attributes.setBody(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getBody());

        attributes.setBody(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getBody());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setBody(i);
            assertEquals(i, attributes.getBody());
        }
    }

    @Test
    public void testSetReflexes() {
        Attributes attributes = new Attributes();

        attributes.setReflexes(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getReflexes());

        attributes.setReflexes(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getReflexes());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setReflexes(i);
            assertEquals(i, attributes.getReflexes());
        }
    }

    @Test
    public void testSetDexterity() {
        Attributes attributes = new Attributes();

        attributes.setDexterity(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getDexterity());

        attributes.setDexterity(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getDexterity());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setDexterity(i);
            assertEquals(i, attributes.getDexterity());
        }
    }

    @Test
    public void testSetIntelligence() {
        Attributes attributes = new Attributes();

        attributes.setIntelligence(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getIntelligence());

        attributes.setIntelligence(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getIntelligence());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setIntelligence(i);
            assertEquals(i, attributes.getIntelligence());
        }
    }

    @Test
    public void testSetWillpower() {
        Attributes attributes = new Attributes();

        attributes.setWillpower(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getWillpower());

        attributes.setWillpower(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getWillpower());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setWillpower(i);
            assertEquals(i, attributes.getWillpower());
        }
    }

    @Test
    public void testSetCharisma() {
        Attributes attributes = new Attributes();

        attributes.setCharisma(MAXIMUM_ATTRIBUTE_SCORE + 1);
        assertEquals(MAXIMUM_ATTRIBUTE_SCORE, attributes.getCharisma());

        attributes.setCharisma(MINIMUM_ATTRIBUTE_SCORE - 1);
        assertEquals(MINIMUM_ATTRIBUTE_SCORE, attributes.getCharisma());

        for (int i = MINIMUM_ATTRIBUTE_SCORE; i <= MAXIMUM_ATTRIBUTE_SCORE; i++) {
            attributes.setCharisma(i);
            assertEquals(i, attributes.getCharisma());
        }
    }

    @Test
    public void testChangeAllAttributes_BelowMinimum() {
        Attributes attributes = new Attributes();
        attributes.changeAllAttributes(-999);
        assertEqualsAllAttributes(MINIMUM_ATTRIBUTE_SCORE, attributes);
    }

    @Test
    public void testChangeAllAttributes_AboveMaximum() {
        Attributes attributes = new Attributes();
        attributes.changeAllAttributes(999);
        assertEqualsAllAttributes(MAXIMUM_ATTRIBUTE_SCORE, attributes);
    }

    @Test
    public void testChangeAllAttributes_AllPossibleValues() {
        int minimum = MINIMUM_ATTRIBUTE_SCORE - DEFAULT_ATTRIBUTE_SCORE;
        int maximum = MAXIMUM_ATTRIBUTE_SCORE - DEFAULT_ATTRIBUTE_SCORE;

        for (int i = minimum; i <= maximum; i++) {
            Attributes attributes = new Attributes();
            int expectation = DEFAULT_ATTRIBUTE_SCORE + i;

            attributes.changeAllAttributes(i);
            assertEqualsAllAttributes(expectation, attributes);
        }
    }

    /**
     * Asserts that all attributes of the given {@link Attributes} object are equal to the expected value.
     *
     * <p>This utility method performs assertions on the following attributes:</p>
     * <ul>
     *     <li>Strength</li>
     *     <li>Body</li>
     *     <li>Reflexes</li>
     *     <li>Dexterity</li>
     *     <li>Intelligence</li>
     *     <li>Willpower</li>
     *     <li>Charisma</li>
     * </ul>
     * <p>If any attribute does not match the expected value, an assertion error will be thrown.</p>
     *
     * @param expectation the expected value for all attributes.
     * @param attributes  the {@link Attributes} object whose attributes are being tested.
     *
     * @throws AssertionError if any attribute does not match the expected value.
     * @since 0.50.5
     */
    private static void assertEqualsAllAttributes(int expectation, Attributes attributes) {
        assertEquals(expectation, attributes.getStrength());
        assertEquals(expectation, attributes.getBody());
        assertEquals(expectation, attributes.getReflexes());
        assertEquals(expectation, attributes.getDexterity());
        assertEquals(expectation, attributes.getIntelligence());
        assertEquals(expectation, attributes.getWillpower());
        assertEquals(expectation, attributes.getCharisma());
    }
}
