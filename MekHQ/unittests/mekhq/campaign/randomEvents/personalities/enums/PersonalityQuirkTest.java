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

import megamek.common.enums.Gender;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.Test;

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_HR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.CHRONIC_LATENESS;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.MAXIMUM_VARIATIONS;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.NONE;
import static mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk.OBJECT;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testFromString_Ordinal() {
        PersonalityQuirk status = PersonalityQuirk.fromString(CHRONIC_LATENESS.ordinal() + "");

        assertEquals(CHRONIC_LATENESS, status);
    }

    @Test
    public void testGetDescription_notInvalid_Combatant() {
        Faction originFaction = Factions.getInstance().getFaction("MERC");
        for (PersonalityQuirk trait : PersonalityQuirk.values()) {
            for (int i = 0; i < 3; i++) {
                String description = trait.getDescription(MEKWARRIOR, i, Gender.MALE, originFaction, "Barry");
                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_notInvalid_Support() {
        Faction originFaction = Factions.getInstance().getFaction("MERC");
        for (PersonalityQuirk trait : PersonalityQuirk.values()) {
            for (int i = 0; i < 3; i++) {
                String description = trait.getDescription(ADMINISTRATOR_HR, i, Gender.MALE, originFaction,
                    "Barry");

                assertTrue(isResourceKeyValid(description));
            }
        }
    }

    @Test
    public void testGetDescription_InvalidDescriptionIndex() {
        Faction originFaction = Factions.getInstance().getFaction("MERC");

        String description = NONE.getDescription(MEKWARRIOR, MAXIMUM_VARIATIONS, Gender.MALE, originFaction,
            "Barry");

        assertTrue(isResourceKeyValid(description));
    }
}
