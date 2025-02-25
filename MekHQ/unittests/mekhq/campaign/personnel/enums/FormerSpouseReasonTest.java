/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

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
