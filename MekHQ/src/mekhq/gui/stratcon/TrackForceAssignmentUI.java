package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.StratconPanel;

/**
 * This class handles the "assign force to track" interaction, 
 * where a user may assign a force to a track directly, either to a facility or 
 * @author NickAragua
 *
 */
public class TrackForceAssignmentUI extends JDialog implements ActionListener {
    private final static String CMD_CONFIRM = "CMD_TRACK_FORCE_CONFIRM";
    
    Campaign campaign;
    StratconCampaignState currentCampaignState;
    int currentTrackIndex;
    StratconCoords selectedCoords;
    private JList<Force> availableForceList = new JList<>();
    private JButton btnConfirm = new JButton();
    private StratconPanel ownerPanel;
    
    public TrackForceAssignmentUI(StratconPanel parent) {
        ownerPanel = parent;
        btnConfirm = new JButton("Confirm");
        btnConfirm.setActionCommand(CMD_CONFIRM);
        btnConfirm.addActionListener(this);
    }
    
    private void initializeUI() {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        
        JLabel forceAssignmentInstructions = new JLabel("Select force to assign to this sector.");
        getContentPane().add(forceAssignmentInstructions, gbc);
        gbc.gridy++;

        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        // if we're waiting to assign primary forces, we can only do so from the current track 
        lanceModel = new ScenarioWizardLanceModel(campaign, 
                StratconRulesManager.getAvailableForceIDs(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX, 
                        campaign, false));
        
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        
        forceListContainer.setViewportView(availableForceList);

        getContentPane().add(forceListContainer, gbc);
        

        gbc.gridy++;
        
        getContentPane().add(btnConfirm, gbc);
        
        pack();
        repaint();
    }
    
    /**
     * Display the track force assignment UI.
     * @param campaign
     * @param campaignState
     * @param currentTrackIndex
     */
    public void display(Campaign campaign, StratconCampaignState campaignState, int currentTrackIndex, StratconCoords coords) {
        this.campaign = campaign;
        this.currentCampaignState = campaignState;
        this.currentTrackIndex = currentTrackIndex;
        this.selectedCoords = coords;
        
        initializeUI();
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {
        switch(e.getActionCommand()) {
            case CMD_CONFIRM:
                // sometimes the scenario templates take a little while to load, we don't want the user
                // clicking the button fifty times and getting a bunch of scenarios.
                btnConfirm.setEnabled(false);                
                for(Force force : availableForceList.getSelectedValuesList()) {
                    StratconRulesManager.deployForceToCoords(selectedCoords, force.getId(), 
                            campaign, currentCampaignState.getContract(), currentCampaignState.getTrack(currentTrackIndex));
                }
                setVisible(false);
                ownerPanel.repaint();
                btnConfirm.setEnabled(true);
                break;
        }
    }
}
