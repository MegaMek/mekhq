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

import static mekhq.campaign.randomEvents.personalities.enums.Ambition.ASPIRING;
import static mekhq.campaign.randomEvents.personalities.enums.Ambition.CONNIVING;
import static mekhq.campaign.randomEvents.personalities.enums.Ambition.MAXIMUM_VARIATIONS;
import static mekhq.campaign.randomEvents.personalities.enums.Ambition.NONE;
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

public class AmbitionTest {
    @Test
    public void testFromString_ValidStatus() {
        Ambition status = Ambition.fromString(ASPIRING.name());
        assertEquals(ASPIRING, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        Ambition status = Ambition.fromString("INVALID_STATUS");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        Ambition status = Ambition.fromString(null);

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        Ambition status = Ambition.fromString("");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_FromOrdinal() {
        Ambition status = Ambition.fromString(CONNIVING.ordinal() + "");

        assertEquals(CONNIVING, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (Ambition status : Ambition.values()) {
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

        for (Ambition trait : Ambition.values()) {
            for (int i = 0; i < MAXIMUM_VARIATIONS; i++) {
                person.setAmbitionDescriptionIndex(i);
                String description = trait.getDescription(i, Gender.MALE, "Barry");
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_InvalidDescriptionIndex() {
        String description = NONE.getDescription(MAXIMUM_VARIATIONS, Gender.MALE, "Barry");
        assertTrue(isResourceKeyValid(description));
    }

    @Test
    public void testGetRoninMessage_notInvalid() {
        for (Ambition trait : Ambition.values()) {
            for (int i = 0; i < Ambition.MAXIMUM_VARIATIONS; i++) {
                String description = trait.getRoninMessage("Commander");
                assertTrue(isResourceKeyValid(description));
            }
        }
    }
}
