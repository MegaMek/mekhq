package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.campaign.finances.Transaction;

/**
 * A table model for displaying financial transactions (i.e. a ledger)
 */
public class FinanceTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_DATE    =    0;
    public final static int COL_CATEGORY =   1;
    public final static int COL_DESC       = 2;
    public final static int COL_DEBIT     =  3;
    public final static int COL_CREDIT   =   4;
    public final static int COL_BALANCE  =   5;
    public final static int N_COL          = 6;

    public FinanceTableModel() {
        data = new ArrayList<Transaction>();
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
        case COL_DATE:
            return "Date";
        case COL_CATEGORY:
            return "Category";
        case COL_DESC:
            return "Notes";
        case COL_DEBIT:
            return "Debit";
        case COL_CREDIT:
            return "Credit";
        case COL_BALANCE:
            return "Balance";
        default:
            return "?";
        }
    }

    public Object getValueAt(int row, int col) {
        Transaction transaction = getTransaction(row);
        long amount = transaction.getAmount();
        long balance = 0;
        for(int i = 0; i <= row; i++) {
            balance += getTransaction(i).getAmount();
        }
        DecimalFormat formatter = new DecimalFormat();
        if(col == COL_CATEGORY) {
            return transaction.getCategoryName();
        }
        if(col == COL_DESC) {
            return transaction.getDescription();
        }
        if(col == COL_DEBIT) {
            if(amount < 0) {
                return formatter.format(-1 * amount);
            } else {
                return "";
            }
        }
        if(col == COL_CREDIT) {
            if(amount > 0) {
                return formatter.format(amount);
            } else {
                return "";
            }
        }
        if(col == COL_BALANCE) {
            return formatter.format(balance);
        }
        if(col == COL_DATE) {
            SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
            return shortDateFormat.format(transaction.getDate());
        }
        return "?";
    }

    public int getColumnWidth(int c) {
        switch(c) {
        case COL_DESC:
            return 150;
        case COL_CATEGORY:
            return 100;
        default:
            return 50;
        }
    }

    public int getAlignment(int col) {
        switch(col) {
        case COL_DEBIT:
        case COL_CREDIT:
        case COL_BALANCE:
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

    public Transaction getTransaction(int row) {
        return (Transaction)data.get(row);
    }

    public void setTransaction(int row, Transaction transaction) {
        //FIXME
        //data.set(row, transaction);
    }

    public void deleteTransaction(int row) {
        data.remove(row);
    }

    public FinanceTableModel.Renderer getRenderer() {
        return new FinanceTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 9054581142945717303L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(getAlignment(column));

            setForeground(Color.BLACK);
            if (isSelected) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            } else {
                // tiger stripes
                if (row % 2 == 1) {
                    setBackground(new Color(230,230,230));
                } else {
                    setBackground(Color.WHITE);
                }
            }
            return this;
        }

    }
}