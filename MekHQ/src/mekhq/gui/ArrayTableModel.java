package mekhq.gui;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

/**
     * A table model for displaying work items
     */
    public abstract class ArrayTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 9081706049165214129L;
        protected String[] columnNames;
        protected ArrayList<?> data;

        public int getRowCount() {
            return data.size();
        }

        public int getColumnCount() {
            return 1;
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

        // fill table with values
        public void setData(ArrayList<?> array) {
            data = array;
            fireTableDataChanged();
        }

    }
