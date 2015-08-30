/*
 * ResolveWizardChooseFilesDialog.java
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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

import mekhq.campaign.ResolveScenarioTracker;

/**
 *
 * @author  Taharqa
 */
public class ChooseMulFilesDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;

	private ResolveScenarioTracker tracker;

	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JButton btnUnitFile;
    private javax.swing.JTextField txtUnitFile;
    private javax.swing.JTextArea txtInstructions;
    private boolean cancelled;

    /** Creates new form NewTeamDialog */
    public ChooseMulFilesDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

    	 panButtons = new javax.swing.JPanel();
    	 btnNext = new javax.swing.JButton();
    	 btnCancel = new javax.swing.JButton();
    	 btnUnitFile = new javax.swing.JButton();
    	 txtUnitFile = new javax.swing.JTextField();
    	 new javax.swing.JButton();
    	 new javax.swing.JTextField();
    	 txtInstructions = new javax.swing.JTextArea();

    	 ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseMulFilesDialog");
    	 setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    	 setName("Form"); // NOI18N

    	 getContentPane().setLayout(new java.awt.GridBagLayout());
    	 setTitle(resourceMap.getString("title"));

    	 txtInstructions.setText(resourceMap.getString("txtInstructions.text"));
    	 txtInstructions.setName("txtInstructions");
    	 txtInstructions.setEditable(false);
    	 txtInstructions.setEditable(false);
    	 txtInstructions.setLineWrap(true);
    	 txtInstructions.setWrapStyleWord(true);
    	 txtInstructions.setBorder(BorderFactory.createCompoundBorder(
    			 BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
    			 BorderFactory.createEmptyBorder(5,5,5,5)));
    	 txtInstructions.setPreferredSize(new Dimension(400,200));
    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 0;
    	 gridBagConstraints.gridy = 0;
    	 gridBagConstraints.gridwidth = 2;
    	 gridBagConstraints.weightx = 1.0;
    	 gridBagConstraints.weighty = 0.0;
    	 gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 getContentPane().add(txtInstructions, gridBagConstraints);

    	 btnUnitFile.setText(resourceMap.getString("btnUnitFile.text")); // NOI18N
    	 btnUnitFile.setName("btnUnitFile"); // NOI18N
    	 btnUnitFile.addActionListener(new java.awt.event.ActionListener() {
    		 public void actionPerformed(java.awt.event.ActionEvent evt) {
    			 tracker.findUnitFile();
    			 txtUnitFile.setText(tracker.getUnitFilePath());
    		 }
    	 });
    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 0;
    	 gridBagConstraints.gridy = 1;
    	 gridBagConstraints.gridwidth = 1;
    	 gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 getContentPane().add(btnUnitFile, gridBagConstraints);

    	 txtUnitFile.setText(tracker.getUnitFilePath());
    	 txtUnitFile.setName("txtUnitFile");
    	 txtUnitFile.setEditable(false);
    	 txtUnitFile.setOpaque(false);
    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 1;
    	 gridBagConstraints.gridy = 1;
    	 gridBagConstraints.gridwidth = 1;
    	 gridBagConstraints.weightx = 1.0;
    	 gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 getContentPane().add(txtUnitFile, gridBagConstraints);

    	 panButtons.setName("panButtons");
    	 panButtons.setLayout(new GridBagLayout());

    	 btnNext.setText(resourceMap.getString("btnNext.text")); // NOI18N
    	 btnNext.setName("btnNext"); // NOI18N
    	 btnNext.addActionListener(new java.awt.event.ActionListener() {
    		 public void actionPerformed(java.awt.event.ActionEvent evt) {
    			 btnNextActionPerformed(evt);
    		 }
    	 });
    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 0;
    	 gridBagConstraints.gridy = 0;
    	 gridBagConstraints.gridwidth = 1;
    	 gridBagConstraints.weightx = 1.0;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 panButtons.add(btnCancel, gridBagConstraints);

    	 btnCancel.setText(resourceMap.getString("btnCancel.text")); // NOI18N
    	 btnCancel.setName("btnClose"); // NOI18N
    	 btnCancel.addActionListener(new java.awt.event.ActionListener() {
    		 public void actionPerformed(java.awt.event.ActionEvent evt) {
    			 btnCancelActionPerformed(evt);
    		 }
    	 });
    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 1;
    	 gridBagConstraints.gridy = 0;
    	 gridBagConstraints.gridwidth = 1;
    	 gridBagConstraints.weightx = 0.0;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 panButtons.add(btnNext, gridBagConstraints);

    	 gridBagConstraints = new java.awt.GridBagConstraints();
    	 gridBagConstraints.gridx = 0;
    	 gridBagConstraints.gridy = 2;
    	 gridBagConstraints.gridwidth = 2;
    	 gridBagConstraints.weightx = 1.0;
    	 gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    	 gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    	 getContentPane().add(panButtons, gridBagConstraints);

    	 pack();
    }


    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	tracker.processMulFiles();
    	this.setVisible(false);
    }


    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	cancelled = true;
    	this.setVisible(false);
    }

    public boolean wasCancelled() {
    	return cancelled;
    }
}
