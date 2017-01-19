package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import mekhq.MekHQ;
import mekhq.campaign.event.TransactionChangedEvent;
import mekhq.campaign.event.TransactionVoidedEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.EditTransactionDialog;
import mekhq.gui.model.FinanceTableModel;

public class FinanceTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {
    private CampaignGUI gui;
    private JTable financeTable;
    private FinanceTableModel financeModel;

    public FinanceTableMouseAdapter(CampaignGUI gui,
            JTable financeTable,
            FinanceTableModel financeModel) {
        super();
        this.gui = gui;
        this.financeTable = financeTable;
        this.financeModel = financeModel;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Transaction transaction = financeModel.getTransaction(financeTable
                .getSelectedRow());
        int row = financeTable.getSelectedRow();
        if (null == transaction) {
            return;
        }
        if (command.equalsIgnoreCase("DELETE")) {
            gui.getCampaign().addReport(transaction.voidTransaction());
            financeModel.deleteTransaction(row);
            MekHQ.triggerEvent(new TransactionVoidedEvent(transaction));
            gui.refreshReport();
        } else if (command.contains("EDIT")) {
            EditTransactionDialog dialog = new EditTransactionDialog(
                    transaction, gui.getFrame(), true);
            dialog.setVisible(true);
            transaction = dialog.getNewTransaction();
            financeModel.setTransaction(row, transaction);
            MekHQ.triggerEvent(new TransactionChangedEvent(dialog.getOldTransaction(), transaction));
            gui.getCampaign().addReport(
                    transaction.updateTransaction(dialog
                            .getOldTransaction()));
            gui.refreshReport();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger()) {
            int row = financeTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            JMenu menu = new JMenu("GM Mode");
            popup.add(menu);

            JMenuItem deleteItem = new JMenuItem("Delete Transaction");
            deleteItem.setActionCommand("DELETE");
            deleteItem.addActionListener(this);
            deleteItem.setEnabled(gui.getCampaign().isGM());
            menu.add(deleteItem);

            JMenuItem editItem = new JMenuItem("Edit Transaction");
            editItem.setActionCommand("EDIT");
            editItem.addActionListener(this);
            editItem.setEnabled(gui.getCampaign().isGM());
            menu.add(editItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
