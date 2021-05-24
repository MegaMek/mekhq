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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.util.ResourceBundle;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class CustomizeMissionDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Mission mission;
    private Campaign campaign;
    private boolean newMission;

    /** Creates new form NewTeamDialog */
    public CustomizeMissionDialog(java.awt.Frame parent, boolean modal, Mission m, Campaign c) {
        super(parent, modal);
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
        setUserPreferences();
        pack();
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        txtType = new javax.swing.JTextField();
        lblType = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        lblPlanetName = new javax.swing.JLabel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeMissionDialog", new EncodeControl()); //$NON-NLS-1$
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

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        if (!newMission) {
            suggestPlanet.setText(mission.getSystemName(campaign.getLocalDate()));
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(suggestPlanet, gridBagConstraints);

        txtDesc = new MarkdownEditorPanel(resourceMap.getString("txtDesc.title"));
        txtDesc.setText(mission.getDescription());
        txtDesc.setMinimumSize(new Dimension(400, 100));
        txtDesc.setPreferredSize(new Dimension(400, 250));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtDesc, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CustomizeMissionDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed

    	mission.setName(txtName.getText());
    	mission.setType(txtType.getText());

    	PlanetarySystem canonSystem = Systems.getInstance().getSystemByName(suggestPlanet.getText(),
                campaign.getLocalDate());

    	if (canonSystem != null) {
    	    mission.setSystemId(canonSystem.getId());
    	} else {
    	    mission.setSystemId(null);
    	    mission.setLegacyPlanetName(suggestPlanet.getText());
    	}

    	mission.setDesc(txtDesc.getText());
    	if (newMission) {
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
    private MarkdownEditorPanel txtDesc;
    private javax.swing.JLabel lblPlanetName;
	private JSuggestField suggestPlanet;


    // End of variables declaration//GEN-END:variables

}
