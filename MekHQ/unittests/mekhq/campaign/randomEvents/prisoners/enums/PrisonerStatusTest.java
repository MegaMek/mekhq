/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
package mekhq.campaign.randomEvents.prisoners.enums;

import org.junit.jupiter.api.Test;

import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.FREE;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
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
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetTitleExtension_notInvalid() {
        for (PrisonerStatus status : PrisonerStatus.values()) {
            String titleExtension = status.getTitleExtension();
            assertTrue(isResourceKeyValid(titleExtension));
        }
    }
}
