/*
 * Copyright (c) 2013-2021 - The MegaMek Team. All Rights Reserved.
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
import java.util.Optional;
import java.util.stream.IntStream;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * A table model for displaying acquisitions. Unlike the other table models here, this one
 * can apply to multiple tables and so we have to be more careful in its design
 */
public class ProcurementTableModel extends DataTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = 534443424190075264L;

    private final Campaign campaign;

    public final static int COL_NAME    =    0;
    public final static int COL_TYPE     =   1;
    public final static int COL_COST     =   2;
    public final static int COL_TOTAL_COST = 3;
    public final static int COL_TARGET    =  4;
    public final static int COL_NEXT      =  5;
    public final static int COL_QUEUE     =  6;
    public final static int N_COL          = 7;
    //endregion Variable Declarations

    //region Constructors
    public ProcurementTableModel(final Campaign campaign) {
        this.campaign = campaign;
        setData(campaign.getShoppingList().getPartList());
    }
    //endregion Constructors

    @Override
    public int getRowCount() {
        return getData().size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(final int column) {
        switch (column) {
            case COL_NAME:
                return "Name";
            case COL_TYPE:
                return "Type";
            case COL_COST:
                return "Cost per Unit";
            case COL_TOTAL_COST:
                return "Total Cost";
            case COL_TARGET:
                return "Target";
            case COL_QUEUE:
                return "Quantity";
            case COL_NEXT:
                return "Next Check";
            default:
                return "?";
        }
    }

    public void incrementItem(final int row) {
        getAcquisition(row).ifPresent(IAcquisitionWork::incrementQuantity);
        fireTableCellUpdated(row, COL_QUEUE);
    }

    public void decrementItem(final int row) {
        getAcquisition(row).ifPresent(IAcquisitionWork::decrementQuantity);
        fireTableCellUpdated(row, COL_QUEUE);
    }

    public void removeRow(final int row) {
        getNewEquipmentAt(row).ifPresent(getCampaign().getShoppingList()::removeItem);
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return data.isEmpty() ? "" : getAcquisition(row).map(a -> getValueFor(a, column)).orElse("?");
    }

    private Object getValueFor(final IAcquisitionWork shoppingItem, final int column) {
        switch (column) {
            case COL_NAME:
                return shoppingItem.getAcquisitionName();
            case COL_TYPE:
                if (shoppingItem instanceof UnitOrder) {
                    return "Unit";
                } else if (shoppingItem instanceof Part) {
                    return "Part";
                } else {
                    return "Other";
                }
            case COL_COST:
                return shoppingItem.getBuyCost().toAmountAndSymbolString();
            case COL_TOTAL_COST:
                return shoppingItem.getBuyCost().multipliedBy(shoppingItem.getQuantity()).toAmountAndSymbolString();
            case COL_TARGET:
                final TargetRoll target = getCampaign().getTargetForAcquisition(shoppingItem, getCampaign().getLogisticsPerson(), false);
                String value = target.getValueAsString();
                if (IntStream.of(TargetRoll.IMPOSSIBLE, TargetRoll.AUTOMATIC_SUCCESS, TargetRoll.AUTOMATIC_FAIL)
                        .allMatch(i -> (target.getValue() != i))) {
                    value += "+";
                }
                return value;
            case COL_NEXT:
                final int days = shoppingItem.getDaysToWait();
                return String.format("%d %s", days, (days == 1) ? "day" : "days");
            case COL_QUEUE:
                return shoppingItem.getQuantity();
            default:
                return "?";

        }
    }

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        return getValueAt(0, column).getClass();
    }

    public Optional<Object> getNewEquipmentAt(final int row) {
        return getAcquisition(row).map(IAcquisitionWork::getNewEquipment);
    }

    public Optional<IAcquisitionWork> getAcquisition(final int row) {
        return ((row >= 0) && (row < data.size())) ? Optional.of((IAcquisitionWork) data.get(row))
                : Optional.empty();
    }

    public int getColumnWidth(final int column) {
        switch (column) {
            case COL_NAME:
                return 200;
            case COL_COST:
            case COL_TOTAL_COST:
            case COL_TARGET:
            case COL_NEXT:
                return 40;
            default:
                return 15;
        }
    }

    public int getAlignment(final int column) {
        switch (column) {
            case COL_COST:
            case COL_TOTAL_COST:
            case COL_QUEUE:
                return SwingConstants.RIGHT;
            case COL_TARGET:
            case COL_NEXT:
            case COL_TYPE:
                return SwingConstants.CENTER;
            default:
                return SwingConstants.LEFT;
        }
    }

    public String getTooltip(final int row, final int column) {
        return getAcquisition(row).map(a -> getTooltipFor(a, column)).orElse(null);
    }

    private String getTooltipFor(final IAcquisitionWork shoppingItem, final int column) {
        switch (column) {
            case COL_TARGET:
                return getCampaign().getTargetForAcquisition(shoppingItem,
                        getCampaign().getLogisticsPerson(), false).getDesc();
            default:
                return "<html>You can increase or decrease the quantity with the left/right arrows keys or the plus/minus keys.<br>Quantities reduced to zero will remain on the list until the next procurement cycle.</html>";
        }
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public ProcurementTableModel.Renderer getRenderer() {
        return new ProcurementTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            final int actualCol = table.convertColumnIndexToModel(column);
            final int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));
            return this;
        }
    }
}
