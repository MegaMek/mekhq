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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.gui.utilities.JMoneyTextField;

public class EditTransactionDialog extends JDialog implements ActionListener, FocusListener, MouseListener {
    private static final MMLogger LOGGER = MMLogger.create(EditTransactionDialog.class);

    private final Transaction oldTransaction;
    private final Transaction newTransaction;
    private final JFrame parent;

    private JMoneyTextField amountField;
    private JTextField descriptionField;
    private JButton dateButton;
    private MMComboBox<TransactionType> categoryCombo;

    private JButton saveButton;
    private JButton cancelButton;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle(
          "mekhq.resources.EditTransactionDialog",
          MekHQ.getMHQOptions().getLocale());

    public EditTransactionDialog(JFrame parent, Transaction transaction, boolean modal) {
        super(parent, modal);
        // we need to make a copy of the object since objects are referenced by passing
        // it to the dialog
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

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditTransactionDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
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
        c.insets = new Insets(2, 2, 2, 2);

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
        dateButton = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(newTransaction.getDate()));
        dateButton.addActionListener(this);
        l.setConstraints(dateButton, c);
        panel.add(dateButton);

        c.gridx++;
        categoryCombo = new MMComboBox<>("categoryCombo", TransactionType.values());
        categoryCombo.setSelectedItem(newTransaction.getType());
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (saveButton.equals(e.getSource())) {
            newTransaction.setAmount(amountField.getMoney());
            newTransaction.setType(categoryCombo.getSelectedItem());
            newTransaction.setDescription(descriptionField.getText());
            newTransaction.setDate(MekHQ.getMHQOptions().parseDisplayFormattedDate(dateButton.getText()));
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        } else if (dateButton.equals(e.getSource())) {
            DateChooser chooser = new DateChooser(parent, newTransaction.getDate());
            if (chooser.showDateChooser() == DateChooser.OK_OPTION) {
                dateButton.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(chooser.getDate()));
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
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // To change body of implemented methods use File | Settings | File Templates.
    }
}
