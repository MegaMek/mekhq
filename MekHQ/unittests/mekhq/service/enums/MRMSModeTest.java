/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.service.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MRMSModeTest {
    //region Variable Declarations
    private static final MRMSMode[] modes = MRMSMode.values();
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsUnits() {
        for (final MRMSMode mrmsMode : modes) {
            if (mrmsMode == MRMSMode.UNITS) {
                assertTrue(mrmsMode.isUnits());
            } else {
                assertFalse(mrmsMode.isUnits());
            }
        }
    }
    @Test
    public void testIsWarehouse() {
        for (final MRMSMode mrmsMode : modes) {
            if (mrmsMode == MRMSMode.WAREHOUSE) {
                assertTrue(mrmsMode.isWarehouse());
            } else {
                assertFalse(mrmsMode.isWarehouse());
            }
        }
    }
    //endregion Boolean Comparison Methods
}
