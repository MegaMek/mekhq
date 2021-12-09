/*
 * Copyright (c) 2014, 2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import mekhq.MekHQ;
import mekhq.campaign.event.TransactionChangedEvent;
import mekhq.campaign.event.TransactionVoidedEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.EditTransactionDialog;
import mekhq.gui.model.FinanceTableModel;

public class FinanceTableMouseAdapter extends JPopupMenuAdapter {
    private CampaignGUI gui;
    private JTable financeTable;
    private FinanceTableModel financeModel;

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
            gui.getCampaign().addReport(transaction.voidTransaction());
            financeModel.deleteTransaction(row);
            MekHQ.triggerEvent(new TransactionVoidedEvent(transaction));
        } else if (command.contains("EDIT")) {
            EditTransactionDialog dialog = new EditTransactionDialog(gui.getFrame(), transaction, true);
            dialog.setVisible(true);
            if (!transaction.equals(dialog.getOldTransaction())) {
	            financeModel.setTransaction(row, transaction);
	            MekHQ.triggerEvent(new TransactionChangedEvent(dialog.getOldTransaction(), transaction));
	            gui.getCampaign().addReport(transaction.updateTransaction(dialog.getOldTransaction()));
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
