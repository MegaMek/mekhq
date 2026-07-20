/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

import jakarta.annotation.Nonnull;
import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import megamek.common.ui.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.StratConDeploymentEvent;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.events.missions.MissionRemovedEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.stratCon.CampaignManagementDialog;
import mekhq.gui.view.ContractMeterBar;
import mekhq.utilities.ReportingUtilities;

/**
 * This class contains code relevant to rendering the StratCon ("AtB Campaign State") tab.
 *
 * @author NickAragua
 */
public class StratConTab extends CampaignGuiTab {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private static final String OBJECTIVE_FAILED = "x";
    private static final String OBJECTIVE_COMPLETED = "&#10003;";
    private static final String OBJECTIVE_IN_PROGRESS = "o";

    private StratConPanel stratconPanel;
    private JScrollPane mapScrollPane;
    private JComboBox<ContractItem> contractSelector;
    private JTabbedPane sectorTabs;
    private List<StratConTrackState> currentSectorTracks;
    private JPanel infoPanel;
    private JLabel infoPanelText;
    private JLabel campaignStatusText;
    private JLabel objectiveStatusText;
    private JPanel victoryPointsPanel;
    private JScrollPane expandedObjectivePanel;
    private boolean objectivesCollapsed = false;

    private AtBContract currentContract;
    private StratConTrackState currentSectorTrack;

    private boolean adjustingSelectors = false;

    CampaignManagementDialog cmd;

    //region Constructors

    /**
     * Creates an instance of the StratConTab.
     */
    public StratConTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        setEnabled(!gui.getCampaign().getCampaignOptions().isUseStratConMaplessMode());
    }
    //endregion Constructors

    public StratConPanel getStratconPanel() {
        return stratconPanel;
    }

    /**
     * Override of the base initTab method. Populates the tab.
     */
    @Override
    public void initTab() {
        removeAll();

        currentSectorTracks = new ArrayList<>();

        infoPanelText = new JLabel();
        infoPanelText.setHorizontalAlignment(SwingConstants.LEFT);
        infoPanelText.setVerticalAlignment(SwingConstants.TOP);

        campaignStatusText = new JLabel();
        campaignStatusText.setHorizontalAlignment(SwingConstants.LEFT);
        campaignStatusText.setVerticalAlignment(SwingConstants.TOP);

        objectiveStatusText = new JLabel();
        objectiveStatusText.setHorizontalAlignment(SwingConstants.LEFT);
        objectiveStatusText.setVerticalAlignment(SwingConstants.TOP);
        objectiveStatusText.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if ((currentContract == null) || (currentSectorTrack == null)) {
                    return;
                }
                objectivesCollapsed = !objectivesCollapsed;
                objectiveStatusText.setText(getStrategicObjectiveText(currentContract.getStratConCampaignState(),
                      currentSectorTrack));
            }
        });

        setLayout(new BorderLayout());

        stratconPanel = new StratConPanel(getCampaignGui(), infoPanelText);
        mapScrollPane = new JScrollPane(stratconPanel);
        mapScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        mapScrollPane.getHorizontalScrollBar().setUnitIncrement(StratConPanel.HEX_X_RADIUS);
        mapScrollPane.getVerticalScrollBar().setUnitIncrement(StratConPanel.HEX_Y_RADIUS);
        // Repaint the whole map on scroll rather than blit-copying old pixels; the default BLIT mode tears when panning
        // the large, scaled hex map.
        mapScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        contractSelector = new JComboBox<>();
        contractSelector.addActionListener(evt -> contractSelectionHandler());

        JPanel contractPanel = new JPanel(new BorderLayout(UIUtil.scaleForGUI(5), 0));
        contractPanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "stratConTab.contractSelector.label")),
              BorderLayout.WEST);
        contractPanel.add(contractSelector, BorderLayout.CENTER);

        sectorTabs = new JTabbedPane();
        sectorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        sectorTabs.addChangeListener(evt -> sectorSelectionHandler());

        // TODO: lance role assignment UI here?

        JPanel pnlTutorial = new TutorialHyperlinkPanel("stratConTab.keyText");

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(contractPanel, BorderLayout.NORTH);
        centerPanel.add(sectorTabs, BorderLayout.CENTER);
        centerPanel.add(pnlTutorial, BorderLayout.SOUTH);

        this.add(centerPanel, BorderLayout.CENTER);

        initializeInfoPanel();
        cmd = new CampaignManagementDialog(this);

        JScrollPane infoScrollPane = new FastJScrollPane(infoPanel);
        infoScrollPane.setBorder(null);
        infoScrollPane.setMaximumSize(new Dimension(UIUtil.scaleForGUI(UIUtil.scaleForGUI(600),
              infoScrollPane.getHeight())));
        this.add(infoScrollPane, BorderLayout.EAST);

        repopulateContractsAndSectors();
    }

    /**
     * Worker function that sets up the layout of the right-side info panel.
     */
    private void initializeInfoPanel() {
        int gridY = 0;
        infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // Default settings for left-aligned components
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;

        // Add campaign status text
        constraints.gridy = gridY++;
        infoPanel.add(campaignStatusText, constraints);

        // Add "Manage Campaign State" button
        RoundedJButton btnManageCampaignState = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "stratConTab.manageCampaignState.text"));
        btnManageCampaignState.addActionListener(this::showCampaignStateManagement);
        constraints.gridy = gridY++;
        infoPanel.add(btnManageCampaignState, constraints);

        // Add the victory-point progress bar directly above the objectives list
        victoryPointsPanel = new JPanel(new BorderLayout());
        victoryPointsPanel.setOpaque(false);
        constraints.gridy = gridY++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        infoPanel.add(victoryPointsPanel, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        // Add an expanded objective panel (scrollable)
        expandedObjectivePanel = new FastJScrollPane(objectiveStatusText);
        expandedObjectivePanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        expandedObjectivePanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        expandedObjectivePanel.setPreferredSize(new Dimension(UIUtil.scaleForGUI(550, 300)));
        constraints.gridy = gridY++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        infoPanel.add(expandedObjectivePanel, constraints);

        // Reset horizontal fill for subsequent components
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        // Add additional info panel text or components
        constraints.gridx = 0;
        constraints.gridy = gridY++;
        constraints.gridheight = 3;
        infoPanel.add(infoPanelText, constraints);

        // Add a spacer to push all components upward (top alignment)
        constraints.gridx = 0;
        constraints.gridy = gridY;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        infoPanel.add(new JPanel(), constraints); // Invisible filler component
    }

    /**
     * Handles selection of a contract from the dropdown: rebuilds the sector tabs for that contract's tracks.
     */
    private void contractSelectionHandler() {
        if (adjustingSelectors) {
            return;
        }

        ContractItem selected = (ContractItem) contractSelector.getSelectedItem();
        if (selected == null) {
            return;
        }

        currentContract = selected.contract;
        repopulateSectorTabs(currentContract);
    }

    /**
     * Handles selection of a sector tab: moves the shared AO map into the active tab and points the map and the
     * objective display at that sector.
     */
    private void sectorSelectionHandler() {
        if (adjustingSelectors) {
            return;
        }

        int index = sectorTabs.getSelectedIndex();
        if ((index < 0) || (index >= currentSectorTracks.size()) || (currentContract == null)) {
            return;
        }

        currentSectorTrack = currentSectorTracks.get(index);

        Component tabContent = sectorTabs.getComponentAt(index);
        if (tabContent instanceof JPanel holder) {
            holder.add(mapScrollPane, BorderLayout.CENTER);
            holder.revalidate();
            holder.repaint();
        }

        stratconPanel.selectTrack(currentContract.getStratConCampaignState(), currentSectorTrack);
        stratconPanel.setVisible(true);
        updateCampaignState();
    }

    @Override
    public void repaint() {
        updateCampaignState();
        super.repaint();
    }

    @Override
    public void refreshAll() {
        stratconPanel.repaint();
        updateCampaignState();
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.STRAT_CON;
    }

    /**
     * Worker function that updates the campaign state section of the info panel with such info as current objective
     * status, VP/SP totals, etc.
     */
    public void updateCampaignState() {
        if ((contractSelector == null) || (campaignStatusText == null)) {
            return;
        }

        // campaign state text should contain:
        // list of remaining objectives, percentage remaining
        // current VP
        // current support points
        if (currentContract == null) {
            campaignStatusText.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.status.noContract"));
            expandedObjectivePanel.setVisible(false);
            victoryPointsPanel.setVisible(false);
            return;
        }

        LocalDate currentDate = getCampaignGui().getCampaign().getLocalDate();

        LocalDate startDate = currentContract.getStartDate();
        if (startDate != null && startDate.isAfter(currentDate)) {
            campaignStatusText.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.status.notStarted"));
            expandedObjectivePanel.setVisible(false);
            victoryPointsPanel.setVisible(false);
            return;
        }

        StratConCampaignState campaignState = currentContract.getStratConCampaignState();
        expandedObjectivePanel.setVisible(true);
        updateVictoryPointsBar(campaignState);

        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.status.header",
              currentContract.getContractType(), currentContract.getName(), campaignState.getBriefingText()));

        LocalDate endingDate = currentContract.getEndingDate();
        if (endingDate != null && endingDate.isBefore(currentDate)) {
            sb.append(getTextAt(RESOURCE_BUNDLE, "stratConTab.status.expired"));
        }

        // Campaign Victory Points are charted by the progress bar above the objectives list, so they are no longer
        // repeated as text here; only the support-point total remains in the status header.
        sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.status.supportPoints",
              String.valueOf(campaignState.getSupportPoints())));

        if (currentSectorTrack != null) {
            sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.status.deploymentPeriod",
                  String.valueOf(currentSectorTrack.getDeploymentTime())));
        }
        sb.append("</html>");

        campaignStatusText.setText(sb.toString());

        if (currentSectorTrack != null) {
            objectiveStatusText.setText(getStrategicObjectiveText(campaignState, currentSectorTrack));
        } else {
            objectiveStatusText.setText("");
        }

        // keep the sector tab colors in sync as objectives are completed/failed over the course of the contract
        applySectorTabColors();
    }

    /**
     * Refreshes the victory-point progress bar shown above the objectives list. Mirrors the Briefing Room's contract
     * gauge: a {@link ContractMeterBar} charting current-versus-required Campaign Victory Points when a positive target
     * exists, falling back to a plain current/required label when it does not.
     *
     * @param campaignState the StratCon state of the currently selected contract
     */
    private void updateVictoryPointsBar(StratConCampaignState campaignState) {
        victoryPointsPanel.removeAll();
        victoryPointsPanel.setVisible(true);

        boolean maplessMode = getCampaignGui().getCampaign().getCampaignOptions().isUseStratConMaplessMode();
        int currentScore = currentContract.getContractScore(maplessMode);
        int requiredScore = currentContract.getRequiredVictoryPoints();

        if (requiredScore > 0) {
            boolean canEndEarly = (campaignState == null) || campaignState.allowEarlyVictory();
            victoryPointsPanel.add(ContractMeterBar.victoryPoints(currentScore, requiredScore, canEndEarly),
                  BorderLayout.CENTER);
        } else {
            // No positive target to chart (e.g., a contract with a zero requirement); show the figures as text instead,
            // matching the Briefing Room's fallback.
            victoryPointsPanel.add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "stratConTab.status.victoryPointsBarFallback", currentScore, requiredScore)), BorderLayout.CENTER);
        }

        victoryPointsPanel.revalidate();
        victoryPointsPanel.repaint();
    }

    /**
     * Builds strategic objective text, appropriately appending details if the objectives are not "collapsed".
     */
    private String getStrategicObjectiveText(StratConCampaignState campaignState, StratConTrackState track) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
              .append(buildShortStrategicObjectiveText(track));

        if (objectivesCollapsed) {
            sb.append(" [+] ");
        } else {
            sb.append(" [-]<br/>")
                  .append(buildStrategicObjectiveText(campaignState, track));
        }

        sb.append("</html>");

        return sb.toString();
    }

    /**
     * Builds strategic objective one-liner summary for a single sector (track).
     */
    private String buildShortStrategicObjectiveText(StratConTrackState track) {
        int completedObjectives = 0, desiredObjectives = 0;

        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            desiredObjectives++;

            if (objective.isObjectiveCompleted(track)) {
                completedObjectives++;
            }
        }

        String summary = getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.summary",
              String.valueOf(completedObjectives), String.valueOf(desiredObjectives));

        // Colour the summary to match this sector's tab and detailed objective list: green only when every
        // objective is complete, red only when every objective has failed, amber while any remain in progress, and
        // no emphasis colour when the sector has no objectives at all (a 0/0 sector is not "complete").
        String color = switch (sectorObjectiveState(track)) {
            case NONE -> null;
            case ALL_COMPLETE -> ReportingUtilities.getPositiveColor();
            case ALL_FAILED -> ReportingUtilities.getNegativeColor();
            case IN_PROGRESS -> ReportingUtilities.getWarningColor();
        };

        if (color == null) {
            return summary;
        }
        return "<span color='" + color + "'>" + summary + "</span>";
    }

    /**
     * Fetches a localized objective phrase, capitalizing its first letter when the objective's location is revealed (it
     * starts the sentence) or lower-casing it when the phrase follows the "Locate and" prefix.
     *
     * @param key         the resource key of the phrase, stored capitalized
     * @param capitalized whether the phrase should keep its leading capital letter
     *
     * @return the phrase with its first letter cased appropriately
     */
    private String objectivePhrase(String key, boolean capitalized) {
        String phrase = getTextAt(RESOURCE_BUNDLE, key);
        if (capitalized || phrase.isEmpty()) {
            return phrase;
        }
        return Character.toLowerCase(phrase.charAt(0)) + phrase.substring(1);
    }

    /**
     * Builds the detailed strategic objective list for a single sector (track). Only objectives belonging to that
     * sector are shown; the contract-wide Turning Point / Victory Point reminder is intentionally not included here.
     */
    private String buildStrategicObjectiveText(StratConCampaignState campaignState, StratConTrackState track) {
        StringBuilder sb = new StringBuilder();

        // for each objective in this sector, grab the coordinates
        // if !revealed, "locate and"
        // if specific scenario "engage hostile forces"
        // if hostile facility "capture or destroy [facility name]"
        // if allied facility "maintain control of [facility name]"
        // if revealed, " on track [current track] at coordinates [coords]
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            boolean coordsRevealed = track.getRevealedCoords().contains(objective.getObjectiveCoords());
            boolean displayCoordinateData = objective.getObjectiveCoords() != null;
            boolean objectiveCompleted = objective.isObjectiveCompleted(track);
            boolean objectiveFailed = objective.isObjectiveFailed(track);

            // special case: allied facilities can get lost at any point in time
            if ((objective.getObjectiveType() == StrategicObjectiveType.AlliedFacilityControl) &&
                      !campaignState.allowEarlyVictory()) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getWarningColor())
                      .append("'>")
                      .append(OBJECTIVE_IN_PROGRESS);
            } else if (objectiveCompleted) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getPositiveColor())
                      .append("'>")
                      .append(OBJECTIVE_COMPLETED);
            } else if (objectiveFailed) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getNegativeColor())
                      .append("'>")
                      .append(OBJECTIVE_FAILED);
            } else {
                sb.append("<span color='")
                      .append(ReportingUtilities.getWarningColor())
                      .append("'>")
                      .append(OBJECTIVE_IN_PROGRESS);
            }

            sb.append(' ');

            if (!coordsRevealed && displayCoordinateData) {
                sb.append(getTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.locatePrefix")).append(' ');
            }

            switch (objective.getObjectiveType()) {
                case SpecificScenarioVictory:
                    sb.append(objectivePhrase("stratConTab.objectives.specificScenario", coordsRevealed));
                    break;
                case HostileFacilityControl:
                    sb.append(objectivePhrase("stratConTab.objectives.hostileFacility", coordsRevealed));
                    break;
                case AlliedFacilityControl:
                    sb.append(objectivePhrase("stratConTab.objectives.alliedFacility", coordsRevealed));

                    if (!campaignState.allowEarlyVictory()) {
                        sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.until",
                              campaignState.getContract().getEndingDate()));
                    }
                    break;
                case AnyScenarioVictory:
                    sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.anyScenario",
                          String.valueOf(objective.getCurrentObjectiveCount()),
                          String.valueOf(objective.getDesiredObjectiveCount()),
                          track.getDisplayableName()));
                    break;
                default:
                    break;
            }
            if (coordsRevealed && displayCoordinateData) {
                sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.location",
                      objective.getObjectiveCoords().toBTString(), track.getDisplayableName()));
            }

            sb.append("</span><br/>");
        }

        return sb.toString();
    }

    /**
     * Refreshes the contract dropdown (one entry per active contract in the current system) and the sector tabs for the
     * selected contract, preserving the current selection where possible.
     */
    private void repopulateContractsAndSectors() {
        ContractItem previouslySelected = (ContractItem) contractSelector.getSelectedItem();

        adjustingSelectors = true;
        contractSelector.removeAllItems();

        Campaign campaign = getCampaignGui().getCampaign();
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        for (AtBContract contract : campaign.getActiveAtBContracts(false)) {
            if (!currentSystem.equals(contract.getSystem())) {
                continue;
            }
            if (contract.getStratConCampaignState() == null) {
                continue;
            }
            contractSelector.addItem(new ContractItem(contract));
        }

        // restore the previously selected contract if it is still present
        if (previouslySelected != null) {
            for (int i = 0; i < contractSelector.getItemCount(); i++) {
                if (contractSelector.getItemAt(i).equals(previouslySelected)) {
                    contractSelector.setSelectedIndex(i);
                    break;
                }
            }
        }
        adjustingSelectors = false;

        ContractItem selected = (ContractItem) contractSelector.getSelectedItem();
        if (selected != null) {
            currentContract = selected.contract;
            repopulateSectorTabs(currentContract);
        } else {
            currentContract = null;
            currentSectorTrack = null;
            currentSectorTracks.clear();

            adjustingSelectors = true;
            sectorTabs.removeAll();
            adjustingSelectors = false;

            infoPanelText.setText("");
            stratconPanel.setVisible(false);
            updateCampaignState();
        }
    }

    /**
     * Rebuilds the sector tabs for the given contract, one tab per track. Preserves the currently displayed sector when
     * it still exists, otherwise selects the first sector.
     */
    private void repopulateSectorTabs(AtBContract contract) {
        StratConTrackState previousTrack = currentSectorTrack;

        adjustingSelectors = true;
        sectorTabs.removeAll();
        currentSectorTracks.clear();

        int selectIndex = 0;
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState != null) {
            int index = 0;
            for (StratConTrackState track : campaignState.getTracks()) {
                currentSectorTracks.add(track);
                // each tab holds an empty panel; the shared map is dropped into the active one on selection
                sectorTabs.addTab(track.getDisplayableName(), new JPanel(new BorderLayout()));

                if (track.equals(previousTrack)) {
                    selectIndex = index;
                }
                index++;
            }

            if (!currentSectorTracks.isEmpty()) {
                sectorTabs.setSelectedIndex(selectIndex);
            }
        }
        adjustingSelectors = false;

        if (!currentSectorTracks.isEmpty()) {
            sectorSelectionHandler();
        } else {
            currentSectorTrack = null;
            stratconPanel.setVisible(false);
            updateCampaignState();
        }

        applySectorTabColors();
    }

    /**
     * Colors each sector tab's title by that sector's objective status: positive when every objective is complete,
     * negative when an objective has failed, and warning while objectives are still in progress. Sectors with no
     * objectives keep the default tab color.
     */
    private void applySectorTabColors() {
        if (sectorTabs.getTabCount() != currentSectorTracks.size()) {
            return;
        }

        for (int i = 0; i < currentSectorTracks.size(); i++) {
            // a null foreground resets the tab to the tabbed pane's default color
            sectorTabs.setForegroundAt(i, sectorTabColor(currentSectorTracks.get(i)));
        }
    }

    /**
     * The aggregate objective status of a single sector, shared by the sector tab colour and the objective summary
     * one-liner so the two can never disagree.
     */
    private enum SectorObjectiveState {NONE, ALL_COMPLETE, ALL_FAILED, IN_PROGRESS}

    /**
     * @return the aggregate objective state for the given sector: {@link SectorObjectiveState#NONE} when it has no
     *       objectives, {@code ALL_COMPLETE}/{@code ALL_FAILED} only when <em>every</em> objective is complete/failed,
     *       and {@code IN_PROGRESS} whenever any objectives remain outstanding.
     */
    private SectorObjectiveState sectorObjectiveState(StratConTrackState track) {
        boolean hasObjectives = false;
        boolean allCompleted = true;
        boolean allFailed = true;

        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            hasObjectives = true;

            if (!objective.isObjectiveCompleted(track)) {
                allCompleted = false;
            }
            if (!objective.isObjectiveFailed(track)) {
                allFailed = false;
            }
        }

        if (!hasObjectives) {
            return SectorObjectiveState.NONE;
        }
        if (allCompleted) {
            return SectorObjectiveState.ALL_COMPLETE;
        }
        if (allFailed) {
            return SectorObjectiveState.ALL_FAILED;
        }
        return SectorObjectiveState.IN_PROGRESS;
    }

    /**
     * @return the tab color for the given sector, or {@code null} to use the default color when the sector has no
     *       objectives.
     */
    private Color sectorTabColor(StratConTrackState track) {
        return switch (sectorObjectiveState(track)) {
            case NONE -> null;
            case ALL_COMPLETE -> MekHQ.getMHQOptions().getFontColorPositive();
            case ALL_FAILED -> MekHQ.getMHQOptions().getFontColorNegative();
            case IN_PROGRESS -> MekHQ.getMHQOptions().getFontColorWarning();
        };
    }

    private void showCampaignStateManagement(ActionEvent e) {
        if ((currentContract == null) || (currentSectorTrack == null)) {
            return;
        }
        cmd.display(getCampaign(), currentContract.getStratConCampaignState(),
              currentSectorTrack, getCampaign().isGM());
        cmd.setModalityType(ModalityType.APPLICATION_MODAL);
        cmd.setVisible(true);
    }

    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionRemovedEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionCompletedEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionChangedEvent ev) {
        // Fired (among other times) once a newly accepted contract's StratCon state has been initialized, so a
        // concurrent contract shows up in the selector immediately instead of only after the next day.
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(StratConDeploymentEvent ev) {
        updateCampaignState();
    }

    /**
     * Data structure backing an entry in the contract selection dropdown.
     */
    private record ContractItem(AtBContract contract) {
        @Override
        @Nonnull
        public String toString() {
            return contract.getName();
        }
    }
}
