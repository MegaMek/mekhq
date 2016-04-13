package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import megamek.common.util.EncodeControl;
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
        resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelEventLogModel", new EncodeControl()); //$NON-NLS-1$
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
                return StyleConstants.ALIGN_RIGHT;
            case COL_TEXT:
                return StyleConstants.ALIGN_LEFT;
            default:
                return StyleConstants.ALIGN_CENTER;
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
    
    public boolean hasConstantWidth(int col) {
        switch(col) {
            case COL_DATE:
                return true;
            default:
                return false;
        }
    }
    
    public PersonnelEventLogModel.Renderer getRenderer() {
        return new PersonnelEventLogModel.Renderer();
    }
    
    public static class Renderer extends JTextPane implements TableCellRenderer {
        private static final long serialVersionUID = -2201201114822098877L;

        private final SimpleAttributeSet attribs = new SimpleAttributeSet();
        private final FontMetrics metrics;

        public Renderer() {
            super();
            setOpaque(true);
            setFont(UIManager.getDefaults().getFont("TabbedPane.font")); //$NON-NLS-1$
            metrics = getFontMetrics(getFont());
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((String) value);
            StyleConstants.setAlignment(attribs, ((PersonnelEventLogModel)table.getModel()).getAlignment(column));
            setParagraphAttributes(attribs, false);

            int fontHeight = metrics.getHeight();
            int textLength = metrics.stringWidth(getText()) + 10;
            int lines = (int) Math.ceil(1.0 * textLength / table.getColumnModel().getColumn(column).getWidth());
            if (lines == 0) {
                lines = 1;
            }
            // check for new lines
            int newLines = getText().split("\r\n|\r|\n").length; //$NON-NLS-1$
            lines = Math.max(lines, newLines);

            int height = fontHeight * lines + 4;
            table.setRowHeight(row, height);

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
