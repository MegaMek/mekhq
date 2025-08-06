/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

public class MekHqTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
          Object value, boolean isSelected, boolean hasFocus,
          int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected,
              hasFocus, row, column);

        setupTableColors(this, table, isSelected, hasFocus, row);

        return this;
    }

    public static void setupTableColors(Component c, JTable table, boolean isSelected,
          boolean hasFocus, int row) {
        if (isSelected) {
            c.setForeground(table.getSelectionForeground());
            c.setBackground(table.getSelectionBackground());
        } else {
            setupTigerStripes(c, table, row);
        }

        if (hasFocus) {
            if (!isSelected) {
                Color color = UIManager.getColor("Table.focusCellForeground");
                if (color != null) {
                    c.setForeground(color);
                }
                color = UIManager.getColor("Table.focusCellBackground");
                if (color != null) {
                    c.setBackground(color);
                }
            }
        }
    }

    public static void setupTigerStripes(Component c, JTable table, int row) {
        Color background = table.getBackground();
        if (row % 2 != 0) {
            Color alternateColor = UIManager.getColor("Table.alternateRowColor");
            if (alternateColor == null) {
                // If we don't have an alternate row color, use 'controlHighlight'
                // as it is pretty reasonable across the various themes.
                alternateColor = UIManager.getColor("controlHighlight");
            }
            if (alternateColor != null) {
                background = alternateColor;
            }
        }
        c.setForeground(table.getForeground());
        c.setBackground(background);
    }
}
