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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.MissionCompletedEvent;
import mekhq.campaign.event.MissionRemovedEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.StratconDeploymentEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconTrackState;

/**
 * This class contains code relevant to rendering the StratCon ("AtB Campaign State") tab.
 * @author NickAragua
 */
public class StratconTab extends CampaignGuiTab {
    private static final long serialVersionUID = 8179754409939346465L;
    
    private StratconPanel stratconPanel;
    private JPanel infoPanel;
    private JComboBox<TrackDropdownItem> cboCurrentTrack;
    private JLabel infoPanelText;
    private JLabel campaignStatusText;

    /**
     * Creates an instance of the StratconTab.
     */
    StratconTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    /**
     * Override of the base initTab method. Populates the tab.
     */
    @Override
    public void initTab() { 
        removeAll();
        
        infoPanelText = new JLabel();
        campaignStatusText = new JLabel();
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.ipadx = 5;
        stratconPanel = new StratconPanel(getCampaignGui(), infoPanelText);
        JScrollPane scrollPane = new JScrollPane(stratconPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(StratconPanel.HEX_X_RADIUS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(StratconPanel.HEX_Y_RADIUS);
        this.add(scrollPane, gbc);
        
        // TODO: lance role assignment UI here?
        
        initializeInfoPanel();
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        this.add(infoPanel, gbc);
        
        MekHQ.registerHandler(this);
    }

    /**
     * Worker function that sets up the layout of the right-side info panel.
     */
    private void initializeInfoPanel() {
        infoPanel = new JPanel();        
        infoPanel.setLayout(new GridBagLayout()); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        
        infoPanel.add(new JLabel("Current Campaign Status:"), gbc);
        
        gbc.gridy++;
        
        infoPanel.add(campaignStatusText, gbc);        
        gbc.gridy++;
        
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        JLabel lblCurrentTrack = new JLabel("Current Track:");
        infoPanel.add(lblCurrentTrack, gbc);
        gbc.gridx = 1;
                
        cboCurrentTrack = new JComboBox<>();
        repopulateTrackList();
        cboCurrentTrack.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                trackSelectionHandler();
            }
        });

        infoPanel.add(cboCurrentTrack, gbc);
        
        // have a default selected
        if (cboCurrentTrack.getItemCount() > 0) {
        	trackSelectionHandler();
        }
        
        gbc.gridy += 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        infoPanel.add(infoPanelText, gbc);
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
        super.repaint();
        updateCampaignState();
    }
    
    @Override
    public void refreshAll() {
        stratconPanel.repaint();
        updateCampaignState();
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.STRATCON;
    }
    
    /**
     * Worker function that updates the campaign state section of the info panel
     * with such info as current objective status, VP/SP totals, etc.
     */
    private void updateCampaignState() {
        if ((cboCurrentTrack == null) || (campaignStatusText == null)) {
            return;
        }
        
        // campaign state text should contain:
        // list of remaining objectives, percentage remaining
        // current VP
        // current support points
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        if (currentTDI == null) {
            return;
        }
        AtBContract currentContract = currentTDI.contract;
        StratconCampaignState campaignState = currentContract.getStratconCampaignState();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
            .append(currentContract.getMissionTypeName()).append(": ").append(currentContract.getName())
            .append("<br/>")
            .append(campaignState.getBriefingText());
        
        // avoid confusing users by showing strategic objectives when there are none to show
        if (!campaignState.strategicObjectivesBehaveAsVPs()) {
            sb.append("<br/>Strategic Objectives: ").append(campaignState.getStrategicObjectiveCompletedCount())
                .append("/").append(campaignState.getPendingStrategicObjectiveCount());
        }
        
        sb.append("<br/>Victory Points: ").append(campaignState.getVictoryPoints())
            .append("<br/>Support Points: ").append(campaignState.getSupportPoints())
            .append("<br/>Deployment Period: ").append(currentTDI.track.getDeploymentTime())
            .append(" days")
            .append("</html>");
        
        campaignStatusText.setText(sb.toString());
    }
    
    /**
     * Refreshes the list of tracks
     */
    private void repopulateTrackList() {
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        cboCurrentTrack.removeAllItems();
        
        // track dropdown is populated with all tracks across all active contracts
        for (Contract contract : getCampaignGui().getCampaign().getActiveContracts()) {
            if ((contract instanceof AtBContract) && 
            		contract.isActiveOn(getCampaignGui().getCampaign().getLocalDate()) && 
            		(((AtBContract) contract).getStratconCampaignState() != null)) {
                for (StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    TrackDropdownItem tdi = new TrackDropdownItem((AtBContract) contract, track);
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