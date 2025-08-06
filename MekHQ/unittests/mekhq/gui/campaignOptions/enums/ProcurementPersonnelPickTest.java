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
package mekhq.gui.campaignOptions.enums;

import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.LOGISTICS;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.NONE;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProcurementPersonnelPickTest {
    @Test
    public void testFromString_Validpick() {
        ProcurementPersonnelPick pick = ProcurementPersonnelPick.fromString(LOGISTICS.name());
        assertEquals(LOGISTICS, pick);
    }

    @Test
    public void testFromString_Invalidpick() {
        ProcurementPersonnelPick pick = ProcurementPersonnelPick.fromString("INVALID_pick");

        assertEquals(NONE, pick);
    }

    @Test
    public void testFromString_Nullpick() {
        ProcurementPersonnelPick pick = ProcurementPersonnelPick.fromString(null);

        assertEquals(NONE, pick);
    }

    @Test
    public void testFromString_EmptyString() {
        ProcurementPersonnelPick pick = ProcurementPersonnelPick.fromString("");

        assertEquals(NONE, pick);
    }

    @Test
    public void testFromString_FromOrdinal() {
        ProcurementPersonnelPick pick = ProcurementPersonnelPick.fromString(LOGISTICS.ordinal() + "");

        assertEquals(LOGISTICS, pick);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (ProcurementPersonnelPick pick : ProcurementPersonnelPick.values()) {
            String label = pick.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetDescription_notInvalid() {
        for (ProcurementPersonnelPick pick : ProcurementPersonnelPick.values()) {
            String label = pick.getDescription();
            assertTrue(isResourceKeyValid(label));
        }
    }
}
