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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MHQConstants;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

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
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.BORDER.text"),
              ForceIconOperationalStatusStyle.BORDER.toString());
        assertEquals(resources.getString("ForceIconOperationalStatusStyle.TAB.text"),
              ForceIconOperationalStatusStyle.TAB.toString());
    }
}
