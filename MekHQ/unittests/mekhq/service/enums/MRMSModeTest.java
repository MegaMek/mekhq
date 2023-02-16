/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
