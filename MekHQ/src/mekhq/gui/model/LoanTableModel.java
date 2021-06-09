/*
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved
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
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.MekHQ;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;

/**
 * A table model for displaying active loans
 */
public class LoanTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    public final static int COL_DESC      =    0;
    public final static int COL_RATE       =   1;
    public final static int COL_PRINCIPAL  =   2;
    public final static int COL_COLLATERAL =   3;
    public final static int COL_VALUE        = 4;
    public final static int COL_PAYMENT     =  5;
    public final static int COL_SCHEDULE   =   6;
    public final static int COL_NLEFT      =   7;
    public final static int COL_NEXT_PAY   =   8;
    public final static int N_COL            = 9;

    public LoanTableModel() {
        data = new ArrayList<Loan>();
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
            case COL_DESC:
                return "Description";
            case COL_COLLATERAL:
                return "Collateral";
            case COL_VALUE:
                return "Remaining";
            case COL_PAYMENT:
                return "Payment";
            case COL_NLEFT:
                return "# Left";
            case COL_NEXT_PAY:
                return "Next Payment Due";
            case COL_RATE:
                return "APR";
            case COL_SCHEDULE:
                return "Schedule";
            case COL_PRINCIPAL:
                return "Principal";
            default:
                return "?";
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        Loan loan = getLoan(row);
        if (col == COL_DESC) {
            return loan.getDescription();
        } else if (col == COL_COLLATERAL) {
            return loan.getCollateralAmount().toAmountAndSymbolString();
        } else if (col == COL_VALUE) {
            return loan.getRemainingValue().toAmountAndSymbolString();
        } else if (col == COL_PAYMENT) {
            return loan.getPaymentAmount().toAmountAndSymbolString();
        } else if (col == COL_PRINCIPAL) {
            return loan.getPrincipal().toAmountAndSymbolString();
        } else if (col == COL_SCHEDULE) {
            return Finances.getScheduleName(loan.getPaymentSchedule());
        } else if (col == COL_RATE) {
            return loan.getInterestRate() + "%";
        } else if (col == COL_NLEFT) {
            return loan.getRemainingPayments();
        } else if (col == COL_NEXT_PAY) {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(loan.getNextPayment());
        } else {
            return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_DESC:
                return 200;
            case COL_RATE:
                return 20;
            default:
                return 50;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_NLEFT:
            case COL_RATE:
                return SwingConstants.CENTER;
            case COL_DESC:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.RIGHT;
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

    public Loan getLoan(int row) {
        return (Loan) data.get(row);
    }

    public LoanTableModel.Renderer getRenderer() {
        return new LoanTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 9054581142945717303L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(getAlignment(column));
            Loan loan = getLoan(table.convertRowIndexToModel(row));

            setForeground(UIManager.getColor("Table.foreground"));
            if (isSelected) {
                setBackground(UIManager.getColor("Table.selectionBackground"));
                setForeground(UIManager.getColor("Table.selectionForeground"));
            } else {
                if (loan.isOverdue()) {
                    setForeground(MekHQ.getMekHQOptions().getLoanOverdueForeground());
                    setBackground(MekHQ.getMekHQOptions().getLoanOverdueBackground());
                } else {
                    setBackground(UIManager.getColor("Table.background"));
                }
            }

            return this;
        }
    }
}
