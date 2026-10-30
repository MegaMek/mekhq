/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.baseComponents.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.gui.baseComponents.hud.HudFilterList.Item;
import org.junit.jupiter.api.Test;

class HudFilterListTest {
    private static final List<Item<String>> ITEMS = List.of(new Item<>(null, "All types", 241),
          new Item<>("Mek", "Mek", 59), new Item<>("Tank", "Tank", 72));

    @Test
    void findsRowsByValueIncludingTheNullAllRow() {
        assertEquals(0, HudFilterList.indexOf(ITEMS, null));
        assertEquals(2, HudFilterList.indexOf(ITEMS, "Tank"));
        assertEquals(-1, HudFilterList.indexOf(ITEMS, "VTOL"));
    }

    @Test
    void arrowStepsStayWithinTheList() {
        assertEquals(1, HudFilterList.step(0, 1, 3));
        assertEquals(0, HudFilterList.step(0, -1, 3));
        assertEquals(2, HudFilterList.step(2, 1, 3));
        assertEquals(-1, HudFilterList.step(0, 1, 0));
    }

    @Test
    void negativeCountsShowNothing() {
        assertEquals("", HudFilterList.formatCount(-1));
        assertEquals(String.valueOf(59), HudFilterList.formatCount(59));
    }

    @Test
    void settingItemsOrTheSelectionNeverNotifies() {
        List<String> chosen = new ArrayList<>();
        HudFilterList<String> list = new HudFilterList<>(chosen::add);

        list.setItems(ITEMS);
        list.setSelected("Mek");

        assertTrue(chosen.isEmpty());
        assertEquals("Mek", list.getSelected());
        assertEquals(3, list.getComponentCount());
    }

    @Test
    void selectionIsKeptWhenItemsAreReplaced() {
        HudFilterList<String> list = new HudFilterList<>(value -> {});
        list.setItems(ITEMS);
        list.setSelected(null);

        list.setItems(List.of(new Item<>(null, "All types", 200), new Item<>("Mek", "Mek", 40)));

        assertNull(list.getSelected());
        assertEquals(2, list.getComponentCount());
    }
}
