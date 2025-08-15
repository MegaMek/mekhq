/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
import javax.swing.ListSelectionModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.gui.StratconPanel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * This class handles the "assign force to track" interaction, where a user may assign a force to a track directly,
 * either to a facility or to an empty hex
 *
 * @author NickAragua
 */
public class TrackForceAssignmentUI extends JDialog implements ActionListener {
    private static final String CMD_CONFIRM = "CMD_TRACK_FORCE_CONFIRM";

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

        JScrollPane forceListContainer = new JScrollPaneWithSpeed();

        // if we're waiting to assign primary forces, we can only do so from the current track
        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign,
              StratconRulesManager.getAvailableForceIDsForManualDeployment(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX,
                    campaign, ownerPanel.getCurrentTrack(), false, null, currentCampaignState));

        availableForceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));

        forceListContainer.setViewportView(availableForceList);

        getContentPane().add(forceListContainer, gbc);

        gbc.gridy++;

        getContentPane().add(btnConfirm, gbc);
        btnConfirm.setEnabled(true);

        pack();
        repaint();
        setModal(true);
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
        if (e.getActionCommand().equals(CMD_CONFIRM)) {
            // sometimes the scenario templates take a little while to load, we don't want the user
            // clicking the button fifty times and getting a bunch of scenarios.
            btnConfirm.setEnabled(false);
            for (Force force : availableForceList.getSelectedValuesList()) {
                StratconRulesManager.deployForceToCoords(ownerPanel.getSelectedCoords(),
                      force.getId(), campaign, currentCampaignState.getContract(), ownerPanel.getCurrentTrack(), false);
            }
            setVisible(false);
            ownerPanel.repaint();
            btnConfirm.setEnabled(true);
        }
    }
}
