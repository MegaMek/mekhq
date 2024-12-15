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

import megamek.common.Minefield;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.ScenarioForceTemplate;
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

import static java.lang.Math.min;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.translateTemplateObjectives;
import static mekhq.campaign.personnel.SkillType.S_LEADER;
import static mekhq.campaign.stratcon.StratconRulesManager.BASE_LEADERSHIP_BUDGET;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.stratcon.StratconRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.stratcon.StratconRulesManager.getEligibleLeadershipUnits;
import static mekhq.campaign.stratcon.StratconRulesManager.processReinforcementDeployment;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * UI for managing force/unit assignments for individual StratCon scenarios.
 */
public class StratconScenarioWizard extends JDialog {
    private StratconScenario currentScenario;
    private final Campaign campaign;
    private StratconTrackState currentTrackState;
    private StratconCampaignState currentCampaignState;
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AtBStratCon",
            MekHQ.getMHQOptions().getLocale());

    private final Map<String, JList<Force>> availableForceLists = new HashMap<>();
    private final Map<String, JList<Unit>> availableUnitLists = new HashMap<>();

    private JList<Unit> availableInfantryUnits = new JList<>();
    private JList<Unit> availableLeadershipUnits = new JList<>();

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
        setTitle("Scenario Setup Wizard");
        getContentPane().removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        getContentPane().setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        setInstructions(gbc);

        if (Objects.requireNonNull(currentScenario.getCurrentState()) == ScenarioState.UNRESOLVED) {
            gbc.gridy++;
            setAssignForcesUI(gbc, false);
        } else {
            gbc.gridy++;
            setAssignForcesUI(gbc, true);
            gbc.gridy++;

            int leadershipSkill = currentScenario.getBackingScenario().getLanceCommanderSkill(
                S_LEADER, campaign);

            List<Unit> eligibleLeadershipUnits = getEligibleLeadershipUnits(campaign,
                currentScenario.getPrimaryForceIDs(), leadershipSkill);
            eligibleLeadershipUnits.sort(Comparator.comparing(this::getForceNameReversed));

            if (!eligibleLeadershipUnits.isEmpty() && (leadershipSkill > 0)) {
                setLeadershipUI(gbc, eligibleLeadershipUnits, leadershipSkill);
                gbc.gridy++;
            }

            if (currentScenario.getNumDefensivePoints() > 0) {
                setDefensiveUI(gbc);
                gbc.gridy++;
            }
        }

        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);

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
                || currentTrackState.getRevealedCoords().contains(currentScenario.getCoords()) ||
                (currentScenario.getDeploymentDate() != null)) {
            labelBuilder.append(currentScenario.getInfo(campaign));
        }

        if (Objects.requireNonNull(currentScenario.getCurrentState()) == ScenarioState.UNRESOLVED) {
            labelBuilder.append("primaryForceAssignmentInstructions.text");
        } else {
            labelBuilder.append("reinforcementsAndSupportInstructions.text");
        }

        labelBuilder.append("<br/>");
        lblInfo.setText(labelBuilder.toString());
        getContentPane().add(lblInfo, gbc);
    }

    /**
     * Worker function that sets up the "assign forces to scenario" UI elements.
     */
    private void setAssignForcesUI(GridBagConstraints gbc, boolean reinforcements) {
        // generate a lance selector with the following parameters:
        // all forces assigned to the current track that aren't already assigned
        // elsewhere
        // max number of items that can be selected = current scenario required lances

        List<ScenarioForceTemplate> eligibleForceTemplates = reinforcements
                ? currentScenario.getScenarioTemplate().getAllPlayerReinforcementForces()
                : currentScenario.getScenarioTemplate().getAllPrimaryPlayerForces();

        for (ScenarioForceTemplate forceTemplate : eligibleForceTemplates) {
            JPanel forcePanel = new JPanel();
            forcePanel.setLayout(new GridBagLayout());
            GridBagConstraints localGbc = new GridBagConstraints();
            localGbc.gridx = 0;
            localGbc.gridy = 0;

            String reinforcementMessage = currentCampaignState.getSupportPoints() > 0 ?
                resourceMap.getString("selectReinforcementsForTemplate.Text") :
                resourceMap.getString("selectReinforcementsForTemplateNoSupportPoints.Text");

            String labelText = reinforcements ? reinforcementMessage
                : resourceMap.getString("selectForceForTemplate.Text");

            JLabel assignForceListInstructions = new JLabel(labelText);
            forcePanel.add(assignForceListInstructions, localGbc);

            localGbc.gridy = 1;
            JLabel selectedForceInfo = new JLabel();
            JList<Force> availableForceList = addAvailableForceList(forcePanel, localGbc, forceTemplate);

            availableForceList
                    .addListSelectionListener(e -> availableForceSelectorChanged(e, selectedForceInfo, reinforcements));

            availableForceLists.put(forceTemplate.getForceName(), availableForceList);

            localGbc.gridx = 1;
            forcePanel.add(selectedForceInfo, localGbc);

            getContentPane().add(forcePanel, gbc);
            gbc.gridy++;
        }
    }

    /**
     * Set up the UI for "defensive elements", such as infantry, gun emplacements,
     * minefields, etc.
     */
    private void setDefensiveUI(GridBagConstraints gbc) {
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblDefensivePostureInstructions = new JLabel(
                resourceMap.getString("lblDefensivePostureInstructions.Text"));
        getContentPane().add(lblDefensivePostureInstructions, gbc);

        gbc.gridy++;

        List<Unit> eligibleInfantryUnits = StratconRulesManager.getEligibleDefensiveUnits(campaign);
        eligibleInfantryUnits.sort(Comparator.comparing(Unit::getName));

        availableInfantryUnits = addIndividualUnitSelector(eligibleInfantryUnits, gbc,
                currentScenario.getNumDefensivePoints(), false);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblDefensiveMinefieldCount = new JLabel(
                String.format(resourceMap.getString("lblDefensiveMinefieldCount.text"),
                        currentScenario.getNumDefensivePoints()));

        availableInfantryUnits
                .addListSelectionListener(e -> availableInfantrySelectorChanged(lblDefensiveMinefieldCount));

        getContentPane().add(lblDefensiveMinefieldCount, gbc);
    }

    private void setLeadershipUI(GridBagConstraints gbc, List<Unit> eligibleUnits, int leadershipSkill) {
        // Leadership budget is capped at 5 levels
        int leadershipBudget = min(BASE_LEADERSHIP_BUDGET * leadershipSkill, BASE_LEADERSHIP_BUDGET * 5);
        int maxSelectionSize = leadershipBudget - currentScenario.getLeadershipPointsUsed();

        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblLeadershipInstructions = new JLabel(String.format(resourceMap.getString("lblLeadershipInstructions.Text"),
            maxSelectionSize));
        getContentPane().add(lblLeadershipInstructions, gbc);

        gbc.gridy++;

        availableLeadershipUnits = addIndividualUnitSelector(eligibleUnits, gbc, maxSelectionSize, true);
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
     * Adds an individual unit selector, given a list of individual units, a global
     * grid bag constraint set
     * and a maximum selection size.
     *
     * @param units            The list of units to use as a data source.
     * @param gridBagConstraints              Gridbag constraints to indicate where the control will go
     * @param maxSelectionSize Maximum number of units that can be selected
     */
    private JList<Unit> addIndividualUnitSelector(List<Unit> units, GridBagConstraints gridBagConstraints,
                                                  int maxSelectionSize, boolean usesBV) {
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridBagLayout());
        GridBagConstraints localGridBagConstraints = new GridBagConstraints();

        localGridBagConstraints.gridx = 0;
        localGridBagConstraints.gridy = 0;
        localGridBagConstraints.anchor = GridBagConstraints.WEST;
        JLabel instructions = new JLabel();
        instructions.setText(String.format(resourceMap.getString("lblSelectIndividualUnits.text"),
                maxSelectionSize));
        unitPanel.add(instructions);

        localGridBagConstraints.gridy++;
        DefaultListModel<Unit> availableModel = new DefaultListModel<>();
        availableModel.addAll(units);

        JLabel unitStatusLabel = new JLabel();

        // add the # units selected control
        JLabel unitSelectionLabel = new JLabel();
        unitSelectionLabel.setText("0 selected");

        localGridBagConstraints.gridy++;
        unitPanel.add(unitSelectionLabel, localGridBagConstraints);

        JList<Unit> availableUnits = new JList<>();
        availableUnits.setModel(availableModel);
        availableUnits.setCellRenderer(new ScenarioWizardUnitRenderer());
        availableUnits.addListSelectionListener(
                e -> availableUnitSelectorChanged(e, unitSelectionLabel, unitStatusLabel, maxSelectionSize, usesBV));

        JScrollPane infantryContainer = new JScrollPaneWithSpeed();
        infantryContainer.setViewportView(availableUnits);
        localGridBagConstraints.gridy++;
        unitPanel.add(infantryContainer, localGridBagConstraints);

        // add the 'status display' control
        localGridBagConstraints.gridx++;
        localGridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        unitPanel.add(unitStatusLabel, localGridBagConstraints);

        getContentPane().add(unitPanel, gridBagConstraints);

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
                costBuilder.append(resourceMap.getString("regular.text"));
                break;
            case CHAINED_SCENARIO:
                costBuilder.append(resourceMap.getString("fromChainedScenario.text"));
                break;
            case FIGHT_LANCE:
                costBuilder.append(resourceMap.getString("lanceInFightRole.text"));
                break;
            default:
                costBuilder.append("Error: Invalid Reinforcement Type");
                break;
        }

        costBuilder.append(')');
        return costBuilder.toString();
    }

    /**
     * Sets the navigation buttons - commit, cancel, etc.
     */
    private void setNavigationButtons(GridBagConstraints gbc) {
        // you're on one of two screens:
        // the 'primary force selection' screen
        // the 'reinforcement selection' screen
        btnCommit = new JButton("Commit");
        btnCommit.setActionCommand("COMMIT_CLICK");
        btnCommit.addActionListener(this::btnCommitClicked);

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        getContentPane().add(btnCommit, gbc);
    }

    /**
     * Event handler for when the user clicks the 'commit' button.
     * Behaves differently depending on the state of the scenario
     */
    private void btnCommitClicked(ActionEvent e) {
        // go through all the force lists and add the selected forces to the scenario
        for (String templateID : availableForceLists.keySet()) {
            for (Force force : availableForceLists.get(templateID).getSelectedValuesList()) {
                // if we are assigning reinforcements, pay the price if appropriate
                if (currentScenario.getCurrentState() == ScenarioState.PRIMARY_FORCES_COMMITTED) {
                    if (currentCampaignState.getSupportPoints() <= 0) {
                        campaign.addReport(String.format(resourceMap.getString("reinforcementsNoSupportPoints.text"),
                            currentScenario.getName(),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG));
                        continue;
                    }

                    ReinforcementEligibilityType reinforcementType = StratconRulesManager.getReinforcementType(
                            force.getId(), currentTrackState,
                            campaign, currentCampaignState);

                    // if we failed to deploy as reinforcements, move on to the next force
                    ReinforcementResultsType reinforcementResults = processReinforcementDeployment(
                        force, reinforcementType, currentCampaignState, currentScenario, campaign);

                    if (reinforcementResults.ordinal() >= FAILED.ordinal()) {
                        currentScenario.addFailedReinforcements(force.getId());
                        continue;
                    }

                    currentScenario.addForce(force, templateID, campaign);

                    if (reinforcementResults == DELAYED) {
                        List<UUID> delayedReinforcements = currentScenario.getBackingScenario().getFriendlyDelayedReinforcements();

                        for (UUID unitId : force.getAllUnits(true)) {
                            try {
                                delayedReinforcements.add(unitId);
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                } else {
                    // In the event the player has selected multiple forces to act as the primary
                    // force, only commit the first force
                    currentScenario.addForce(force, templateID, campaign);
                    break;
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
                selectionCountLabel.setText(String.format("%d selected (ignores crew skill)", selectedItems));
            }
        } else {
            selectedItems = changedList.getSelectedIndices().length;
            selectionCountLabel.setText(String.format("%d selected", selectedItems));
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
        defensiveMineCountLabel.setText(String.format(resourceMap.getString("lblDefensiveMinefieldCount.text"),
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
