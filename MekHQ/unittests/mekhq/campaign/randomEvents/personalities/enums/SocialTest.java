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
import static mekhq.campaign.randomEvents.personalities.enums.Social.NONE;
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

        for (Social status : Social.values()) {
            String titleExtension = status.getDescription(person);
            assertTrue(isResourceKeyValid(titleExtension));
        }
    }

    /**
     * Checks if the given text is a valid title extension. A valid title extension
     * does not start or end with an exclamation mark ('!').
     *
     * <p>If {@link mekhq.utilities.MHQInternationalization} fails to fetch a valid return it
     * returns the key between two {@code !}. So by checking the returned string doesn't begin and
     * end with that punctuation, we can easily verify that all statuses have been provided results
     * for the keys we're using.</p>
     *
     * @param text The text to validate as a title extension.
     * @return true if the text is valid (does not start or end with an '!');
     *         false otherwise.
     */
    public static boolean isResourceKeyValid(String text) {
        return !text.startsWith("!") && !text.endsWith("!");
    }
}
