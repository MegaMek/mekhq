/*
 * ResolveWizardFinalCheckDialog.java
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
import javax.swing.DefaultComboBoxModel;

import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Taharqa
 */
public class ResolveWizardFinalCheckDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JComboBox choiceStatus;
    private javax.swing.JScrollPane scrReport;
    private javax.swing.JScrollPane scrRecoveredUnits;
    private javax.swing.JScrollPane scrRecoveredPilots;
    private javax.swing.JScrollPane scrMissingUnits;
    private javax.swing.JScrollPane scrMissingPilots;
    private javax.swing.JScrollPane scrDeadPilots;
    private javax.swing.JScrollPane scrSalvage;
    private javax.swing.JTextArea txtInstructions;
    private javax.swing.JTextArea txtReport;
    private javax.swing.JTextArea txtRecoveredUnits;
    private javax.swing.JTextArea txtRecoveredPilots;
    private javax.swing.JTextArea txtMissingUnits;
    private javax.swing.JTextArea txtMissingPilots;
    private javax.swing.JTextArea txtDeadPilots;
    private javax.swing.JTextArea txtSalvage;
    private javax.swing.JLabel lblStatus;
	
    /** Creates new form NewTeamDialog */
    public ResolveWizardFinalCheckDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	panButtons = new javax.swing.JPanel();
    	btnNext = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        choiceStatus = new javax.swing.JComboBox();
        scrReport = new javax.swing.JScrollPane();
        scrRecoveredUnits = new javax.swing.JScrollPane();
        scrRecoveredPilots = new javax.swing.JScrollPane();
        scrMissingUnits = new javax.swing.JScrollPane();
        scrMissingPilots = new javax.swing.JScrollPane();
        scrDeadPilots = new javax.swing.JScrollPane();
        scrSalvage = new javax.swing.JScrollPane();
        txtInstructions = new javax.swing.JTextArea();
        txtReport = new javax.swing.JTextArea();
        txtRecoveredUnits = new javax.swing.JTextArea();
        txtRecoveredPilots = new javax.swing.JTextArea();
        txtMissingUnits = new javax.swing.JTextArea();
        txtMissingPilots = new javax.swing.JTextArea();
        txtDeadPilots = new javax.swing.JTextArea();
        txtSalvage = new javax.swing.JTextArea();
        lblStatus = new javax.swing.JLabel();
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveWizardFinalCheckDialog");
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
        txtInstructions.setPreferredSize(new Dimension(400,100));
        txtInstructions.setMinimumSize(new Dimension(400,100));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(txtInstructions, gridBagConstraints);
        
        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(lblStatus, gridBagConstraints);
        
        DefaultComboBoxModel statusModel = new DefaultComboBoxModel();
		for (int i = 1; i < Scenario.S_NUM; i++) {
			statusModel.addElement(Scenario.getStatusName(i));
		}
		choiceStatus.setModel(statusModel);
		choiceStatus.setName("choiceStatus"); // NOI18N
		choiceStatus.setSelectedIndex(0);     
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(choiceStatus, gridBagConstraints);
		
        txtReport.setText("");
        txtReport.setName("txtReport");
        txtReport.setEditable(true);
        txtReport.setLineWrap(true);
        txtReport.setWrapStyleWord(true);
        txtReport.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtReport.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrReport.setViewportView(txtReport);
        scrReport.setPreferredSize(new Dimension(400,200));
        scrReport.setMinimumSize(new Dimension(400,200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrReport, gridBagConstraints);
	         
        txtRecoveredUnits.setName("txtRecoveredUnits");
        txtRecoveredUnits.setText(resourceMap.getString("none"));
        String names = "";
        if(tracker.getRecoveredUnits().size() > 0) {
        	for(Unit u : tracker.getRecoveredUnits()) {
        		names += u.getEntity().getDisplayName() + "\n";
        	}
        	txtRecoveredUnits.setText(names);
        }
        txtRecoveredUnits.setEditable(false);
        txtRecoveredUnits.setLineWrap(true);
        txtRecoveredUnits.setWrapStyleWord(true);
        txtRecoveredUnits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrRecoveredUnits.setViewportView(txtRecoveredUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrRecoveredUnits, gridBagConstraints);
        
        txtRecoveredPilots.setName("txtRecoveredPilots");
        txtRecoveredPilots.setText(resourceMap.getString("none"));
        names = "";
        if(tracker.getRecoveredPersonnel().size() > 0) {
        	for(Person p : tracker.getRecoveredPersonnel()) {
        		names += p.getName() + "\n";
        	}
        	txtRecoveredPilots.setText(names);
        }
        txtRecoveredPilots.setEditable(false);
        txtRecoveredPilots.setLineWrap(true);
        txtRecoveredPilots.setWrapStyleWord(true);
        txtRecoveredPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrRecoveredPilots.setViewportView(txtRecoveredPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrRecoveredPilots, gridBagConstraints);
        
        txtMissingUnits.setName("txtMissingUnits");
        txtMissingUnits.setText(resourceMap.getString("none"));
        names = "";
        if(tracker.getMissingUnits().size() > 0) {
        	for(Unit u : tracker.getMissingUnits()) {
        		names += u.getEntity().getDisplayName() + "\n";
        	}
        	txtMissingUnits.setText(names);
        }
        txtMissingUnits.setEditable(false);
        txtMissingUnits.setLineWrap(true);
        txtMissingUnits.setWrapStyleWord(true);
        txtMissingUnits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtMissingUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrMissingUnits.setViewportView(txtMissingUnits);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrMissingUnits, gridBagConstraints);
        
        txtMissingPilots.setName("txtMissingPilots");
        txtMissingPilots.setText(resourceMap.getString("none"));
        names = "";
        if(tracker.getMissingPersonnel().size() > 0) {
        	for(Person p : tracker.getMissingPersonnel()) {
        		names += p.getName() + "\n";
        	}
        	txtMissingPilots.setText(names);
        }
        txtMissingPilots.setEditable(false);
        txtMissingPilots.setLineWrap(true);
        txtMissingPilots.setWrapStyleWord(true);
        txtMissingPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtMissingPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrMissingPilots.setViewportView(txtMissingPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrMissingPilots, gridBagConstraints);
        
        txtSalvage.setName("txtSalvage");
        txtSalvage.setText("None");
        names = "";
        if(tracker.getActualSalvage().size() > 0) {
        	for(Unit u : tracker.getActualSalvage()) {
        		names += u.getEntity().getDisplayName() + "\n";
        	}
        	txtSalvage.setText(names);
        }
        txtSalvage.setEditable(false);
        txtSalvage.setLineWrap(true);
        txtSalvage.setWrapStyleWord(true);
        txtSalvage.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtSalvagedUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrSalvage.setViewportView(txtSalvage);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrSalvage, gridBagConstraints);
        
        txtDeadPilots.setName("txtDeadPilots");
        txtDeadPilots.setText(resourceMap.getString("none"));
        names = "";
        if(tracker.getDeadPersonnel().size() > 0) {
        	for(Person p : tracker.getDeadPersonnel()) {
        		names += p.getName() + "\n";
        	}
        	txtDeadPilots.setText(names);
        }
        txtDeadPilots.setEditable(false);
        txtDeadPilots.setLineWrap(true);
        txtDeadPilots.setWrapStyleWord(true);
        txtDeadPilots.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(resourceMap.getString("txtDeadPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDeadPilots.setViewportView(txtDeadPilots);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrDeadPilots, gridBagConstraints);
        
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
	   	 gridBagConstraints.gridy = 5;
	   	 gridBagConstraints.gridwidth = 3;
	   	 gridBagConstraints.weightx = 1.0;
	   	 gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	   	 gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	   	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	   	 getContentPane().add(panButtons, gridBagConstraints);
	   	 
	   	 pack();
    }

    
    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {
    	tracker.resolveScenario(choiceStatus.getSelectedIndex()+1,txtReport.getText());
    	this.setVisible(false);  	
    }


    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
    	this.setVisible(false);
    }
}
