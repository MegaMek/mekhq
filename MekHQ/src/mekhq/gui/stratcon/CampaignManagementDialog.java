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

import megamek.client.ui.swing.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.atb.supplyDrops.SupplyDrop;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.StratconTab;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This class handles the UI for campaign VP/SP management
 * @author NickAragua
 */
public class CampaignManagementDialog extends JDialog {
    private Campaign campaign;
    private StratconCampaignState currentCampaignState;
    private StratconTab parent;
    private JButton btnConvertVPToSP;
    private JButton btnRequestSupplyDrop;
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
    public void display(Campaign campaign, StratconCampaignState campaignState,
                        StratconTrackState currentTrack, boolean gmMode) {
        currentCampaignState = campaignState;

        btnConvertVPToSP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        btnRequestSupplyDrop.setEnabled(currentCampaignState.getSupportPoints() > 0);
        btnGMAddVP.setEnabled(gmMode);
        btnGMAddSP.setEnabled(gmMode);

        lblTrackScenarioOdds.setVisible(gmMode);
        if (gmMode) {
            lblTrackScenarioOdds.setText(String.format("Track Scenario Odds: %d%%",
                    StratconRulesManager.calculateScenarioOdds(currentTrack, campaignState.getContract(),
                        false)));
        }

        this.campaign = campaign;
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

        btnConvertVPToSP = new JButton();
        btnConvertVPToSP.setText("Convert CVP to SP");
        btnConvertVPToSP.addActionListener(this::convertVPtoSPHandler);
        getContentPane().add(btnConvertVPToSP);

        btnRequestSupplyDrop = new JButton();
        btnRequestSupplyDrop.setText("Request Supply Drop");
        btnRequestSupplyDrop.addActionListener(this::requestSupplyDrop);
        getContentPane().add(btnRequestSupplyDrop);

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

    private void convertVPtoSPHandler(ActionEvent e) {
        currentCampaignState.convertVictoryToSupportPoint();
        btnConvertVPToSP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        btnRequestSupplyDrop.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }

    private void requestSupplyDrop(ActionEvent e) {
        if (currentCampaignState.getSupportPoints() > 1) {
            supplyDropDialog();
        } else {
            LogManager.getLogger().info("CampaignManagementDialog.java 1");
            AtBContract contract = currentCampaignState.getContract();
            SupplyDrop supplyDrops = new SupplyDrop(campaign, contract, false, false);
            supplyDrops.getSupplyDropParts(1);

            currentCampaignState.useSupportPoint();
        }

        btnRequestSupplyDrop.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }

    public void supplyDropDialog() {
        final JDialog dialog = new JDialog();
        dialog.setLayout(new GridBagLayout());
        dialog.setTitle("Requesting Supply Drop");
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(null);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 10, 10, 10);

        constraints.gridx = 0;
        constraints.gridy = 0;
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(500), "How many Support Points would you like to spend?" +
                    "<br><br>Each SP point increases the value of the Supply Drop by roughly 250,000 C-Bills"));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        dialog.add(description, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        SpinnerNumberModel numberModel = new SpinnerNumberModel(1, 1,
            currentCampaignState.getSupportPoints(), 1);
        JSpinner spinner = new JSpinner(numberModel);
        dialog.add(spinner, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.SOUTH;
        JButton btnConfirm = new JButton("Confirm");
        btnConfirm.addActionListener( e-> {
            dialog.dispose();

            LogManager.getLogger().info("CampaignManagementDialog.java 1");
            AtBContract contract = currentCampaignState.getContract();
            SupplyDrop supplyDrops = new SupplyDrop(campaign, contract, false, false);
            supplyDrops.getSupplyDropParts((int) numberModel.getValue());
            currentCampaignState.useSupportPoints((int) numberModel.getValue());
        });

        dialog.add(btnConfirm, constraints);

        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void gmAddVPHandler(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(1);
        btnConvertVPToSP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        parent.updateCampaignState();
    }

    private void gmAddSPHandler(ActionEvent e) {
        currentCampaignState.addSupportPoints(1);
        btnRequestSupplyDrop.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }
}
