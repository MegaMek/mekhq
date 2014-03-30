package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class RankTableModel extends DefaultTableModel {
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

    public RankTableModel(Object[][] ranksArray, String[] rankColNames) {
        super(ranksArray, rankColNames);
    }

    @Override
    public Class<?> getColumnClass(int c) {
    	switch (c) {
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
    			return getValueAt(0, c).getClass();
    	}
    }
    
    public int getColumnWidth(int c) {
    	switch (c) {
    		case COL_NAME_RATE:
    			return 100;
        	case COL_NAME_MW:
        	case COL_NAME_ASF:
        	case COL_NAME_VEE:
        	case COL_NAME_NAVAL:
        	case COL_NAME_INF:
        	case COL_NAME_TECH:
        		return 500;
        	case COL_OFFICER:
        	case COL_PAYMULT:
        		return 250;
    		default:
    			return 500;
    	}
    }

    public int getAlignment(int col) {
        switch(col) {
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

    public String getTooltip(int row, int col) {
    	switch(col) {
			case COL_NAME_RATE:
				return "Rank's Rating";
	    	case COL_NAME_MW:
	    		return "Rank Name for MechWarriors (Used as the default if it doesn't exist for another job)";
	    	case COL_NAME_ASF:
	    		return "Rank Name for ASF Pilots";
	    	case COL_NAME_VEE:
	    		return "Rank Name for Vehicle Crewmen";
	    	case COL_NAME_NAVAL:
	    		return "Rank Name for Naval Personnel (Used as the default for ASF if that column isn't filled)";
	    	case COL_NAME_INF:
	    		return "Rank Name for Infantry Troopers";
	    	case COL_NAME_TECH:
	    		return "Rank Name for Technicians";
	        default:
	        	return "ERROR: Default Case Returned In RankTableModel.getTooltip!";
	    }
    }

    public TableCellRenderer getRenderer() {
        return new RankTableModel.Renderer();
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
            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
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
}