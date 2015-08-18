/*
 * NewMissionDialog.java
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
import java.awt.Frame;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.gui.JSuggestField;

/**
 *
 * @author  Taharqa
 */
public class CustomizeMissionDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    @SuppressWarnings("unused") // FIXME
	private Frame frame;
    private Mission mission;
    private Campaign campaign;
    private boolean newMission;
    
    /** Creates new form NewTeamDialog */
    public CustomizeMissionDialog(java.awt.Frame parent, boolean modal, Mission m, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        if(null == m) {
        	mission = new Mission("New Mission");
        	newMission = true;
        } else {
        	mission = m;
        	newMission = false;
        }
        campaign = c;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        txtType = new javax.swing.JTextField();
        lblType = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        scrDesc = new javax.swing.JScrollPane();
        txtDesc = new javax.swing.JTextArea();
        lblPlanetName = new javax.swing.JLabel();
     
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeMissionDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title"));
        if(newMission) {
            setTitle(resourceMap.getString("title.new"));

        }
        
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblName, gridBagConstraints);
        
        txtName.setText(mission.getName());
        txtName.setName("txtName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtName, gridBagConstraints);
 
        lblType.setText(resourceMap.getString("lblType.text")); // NOI18N
        lblType.setName("lblType"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblType, gridBagConstraints);
        
        txtType.setText(mission.getType());
        txtType.setName("txtType"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtType, gridBagConstraints);       
        
        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text")); // NOI18N
        lblPlanetName.setName("lblPlanetName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblPlanetName, gridBagConstraints);
        
        suggestPlanet = new JSuggestField(this, campaign.getPlanetNames());
        if(!newMission) {
        	suggestPlanet.setText(mission.getPlanetName());
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(suggestPlanet, gridBagConstraints);
        
        txtDesc.setText(mission.getDescription());
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Description"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDesc.setViewportView(txtDesc);
        scrDesc.setPreferredSize(new Dimension(400,200));
        scrDesc.setMinimumSize(new Dimension(400,200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        getContentPane().add(scrDesc, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	String chosenName = txtName.getText();
    	for(Mission m : campaign.getMissions()) {
    		if(m.getName().equals(chosenName)) {
    			JOptionPane.showMessageDialog(frame,
    				    "There is already a missiong with the name " + chosenName,
    				    "Duplicate Mission Name",
    				    JOptionPane.ERROR_MESSAGE);
    			return;
    		}
    	}   	
    	mission.setName(txtName.getText());
    	mission.setType(txtType.getText());
    	mission.setPlanetName(suggestPlanet.getText());
    	mission.setDesc(txtDesc.getText());
    	if(newMission) {
    		campaign.addMission(mission);
    	}
    	this.setVisible(false);
    }
    
    public int getMissionId() {
    	return mission.getId();
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	this.setVisible(false);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtName;
    private javax.swing.JLabel lblType;
    private javax.swing.JTextField txtType;
    private javax.swing.JTextArea txtDesc;
    private javax.swing.JScrollPane scrDesc;
    private javax.swing.JLabel lblPlanetName;
	private JSuggestField suggestPlanet;


    // End of variables declaration//GEN-END:variables

}
