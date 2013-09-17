package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;

/**
 * A table model for displaying units that are being serviced in the repair bay
 * TODO: we should be able to handle this with the basic UnitTableModel with 
 * appropriate filtering of columns
 */
public class ServicedUnitTableModel extends DataTableModel {
    private static final long serialVersionUID = 3314061779690077204L;

    public final static int COL_NAME    =    0;
    public final static int COL_TYPE    =    1;
    public final static int COL_STATUS   =   2;
    public final static int COL_SITE =   3;
    public final static int COL_REPAIR  =    4;
    public final static int COL_PARTS    =   5;
    public final static int N_COL =          6;

    public ServicedUnitTableModel() {
        data = new ArrayList<Unit>();
    }
    
    public int getRowCount() {
        return data.size();
    }

    @Override
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
        case COL_STATUS:
            return "Status";
        case COL_SITE:
            return "Site";
        case COL_REPAIR:
            return "# Repairs";
        case COL_PARTS:
            return "# Parts";
        default:
            return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_TYPE:
        case COL_STATUS:
        case COL_SITE:
            return 50;
        case COL_NAME:
            return 100;
        default:
            return 25;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        case COL_REPAIR:
        case COL_PARTS:
            return SwingConstants.RIGHT;
        default:
            return SwingConstants.LEFT;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Unit getUnit(int i) {
        return (Unit)data.get(i);
    }

    public Object getValueAt(int row, int col) {
        Unit u;
        if(data.isEmpty()) {
            return "";
        } else {
            u = getUnit(row);
        }
        Entity e = u.getEntity();
        if(null == e) {
            return "?";
        }
        if(col == COL_NAME) {
            return u.getName();
        }
        if(col == COL_TYPE) {
            return UnitType.getTypeDisplayableName(UnitType.determineUnitTypeCode(e));
        }
        if(col == COL_STATUS) {
            return u.getStatus();
        }
        if(col == COL_SITE) {
            return Unit.getSiteName(u.getSite());
        }
        if(col == COL_REPAIR) {
            return u.getPartsNeedingFixing().size();
        }
        if(col == COL_PARTS) {
            return u.getPartsNeeded().size();
        }
        return "?";
    }

    public ServicedUnitTableModel.Renderer getRenderer() {
        return new ServicedUnitTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 9054581142945717303L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            Unit u = getUnit(actualRow);

            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            } else {
                if (u.isDeployed()) {
                    setBackground(Color.LIGHT_GRAY);
                }
                if(u.isRefitting()) {
                    setBackground(Color.CYAN);
                }
                else if (null != u && !u.isRepairable()) {
                    setBackground(new Color(190, 150, 55));
                } else if ((null != u) && !u.isFunctional()) {
                    setBackground(new Color(205, 92, 92));
                } else if ((null != u)
                        && (u.getPartsNeedingFixing().size() > 0)) {
                    setBackground(new Color(238, 238, 0));
                } else if (u.getEntity() instanceof Infantry
                        && u.getActiveCrew().size() < u.getFullCrewSize()) {
                    setBackground(Color.RED);
                }
                else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }

    }
}