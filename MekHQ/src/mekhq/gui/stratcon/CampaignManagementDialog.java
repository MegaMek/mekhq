/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConRulesManager;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.gui.StratconTab;

/**
 * This class handles the UI for campaign VP/SP management
 *
 * @author NickAragua
 */
public class CampaignManagementDialog extends JDialog {
    private Campaign campaign;
    private StratConCampaignState currentCampaignState;
    private final StratconTab parent;
    private JButton btnRemoveCVP;
    private JButton btnGMRemoveSP;
    private JButton btnGMAddVP;
    private JButton btnGMAddSP;
    private JLabel lblTrackScenarioOdds;

    final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
          MekHQ.getMHQOptions().getLocale());

    public CampaignManagementDialog(StratconTab parent) {
        this.parent = parent;
        this.setTitle("Manage SP/CVP");
        initializeUI();
    }

    /**
     * Show the dialog for a given campaign state, and whether GM mode is on or not
     */
    public void display(Campaign campaign, StratConCampaignState campaignState,
          StratConTrackState currentTrack, boolean gmMode) {
        currentCampaignState = campaignState;

        btnRemoveCVP.setEnabled(gmMode);
        btnGMRemoveSP.setEnabled(gmMode);
        btnGMAddVP.setEnabled(gmMode);
        btnGMAddSP.setEnabled(gmMode);

        lblTrackScenarioOdds.setVisible(gmMode);
        if (gmMode) {
            lblTrackScenarioOdds.setText(String.format(resources.getString("trackScenarioOdds.text"),
                  StratConRulesManager.calculateScenarioOdds(currentTrack, campaignState.getContract(),
                        false)));
        }

        this.campaign = campaign;
    }

    /**
     * One-time set up for all the buttons.
     */
    private void initializeUI() {
        getContentPane().removeAll();

        // Set up GridBagLayout and constraints
        GridBagLayout layout = new GridBagLayout();
        getContentPane().setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        int insertSize = UIUtil.scaleForGUI(8);
        gbc.insets = new Insets(insertSize, insertSize, insertSize, insertSize);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add the "Track Scenario Odds" label
        lblTrackScenarioOdds = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        getContentPane().add(lblTrackScenarioOdds, gbc);

        // Add the "Remove SP" button
        btnGMRemoveSP = new JButton(resources.getString("btnRemoveSP.text"));
        btnGMRemoveSP.addActionListener(this::removeSP);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        getContentPane().add(btnGMRemoveSP, gbc);

        // Add the "Add SP (GM)" button
        btnGMAddSP = new JButton(resources.getString("btnAddSP.text"));
        btnGMAddSP.addActionListener(this::gmAddSPHandler);
        gbc.gridx = 1;
        gbc.gridy = 1;
        getContentPane().add(btnGMAddSP, gbc);

        // Add the "Add CVP (GM)" button
        btnGMAddVP = new JButton(resources.getString("btnAddCVP.text"));
        btnGMAddVP.addActionListener(this::gmAddVPHandler);
        gbc.gridx = 0;
        gbc.gridy = 2;
        getContentPane().add(btnGMAddVP, gbc);

        // Add the "Remove CVP (GM)" button
        btnRemoveCVP = new JButton(resources.getString("btnRemoveCVP.text"));
        btnRemoveCVP.addActionListener(this::removeCVP);
        gbc.gridx = 1;
        gbc.gridy = 2;
        getContentPane().add(btnRemoveCVP, gbc);

        // Finalize the dialog
        pack();
        setModal(true);
        setResizable(false);
    }

    private void removeCVP(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(-1);

        parent.updateCampaignState();
    }

    private void removeSP(ActionEvent event) {
        int currentSupportPoints = currentCampaignState.getSupportPoints();
        if (currentSupportPoints > 0) {
            currentCampaignState.changeSupportPoints(-1);
        }

        parent.updateCampaignState();
    }

    private void gmAddVPHandler(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(1);
        btnRemoveCVP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        parent.updateCampaignState();
    }

    private void gmAddSPHandler(ActionEvent e) {
        currentCampaignState.changeSupportPoints(1);
        btnGMRemoveSP.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }
}
