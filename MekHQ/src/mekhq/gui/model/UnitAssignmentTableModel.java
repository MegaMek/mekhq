package mekhq.gui.model;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.UnitType;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class UnitAssignmentTableModel extends AbstractTableModel {

    /**
     *
     */
    private static final long serialVersionUID = 7740627991191879456L;

    public static final int COL_UNIT = 0;
    public static final int COL_CLASS = 1;
    public static final int COL_COST = 2;
    public static final int N_COL = 3;

    private static final String[] colNames = {
        "Unit", "Class", "Cost"
    };

    private Campaign campaign;
    ArrayList<UUID> data;

    public UnitAssignmentTableModel(Campaign c) {
        this.campaign = c;
        data = new ArrayList<UUID>();
    }

    public void setData(ArrayList<UUID> data) {
        this.data = data;
        fireTableDataChanged();
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
        return colNames[column];
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_UNIT:
            return 125;
        case COL_COST:
            return 70;
        case COL_CLASS:
        default:
            return 20;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        case COL_UNIT:
            return SwingConstants.LEFT;
        case COL_COST:
            return SwingConstants.RIGHT;
        case COL_CLASS:
        default:
            return SwingConstants.CENTER;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return getValueAt(0, col).getClass();
    }

    @Override
    public Object getValueAt(int row, int col) {
        Unit u;
        if(data.isEmpty()) {
            return "";
        } else {
            u = campaign.getUnit(data.get(row));
        }
        switch (col) {
        case COL_UNIT:
            if(null != u) {
                return u.getName();
            }
            return "-";
        case COL_CLASS:
            return RetirementDefectionDialog.weightClassIndex(u);
        case COL_COST:
            return u.getBuyCost().toAmountAndSymbolString();
        default:
            return "?";
        }
    }

    public Unit getUnit(int row) {
        return campaign.getUnit(data.get(row));
    }

    public TableCellRenderer getRenderer(int col, IconPackage icons) {
        if (col == COL_UNIT) {
            return new VisualRenderer(icons);
        } else return new TextRenderer();
    }

    public class TextRenderer extends MekHqTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -3368335772600192895L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            int actualCol = table.convertColumnIndexToModel(column);
            setHorizontalAlignment(getAlignment(actualCol));
            return this;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 7261885081786958754L;

        public VisualRenderer(IconPackage icons) {
            super(icons);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            Unit u = getUnit(actualRow);
            setText(getValueAt(actualRow, actualCol).toString());
            if (actualCol == COL_UNIT) {
                if(null != u) {
                    String desc = "<b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if(!(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship)) {
                        desc += " " + UnitType.getTypeDisplayableName(u.getEntity().getUnitType());
                    }
                    desc += "<br>" + u.getStatus() + "";
                    setText(desc);
                    Image mekImage = getImageFor(u);
                    if(null != mekImage) {
                        setImage(mekImage);
                    } else {
                        clearImage();
                    }
                } else {
                    clearImage();
                }
            }

            MekHqTableCellRenderer.setupTableColors(c, table, isSelected, hasFocus, row);
            return c;
        }
    }
}
