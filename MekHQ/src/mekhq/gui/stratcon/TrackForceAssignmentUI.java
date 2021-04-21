/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.gui.StratconPanel;

/**
 * This class handles the "assign force to track" interaction, 
 * where a user may assign a force to a track directly, either to a facility or to an 
 * empty hex
 * @author NickAragua
 */
public class TrackForceAssignmentUI extends JDialog implements ActionListener {

    private static final long serialVersionUID = 2536202500721377965L;

    private final static String CMD_CONFIRM = "CMD_TRACK_FORCE_CONFIRM";
    
    private Campaign campaign;
    private StratconCampaignState currentCampaignState;
    private JList<Force> availableForceList = new JList<>();
    private JButton btnConfirm = new JButton();
    private StratconPanel ownerPanel;
    
    /**
     * Constructor, given a parent StratCon panel.
     */
    public TrackForceAssignmentUI(StratconPanel parent) {
        ownerPanel = parent;
        btnConfirm = new JButton("Confirm");
        btnConfirm.setActionCommand(CMD_CONFIRM);
        btnConfirm.addActionListener(this);
    }
    
    /**
     * Worker function that initializes UI elements
     */
    private void initializeUI() {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        
        JLabel forceAssignmentInstructions = new JLabel("Select force to assign to this track.");
        getContentPane().add(forceAssignmentInstructions, gbc);
        gbc.gridy++;

        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        // if we're waiting to assign primary forces, we can only do so from the current track 
        lanceModel = new ScenarioWizardLanceModel(campaign, 
                StratconRulesManager.getAvailableForceIDs(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX, 
                        campaign, ownerPanel.getCurrentTrack(), false, null));
        
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        
        forceListContainer.setViewportView(availableForceList);

        getContentPane().add(forceListContainer, gbc);
        
        gbc.gridy++;
        
        getContentPane().add(btnConfirm, gbc);
        btnConfirm.setEnabled(true);
        
        pack();
        repaint();
    }
    
    /**
     * Display the track force assignment UI.
     */
    public void display(Campaign campaign, StratconCampaignState campaignState, StratconCoords coords) {
        this.campaign = campaign;
        this.currentCampaignState = campaignState;
        
        initializeUI();
    }

    /**
     * Event handler for button commands.    
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case CMD_CONFIRM:
                // sometimes the scenario templates take a little while to load, we don't want the user
                // clicking the button fifty times and getting a bunch of scenarios.
                btnConfirm.setEnabled(false);                
                for (Force force : availableForceList.getSelectedValuesList()) {
                    StratconRulesManager.deployForceToCoords(
                            ownerPanel.getSelectedCoords(), 
                            force.getId(), 
                            campaign, currentCampaignState.getContract(), 
                            ownerPanel.getCurrentTrack(), false);
                }
                setVisible(false);
                ownerPanel.repaint();
                btnConfirm.setEnabled(true);
                break;
        }
    }
}
