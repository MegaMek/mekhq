/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All rights reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.finances.Transaction;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.gui.utilities.JMoneyTextField;
import megamek.client.ui.preferences.PreferencesNode;

public class EditTransactionDialog extends JDialog implements ActionListener, FocusListener, MouseListener {
    private static final long serialVersionUID = -8742160448355293487L;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditTransactionDialog", new EncodeControl());

    private Transaction oldTransaction;
    private Transaction newTransaction;
    private JFrame parent;

    private JMoneyTextField amountField;
    private JTextField descriptionField;
    private JButton dateButton;
    private JComboBox<String> categoryCombo;

    private JButton saveButton;
    private JButton cancelButton;

    public EditTransactionDialog(JFrame parent, Transaction transaction, boolean modal) {
        super(parent, modal);
        //we need to make a copy of the object since objects are referenced by passing it to the dialog
        oldTransaction = new Transaction(transaction);
        newTransaction = transaction;
        this.parent = parent;

        initGUI();
        setTitle(resourceMap.getString("dialog.title"));
        setLocationRelativeTo(parent);
        pack();
        setUserPreferences();
    }

    private void initGUI() {
        setLayout(new BorderLayout());
        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(EditTransactionDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.BASELINE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(2,2,2,2);

        GridBagLayout l = new GridBagLayout();
        panel.setLayout(l);

        JLabel amountLabel = new JLabel("Amount");
        l.setConstraints(amountLabel, c);
        panel.add(amountLabel);

        c.gridx++;
        JLabel dateLabel = new JLabel("Date");
        l.setConstraints(dateLabel, c);
        panel.add(dateLabel);

        c.gridx++;
        JLabel categoryLabel = new JLabel("Category");
        l.setConstraints(categoryLabel, c);
        panel.add(categoryLabel);

        c.gridx++;
        JLabel descriptionLabel = new JLabel("Description");
        l.setConstraints(descriptionLabel, c);
        panel.add(descriptionLabel);

        c.gridx = 0;
        c.gridy++;
        amountField = new JMoneyTextField();
        amountField.addFocusListener(this);
        amountField.setMoney(newTransaction.getAmount());
        amountField.setToolTipText(resourceMap.getString("fundsQuantityField.toolTipText"));
        amountField.setName("amountField");
        amountField.setColumns(10);
        l.setConstraints(amountField, c);
        panel.add(amountField);

        c.gridx++;
        dateButton = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(newTransaction.getDate()));
        dateButton.addActionListener(this);
        l.setConstraints(dateButton, c);
        panel.add(dateButton);

        c.gridx++;
        categoryCombo = new JComboBox<>(Transaction.getCategoryList());
        categoryCombo.setSelectedItem(Transaction.getCategoryName(newTransaction.getCategory()));
        categoryCombo.setToolTipText("Category of the transaction");
        categoryCombo.setName("categoryCombo");
        l.setConstraints(categoryCombo, c);
        panel.add(categoryCombo);

        c.gridx++;
        descriptionField = new JTextField(newTransaction.getDescription());
        descriptionField.addFocusListener(this);
        descriptionField.setToolTipText("Description of the transaction.");
        descriptionField.setName("descriptionField");
        descriptionField.setColumns(10);
        l.setConstraints(descriptionField, c);
        panel.add(descriptionField);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        saveButton.setMnemonic('s');
        panel.add(saveButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setMnemonic('c');
        panel.add(cancelButton);

        return panel;
    }

    public Transaction getOldTransaction() {
        return oldTransaction;
    }

    public Transaction getNewTransaction() {
        return newTransaction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (saveButton.equals(e.getSource())) {
            newTransaction.setAmount(amountField.getMoney());
            newTransaction.setCategory(Transaction.getCategoryIndex((String) categoryCombo.getSelectedItem()));
            newTransaction.setDescription(descriptionField.getText());
            newTransaction.setDate(MekHQ.getMekHQOptions().parseDisplayFormattedDate(dateButton.getText()));
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        } else if (dateButton.equals(e.getSource())) {
            DateChooser chooser = new DateChooser(parent, newTransaction.getDate());
            if (chooser.showDateChooser() == DateChooser.OK_OPTION) {
                dateButton.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(chooser.getDate()));
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (amountField.equals(e.getSource())) {
            selectAllTextInField(amountField);
        } else if (descriptionField.equals(e.getSource())) {
            selectAllTextInField(descriptionField);
        }
    }

    private void selectAllTextInField(final JTextField field) {
        SwingUtilities.invokeLater(field::selectAll);
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
