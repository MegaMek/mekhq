/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.stratCon;

import static mekhq.campaign.mission.AtBDynamicScenarioFactory.scaleObjectiveTimeLimits;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.translateTemplateObjectives;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.stratCon.StratConRulesManager.BASE_LEADERSHIP_BUDGET;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.stratCon.StratConRulesManager.ReinforcementResultsType.INSTANT;
import static mekhq.campaign.stratCon.StratConRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.stratCon.StratConRulesManager.getEligibleLeadershipUnits;
import static mekhq.campaign.stratCon.StratConRulesManager.getReinforcementType;
import static mekhq.campaign.stratCon.StratConRulesManager.processReinforcementDeployment;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.PRIMARY_FORCES_COMMITTED;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.REINFORCEMENTS_COMMITTED;
import static mekhq.campaign.utilities.CampaignTransportUtilities.getLeadershipDropdownVectorPair;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.Minefield;
import megamek.common.rolls.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConRulesManager;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.StratConPanel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.StratConReinforcementsConfirmationDialog;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Pair;

/**
 * UI for managing force/unit assignments for individual StratCon scenarios.
 */
public class StratConScenarioWizard extends JDialog {
    private StratConScenario currentScenario;
    private final Campaign campaign;
    private StratConTrackState currentTrackState;
    private StratConCampaignState currentCampaignState;
    private final String resourcePath = "mekhq.resources.AtBStratCon";
    private final transient ResourceBundle resources = ResourceBundle.getBundle(resourcePath,
          MekHQ.getMHQOptions().getLocale());

    private final Map<String, JList<Force>> availableForceLists = new HashMap<>();
    private final Map<String, JList<Unit>> availableUnitLists = new HashMap<>();

    private List<Unit> eligibleLeadershipUnits;
    private JList<Unit> availableInfantryUnits = new JList<>();
    private JList<Unit> availableLeadershipUnits = new JList<>();
    private CampaignTransportType selectedCampaignTransportType = null;

    private JComboBox<String> cboTransportType = new JComboBox<>();

    private boolean wasCanceled;

    private JPanel contentPanel;
    private JButton btnCommit;

    private final StratConPanel parent;

    public StratConScenarioWizard(Campaign campaign, StratConPanel parent) {
        this.campaign = campaign;
        this.parent = parent;
        this.setModalityType(ModalityType.APPLICATION_MODAL);
    }

    /**
     * Configures and sets the current StratCon scenario, updating the associated track and campaign states as well as
     * preparing the available forces and units for the scenario.
     *
     * @param scenario       the {@link StratConScenario} to be set as the current scenario.
     * @param trackState     the {@link StratConTrackState} representing the state of the scenario's track.
     * @param campaignState  the {@link StratConCampaignState} representing the state of the overall campaign.
     * @param isPrimaryForce a boolean flag indicating whether the primary force is being assigned for this scenario.
     *                                             <ul>
     *                                               <li>{@code true}: Indicates that the primary force is being deployed.</li>
     *                                               <li>{@code false}: Indicates that the scenario is being configured without primary force assignment.</li>
     *                                             </ul>
     *
     *                       <p>Functionality and Process:</p>
     *                       <ul>
     *                         <li>Sets the provided scenario as the {@code currentScenario}.</li>
     *                         <li>Updates the {@link StratConCampaignState}, {@link StratConTrackState}, and clears previous force/unit lists.</li>
     *                         <li>Initializes the user interface by calling {@link #setUI(boolean)}, passing the {@code isPrimaryForce} parameter.</li>
     *                       </ul>
     */
    public void setCurrentScenario(StratConScenario scenario, StratConTrackState trackState,
          StratConCampaignState campaignState, boolean isPrimaryForce) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        currentTrackState = trackState;
        availableForceLists.clear();
        availableUnitLists.clear();

        availableInfantryUnits.clearSelection();
        availableLeadershipUnits.clearSelection();

        setUI(isPrimaryForce);
    }

    public boolean isWasCanceled() {
        return wasCanceled;
    }

    public void setWasCanceled(boolean wasCancelled) {
        this.wasCanceled = wasCancelled;
    }

    /**
     * Configures and initializes the user interface for the scenario setup wizard. This method dynamically assembles
     * various UI components based on the scenario's state and whether the primary force is being assigned.
     *
     * @param isPrimaryForce a boolean flag indicating whether the primary force is being assigned:
     *                                             <ul>
     *                                               <li>{@code true}: Configures the UI with additional components for leadership and defensive points.</li>
     *                                               <li>{@code false}: Configures the UI for scenarios without primary force-specific components.</li>
     *                                             </ul>
     *
     *                       <p>Process and Behavior:</p>
     *                       <ol>
     *                         <li>Sets the dialog window's title based on resource strings.</li>
     *                         <li>Clears the existing content pane to rebuild the UI.</li>
     *                         <li>Uses a {@link JPanel} with {@link GridBagLayout} to organize UI components.</li>
     *                         <li>Adds components for instructions and force assignment.</li>
     *                         <li>If {@code isPrimaryForce} is {@code true}, adds special UI components such as:
     *                             <ul>
     *                               <li>Leadership unit selection, sorted by force name (if leadership skill is greater than 0).</li>
     *                               <li>Defensive points configuration.</li>
     *                             </ul>
     *                         </li>
     *                         <li>Adds navigation buttons for controlling the wizard flow (Next, Back, Cancel, etc.).</li>
     *                         <li>Wraps all content in a {@link JScrollPane} to handle large UI layouts with scrollbars.</li>
     *                         <li>Finalizes the UI setup with {@code pack()} and {@code validate()} for proper rendering.</li>
     *                       </ol>
     *
     *                       <p>Roles and Responsibilities:</p>
     *                       <ul>
     *                         <li>Handles complex UI layouts dynamically based on scenario state and user input requirements.</li>
     *                         <li>Ensures scalability for larger content using scrollbars.</li>
     *                       </ul>
     */
    private void setUI(boolean isPrimaryForce) {
        setTitle(resources.getString("scenarioSetupWizard.title"));
        getContentPane().removeAll();

        // Create a new panel to hold all components
        contentPanel = new JPanel(new CardLayout());
        contentPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        // Add instructions
        setInstructions(gbc);

        // Move to the next row
        gbc.gridy++;

        // Add UI for assigning forces
        setAssignForcesUI(gbc, isPrimaryForce);

        // Handle optional UI for eligible leadership, defensive points, etc.
        if (isPrimaryForce) {
            gbc.gridy++;
            int leadershipSkill = currentScenario.getBackingScenario().getLanceCommanderSkill(S_LEADER, campaign);
            eligibleLeadershipUnits = getEligibleLeadershipUnits(campaign, currentScenario, leadershipSkill);
            eligibleLeadershipUnits.sort(Comparator.comparing(this::getForceNameReversed));

            setLeadershipUI(gbc, eligibleLeadershipUnits, leadershipSkill);
            gbc.gridy++;

            if (currentScenario.getNumDefensivePoints() > 0) {
                setDefensiveUI(gbc);
                gbc.gridy++;
            }
        }

        // Add navigation buttons
        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc, isPrimaryForce);

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
     * Returns a concatenated string of a unit's force hierarchy, in reversed order, starting from the highest parent
     * Force going down to the given unit's direct Force.
     * <p>
     * If the unit does not belong to any Force, an empty string is returned.
     *
     * @param unit The Unit whose Force hierarchy names are to be returned.
     *
     * @return A concatenated string of Force names in reversed order separated by a slash, or an empty string if the
     *       unit is not assigned to any Force.
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
     * Worker function that sets up the instructions for the currently selected scenario.
     */
    private void setInstructions(GridBagConstraints gbc) {
        JLabel lblInfo = new JLabel();
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append("<html>");

        if (currentTrackState.isGmRevealed() ||
                  currentTrackState.getRevealedCoords().contains(currentScenario.getCoords()) ||
                  (currentScenario.getDeploymentDate() != null)) {
            labelBuilder.append(currentScenario.getInfo(campaign));
        }

        labelBuilder.append("<br/>");
        lblInfo.setText(labelBuilder.toString());

        contentPanel.add(lblInfo, gbc);
    }

    /**
     * Configures and populates the user interface for assigning forces to the scenario. This method dynamically creates
     * UI panels, one for each eligible force template, and displays the available forces for selection based on the
     * reinforcement or primary force status.
     *
     * @param gbc            the {@link GridBagConstraints} to manage the layout of the force assignment UI.
     * @param isPrimaryForce a boolean flag indicating whether the UI is being configured for the primary force
     *                       assignment:
     *                                             <ul>
     *                                               <li>{@code true}: Lists player forces designated as primary forces.</li>
     *                                               <li>{@code false}: Lists reinforcement forces that may require support points.</li>
     *                                             </ul>
     *
     *                       <p>Process and Behavior:</p>
     *                       <ol>
     *                         <li>Retrieves a list of eligible force templates based on the value of {@code isPrimaryForce}.
     *                             <ul>
     *                               <li>For primary forces, {@link ScenarioTemplate#getAllPrimaryPlayerForces()} is used.</li>
     *                               <li>For reinforcement forces, {@link ScenarioTemplate#getAllPlayerReinforcementForces()} is used.</li>
     *                             </ul>
     *                         </li>
     *                         <li>For each eligible force template:
     *                             <ul>
     *                               <li>Creates a panel to hold UI components specific to that force.</li>
     *                               <li>Adds instructional text for reinforcements if applicable, which adapts based on the available support points.</li>
     *                               <li>If not a primary force:
     *                                   <ul>
     *                                     <li>Constructs a force selection list populated with available reinforcements.</li>
     *                                     <li>Tracks the selection through listener events, enabling force selection updates dynamically.</li>
     *                                     <li>Displays detailed information about the selected force beside the force list.</li>
     *                                   </ul>
     *                               </li>
     *                               <li>Attaches the created force panel to the main content panel managed by the {@code GridBagConstraints}.</li>
     *                             </ul>
     *                         </li>
     *                         <li>Updates the {@code availableForceLists} map with force templates and their associated UI components for later reference.</li>
     *                         <li>Increments the layout constraints for the force panel to ensure proper stacking in the UI layout.</li>
     *                       </ol>
     *
     *                       <p>Roles and Responsibilities:</p>
     *                       <ul>
     *                         <li>Adds and configures force-related UI elements dynamically, adapting to the scenario's configuration.</li>
     *                         <li>Handles player force assignment and manages user input for reinforcement and primary force selection.</li>
     *                         <li>Links the force selection lists to the backing logic for dynamic interaction and validation.</li>
     *                       </ul>
     */
    private void setAssignForcesUI(GridBagConstraints gbc, boolean isPrimaryForce) {
        // Get eligible templates depending on reinforcement status
        List<ScenarioForceTemplate> eligibleForceTemplates = isPrimaryForce ?
                                                                   currentScenario.getScenarioTemplate()
                                                                         .getAllPrimaryPlayerForces() :
                                                                   currentScenario.getScenarioTemplate()
                                                                         .getAllPlayerReinforcementForces();

        for (ScenarioForceTemplate forceTemplate : eligibleForceTemplates) {
            // Create a panel for each force template
            JPanel forcePanel = new JPanel();
            forcePanel.setLayout(new GridBagLayout());
            GridBagConstraints localGbc = new GridBagConstraints();
            localGbc.gridx = 0;
            localGbc.gridy = 0;

            // Add instructions for assigning forces
            String reinforcementMessage = currentCampaignState.getSupportPoints() > 0 ?
                                                resources.getString("selectReinforcementsForTemplate.Text") :
                                                resources.getString(
                                                      "selectReinforcementsForTemplateNoSupportPoints.Text");

            JLabel assignForceListInstructions = new JLabel(reinforcementMessage);

            if (!isPrimaryForce) {
                forcePanel.add(assignForceListInstructions, localGbc);

                // Add a list to display available forces
                localGbc.gridy = 1;
                JLabel selectedForceInfo = new JLabel();
                JList<Force> availableForceList = addAvailableForceList(forcePanel, localGbc, forceTemplate);
                availableForceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                // Add a listener to handle changes to the selected force
                availableForceList.addListSelectionListener(e -> {
                    availableForceSelectorChanged(e, selectedForceInfo, false);
                    btnCommit.setEnabled(true);
                });

                // Store the list in the map for later reference
                availableForceLists.put(forceTemplate.getForceName(), availableForceList);

                // Add the selected force info to the panel
                localGbc.gridx = 1;
                forcePanel.add(selectedForceInfo, localGbc);
            }

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
        JLabel lblDefensivePostureInstructions = new JLabel(resources.getString("lblFrontlineInstructions.text"));
        contentPanel.add(lblDefensivePostureInstructions, gbc);

        gbc.gridy++;

        // Obtain eligible infantry units
        List<Unit> eligibleInfantryUnits = StratConRulesManager.getEligibleFrontlineUnits(campaign, currentScenario);
        eligibleInfantryUnits.sort(Comparator.comparing(Unit::getName));

        // Add a unit selector for infantry units
        availableInfantryUnits = addIndividualUnitSelector(eligibleInfantryUnits,
              gbc,
              currentScenario.getNumDefensivePoints(),
              false);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;

        // Label to display the minefield count
        JLabel lblDefensiveMinefieldCount = new JLabel(String.format(resources.getString(
              "lblDefensiveMinefieldCount.text"), currentScenario.getNumDefensivePoints()));

        // Add a listener to update the minefield count label when infantry units are selected
        availableInfantryUnits.addListSelectionListener(e -> availableInfantrySelectorChanged(lblDefensiveMinefieldCount));

        contentPanel.add(lblDefensiveMinefieldCount, gbc);
    }

    private void setLeadershipUI(GridBagConstraints gbc, List<Unit> eligibleUnits, int leadershipSkill) {
        // Leadership budget is capped at 5 levels
        int leadershipBudget = Math.min(BASE_LEADERSHIP_BUDGET * leadershipSkill, BASE_LEADERSHIP_BUDGET * 5);
        int maxSelectionSize = leadershipBudget - currentScenario.getLeadershipPointsUsed();

        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblLeadershipInstructions = new JLabel(String.format(resources.getString("lblLeadershipInstructions.Text"),
              maxSelectionSize));
        contentPanel.add(lblLeadershipInstructions, gbc);

        // Transport Type
        gbc.gridy++;
        JLabel lblTransportInstructions = new JLabel(MHQInternationalization.getTextAt(resourcePath,
              "lblLeadershipTransportInstructions.text"));
        contentPanel.add(lblTransportInstructions, gbc);

        gbc.gridy++;

        cboTransportType = new JComboBox<>(new Vector<>(getLeadershipDropdownVectorPair().stream()
                                                              .map(Pair::getKey)
                                                              .collect(Collectors.toSet())));
        cboTransportType.setSelectedItem(getLeadershipDropdownVectorPair().firstElement().getKey());

        contentPanel.add(cboTransportType, gbc);


        gbc.gridy++;
        CardLayout leadershipTransportCard = new CardLayout();
        JPanel leadershipUnitJPanel = new JPanel(leadershipTransportCard);

        availableLeadershipUnits = addIndividualUnitSelector(eligibleUnits, gbc, maxSelectionSize, true);

        ItemListener dropdownChangeListener = this::campaignTransportTypeChangeHandler;
        cboTransportType.addItemListener(dropdownChangeListener);
        contentPanel.add(leadershipUnitJPanel);
    }

    /**
     * Add an "available force list" to the given control
     */
    private JList<Force> addAvailableForceList(JPanel parent, GridBagConstraints gbc,
          ScenarioForceTemplate forceTemplate) {
        JScrollPane forceListContainer = new JScrollPaneWithSpeed();

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign,
              StratConRulesManager.getAvailableForceIDsForManualDeployment(forceTemplate.getAllowedUnitType(),
                    campaign,
                    currentTrackState,
                    (forceTemplate.getArrivalTurn() == ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS),
                    currentScenario,
                    currentCampaignState));

        JList<Force> availableForceList = new JList<>();
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));

        forceListContainer.setViewportView(availableForceList);

        parent.add(forceListContainer, gbc);
        return availableForceList;
    }

    /**
     * Adds an individual unit selector, given a list of individual units, a global grid bag constraint set, and a
     * maximum selection size.
     *
     * @param units              The list of units to use as a data source.
     * @param gridBagConstraints GridBagConstraints object to position the selector panel.
     * @param maxSelectionSize   Maximum number of units that can be selected.
     * @param usesBV             Whether to track the Battle Value (BV) of selected items or simply count.
     *
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
        JLabel instructions = new JLabel(String.format(resources.getString("lblSelectIndividualUnits.text"),
              maxSelectionSize));
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
        availableUnits.addListSelectionListener(e -> availableUnitSelectorChanged(e,
              unitSelectionLabel,
              unitStatusLabel,
              maxSelectionSize,
              usesBV));

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

    private void campaignTransportTypeChangeHandler(ItemEvent event) {
        if (!(event.getSource() instanceof JComboBox<?>) || (event.getStateChange() != ItemEvent.SELECTED)) {
            return;
        }

        for (Pair<String, CampaignTransportType> pair : getLeadershipDropdownVectorPair()) {
            if (pair.getKey().equals(cboTransportType.getSelectedItem())) {
                selectedCampaignTransportType = pair.getValue();
                break;
            }
        }
    }

    /**
     * Worker function that builds a "html-enabled" string indicating the brief status of a force
     */
    private String buildForceStatus(Force f, boolean hideForceCost) {
        StringBuilder sb = new StringBuilder();

        sb.append(f.getFullName());
        sb.append(": ");
        if (!hideForceCost) {
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
     * Worker function that builds a "html-enabled" string indicating the brief status of an individual unit
     */
    private String buildUnitStatus(Unit u) {
        StringBuilder sb = new StringBuilder();

        sb.append(u.getName());
        sb.append(": ");
        sb.append(u.getStatus());

        int injuryCount = (int) u.getCrew().stream().filter(p -> p.hasInjuries(true)).count();

        if (injuryCount > 0) {
            sb.append(String.format(", <span color='" +
                                          ReportingUtilities.getNegativeColor() +
                                          "'>%d/%d injured crew</span>", injuryCount, u.getCrew().size()));
        }

        sb.append("<br/>");
        return sb.toString();
    }

    /**
     * Worker function that builds an indicator of what it will take to deploy a particular force to the current
     * scenario as reinforcements.
     */
    private String buildForceCost(int forceID) {
        StringBuilder costBuilder = new StringBuilder();
        costBuilder.append('(');

        switch (getReinforcementType(forceID, currentTrackState, campaign, currentCampaignState)) {
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
     * Creates and configures the navigation buttons, specifically the "Commit" button, and adds it to the UI layout.
     * The behavior of the "Commit" button is determined based on whether the scenario involves primary force assignment
     * or reinforcements.
     *
     * @param constraints    the {@link GridBagConstraints} used to define the position and alignment of the button
     *                       within the panel.
     * @param isPrimaryForce a boolean flag indicating the purpose of the button:
     *                                             <ul>
     *                                               <li>{@code true}: The "Commit" button triggers a direct commit
     *                                                   for scenarios involving the primary force.</li>
     *                                               <li>{@code false}: The "Commit" button opens the reinforcement
     *                                                   confirmation dialog and is only enabled when sufficient
     *                                                   support points are available.</li>
     *                                             </ul>
     *
     *                       <p>Behavior and Functionality:</p>
     *                       <ul>
     *                         <li>When {@code isPrimaryForce} is {@code true}:
     *                             <ul>
     *                               <li>The "Commit" button invokes the
     *                               {@link #btnCommitClicked(Integer, boolean, boolean)}
     *                                   method to directly complete the action.</li>
     *                             </ul>
     *                         </li>
     *                         <li>When {@code isPrimaryForce} is {@code false}:
     *                             <ul>
     *                               <li>The button opens the {@link #reinforcementConfirmDialog()}, which handles
     *                                   reinforcement confirmation logic.</li>
     *                               <li>The button is only enabled if the current campaign has sufficient support points,
     *                                   as determined by {@link StratConCampaignState#getSupportPoints()}.</li>
     *                             </ul>
     *                         </li>
     *                         <li>The button is added to the content panel, with its position controlled by the provided
     *                             {@link GridBagConstraints}, ensuring proper alignment within the UI layout.</li>
     *                       </ul>
     */
    private void setNavigationButtons(GridBagConstraints constraints, boolean isPrimaryForce) {
        // Create the commit button
        btnCommit = new JButton(MHQInternationalization.getTextAt(resourcePath, "leadershipCommit.text"));
        btnCommit.setActionCommand("COMMIT_CLICK");
        if (isPrimaryForce) {
            btnCommit.addActionListener(evt -> btnCommitClicked(null, false, true));
        } else {
            btnCommit.addActionListener(evt -> reinforcementConfirmDialog());
        }

        JButton btnCancel = new JButton(MHQInternationalization.getTextAt(resourcePath, "leadershipCancel.text"));
        btnCancel.setActionCommand("CANCEL_CLICK");
        btnCancel.addActionListener(evt -> {
            wasCanceled = true;
            closeWizard();
        });
        btnCancel.setEnabled(true);

        // Configure layout constraints for the buttons
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.CENTER;

        //Final instructions:
        if (isPrimaryForce) {
            String instructions;
            Force primaryForce = currentScenario.getBackingScenario()
                                       .getForces(campaign)
                                       .getAllSubForces()
                                       .stream()
                                       .findFirst()
                                       .orElse(null);
            if (primaryForce != null) {
                instructions = MHQInternationalization.getFormattedTextAt(resourcePath,
                      "lblLeadershipCommitForces.text",
                      primaryForce.getName());
            } else {
                instructions = MHQInternationalization.getTextAt(resourcePath,
                      "lblLeadershipCommitForces.fallback.text");
            }

            contentPanel.add(new JLabel(instructions), constraints);
        }

        // Align and add cancel button to the content panel
        constraints.gridy++;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        contentPanel.add(btnCancel, constraints);
        constraints.anchor = GridBagConstraints.CENTER;

        // Add the commit button to the content panel
        contentPanel.add(btnCommit, constraints);
    }

    /**
     * Creates and displays the "Reinforcement Confirmation" dialog, allowing the user to review, adjust, and commit
     * reinforcements to a scenario. The dialog provides information about the current target roll modifiers, the
     * ability to adjust Support Points, and handles calculation and changes to the reinforcement target number
     * dynamically.
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
        // Hide the old dialog until we're done.
        // The dialog will be 'disposed' if the confirmation dialog is confirmed and re-shown if the dialog is canceled
        setVisible(false);
        final int SUPPORT_POINTS_MODIFIER = -2;

        Person commandLiaison = campaign.getSeniorAdminPerson(AdministratorSpecialization.COMMAND);
        TargetRoll targetNumber = calculateReinforcementTargetNumber(commandLiaison,
              currentCampaignState.getContract());
        // The -1 is due to the default cost for reinforcing
        int availableSupportPoints = currentCampaignState.getSupportPoints() - 1;

        AtBContract contract = currentScenario.getBackingContract(campaign);
        Faction enemy = contract.getEnemy();
        boolean isClanEnemy = enemy.isClan();
        boolean isBatchallAccepted = contract.isBatchallAccepted();

        boolean brokeBatchallTerms = false;
        if (isClanEnemy && isBatchallAccepted) {
            boolean backoutOfReinforcements = processBatchallWarningDialog();
            if (backoutOfReinforcements) {
                return;
            }

            brokeBatchallTerms = true;
        }

        StratConReinforcementsConfirmationDialog dialog = new StratConReinforcementsConfirmationDialog(campaign,
              targetNumber, availableSupportPoints);
        StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType responseType =
              dialog.getResponseType();
        switch (responseType) {
            case CANCEL -> setVisible(true);
            case REINFORCE -> {
                int supportPointsSpent = dialog.getSupportPoints();
                int supportPointModifier = supportPointsSpent * SUPPORT_POINTS_MODIFIER;
                int finalTargetNumber = targetNumber.getValue() + supportPointModifier;
                currentCampaignState.changeSupportPoints(-(supportPointsSpent + 1));
                btnCommitClicked(finalTargetNumber, false, false);
                if (brokeBatchallTerms) {
                    processBatchallBreach(contract, enemy.getShortName());
                }
            }
            case REINFORCE_INSTANTLY -> {
                int supportPointsSpent = dialog.getSupportPoints();
                int supportPointModifier = supportPointsSpent * SUPPORT_POINTS_MODIFIER;
                int finalTargetNumber = targetNumber.getValue() + supportPointModifier;
                currentCampaignState.changeSupportPoints(-(supportPointsSpent + 1) * 2);
                btnCommitClicked(finalTargetNumber, false, true);
                if (brokeBatchallTerms) {
                    processBatchallBreach(contract, enemy.getShortName());
                }
            }
            case REINFORCE_GM -> btnCommitClicked(0, true, false);
            case REINFORCE_GM_INSTANTLY -> btnCommitClicked(0, true, true);
        }
    }

    /**
     * Handles the event when the user clicks the 'commit' button. This method processes the selected forces,
     * reinforcements, and scenario states, committing primary forces, reinforcements, and units based on the current
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
     * @param reinforcementTargetNumber the number representing the reinforcement target threshold used when processing
     *                                  reinforcement deployment
     * @param isGMReinforcement         {@code true} if the player is using GM powers to bypass the reinforcement check,
     *                                  {@code false} otherwise.
     * @param isInstantlyDeployed       {@code true} if the player is deploying instantly
     */
    private void btnCommitClicked(@Nullable Integer reinforcementTargetNumber, boolean isGMReinforcement,
          boolean isInstantlyDeployed) {
        if (parent != null) {
            parent.setCommitForces(true);
        }

        // go through all the force lists and add the selected forces to the scenario
        for (String templateID : availableForceLists.keySet()) {
            for (Force force : availableForceLists.get(templateID).getSelectedValuesList()) {
                if (currentScenario.getCurrentState() == PRIMARY_FORCES_COMMITTED) {

                    ReinforcementEligibilityType reinforcementType = getReinforcementType(force.getId(),
                          currentTrackState,
                          campaign,
                          currentCampaignState);

                    ReinforcementResultsType reinforcementResults = processReinforcementDeployment(force,
                          reinforcementType,
                          currentCampaignState,
                          currentScenario,
                          campaign,
                          reinforcementTargetNumber,
                          isGMReinforcement,
                          isInstantlyDeployed);

                    if (reinforcementResults.ordinal() >= FAILED.ordinal()) {
                        currentScenario.addFailedReinforcements(force.getId());
                        continue;
                    }

                    currentScenario.addForce(force, templateID, campaign);

                    if (reinforcementResults == DELAYED) {
                        List<UUID> delayedReinforcements = currentScenario.getBackingScenario()
                                                                 .getFriendlyDelayedReinforcements();

                        for (UUID unitId : force.getAllUnits(true)) {
                            if (campaign.getUnit(unitId) != null) {
                                delayedReinforcements.add(unitId);
                            }
                        }
                    } else if (reinforcementResults == INSTANT) {
                        List<UUID> instantReinforcements = currentScenario.getBackingScenario()
                                                                 .getFriendlyInstantReinforcements();

                        for (UUID unitId : force.getAllUnits(true)) {
                            if (campaign.getUnit(unitId) != null) {
                                instantReinforcements.add(unitId);
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
            StratConRulesManager.processForceDeployment(currentScenario.getCoords(),
                  forceID,
                  campaign,
                  currentTrackState,
                  false);
        }

        currentScenario.updateMinefieldCount(Minefield.TYPE_CONVENTIONAL, getNumMinefields());

        if (currentScenario.getCurrentState().ordinal() < REINFORCEMENTS_COMMITTED.ordinal()) {
            translateTemplateObjectives(currentScenario.getBackingScenario(), campaign);
            scaleObjectiveTimeLimits(currentScenario.getBackingScenario(), campaign);
        }

        closeWizard();
    }

    /**
     * Displays a warning dialog to the user regarding a Batchall breach and captures their decision.
     *
     * <p>The dialog presents an in-character and out-of-character message and allows the user to either cancel or
     * continue. The dialog will repeat until the user confirms a decision. This method returns {@code true} if the user
     * chose to continue (did not back out of Batchall), or {@code false} if the user canceled.</p>
     *
     * @return {@code true} if the user chose to continue with Batchall, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private boolean processBatchallWarningDialog() {
        boolean dialogAccepted = false;
        boolean backedOutOfBatchall = false;

        Person speaker = campaign.getSeniorAdminPerson(AdministratorSpecialization.COMMAND);
        String inCharacterMessage = String.format(resources.getString("batchallBreach.ic"),
              campaign.getCommanderAddress());
        String outOfCharacterMessage = resources.getString("batchallBreach.ooc");
        String cancelButton = resources.getString("batchallBreach.button.cancel");
        String continueButton = resources.getString("batchallBreach.button.continue");

        while (!dialogAccepted) {
            ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign, speaker, null,
                  inCharacterMessage, List.of(cancelButton, continueButton), outOfCharacterMessage,
                  null, true);
            backedOutOfBatchall = dialog.getDialogChoice() == 1;

            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign);
            dialogAccepted = confirmation.wasConfirmed();
        }

        return !backedOutOfBatchall;
    }

    /**
     * Processes the consequences of a Batchall breach for the given contract.
     *
     * <p>This method marks the Batchall as not accepted in the contract and, if the campaign is configured to track
     * faction standing, adjusts regard accordingly and adds all relevant standing reports to the campaign log.</p>
     *
     * @param contract  the active {@link AtBContract} for which the Batchall was breached
     * @param enemyCode the code representing the enemy faction involved in the breach
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processBatchallBreach(AtBContract contract, String enemyCode) {
        contract.setBatchallAccepted(false);

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.isTrackFactionStanding()) {
            double regardMultiplier = campaignOptions.getRegardMultiplier();
            FactionStandings factionStandings = campaign.getFactionStandings();
            List<String> reports = factionStandings.processRefusedBatchall(campaign.getFaction().getShortName(),
                  enemyCode, campaign.getGameYear(), regardMultiplier);

            for (String report : reports) {
                campaign.addReport(report);
            }

        }
    }

    private void closeWizard() {
        this.getParent().repaint();

        dispose();
    }

    /**
     * Handles the event triggered when the user makes a selection in the available force selector UI component. This
     * method updates the provided status label to display detailed information about the selected forces and refreshes
     * the UI to reflect the changes.
     *
     * @param listSelectionEvent the {@link ListSelectionEvent} representing the user's selection action. This event
     *                           contains the source list and the selection details.
     * @param forceStatusLabel   the {@link JLabel} used to display the status of the selected forces.
     * @param isPrimaryForce     a boolean flag indicating whether the selected forces are part of the primary force:
     *                                                  <ul>
     *                                                    <li>{@code true}: Displays details relevant to the primary force (listSelectionEvent.g., leadership, roles).</li>
     *                                                    <li>{@code false}: Displays details relevant to reinforcement forces (listSelectionEvent.g., support point requirements).</li>
     *                                                  </ul>
     *
     *                           <p>Behavior and Process:</p>
     *                           <ul>
     *                             <li>Verifies that the event source is a {@link JList}. If the source is not a {@code JList}, the method returns immediately.</li>
     *                             <li>Retrieves the list of selected forces from the {@code JList}.</li>
     *                             <li>Builds an HTML-formatted string with status details for each selected force using the
     *                                 {@link #buildForceStatus(Force, boolean)} method.</li>
     *                             <li>Updates the provided status label with the constructed HTML string, effectively updating the displayed information.</li>
     *                             <li>Refreshes the UI by calling {@link #pack()} to ensure the dialog adjusts properly to any layout changes.</li>
     *                           </ul>
     *
     *                           <p>Roles and Responsibilities:</p>
     *                           <ul>
     *                             <li>Processes the user's selection of forces in the UI and dynamically updates the status display accordingly.</li>
     *                             <li>Ensures that the appropriate details (based on whether the forces are primary or reinforcements) are shown in the UI.</li>
     *                             <li>Maintains a responsive UI by packing the dialog after the update, adjusting its layout if necessary.</li>
     *                           </ul>
     */
    private void availableForceSelectorChanged(ListSelectionEvent listSelectionEvent, JLabel forceStatusLabel,
          boolean isPrimaryForce) {
        Object source = listSelectionEvent.getSource();
        Vector<Force> forceList = new Vector<>();

        if (source instanceof JList<?> objectList) {
            for (Object item : objectList.getSelectedValuesList()) {
                if (item instanceof Force force) {
                    forceList.add(force);
                }
            }
        }

        if (forceList.isEmpty()) {
            return;
        }

        JList<Force> sourceList = new JList<>(forceList);
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("<html>");

        for (Force force : sourceList.getSelectedValuesList()) {
            statusBuilder.append(buildForceStatus(force, isPrimaryForce));
        }

        statusBuilder.append("</html>");

        forceStatusLabel.setText(statusBuilder.toString());

        pack();
    }

    /**
     * Event handler for when an available unit selector's selection changes. Updates the "# units selected" label and
     * the unit status label. Also checks maximum selection size and disables commit button (TBD).
     *
     * @param event               The triggering event
     * @param selectionCountLabel Which label to update with how many items are selected
     * @param unitStatusLabel     Which label to update with detailed unit info
     * @param maxSelectionSize    How many items can be selected at most
     * @param usesBV              Whether we are tracking the BV of selected items, {@code true}, or simply the count of
     *                            selected items, {@code false}
     */
    private void availableUnitSelectorChanged(ListSelectionEvent event, JLabel selectionCountLabel,
          JLabel unitStatusLabel, int maxSelectionSize, boolean usesBV) {
        Object source = event.getSource();
        Vector<Unit> unitVector = new Vector<>();

        if (source instanceof JList<?> objectList) {
            for (Object item : objectList.getSelectedValuesList()) {
                if (item instanceof Unit unit) {
                    unitVector.add(unit);
                }
            }
        }

        if (unitVector.isEmpty()) {
            return;
        }

        JList<Unit> changedList = new JList<>(unitVector);

        ListSelectionListener[] listeners = (((JList<?>) event.getSource()).getListSelectionListeners());
        ((JList<?>) event.getSource()).removeListSelectionListener(listeners[0]);

        int selectedItems;
        if (usesBV) {
            selectedItems = 0;
            for (Unit unit : changedList.getSelectedValuesList()) {
                selectedItems += unit.getEntity().calculateBattleValue(true, true);
                selectionCountLabel.setText(String.format("%d %s",
                      selectedItems,
                      resources.getString("unitsSelectedLabel.bv")));
                selectTransportedUnitsAndTransport(selectedCampaignTransportType, unit, changedList);

            }
        } else {
            selectedItems = changedList.getSelectedIndices().length;
            selectionCountLabel.setText(String.format("%d %s",
                  selectedItems,
                  resources.getString("unitsSelectedLabel.count")));
        }

        // if we've selected too many units here, change the label and disable the
        // commit button
        if (selectedItems > maxSelectionSize) {
            selectionCountLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
            btnCommit.setEnabled(false);
        } else {
            selectionCountLabel.setForeground(null);
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

        ((JList<?>) event.getSource()).addListSelectionListener(listeners[0]);
    }

    private void selectTransportedUnitsAndTransport(CampaignTransportType campaignTransportType, Unit unit,
          JList<Unit> changedList) {
        if (campaignTransportType != null) {
            if (unit.hasTransportedUnits(campaignTransportType)) {
                Set<Unit> potentialTransportedUnits = unit.getTransportedUnits(campaignTransportType);
                for (Unit transportedUnit : potentialTransportedUnits) {
                    // if this unit isn't selected but is an eligible leadership unit
                    if (!changedList.getSelectedValuesList().contains(transportedUnit) &&
                              (eligibleLeadershipUnits.contains(transportedUnit))) {

                        int index = eligibleLeadershipUnits.indexOf(transportedUnit);
                        changedList.setSelectedIndices(ArrayUtils.add(changedList.getSelectedIndices(), index));
                    }
                }
            }

            if (unit.hasTransportAssignment(campaignTransportType)) {
                Unit transport = unit.getTransportAssignment(campaignTransportType).getTransport();
                // if this unit isn't selected but is an eligible leadership unit
                if (!changedList.getSelectedValuesList().contains(transport) &&
                          (eligibleLeadershipUnits.contains(transport))) {

                    int index = eligibleLeadershipUnits.indexOf(transport);
                    changedList.setSelectedIndices(ArrayUtils.add(changedList.getSelectedIndices(), index));
                }
            }
        }
    }

    /**
     * Worker function that de-selects duplicate units.
     *
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
     * Specific event handler for logic related to available infantry units. Updates the defensive minefield count
     */
    private void availableInfantrySelectorChanged(JLabel defensiveMineCountLabel) {
        defensiveMineCountLabel.setText(String.format(resources.getString("lblDefensiveMinefieldCount.text"),
              getNumMinefields()));
    }

    /**
     * Worker function that calculates how many minefields should be available for the current scenario.
     */
    private int getNumMinefields() {
        return Math.max(0,
              currentScenario.getNumDefensivePoints() - availableInfantryUnits.getSelectedIndices().length);
    }
}
