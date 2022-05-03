/*
 * EditAssetDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.gui.utilities.JMoneyTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * @author Taharqa
 */
public class EditAssetDialog extends JDialog {
    private Asset asset;

    private JButton btnClose;
    private JButton btnOK;
    private JTextField txtName;
    private JMoneyTextField assetValueField;
    private JMoneyTextField assetIncomeField;
    private MMComboBox<FinancialTerm> choiceSchedule;
    boolean cancelled;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditAssetDialog",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());

    public EditAssetDialog(Frame parent, Asset a) {
        super(parent, true);
        this.asset = a;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelName.text")), gridBagConstraints);

        txtName = new JTextField();
        txtName.setText(asset.getName());
        txtName.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(txtName, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelValue.text")), gridBagConstraints);

        assetValueField = new JMoneyTextField();
        assetValueField.setMoney(asset.getValue());
        assetValueField.setToolTipText(resourceMap.getString("assetValueField.toolTipText"));
        assetValueField.setName("assetValueField");
        assetValueField.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(assetValueField, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelIncome.text")), gridBagConstraints);

        assetIncomeField = new JMoneyTextField();
        assetIncomeField.setMoney(asset.getIncome());
        assetIncomeField.setToolTipText(resourceMap.getString("assetIncomeField.toolTipText"));
        assetIncomeField.setName("assetIncomeField");
        assetIncomeField.setMinimumSize(new Dimension(150, 28));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(assetIncomeField, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Income Schedule:"), gridBagConstraints);

        choiceSchedule = new MMComboBox<>("choiceSchedule", FinancialTerm.values());
        choiceSchedule.setSelectedItem(asset.getFinancialTerm());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(choiceSchedule, gridBagConstraints);

        btnOK = new JButton();
        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setActionCommand(resourceMap.getString("btnOK.actionCommand"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose = new JButton();
        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setActionCommand(resourceMap.getString("btnClose.actionCommand"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditAssetDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        asset.setName(txtName.getText());
        try {
            asset.setValue(assetValueField.getMoney());
        } catch (Exception ignored) {

        }
        try {
            asset.setIncome(assetIncomeField.getMoney());
        } catch (Exception ignored) {

        }

        asset.setFinancialTerm(choiceSchedule.getSelectedItem());
        setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        cancelled = true;
        this.setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
