/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.UUID;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import mekhq.MekHQ;
import mekhq.campaign.event.LoanRemovedEvent;
import mekhq.campaign.event.PartRemovedEvent;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.parts.Part;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.PayCollateralDialog;
import mekhq.gui.model.LoanTableModel;

public class LoanTableMouseAdapter extends JPopupMenuAdapter {
    private CampaignGUI gui;
    private JTable loanTable;
    private LoanTableModel loanModel;

    protected LoanTableMouseAdapter(CampaignGUI gui, JTable loanTable, LoanTableModel loanModel) {
        this.gui = gui;
        this.loanTable = loanTable;
        this.loanModel = loanModel;
    }

    public static void connect(CampaignGUI gui, JTable loanTable, LoanTableModel loanModel) {
        new LoanTableMouseAdapter(gui, loanTable, loanModel)
                .connect(loanTable);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        int row = loanTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Loan selectedLoan = loanModel.getLoan(loanTable
                .convertRowIndexToModel(row));
        if (null == selectedLoan) {
            return;
        }
        if (command.equalsIgnoreCase("DEFAULT")) {
            if (0 == JOptionPane
                    .showConfirmDialog(
                            null,
                            "Defaulting on this loan will affect your unit rating the same as a contract breach.\nDo you wish to proceed?",
                            "Default on " + selectedLoan + "?", JOptionPane.YES_NO_OPTION)) {
                PayCollateralDialog pcd = new PayCollateralDialog(
                        gui.getFrame(), true, gui.getCampaign(), selectedLoan);
                pcd.setVisible(true);
                if (pcd.wasCancelled()) {
                    return;
                }
                gui.getCampaign().getFinances().defaultOnLoan(selectedLoan,
                        pcd.wasPaid());
                if (pcd.wasPaid()) {
                    for (UUID id : pcd.getUnits()) {
                        gui.getCampaign().removeUnit(id);
                    }
                    for (int[] part : pcd.getParts()) {
                        Part p = gui.getCampaign().getPart(part[0]);
                        if (null != p) {
                            int quantity = part[1];
                            while (quantity > 0 && p.getQuantity() > 0) {
                                p.decrementQuantity();
                                quantity--;
                                MekHQ.triggerEvent(new PartRemovedEvent(p));
                            }
                        }
                    }
                    gui.getCampaign().getFinances().setAssets(
                            pcd.getRemainingAssets());
                }
            }
        } else if (command.equalsIgnoreCase("PAY_BALANCE")) {
            gui.getCampaign().payOffLoan(selectedLoan);
        } else if (command.equalsIgnoreCase("REMOVE")) {
            gui.getCampaign().getFinances().removeLoan(selectedLoan);
            MekHQ.triggerEvent(new LoanRemovedEvent(selectedLoan));
        }
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (loanTable.getSelectedRowCount() == 0) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        Loan loan = loanModel.getLoan(loanTable.convertRowIndexToModel(loanTable.getSelectedRow()));
        JMenuItem menuItem;
        JMenu menu;
        // **lets fill the pop up menu**//
        menuItem = new JMenuItem("Pay Off Full Balance ("
                + loan.determineRemainingValue().toAmountAndSymbolString() + ")");
        menuItem.setActionCommand("PAY_BALANCE");
        menuItem.setEnabled(gui.getCampaign().getFunds().isGreaterOrEqualThan(loan.determineRemainingValue()));
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Default on This Loan");
        menuItem.setActionCommand("DEFAULT");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        if (gui.getCampaign().isGM()) {
            // GM mode
            menu = new JMenu("GM Mode");
            // remove part
            menuItem = new JMenuItem("Remove Loan");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menu.add(menuItem);
            // end
            popup.addSeparator();
            popup.add(menu);
        }

        return Optional.of(popup);
    }
}
