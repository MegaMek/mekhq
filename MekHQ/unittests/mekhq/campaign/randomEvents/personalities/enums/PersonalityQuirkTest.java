/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.personalities.enums;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

import static mekhq.campaign.personnel.enums.PersonnelRole.MECHANIC;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.CLAUSTROPHOBIA;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.NONE;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.OBJECT;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class PersonalityQuirkTest {
    @Test
    public void testFromString_ValidStatus() {
        PersonalityQuirk status = PersonalityQuirk.fromString(OBJECT.name());
        assertEquals(OBJECT, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        PersonalityQuirk status = PersonalityQuirk.fromString("INVALID_STATUS");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        PersonalityQuirk status = PersonalityQuirk.fromString(null);

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        PersonalityQuirk status = PersonalityQuirk.fromString("");

        assertEquals(NONE, status);
    }

    @Test
    public void testGetDescription_notInvalid_Combatant() {
        Campaign campaign = mock(Campaign.class);

        Person person = new Person(campaign);
        person.setPrimaryRole(campaign, MEKWARRIOR);

        for (PersonalityQuirk trait : PersonalityQuirk.values()) {
            for (int i = 0; i < 3; i++) {
                person.setPersonalityQuirkDescriptionIndex(i);
                String description = trait.getDescription(person);
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_notInvalid_Support() {
        Campaign campaign = mock(Campaign.class);

        Person person = new Person(campaign);
        person.setPrimaryRole(campaign, MECHANIC);

        for (PersonalityQuirk trait : PersonalityQuirk.values()) {
            for (int i = 0; i < 3; i++) {
                person.setPersonalityQuirkDescriptionIndex(i);
                String description = trait.getDescription(person);
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_InvalidDescriptionIndex() {
        Campaign campaign = mock(Campaign.class);

        Person person = new Person(campaign);
        person.setPrimaryRole(campaign, MEKWARRIOR);
        person.setPersonalityQuirkDescriptionIndex(Integer.MAX_VALUE);

        String description = NONE.getDescription(person);
        assertFalse(isResourceKeyValid(description));
    }
}
