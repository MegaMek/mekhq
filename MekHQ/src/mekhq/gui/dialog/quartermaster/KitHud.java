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
package mekhq.gui.dialog.quartermaster;

import java.awt.Color;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudStatTile;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.baseComponents.hud.HudTabStrip;

/**
 * The heads-up-display building blocks for the kit-issue dialog, giving it the look of the interstellar-map tab, the
 * StratCon deployment wizard, and the contract debrief console. The building blocks themselves live in the shared
 * {@link mekhq.gui.baseComponents.hud} kit; this class keeps the dialog's short names for them.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class KitHud {
    private KitHud() {
    }

    /** @see Hud#sectionHeading(String) */
    static JComponent sectionHeading(String text) {
        return Hud.sectionHeading(text);
    }

    /** @see Hud#hint(String) */
    static JLabel hint(String text) {
        return Hud.hint(text);
    }

    /** @see Hud#notice(String) */
    static JLabel notice(String text) {
        return Hud.notice(text);
    }

    /** @see Hud#styleScroll(JScrollPane, Color, boolean) */
    static void styleScroll(JScrollPane scroll, Color viewportColor, boolean framed) {
        Hud.styleScroll(scroll, viewportColor, framed);
    }

    /** @see Hud#styleTable(JTable) */
    static void styleTable(JTable table) {
        Hud.styleTable(table);
    }

    /** @see Hud#tileRow(JComponent...) */
    static JPanel tileRow(JComponent... tiles) {
        return Hud.tileRow(tiles);
    }

    static JComponent leftAligned(JComponent component) {
        return HudStyle.leftAligned(component);
    }

    /** A stat tile whose value can be updated as the player's selection changes. @see HudStatTile */
    static final class StatTile extends HudStatTile {
        StatTile(String key, String sub) {
            super(key, sub);
        }
    }

    /** A segmented tab strip. @see HudTabStrip */
    static final class TabStrip extends HudTabStrip {
        TabStrip(List<String> labels, boolean compact, IntConsumer onSelect) {
            super(labels, compact, onSelect);
        }
    }
}
