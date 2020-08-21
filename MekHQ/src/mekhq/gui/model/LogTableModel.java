/*
 * Copyright (c) 2019 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import mekhq.campaign.Campaign;
import mekhq.campaign.log.LogEntry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * A table model for displaying log entries.
 */
public class LogTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    protected List<LogEntry> data;
    private Campaign campaign;

    public final static int COL_DATE = 0;
    public final static int COL_DESC = 1;
    public final static int N_COL = 2;

    public LogTableModel(List<LogEntry> entries, Campaign campaign) {
        assert entries != null;
        data = entries;
        this.campaign = campaign;
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
        switch (column) {
            case COL_DATE:
                return "Date";
            case COL_DESC:
                return "Description";
            default:
                return "?";
        }
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
            return campaign.getCampaignOptions().getDisplayFormattedDate(entry.getDate());
        } else if (col == COL_DESC) {
            return entry.getDesc();
        } else {
            return "?";
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public LogEntry getEntry(int row) {
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_DESC:
                return 200;
            default:
                return 10;
        }
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public String getTooltip(int row, int col) {
        switch (col) {
            default:
                return null;
        }
    }

    public void setData(List<LogEntry> entries) {
        assert entries != null;
        data = entries;
        fireTableDataChanged();
    }

    public LogTableModel.Renderer getRenderer() {
        return new LogTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 9054581142945717303L;

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
