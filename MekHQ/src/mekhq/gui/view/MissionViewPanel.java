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

import static java.lang.Math.round;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.utilities.ImageUtilities.scaleImageIcon;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;
import static mekhq.campaign.universe.Factions.getFactionLogo;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.utilities.ContractScore;
import mekhq.campaign.mission.contract.utilities.SalvageUtilities;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.BriefingStyle;
import mekhq.gui.utilities.MarkdownRenderer;
import org.apache.commons.lang3.StringUtils;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissionViewPanel extends JScrollablePanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractViewPanel";

    private final AbstractContract mission;
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

    public MissionViewPanel(AbstractContract mission, CampaignGUI gui) {
        super();
        this.mission = mission;
        this.gui = gui;
        initComponents();
    }

    /**
     * Recomputes and updates the Deployment Coverage label so it reflects the current assignment state without needing
     * to rebuild the whole panel. Has no effect when the panel does not display a deployment coverage value (e.g. for
     * non-AtB contracts, when StratCon is disabled, or when the contract is not currently active).
     */
    public void updateDeploymentCoverage() {
        if (txtDeploymentCoverage == null) {
            return;
        }

        Campaign campaign = gui.getCampaign();
        if (!campaign.getCampaignOptions().isUseStratCon() || !mission.isActiveOn(campaign.getLocalDate())) {
            return;
        }

        int assignedCombatElements = RequiredLancesTableModel.getAssignedCombatElementCount(campaign, mission);
        int requiredCombatElements = mission.getRequiredCombatElements();
        txtDeploymentCoverage.setText(assignedCombatElements + " / " + requiredCombatElements);
        if (RequiredLancesTableModel.hasDeploymentShortfall(campaign, mission)) {
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
        Campaign campaign = gui.getCampaign();

        // TODO : Switch me to use IUnitRating
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
        final String employerTooltip = getFormattedTextAt(RESOURCE_BUNDLE, "belligerents.employer.tooltip",
              mission.getEmployerDisplayName());
        final String enemyTooltip = getFormattedTextAt(RESOURCE_BUNDLE, "belligerents.enemy.tooltip",
              mission.getEnemyDisplayName());
        lblBelligerents = getBelligerentsPanel(mission, gui.getCampaign().getGameYear(), employerTooltip,
              enemyTooltip);
        addHeaderRow(lblBelligerents, y++, GridBagConstraints.NORTH);

        // === Identity: the orienting facts (where, who) ===
        lblLocation.setName("lblLocation");
        lblLocation.setText(getTextAt(RESOURCE_BUNDLE, "lblLocation.text"));
        txtLocation.setName("txtLocation");
        String systemName = mission.getTargetSystemName(campaign.getLocalDate());
        txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                gui.getNavigationTab().showSystem(mission.getTargetSystem());
                gui.setSelectedTab(gui.getNavigationTab());
            }
        });
        addStatRow(lblLocation, txtLocation, y++);

        // === Dashboard: all gauges grouped together, most important first ===
        // Enemy morale (always shown for AtB contracts).
        final MoraleBar.MoraleDisplay moraleDisplay = MoraleBar.getMoraleDisplay(mission);
        MoraleBar moraleBar = new MoraleBar(mission.getMoraleLevel(), moraleDisplay.label());
        moraleBar.setToolTipText(wordWrap(moraleDisplay.tooltip()));
        addGaugeRow(moraleBar, y++);

        final boolean useStratCon = campaign.getCampaignOptions().isUseStratCon();
        if (useStratCon) {
            // Victory points gauge, or a text fallback when there is no positive target.
            int currentScore = ContractScore.getContractScore(campaign.getCampaignOptions().isUseStratConMaplessMode(),
                  mission);
            int neededScore = mission.getRequiredVictoryPoints();
            if (neededScore > 0) {
                addGaugeRow(ContractMeterBar.victoryPoints(currentScore, neededScore,
                      mission.getStratConCampaignState()), y++);
            } else {
                lblScore.setName("lblScore");
                lblScore.setText(getTextAt(RESOURCE_BUNDLE, "lblScore.text"));
                txtScore.setName("txtScore");
                txtScore.setText(currentScore + " / " + neededScore);
                addStatRow(lblScore, txtScore, y++);
            }

            // Support points gauge, or a text fallback when there is no positive reserve.
            int currentSupportPoints = mission.getCurrentSupportPoints();
            int maximumSupportPoints = mission.getMaximumSupportPoints();
            if (maximumSupportPoints > 0) {
                addGaugeRow(ContractMeterBar.supportPoints(currentSupportPoints, maximumSupportPoints), y++);
            } else {
                lblSupportPoints.setName("lblSupportPoints");
                lblSupportPoints.setText(getTextAt(RESOURCE_BUNDLE, "lblSupportPoints.text"));
                txtSupport.setName("txtSupport");
                txtSupport.setText(Integer.toString(currentSupportPoints));
                addStatRow(lblSupportPoints, txtSupport, y++);
            }
        }

        // Salvage gauge for a normal salvage percentage; the exchange / no-salvage cases are shown as text among the
        // reference terms below.
        final boolean salvageIsMeter = !mission.isSalvageExchange() && (mission.getSalvageRightsMultiplier() > 0);
        if (salvageIsMeter) {
            addGaugeRow(ContractMeterBar.salvage(SalvageUtilities.calculateSalvagePercentage(mission.getSalvagedByUnitValue(),
                  mission.getSalvagedByEmployerValue()), (int) round(mission.getSalvageRightsMultiplier() * 100)), y++);
        }

        // Contract timeline: a neutral progress gauge from start to end with a marker for today, shown once the
        // contract is active and the player has landed at the destination. Contracts that have not started yet, are
        // still in transit, or have been completed keep the compact dates row instead.
        final String startLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(mission.getStartDate());
        final String endLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(mission.getEndingDate());
        if (shouldShowContractTimeline(campaign, mission)) {
            final String todayLabel = MekHQ.getMHQOptions().getDisplayFormattedDate(campaign.getLocalDate());
            addGaugeRow(ContractMeterBar.timeline(mission.getStartDate(), mission.getEndingDate(),
                  campaign.getLocalDate(), startLabel, endLabel, todayLabel), y++);
        } else {
            JLabel lblDates = new JLabel(getTextAt(RESOURCE_BUNDLE, "lblDates.text"));
            JLabel txtDates = new JLabel(startLabel + " \u2013 " + endLabel);
            final String timelineFallbackTooltip = contractTimelineFallbackTooltip(campaign, mission);
            lblDates.setToolTipText(timelineFallbackTooltip);
            txtDates.setToolTipText(timelineFallbackTooltip);
            addStatRow(lblDates, txtDates, y++);
        }

        // === Reference terms: the static contract details ===
        lblAllyRating.setName("lblAllyRating");
        lblAllyRating.setText(getTextAt(RESOURCE_BUNDLE, "lblAllyRating.text"));
        txtAllyRating.setName("txtAllyRating");
        txtAllyRating.setText(mission.getEmployerForceSkill() +
                                    "/" +
                                    DragoonRating.fromRating(mission.getEmployerEquipmentRating()).getLabel());
        addStatRow(lblAllyRating, txtAllyRating, y++);

        lblEnemyRating.setName("lblEnemyRating");
        lblEnemyRating.setText(getTextAt(RESOURCE_BUNDLE, "lblEnemyRating.text"));
        txtEnemyRating.setName("txtEnemyRating");
        txtEnemyRating.setText(mission.getEnemyForceSkill() +
                                     "/" +
                                     DragoonRating.fromRating(mission.getEnemyEquipmentRating()).getLabel());
        addStatRow(lblEnemyRating, txtEnemyRating, y++);

        if (campaign.getCampaignOptions().get(CampaignOption.USE_SHARE_SYSTEM)) {
            lblSharePct.setName("lblSharePct");
            lblSharePct.setText(getTextAt(RESOURCE_BUNDLE, "lblSharePct.text"));
            txtSharePct.setName("txtSharePct");
            txtSharePct.setText(mission.getSharesPercent() + "%");
            addStatRow(lblSharePct, txtSharePct, y++);
        }

        lblPayout.setName("lblPayout");
        lblPayout.setText(getTextAt(RESOURCE_BUNDLE, "lblPayout.text"));
        txtPayout.setName("txtPayout");
        txtPayout.setText(mission.getMonthlyPayOut().toAmountAndSymbolString());
        addStatRow(lblPayout, txtPayout, y++);

        lblCommand.setName("lblCommand");
        lblCommand.setText(getTextAt(RESOURCE_BUNDLE, "lblCommand.text"));
        txtCommand.setName("txtCommand");
        txtCommand.setText(mission.getCommandRights().toString());
        txtCommand.setToolTipText(wordWrap(mission.getCommandRights().getToolTipText()));
        addStatRow(lblCommand, txtCommand, y++);

        lblBLC.setName("lblBLC");
        lblBLC.setText(getTextAt(RESOURCE_BUNDLE, "lblBLC.text"));
        txtBLC.setName("txtBLC");
        txtBLC.setText((int) round(mission.getBattlefieldLossMultiplier() * 100) + "%");
        addStatRow(lblBLC, txtBLC, y++);

        lblSalvageValueMerc = new JLabel(getTextAt(RESOURCE_BUNDLE, "lblSalvageValueMerc.text"));
        txtSalvageValueMerc = new JLabel();
        txtSalvageValueMerc.setText(mission.getSalvagedByUnitValue().toAmountAndSymbolString());
        addStatRow(lblSalvageValueMerc, txtSalvageValueMerc, y++);

        lblSalvageValueEmployer = new JLabel(getTextAt(RESOURCE_BUNDLE, "lblSalvageValueEmployer.text"));
        txtSalvageValueEmployer = new JLabel();
        txtSalvageValueEmployer.setText(mission.getSalvagedByEmployerValue().toAmountAndSymbolString());
        addStatRow(lblSalvageValueEmployer, txtSalvageValueEmployer, y++);

        // Salvage as text for the exchange / no-salvage cases (the normal case is the gauge in the dashboard above).
        if (!salvageIsMeter) {
            JLabel lblSalvagePct = new JLabel(getTextAt(RESOURCE_BUNDLE, "lblSalvage.text"));
            JLabel txtSalvagePct = new JLabel();
            txtSalvagePct.setName("txtSalvagePct");
            if (mission.isSalvageExchange()) {
                txtSalvagePct.setText(getTextAt(RESOURCE_BUNDLE, "exchange") +
                                            " (" +
                                            SalvageUtilities.calculateSalvagePercentage(mission.getSalvagedByUnitValue(),
                                                  mission.getSalvagedByEmployerValue()) +
                                            "%)");
            } else {
                txtSalvagePct.setText(getTextAt(RESOURCE_BUNDLE, "none"));
            }
            addStatRow(lblSalvagePct, txtSalvagePct, y++);
        }

        if (campaign.getCampaignOptions().get(CampaignOption.USE_SHARE_SYSTEM)) {
            lblSharePct.setName("lblSharePct");
            lblSharePct.setText(getTextAt(RESOURCE_BUNDLE, "lblSharePct.text"));
            lblSharePct.setToolTipText(wordWrap(mission.getMoraleLevel().getToolTipText()));
            txtSharePct.setName("txtSharePct");
            txtSharePct.setText(mission.getSharesPercent() + "%");
            txtSharePct.setToolTipText(wordWrap(mission.getMoraleLevel().getToolTipText()));
            addStatRow(lblSharePct, txtSharePct, y++);
        }

        if (useStratCon) {
            lblCargoRequirement.setName("lblCargoRequirement");
            lblCargoRequirement.setText(getTextAt(RESOURCE_BUNDLE, "lblCargoRequirement.text"));
            txtCargoRequirement.setName("txtCargoRequirement");
            txtCargoRequirement.setText("~" + estimateCargoRequirements(campaign, mission) + 't');
            addStatRow(lblCargoRequirement, txtCargoRequirement, y++);

            if (mission.isActiveOn(campaign.getLocalDate())) {
                String deploymentCoverageTooltip = wordWrap(getTextAt(RESOURCE_BUNDLE,
                      "txtDeploymentCoverage.tooltip"));
                lblDeploymentCoverage.setName("lblDeploymentCoverage");
                lblDeploymentCoverage.setText(getTextAt(RESOURCE_BUNDLE, "lblDeploymentCoverage.text"));
                lblDeploymentCoverage.setToolTipText(deploymentCoverageTooltip);

                int assignedCombatElements = RequiredLancesTableModel.getAssignedCombatElementCount(campaign, mission);
                int requiredCombatElements = mission.getRequiredCombatElements();
                txtDeploymentCoverage.setName("txtDeploymentCoverage");
                txtDeploymentCoverage.setText(assignedCombatElements + " / " + requiredCombatElements);
                txtDeploymentCoverage.setToolTipText(deploymentCoverageTooltip);
                if (RequiredLancesTableModel.hasDeploymentShortfall(campaign, mission)) {
                    txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getBelowContractMinimumForeground());
                } else {
                    txtDeploymentCoverage.setForeground(MekHQ.getMHQOptions().getFontColorPositive());
                }
                addStatRow(lblDeploymentCoverage, txtDeploymentCoverage, y++);
            }
        }

        addDescriptionPane(mission.getDescription(), y++, 0.0);

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

    static boolean shouldShowContractTimeline(Campaign campaign, AbstractContract contract) {
        final AbstractLocation currentLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        if (currentLocation == ILocation.NO_LOCATION) {
            return false;
        }

        final PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        final PlanetarySystem contractSystem = contract.getTargetSystem();
        return contract.isActiveOn(campaign.getLocalDate()) &&
                     (currentLocation != null) && currentLocation.isOnPlanet() &&
                     (currentSystem != null) && (contractSystem != null) &&
                     currentSystem.getId().equals(contractSystem.getId());
    }

    static String contractTimelineFallbackTooltip(Campaign campaign, AbstractContract contract) {
        final String contractLocation = contract.getTargetSystemName(campaign.getLocalDate());
        final String currentLocation = currentLocationDescription(campaign);
        return wordWrap(getFormattedTextAt(RESOURCE_BUNDLE, "contractTimelineBar.fallback.tooltip", contractLocation,
              currentLocation));
    }

    static String currentLocationDescription(Campaign campaign) {
        final AbstractLocation currentLocation = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        if (currentLocation == ILocation.NO_LOCATION) {
            return getTextAt(RESOURCE_BUNDLE, "contractTimelineBar.location.unknown");
        }

        final PlanetarySystem currentSystem = currentLocation.getCurrentSystem();
        final LocalDate currentDate = campaign.getLocalDate();
        if (currentLocation.isOnPlanet()) {
            final Planet currentPlanet = currentLocation.getPlanet();
            final String planetName = (currentPlanet == null) ? currentSystem.getPrintableName(currentDate) :
                                            currentPlanet.getPrintableName(currentDate);
            return getFormattedTextAt(RESOURCE_BUNDLE, "contractTimelineBar.location.landed", planetName);
        }

        final String systemName = currentSystem.getPrintableName(currentDate);
        final String locationKey = currentLocation.isAtJumpPoint() ? "contractTimelineBar.location.jumpPoint" :
                                         "contractTimelineBar.location.inTransit";
        return getFormattedTextAt(RESOURCE_BUNDLE, locationKey, systemName);
    }

    /**
     * Creates and returns a {@link JPanel} containing the belligerent factions' logos for the specified game year.
     *
     * <p>This panel displays the employer and enemy faction logos side by side, separated by a styled divider.
     * The logos are determined based on the provided game year and faction codes, scaled appropriately for the
     * GUI.</p>
     *
     * @param gameYear        the year used to determine which faction logos to display
     * @param employerTooltip the tooltip to show on the employer (left) logo, or {@code null} for none
     * @param enemyTooltip    the tooltip to show on the enemy (right) logo, or {@code null} for none
     *
     * @return a {@link JPanel} with the employer and enemy faction logos, with a divider in between
     *
     * @author Illiani
     * @since 0.50.06
     */
    private JPanel getBelligerentsPanel(AbstractContract contract, int gameYear, String employerTooltip,
          String enemyTooltip) {
        final int SIZE = 64;

        String employer = contract.getEmployerFactionCode();
        ImageIcon employerImage = getFactionLogo(gameYear, employer);
        employerImage = scaleImageIcon(employerImage, SIZE, true);
        JLabel employerLabel = new JLabel(employerImage);
        employerLabel.setToolTipText(employerTooltip);

        JLabel divider = new JLabel("/");
        divider.setHorizontalAlignment(SwingConstants.CENTER);
        int fontSize = scaleForGUI(SIZE); // scaleImageIcon already includes the necessary scaling
        divider.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        divider.setForeground(new Color(0, 0, 0, 128));

        String enemy = contract.getEnemyFactionCode();
        ImageIcon enemyImage = getFactionLogo(gameYear, enemy);
        enemyImage = scaleImageIcon(enemyImage, SIZE, true);
        JLabel enemyLabel = new JLabel(enemyImage);
        enemyLabel.setToolTipText(enemyTooltip);

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(employerLabel);
        panel.add(divider);
        panel.add(enemyLabel);

        return panel;
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
        if (StringUtils.isBlank(description)) {
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
