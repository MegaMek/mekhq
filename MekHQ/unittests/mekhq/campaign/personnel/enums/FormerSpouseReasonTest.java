/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

class FormerSpouseReasonTest {
    // region Variable Declarations
    private static final FormerSpouseReason[] reasons = FormerSpouseReason.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Boolean Comparison Methods
    @Test
    void testIsWidowed() {
        for (final FormerSpouseReason formerSpouseReason : reasons) {
            if (formerSpouseReason == FormerSpouseReason.WIDOWED) {
                assertTrue(formerSpouseReason.isWidowed());
            } else {
                assertFalse(formerSpouseReason.isWidowed());
            }
        }
    }

    @Test
    void testIsDivorce() {
        for (final FormerSpouseReason formerSpouseReason : reasons) {
            if (formerSpouseReason == FormerSpouseReason.DIVORCE) {
                assertTrue(formerSpouseReason.isDivorce());
            } else {
                assertFalse(formerSpouseReason.isDivorce());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(FormerSpouseReason.DIVORCE, FormerSpouseReason.parseFromString("DIVORCE"));
        assertEquals(FormerSpouseReason.WIDOWED, FormerSpouseReason.parseFromString("WIDOWED"));

        // Error Case
        assertEquals(FormerSpouseReason.WIDOWED, FormerSpouseReason.parseFromString("2"));
        assertEquals(FormerSpouseReason.WIDOWED, FormerSpouseReason.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("FormerSpouseReason.DIVORCE.text"), FormerSpouseReason.DIVORCE.toString());
        assertEquals(resources.getString("FormerSpouseReason.WIDOWED.text"), FormerSpouseReason.WIDOWED.toString());
    }
}
