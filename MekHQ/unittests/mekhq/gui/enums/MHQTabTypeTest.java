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
package mekhq.gui.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.gui.*;
import org.junit.jupiter.api.Disabled;
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

    /**
     * This test is disabled because it will run through all the GUI tabs and initialize them, which is not a quick
     * process. This also requires lots more mock handling to actually go through the initializations too.
     */
    @Disabled
    @Test
    public void testCreateTab() {
        final MekHQ mockMekHQ = mock(MekHQ.class);
        final CampaignGUI gui = new CampaignGUI(mockMekHQ);
        for (final MHQTabType mhqTabType : types) {
            final CampaignGuiTab tab = mhqTabType.createTab(gui);
            switch (mhqTabType) {
                case COMMAND_CENTER:
                    assertInstanceOf(CommandCenterTab.class, tab);
                    break;
                case TOE:
                    assertInstanceOf(TOETab.class, tab);
                    break;
                case BRIEFING_ROOM:
                    assertInstanceOf(BriefingTab.class, tab);
                    break;
                case INTERSTELLAR_MAP:
                    assertInstanceOf(MapTab.class, tab);
                    break;
                case PERSONNEL:
                    assertInstanceOf(PersonnelTab.class, tab);
                    break;
                case HANGAR:
                    assertInstanceOf(HangarTab.class, tab);
                    break;
                case WAREHOUSE:
                    assertInstanceOf(WarehouseTab.class, tab);
                    break;
                case REPAIR_BAY:
                    assertInstanceOf(RepairTab.class, tab);
                    break;
                case INFIRMARY:
                    assertInstanceOf(InfirmaryTab.class, tab);
                    break;
                case FINANCES:
                    assertInstanceOf(FinancesTab.class, tab);
                    break;
                case MEK_LAB:
                    assertInstanceOf(MekLabTab.class, tab);
                    break;
                case STRAT_CON:
                    assertInstanceOf(StratConTab.class, tab);
                    break;
                default:
                    assertNull(tab);
                    break;
            }
        }
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("MHQTabType.COMMAND_CENTER.text"), MHQTabType.COMMAND_CENTER.toString());
        assertEquals(resources.getString("MHQTabType.WAREHOUSE.text"), MHQTabType.WAREHOUSE.toString());
        assertEquals(resources.getString("MHQTabType.STRAT_CON.text"), MHQTabType.STRAT_CON.toString());
    }
}
