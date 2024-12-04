/*
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
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

package mekhq.gui.stratcon;

import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.StratconTab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class handles the UI for campaign VP/SP management
 * @author NickAragua
 */
public class CampaignManagementDialog extends JDialog {
    private StratconCampaignState currentCampaignState;
    private final StratconTab parent;
    private JButton btnRemoveCVP;
    private JButton btnConvertSPtoBonusPart;
    private JButton btnGMAddVP;
    private JButton btnGMAddSP;
    private JLabel lblTrackScenarioOdds;

    public CampaignManagementDialog(StratconTab parent) {
        this.parent = parent;
        this.setTitle("Manage SP/CVP");
        initializeUI();
    }

    /**
     * Show the dialog for a given campaign state, and whether GM mode is on or not
     */
    public void display(StratconCampaignState campaignState, StratconTrackState currentTrack, boolean gmMode) {
        currentCampaignState = campaignState;

        btnRemoveCVP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        btnConvertSPtoBonusPart.setEnabled(currentCampaignState.getSupportPoints() > 0);
        btnGMAddVP.setEnabled(gmMode);
        btnGMAddSP.setEnabled(gmMode);

        lblTrackScenarioOdds.setVisible(gmMode);
        if (gmMode) {
            lblTrackScenarioOdds.setText(String.format("Track Scenario Odds: %d%%",
                    StratconRulesManager.calculateScenarioOdds(currentTrack, campaignState.getContract(), false)));
        }
    }

    /**
     * One-time set up for all the buttons.
     */
    private void initializeUI() {
        GridLayout layout = new GridLayout();
        layout.setColumns(2);
        layout.setRows(0);
        layout.setHgap(1);
        layout.setVgap(1);

        getContentPane().removeAll();
        getContentPane().setLayout(layout);

        btnRemoveCVP = new JButton();
        btnRemoveCVP.setText("Remove CVP (GM)");
        btnRemoveCVP.addActionListener(this::removeCVP);
        getContentPane().add(btnRemoveCVP);

        btnConvertSPtoBonusPart = new JButton();
        btnConvertSPtoBonusPart.setText("Convert SP to bonus part");
        btnConvertSPtoBonusPart.addActionListener(this::convertSPtoBonusPartHandler);
        getContentPane().add(btnConvertSPtoBonusPart);

        btnGMAddVP = new JButton();
        btnGMAddVP.setText("Add CVP (GM)");
        btnGMAddVP.addActionListener(this::gmAddVPHandler);
        getContentPane().add(btnGMAddVP);

        btnGMAddSP = new JButton();
        btnGMAddSP.setText("Add SP (GM)");
        btnGMAddSP.addActionListener(this::gmAddSPHandler);
        getContentPane().add(btnGMAddSP);

        lblTrackScenarioOdds = new JLabel();
        getContentPane().add(lblTrackScenarioOdds);

        pack();
    }

    private void removeCVP(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(-1);

        parent.updateCampaignState();
    }

    private void convertSPtoBonusPartHandler(ActionEvent e) {
        currentCampaignState.useSupportPoint();
        currentCampaignState.getContract().addBonusParts(1);
        btnConvertSPtoBonusPart.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }

    private void gmAddVPHandler(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(1);
        btnRemoveCVP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        parent.updateCampaignState();
    }

    private void gmAddSPHandler(ActionEvent e) {
        currentCampaignState.addSupportPoints(1);
        btnConvertSPtoBonusPart.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }
}
