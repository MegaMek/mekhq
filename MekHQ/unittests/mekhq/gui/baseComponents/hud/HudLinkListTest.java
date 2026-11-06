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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/** How the list finds links and tooltips inside a rendered row, tested on plain component trees. */
class HudLinkListTest {
    @Test
    void aLinkIsFoundFromTheLabelItself() {
        JPanel row = new JPanel();
        JLabel unit = HudLinkList.markLink(new JLabel("Atlas AS7-D"), "unit-7");
        row.add(unit);

        assertEquals("unit-7", HudLinkList.linkOf(unit, row));
    }

    @Test
    void aLinkIsFoundFromTextNestedInsideIt() {
        JPanel row = new JPanel();
        JPanel link = HudLinkList.markLink(new JPanel(), "person-3");
        JLabel text = new JLabel("Lt Lio Drace");
        link.add(text);
        row.add(link);

        assertEquals("person-3", HudLinkList.linkOf(text, row));
    }

    @Test
    void plainPartsOfARowAreNotLinks() {
        JPanel row = new JPanel();
        JLabel name = new JLabel("Foot Platoon");
        row.add(name);

        assertNull(HudLinkList.linkOf(name, row));
        assertNull(HudLinkList.linkOf(null, row));
    }

    @Test
    void theSearchStopsAtTheRowEvenIfItHasAParent() {
        JPanel outside = HudLinkList.markLink(new JPanel(), "outside");
        JPanel row = new JPanel();
        JLabel name = new JLabel("Name");
        row.add(name);
        // A list's renderer component is parented to its cell renderer pane once it has been painted
        outside.add(row);

        assertNull(HudLinkList.linkOf(name, row));
    }

    @Test
    void markingALinkWithNullMakesItPlainAgain() {
        JPanel row = new JPanel();
        JLabel label = HudLinkList.markLink(new JLabel("Name"), "link");
        row.add(label);

        HudLinkList.markLink(label, null);

        assertNull(HudLinkList.linkOf(label, row));
    }

    @Test
    void theInnermostTooltipWins() {
        JPanel row = new JPanel();
        row.setToolTipText("The row");
        JLabel badge = new JLabel("3 SPA");
        badge.setToolTipText("Sniper, Dodge, Iron Man");
        JLabel name = new JLabel("Name");
        row.add(badge);
        row.add(name);

        assertEquals("Sniper, Dodge, Iron Man", HudLinkList.toolTipOf(badge, row));
        assertEquals("The row", HudLinkList.toolTipOf(name, row));
    }

    @Test
    void nothingIsHoveredOnANewList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Row");
        HudLinkList<String> list = new HudLinkList<>(model);

        assertFalse(HudLinkList.isLinkHovered(list, 0, "link"));
        assertSame(HudToolTip.class, list.createToolTip().getClass());
    }
}
