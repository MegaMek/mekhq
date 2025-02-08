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
package mekhq.campaign.enums;

import org.junit.jupiter.api.Test;

import static mekhq.campaign.enums.Glossary.PRISONER_CAPACITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GlossaryTest {
    @Test
    public void testParseFromString_ValidStatus() {
        Glossary status = Glossary.valueOf("PRISONER_CAPACITY");
        assertEquals(PRISONER_CAPACITY, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Glossary status : Glossary.values()) {
            String label = status.getTitle();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetTitleExtension_notInvalid() {
        for (Glossary status : Glossary.values()) {
            String titleExtension = status.getDescription();
            assertTrue(isResourceKeyValid(titleExtension));
        }
    }

    /**
     * Checks if the given text is valid. A valid string does not start or end with an exclamation
     * mark ('!').
     *
     * <p>If {@link mekhq.utilities.MHQInternationalization} fails to fetch a valid return it
     * returns the key between two {@code !}. So by checking the returned string doesn't begin and
     * end with that punctuation, we can easily verify that all statuses have been provided results
     * for the keys we're using.</p>
     *
     * @param text The text to validate.
     * @return true if the text is valid (does not start or end with an '!');
     *         false otherwise.
     */
    public static boolean isResourceKeyValid(String text) {
        return !text.startsWith("!") && !text.endsWith("!");
    }
}
