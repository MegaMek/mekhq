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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;

/**
 *
 * @author  Ralgith
 */
public class EditInjuryEntryDialog extends JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    @SuppressWarnings("unused")
	private Frame frame; // FIXME: Unusued => Unneeded?
    private Injury injury;
    
    private JButton btnClose;
    private JButton btnOK;
    private JTextArea txtDays;
    private JComboBox<BodyLocationChoice> ddLocation;
    private JComboBox<String> ddType;
    private JTextArea txtFluff;
    private JTextArea txtHits;
    private JComboBox<String> ddPermanent;
    private JComboBox<String> ddWorkedOn;
    private JComboBox<String> ddExtended;
    private JPanel panBtn;
    private JPanel panMain;
    
    private BodyLocationChoice[] locations;
    
    /** Creates new form EditInjuryEntryDialog */
    public EditInjuryEntryDialog(Frame parent, boolean modal, Injury e) {
        super(parent, modal);
        this.frame = parent;
        injury = e;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	GridBagConstraints gridBagConstraints;

        locations = new BodyLocationChoice[BodyLocation.values().length];
        int i = 0;
        for(BodyLocation loc : BodyLocation.values()) {
            locations[i] = new BodyLocationChoice(loc);
            ++ i;
        }
        String[] typeNames = new String[Injury.INJ_NUM];
    	for (i = 0; i < Injury.INJ_NUM; i++) {
    		typeNames[i] = Injury.getTypeName(i);
    	}
    	String[] tf = { "True", "False" };
    	txtDays = new JTextArea();
    	ddLocation = new JComboBox<BodyLocationChoice>(locations);
    	ddType = new JComboBox<String>(typeNames);
    	txtFluff = new JTextArea();
    	txtHits = new JTextArea();
    	ddPermanent = new JComboBox<String>(tf);
    	ddWorkedOn = new JComboBox<String>(tf);
    	ddExtended = new JComboBox<String>(tf);
        btnOK = new JButton();
        btnClose = new JButton();
        panBtn = new JPanel();
        panMain = new JPanel();
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditInjuryEntryDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(txtDays, gridBagConstraints);
        
        for(BodyLocationChoice choice : locations) {
            if(injury.getLocation() == choice.loc) {
                ddLocation.setSelectedItem(choice);
                break;
            }
        }
        ddLocation.setName("ddLocation");
        ddLocation.setEditable(false);
        ddLocation.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Location on Body"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        ddLocation.setPreferredSize(new Dimension(250,75));
        ddLocation.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(ddExtended, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    
    private void btnOKActionPerformed(ActionEvent evt) {
    	injury.setTime(Integer.parseInt(txtDays.getText()));
    	injury.setHits(Integer.parseInt(txtHits.getText()));
    	injury.setFluff(txtFluff.getText());
        injury.setLocation(((BodyLocationChoice) ddLocation.getSelectedItem()).loc);
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

    private void btnCloseActionPerformed(ActionEvent evt) {
    	injury = null;
    	this.setVisible(false);
    }
    
    public Injury getEntry() {
    	return injury;
    }
    
    private static class BodyLocationChoice {
        public final BodyLocation loc;
        
        public BodyLocationChoice(BodyLocation loc) {
            this.loc = loc;
        }
        
        @Override
        public String toString() {
            return loc.readableName;
        }
    }
}
