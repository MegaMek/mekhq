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
package mekhq.campaign.randomEvents.prisoners.enums;

import org.junit.jupiter.api.Test;

import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.FREE;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrisonerStatusTest {
    @Test
    public void testParseFromString_ValidStatus() {
        PrisonerStatus status = PrisonerStatus.parseFromString("PRISONER");
        assertEquals(PRISONER, status);
    }

    @Test
    public void testParseFromString_InvalidStatus() {
        PrisonerStatus status = PrisonerStatus.parseFromString("INVALID_STATUS");

        assertEquals(FREE, status);
    }

    @Test
    public void testParseFromString_NullStatus() {
        PrisonerStatus status = PrisonerStatus.parseFromString(null);

        assertEquals(FREE, status);
    }

    @Test
    public void testParseFromString_EmptyString() {
        PrisonerStatus status = PrisonerStatus.parseFromString("");

        assertEquals(FREE, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (PrisonerStatus status : PrisonerStatus.values()) {
            String label = status.getLabel();
            assertTrue(isTitleExtensionValid(label));
        }
    }

    @Test
    public void testGetTitleExtension_notInvalid() {
        for (PrisonerStatus status : PrisonerStatus.values()) {
            String titleExtension = status.getTitleExtension();
            assertTrue(isTitleExtensionValid(titleExtension));
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
    public static boolean isTitleExtensionValid(String text) {
        return !text.startsWith("!") && !text.endsWith("!");
    }
}
