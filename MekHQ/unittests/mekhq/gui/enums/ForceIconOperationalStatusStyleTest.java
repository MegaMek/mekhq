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

import mekhq.MHQConstants;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ForceIconOperationalStatusStyleTest {
    //region Variable Declarations
    private static final ForceIconOperationalStatusStyle[] styles = ForceIconOperationalStatusStyle.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.BORDER.toolTipText"),
                ForceIconOperationalStatusStyle.BORDER.getToolTipText());
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.TAB.toolTipText"),
                ForceIconOperationalStatusStyle.TAB.getToolTipText());
    }

    @Test
    public void testGetPath() {
        assertEquals(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_BORDER_PATH,
                ForceIconOperationalStatusStyle.BORDER.getPath());
        assertEquals(MHQConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_TAB_PATH,
                ForceIconOperationalStatusStyle.TAB.getPath());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsBorder() {
        for (final ForceIconOperationalStatusStyle forceIconOperationalStatusStyle : styles) {
            if (forceIconOperationalStatusStyle == ForceIconOperationalStatusStyle.BORDER) {
                assertTrue(forceIconOperationalStatusStyle.isBorder());
            } else {
                assertFalse(forceIconOperationalStatusStyle.isBorder());
            }
        }
    }

    @Test
    public void testIsTab() {
        for (final ForceIconOperationalStatusStyle forceIconOperationalStatusStyle : styles) {
            if (forceIconOperationalStatusStyle == ForceIconOperationalStatusStyle.TAB) {
                assertTrue(forceIconOperationalStatusStyle.isTab());
            } else {
                assertFalse(forceIconOperationalStatusStyle.isTab());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.BORDER.text"), ForceIconOperationalStatusStyle.BORDER.toString());
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.TAB.text"), ForceIconOperationalStatusStyle.TAB.toString());
    }
}
