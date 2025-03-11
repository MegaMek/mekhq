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

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

import static mekhq.campaign.randomEvents.personalities.enums.Intelligence.AVERAGE;
import static mekhq.campaign.randomEvents.personalities.enums.Intelligence.MAXIMUM_VARIATIONS;
import static mekhq.campaign.randomEvents.personalities.enums.Intelligence.OBTUSE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class IntelligenceTest {
    @Test
    public void testFromString_ValidStatus() {
        Intelligence status = Intelligence.fromString(OBTUSE.name());
        assertEquals(OBTUSE, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        Intelligence status = Intelligence.fromString("INVALID_STATUS");

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        Intelligence status = Intelligence.fromString(null);

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        Intelligence status = Intelligence.fromString("");

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Intelligence status : Intelligence.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        Campaign campaign = mock(Campaign.class);
        Person person = new Person(campaign);

        for (Intelligence trait : Intelligence.values()) {
            for (int i = 0; i < MAXIMUM_VARIATIONS; i++) {
                person.setIntelligenceDescriptionIndex(i);
                String description = trait.getDescription(i, Gender.MALE, "Barry");
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_InvalidDescriptionIndex() {
        String description = AVERAGE.getDescription(MAXIMUM_VARIATIONS, Gender.MALE, "Barry");
        assertTrue(isResourceKeyValid(description));
    }
}
