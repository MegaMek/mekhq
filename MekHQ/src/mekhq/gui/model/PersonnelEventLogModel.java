package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.campaign.Kill;
import mekhq.campaign.LogEntry;

public class PersonnelEventLogModel extends DataTableModel {
    private static final long serialVersionUID = 2930826794853379579L;

    private static final String EMPTY_CELL = ""; //$NON-NLS-1$

    public final static int COL_DATE = 0;
    public final static int COL_TEXT = 1;

    private ResourceBundle resourceMap;
    private SimpleDateFormat shortDateFormat;

    public PersonnelEventLogModel() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelEventLogModel"); //$NON-NLS-1$
        shortDateFormat = new SimpleDateFormat(resourceMap.getString("date.format")); //$NON-NLS-1$
        data = new ArrayList<Kill>();
    }
   
    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COL_TEXT + 1;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case COL_DATE:
                return resourceMap.getString("date.heading"); //$NON-NLS-1$
            case COL_TEXT:
                return resourceMap.getString("event.heading"); //$NON-NLS-1$
            default:
                return EMPTY_CELL;
        }
    }
    
    @Override
    public Object getValueAt(int row, int column) {
        LogEntry event = getEvent(row);
        switch(column) {
            case COL_DATE:
                return shortDateFormat.format(event.getDate());
            case COL_TEXT:
                return event.getDesc();
            default:
                return EMPTY_CELL;
        }
    }
    
    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }
    
    public LogEntry getEvent(int row) {
        if((row < 0) || (row >= data.size())) {
            return null;
        }
        return (LogEntry) data.get(row);
    }
    
    public int getAlignment(int column) {
        switch(column) {
            case COL_DATE:
                return SwingConstants.RIGHT;
            case COL_TEXT:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
    }
    
    public int getPreferredWidth(int column) {
        switch(column) {
            case COL_DATE:
                return 80;
            case COL_TEXT:
                return 300;
            default:
                return 100;
        }
    }
    
    public PersonnelEventLogModel.Renderer getRenderer() {
        return new PersonnelEventLogModel.Renderer();
    }
    
    public static class Renderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -2201201114822098877L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(((PersonnelEventLogModel)table.getModel()).getAlignment(column));

            setForeground(Color.BLACK);
            // tiger stripes
            if (row % 2 == 0) {
                setBackground(new Color(230,230,230));
            } else {
                setBackground(Color.WHITE);
            }
            return this;
        }
    }
}
