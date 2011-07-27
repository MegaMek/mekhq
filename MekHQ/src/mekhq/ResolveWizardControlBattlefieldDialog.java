/*
 * ResolveWizardControlBattlefieldDialog.java
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

package mekhq;

import java.awt.Frame;
import java.awt.GridBagLayout;

import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.mission.Contract;

/**
 *
 * @author  Taharqa
 */
public class ResolveWizardControlBattlefieldDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnNo;
    private javax.swing.JButton btnYes;
    private javax.swing.JLabel lblQuestion;

	
    /** Creates new form NewTeamDialog */
    public ResolveWizardControlBattlefieldDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	panButtons = new javax.swing.JPanel();
    	btnYes = new javax.swing.JButton();
        btnNo = new javax.swing.JButton();
        lblQuestion = new javax.swing.JLabel();
     
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(ResolveWizardControlBattlefieldDialog.class);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        setTitle(resourceMap.getString("title"));
        
        lblQuestion.setText(resourceMap.getString("txtInstructions.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(lblQuestion, gridBagConstraints);
        
        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());  
        
        btnYes.setText(resourceMap.getString("btnNext.text")); // NOI18N
        btnYes.setName("btnNext"); // NOI18N
        btnYes.addActionListener(new java.awt.event.ActionListener() {
	   		 public void actionPerformed(java.awt.event.ActionEvent evt) {
	   			 resolve(true);
	   		 }
	   	 });
	   	 gridBagConstraints = new java.awt.GridBagConstraints();
	   	 gridBagConstraints.gridx = 1;
	   	 gridBagConstraints.gridy = 0;
	   	 gridBagConstraints.gridwidth = 1;
	   	 gridBagConstraints.weightx = 0.0;
	   	 gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
	   	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	   	 panButtons.add(btnNo, gridBagConstraints);
		
	   	 btnNo.setText(resourceMap.getString("btnCancel.text")); // NOI18N
	   	 btnNo.setName("btnClose"); // NOI18N
	   	 btnNo.addActionListener(new java.awt.event.ActionListener() {
	   		 public void actionPerformed(java.awt.event.ActionEvent evt) {
	   			resolve(false);
	   		 }
	   	 });
	   	 gridBagConstraints = new java.awt.GridBagConstraints();
	   	 gridBagConstraints.gridx = 0;
	   	 gridBagConstraints.gridy = 0;
	   	 gridBagConstraints.gridwidth = 1;
	   	 gridBagConstraints.weightx = 1.0;
	   	 gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
	   	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	   	 panButtons.add(btnYes, gridBagConstraints);
		
	   	 gridBagConstraints = new java.awt.GridBagConstraints();
	   	 gridBagConstraints.gridx = 0;
	   	 gridBagConstraints.gridy = 1;
	   	 gridBagConstraints.gridwidth = 1;
	   	 gridBagConstraints.weightx = 1.0;
	   	 gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	   	 gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
	   	 gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	   	 getContentPane().add(panButtons, gridBagConstraints);
	   	 
	   	 pack();
    }

    
    private void resolve(boolean control) {
    	tracker.processGame(control);
    	this.setVisible(false);
    	if(tracker.getMissingUnits().size() > 0) {
    		ResolveWizardMissingUnitsDialog resolveDialog = new ResolveWizardMissingUnitsDialog((Frame)getParent(), true, tracker);
    		resolveDialog.setVisible(true);
    	} else if(!tracker.getPeopleStatus().isEmpty()) {
    		ResolveWizardPilotStatusDialog resolveDialog = new ResolveWizardPilotStatusDialog((Frame)getParent(), true, tracker);
    		resolveDialog.setVisible(true);
    	} else if(tracker.getPotentialSalvage().size() > 0 
    			&& (!(tracker.getMission() instanceof Contract) || ((Contract)tracker.getMission()).canSalvage())) {
    		ResolveWizardSalvageDialog resolveDialog = new ResolveWizardSalvageDialog((Frame)getParent(), true, tracker);
    		resolveDialog.setVisible(true);
    	} else {
    		ResolveWizardFinalCheckDialog resolveDialog = new ResolveWizardFinalCheckDialog((Frame)getParent(), true, tracker);
    		resolveDialog.setVisible(true);
    	}
    }
} 
