/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.stream.Collectors;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.units.Entity;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.parts.Part;

/**
 * A table model for displaying loot for scenarios and missions
 */
public class LootTableModel extends AbstractTableModel {
    // region Variable Declarations
    protected String[] columnNames;
    protected List<Loot> data;

    public static final int COL_NAME = 0;
    public static final int COL_MONEY = 1;
    public static final int COL_MEKS = 2;
    public static final int COL_PARTS = 3;
    public static final int N_COL = 4;
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
        return switch (column) {
            case COL_NAME -> "Name";
            case COL_MONEY -> "Money";
            case COL_MEKS -> "# Units";
            case COL_PARTS -> "# Parts";
            default -> "?";
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        Loot loot;
        if (data.isEmpty()) {
            return "";
        } else {
            loot = getLootAt(row);
        }

        return switch (col) {
            case COL_NAME -> loot.getName();
            case COL_MONEY -> loot.getCash().toAmountAndSymbolString();
            case COL_MEKS -> loot.getUnits().size();
            case COL_PARTS -> loot.getParts().size();
            default -> "?";
        };
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
        return switch (c) {
            case COL_MONEY, COL_NAME -> 100;
            default -> 20;
        };
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public String getTooltip(int row, int col) {
        return switch (col) {
            case COL_MEKS -> getLootAt(row).getUnits().stream().map(Entity::getDisplayName)
                                   .collect(Collectors.joining(", "));
            case COL_PARTS -> getLootAt(row).getParts().stream().map(Part::getPartName)
                                    .collect(Collectors.joining(", "));
            default -> null;
        };
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
