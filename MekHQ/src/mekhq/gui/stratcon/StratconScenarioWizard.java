package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
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
    private final static String CMD_MOVE_CONFIRM = "CMD_CONFIRM";
    
    StratconScenario currentScenario;
    Campaign campaign;
    StratconTrackState currentTrackState;
    StratconCampaignState currentCampaignState;

    JLabel lblTotalBV = new JLabel();
    JList<Force> availableForceList = new JList<>();
    JList<Force> selectedForceList = new JList<>();
    
    JButton btnRight = new JButton(">>>");
    JButton btnLeft = new JButton("<<<");


    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
        initializeButtons();
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
        gbc.gridy = 0;
        
        setInstructions(gbc);
        gbc.gridy = 2;
        
        switch(currentScenario.getCurrentState()) {        
            case UNRESOLVED:
                setAssignPrimaryForcesUI(gbc);
                break;
            default:
                setAssignReinforcementsUI(gbc);
                break;
        }

        gbc.gridy = GridBagConstraints.REMAINDER;
        setNavigationButtons(gbc);
        pack();
        validate();
    }
    
    private void setInstructions(GridBagConstraints gbc) {
        JLabel lblInfo = new JLabel(currentScenario.getInfo(false));
        getContentPane().add(lblInfo,  gbc);
        gbc.gridy++;
        
        JLabel lblInstructions = new JLabel();
        switch(currentScenario.getCurrentState()) {
        case UNRESOLVED:
            lblInstructions.setText("Assign the required number of lances to the scenario.");
            break;
        default:
            lblInstructions.setText("Assign any reinforcements or make allied support requests.");
            break;
        }
        
        getContentPane().add(lblInstructions, gbc);
    }
    
    private void setAssignPrimaryForcesUI(GridBagConstraints gbc) {
        // generate a lance selector with the following parameters:
        // all forces assigned to the current track that aren't already assigned elsewhere
        // max number of items that can be selected = current scenario required lances
        
        addAvailableForceList(gbc);
        addSelectedBVLabel(gbc);
        
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(btnRight, gbc);
        
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTH;
        getContentPane().add(btnLeft, gbc);
        
        gbc.gridx = 2;
        gbc.gridy -= 2;
        addSelectedForceList(gbc);
    }
    
    private void setAssignReinforcementsUI(GridBagConstraints gbc) {
        // generate a lance selector with the following parameters:
        // A) all forces assigned to the current track that aren't already assigned elsewhere
        // PLUS B) all forces in the campaign that aren't already deployed to a scenario
        // max number of items is unlimited, but some have an SP selection cost (the ones from B)
        
        // to the right, have an SP cost readout (fancy: turns red when goes below 0 SP)
        // have three buttons: request allied air support, request allied ground support, request allied arty support
        // last two unavailable on atmo and space maps for obvious reasons, each costs 1 SP
        // grayed out if campaign state SP 
        
        addAvailableForceList(gbc);
        
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.NONE;
        getContentPane().add(btnRight, gbc);
        
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTH;
        getContentPane().add(btnLeft, gbc);
        
        gbc.gridx = 2;
        gbc.gridy -= 2;
        addSelectedForceList(gbc);
    }

    private void addAvailableForceList(GridBagConstraints gbc) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        // if we're waiting to assign primary forces, we can only do so from the current track 
        if(currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            lanceModel = new ScenarioWizardLanceModel(campaign, currentTrackState.getAvailableForceIDs());
        // if we want to assign reinforcements, then we can do so from any unassigned forces
        } else {
            lanceModel = new ScenarioWizardLanceModel(campaign, currentCampaignState.getAvailableForceIDs());
        }
        
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        availableForceList.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                availableForceSelectorChanged(e);
            }
        });

        forceListContainer.setViewportView(availableForceList);

        gbc.gridheight = 3;
        getContentPane().add(forceListContainer, gbc);
    }
    
    private void addSelectedForceList(GridBagConstraints gbc) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign, currentScenario.getAssignedForces());
        
        selectedForceList.setModel(lanceModel);
        selectedForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        selectedForceList.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                assignedForceSelectorChanged(e);
            }
        });

        forceListContainer.setViewportView(selectedForceList);

        gbc.gridheight = 3;
        getContentPane().add(forceListContainer, gbc);
    }
    
    private void addSelectedBVLabel(GridBagConstraints gbc) {
        gbc.gridx++;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        lblTotalBV.setText("Selected BV: 0");
        getContentPane().add(lblTotalBV, gbc);
        gbc.gridx--;
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
     * Sets up the move right / move left buttons
     */
    private void initializeButtons() {
        btnRight.setActionCommand(CMD_MOVE_RIGHT);
        btnRight.addActionListener(this);        
        
        btnLeft.setActionCommand(CMD_MOVE_LEFT);
        btnLeft.addActionListener(this);
    }
    
    /**
     * Event handler for when the user clicks the 'commit' button.
     * Behaves differently depending on the state of the scenario
     * @param e
     */
    private void btnCommitClicked(ActionEvent e) {
        // gather up all the forces on the right side, and assign them to the scenario
        DefaultListModel<Force> assignedListModel = (DefaultListModel<Force>) selectedForceList.getModel();
        Set<Integer> forceIDs = new HashSet<>(); 
        for(int x = 0; x < assignedListModel.size(); x++) {
            forceIDs.add(assignedListModel.get(x).getId());
        }
        
        // scenarios that haven't had primary forces committed yet get those committed now
        // and the scenario gets published to the campaign and may be played immediately from the briefing room
        if(currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            currentScenario.addForces(forceIDs);
            currentScenario.commitPrimaryForces(campaign, currentCampaignState.getContract());
        // scenarios that have had primary forces committed can have reinforcements added and removed "at will"
        // until they've been actually played out
        } else {
            currentScenario.clearReinforcements();
            currentScenario.addForces(forceIDs);
        }
        
        setVisible(false);
    }

    /**
     * Event handler for when the user makes a selection on the assigned force selector.
     * @param e The event fired. 
     */
    private void assignedForceSelectorChanged(ListSelectionEvent e) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }

        int totalBV = 0;
        JList<Force> sourceList = (JList<Force>) e.getSource();
        for(Force force : sourceList.getSelectedValuesList()) {
            totalBV += force.getTotalBV(campaign);
        }

        lblTotalBV.setText(String.format("Selected BV: %d", totalBV));
        
        boolean enableButton = !sourceList.isSelectionEmpty() &&
                !currentScenario.getPrimaryPlayerForceIDs().contains(sourceList.getSelectedValue().getId());
        
        btnLeft.setEnabled(enableButton);
    }
    
    /**
     * Event handler for when the user makes a selection on the available force selector.
     * @param e The event fired. 
     */
    private void availableForceSelectorChanged(ListSelectionEvent e) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }

        /*JList<Force> sourceList = (JList<Force>) e.getSource();
                
        boolean enableButton = !sourceList.isSelectionEmpty() &&
                !currentScenario.getAssignedForces().contains(sourceList.getSelectedValue().getId());
        
        btnRight.setEnabled(enableButton);*/
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ScenarioWizardLanceModel selectedForceModel = (ScenarioWizardLanceModel) selectedForceList.getModel();
        ScenarioWizardLanceModel availableForceModel = (ScenarioWizardLanceModel) availableForceList.getModel();
        
        switch(e.getActionCommand()) {
        case CMD_MOVE_RIGHT:
            if(!availableForceList.isSelectionEmpty()) {
                selectedForceModel.addElement(availableForceList.getSelectedValue());
                availableForceModel.removeElement(availableForceList.getSelectedValue());
            }
            break;
        case CMD_MOVE_LEFT:
            if(!selectedForceList.isSelectionEmpty()) {
                availableForceModel.addElement(selectedForceList.getSelectedValue());
                selectedForceModel.removeElement(selectedForceList.getSelectedValue());
            }
            break;
        }
    }
}