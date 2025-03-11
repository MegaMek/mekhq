/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui.model;

import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * A table model for displaying financial transactions (i.e. a ledger)
 */
public class FinanceTableModel extends DataTableModel {
    public static final int COL_DATE = 0;
    public static final int COL_CATEGORY = 1;
    public static final int COL_DESC = 2;
    public static final int COL_DEBIT = 3;
    public static final int COL_CREDIT = 4;
    public static final int COL_BALANCE = 5;
    public static final int N_COL = 6;

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
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return "";
        }
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
            return MekHQ.getMHQOptions().getDisplayFormattedDate(transaction.getDate());
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
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(getAlignment(column));

            return this;
        }
    }
}
