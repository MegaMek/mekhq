package mekhq.gui.model;

import javax.swing.table.DefaultTableModel;

public class RankTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_NAME = 0;
    public final static int COL_OFFICER = 1;
    public final static int COL_PAYMULT = 2;
    public final static int COL_LEVELS = 3;

    public RankTableModel(Object[][] ranksArray, String[] rankColNames) {
        super(ranksArray, rankColNames);
    }

    @Override
    public Class<?> getColumnClass(int c) {
    	switch (c) {
    		case COL_NAME:
    			return String.class;
    		case COL_OFFICER:
    			return Boolean.class;
    		case COL_PAYMULT:
    			return Double.class;
    		case COL_LEVELS:
    			return Integer.class;
    		default:
    			return getValueAt(0, c).getClass();
    	}
    }
}