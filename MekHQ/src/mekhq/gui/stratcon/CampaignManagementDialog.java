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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.StratconTab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import static mekhq.campaign.mission.resupplyAndCaches.PerformResupply.performResupply;

/**
 * This class handles the UI for campaign VP/SP management
 * @author NickAragua
 */
public class CampaignManagementDialog extends JDialog {
    private Campaign campaign;
    private StratconCampaignState currentCampaignState;
    private final StratconTab parent;
    private JButton btnRemoveCVP;
    private JButton btnRequestResupply;
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
    public void display(Campaign campaign, StratconCampaignState campaignState,
                        StratconTrackState currentTrack, boolean gmMode) {
        currentCampaignState = campaignState;

        btnRemoveCVP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        btnRequestResupply.setEnabled(currentCampaignState.getSupportPoints() > 0);
        btnGMAddVP.setEnabled(gmMode);
        btnGMAddSP.setEnabled(gmMode);

        lblTrackScenarioOdds.setVisible(gmMode);
        if (gmMode) {
            lblTrackScenarioOdds.setText(String.format(resources.getString("trackScenarioOdds.text"),
                    StratconRulesManager.calculateScenarioOdds(currentTrack, campaignState.getContract(),
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

        // Row and column counters
        int row = 0;

        // Add the "Track Scenario Odds" label
        lblTrackScenarioOdds = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        getContentPane().add(lblTrackScenarioOdds, gbc);

        // Add the "Request Resupply" button
        btnRequestResupply = new JButton(resources.getString("btnRequestResupply.text"));
        btnRequestResupply.addActionListener(evt -> {
            dispose();
            requestResupply(evt);
        });
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        getContentPane().add(btnRequestResupply, gbc);

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

    /**
     * Requests resupply. If there are more than one available support points, it triggers a dialog
     * to specify how many points to use for the resupply.
     * If there is exactly one support point, it automatically uses this one point to resupply.
     * It also updates the button state based on the remaining support points and updates the parent
     * campaign state.
     *
     * @param event The triggering ActionEvent (not used in this method).
     */
    private void requestResupply(ActionEvent event) {
        if (currentCampaignState.getSupportPoints() > 1) {
            supplyDropDialog();
        } else {
            AtBContract contract = currentCampaignState.getContract();
            Resupply resupply = new Resupply(campaign, contract, ResupplyType.RESUPPLY_NORMAL);
            performResupply(resupply, contract, 1);
        }

        btnRequestResupply.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }

    public void supplyDropDialog() {
        final JDialog dialog = new JDialog();
        dialog.setLayout(new GridBagLayout());
        dialog.setTitle(resources.getString("requestingResupply.title"));
        dialog.setSize(UIUtil.scaleForGUI(500, 200));
        dialog.setLocationRelativeTo(null);

        GridBagConstraints constraints = new GridBagConstraints();
        int insertSize = UIUtil.scaleForGUI(8);
        constraints.insets = new Insets(insertSize, insertSize, insertSize, insertSize);

        constraints.gridx = 0;
        constraints.gridy = 0;
        JLabel description = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                UIUtil.scaleForGUI(500), resources.getString("supplyPointExpenditure.text")));
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
        JButton btnConfirm = new JButton(resources.getString("btnConfirm.text"));
        btnConfirm.addActionListener( e-> {
            dialog.dispose();

            AtBContract contract = currentCampaignState.getContract();
            Resupply resupply = new Resupply(campaign, contract, ResupplyType.RESUPPLY_NORMAL);
            performResupply(resupply, contract, 1);

            currentCampaignState.useSupportPoints((int) numberModel.getValue());
        });

        dialog.add(btnConfirm, constraints);

        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void gmAddVPHandler(ActionEvent e) {
        currentCampaignState.updateVictoryPoints(1);
        btnRemoveCVP.setEnabled(currentCampaignState.getVictoryPoints() > 0);
        parent.updateCampaignState();
    }

    private void gmAddSPHandler(ActionEvent e) {
        currentCampaignState.addSupportPoints(1);
        btnRequestResupply.setEnabled(currentCampaignState.getSupportPoints() > 0);
        parent.updateCampaignState();
    }
}
