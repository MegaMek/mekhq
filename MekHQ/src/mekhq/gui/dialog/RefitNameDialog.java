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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import megamek.common.BattleArmor;
import megamek.common.Infantry;
import megamek.common.MechSummaryCache;
import mekhq.campaign.parts.Refit;

/**
 *
 * @author  Taharqa
 */
public class RefitNameDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Refit refit;
    private boolean cancelled;
    
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblChassis;
    private javax.swing.JTextField txtChassis;
    private javax.swing.JLabel lblModel;
    private javax.swing.JTextField txtModel;



    
    /** Creates new form NewTeamDialog */
    public RefitNameDialog(java.awt.Frame parent, boolean modal, Refit r) {
        super(parent, modal);
        this.frame = parent;
        this.refit = r;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtChassis = new javax.swing.JTextField();
        lblChassis = new javax.swing.JLabel();
        txtModel = new javax.swing.JTextField();
        lblModel = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
    
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RefitNameDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        lblChassis.setText(resourceMap.getString("lblChassis.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblChassis, gridBagConstraints);
        
        txtChassis.setText(refit.getNewEntity().getChassis());
        txtChassis.setMinimumSize(new java.awt.Dimension(150, 28));
        //only allow chassis renaming for conventional infantry
        if(!(refit.getNewEntity() instanceof Infantry) || refit.getNewEntity() instanceof BattleArmor) {
        	txtChassis.setEditable(false);
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
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
