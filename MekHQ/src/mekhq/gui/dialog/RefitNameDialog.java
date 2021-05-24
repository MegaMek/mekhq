/*
 * RefitNameDialog.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.MechSummaryCache;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.parts.Refit;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class RefitNameDialog extends JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    @SuppressWarnings("unused") // FIXME
	private Frame frame;
    private Refit refit;
    private boolean cancelled;

    private JButton btnCancel;
    private JButton btnOK;
    private JLabel lblChassis;
    private JTextField txtChassis;
    private JLabel lblModel;
    private JTextField txtModel;

    /** Creates new form RefitTeamDialog */
    public RefitNameDialog(Frame parent, boolean modal, Refit r) {
        super(parent, modal);
        this.frame = parent;
        this.refit = r;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtChassis = new JTextField();
        lblChassis = new JLabel();
        txtModel = new JTextField();
        lblModel = new JLabel();
        btnOK = new JButton();
        btnCancel = new JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RefitNameDialog", new EncodeControl());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new GridBagLayout());

        lblChassis.setText(resourceMap.getString("lblChassis.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(lblChassis, gridBagConstraints);

        txtChassis.setText(refit.getNewEntity().getChassis());
        txtChassis.setMinimumSize(new Dimension(150, 28));
        //only allow chassis renaming for conventional infantry
        if (!refit.getNewEntity().isConventionalInfantry()) {
        	txtChassis.setEditable(false);
        	txtChassis.setEnabled(false);
        }
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtChassis, gridBagConstraints);

        lblModel.setText(resourceMap.getString("lblModel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblModel, gridBagConstraints);

        txtModel.setText(refit.getNewEntity().getModel());
        txtModel.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtModel, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnClose"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(RefitNameDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	String chassis = txtChassis.getText().trim();
    	String model = txtModel.getText().trim();
    	if(chassis.isEmpty()) {
    		chassis = refit.getOriginalEntity().getChassis();
    	}
    	if(model.isEmpty()) {
    		model = refit.getOriginalEntity().getModel() + " Mk II";
    	}
    	if(null != MechSummaryCache.getInstance().getMech(chassis + " " + model)) {
    		JOptionPane.showMessageDialog(null,
					"There is already a unit in the database with this name.\nPlease select a different name.",
					"Name already in use",
					JOptionPane.ERROR_MESSAGE);
    		return;
    	}
    	refit.getNewEntity().setChassis(chassis);
    	refit.getNewEntity().setModel(model);
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	cancelled = true;
    	this.setVisible(false);
    }

    public boolean wasCancelled() {
    	return cancelled;
    }
}
