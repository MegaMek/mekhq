package mekhq.gui.model;

import javax.swing.table.DefaultTableModel;

public class RankTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_NAME_MW		= 0;
    public final static int COL_NAME_ASF	= 1;
    public final static int COL_NAME_VEE	= 2;
    public final static int COL_NAME_NAVAL	= 3;
    public final static int COL_NAME_INF	= 4;
    public final static int COL_NAME_TECH	= 5;
    public final static int COL_OFFICER		= 6;
    public final static int COL_PAYMULT		= 7;
    public final static int COL_NUM			= 8;

    public RankTableModel(Object[][] ranksArray, String[] rankColNames) {
        super(ranksArray, rankColNames);
    }

    @Override
    public Class<?> getColumnClass(int c) {
    	switch (c) {
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
}