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

import static mekhq.campaign.enums.DailyReportType.FINANCES;

import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import mekhq.MekHQ;
import mekhq.campaign.events.transactions.TransactionChangedEvent;
import mekhq.campaign.events.transactions.TransactionVoidedEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.EditTransactionDialog;
import mekhq.gui.model.FinanceTableModel;

public class FinanceTableMouseAdapter extends JPopupMenuAdapter {
    private final CampaignGUI gui;
    private final JTable financeTable;
    private final FinanceTableModel financeModel;

    protected FinanceTableMouseAdapter(CampaignGUI gui, JTable financeTable, FinanceTableModel financeModel) {
        this.gui = gui;
        this.financeTable = financeTable;
        this.financeModel = financeModel;
    }

    public static void connect(CampaignGUI gui, JTable financeTable, FinanceTableModel financeModel) {
        new FinanceTableMouseAdapter(gui, financeTable, financeModel)
              .connect(financeTable);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Transaction transaction = financeModel.getTransaction(financeTable.getSelectedRow());
        int row = financeTable.getSelectedRow();
        if (null == transaction) {
            return;
        }
        if (command.equalsIgnoreCase("DELETE")) {
            gui.getCampaign().addReport(FINANCES, transaction.voidTransaction());
            financeModel.deleteTransaction(row);
            gui.getCampaign().getFinances().clearCachedBalance();
            MekHQ.triggerEvent(new TransactionVoidedEvent(transaction));
        } else if (command.contains("EDIT")) {
            EditTransactionDialog dialog = new EditTransactionDialog(gui.getFrame(), transaction, true);
            dialog.setVisible(true);
            if (!transaction.equals(dialog.getOldTransaction())) {
                financeModel.setTransaction(row, transaction);
                gui.getCampaign().getFinances().clearCachedBalance();
                MekHQ.triggerEvent(new TransactionChangedEvent(dialog.getOldTransaction(), transaction));
                gui.getCampaign().addReport(FINANCES, transaction.updateTransaction(dialog.getOldTransaction()));
            }
        }
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        int row = financeTable.getSelectedRow();
        if ((row < 0) || !gui.getCampaign().isGM()) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        JMenu menu = new JMenu("GM Mode");
        popup.add(menu);

        JMenuItem deleteItem = new JMenuItem("Delete Transaction");
        deleteItem.setActionCommand("DELETE");
        deleteItem.addActionListener(this);
        menu.add(deleteItem);

        JMenuItem editItem = new JMenuItem("Edit Transaction");
        editItem.setActionCommand("EDIT");
        editItem.addActionListener(this);
        menu.add(editItem);

        return Optional.of(popup);
    }
}
