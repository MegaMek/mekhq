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
package mekhq.gui.baseComponents.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** The date field's reading of what was typed, tested without building the field. */
class HudDateFieldTest {
    @Test
    void readsIsoDatesIgnoringSurroundingSpaces() {
        assertEquals(LocalDate.of(3025, 6, 1), HudDateField.parse("3025-06-01"));
        assertEquals(LocalDate.of(3025, 6, 1), HudDateField.parse(" 3025-06-01 "));
    }

    @Test
    void anythingElseIsNoDate() {
        assertNull(HudDateField.parse(""));
        assertNull(HudDateField.parse("   "));
        assertNull(HudDateField.parse("June 1st"));
        assertNull(HudDateField.parse("3025-13-01"));
        assertNull(HudDateField.parse("3025-6-1"));
    }
}
