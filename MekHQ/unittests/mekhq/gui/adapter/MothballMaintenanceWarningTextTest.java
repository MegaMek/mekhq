/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Picking a tech who already looks after other units to mothball a unit warns the player. Maintenance comes first
 * each day, so the warning must say the units are still maintained and only the mothballing slows down.
 */
class MothballMaintenanceWarningTextTest {
    private static final String BUNDLE = "mekhq.resources.GUI";
    private static final String TEXT_KEY = "mothballUnit.maintenanceFirst.text";

    @Test
    void titleExists() {
        String title = getFormattedTextAt(BUNDLE, "mothballUnit.maintenanceFirst.title");

        assertFalse(title.startsWith("!"), "mothballUnit.maintenanceFirst.title is missing from the bundle");
    }

    @Test
    void oneUnitReadsAsSingular() {
        String text = getFormattedTextAt(BUNDLE, TEXT_KEY, "Jane Doe", 1);

        assertFalse(text.contains("{"), text);
        assertTrue(text.contains("Jane Doe will keep maintaining 1 assigned unit first"), text);
    }

    @Test
    void severalUnitsReadAsPlural() {
        String text = getFormattedTextAt(BUNDLE, TEXT_KEY, "Jane Doe", 4);

        assertFalse(text.contains("{"), text);
        assertTrue(text.contains("Jane Doe will keep maintaining 4 assigned units first"), text);
    }

    @Test
    void doesNotClaimTheUnitsGoUnmaintained() {
        String text = getFormattedTextAt(BUNDLE, TEXT_KEY, "Jane Doe", 2);

        assertFalse(text.contains("will not be able to perform maintenance"), text);
    }
}
