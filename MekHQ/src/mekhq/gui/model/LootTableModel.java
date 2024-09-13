/*
 * LootTableModel.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.Entity;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.parts.Part;

/**
 * A table model for displaying loot for scenarios and missions
 */
public class LootTableModel extends AbstractTableModel {
    // region Variable Declarations
    protected String[] columnNames;
    protected List<Loot> data;

    public final static int COL_NAME = 0;
    public final static int COL_MONEY = 1;
    public final static int COL_MEKS = 2;
    public final static int COL_PARTS = 3;
    public final static int N_COL = 4;
    // endregion Variable Declarations

    public LootTableModel(List<Loot> entries) {
        data = entries;
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
            case COL_NAME:
                return "Name";
            case COL_MONEY:
                return "Money";
            case COL_MEKS:
                return "# Units";
            case COL_PARTS:
                return "# Parts";
            default:
                return "?";
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        Loot loot;
        if (data.isEmpty()) {
            return "";
        } else {
            loot = getLootAt(row);
        }

        switch (col) {
            case COL_NAME:
                return loot.getName();
            case COL_MONEY:
                return loot.getCash().toAmountAndSymbolString();
            case COL_MEKS:
                return loot.getUnits().size();
            case COL_PARTS:
                return loot.getParts().size();
            default:
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

    public Loot getLootAt(int row) {
        return data.get(row);
    }

    public void addLoot(Loot loot) {
        data.add(loot);
        fireTableDataChanged();
    }

    public List<Loot> getAllLoot() {
        return data;
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_MONEY:
            case COL_NAME:
                return 100;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public String getTooltip(int row, int col) {
        switch (col) {
            case COL_MEKS:
                return getLootAt(row).getUnits().stream().map(Entity::getDisplayName)
                        .collect(Collectors.joining(", "));
            case COL_PARTS:
                return getLootAt(row).getParts().stream().map(Part::getPartName)
                        .collect(Collectors.joining(", "));
            default:
                return null;
        }
    }

    // fill table with values
    public void setData(List<Loot> loot) {
        data = loot;
        fireTableDataChanged();
    }

    public LootTableModel.Renderer getRenderer() {
        return new LootTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
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
