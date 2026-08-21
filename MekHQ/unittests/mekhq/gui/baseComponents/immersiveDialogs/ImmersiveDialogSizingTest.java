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
package mekhq.gui.baseComponents.immersiveDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImmersiveDialogSizingTest {
    @Test
    void naturalContentFitsWithoutScrolling() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(600, 320, 120, 900);

        assertEquals(600, result.dialogHeight());
        assertEquals(320, result.viewportHeight());
        assertFalse(result.requiresScrolling());
    }

    @Test
    void oversizedContentShrinksOnlyViewportToScreenCap() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(1_100, 700, 120, 1_000);

        assertEquals(900, result.dialogHeight());
        assertEquals(500, result.viewportHeight());
        assertTrue(result.requiresScrolling());
    }

    @Test
    void viewportNeverShrinksBelowReadingMinimum() {
        ImmersiveDialogSizing.SizingResult result = ImmersiveDialogSizing.calculate(1_500, 400, 120, 800);

        assertEquals(720, result.dialogHeight());
        assertEquals(120, result.viewportHeight());
        assertTrue(result.requiresScrolling());
    }
}
