/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.awt.FontMetrics;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import mekhq.MekHQ;
import mekhq.campaign.Kill;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class PersonnelKillLogModel extends DataTableModel {
    private static final String EMPTY_CELL = "";

    public static final int COL_DATE = 0;
    public static final int COL_TEXT = 1;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.PersonnelKillLogModel",
          MekHQ.getMHQOptions().getLocale());
    private final int dateTextWidth;

    public PersonnelKillLogModel() {
        data = new ArrayList<Kill>();
        dateTextWidth = getRenderer().metrics.stringWidth(MekHQ.getMHQOptions()
                                                                .getDisplayFormattedDate(LocalDate.now())
                                                                .concat("MM"));
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COL_TEXT + 1;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_DATE:
                return resourceMap.getString("date.heading");
            case COL_TEXT:
                return resourceMap.getString("kill.heading");
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Kill kill = getKill(row);
        switch (column) {
            case COL_DATE:
                return MekHQ.getMHQOptions().getDisplayFormattedDate(kill.getDate());
            case COL_TEXT:
                return String.format(resourceMap.getString("killDetail.format"),
                      kill.getWhatKilled(),
                      kill.getKilledByWhat());
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Kill getKill(int row) {
        if ((row < 0) || (row >= data.size())) {
            return null;
        } else {
            return (Kill) data.get(row);
        }
    }

    public int getAlignment(int column) {
        switch (column) {
            case COL_DATE:
                return StyleConstants.ALIGN_RIGHT;
            case COL_TEXT:
                return StyleConstants.ALIGN_LEFT;
            default:
                return StyleConstants.ALIGN_CENTER;
        }
    }

    public int getPreferredWidth(int column) {
        switch (column) {
            case COL_DATE:
                return dateTextWidth;
            case COL_TEXT:
                return 300;
            default:
                return 100;
        }
    }

    public boolean hasConstantWidth(int col) {
        switch (col) {
            case COL_DATE:
                return true;
            default:
                return false;
        }
    }

    public PersonnelKillLogModel.Renderer getRenderer() {
        return new PersonnelKillLogModel.Renderer();
    }

    public static class Renderer extends JTextPane implements TableCellRenderer {
        private final SimpleAttributeSet attribs = new SimpleAttributeSet();
        private final FontMetrics metrics;

        public Renderer() {
            super();
            setOpaque(true);
            setFont(UIManager.getDefaults().getFont("TabbedPane.font"));
            metrics = getFontMetrics(getFont());
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            setText((String) value);
            StyleConstants.setAlignment(attribs, ((PersonnelKillLogModel) table.getModel()).getAlignment(column));
            setParagraphAttributes(attribs, false);

            int fontHeight = metrics.getHeight();
            int textLength = metrics.stringWidth(getText());
            int lines = (int) Math.ceil(1.0 * textLength / table.getColumnModel().getColumn(column).getWidth());
            if (lines == 0) {
                lines = 1;
            }

            int height = fontHeight * lines + 4;
            if (table.getRowHeight(row) < height) {
                table.setRowHeight(row, height);
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, row);
            return this;
        }
    }
}
