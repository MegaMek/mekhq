/*
 * ResolveWizardMissingUnitsDialog.java
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.PilotPerson;
/**
 *
 * @author  Taharqa
 */
public class ResolveWizardMissingPilotsDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JScrollPane scrMissingPilots;
	private javax.swing.JPanel panMissingPilots;
    private javax.swing.JTextArea txtInstructions;
    private ArrayList<JRadioButton> activeBtns;
    private ArrayList<JRadioButton> miaBtns;
    private ArrayList<JRadioButton> kiaBtns;

	
    /** Creates new form NewTeamDialog */
    public ResolveWizardMissingPilotsDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	panButtons = new javax.swing.JPanel();
    	panMissingPilots = new javax.swing.JPanel();
    	btnNext = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        scrMissingPilots = new javax.swing.JScrollPane();
        txtInstructions = new javax.swing.JTextArea();
        activeBtns = new ArrayList<JRadioButton>();
        miaBtns = new ArrayList<JRadioButton>();
        kiaBtns = new ArrayList<JRadioButton>();
       
     
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(ResolveWizardMissingPilotsDialog.class);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        setTitle(resourceMap.getString("title"));
        
        txtInstructions.setText(resourceMap.getString("txtInstructions.text"));
        txtInstructions.setName("txtInstructions");
        txtInstructions.setEditable(false);
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Instructions"),
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
	
        panMissingPilots.setName("panMissingPilots");
        panMissingPilots.setLayout(new GridBagLayout()); 
        
        int i = 1;
        JLabel nameLbl;
        JRadioButton activeButton;
        JRadioButton miaButton;
        JRadioButton kiaButton; 
        ButtonGroup group;
        for(PilotPerson pp : tracker.getMissingPilots()) {
        	nameLbl = new JLabel(pp.getFullTitle());
        	activeButton = new JRadioButton("Active");
        	activeBtns.add(activeButton);
        	miaButton = new JRadioButton("MIA");
        	miaButton.setSelected(true);
        	miaBtns.add(miaButton);
        	kiaButton = new JRadioButton("KIA"); 
        	kiaBtns.add(kiaButton);
        	group = new ButtonGroup();
        	group.add(activeButton);
        	group.add(miaButton);
        	group.add(kiaButton);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMissingPilots.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            panMissingPilots.add(activeButton, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            panMissingPilots.add(miaButton, gridBagConstraints);
            gridBagConstraints.gridx = 3;
            panMissingPilots.add(kiaButton, gridBagConstraints);
            i++;
        }              
        scrMissingPilots.setViewportView(panMissingPilots);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrMissingPilots, gridBagConstraints);
        
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

    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {
    	for(int i = 0; i < activeBtns.size(); i++) {
    		JRadioButton activeBtn = activeBtns.get(i);
    		if(activeBtn.getModel().isSelected()) {
    			tracker.recoverMissingPilot(i);
    		}
    	}
    	tracker.identifyMissingPilots();
    	this.setVisible(false);
    	tracker.checkForCasualties();
    	if(tracker.getDeadPilots().size() > 0) {
    		ResolveWizardCasualtiesDialog resolveDialog = new ResolveWizardCasualtiesDialog((Frame)getParent(), true, tracker);
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


    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
    	this.setVisible(false);
    }
}
