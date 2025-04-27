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
package mekhq.campaign.randomEvents.personalities.enums;

import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.AVERAGE;
import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.MAXIMUM_VARIATIONS;
import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.OBTUSE;
import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.UNDER_PERFORMING;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

public class ReasoningTest {
    @Test
    public void testFromString_ValidStatus() {
        Reasoning status = Reasoning.fromString(OBTUSE.name());
        assertEquals(OBTUSE, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        Reasoning status = Reasoning.fromString("INVALID_STATUS");

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        Reasoning status = Reasoning.fromString(null);

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        Reasoning status = Reasoning.fromString("");

        assertEquals(AVERAGE, status);
    }

    @Test
    public void testFromString_FromOrdinal() {
        Reasoning status = Reasoning.fromString(UNDER_PERFORMING.ordinal() + "");

        assertEquals(UNDER_PERFORMING, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Reasoning status : Reasoning.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        Campaign campaign = mock(Campaign.class);
        Faction campaignFaction = mock(Faction.class);
        when(campaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        Person person = new Person(campaign);

        for (Reasoning trait : Reasoning.values()) {
            for (int i = 0; i < MAXIMUM_VARIATIONS; i++) {
                person.setReasoningDescriptionIndex(i);
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
