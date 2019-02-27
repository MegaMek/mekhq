/*
 * EditAssetDialog.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.finances.Asset;
import mekhq.campaign.finances.Finances;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.JMoneyTextField;
import mekhq.preferences.PreferencesNode;

/**
 *
 * @author  Taharqa
 */
public class EditAssetDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;
    private Asset asset;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditAssetDialog", new EncodeControl()); //$NON-NLS-1$
    private JButton btnClose;
    private JButton btnOK;
    private JTextField txtName;
    private JMoneyTextField assetValueField;
    private JMoneyTextField assetIncomeField;
    private JComboBox<String> choiceSchedule;
    boolean cancelled;
    
    public EditAssetDialog(Frame parent, Asset a) {
        super(parent, true);
        this.asset = a;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelName.text")), gridBagConstraints);

        txtName = new JTextField();
        txtName.setText(asset.getName());
        txtName.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtName, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelValue.text")), gridBagConstraints);

        assetValueField = new JMoneyTextField(() -> btnOKActionPerformed(null));
        assetValueField.setMoney(asset.getValue());
        assetValueField.setToolTipText(resourceMap.getString("assetValueField.toolTipText")); // NOI18N
        assetValueField.setName("assetValueField"); // NOI18N
        assetValueField.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(assetValueField, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel(resourceMap.getString("labelIncome.text")), gridBagConstraints);

        assetIncomeField = new JMoneyTextField(() -> btnOKActionPerformed(null));
        assetIncomeField.setMoney(asset.getIncome());
        assetIncomeField.setToolTipText(resourceMap.getString("assetIncomeField.toolTipText")); // NOI18N
        assetIncomeField.setName("assetIncomeField"); // NOI18N
        assetIncomeField.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(assetIncomeField, gridBagConstraints);
 
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(new JLabel("Income Schedule:"), gridBagConstraints);
        
        DefaultComboBoxModel<String> scheduleModel = new DefaultComboBoxModel<>();
        scheduleModel.addElement(Finances.getScheduleName(Finances.SCHEDULE_MONTHLY));
        scheduleModel.addElement(Finances.getScheduleName(Finances.SCHEDULE_YEARLY));
        choiceSchedule = new JComboBox<>(scheduleModel);
        choiceSchedule.setSelectedIndex(0);
        if(asset.getSchedule() == Finances.SCHEDULE_YEARLY) {
            choiceSchedule.setSelectedIndex(1);
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(choiceSchedule, gridBagConstraints);

        btnOK = new javax.swing.JButton();
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setActionCommand(resourceMap.getString("btnOK.actionCommand"));
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(evt -> btnOKActionPerformed(evt));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose = new javax.swing.JButton();
        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setActionCommand(resourceMap.getString("btnClose.actionCommand")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(evt -> btnCloseActionPerformed(evt));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(EditAssetDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        asset.setName(txtName.getText());
        try {
            asset.setValue(assetValueField.getMoney());
        } catch(Exception ignored) {
            
        }
        try {
            asset.setIncome(assetIncomeField.getMoney());
        } catch(Exception ignored) {
            
        }
        if(choiceSchedule.getSelectedIndex() == 1) {
            asset.setSchedule(Finances.SCHEDULE_YEARLY);
        } else {
            asset.setSchedule(Finances.SCHEDULE_MONTHLY);
        }
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        cancelled = true;
        this.setVisible(false);
    }
 
    public boolean wasCancelled() {
        return cancelled;
    }
}
