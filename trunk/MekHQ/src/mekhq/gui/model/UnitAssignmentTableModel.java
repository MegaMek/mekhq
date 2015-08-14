package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.UnitType;
import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.gui.BasicInfo;
import mekhq.gui.dialog.RetirementDefectionDialog;

public class UnitAssignmentTableModel extends AbstractTableModel {

    /**
     *
     */
    private static final long serialVersionUID = 7740627991191879456L;

    public final static int COL_UNIT = 0;
    public final static int COL_CLASS = 1;
    public final static int COL_COST = 2;
    public final static int N_COL = 3;

    private final static String[] colNames = {
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
        DecimalFormat formatter = new DecimalFormat();
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
            return formatter.format(u.getBuyCost());
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

    public class TextRenderer extends DefaultTableCellRenderer {

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
            setForeground(isSelected?Color.WHITE:Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
            } else {
                // tiger stripes
                if ((row % 2) == 0) {
                    setBackground(new Color(220, 220, 220));
                } else {
                    setBackground(Color.WHITE);
                }
            }
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
            String color = "black";
            if(isSelected) {
                color = "white";
            }
            setText(getValueAt(actualRow, actualCol).toString(), color);
            if (actualCol == COL_UNIT) {
                if(null != u) {
                    String desc = "<b>" + u.getName() + "</b><br>";
                    desc += u.getEntity().getWeightClassName();
                    if(!(u.getEntity() instanceof SmallCraft || u.getEntity() instanceof Jumpship)) {
                        desc += " " + UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(u.getEntity()));
                    }
                    desc += "<br>" + u.getStatus() + "";
                    setText(desc, color);
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

            if (isSelected) {
                c.setBackground(Color.DARK_GRAY);
            } else {
                // tiger stripes
                if ((row % 2) == 0) {
                    c.setBackground(new Color(220, 220, 220));
                } else {
                    c.setBackground(Color.WHITE);
                }
            }
            return c;
        }
    }
}
