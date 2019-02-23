/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.model;

import mekhq.campaign.Kill;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class KillTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -58915479895694545L;

    protected java.util.List<Kill> data;

    public final static int COL_DATE    = 0;
    public final static int COL_KILLED  = 1;
    public final static int COL_KILLER  = 2;
    public final static int N_COL       = 3;

    public KillTableModel(java.util.List<Kill> entries) {
        assert entries != null;
        data = entries;
    }

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case COL_DATE:
                return "Date";
            case COL_KILLED:
                return "Kill";
            case COL_KILLER:
                return "With";
            default:
                return "?";
        }
    }

    public Object getValueAt(int row, int col) {
        Kill kill;
        if(data.isEmpty()) {
            return "";
        } else {
            kill = data.get(row);
        }
        if(col == COL_DATE) {
            SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
            return shortDateFormat.format(kill.getDate());
        }
        if(col == COL_KILLED) {
            return kill.getWhatKilled();
        }
        if(col == COL_KILLER) {
            return kill.getKilledByWhat();
        }
        return "?";
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public Kill getKillAt(int row) {
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        switch(c) {
            case COL_DATE:
                return 20;
            default:
                return 100;
        }
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public String getTooltip(int row, int col) {
        switch(col) {
            default:
                return null;
        }
    }

    public void setData(java.util.List<Kill> kills) {
        assert kills != null;
        data = kills;
        fireTableDataChanged();
    }

    public KillTableModel.Renderer getRenderer() {
        return new KillTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -2888173457152182907L;

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }
    }
}
