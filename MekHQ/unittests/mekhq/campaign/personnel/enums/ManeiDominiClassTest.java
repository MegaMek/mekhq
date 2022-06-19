/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManeiDominiClassTest {
    //region Variable Declarations
    private static final ManeiDominiClass[] classes = ManeiDominiClass.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.NONE) {
                assertTrue(maneiDominiClass.isNone());
            } else {
                assertFalse(maneiDominiClass.isNone());
            }
        }
    }

    @Test
    public void testIsGhost() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.GHOST) {
                assertTrue(maneiDominiClass.isGhost());
            } else {
                assertFalse(maneiDominiClass.isGhost());
            }
        }
    }

    @Test
    public void testIsWraith() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.WRAITH) {
                assertTrue(maneiDominiClass.isWraith());
            } else {
                assertFalse(maneiDominiClass.isWraith());
            }
        }
    }

    @Test
    public void testIsBanshee() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.BANSHEE) {
                assertTrue(maneiDominiClass.isBanshee());
            } else {
                assertFalse(maneiDominiClass.isBanshee());
            }
        }
    }

    @Test
    public void testIsZombie() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.ZOMBIE) {
                assertTrue(maneiDominiClass.isZombie());
            } else {
                assertFalse(maneiDominiClass.isZombie());
            }
        }
    }

    @Test
    public void testIsPhantom() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.PHANTOM) {
                assertTrue(maneiDominiClass.isPhantom());
            } else {
                assertFalse(maneiDominiClass.isPhantom());
            }
        }
    }

    @Test
    public void testIsSpectre() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.SPECTER) {
                assertTrue(maneiDominiClass.isSpecter());
            } else {
                assertFalse(maneiDominiClass.isSpecter());
            }
        }
    }

    @Test
    public void testIsPoltergeist() {
        for (final ManeiDominiClass maneiDominiClass : classes) {
            if (maneiDominiClass == ManeiDominiClass.POLTERGEIST) {
                assertTrue(maneiDominiClass.isPoltergeist());
            } else {
                assertFalse(maneiDominiClass.isPoltergeist());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * Testing to ensure the enum is properly parsed from a given String, dependent on whether it
     * is parsing from ManeiDominiClass.name(), the ordinal (formerly magic numbers), or a failure
     * condition
     */
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("NONE"));
        assertEquals(ManeiDominiClass.GHOST, ManeiDominiClass.parseFromString("GHOST"));

        // Legacy Parsing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("0"));
        assertEquals(ManeiDominiClass.BANSHEE, ManeiDominiClass.parseFromString("3"));
        assertEquals(ManeiDominiClass.POLTERGEIST, ManeiDominiClass.parseFromString("7"));

        // Error Case
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("10"));
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiClass.NONE.text"), ManeiDominiClass.NONE.toString());
        assertEquals(resources.getString("ManeiDominiClass.PHANTOM.text"), ManeiDominiClass.PHANTOM.toString());
        assertEquals(resources.getString("ManeiDominiClass.POLTERGEIST.text"), ManeiDominiClass.POLTERGEIST.toString());
    }
}
