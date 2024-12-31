/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.Minefield;
import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

import static mekhq.campaign.mission.AtBDynamicScenarioFactory.translateTemplateObjectives;
import static mekhq.campaign.personnel.SkillType.S_LEADER;
import static mekhq.campaign.stratcon.StratconRulesManager.BASE_LEADERSHIP_BUDGET;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.stratcon.StratconRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.stratcon.StratconRulesManager.getEligibleLeadershipUnits;
import static mekhq.campaign.stratcon.StratconRulesManager.processReinforcementDeployment;
import static mekhq.gui.baseComponents.AbstractMHQNagDialog.getSpeakerDescription;
import static mekhq.gui.dialog.resupplyAndCaches.ResupplyDialogUtilities.getSpeakerIcon;
import static mekhq.utilities.ImageUtilities.scaleImageIconToWidth;

/**
 * UI for managing force/unit assignments for individual StratCon scenarios.
 */
public class StratconScenarioWizard extends JDialog {
    private StratconScenario currentScenario;
    private final Campaign campaign;
    private StratconTrackState currentTrackState;
    private StratconCampaignState currentCampaignState;
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
            MekHQ.getMHQOptions().getLocale());

    private final Map<String, JList<Force>> availableForceLists = new HashMap<>();
    private final Map<String, JList<Unit>> availableUnitLists = new HashMap<>();

    private JList<Unit> availableInfantryUnits = new JList<>();
    private JList<Unit> availableLeadershipUnits = new JList<>();

    private JPanel contentPanel;
    private JButton btnCommit;

    private static final MMLogger logger = MMLogger.create(StratconScenarioWizard.class);

    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
        this.setModalityType(ModalityType.APPLICATION_MODAL);
    }

    /**
     * Selects a scenario on a particular track in a particular campaign.
     */
    public void setCurrentScenario(StratconScenario scenario, StratconTrackState trackState,
            StratconCampaignState campaignState) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        currentTrackState = trackState;
        availableForceLists.clear();
        availableUnitLists.clear();

        setUI();
    }

    /**
     * Sets up the UI as appropriate for the currently selected scenario.
     */
    private void setUI() {
        setTitle(resources.getString("scenarioSetupWizard.title"));
        getContentPane().removeAll();

        // Create a new panel to hold all components
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        // Add instructions
        setInstructions(gbc);

        // Move to the next row
        gbc.gridy++;
        boolean reinforcements = Objects.requireNonNull(currentScenario.getCurrentState()) != ScenarioState.UNRESOLVED;

        // Add UI for assigning forces
        setAssignForcesUI(gbc, reinforcements);

        // Handle optional UI for eligible leadership, defensive points, etc.
        if (!reinforcements) {
            gbc.gridy++;
            int leadershipSkill = currentScenario.getBackingScenario().getLanceCommanderSkill(S_LEADER, campaign);
            List<Unit> eligibleLeadershipUnits = getEligibleLeadershipUnits(
                campaign, currentScenario.getPrimaryForceIDs(), leadershipSkill);
            eligibleLeadershipUnits.sort(Comparator.comparing(this::getForceNameReversed));

            if (!eligibleLeadershipUnits.isEmpty() && leadershipSkill > 0) {
                setLeadershipUI(gbc, eligibleLeadershipUnits, leadershipSkill);
                gbc.gridy++;
            }

            if (currentScenario.getNumDefensivePoints() > 0) {
                setDefensiveUI(gbc);
                gbc.gridy++;
            }
        }

        // Add navigation buttons
        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);

        // Wrap contentPanel in a scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Add the scrollPane to the content pane of the dialog
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        pack();
        validate();
    }

    /**
     * Returns a concatenated string of a unit's force hierarchy, in reversed order,
     * starting from the highest parent Force going down to the given unit's direct Force.
     * <p>
     * If the unit does not belong to any Force, an empty string is returned.
     *
     * @param unit The Unit whose Force hierarchy names are to be returned.
     * @return A concatenated string of Force names in reversed order separated by a slash,
     *         or an empty string if the unit is not assigned to any Force.
     */
    private String getForceNameReversed(Unit unit) {
        List<String> forceNames = new ArrayList<>();

        Force force = campaign.getForce(unit.getForceId());

        if (force == null) {
            return "";
        }

        forceNames.add(force.getName());

        Force parentForce = force.getParentForce();
        while (parentForce != null) {
            forceNames.add(parentForce.getName());

            parentForce = parentForce.getParentForce();
        }

        Collections.reverse(forceNames);

        StringBuilder forceNameReversed = new StringBuilder();

        for (String forceName : forceNames) {
            forceNameReversed.append(forceName);
        }

        return forceNameReversed.toString();
    }

    /**
     * Worker function that sets up the instructions for the currently selected
     * scenario.
     */
    private void setInstructions(GridBagConstraints gbc) {
        JLabel lblInfo = new JLabel();
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append("<html>");

        if (currentTrackState.isGmRevealed()
            || currentTrackState.getRevealedCoords().contains(currentScenario.getCoords())
            || (currentScenario.getDeploymentDate() != null)) {
            labelBuilder.append(currentScenario.getInfo(campaign));
        }

        labelBuilder.append("<br/>");
        lblInfo.setText(labelBuilder.toString());

        contentPanel.add(lblInfo, gbc);
    }

    /**
     * Worker function that sets up the "assign forces to scenario" UI elements.
     */
    private void setAssignForcesUI(GridBagConstraints gbc, boolean reinforcements) {
        // Get eligible templates depending on reinforcement status
        List<ScenarioForceTemplate> eligibleForceTemplates = reinforcements
            ? currentScenario.getScenarioTemplate().getAllPlayerReinforcementForces()
            : currentScenario.getScenarioTemplate().getAllPrimaryPlayerForces();

        for (ScenarioForceTemplate forceTemplate : eligibleForceTemplates) {
            // Create a panel for each force template
            JPanel forcePanel = new JPanel();
            forcePanel.setLayout(new GridBagLayout());
            GridBagConstraints localGbc = new GridBagConstraints();
            localGbc.gridx = 0;
            localGbc.gridy = 0;

            // Add instructions for assigning forces
            String reinforcementMessage = currentCampaignState.getSupportPoints() > 0
                ? resources.getString("selectReinforcementsForTemplate.Text")
                : resources.getString("selectReinforcementsForTemplateNoSupportPoints.Text");

            String labelText = reinforcements ? reinforcementMessage
                : resources.getString("selectForceForTemplate.Text");

            JLabel assignForceListInstructions = new JLabel(labelText);
            forcePanel.add(assignForceListInstructions, localGbc);

            // Add a list to display available forces
            localGbc.gridy = 1;
            JLabel selectedForceInfo = new JLabel();
            JList<Force> availableForceList = addAvailableForceList(forcePanel, localGbc, forceTemplate);
            availableForceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // Add a listener to handle changes to the selected force
            availableForceList
                    .addListSelectionListener(e -> {
                        availableForceSelectorChanged(e, selectedForceInfo, reinforcements);
                        btnCommit.setEnabled(true);
                    });

            // Store the list in the map for later reference
            availableForceLists.put(forceTemplate.getForceName(), availableForceList);

            // Add the selected force info to the panel
            localGbc.gridx = 1;
            forcePanel.add(selectedForceInfo, localGbc);

            // Add the forcePanel to contentPanel (not getContentPane)
            contentPanel.add(forcePanel, gbc);
            gbc.gridy++;
        }
    }

    /**
     * Sets up the UI for "defensive elements", such as infantry, gun emplacements, minefields, etc.
     *
     * @param gbc GridBagConstraints for layout positioning.
     */
    private void setDefensiveUI(GridBagConstraints gbc) {
        // Label with defensive posture instructions
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblDefensivePostureInstructions = new JLabel(
            resources.getString("lblDefensivePostureInstructions.Text"));
        contentPanel.add(lblDefensivePostureInstructions, gbc);

        gbc.gridy++;

        // Obtain eligible infantry units
        List<Unit> eligibleInfantryUnits = StratconRulesManager.getEligibleDefensiveUnits(campaign);
        eligibleInfantryUnits.sort(Comparator.comparing(Unit::getName));

        // Add a unit selector for infantry units
        availableInfantryUnits = addIndividualUnitSelector(
            eligibleInfantryUnits, gbc, currentScenario.getNumDefensivePoints(), false);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;

        // Label to display the minefield count
        JLabel lblDefensiveMinefieldCount = new JLabel(
            String.format(resources.getString("lblDefensiveMinefieldCount.text"),
                currentScenario.getNumDefensivePoints()));

        // Add a listener to update the minefield count label when infantry units are selected
        availableInfantryUnits.addListSelectionListener(
            e -> availableInfantrySelectorChanged(lblDefensiveMinefieldCount));

        contentPanel.add(lblDefensiveMinefieldCount, gbc);
    }

    private void setLeadershipUI(GridBagConstraints gbc, List<Unit> eligibleUnits, int leadershipSkill) {
        // Leadership budget is capped at 5 levels
        int leadershipBudget = Math.min(BASE_LEADERSHIP_BUDGET * leadershipSkill, BASE_LEADERSHIP_BUDGET * 5);
        int maxSelectionSize = leadershipBudget - currentScenario.getLeadershipPointsUsed();

        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblLeadershipInstructions = new JLabel(
            String.format(resources.getString("lblLeadershipInstructions.Text"), maxSelectionSize));
        contentPanel.add(lblLeadershipInstructions, gbc);

        gbc.gridy++;

        availableLeadershipUnits = addIndividualUnitSelector(eligibleUnits, gbc, maxSelectionSize,
            true);
    }

    /**
     * Add an "available force list" to the given control
     */
    private JList<Force> addAvailableForceList(JPanel parent, GridBagConstraints gbc,
            ScenarioForceTemplate forceTemplate) {
        JScrollPane forceListContainer = new JScrollPaneWithSpeed();

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign,
            StratconRulesManager.getAvailableForceIDs(forceTemplate.getAllowedUnitType(), campaign, currentTrackState,
                (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS),
                currentScenario, currentCampaignState));

        JList<Force> availableForceList = new JList<>();
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));

        forceListContainer.setViewportView(availableForceList);

        parent.add(forceListContainer, gbc);
        return availableForceList;
    }

    /**
     * Adds an individual unit selector, given a list of individual units, a global grid bag constraint set,
     * and a maximum selection size.
     *
     * @param units            The list of units to use as a data source.
     * @param gridBagConstraints GridBagConstraints object to position the selector panel.
     * @param maxSelectionSize Maximum number of units that can be selected.
     * @param usesBV           Whether to track the Battle Value (BV) of selected items or simply count.
     * @return A JList of units that can be selected.
     */
    private JList<Unit> addIndividualUnitSelector(List<Unit> units, GridBagConstraints gridBagConstraints,
                                                  int maxSelectionSize, boolean usesBV) {
        // Create the panel for the individual unit selector
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridBagLayout());

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.WEST;

        // Instructions for selecting units
        JLabel instructions = new JLabel(
            String.format(resources.getString("lblSelectIndividualUnits.text"), maxSelectionSize));
        unitPanel.add(instructions, localGbc);

        localGbc.gridy++;
        DefaultListModel<Unit> availableModel = new DefaultListModel<>();
        availableModel.addAll(units);

        // Add labels for unit selection details
        JLabel unitStatusLabel = new JLabel();
        JLabel unitSelectionLabel = new JLabel(resources.getString("unitSelectLabelDefaultValue.text"));

        // Add the "# units selected" label
        localGbc.gridy++;
        unitPanel.add(unitSelectionLabel, localGbc);

        // Create the unit selection list
        JList<Unit> availableUnits = new JList<>(availableModel);
        availableUnits.setCellRenderer(new ScenarioWizardUnitRenderer());
        availableUnits.addListSelectionListener(
            e -> availableUnitSelectorChanged(e, unitSelectionLabel, unitStatusLabel,
                maxSelectionSize, usesBV));

        // Scroll pane for the unit selection list
        JScrollPane unitScrollPane = new JScrollPane(availableUnits);
        localGbc.gridy++;
        unitPanel.add(unitScrollPane, localGbc);

        // Add the 'unit status' label
        localGbc.gridx++;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        unitPanel.add(unitStatusLabel, localGbc);

        // Add the unitPanel to the contentPanel
        contentPanel.add(unitPanel, gridBagConstraints);

        return availableUnits;
    }

    /**
     * Worker function that builds an "html-enabled" string indicating the brief
     * status of a force
     */
    private String buildForceStatus(Force f, boolean showForceCost) {
        StringBuilder sb = new StringBuilder();

        sb.append(f.getFullName());
        sb.append(": ");
        if (showForceCost) {
            sb.append(buildForceCost(f.getId()));
        }
        sb.append("<br/>");

        for (UUID unitID : f.getUnits()) {
            Unit u = campaign.getUnit(unitID);
            sb.append(buildUnitStatus(u));
        }

        return sb.toString();
    }

    /**
     * Worker function that builds an "html-enabled" string indicating the brief
     * status of an individual unit
     */
    private String buildUnitStatus(Unit u) {
        StringBuilder sb = new StringBuilder();

        sb.append(u.getName());
        sb.append(": ");
        sb.append(u.getStatus());

        int injuryCount = (int) u.getCrew().stream().filter(p -> p.hasInjuries(true)).count();

        if (injuryCount > 0) {
            sb.append(String.format(
                    ", <span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor()
                            + "'>%d/%d injured crew</span>",
                    injuryCount,
                    u.getCrew().size()));
        }

        sb.append("<br/>");
        return sb.toString();
    }

    /**
     * Worker function that builds an indicator of what it will take to deploy a
     * particular force
     * to the current scenario as reinforcements.
     */
    private String buildForceCost(int forceID) {
        StringBuilder costBuilder = new StringBuilder();
        costBuilder.append('(');

        switch (StratconRulesManager.getReinforcementType(forceID, currentTrackState, campaign, currentCampaignState)) {
            case REGULAR:
                costBuilder.append(resources.getString("regular.text"));
                break;
            case CHAINED_SCENARIO:
                costBuilder.append(resources.getString("fromChainedScenario.text"));
                break;
            case AUXILIARY:
                costBuilder.append(resources.getString("auxiliary.text"));
                break;
            default:
                costBuilder.append("Error: Invalid Reinforcement Type");
                break;
        }

        costBuilder.append(')');
        return costBuilder.toString();
    }

    /**
     * Creates and configures the "Commit" navigation button and adds it to the content panel.
     * The behavior of the button is determined based on the state of the current scenario:
     * <ul>
     *   <li>If the scenario state is {@code UNRESOLVED}, the button triggers the immediate commit
     *       action via {@link #btnCommitClicked(ActionEvent, Integer, boolean)}.</li>
     *   <li>If the scenario state requires reinforcements, the button opens the
     *       {@link #reinforcementConfirmDialog()} and is only enabled if there are enough
     *       support points available.</li>
     * </ul>
     *
     * <p>The button is added to the layout using the provided {@link GridBagConstraints},
     * which defines its position and alignment within the panel.</p>
     *
     * @param constraints the {@link GridBagConstraints} used for positioning the button within the panel.
     */
    private void setNavigationButtons(GridBagConstraints constraints) {
        // Create the commit button
        btnCommit = new JButton("Commit");
        btnCommit.setActionCommand("COMMIT_CLICK");
        if (currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            btnCommit.addActionListener(evt -> btnCommitClicked(evt,
                null, false));
        } else {
            btnCommit.addActionListener(evt -> reinforcementConfirmDialog());
            btnCommit.setEnabled(currentCampaignState.getSupportPoints() > 0);
        }

        // Configure layout constraints for the button
        constraints.gridheight = GridBagConstraints.REMAINDER;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.CENTER;

        // Add the commit button to the content panel
        contentPanel.add(btnCommit, constraints);
    }

    /**
     * Creates and displays the "Reinforcement Confirmation" dialog, allowing the user to review,
     * adjust, and commit reinforcements to a scenario. The dialog provides information about
     * the current target roll modifiers, the ability to adjust Support Points, and handles
     * calculation and changes to the reinforcement target number dynamically.
     *
     * <p>The dialog includes the following components:</p>
     * <ul>
     *   <li><b>Left Panel:</b> Contains details about the speaker, including an icon (if available)
     *       and a description of their role in the campaign.</li>
     *   <li><b>Right Panel:</b> Shows a breakdown of the current target roll modifiers and target number.</li>
     *   <li><b>Support Point Selector:</b> A spinner that allows the user to adjust the number of
     *       Support Points they wish to spend to modify the target number. The maximum available
     *       Support Points are based on the current campaign state.</li>
     *   <li><b>Check/Confirm Buttons:</b> Includes two confirm buttons (standard and GM-specific)
     *       that allow the user to commit reinforcements or additional actions, along with a cancel button
     *       to close the dialog without making any changes.</li>
     *   <li><b>Info Panel:</b> A supplemental label with additional guidance or information displayed
     *       below the buttons, which can help inform the user about the reinforcement process or provide
     *       clarification about decisions made.</li>
     * </ul>
     */
    private void reinforcementConfirmDialog() {
        final int LEFT_WIDTH = UIUtil.scaleForGUI(200);
        final int RIGHT_WIDTH = UIUtil.scaleForGUI(400);

        JDialog dialog = new JDialog();
        dialog.setTitle(resources.getString("incomingTransmission.title"));
        dialog.setLayout(new BorderLayout());

        // Main Panel to hold left and right boxes
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        // Left box (commandLiaison details)
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Get commandLiaison details
        Person commandLiaison = campaign.getSeniorAdminCommandPerson();
        String speakerName = (commandLiaison != null) ? commandLiaison.getFullTitle() : campaign.getName();

        // Add commandLiaison image (icon)
        ImageIcon speakerIcon = getSpeakerIcon(campaign, commandLiaison);
        if (speakerIcon != null) {
            speakerIcon = scaleImageIconToWidth(speakerIcon, 100);
        }
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(speakerIcon);
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Speaker description (below the icon)
        StringBuilder speakerDescription = getSpeakerDescription(campaign, commandLiaison, speakerName);
        JLabel leftDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                LEFT_WIDTH, speakerDescription));
        leftDescription.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to leftBox
        leftBox.add(imageLabel);
        leftBox.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        leftBox.add(leftDescription);

        // Add leftBox to mainPanel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        mainPanel.add(leftBox, gbc);

        // Right box (message details)
        JPanel rightBox = new JPanel(new BorderLayout());
        rightBox.setBorder(BorderFactory.createEtchedBorder());

        TargetRoll reinforcementTargetNumber = calculateReinforcementTargetNumber(
            campaign, currentScenario, commandLiaison, currentCampaignState, currentCampaignState.getContract());
        int targetNumber = reinforcementTargetNumber.getValue();

        StringBuilder rightDescriptionMessage = new StringBuilder();
        rightDescriptionMessage.append(String.format(
            resources.getString("reinforcementConfirmation.introduction"),
            campaign.getCommanderAddress(false)));
        rightDescriptionMessage.append(resources.getString("reinforcementConfirmation.breakdown"));

        StringBuilder breakdownContents = new StringBuilder();
        for (TargetRollModifier modifier : reinforcementTargetNumber.getModifiers()) {
            breakdownContents.append(String.format("%s: %d<br>", modifier.getDesc(), modifier.getValue()));
        }
        breakdownContents.append(String.format("<b>%s:</b> %s",
            resources.getString("reinforcementConfirmation.breakdown.total"),
            reinforcementTargetNumber.getValue())).append('+');
        rightDescriptionMessage.append(breakdownContents);

        JLabel rightDescription = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
                RIGHT_WIDTH, rightDescriptionMessage)
        );
        rightBox.add(rightDescription);

        // Add rightBox to mainPanel
        gbc.gridx = 1;
        gbc.weightx = 1; // Allow horizontal stretching
        mainPanel.add(rightBox, gbc);

        // Add mainPanel to dialog
        dialog.add(mainPanel, BorderLayout.CENTER);

        // Spinner Panel (Support Points)
        JPanel spinnerPanel = new JPanel();
        spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.X_AXIS));
        JLabel lblSpinner = new JLabel(String.format("%s: ",
            resources.getString("reinforcementConfirmation.spinnerLabel")));
        lblSpinner.setToolTipText(resources.getString("reinforcementConfirmation.spinnerLabel.tooltip"));
        lblSpinner.setAlignmentY(Component.CENTER_ALIGNMENT);

        int availableSupportPoints = currentCampaignState.getSupportPoints();
        JSpinner spnSupportPointCost = new JSpinner(
            new SpinnerNumberModel(1, 1, availableSupportPoints, 1));
        spnSupportPointCost.setToolTipText(resources.getString("reinforcementConfirmation.spinnerLabel.tooltip"));
        spnSupportPointCost.setMaximumSize(spnSupportPointCost.getPreferredSize());
        spnSupportPointCost.setAlignmentY(Component.CENTER_ALIGNMENT);
        spinnerPanel.add(lblSpinner);
        spinnerPanel.add(spnSupportPointCost);
        spinnerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Combine spinnerPanel and checkboxPanel into a vertical layout panel
        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
        subPanel.add(spinnerPanel);
        subPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing between sections

        // Buttons panel
        JPanel buttonPanel = new JPanel();

        JButton reinforceButton = new JButton(resources.getString("reinforcementConfirmation.confirmButton"));
        reinforceButton.setToolTipText(resources.getString("reinforcementConfirmation.confirmButton.tooltip"));
        reinforceButton.addActionListener(evt -> {
            int spentSupportPoints = (int) spnSupportPointCost.getValue();
            int finalTargetNumber = targetNumber - ((spentSupportPoints - 1) * 2);

            currentCampaignState.setSupportPoints(currentCampaignState.getSupportPoints() - spentSupportPoints);

            btnCommitClicked(evt, finalTargetNumber, false);
            dialog.dispose();
        });

        JButton reinforceButtonGM = new JButton(resources.getString("reinforcementConfirmation.confirmButton.gm"));
        reinforceButtonGM.setToolTipText(resources.getString("reinforcementConfirmation.confirmButton.gm.tooltip"));
        reinforceButtonGM.addActionListener(evt -> {
            int spentSupportPoints = (int) spnSupportPointCost.getValue();
            int finalTargetNumber = targetNumber - ((spentSupportPoints - 1) * 2);

            currentCampaignState.setSupportPoints(currentCampaignState.getSupportPoints() - spentSupportPoints);

            btnCommitClicked(evt, finalTargetNumber, true);
            dialog.dispose();
        });
        reinforceButtonGM.setVisible(campaign.isGM());

        JButton cancelButton = new JButton(resources.getString("reinforcementConfirmation.cancelButton"));
        cancelButton.addActionListener(evt -> dialog.dispose());
        buttonPanel.add(reinforceButton);
        buttonPanel.add(reinforceButtonGM);
        buttonPanel.add(cancelButton);

        // Info label panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel lblInfo = new JLabel(
            String.format("<html><div style='width: %s; text-align:center;'>%s</div></html>",
            RIGHT_WIDTH + LEFT_WIDTH,
            String.format(resources.getString("reinforcementConfirmation.addendum"))));
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        infoPanel.add(lblInfo, BorderLayout.CENTER);
        infoPanel.setBorder(BorderFactory.createEtchedBorder());

        // South Panel
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(subPanel, BorderLayout.NORTH);
        southPanel.add(buttonPanel, BorderLayout.CENTER);
        southPanel.add(infoPanel, BorderLayout.SOUTH);

        // Add southPanel to the dialog
        dialog.add(southPanel, BorderLayout.SOUTH);

        // Dialog settings
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Handles the event when the user clicks the 'commit' button.
     * This method processes the selected forces, reinforcements, and scenario states,
     * committing primary forces, reinforcements, and units based on the current
     * state of the scenario, and updating the scenario as appropriate.
     *
     * <p>Depending on the current state of the scenario, this method either:
     * <ul>
     *   <li>Commits primary forces to the scenario if in the unresolved state.</li>
     *   <li>Commits reinforcement forces and processes their deployment.</li>
     *   <li>Adds units (e.g., infantry and leadership units) to the scenario.</li>
     *   <li>Assigns deployed forces to the campaign track and updates scenario parameters (e.g., minefields).</li>
     *   <li>Publishes scenarios to the campaign and allows immediate play if forces have been committed.</li>
     * </ul>
     *
     * @param evt the {@link ActionEvent} triggered by the button press
     * @param reinforcementTargetNumber the number representing the reinforcement
     *        target threshold used when processing reinforcement deployment
     * @param isGMReinforcement   {@code true} if the player is using GM powers to bypass the
     *                                       reinforcement check, {@code false} otherwise.
     */
    private void btnCommitClicked(ActionEvent evt, @Nullable Integer reinforcementTargetNumber,
                                  boolean isGMReinforcement) {
        // go through all the force lists and add the selected forces to the scenario
        for (String templateID : availableForceLists.keySet()) {
            for (Force force : availableForceLists.get(templateID).getSelectedValuesList()) {
                if (templateID.equals(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID)) {
                    if (currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
                        currentScenario.addForce(force, templateID, campaign);
                    }
                } else if (currentScenario.getCurrentState() == ScenarioState.PRIMARY_FORCES_COMMITTED) {
                    ReinforcementEligibilityType reinforcementType = StratconRulesManager.getReinforcementType(
                        force.getId(), currentTrackState,
                        campaign, currentCampaignState);

                    // if we failed to deploy as reinforcements, move on to the next force
                    if (reinforcementTargetNumber == null) {
                        // If we've passed a null value into a reinforcement attempt, treat it as
                        // an impossibly large number, so that it's clear something is wrong.
                        reinforcementTargetNumber = 999;
                        logger.error("null reinforcementTargetNumber incorrectly passed into" +
                            " btnCommitClicked for reinforcement attempt");
                    }

                    ReinforcementResultsType reinforcementResults = processReinforcementDeployment(force,
                        reinforcementType, currentCampaignState, currentScenario, campaign,
                        reinforcementTargetNumber, isGMReinforcement);

                    if (reinforcementResults.ordinal() >= FAILED.ordinal()) {
                        currentScenario.addFailedReinforcements(force.getId());
                        continue;
                    }

                    currentScenario.addForce(force, templateID, campaign);

                    if (reinforcementResults == DELAYED) {
                        List<UUID> delayedReinforcements =
                            currentScenario.getBackingScenario().getFriendlyDelayedReinforcements();

                        for (UUID unitId : force.getAllUnits(true)) {
                            try {
                                delayedReinforcements.add(unitId);
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                }
            }
        }

        for (String templateID : availableUnitLists.keySet()) {
            for (Unit unit : availableUnitLists.get(templateID).getSelectedValuesList()) {
                currentScenario.addUnit(unit, templateID, false);
            }
        }

        for (Unit unit : availableInfantryUnits.getSelectedValuesList()) {
            currentScenario.addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, false);
        }

        for (Unit unit : availableLeadershipUnits.getSelectedValuesList()) {
            currentScenario.addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, true);
        }

        // every force that's been deployed to this scenario gets assigned to the track
        for (int forceID : currentScenario.getAssignedForces()) {
            StratconRulesManager.processForceDeployment(currentScenario.getCoords(),
                    forceID, campaign, currentTrackState, false);
        }

        // scenarios that haven't had primary forces committed yet get those committed
        // now and the scenario gets published to the campaign and may be played immediately
        // from the briefing room that being said, give the player a chance to commit reinforcements too
        if (currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            // if we've already generated forces and applied modifiers, no need to do it
            // twice
            if (!currentScenario.getBackingScenario().isFinalized()) {
                AtBDynamicScenarioFactory.finalizeScenario(currentScenario.getBackingScenario(),
                        currentCampaignState.getContract(), campaign);
                StratconRulesManager.setScenarioParametersFromBiome(currentTrackState, currentScenario);
            }

            StratconRulesManager.commitPrimaryForces(campaign, currentScenario, currentTrackState);
            setCurrentScenario(currentScenario, currentTrackState, currentCampaignState);
            currentScenario.updateMinefieldCount(Minefield.TYPE_CONVENTIONAL, getNumMinefields());
            // if we've just committed reinforcements then simply close it down
        } else {
            currentScenario.updateMinefieldCount(Minefield.TYPE_CONVENTIONAL, getNumMinefields());
            setVisible(false);
        }

        translateTemplateObjectives(currentScenario.getBackingScenario(), campaign);

        this.getParent().repaint();
    }

    /**
     * Event handler for when the user makes a selection on the available force
     * selector.
     *
     * @param e The event fired.
     */
    private void availableForceSelectorChanged(ListSelectionEvent e, JLabel forceStatusLabel, boolean reinforcements) {
        if (!(e.getSource() instanceof JList<?>)) {
            return;
        }

        JList<Force> sourceList = (JList<Force>) e.getSource();

        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("<html>");

        for (Force force : sourceList.getSelectedValuesList()) {
            statusBuilder.append(buildForceStatus(force, reinforcements));
        }

        statusBuilder.append("</html>");

        forceStatusLabel.setText(statusBuilder.toString());

        pack();
    }

    /**
     * Event handler for when an available unit selector's selection changes.
     * Updates the "# units selected" label and the unit status label.
     * Also checks maximum selection size and disables commit button (TBD).
     *
     * @param event               The triggering event
     * @param selectionCountLabel Which label to update with how many items are
     *                            selected
     * @param unitStatusLabel     Which label to update with detailed unit info
     * @param maxSelectionSize    How many items can be selected at most
     * @param usesBV              Whether we are tracking the BV of selected items, {@code true},
     *                           or simply the count of selected items, {@code false}
     */
    private void availableUnitSelectorChanged(ListSelectionEvent event, JLabel selectionCountLabel,
                                              JLabel unitStatusLabel, int maxSelectionSize, boolean usesBV) {
        if (!(event.getSource() instanceof JList<?>)) {
            return;
        }

        JList<Unit> changedList = (JList<Unit>) event.getSource();

        int selectedItems;
        if (usesBV) {
            selectedItems = 0;
            for (Unit unit : changedList.getSelectedValuesList()) {
                selectedItems += unit.getEntity().calculateBattleValue(true, true);
                selectionCountLabel.setText(String.format("%d %s", selectedItems,
                    resources.getString("unitsSelectedLabel.bv")));
            }
        } else {
            selectedItems = changedList.getSelectedIndices().length;
            selectionCountLabel.setText(String.format("%d %s", selectedItems,
                resources.getString("unitsSelectedLabel.count")));
        }

        // if we've selected too many units here, change the label and disable the
        // commit button
        if (selectedItems > maxSelectionSize) {
            selectionCountLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
            btnCommit.setEnabled(false);
        } else {
            btnCommit.setEnabled(true);
        }

        // go through the other unit lists in the wizard and deselect the selected units
        // to avoid "issues" and "unpredictable behavior"
        for (JList<Unit> unitList : availableUnitLists.values()) {
            if (!changedList.equals(unitList)) {
                unselectDuplicateUnits(unitList, changedList.getSelectedValuesList());
            }
        }

        if (!changedList.equals(availableInfantryUnits)) {
            unselectDuplicateUnits(availableInfantryUnits, changedList.getSelectedValuesList());
        }

        if (!changedList.equals(availableLeadershipUnits)) {
            unselectDuplicateUnits(availableLeadershipUnits, changedList.getSelectedValuesList());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        for (Unit unit : changedList.getSelectedValuesList()) {
            sb.append(buildUnitStatus(unit));
        }

        sb.append("</html>");

        unitStatusLabel.setText(sb.toString());
        pack();
    }

    /**
     * Worker function that de-selects duplicate units.
     *
     * @param listToProcess
     * @param selectedUnits
     */
    private void unselectDuplicateUnits(JList<Unit> listToProcess, List<Unit> selectedUnits) {
        for (Unit selectedUnit : selectedUnits) {
            for (int potentialClearIndex : listToProcess.getSelectedIndices()) {
                Unit potentialClearTarget = listToProcess.getModel().getElementAt(potentialClearIndex);

                if (potentialClearTarget.getId().equals(selectedUnit.getId())) {
                    listToProcess.removeSelectionInterval(potentialClearIndex, potentialClearIndex);
                }
            }
        }
    }

    /**
     * Specific event handler for logic related to available infantry units.
     * Updates the defensive minefield count
     */
    private void availableInfantrySelectorChanged(JLabel defensiveMineCountLabel) {
        defensiveMineCountLabel.setText(String.format(resources.getString("lblDefensiveMinefieldCount.text"),
                getNumMinefields()));
    }

    /**
     * Worker function that calculates how many minefields should be available for
     * the current scenario.
     */
    private int getNumMinefields() {
        return Math.max(0,
                currentScenario.getNumDefensivePoints() - availableInfantryUnits.getSelectedIndices().length);
    }
}
