package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.DayOfWeek;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;

public class StratconTab extends CampaignGuiTab {
    /**
     * 
     */
    private static final long serialVersionUID = 8179754409939346465L;
    
    private StratconPanel stratconPanel;
    private JPanel infoPanel;
    private JComboBox<TrackDropdownItem> cboCurrentTrack;
    private JLabel infoPanelText;
    private JLabel campaignStatusText;

    StratconTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

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
        stratconPanel = new StratconPanel(getCampaignGui(), infoPanelText);
        JScrollPane scrollPane = new JScrollPane(stratconPanel);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(StratconPanel.HEX_X_RADIUS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(StratconPanel.HEX_Y_RADIUS);
        this.add(scrollPane, gbc);
        
        initializeInfoPanel();
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        this.add(infoPanel, gbc);
        
        MekHQ.registerHandler(this);
    }

    private void initializeInfoPanel() {
        infoPanel = new JPanel();        
        infoPanel.setLayout(new GridBagLayout()); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        
        infoPanel.add(new JLabel("Current Campaign Status:"), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy++;
        
        infoPanel.add(campaignStatusText, gbc);        
        gbc.gridy++;
        
        JLabel lblCurrentTrack = new JLabel("Current Track:");
        infoPanel.add(lblCurrentTrack, gbc);
        gbc.gridx = 1;
        
        cboCurrentTrack = new JComboBox<>();
        repopulateTrackList();
        cboCurrentTrack.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                TrackDropdownItem tdi = (TrackDropdownItem) e.getItem();
                stratconPanel.selectTrack(tdi.contract.getStratconCampaignState(), tdi.track);
            }
        });

        infoPanel.add(cboCurrentTrack, gbc);
        
        // have a default selected
        if(cboCurrentTrack.getItemCount() > 0) {
            TrackDropdownItem tdi = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
            stratconPanel.selectTrack(tdi.contract.getStratconCampaignState(), tdi.track);
        }
        
        gbc.gridy++;
        infoPanel.add(infoPanelText, gbc);
    }
    
    @Override
    public void repaint() {
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
    
    private void updateCampaignState() {
        if ((cboCurrentTrack == null) || (campaignStatusText == null)) {
            return;
        }
        
        // campaign state text should contain:
        // list of remaining objectives, percentage remaining
        // current VP
        // current support points
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        if(currentTDI == null) {
            return;
        }
        AtBContract currentContract = currentTDI.contract;
        StratconCampaignState campaignState = currentContract.getStratconCampaignState();
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        
        sb.append(currentContract.getMissionTypeName()).append(": ").append(currentContract.getName());
        sb.append("<br/>Strategic Objectives: ").append(campaignState.getStrategicObjectiveCompletedCount())
            .append("/").append(campaignState.getPendingStrategicObjectiveCount());
        
        sb.append("<br/>Victory Points: ").append(campaignState.getVictoryPoints());
        
        sb.append("<br/>Support Points: ").append(campaignState.getSupportPoints());
        
        sb.append("</html>");
        
        campaignStatusText.setText(sb.toString());
    }
    
    private void repopulateTrackList() {
        TrackDropdownItem currentTDI = (TrackDropdownItem) cboCurrentTrack.getSelectedItem();
        cboCurrentTrack.removeAllItems();
        
        // track dropdown is populated with all tracks across all active contracts
        for(Contract contract : getCampaignGui().getCampaign().getActiveContracts()) {
            if((contract instanceof AtBContract) && contract.isActive() && (((AtBContract) contract).getStratconCampaignState() != null)) {
                for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    TrackDropdownItem tdi = new TrackDropdownItem((AtBContract) contract, track);
                    cboCurrentTrack.addItem(tdi);
                    
                    if((currentTDI != null) && currentTDI.equals(tdi)) {
                        currentTDI = tdi;
                        cboCurrentTrack.setSelectedItem(tdi);
                    } else if (currentTDI == null) {
                        currentTDI = tdi;
                        cboCurrentTrack.setSelectedItem(tdi);
                    }
                }
            }
        }
        
        if((cboCurrentTrack.getItemCount() > 0) && (currentTDI != null) && (currentTDI.contract != null)) {
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
    
    /**
     * Data structure to hold necessary information about a track drop down item.
     * @author NickAragua
     *
     */
    private class TrackDropdownItem {
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
            if(!(other instanceof TrackDropdownItem) || (other == null)) {
                return false;
            } else {
                TrackDropdownItem otherTDI = (TrackDropdownItem) other;
                return otherTDI.contract.equals(this.contract) && otherTDI.track.equals(this.track);
            }
        }
    }
}