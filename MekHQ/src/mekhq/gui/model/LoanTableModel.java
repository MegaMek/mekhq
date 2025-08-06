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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.MekHQ;
import mekhq.campaign.finances.Loan;

/**
 * A table model for displaying active loans
 */
public class LoanTableModel extends DataTableModel {
    public static final int COL_DESC = 0;
    public static final int COL_RATE = 1;
    public static final int COL_PRINCIPAL = 2;
    public static final int COL_COLLATERAL = 3;
    public static final int COL_VALUE = 4;
    public static final int COL_PAYMENT = 5;
    public static final int COL_SCHEDULE = 6;
    public static final int COL_NLEFT = 7;
    public static final int COL_NEXT_PAY = 8;
    public static final int N_COL = 9;

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
            return loan.toString();
        } else if (col == COL_COLLATERAL) {
            return loan.determineCollateralAmount().toAmountAndSymbolString();
        } else if (col == COL_VALUE) {
            return loan.determineRemainingValue().toAmountAndSymbolString();
        } else if (col == COL_PAYMENT) {
            return loan.getPaymentAmount().toAmountAndSymbolString();
        } else if (col == COL_PRINCIPAL) {
            return loan.getPrincipal().toAmountAndSymbolString();
        } else if (col == COL_SCHEDULE) {
            return loan.getFinancialTerm();
        } else if (col == COL_RATE) {
            return loan.getRate() + "%";
        } else if (col == COL_NLEFT) {
            return loan.getRemainingPayments();
        } else if (col == COL_NEXT_PAY) {
            return MekHQ.getMHQOptions().getDisplayFormattedDate(loan.getNextPayment());
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
                    setForeground(MekHQ.getMHQOptions().getLoanOverdueForeground());
                    setBackground(MekHQ.getMHQOptions().getLoanOverdueBackground());
                } else {
                    setBackground(UIManager.getColor("Table.background"));
                }
            }

            return this;
        }
    }
}
