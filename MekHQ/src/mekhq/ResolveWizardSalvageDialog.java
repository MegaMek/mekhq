/*
 * ResolveWizardSalvageDialog.java
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import megamek.common.Entity;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.Unit;
import mekhq.campaign.mission.Contract;
/**
 *
 * @author  Taharqa
 */
public class ResolveWizardSalvageDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    
	private ResolveScenarioTracker tracker;
    
	private javax.swing.JPanel panButtons;
	private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnNext;
    private javax.swing.JScrollPane scrSalvage;
	private javax.swing.JPanel panSalvage;
    private javax.swing.JTextArea txtInstructions;
    private ArrayList<javax.swing.JCheckBox> boxes;
    private ArrayList<Unit> salvageables;
    
    private JLabel lblSalvageValueUnit1;
    private JLabel lblSalvageValueEmployer1;
    private JLabel lblSalvagePct1;
    private JLabel lblSalvageValueUnit2;
    private JLabel lblSalvageValueEmployer2;
    private JLabel lblSalvagePct2;
	
    private long salvageEmployer;
    private long salvageUnit;
    private int currentSalvagePct;
    private int maxSalvagePct;
    
    DecimalFormat formatter;
    
    /** Creates new form NewTeamDialog */
    public ResolveWizardSalvageDialog(java.awt.Frame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.tracker = t;
        salvageables = new ArrayList<Unit>();
        if(tracker.getMission() instanceof Contract) {
	        salvageEmployer = ((Contract)tracker.getMission()).getSalvagedByEmployer();
	    	salvageUnit = ((Contract)tracker.getMission()).getSalvagedByUnit();
	    	maxSalvagePct = ((Contract)tracker.getMission()).getSalvagePct();
	    	currentSalvagePct = (int)(100*((double)salvageUnit)/(salvageUnit+salvageEmployer));
        }
        formatter = new DecimalFormat();
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	panButtons = new javax.swing.JPanel();
    	panSalvage = new javax.swing.JPanel();
    	btnNext = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        scrSalvage = new javax.swing.JScrollPane();
        txtInstructions = new javax.swing.JTextArea();
        boxes = new ArrayList<javax.swing.JCheckBox>();

     
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(ResolveWizardSalvageDialog.class);
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
	
        panSalvage.setName("panSalvage");
        panSalvage.setLayout(new GridBagLayout()); 
        
        int i = 0;
        if(tracker.getMission() instanceof Contract) {
        	lblSalvageValueUnit1 = new JLabel(resourceMap.getString("lblSalvageValueUnit1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panSalvage.add(lblSalvageValueUnit1, gridBagConstraints);
        	lblSalvageValueUnit2 = new JLabel(formatter.format(salvageUnit) + " C-Bills");   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panSalvage.add(lblSalvageValueUnit2, gridBagConstraints);
            i++;
            lblSalvageValueEmployer1 = new JLabel(resourceMap.getString("lblSalvageValueEmployer1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panSalvage.add(lblSalvageValueEmployer1, gridBagConstraints);
        	lblSalvageValueEmployer2 = new JLabel(formatter.format(salvageEmployer) + " C-Bills");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panSalvage.add(lblSalvageValueEmployer2, gridBagConstraints);
            i++;
            String lead = "<html><font color='black'>";
            if(currentSalvagePct > maxSalvagePct) {
            	lead = "<html><font color='red'>";
            }
            lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvagePct1.text"));   	
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 20, 0);
            panSalvage.add(lblSalvagePct1, gridBagConstraints);
            lblSalvagePct2 = new JLabel(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 20, 0);
            panSalvage.add(lblSalvagePct2, gridBagConstraints);
            i++;
        }
        
        
        JCheckBox box;
        i++;
        for(Entity en : tracker.getPotentialSalvage()) {
        	Unit u = new Unit(en, tracker.getCampaign());
        	u.runDiagnostic();
        	salvageables.add(u);
        	box = new JCheckBox(en.getDisplayName() + " (" + formatter.format(u.getSellValue()) + " C-Bills)");
        	box.setSelected(false);
        	box.addItemListener(new ItemListener() {
        		@Override
        		public void itemStateChanged(ItemEvent evt) {
     				checkSalvageRights();
     			}
        	});
        	boxes.add(box);
        	gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panSalvage.add(box, gridBagConstraints);
            i++;
        }              
        scrSalvage.setViewportView(panSalvage);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrSalvage, gridBagConstraints);
        
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
	   	 
	   	 checkSalvageRights();
	   	 
	   	 pack();
    }

    private void checkSalvageRights() {
    	if(!(tracker.getMission() instanceof Contract)) {
    		return;
    	}
    	salvageEmployer = ((Contract)tracker.getMission()).getSalvagedByEmployer();
    	salvageUnit = ((Contract)tracker.getMission()).getSalvagedByUnit();
    	for(int i = 0; i < boxes.size(); i++) {
    		JCheckBox box = boxes.get(i);
    		if(box.isSelected()) {
    			salvageUnit += salvageables.get(i).getSellValue();
    		} else {
    			salvageEmployer += salvageables.get(i).getSellValue();
    		}
    	}
    	currentSalvagePct = (int)(100*((double)salvageUnit)/(salvageUnit+salvageEmployer));
    	for(JCheckBox box : boxes) {
    		if(!box.isSelected() && currentSalvagePct >= maxSalvagePct) {
    			box.setEnabled(false);
    		} else {
    			box.setEnabled(true);
    		}
    	}
    	lblSalvageValueUnit2.setText(formatter.format(salvageUnit) + " C-Bills");
    	lblSalvageValueEmployer2.setText(formatter.format(salvageEmployer) + " C-Bills");
    	String lead = "<html><font color='black'>";
        if(currentSalvagePct > maxSalvagePct) {
        	lead = "<html><font color='red'>";
        }
        lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");

    }
    
    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {
    	for(int i = 0; i < boxes.size(); i++) {
    		JCheckBox box = boxes.get(i);
    		if(box.isSelected()) {
    			tracker.salvageUnit(i);
    		} else {
    			tracker.dontSalvageUnit(i);
    		}
    	}
    	this.setVisible(false);
    	if(!tracker.getKillCredits().isEmpty()) {
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
