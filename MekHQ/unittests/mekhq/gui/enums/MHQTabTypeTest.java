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
 */
package mekhq.gui.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

public class MHQTabTypeTest {
    //region Variable Declarations
    private static final MHQTabType[] types = MHQTabType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetMnemonic() {
        assertEquals(KeyEvent.VK_O, MHQTabType.COMMAND_CENTER.getMnemonic());
        assertEquals(KeyEvent.VK_S, MHQTabType.INTERSTELLAR_MAP.getMnemonic());
        assertEquals(KeyEvent.VK_H, MHQTabType.HANGAR.getMnemonic());
        assertEquals(KeyEvent.VK_L, MHQTabType.MEK_LAB.getMnemonic());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsCommandCenter() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.COMMAND_CENTER) {
                assertTrue(mhqTabType.isCommandCenter());
            } else {
                assertFalse(mhqTabType.isCommandCenter());
            }
        }
    }

    @Test
    public void testIsTOE() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.TOE) {
                assertTrue(mhqTabType.isTOE());
            } else {
                assertFalse(mhqTabType.isTOE());
            }
        }
    }

    @Test
    public void testIsBriefingRoom() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.BRIEFING_ROOM) {
                assertTrue(mhqTabType.isBriefingRoom());
            } else {
                assertFalse(mhqTabType.isBriefingRoom());
            }
        }
    }

    @Test
    public void testIsInterstellarMap() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.INTERSTELLAR_MAP) {
                assertTrue(mhqTabType.isInterstellarMap());
            } else {
                assertFalse(mhqTabType.isInterstellarMap());
            }
        }
    }

    @Test
    public void testIsPersonnel() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.PERSONNEL) {
                assertTrue(mhqTabType.isPersonnel());
            } else {
                assertFalse(mhqTabType.isPersonnel());
            }
        }
    }

    @Test
    public void testIsHangar() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.HANGAR) {
                assertTrue(mhqTabType.isHangar());
            } else {
                assertFalse(mhqTabType.isHangar());
            }
        }
    }

    @Test
    public void testIsWarehouse() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.WAREHOUSE) {
                assertTrue(mhqTabType.isWarehouse());
            } else {
                assertFalse(mhqTabType.isWarehouse());
            }
        }
    }

    @Test
    public void testIsRepairBay() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.REPAIR_BAY) {
                assertTrue(mhqTabType.isRepairBay());
            } else {
                assertFalse(mhqTabType.isRepairBay());
            }
        }
    }

    @Test
    public void testIsInfirmary() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.INFIRMARY) {
                assertTrue(mhqTabType.isInfirmary());
            } else {
                assertFalse(mhqTabType.isInfirmary());
            }
        }
    }

    @Test
    public void testIsFinances() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.FINANCES) {
                assertTrue(mhqTabType.isFinances());
            } else {
                assertFalse(mhqTabType.isFinances());
            }
        }
    }

    @Test
    public void testIsMekLab() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.MEK_LAB) {
                assertTrue(mhqTabType.isMekLab());
            } else {
                assertFalse(mhqTabType.isMekLab());
            }
        }
    }

    @Test
    public void testIsStratCon() {
        for (final MHQTabType mhqTabType : types) {
            if (mhqTabType == MHQTabType.STRAT_CON) {
                assertTrue(mhqTabType.isStratCon());
            } else {
                assertFalse(mhqTabType.isStratCon());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("MHQTabType.COMMAND_CENTER.text"), MHQTabType.COMMAND_CENTER.toString());
        assertEquals(resources.getString("MHQTabType.WAREHOUSE.text"), MHQTabType.WAREHOUSE.toString());
        assertEquals(resources.getString("MHQTabType.STRAT_CON.text"), MHQTabType.STRAT_CON.toString());
    }
}
