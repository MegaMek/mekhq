/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

class ManeiDominiClassTest {
    // region Variable Declarations
    private static final ManeiDominiClass[] classes = ManeiDominiClass.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Boolean Comparison Methods
    @Test
    void testIsNone() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.NONE) {
                assertTrue(maneiDominiClass.isNone());
            } else {
                assertFalse(maneiDominiClass.isNone());
            }
        }
    }

    @Test
    void testIsGhost() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.GHOST) {
                assertTrue(maneiDominiClass.isGhost());
            } else {
                assertFalse(maneiDominiClass.isGhost());
            }
        }
    }

    @Test
    void testIsWraith() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.WRAITH) {
                assertTrue(maneiDominiClass.isWraith());
            } else {
                assertFalse(maneiDominiClass.isWraith());
            }
        }
    }

    @Test
    void testIsBanshee() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.BANSHEE) {
                assertTrue(maneiDominiClass.isBanshee());
            } else {
                assertFalse(maneiDominiClass.isBanshee());
            }
        }
    }

    @Test
    void testIsZombie() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.ZOMBIE) {
                assertTrue(maneiDominiClass.isZombie());
            } else {
                assertFalse(maneiDominiClass.isZombie());
            }
        }
    }

    @Test
    void testIsPhantom() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.PHANTOM) {
                assertTrue(maneiDominiClass.isPhantom());
            } else {
                assertFalse(maneiDominiClass.isPhantom());
            }
        }
    }

    @Test
    void testIsSpectre() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.SPECTER) {
                assertTrue(maneiDominiClass.isSpecter());
            } else {
                assertFalse(maneiDominiClass.isSpecter());
            }
        }
    }

    @Test
    void testIsPoltergeist() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.POLTERGEIST) {
                assertTrue(maneiDominiClass.isPoltergeist());
            } else {
                assertFalse(maneiDominiClass.isPoltergeist());
            }
        }
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    /**
     * Testing to ensure the enum is properly parsed from a given String, dependent on whether it is parsing from
     * ManeiDominiClass.name(), the ordinal (formerly magic numbers), or a failure condition
     */
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("NONE"));
        assertEquals(ManeiDominiClass.GHOST, ManeiDominiClass.parseFromString("GHOST"));

        // Error Case
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("10"));
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiClass.NONE.text"), ManeiDominiClass.NONE.toString());
        assertEquals(resources.getString("ManeiDominiClass.PHANTOM.text"), ManeiDominiClass.PHANTOM.toString());
        assertEquals(resources.getString("ManeiDominiClass.POLTERGEIST.text"), ManeiDominiClass.POLTERGEIST.toString());
    }
}
