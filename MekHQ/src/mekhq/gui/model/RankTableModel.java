/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
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
import java.util.ResourceBundle;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class RankTableModel extends DefaultTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_NAME_RATE	= 0;
    public final static int COL_NAME_MW		= 1;
    public final static int COL_NAME_ASF	= 2;
    public final static int COL_NAME_VEE	= 3;
    public final static int COL_NAME_NAVAL	= 4;
    public final static int COL_NAME_INF	= 5;
    public final static int COL_NAME_TECH	= 6;
    public final static int COL_OFFICER		= 7;
    public final static int COL_PAYMULT		= 8;
    public final static int COL_NUM			= 9;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public RankTableModel(final RankSystem rankSystem) {
        setData(rankSystem);
    }
    //endregion Constructors

    public void setData(final RankSystem rankSystem) {
        setDataVector(rankSystem.getRanksForModel(), resources.getString("RankTableModel.columnNames").split(","));
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return (column != COL_NAME_RATE) && (column != COL_OFFICER);
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        switch (column) {
            case COL_NAME_RATE:
            case COL_NAME_MW:
            case COL_NAME_ASF:
            case COL_NAME_VEE:
            case COL_NAME_NAVAL:
            case COL_NAME_INF:
            case COL_NAME_TECH:
                return String.class;
            case COL_OFFICER:
                return Boolean.class;
            case COL_PAYMULT:
                return Double.class;
            default:
                return getValueAt(0, column).getClass();
        }
    }

    public int getColumnWidth(final int column) {
        switch (column) {
            case COL_NAME_RATE:
                return 100;
            case COL_OFFICER:
            case COL_PAYMULT:
                return 250;
            default:
                return 500;
        }
    }

    public int getAlignment(final int column) {
        switch (column) {
            case COL_NAME_RATE:
            case COL_NAME_MW:
            case COL_NAME_ASF:
            case COL_NAME_VEE:
            case COL_NAME_NAVAL:
            case COL_NAME_INF:
            case COL_NAME_TECH:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
    }

    public String getToolTip(final int column) {
        switch (column) {
            case COL_NAME_RATE:
                return resources.getString("RankTableModel.COL_NAME_RATE.toolTipText");
            case COL_NAME_MW:
                return resources.getString("RankTableModel.COL_NAME_MW.toolTipText");
            case COL_NAME_ASF:
                return resources.getString("RankTableModel.COL_NAME_ASF.toolTipText");
            case COL_NAME_VEE:
                return resources.getString("RankTableModel.COL_NAME_VEE.toolTipText");
            case COL_NAME_NAVAL:
                return resources.getString("RankTableModel.COL_NAME_NAVAL.toolTipText");
            case COL_NAME_INF:
                return resources.getString("RankTableModel.COL_NAME_INF.toolTipText");
            case COL_NAME_TECH:
                return resources.getString("RankTableModel.COL_NAME_TECH.toolTipText");
            case COL_OFFICER:
                return resources.getString("RankTableModel.COL_OFFICER.toolTipText");
            case COL_PAYMULT:
                return resources.getString("RankTableModel.COL_PAYMULT.toolTipText");
            default:
                return resources.getString("RankTableModel.defaultToolTip.toolTipText");
        }
    }

    public TableCellRenderer getRenderer() {
        return new RankTableModel.Renderer();
    }

    public class Renderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int actualCol = table.convertColumnIndexToModel(column);
            setToolTipText(getToolTip(actualCol));
            setOpaque(true);
            setHorizontalAlignment(getAlignment(actualCol));
            return this;
        }
    }
}
