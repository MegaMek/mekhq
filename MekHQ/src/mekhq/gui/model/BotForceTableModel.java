/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.BotForce;

public class BotForceTableModel extends AbstractTableModel {

    // region Variable Declarations
    protected String[] columnNames;
    protected List<BotForce> data;
    private final Campaign campaign;

    public static final int COL_NAME = 0;
    public static final int COL_IFF = 1;
    public static final int COL_FIXED = 2;
    public static final int COL_RANDOM = 3;
    public static final int COL_DEPLOYMENT = 4;
    public static final int N_COL = 5;
    // endregion Variable Declarations

    public BotForceTableModel(List<BotForce> entries, Campaign c) {
        data = entries;
        this.campaign = c;
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
            case COL_IFF -> "IFF";
            case COL_FIXED -> "Fixed";
            case COL_RANDOM -> "Random";
            case COL_DEPLOYMENT -> "Deployment";
            default -> "?";
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        BotForce botForce;
        if (data.isEmpty()) {
            return "";
        } else {
            botForce = getBotForceAt(row);
        }

        return switch (col) {
            case COL_NAME -> botForce.getName();
            case COL_IFF -> (botForce.getTeam() == 1) ? "Allied" : "Enemy (Team " + botForce.getTeam() + ")";
            case COL_FIXED -> botForce.getFixedEntityList().size() + " Units, BV: " + botForce.getFixedBV();
            case COL_RANDOM -> ((null == botForce.getBotForceRandomizer()) ? ""
                                      : botForce.getBotForceRandomizer().getShortDescription());
            case COL_DEPLOYMENT -> Utilities.getDeploymentString(botForce);
            default -> "?";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public BotForce getBotForceAt(int row) {
        return data.get(row);
    }

    public void addForce(BotForce botForce) {
        data.add(botForce);
        fireTableDataChanged();
    }

    public List<BotForce> getAllBotForces() {
        return data;
    }

    public int getColumnWidth(int col) {
        return switch (col) {
            case COL_NAME -> 80;
            case COL_DEPLOYMENT -> 20;
            default -> 30;
        };
    }

    public int getAlignment(int col) {
        return switch (col) {
            case COL_NAME, COL_IFF -> SwingConstants.LEFT;
            case COL_DEPLOYMENT -> SwingConstants.CENTER;
            default -> SwingConstants.RIGHT;
        };
    }

    public String getTooltip(int row, int col) {
        BotForce botForce;
        if (data.isEmpty()) {
            return "";
        } else {
            botForce = getBotForceAt(row);
        }

        if (col == COL_RANDOM) {
            return ((null == botForce.getBotForceRandomizer()) ? ""
                          : botForce.getBotForceRandomizer().getDescription(campaign));
        }
        return null;
    }

    // fill table with values
    public void setData(List<BotForce> botForce) {
        data = botForce;
        fireTableDataChanged();
    }

    public BotForceTableModel.Renderer getRenderer() {
        return new BotForceTableModel.Renderer();
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
