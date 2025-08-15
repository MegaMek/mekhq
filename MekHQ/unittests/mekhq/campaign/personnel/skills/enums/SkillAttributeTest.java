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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills.enums;

import static mekhq.campaign.personnel.skills.enums.SkillAttribute.DEXTERITY;
import static mekhq.campaign.personnel.skills.enums.SkillAttribute.NONE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SkillAttributeTest {
    @Test
    public void testFromString_ValidStatus() {
        SkillAttribute status = SkillAttribute.fromString(DEXTERITY.name());
        assertEquals(DEXTERITY, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        SkillAttribute status = SkillAttribute.fromString("INVALID_STATUS");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        SkillAttribute status = SkillAttribute.fromString(null);

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        SkillAttribute status = SkillAttribute.fromString("");

        assertEquals(NONE, status);
    }

    @Test
    public void testFromString_FromOrdinal() {
        SkillAttribute status = SkillAttribute.fromString(DEXTERITY.ordinal() + "");

        assertEquals(DEXTERITY, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (SkillAttribute attribute : SkillAttribute.values()) {
            String label = attribute.getLabel();

            String results = "";
            if (!isResourceKeyValid(label)) {
                results = "Found Error in: " + label;
            }

            assertEquals("", results);
        }
    }

    @Test
    public void testGetShortName_notInvalid() {
        for (SkillAttribute attribute : SkillAttribute.values()) {
            String label = attribute.getShortName();

            String results = "";
            if (!isResourceKeyValid(label)) {
                results = "Found Error in: " + label;
            }

            assertEquals("", results);
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        for (SkillAttribute attribute : SkillAttribute.values()) {
            String label = attribute.getDescription();

            String results = "";
            if (!isResourceKeyValid(label)) {
                results = "Found Error in: " + label;
            }

            assertEquals("", results);
        }
    }
}
