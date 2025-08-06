/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * This test class verifies the functionality of the `hasAdminStrain` method in the `AdminStrainNagLogic` class. The
 * method checks if the given admin strain value is positive.
 */
public class HRStrainNagLogicTest {
    @Test
    void testHasHRStrainWithPositiveValue() {
        int adminStrain = 10;
        boolean result = HRStrainNagLogic.hasHRStrain(adminStrain);
        assertTrue(result, "Admin strain should be positive and return true.");
    }

    @Test
    void testHasHRStrainWithZeroValue() {
        int adminStrain = 0;
        boolean result = HRStrainNagLogic.hasHRStrain(adminStrain);
        assertFalse(result, "Admin strain of zero should return false.");
    }

    @Test
    void testHasHRStrainWithNegativeValue() {
        int adminStrain = -5;
        boolean result = HRStrainNagLogic.hasHRStrain(adminStrain);
        assertFalse(result, "Admin strain should not be negative; it should return false.");
    }
}
