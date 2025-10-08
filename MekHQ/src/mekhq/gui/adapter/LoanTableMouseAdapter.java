/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.events.loans.LoanRemovedEvent;
import mekhq.campaign.events.parts.PartRemovedEvent;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.parts.Part;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.PayCollateralDialog;
import mekhq.gui.model.LoanTableModel;

public class LoanTableMouseAdapter extends JPopupMenuAdapter {
    private final CampaignGUI gui;
    private final JTable loanTable;
    private final LoanTableModel loanModel;

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
                                 "Defaulting on this loan will affect your unit rating.\nDo you wish to proceed?",
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
                                p.changeQuantity(-1);
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
        // **let's fill the pop-up menu**//
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
