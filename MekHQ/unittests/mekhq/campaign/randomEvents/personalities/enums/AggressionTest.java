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

import static mekhq.campaign.randomEvents.personalities.enums.Aggression.DECISIVE;
import static mekhq.campaign.randomEvents.personalities.enums.Aggression.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class AggressionTest {
    @Test
    public void testFromString_ValidStatus() {
        Aggression status = Aggression.fromString(DECISIVE.name());
        assertEquals(DECISIVE, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        Aggression status = Aggression.fromString("INVALID_STATUS");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        Aggression status = Aggression.fromString(null);

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        Aggression status = Aggression.fromString("");

        assertEquals(NONE, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Aggression status : Aggression.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        Campaign campaign = mock(Campaign.class);
        Person person = new Person(campaign);

        for (Aggression status : Aggression.values()) {
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
