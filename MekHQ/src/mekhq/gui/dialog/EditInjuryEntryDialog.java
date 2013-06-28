/*
 * EditInjuryEntryDialog.java
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.BorderFactory;

import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Ralgith
 */
public class EditInjuryEntryDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    @SuppressWarnings("unused")
	private Frame frame;
    private Injury injury;
    
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JTextArea txtDays;
    private javax.swing.JComboBox ddLocation;
    private javax.swing.JComboBox ddType;
    private javax.swing.JTextArea txtFluff;
    private javax.swing.JTextArea txtHits;
    private javax.swing.JComboBox ddPermanent;
    private javax.swing.JComboBox ddWorkedOn;
    private javax.swing.JComboBox ddExtended;
    private javax.swing.JPanel panBtn;
    private javax.swing.JPanel panMain;
    
    /** Creates new form EditInjuryEntryDialog */
    public EditInjuryEntryDialog(java.awt.Frame parent, boolean modal, Injury e) {
        super(parent, modal);
        this.frame = parent;
        injury = e;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

    	String[] locNames = new String[Person.BODY_NUM];
    	for (int i = 0; i < Person.BODY_NUM; i++) {
    		locNames[i] = Injury.getLocationName(i);
    	}
    	String[] typeNames = new String[Injury.INJ_NUM];
    	for (int i = 0; i < Injury.INJ_NUM; i++) {
    		typeNames[i] = Injury.getTypeName(i);
    	}
    	String[] tf = { "True", "False" };
    	txtDays = new javax.swing.JTextArea();
    	ddLocation = new javax.swing.JComboBox<String>(locNames);
    	ddType = new javax.swing.JComboBox<String>(typeNames);
    	txtFluff = new javax.swing.JTextArea();
    	txtHits = new javax.swing.JTextArea();
    	ddPermanent = new javax.swing.JComboBox<String>(tf);
    	ddWorkedOn = new javax.swing.JComboBox<String>(tf);
    	ddExtended = new javax.swing.JComboBox<String>(tf);
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        panBtn = new javax.swing.JPanel();
        panMain = new javax.swing.JPanel();
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditInjuryEntryDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());
        panBtn.setLayout(new GridLayout(0,2));
        panMain.setLayout(new GridBagLayout());
        
        txtDays.setText(Integer.toString(injury.getTime()));
        txtDays.setName("txtDays");
        txtDays.setEditable(true);
        txtDays.setLineWrap(true);
        txtDays.setWrapStyleWord(true);
        txtDays.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Days Remaining"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtDays.setPreferredSize(new Dimension(250,75));
        txtDays.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtDays, gridBagConstraints);
        
        ddLocation.setSelectedIndex(injury.getLocation());
        ddLocation.setName("ddLocation");
        ddLocation.setEditable(false);
        ddLocation.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Location on Body"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddLocation.setPreferredSize(new Dimension(250,75));
        ddLocation.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(ddLocation, gridBagConstraints);
        
        ddType.setSelectedIndex(injury.getType());
        ddType.setName("ddType");
        ddType.setEditable(false);
        ddType.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Type of Injury"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddType.setPreferredSize(new Dimension(250,75));
        ddType.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(ddType, gridBagConstraints);
        
        txtFluff.setText(injury.getFluff());
        txtFluff.setName("txtFluff");
        txtFluff.setEditable(true);
        txtFluff.setLineWrap(true);
        txtFluff.setWrapStyleWord(true);
        txtFluff.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Fluff Message"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtFluff.setPreferredSize(new Dimension(250,75));
        txtFluff.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtFluff, gridBagConstraints);
        
        txtHits.setText(Integer.toString(injury.getHits()));
        txtHits.setName("txtHits");
        txtHits.setEditable(true);
        txtHits.setLineWrap(true);
        txtHits.setWrapStyleWord(true);
        txtHits.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Number of Hits"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtHits.setPreferredSize(new Dimension(250,75));
        txtHits.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtHits, gridBagConstraints);
        
        ddPermanent.setSelectedIndex(injury.getPermanent() ? 0 : 1);
        ddPermanent.setName("ddPermanent");
        ddPermanent.setEditable(false);
        ddPermanent.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Is Permanent"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddPermanent.setPreferredSize(new Dimension(250,75));
        ddPermanent.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(ddPermanent, gridBagConstraints);
        
        ddWorkedOn.setSelectedIndex(injury.getWorkedOn() ? 0 : 1);
        ddWorkedOn.setName("ddWorkedOn");
        ddWorkedOn.setEditable(false);
        ddWorkedOn.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Doctor Has Worked On"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddWorkedOn.setPreferredSize(new Dimension(250,75));
        ddWorkedOn.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(ddWorkedOn, gridBagConstraints);
        
        ddExtended.setSelectedIndex(injury.getExtended() ? 0 : 1);
        ddExtended.setName("ddExtended");
        ddExtended.setEditable(true);
        ddExtended.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Was Extended Time"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddExtended.setPreferredSize(new Dimension(250,75));
        ddExtended.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(ddExtended, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
    	injury.setTime(Integer.parseInt(txtDays.getText()));
    	injury.setHits(Integer.parseInt(txtHits.getText()));
    	injury.setFluff(txtFluff.getText());
    	injury.setLocation(ddLocation.getSelectedIndex());
    	injury.setType(ddType.getSelectedIndex());
    	if (ddPermanent.getSelectedIndex() == 0) {
    		injury.setPermanent(true);
    	} else {
    		injury.setPermanent(false);
    	}
    	if (ddWorkedOn.getSelectedIndex() == 0) {
    		injury.setWorkedOn(true);
    	} else {
    		injury.setWorkedOn(false);
    	}
    	if (ddExtended.getSelectedIndex() == 0) {
    		injury.setExtended(true);
    	} else {
    		injury.setExtended(false);
    	}
    	injury.setUUID(UUID.randomUUID());
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
    	injury = null;
    	this.setVisible(false);
    }
    
    public Injury getEntry() {
    	return injury;
    }
}
