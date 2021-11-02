/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All Rights Reserved
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * A table model for displaying financial transactions (i.e. a ledger)
 */
public class FinanceTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_DATE = 0;
    public final static int COL_CATEGORY = 1;
    public final static int COL_DESC = 2;
    public final static int COL_DEBIT = 3;
    public final static int COL_CREDIT = 4;
    public final static int COL_BALANCE = 5;
    public final static int N_COL = 6;

    public FinanceTableModel() {
        data = new ArrayList<Transaction>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
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

    @Override
    public Object getValueAt(int row, int col) {
        Transaction transaction = getTransaction(row);
        Money amount = transaction.getAmount();
        Money balance = Money.zero();
        for (int i = 0; i <= row; i++) {
            balance = balance.plus(getTransaction(i).getAmount());
        }

        if (col == COL_CATEGORY) {
            return transaction.getType();
        } else if (col == COL_DESC) {
            return transaction.getDescription();
        } else if (col == COL_DEBIT) {
            if (amount.isNegative()) {
                return amount.absolute().toAmountAndSymbolString();
            } else {
                return "";
            }
        } else if (col == COL_CREDIT) {
            if (amount.isPositive()) {
                return amount.toAmountAndSymbolString();
            } else {
                return "";
            }
        } else if (col == COL_BALANCE) {
            return balance.toAmountAndSymbolString();
        } else if (col == COL_DATE) {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(transaction.getDate());
        } else {
            return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_DESC:
                return 150;
            case COL_CATEGORY:
                return 100;
            default:
                return 50;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
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
        return (Transaction) data.get(row);
    }

    public void setTransaction(int row, Transaction transaction) {
        // FIXME
        // data.set(row, transaction);
    }

    public void deleteTransaction(int row) {
        data.remove(row);
    }

    public FinanceTableModel.Renderer getRenderer() {
        return new FinanceTableModel.Renderer();
    }

    public class Renderer extends MekHqTableCellRenderer {
        private static final long serialVersionUID = 9054581142945717303L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(getAlignment(column));

            return this;
        }
    }
}
