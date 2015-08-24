package mekhq.gui.model;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.campaign.parts.Part;

/**
 * A table model for displaying parts
 */
public class PartsTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_QUANTITY   = 0;
    public final static int COL_NAME    =    1;
    public final static int COL_DETAIL   =   2;
    public final static int COL_TECH_BASE  = 3;
    public final static int COL_QUALITY    = 4;
    public final static int COL_STATUS   =   5;
    public final static int COL_REPAIR   =   6;
    public final static int COL_COST     =   7;
    public final static int COL_TON       =  8;
    public final static int N_COL          = 9;

    public PartsTableModel() {
        data = new ArrayList<Part>();
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
        case COL_COST:
            return "Value";
        case COL_QUANTITY:
            return "#";
        case COL_QUALITY:
            return "Quality";
        case COL_TON:
            return "Tonnage";
        case COL_STATUS:
            return "Status";
        case COL_DETAIL:
            return "Detail";
        case COL_TECH_BASE:
            return "Tech Base";
        case COL_REPAIR:
            return "Repair Details";
        default:
            return "?";
        }
    }

    public Object getValueAt(int row, int col) {
        Part part;
        if(data.isEmpty()) {
            return "";
        } else {
            part = (Part)data.get(row);
        }
        DecimalFormat format = new DecimalFormat();
        if(col == COL_NAME) {
            return "<html><nobr>"+part.getName()+"</nobr></html>";
        }
        if(col == COL_DETAIL) {
            return "<html><nobr>"+part.getDetails()+"</nobr></html>";
        }
        if(col == COL_COST) {
            return format.format(part.getActualValue());
        }
        if(col == COL_QUANTITY) {
            return part.getQuantity();
        }
        if(col == COL_QUALITY) {
            return Part.getQualityName(part.getQuality());
        }
        if(col == COL_TON) {
            return Math.round(part.getTonnage() * 100) / 100.0;
        }
        if(col == COL_STATUS) {
            return "<html><nobr>"+part.getStatus()+"</nobr></html>";
        }
        if(col == COL_TECH_BASE) {
            return part.getTechBaseName();
        }
        if(col == COL_REPAIR) {
            return "<html><nobr>"+part.getRepairDesc()+"</nobr></html>";
        }
        return "?";
    }

    public Part getPartAt(int row) {
        return ((Part) data.get(row));

    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_NAME:
        case COL_DETAIL:
            return 120;
        case COL_REPAIR:
            return 150;
        case COL_STATUS:
            return 40;
        case COL_TECH_BASE:
            return 20;
        case COL_COST:
            return 10;
        default:
            return 3;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        case COL_QUALITY:
        	return SwingConstants.CENTER;
        case COL_COST:
        case COL_TON:
            return SwingConstants.RIGHT;
        default:
            return SwingConstants.LEFT;
        }
    }

    public String getTooltip(int row, int col) {
        switch(col) {
        default:
            return null;
        }
    }
    public PartsTableModel.Renderer getRenderer() {
        return new PartsTableModel.Renderer();
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
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }

    }
}
