package mekhq.gui.adapter;

import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.TransactionChangedEvent;
import mekhq.campaign.event.TransactionVoidedEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.EditTransactionDialog;
import mekhq.gui.model.FinanceTableModel;

public class FinanceTableMouseAdapter extends MouseInputAdapter {

	private static final ResourceBundle I18N = ResourceBundle.getBundle("mekhq.resources.FinanceTableMouseAdapter", new EncodeControl()); //$NON-NLS-1$

	public FinanceTableMouseAdapter(CampaignGUI gui, JTable financeTable, FinanceTableModel financeModel) {
		this.gui = gui;
		this.financeTable = financeTable;
		this.financeModel = financeModel;
	}

	private final CampaignGUI gui;
	private final JTable financeTable;
	private final FinanceTableModel financeModel;

	@Override public void  mousePressed(MouseEvent evt) { showPopup(evt); }
	@Override public void mouseReleased(MouseEvent evt) { showPopup(evt); }

	private void showPopup(MouseEvent me) {
		if (!me.isPopupTrigger()) return;

		int row = financeTable.getSelectedRow();
		if (row < 0) return;

		JPopupMenu popup = new JPopupMenu();
		JMenu menu = new JMenu(I18N.getString("menu.gmMode")); //$NON-NLS-1$
		popup.add(menu);

		menu.add(makeGMOnlyItem("DELETE", I18N.getString("menu.gmMode.action.delete"), (idx, tx) -> { //$NON-NLS-1$ //$NON-NLS-2$

			gui.getCampaign().addReport(tx.voidTransaction());
			financeModel.deleteTransaction(idx);
			MekHQ.triggerEvent(new TransactionVoidedEvent(tx));

		}));

		menu.add(makeGMOnlyItem("EDIT", I18N.getString("menu.gmMode.action.edit"), (idx, tx) -> { //$NON-NLS-1$ //$NON-NLS-2$

			EditTransactionDialog dialog = new EditTransactionDialog(tx, gui.getFrame(), true);
			dialog.setVisible(true);
			if (!tx.equals(dialog.getOldTransaction())) {
				financeModel.setTransaction(idx, tx);
				MekHQ.triggerEvent(new TransactionChangedEvent(dialog.getOldTransaction(), tx));
				gui.getCampaign().addReport(tx.updateTransaction(dialog.getOldTransaction()));
			}

		}));

		popup.show(me.getComponent(), me.getX(), me.getY());
	}

	private JMenuItem makeGMOnlyItem(String command, String label, BiConsumer<Integer, Transaction> listener) {
		JMenuItem deleteItem = new JMenuItem(label);
		deleteItem.setActionCommand(command);
		deleteItem.addActionListener(ae -> {
			int idx = financeTable.getSelectedRow();
			if (idx < 0) return;
			listener.accept(idx, financeModel.getTransaction(idx));
		});
		deleteItem.setEnabled(gui.getCampaign().isGM());
		return deleteItem;
	}

}
