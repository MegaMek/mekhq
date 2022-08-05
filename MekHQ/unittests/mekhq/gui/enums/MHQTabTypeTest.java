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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.gui.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class MHQTabTypeTest {
    //region Variable Declarations
    private static final MHQTabType[] types = MHQTabType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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
     * This test is disabled because it will run through all the GUI tabs and initialize them, which
     * is not a quick process. This also requires lots more mock handling to actually go through
     * the initializations too.
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
                    assertTrue(tab instanceof CommandCenterTab);
                    break;
                case TOE:
                    assertTrue(tab instanceof TOETab);
                    break;
                case BRIEFING_ROOM:
                    assertTrue(tab instanceof BriefingTab);
                    break;
                case INTERSTELLAR_MAP:
                    assertTrue(tab instanceof MapTab);
                    break;
                case PERSONNEL:
                    assertTrue(tab instanceof PersonnelTab);
                    break;
                case HANGAR:
                    assertTrue(tab instanceof HangarTab);
                    break;
                case WAREHOUSE:
                    assertTrue(tab instanceof WarehouseTab);
                    break;
                case REPAIR_BAY:
                    assertTrue(tab instanceof RepairTab);
                    break;
                case INFIRMARY:
                    assertTrue(tab instanceof InfirmaryTab);
                    break;
                case FINANCES:
                    assertTrue(tab instanceof FinancesTab);
                    break;
                case MEK_LAB:
                    assertTrue(tab instanceof MekLabTab);
                    break;
                case STRAT_CON:
                    assertTrue(tab instanceof StratconTab);
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
