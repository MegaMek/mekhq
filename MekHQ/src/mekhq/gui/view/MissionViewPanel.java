/*
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.BriefingStyle;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.utilities.ReportingUtilities;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissionViewPanel extends JScrollablePanel {
    private final Mission mission;
    protected CampaignGUI gui;

    protected JPanel pnlStats;
    protected JTextPane txtDesc;

    /* Basic Mission Parameters */
    private JPanel lblBelligerents;
    private JLabel lblLocation;
    private JLabel txtLocation;
    private JLabel lblType;
    private JLabel txtType;

    /* Contract Parameters */
    private JLabel lblEmployer;
    private JLabel txtEmployer;
    private JLabel lblStartDate;
    private JLabel txtStartDate;
    private JLabel lblEndDate;
    private JLabel txtEndDate;
    private JLabel lblPayout;
    private JLabel txtPayout;
    private JLabel lblCommand;
    private JLabel txtCommand;
    private JLabel lblBLC;
    private JLabel txtBLC;
    private JLabel lblSalvageValueMerc;
    private JLabel txtSalvageValueMerc;
    private JLabel lblSalvageValueEmployer;
    private JLabel txtSalvageValueEmployer;
    private JLabel txtDeploymentCoverage;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel",
          MekHQ.getMHQOptions().getLocale());

    public MissionViewPanel(Mission m, CampaignGUI gui) {
        super();
        this.mission = m;
        this.gui = gui;
        initComponents();
    }

    /**
     * Recomputes and updates the Deployment Coverage label so it reflects the current assignment state without needing
     * to rebuild the whole panel. Has no effect when the panel does not display a deployment coverage value (e.g. for
     * non-AtB contracts, when StratCon is disabled, or when the contract is not currently active).
     */
    public void updateDeploymentCoverage() {
        if ((txtDeploymentCoverage == null) || !(mission instanceof AtBContract contract)) {
            return;
        }

        Campaign campaign = gui.getCampaign();
        if (!campaign.getCampaignOptions().isUseStratCon() || !contract.isActiveOn(campaign.getLocalDate())) {
            return;
        }

        int assignedCombatElements = RequiredLancesTableModel.getAssignedCombatElementCount(campaign, contract);
        int requiredCombatElements = contract.getRequiredCombatElements();
        txtDeploymentCoverage.setText(assignedCombatElements + " / " + requiredCombatElements);
        if (RequiredLancesTableModel.hasDeploymentShortfall(campaign, contract)) {
            txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getBelowContractMinimumForeground());
        } else {
            txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getFontColorPositive());
        }
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        // The mission status is shown in the section's title border ("<name> - <status>") rather than as a row.
        JPanel statsSection = BriefingStyle.createSectionPanel(mission.getName() + " - " + mission.getStatus());
        pnlStats = new JPanel();
        txtDesc = new JTextPane();

        setLayout(new GridBagLayout());

        statsSection.setMaximumSize(UIUtil.scaleForGUI(200, Integer.MAX_VALUE));
        pnlStats.setName("pnlStats");
        fillStats();
        statsSection.add(pnlStats, BorderLayout.CENTER);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(statsSection, gridBagConstraints);
    }

    private void fillStats() {
        if (mission instanceof AtBContract) {
            fillStatsAtBContract();
        } else if (mission instanceof Contract) {
            fillStatsContract();
        } else {
            fillStatsBasic();
        }
    }

    private void fillStatsBasic() {
        lblBelligerents = new JPanel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblType = new JLabel();
        txtType = new JLabel();

        pnlStats.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints;

        if ((null != mission.getSystemName(null)) && !mission.getSystemName(null).isEmpty()) {
            lblLocation.setName("lblLocation");
            lblLocation.setText(resourceMap.getString("lblLocation.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblLocation, gridBagConstraints);

            txtLocation.setName("txtLocation");
            String systemName = mission.getSystemName(null);
            txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
            txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtLocation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Display where it is on the interstellar map
                    gui.getNavigationTab().showSystem(mission.getSystem());
                    gui.setSelectedTab(gui.getNavigationTab());
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtLocation, gridBagConstraints);
        }

        if ((null != mission.getType()) && !mission.getType().isEmpty()) {
            lblType.setName("lblType");
            lblType.setText(resourceMap.getString("lblType.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblType, gridBagConstraints);

            txtType.setName("txtType");
            txtType.setText(mission.getType());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtType, gridBagConstraints);
        }

        addDescriptionPane(mission.getDescription(), 3, 1.0);
    }

    private void fillStatsContract() {
        Contract contract = (Contract) mission;

        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblEmployer = new JLabel();
        txtEmployer = new JLabel();
        lblType = new JLabel();
        txtType = new JLabel();
        lblStartDate = new JLabel();
        txtStartDate = new JLabel();
        lblEndDate = new JLabel();
        txtEndDate = new JLabel();
        lblPayout = new JLabel();
        txtPayout = new JLabel();
        lblCommand = new JLabel();
        txtCommand = new JLabel();
        lblBLC = new JLabel();
        txtBLC = new JLabel();

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        if ((null != contract.getSystemName(null)) && !contract.getSystemName(null).isEmpty()) {
            lblLocation.setName("lblLocation");
            lblLocation.setText(resourceMap.getString("lblLocation.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblLocation, gridBagConstraints);

            txtLocation.setName("txtLocation");
            String systemName = contract.getSystemName(null);
            txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
            txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtLocation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Display where it is on the interstellar map
                    gui.getNavigationTab().showSystem(contract.getSystem());
                    gui.setSelectedTab(gui.getNavigationTab());
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtLocation, gridBagConstraints);
        }

        if ((null != contract.getEmployer()) && !contract.getEmployer().isEmpty()) {
            lblEmployer.setName("lblEmployer");
            lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblEmployer, gridBagConstraints);

            txtEmployer.setName("txtEmployer");
            txtEmployer.setText(contract.getEmployer());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtEmployer, gridBagConstraints);
        }

        if ((null != contract.getType()) && !contract.getType().isEmpty()) {
            lblType.setName("lblType");
            lblType.setText(resourceMap.getString("lblType.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblType, gridBagConstraints);

            txtType.setName("txtType");
            txtType.setText(contract.getType());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtType, gridBagConstraints);
        }

        lblStartDate.setName("lblStartDate");
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStartDate, gridBagConstraints);

        txtStartDate.setName("txtStartDate");
        txtStartDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtStartDate, gridBagConstraints);

        lblEndDate.setName("lblEndDate");
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEndDate, gridBagConstraints);

        txtEndDate.setName("txtEndDate");
        txtEndDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getEndingDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEndDate, gridBagConstraints);

        lblPayout.setName("lblPayout");
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblPayout, gridBagConstraints);

        txtPayout.setName("txtPayout");
        txtPayout.setText(contract.getMonthlyPayOut().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtPayout, gridBagConstraints);

        lblCommand.setName("lblCommand");
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCommand, gridBagConstraints);

        txtCommand.setName("txtCommand");
        txtCommand.setText(contract.getCommandRights().toString());
        txtCommand.setToolTipText(wordWrap(contract.getCommandRights().getToolTipText()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCommand, gridBagConstraints);

        lblBLC.setName("lblBLC");
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBLC, gridBagConstraints);

        txtBLC.setName("txtBLC");
        txtBLC.setText(contract.getBattleLossCompensation() + "%");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBLC, gridBagConstraints);

        int i = 9;
        lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
        txtSalvageValueMerc = new JLabel();
        txtSalvageValueMerc.setText(contract.getSalvagedByUnit().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = i;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtSalvageValueMerc, gridBagConstraints);
        i++;
        lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
        txtSalvageValueEmployer = new JLabel();
        txtSalvageValueEmployer.setText(contract.getSalvagedByEmployer().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = i;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
        i++;

        JLabel lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
        JLabel lblSalvagePct2 = new JLabel();

        if (contract.isSalvageExchange()) {
            lblSalvagePct2.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePercent() + "%)");
        } else if (contract.getSalvagePercent() == 0) {
            lblSalvagePct2.setText(resourceMap.getString("none"));
        } else {
            lblSalvagePct1.setText(resourceMap.getString("lblSalvagePct.text"));
            int maxSalvagePct = contract.getSalvagePercent();

            int currentSalvagePct = contract.getCurrentSalvagePct();

            String lead = "<html><font>";
            if (currentSalvagePct > maxSalvagePct) {
                lead = "<html><font color='" + ReportingUtilities.getNegativeColor() + "'>";
            }
            lblSalvagePct2.setText(lead +
                                         currentSalvagePct +
                                         "%</font> <span>(max " +
                                         maxSalvagePct +
                                         "%)</span></html>");
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct1, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = i;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct2, gridBagConstraints);
        i++;
        addDescriptionPane(contract.getDescription(), i, 1.0);

    }

    private void fillStatsAtBContract() {
        AtBContract contract = (AtBContract) mission;
        Campaign campaign = gui.getCampaign();

        // TODO : Switch me to use IUnitRating
        String[] ratingNames = { "F", "D", "C", "B", "A" };
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        /* AtB Contract Parameters */
        lblPayout = new JLabel();
        txtPayout = new JLabel();
        lblCommand = new JLabel();
        txtCommand = new JLabel();
        lblBLC = new JLabel();
        txtBLC = new JLabel();
        JLabel lblAllyRating = new JLabel();
        JLabel txtAllyRating = new JLabel();
        JLabel lblEnemyRating = new JLabel();
        JLabel txtEnemyRating = new JLabel();
        JLabel lblSharePct = new JLabel();
        JLabel txtSharePct = new JLabel();
        JLabel lblCargoRequirement = new JLabel();
        JLabel txtCargoRequirement = new JLabel();
        JLabel lblDeploymentCoverage = new JLabel();
        txtDeploymentCoverage = new JLabel();
        JLabel lblScore = new JLabel();
        JLabel txtScore = new JLabel();
        JLabel lblSupportPoints = new JLabel();
        JLabel txtSupport = new JLabel();

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        int y = 0;

        // === Header: belligerents (the status is shown in the section's title border). The employer and enemy
        // faction names are shown as tooltips on their logos (employer left, enemy right) rather than as their own
        // label rows, to save vertical space.
        final String employerTooltip = MessageFormat.format(resourceMap.getString("belligerents.employer.tooltip"),
              contract.getEmployerName(campaign.getGameYear()));
        final String enemyTooltip = MessageFormat.format(resourceMap.getString("belligerents.enemy.tooltip"),
              contract.getEnemyBotName());
        lblBelligerents = contract.getBelligerentsPanel(gui.getCampaign().getGameYear(), employerTooltip, enemyTooltip);
        addHeaderRow(lblBelligerents, y++, GridBagConstraints.NORTH);

        // === Identity: the orienting facts (where, who) ===
        lblLocation.setName("lblLocation");
        lblLocation.setText(resourceMap.getString("lblLocation.text"));
        txtLocation.setName("txtLocation");
        String systemName = contract.getSystemName(campaign.getLocalDate());
        txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                gui.getNavigationTab().showSystem(contract.getSystem());
                gui.setSelectedTab(gui.getNavigationTab());
            }
        });
        addStatRow(lblLocation, txtLocation, y++);

        // === Dashboard: all gauges grouped together, most important first ===
        // Enemy morale (always shown for AtB contracts).
        final MoraleBar.MoraleDisplay moraleDisplay = MoraleBar.getMoraleDisplay(contract);
        MoraleBar moraleBar = new MoraleBar(contract.getMoraleLevel(), moraleDisplay.label());
        moraleBar.setToolTipText(wordWrap(moraleDisplay.tooltip()));
        addGaugeRow(moraleBar, y++);

        final boolean useStratCon = campaign.getCampaignOptions().isUseStratCon();
        if (useStratCon) {
            // Victory points gauge, or a text fallback when there is no positive target.
            int currentScore = contract.getContractScore(campaign.getCampaignOptions().isUseStratConMaplessMode());
            int neededScore = contract.getRequiredVictoryPoints();
            if (neededScore > 0) {
                final boolean canEndEarly = (contract.getStratconCampaignState() == null) ||
                                                  contract.getStratconCampaignState().allowEarlyVictory();
                addGaugeRow(ContractMeterBar.victoryPoints(currentScore, neededScore, canEndEarly), y++);
            } else {
                lblScore.setName("lblScore");
                lblScore.setText(resourceMap.getString("lblScore.text"));
                txtScore.setName("txtScore");
                txtScore.setText(currentScore + " / " + neededScore);
                addStatRow(lblScore, txtScore, y++);
            }

            // Support points gauge, or a text fallback when there is no positive reserve.
            int currentSupportPoints = contract.getCurrentSupportPoints();
            int maximumSupportPoints = contract.getMaximumSupportPoints();
            if (maximumSupportPoints > 0) {
                addGaugeRow(ContractMeterBar.supportPoints(currentSupportPoints, maximumSupportPoints), y++);
            } else {
                lblSupportPoints.setName("lblSupportPoints");
                lblSupportPoints.setText(resourceMap.getString("lblSupportPoints.text"));
                txtSupport.setName("txtSupport");
                txtSupport.setText(Integer.toString(currentSupportPoints));
                addStatRow(lblSupportPoints, txtSupport, y++);
            }
        }

        // Salvage gauge for a normal salvage percentage; the exchange / no-salvage cases are shown as text among the
        // reference terms below.
        final boolean salvageIsMeter = !contract.isSalvageExchange() && (contract.getSalvagePercent() > 0);
        if (salvageIsMeter) {
            addGaugeRow(ContractMeterBar.salvage(contract.getCurrentSalvagePct(), contract.getSalvagePercent()), y++);
        }

        // Contract timeline: a neutral progress gauge from start to end with a marker for today, shown only while the
        // contract is genuinely under way at its destination system, where the marker's position is meaningful. The
        // engine shifts the start and end dates each day until the player reaches the contract's system (to track the
        // ETA), so the gauge is only stable - and meaningful - once the player is at that system and today falls
        // within start..end. Otherwise (in transit, or after the contract has ended) the dates are shown as a single
        // compact text row instead.
        final String startLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getStartDate());
        final String endLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getEndingDate());
        final PlanetarySystem currentSystem = campaign.getCurrentSystem();
        final boolean atContractSystem = (currentSystem != null) && (contract.getSystem() != null) &&
                                               currentSystem.getId().equals(contract.getSystem().getId());
        final boolean contractInProgress = atContractSystem &&
                                                 !campaign.getLocalDate().isBefore(contract.getStartDate()) &&
                                                 !campaign.getLocalDate().isAfter(contract.getEndingDate());
        if (contractInProgress) {
            final String todayLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate());
            addGaugeRow(ContractMeterBar.timeline(contract.getStartDate(), contract.getEndingDate(),
                  campaign.getLocalDate(), startLabel, endLabel, todayLabel), y++);
        } else {
            JLabel lblDates = new JLabel(resourceMap.getString("lblDates.text"));
            JLabel txtDates = new JLabel(startLabel + " \u2013 " + endLabel);
            addStatRow(lblDates, txtDates, y++);
        }

        // === Reference terms: the static contract details ===
        lblAllyRating.setName("lblAllyRating");
        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        txtAllyRating.setName("txtAllyRating");
        txtAllyRating.setText(contract.getAllySkill() + "/" + ratingNames[contract.getAllyQuality()]);
        addStatRow(lblAllyRating, txtAllyRating, y++);

        lblEnemyRating.setName("lblEnemyRating");
        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
        txtEnemyRating.setName("txtEnemyRating");
        txtEnemyRating.setText(contract.getEnemySkill() + "/" + ratingNames[contract.getEnemyQuality()]);
        addStatRow(lblEnemyRating, txtEnemyRating, y++);

        lblPayout.setName("lblPayout");
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        txtPayout.setName("txtPayout");
        txtPayout.setText(contract.getMonthlyPayOut().toAmountAndSymbolString());
        addStatRow(lblPayout, txtPayout, y++);

        lblCommand.setName("lblCommand");
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        txtCommand.setName("txtCommand");
        txtCommand.setText(contract.getCommandRights().toString());
        txtCommand.setToolTipText(wordWrap(contract.getCommandRights().getToolTipText()));
        addStatRow(lblCommand, txtCommand, y++);

        lblBLC.setName("lblBLC");
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        txtBLC.setName("txtBLC");
        txtBLC.setText(contract.getBattleLossCompensation() + "%");
        addStatRow(lblBLC, txtBLC, y++);

        lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));
        txtSalvageValueMerc = new JLabel();
        txtSalvageValueMerc.setText(contract.getSalvagedByUnit().toAmountAndSymbolString());
        addStatRow(lblSalvageValueMerc, txtSalvageValueMerc, y++);

        lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));
        txtSalvageValueEmployer = new JLabel();
        txtSalvageValueEmployer.setText(contract.getSalvagedByEmployer().toAmountAndSymbolString());
        addStatRow(lblSalvageValueEmployer, txtSalvageValueEmployer, y++);

        // Salvage as text for the exchange / no-salvage cases (the normal case is the gauge in the dashboard above).
        if (!salvageIsMeter) {
            JLabel lblSalvagePct = new JLabel(resourceMap.getString("lblSalvage.text"));
            JLabel txtSalvagePct = new JLabel();
            txtSalvagePct.setName("txtSalvagePct");
            if (contract.isSalvageExchange()) {
                txtSalvagePct.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePercent() + "%)");
            } else {
                txtSalvagePct.setText(resourceMap.getString("none"));
            }
            addStatRow(lblSalvagePct, txtSalvagePct, y++);
        }

        if (campaign.getCampaignOptions().isUseShareSystem()) {
            lblSharePct.setName("lblSharePct");
            lblSharePct.setText(resourceMap.getString("lblSharePct.text"));
            lblSharePct.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
            txtSharePct.setName("txtSharePct");
            txtSharePct.setText(contract.getSharesPercent() + "%");
            txtSharePct.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
            addStatRow(lblSharePct, txtSharePct, y++);
        }

        if (useStratCon) {
            lblCargoRequirement.setName("lblCargoRequirement");
            lblCargoRequirement.setText(resourceMap.getString("lblCargoRequirement.text"));
            txtCargoRequirement.setName("txtCargoRequirement");
            txtCargoRequirement.setText("~" + estimateCargoRequirements(campaign, contract) + 't');
            addStatRow(lblCargoRequirement, txtCargoRequirement, y++);

            if (contract.isActiveOn(campaign.getLocalDate())) {
                String deploymentCoverageTooltip = wordWrap(resourceMap.getString("txtDeploymentCoverage.tooltip"));
                lblDeploymentCoverage.setName("lblDeploymentCoverage");
                lblDeploymentCoverage.setText(resourceMap.getString("lblDeploymentCoverage.text"));
                lblDeploymentCoverage.setToolTipText(deploymentCoverageTooltip);

                int assignedCombatElements = RequiredLancesTableModel.getAssignedCombatElementCount(campaign, contract);
                int requiredCombatElements = contract.getRequiredCombatElements();
                txtDeploymentCoverage.setName("txtDeploymentCoverage");
                txtDeploymentCoverage.setText(assignedCombatElements + " / " + requiredCombatElements);
                txtDeploymentCoverage.setToolTipText(deploymentCoverageTooltip);
                if (RequiredLancesTableModel.hasDeploymentShortfall(campaign, contract)) {
                    txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getBelowContractMinimumForeground());
                } else {
                    txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getFontColorPositive());
                }
                addStatRow(lblDeploymentCoverage, txtDeploymentCoverage, y++);
            }
        }

        addDescriptionPane(contract.getDescription(), y++, 0.0);

        // A trailing vertical glue absorbs any extra height so every row stays anchored to the top of the panel,
        // regardless of which optional rows (and the variable-height description) are present.
        JPanel verticalGlue = new JPanel();
        verticalGlue.setOpaque(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        pnlStats.add(verticalGlue, gridBagConstraints);
    }

    /**
     * Adds a standard two-column stat row: {@code label} in the left column and {@code value} in the right.
     *
     * @param label the label component (left column)
     * @param value the value component (right column)
     * @param gridY the grid row to place them on
     */
    private void addStatRow(JComponent label, JComponent value, int gridY) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(label, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = gridY;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(0, 10, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(value, gbc);
    }

    /**
     * Adds a component that spans both stat columns and is not part of the label/value grid (currently the belligerents
     * panel).
     *
     * @param component the component to add
     * @param gridY     the grid row to place it on
     * @param anchor    the {@link GridBagConstraints} anchor used to position the component within its row
     */
    private void addHeaderRow(JComponent component, int gridY, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = anchor;
        pnlStats.add(component, gbc);
    }

    /**
     * Adds a full-width gauge spanning both stat columns, with uniform spacing so the dashboard gauges read as a
     * group.
     *
     * @param gauge the gauge component to add
     * @param gridY the grid row to place it on
     */
    private void addGaugeRow(JComponent gauge, int gridY) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridY;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(UIUtil.scaleForGUI(1), 0, UIUtil.scaleForGUI(1), 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(gauge, gbc);
    }

    private void addDescriptionPane(String description, int gridY, double weighty) {
        if ((description == null) || description.isBlank()) {
            return;
        }

        txtDesc.setName("txtDesc");
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(description));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridY;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = weighty;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);
    }

}
