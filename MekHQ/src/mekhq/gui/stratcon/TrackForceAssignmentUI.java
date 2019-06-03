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
import javax.swing.JList;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.stratcon.StratconTrackState;

public class TrackForceAssignmentUI extends JDialog implements ActionListener {
    private final static String CMD_MOVE_RIGHT = "CMD_MOVE_RIGHT";
    private final static String CMD_MOVE_LEFT = "CMD_MOVE_LEFT";
    private final static String CMD_CONFIRM = "CMD_CONFIRM";
    
    Campaign campaign;
    StratconCampaignState currentCampaignState;
    int currentTrackIndex;
    private JList<Force> availableForceList =  new JList<>();
    
    public TrackForceAssignmentUI(Campaign campaign) {
        this.campaign = campaign;
        initializeUI();
    }
    
    private void initializeUI() {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        getContentPane().add(availableForceList, gbc);

        gbc.gridy++;
        
        JButton btnConfirm = new JButton("Confirm");
        btnConfirm.setActionCommand(CMD_CONFIRM);
        btnConfirm.addActionListener(this);
        
        getContentPane().add(btnConfirm, gbc);
        
        pack();
        repaint();
    }
    
    public void display(StratconCampaignState campaignState, int currentTrackIndex) {       
        // first, we build a list of force IDs already assigned to this or other tracks
        // now, we add all un-assigned forces to the left side
        DefaultListModel<Force> availableForcesModel = new DefaultListModel<>();
        //for(Force force : StratconRulesManager.getAvailableForceIDs(template, campaign)) {
            //if(!unavailableForceIDs.contains(force.getId())) {
                //availableForcesModel.addElement(force);
            //}
        //}
        
        availableForceList.setModel(availableForcesModel);
        
        // and all forces already assigned to this track to the right side
        DefaultListModel<Force> assignedForcesModel = new DefaultListModel<>();
        //for(int forceID : campaignState.getTrack(currentTrackIndex).getAssignedForceIDs()) {
        //    assignedForcesModel.addElement(campaign.getForce(forceID));
        //}
        
        //assignedForceList.setModel(assignedForcesModel);
        
        this.currentCampaignState = campaignState;
        this.currentTrackIndex = currentTrackIndex;
        
        initializeUI();
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {
        switch(e.getActionCommand()) {
                case CMD_CONFIRM:
          //  DefaultListModel<Force> assignedListModel = (DefaultListModel<Force>) assignedForceList.getModel();
            Set<Integer> forceIDs = new HashSet<>(); 
            //for(int x = 0; x < assignedListModel.size(); x++) {
            //    forceIDs.add(assignedListModel.get(x).getId());
            //}
            
        //    currentCampaignState.getTrack(currentTrackIndex).setAssignedForceIDs(forceIDs);
            setVisible(false);
            break;
        }
    }
}
