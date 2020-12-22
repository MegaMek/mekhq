package mekhq.gui.model;

import java.awt.Component;
import java.util.Optional;

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
    private static final long serialVersionUID = 534443424190075264L;

    Campaign campaign;

    public final static int COL_NAME    =    0;
    public final static int COL_TYPE     =   1;
    public final static int COL_COST     =   2;
    public final static int COL_TOTAL_COST = 3;
    public final static int COL_TARGET    =  4;
    public final static int COL_NEXT      =  5;
    public final static int COL_QUEUE     =  6;
    public final static int N_COL          = 7;

    public ProcurementTableModel(Campaign c) {
        data = c.getShoppingList().getPartList();
        campaign = c;
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

    public void incrementItem(int row) {
        getAcquisition(row).ifPresent(IAcquisitionWork::incrementQuantity);
        this.fireTableCellUpdated(row, COL_QUEUE);
    }

    public void decrementItem(int row) {
        getAcquisition(row).ifPresent(IAcquisitionWork::decrementQuantity);
        this.fireTableCellUpdated(row, COL_QUEUE);
    }

    public void removeRow(int row) {
        getNewEquipmentAt(row).ifPresent(getCampaign().getShoppingList()::removeItem);
    }

    @Override
    public Object getValueAt(final int row, final int col) {
        if (data.isEmpty()) {
            return "";
        }
        
        return getAcquisition(row).map(a -> getValueFor(a, col)).orElse("?");
    }

    private Object getValueFor(IAcquisitionWork shoppingItem, int col) {
        if(col == COL_NAME) {
            return shoppingItem.getAcquisitionName();
        }
        if (col == COL_TYPE) {
            if (shoppingItem instanceof UnitOrder) {
                return "Unit";
            } else if (shoppingItem instanceof Part) {
                return "Part";
            } else {
                return "Other";
            }
        }
        if(col == COL_COST) {
            return shoppingItem.getBuyCost().toAmountAndSymbolString();
        }
        if(col == COL_TOTAL_COST) {
            return shoppingItem.getBuyCost().multipliedBy(shoppingItem.getQuantity()).toAmountAndSymbolString();
        }
        if(col == COL_TARGET) {
            TargetRoll target = getCampaign().getTargetForAcquisition(shoppingItem, getCampaign().getLogisticsPerson(), false);
            String value = target.getValueAsString();
            if(target.getValue() != TargetRoll.IMPOSSIBLE && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                value += "+";
            }
            return value;
        }
        if(col == COL_QUEUE) {
            return shoppingItem.getQuantity();
        }
        if(col == COL_NEXT) {
            int days = shoppingItem.getDaysToWait();
            String dayName = " day";
            if(days != 1) {
                dayName += "s";
            }
            return days + dayName;
        }
        return "?";
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<? extends Object> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public Optional<Object> getNewEquipmentAt(int row) {
        return getAcquisition(row).map(IAcquisitionWork::getNewEquipment);
    }

    public Optional<IAcquisitionWork> getAcquisition(int row) {
        if (row >= 0 && row < data.size()) {
            return Optional.of((IAcquisitionWork) data.get(row));
        }
        
        return Optional.empty();
    }

    public int getColumnWidth(int c) {
        switch(c) {
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

    public int getAlignment(int col) {
        switch(col) {
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

    public String getTooltip(final int row, final int col) {
        return getAcquisition(row).map(a -> getTooltipFor(a, col)).orElse(null);
    }

    private String getTooltipFor(IAcquisitionWork shoppingItem, int col) {
        switch(col) {
            case COL_TARGET:
                TargetRoll target = getCampaign().getTargetForAcquisition(shoppingItem, getCampaign().getLogisticsPerson(), false);
                return target.getDesc();
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

        private static final long serialVersionUID = 9054581142945717303L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }

    }
}
