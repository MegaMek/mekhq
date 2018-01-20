package mekhq.gui.model;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

/**
 * An table model for displaying data in array lists
 */
public abstract class DataTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 9081706049165214129L;
    protected String[] columnNames;
    protected ArrayList<?> data;

    public int getRowCount() {
        return data.size();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<? extends Object> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public ArrayList<?> getData() {
        return data;
    }

    // fill table with values
    public void setData(ArrayList<?> array) {
        data = array;
        fireTableDataChanged();
    }

}
