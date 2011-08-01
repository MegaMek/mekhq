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

package mekhq.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;

import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.mission.Contract;

/**
 *
 * @author  Taharqa
 */
public class ResolveWizardPilotStatusDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JScrollPane scrMissingPilots;
	private javax.swing.JPanel panMissingPilots;
    private javax.swing.JTextArea txtInstructions;
    private ArrayList<JCheckBox> miaBtns;
    private ArrayList<JSlider> hitSliders;
    private ArrayList<PersonStatus> statuses;

	
    /** Creates new form NewTeamDialog */
    public ResolveWizardPilotStatusDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
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
        miaBtns = new ArrayList<JCheckBox>();
        hitSliders = new ArrayList<JSlider>();
        statuses = new ArrayList<PersonStatus>();
     
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveWizardPilotStatusDialog");
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
        txtInstructions.setMinimumSize(new Dimension(500,125));
        txtInstructions.setPreferredSize(new Dimension(500,125));
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
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panMissingPilots.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panMissingPilots.add(new JLabel(resourceMap.getString("mia")), gridBagConstraints);
        
        int i = 2;
        JLabel nameLbl;
        JCheckBox miaCheck;
        JSlider hitSlider; 
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( 0 ), new JLabel("0") );
        labelTable.put( new Integer( 1 ), new JLabel("1") );
        labelTable.put( new Integer( 2 ), new JLabel("2") );
        labelTable.put( new Integer( 3 ), new JLabel("3") );
        labelTable.put( new Integer( 4 ), new JLabel("4") );
        labelTable.put( new Integer( 5 ), new JLabel("5") );
        labelTable.put( new Integer( 6 ), new JLabel(resourceMap.getString("dead")) );
        for(int pid : tracker.getPeopleStatus().keySet()) {
        	PersonStatus status = tracker.getPeopleStatus().get(pid);
        	statuses.add(status);
        	nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
        	miaCheck = new JCheckBox("");
        	miaBtns.add(miaCheck);
        	hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 6, status.getHits());
        	hitSlider.setMajorTickSpacing(1);
        	hitSlider.setPaintTicks(true);
        	hitSlider.setLabelTable(labelTable);
        	hitSlider.setPaintLabels(true);
        	hitSlider.setSnapToTicks(true);
        	hitSliders.add(hitSlider);
        	if(status.isMissing()) {
        		miaCheck.setSelected(true);
        	} 
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMissingPilots.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            panMissingPilots.add(hitSlider, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            panMissingPilots.add(miaCheck, gridBagConstraints);
            i++;
        }              
        scrMissingPilots.setViewportView(panMissingPilots);
        scrMissingPilots.setMinimumSize(new Dimension(500,400));
        scrMissingPilots.setPreferredSize(new Dimension(500,400));

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
    	for(int i = 0; i < statuses.size(); i++) {
    		PersonStatus status = statuses.get(i);
    		status.setMissing(miaBtns.get(i).isSelected());
    		status.setHits(hitSliders.get(i).getValue());
    	}
    	this.setVisible(false);
    	if(tracker.getPotentialSalvage().size() > 0 
    			&& (!(tracker.getMission() instanceof Contract) || ((Contract)tracker.getMission()).canSalvage())) {
    		ResolveWizardSalvageDialog resolveDialog = new ResolveWizardSalvageDialog((Frame)getParent(), true, tracker);
    		resolveDialog.setVisible(true);
    	} else if(!tracker.getKillCredits().isEmpty()) {
    		ResolveWizardAssignKillsDialog resolveDialog = new ResolveWizardAssignKillsDialog((Frame)getParent(), true, tracker);
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
