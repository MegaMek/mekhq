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
import org.junit.jupiter.api.Test;

import static mekhq.campaign.randomEvents.personalities.enums.Social.FRIENDLY;
import static mekhq.campaign.randomEvents.personalities.enums.Social.MAXIMUM_VARIATIONS;
import static mekhq.campaign.randomEvents.personalities.enums.Social.NONE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class SocialTest {
    @Test
    public void testFromString_ValidStatus() {
        Social status = Social.fromString(FRIENDLY.name());
        assertEquals(FRIENDLY, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        Social status = Social.fromString("INVALID_STATUS");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        Social status = Social.fromString(null);

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        Social status = Social.fromString("");

        assertEquals(NONE, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Social status : Social.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        Campaign campaign = mock(Campaign.class);

        Person person = new Person(campaign);

        for (Social trait : Social.values()) {
            for (int i = 0; i < MAXIMUM_VARIATIONS; i++) {
                person.setSocialDescriptionIndex(i);
                String description = trait.getDescription(person);
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_InvalidDescriptionIndex() {
        Campaign campaign = mock(Campaign.class);

        Person person = new Person(campaign);
        person.setSocialDescriptionIndex(MAXIMUM_VARIATIONS);

        String description = NONE.getDescription(person);
        assertTrue(isResourceKeyValid(description));
    }
}
