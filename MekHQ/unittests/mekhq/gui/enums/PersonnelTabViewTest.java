/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

public class PersonnelTabViewTest {
    private static final PersonnelTabView[] views = PersonnelTabView.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());

    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("PersonnelTabView.GRAPHIC.toolTipText"),
              PersonnelTabView.GRAPHIC.getToolTipText());
        assertEquals(resources.getString("PersonnelTabView.DATES.toolTipText"),
              PersonnelTabView.DATES.getToolTipText());
    }

    @Test
    public void testIsGraphic() {
        for (final PersonnelTabView personnelTabView : views) {
            if (personnelTabView == PersonnelTabView.GRAPHIC) {
                assertTrue(personnelTabView.isGraphic());
            } else {
                assertFalse(personnelTabView.isGraphic());
            }
        }
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PersonnelTabView.GRAPHIC.text"), PersonnelTabView.GRAPHIC.toString());
        assertEquals(resources.getString("PersonnelTabView.TECHNICAL_SKILLS.text"),
              PersonnelTabView.TECHNICAL_SKILLS.toString());
        assertEquals(resources.getString("PersonnelTabView.OTHER.text"), PersonnelTabView.OTHER.toString());
    }
}
