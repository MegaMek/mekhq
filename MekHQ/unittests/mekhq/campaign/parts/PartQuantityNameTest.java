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
package mekhq.campaign.parts;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Quantities in procurement reports are not pluralised by adding an "s", which gave "Vehicle Sensorss" (issue #4806).
 */
class PartQuantityNameTest {
    private static final String PARTS_BUNDLE = "mekhq.resources.Parts";

    @Test
    void oneItemKeepsItsName() {
        assertEquals("1 Vehicle Sensors", Part.formatQuantityName(1, "Vehicle Sensors"));
    }

    @Test
    void severalItemsAreCountedWithoutAddingAnS() {
        assertEquals("2 x Vehicle Sensors", Part.formatQuantityName(2, "Vehicle Sensors"));
        assertEquals("3 x Medium Laser", Part.formatQuantityName(3, "Medium Laser"));
    }

    @Test
    void procurementReportAgreesWithTheQuantity() {
        assertEquals("1 Medium Laser has been added to the procurement list.",
              getFormattedTextAt(PARTS_BUNDLE, "Part.shoppingListReport", "1 Medium Laser", 1));
        assertEquals("2 x Medium Laser have been added to the procurement list.",
              getFormattedTextAt(PARTS_BUNDLE, "Part.shoppingListReport", "2 x Medium Laser", 2));
    }

    @Test
    void arrivalReportAgreesWithTheQuantity() {
        assertEquals("1 Medium Laser has arrived", getFormattedTextAt(PARTS_BUNDLE, "Part.arrivalReport",
              "1 Medium Laser", 1));
        assertEquals("2 x Medium Laser have arrived", getFormattedTextAt(PARTS_BUNDLE, "Part.arrivalReport",
              "2 x Medium Laser", 2));
    }
}
