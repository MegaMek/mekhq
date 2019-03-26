package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconCampaignState;

public class StratconScenarioWizard extends JDialog implements ActionListener {
    private final static String CMD_MOVE_RIGHT = "CMD_MOVE_RIGHT";
    private final static String CMD_MOVE_LEFT = "CMD_MOVE_LEFT";
    
    StratconScenario currentScenario;
    Campaign campaign;
    StratconTrackState currentTrackState;
    StratconCampaignState currentCampaignState;

    JLabel lblTotalBV = new JLabel();
    JList<Force> playerForceList = new JList<>();


    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
    }

    public void setCurrentScenario(StratconScenario scenario, StratconTrackState trackState, StratconCampaignState campaignState) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        currentTrackState = trackState;
        setUI();
    }

    public void setUI() {
        setTitle("Scenario Setup Wizard");
        getContentPane().removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        getContentPane().setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy++;
        switch(currentScenario.getCurrentState()) {        
            case UNRESOLVED:
                setAssignPrimaryForcesUI(gbc);
                break;
            default:
                setAssignReinforcementsUI(gbc);
                break;
        }

        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);
        pack();
        validate();
    }
    
    public void setAssignPrimaryForcesUI(GridBagConstraints gbc) {
        // generate a lance selector with the following parameters:
        // all forces assigned to the current track that aren't already assigned elsewhere
        // max number of items that can be selected = current scenario required lances
        
        setPlayerForceSelector(gbc, true);
    }
    
    public void setAssignReinforcementsUI(GridBagConstraints gbc) {
        // generate a lance selector with the following parameters:
        // A) all forces assigned to the current track that aren't already assigned elsewhere
        // PLUS B) all forces in the campaign that aren't already deployed to a scenario
        // max number of items is unlimited, but some have an SP selection cost (the ones from B)
        
        // to the right, have an SP cost readout (fancy: turns red when goes below 0 SP)
        // have three buttons: request allied air support, request allied ground support, request allied arty support
        // last two unavailable on atmo and space maps for obvious reasons, each costs 1 SP
        // grayed out if campaign state SP 
        
        JButton btnRight = new JButton(">>>");
        btnRight.setActionCommand(CMD_MOVE_RIGHT);
        btnRight.addActionListener(this);
        
        JButton btnLeft = new JButton("<<<");
        btnLeft.setActionCommand(CMD_MOVE_LEFT);
        btnLeft.addActionListener(this);
        
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(btnRight, gbc);
        
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTH;
        getContentPane().add(btnLeft, gbc);
    }

    public void setPlayerForceSelector(GridBagConstraints gbc, boolean trackForcesOnly) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        if(trackForcesOnly) {
            lanceModel = new ScenarioWizardLanceModel(campaign, currentTrackState);
        } else {
            lanceModel = new ScenarioWizardLanceModel(campaign);
        }
        
        playerForceList.setModel(lanceModel);
        playerForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        playerForceList.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                playerForceSelectorChanged(e);
            }
        });

        forceListContainer.setViewportView(playerForceList);

        gbc.gridx = 0;
        getContentPane().add(forceListContainer, gbc);

        gbc.gridx = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        lblTotalBV.setText("Selected BV: 0");
        getContentPane().add(lblTotalBV, gbc);
    }

    /**
     * Sets the navigation button
     * @param gbc
     */
    private void setNavigationButtons(GridBagConstraints gbc) {
        // you're on one of two screens:
        // the 'primary force selection' screen
        // the 'reinforcement selection' screen
        // you can re-visit the second one
        JButton btnCommit = new JButton("Commit");
        btnCommit.setActionCommand("COMMIT_CLICK");
        btnCommit.addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {
                btnCommitClicked(e);
            }
        });

        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;

        getContentPane().add(btnCommit, gbc);
    }

    /**
     * Event handler for when the user clicks the 'commit' button.
     * Behaves differently depending on the state of the 
     * @param e
     */
    private void btnCommitClicked(ActionEvent e) {
        if(currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            currentScenario.commitPrimaryForces(campaign, currentCampaignState.getContract());
        }
    }

    /**
     * Event handler for when the user makes a selection on the player force selector.
     * Updates the "selected BV" label.
     * @param e The event fired. 
     */
    private void playerForceSelectorChanged(ListSelectionEvent e) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }

        int totalBV = 0;
        JList<Force> sourceList = (JList<Force>) e.getSource();
        for(Force force : sourceList.getSelectedValuesList()) {
            totalBV += force.getTotalBV(campaign);
        }

        lblTotalBV.setText(String.format("Selected BV: %d", totalBV));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }
}