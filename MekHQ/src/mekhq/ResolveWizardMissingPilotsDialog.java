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
import javax.swing.JCheckBox;

import mekhq.campaign.Unit;
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
    private ArrayList<javax.swing.JCheckBox> boxes;
	
    /** Creates new form NewTeamDialog */
    public ResolveWizardMissingPilotsDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        initComponents();
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	panButtons = new javax.swing.JPanel();
    	panMissingPilots = new javax.swing.JPanel();
    	btnNext = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        scrMissingPilots = new javax.swing.JScrollPane();
        txtInstructions = new javax.swing.JTextArea();
        boxes = new ArrayList<javax.swing.JCheckBox>();

     
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
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(txtInstructions, gridBagConstraints);
	
        panMissingPilots.setName("panMissingPilots");
        panMissingPilots.setLayout(new GridBagLayout()); 
        
        JCheckBox box;
        int i = 1;
        for(PilotPerson pp : tracker.getMissingPilots()) {
        	box = new JCheckBox(pp.getFullTitle());
        	box.setSelected(false);
        	boxes.add(box);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMissingPilots.add(box, gridBagConstraints);
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
    	for(int i = 0; i < boxes.size(); i++) {
    		JCheckBox box = boxes.get(i);
    		if(box.isSelected()) {
    			tracker.recoverMissingPilot(i);
    		}
    	}
    	this.setVisible(false);
    }


    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
    	this.setVisible(false);
    }
}
