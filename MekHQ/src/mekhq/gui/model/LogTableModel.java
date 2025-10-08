/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.List;
import java.util.Objects;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;

/**
 * A table model for displaying log entries.
 */
public class LogTableModel extends AbstractTableModel {
    protected List<LogEntry> data;

    public static final int COL_DATE = 0;
    public static final int COL_DESC = 1;
    public static final int N_COL = 2;

    public LogTableModel(List<LogEntry> entries) {
        data = Objects.requireNonNull(entries);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_DATE -> "Date";
            case COL_DESC -> "Description";
            default -> "?";
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        LogEntry entry;
        if (data.isEmpty()) {
            return "";
        } else {
            entry = data.get(row);
        }

        if (col == COL_DATE) {
            return MekHQ.getMHQOptions().getDisplayFormattedDate(entry.getDate());
        } else if (col == COL_DESC) {
            return entry.getDesc();
        } else {
            return "?";
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public LogEntry getEntry(int row) {
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        if (c == COL_DESC) {
            return 200;
        }
        return 10;
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public @Nullable String getTooltip(int row, int col) {
        return null;
    }

    public void setData(List<LogEntry> entries) {
        data = Objects.requireNonNull(entries);
        fireTableDataChanged();
    }

    public LogTableModel.Renderer getRenderer() {
        return new LogTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }
    }
}
