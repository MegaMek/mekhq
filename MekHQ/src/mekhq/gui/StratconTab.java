/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package mekhq.gui;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.MissionCompletedEvent;
import mekhq.campaign.event.MissionRemovedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.StratconDeploymentEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;
import mekhq.campaign.stratcon.StratconStrategicObjective;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.stratcon.CampaignManagementDialog;

import javax.swing.*;
import java.awt.Dialog.ModalityType;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.Objects;

/**
 * This class contains code relevant to rendering the StratCon ("AtB Campaign State") tab.
 * @author NickAragua
 */
public class StratconTab extends CampaignGuiTab {
    private static final String OBJECTIVE_FAILED = "x";
    private static final String OBJECTIVE_COMPLETED = "&#10003;";
    private static final String OBJECTIVE_IN_PROGRESS = "o";

    private StratconPanel stratconPanel;
    private JPanel infoPanel;
    private JComboBox<TrackDropdownItem> cboCurrentTrack;
    private JLabel infoPanelText;
    private JLabel campaignStatusText;
    private JLabel objectiveStatusText;
    private JScrollPane expandedObjectivePanel;
    private boolean objectivesCollapsed = true;

    CampaignManagementDialog cmd;

    //region Constructors
    /**
     * Creates an instance of the StratconTab.
     */
    public StratconTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }
    //endregion Constructors

    /**
     * Override of the base initTab method. Populates the tab.
     */
    @Override
    public void initTab() {
        removeAll();

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
                TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
                StratconCampaignState campaignState = currentTDI.contract.getStratconCampaignState();
                objectivesCollapsed = !objectivesCollapsed;
                objectiveStatusText.setText(getStrategicObjectiveText(campaignState));
              }
            });

        setLayout(new GridLayout());
        stratconPanel = new StratconPanel(getCampaignGui(), infoPanelText);
        JScrollPane scrollPane = new JScrollPane(stratconPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(StratconPanel.HEX_X_RADIUS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(StratconPanel.HEX_Y_RADIUS);
        this.add(scrollPane);

        // TODO: lance role assignment UI here?

        initializeInfoPanel();
        cmd = new CampaignManagementDialog(this);

        JScrollPane infoScrollPane = new JScrollPane(infoPanel);
        this.add(infoScrollPane);

        MekHQ.registerHandler(this);
    }

    /**
     * Worker function that sets up the layout of the right-side info panel.
     */
    private void initializeInfoPanel() {
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));

        infoPanel.add(new JLabel("Current Campaign Status:"));
        infoPanel.add(campaignStatusText);

        JButton btnManageCampaignState = new JButton("Manage SP/CVP");
        btnManageCampaignState.setHorizontalAlignment(SwingConstants.LEFT);
        btnManageCampaignState.setVerticalAlignment(SwingConstants.TOP);
        btnManageCampaignState.addActionListener(this::showCampaignStateManagement);
        infoPanel.add(btnManageCampaignState);

        expandedObjectivePanel = new JScrollPane(objectiveStatusText);
        expandedObjectivePanel.setMaximumSize(new Dimension(400, 300));
        expandedObjectivePanel.setAlignmentX(LEFT_ALIGNMENT);
        infoPanel.add(expandedObjectivePanel);

        JLabel lblCurrentTrack = new JLabel("Current Track:");
        infoPanel.add(lblCurrentTrack);

        cboCurrentTrack = new JComboBox<>();
        cboCurrentTrack.setAlignmentX(LEFT_ALIGNMENT);
        cboCurrentTrack.setMaximumSize(new Dimension(320, 20));
        repopulateTrackList();
        cboCurrentTrack.addItemListener(evt -> trackSelectionHandler());

        infoPanel.add(cboCurrentTrack);

        // have a default selected
        if (cboCurrentTrack.getItemCount() > 0) {
            trackSelectionHandler();
        }

        infoPanel.add(infoPanelText);
    }

    /**
     * Worker that handles track selection.
     */
    private void trackSelectionHandler() {
        TrackDropdownItem tdi = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        if (tdi != null) {
            stratconPanel.selectTrack(tdi.contract.getStratconCampaignState(), tdi.track);
            updateCampaignState();
        }
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
     * Worker function that updates the campaign state section of the info panel
     * with such info as current objective status, VP/SP totals, etc.
     */
    public void updateCampaignState() {
        if ((cboCurrentTrack == null) || (campaignStatusText == null)) {
            return;
        }

        // campaign state text should contain:
        // list of remaining objectives, percentage remaining
        // current VP
        // current support points
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        if (currentTDI == null) {
            campaignStatusText.setText("No active contract selected.");
            expandedObjectivePanel.setVisible(false);
            return;
        }
        AtBContract currentContract = currentTDI.contract;

        LocalDate currentDate = getCampaignGui().getCampaign().getLocalDate();

        if (currentContract.getStartDate().isAfter(currentDate)) {
            campaignStatusText.setText("Contract has not started.");
            expandedObjectivePanel.setVisible(false);
            return;
        }

        StratconCampaignState campaignState = currentContract.getStratconCampaignState();
        expandedObjectivePanel.setVisible(true);

        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
            .append(currentContract.getContractType()).append(": ").append(currentContract.getName())
            .append("<br/>")
            .append(campaignState.getBriefingText());

        if (currentContract.getEndingDate().isBefore(currentDate)) {
            sb.append("<br/>Contract term has expired!");
        }

        sb.append("<br/>Campaign Victory Points: ").append(campaignState.getVictoryPoints())
            .append("<br/>Support Points: ").append(campaignState.getSupportPoints())
            .append("<br/>Deployment Period: ").append(currentTDI.track.getDeploymentTime())
            .append(" days")
            .append("</html>");

        campaignStatusText.setText(sb.toString());

        objectiveStatusText.setText(getStrategicObjectiveText(campaignState));
    }

    /**
     * Builds strategic objective text, appropriately appending details
     * if the objectives are not "collapsed".
     */
    private String getStrategicObjectiveText(StratconCampaignState campaignState) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
            .append(buildShortStrategicObjectiveText(campaignState));

        if (objectivesCollapsed) {
            sb.append(" [+] ");
        } else {
            sb.append(" [-]<br/>")
                .append(buildStrategicObjectiveText(campaignState));
        }

        sb.append("</html>");

        return sb.toString();
    }

    /**
     * Builds strategic objective one-liner summary
     */
    private String buildShortStrategicObjectiveText(StratconCampaignState campaignState) {
        int completedObjectives = 0, desiredObjectives = 0;

        for (StratconTrackState track : campaignState.getTracks()) {
            for (StratconStrategicObjective objective : track.getStrategicObjectives()) {
                desiredObjectives++;

                if (objective.isObjectiveCompleted(track)) {
                    completedObjectives++;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        if (completedObjectives >= desiredObjectives) {
            sb.append("<span color='green'>");
        } else {
            sb.append("<span color='red'>");
        }

        // special logic for non-independent command clauses
        if (!campaignState.getContract().getCommandRights().isIndependent()) {
            desiredObjectives++;

            if (campaignState.getVictoryPoints() > 0) {
                completedObjectives++;
            }
        }

        sb.append("Strategic objectives: " + completedObjectives + "/" + desiredObjectives + " completed</span>");
        return sb.toString();
    }

    /**
     * Builds detailed strategic objective list
     */
    private String buildStrategicObjectiveText(StratconCampaignState campaignState) {
        StringBuilder sb = new StringBuilder();

        // loop through all tracks
        // for each track, loop through all objectives
        // for each objective, grab the coordinates
        // if !revealed, "locate and"
        // if specific scenario "engage hostile forces"
        // if hostile facility "capture or destroy [facility name]"
        // if allied facility "maintain control of [facility name]"
        // if revealed, " on track [current track] at coordinates [coords]
        for (StratconTrackState track : campaignState.getTracks()) {
            for (StratconStrategicObjective objective : track.getStrategicObjectives()) {
                boolean coordsRevealed = track.getRevealedCoords().contains(objective.getObjectiveCoords());
                boolean displayCoordinateData = objective.getObjectiveCoords() != null;
                boolean objectiveCompleted = objective.isObjectiveCompleted(track);
                boolean objectiveFailed = objective.isObjectiveFailed(track);

                // special case: allied facilities can get lost at any point in time
                if ((objective.getObjectiveType() == StrategicObjectiveType.AlliedFacilityControl) &&
                        !campaignState.allowEarlyVictory()) {
                    sb.append("<span color='orange'>").append(OBJECTIVE_IN_PROGRESS);
                } else if (objectiveCompleted) {
                    sb.append("<span color='green'>").append(OBJECTIVE_COMPLETED);
                } else if (objectiveFailed) {
                    sb.append("<span color='red'>").append(OBJECTIVE_FAILED);
                } else {
                    sb.append("<span color='orange'>").append(OBJECTIVE_IN_PROGRESS);
                }

                sb.append(" ");

                if (!coordsRevealed && displayCoordinateData) {
                    sb.append("Locate and ");
                }

                switch (objective.getObjectiveType()) {
                    case SpecificScenarioVictory:
                        sb.append(coordsRevealed ? "E" : "e")
                            .append("ngage and defeat hostile forces");
                        break;
                    case HostileFacilityControl:
                        sb.append(coordsRevealed ? "C" : "c")
                            .append("apture or destroy designated facility");
                        break;
                    case AlliedFacilityControl:
                        sb.append(coordsRevealed ? "M" : "m")
                            .append("aintain control of designated facility");

                        if (!campaignState.allowEarlyVictory()) {
                            sb.append(" until " + campaignState.getContract().getEndingDate());
                        }
                        break;
                    case AnyScenarioVictory:
                        sb.append("Engage and defeat hostile forces in ")
                            .append(objective.getCurrentObjectiveCount()).append("/")
                            .append(objective.getDesiredObjectiveCount())
                            .append(" scenarios on ").append(track.getDisplayableName());
                        break;
                    default:
                        break;
                }
                if (coordsRevealed && displayCoordinateData) {
                    sb.append(" at ").append(objective.getObjectiveCoords().toBTString())
                        .append(" on ").append(track.getDisplayableName());
                }

                sb.append("</span><br/>");
            }
        }

        // special case text reminding player to complete required scenarios
        if (!campaignState.getContract().getCommandRights().isIndependent()) {
            boolean contractIsActive = campaignState.getContract().isActiveOn(getCampaignGui().getCampaign().getLocalDate());

            if (contractIsActive) {
                sb.append("<span color='orange'>").append(OBJECTIVE_IN_PROGRESS);
            } else if (campaignState.getVictoryPoints() > 0) {
                sb.append("<span color='green'>").append(OBJECTIVE_COMPLETED);
            } else {
                sb.append("<span color='red'>").append(OBJECTIVE_FAILED);
            }

            sb.append(" Maintain Campaign Victory Point count above 0 by completing required scenarios")
                .append("</span><br/>");
        }

        return sb.toString();
    }

    /**
     * Refreshes the list of tracks
     */
    private void repopulateTrackList() {
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        cboCurrentTrack.removeAllItems();

        // track dropdown is populated with all tracks across all active contracts
        for (AtBContract contract : getCampaignGui().getCampaign().getActiveAtBContracts(true)) {
            if (contract.getStratconCampaignState() != null) {
                for (StratconTrackState track : contract.getStratconCampaignState().getTracks()) {
                    TrackDropdownItem tdi = new TrackDropdownItem(contract, track);
                    cboCurrentTrack.addItem(tdi);

                    if ((currentTDI != null) && currentTDI.equals(tdi)) {
                        currentTDI = tdi;
                        cboCurrentTrack.setSelectedItem(tdi);
                    } else if (currentTDI == null) {
                        currentTDI = tdi;
                        cboCurrentTrack.setSelectedItem(tdi);
                    }
                }
            }
        }

        if ((cboCurrentTrack.getItemCount() > 0) && (currentTDI != null) && (currentTDI.contract != null)) {
            TrackDropdownItem selectedTrack = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();

            stratconPanel.selectTrack(selectedTrack.contract.getStratconCampaignState(), currentTDI.track);
            stratconPanel.setVisible(true);
        } else {
            infoPanelText.setText("No active campaign tracks");
            stratconPanel.setVisible(false);
        }
    }

    private void showCampaignStateManagement(ActionEvent e) {
        TrackDropdownItem selectedTrack = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        if (selectedTrack == null) {
            return;
        }
        cmd.display(selectedTrack.contract.getStratconCampaignState(), selectedTrack.track, getCampaign().isGM());
        cmd.setModalityType(ModalityType.APPLICATION_MODAL);
        cmd.setVisible(true);
    }

    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        repopulateTrackList();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionRemovedEvent ev) {
        repopulateTrackList();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionCompletedEvent ev) {
        repopulateTrackList();
        updateCampaignState();
    }

    @Subscribe
    public void handle(StratconDeploymentEvent ev) {
        updateCampaignState();
    }

    /**
     * Data structure to hold necessary information about a track drop down item.
     * @author NickAragua
     */
    private static class TrackDropdownItem {
        AtBContract contract;
        StratconTrackState track;

        public TrackDropdownItem(AtBContract contract, StratconTrackState track) {
            this.contract = contract;
            this.track = track;
        }

        @Override
        public String toString() {
            return String.format("%s - %s", contract.getName(), track.getDisplayableName());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof TrackDropdownItem)) {
                return false;
            } else {
                TrackDropdownItem otherTDI = (TrackDropdownItem) other;
                return otherTDI.contract.equals(this.contract) && otherTDI.track.equals(this.track);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.contract, this.track);
        }
    }
}
