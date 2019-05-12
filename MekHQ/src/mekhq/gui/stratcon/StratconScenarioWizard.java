package mekhq.gui.stratcon;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconRulesManager;

public class StratconScenarioWizard extends JDialog implements ActionListener {
    private final static String CMD_MOVE_RIGHT = "CMD_MOVE_RIGHT";
    private final static String CMD_MOVE_LEFT = "CMD_MOVE_LEFT";
    private final static String CMD_MOVE_CONFIRM = "CMD_CONFIRM";
    
    StratconScenario currentScenario;
    Campaign campaign;
    StratconTrackState currentTrackState;
    StratconCampaignState currentCampaignState;

    JLabel lblTotalBV = new JLabel();
    
    List<JList<Force>> availableForceLists = new ArrayList<>();
    List<JList<Force>> assignedForceLists = new ArrayList<>();
    List<JButton> rightButtons = new ArrayList<>();
    List<JButton> leftButtons = new ArrayList<>();

    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
    }

    public void setCurrentScenario(StratconScenario scenario, StratconTrackState trackState, StratconCampaignState campaignState) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        currentTrackState = trackState;
        
        availableForceLists.clear();
        assignedForceLists.clear();
        rightButtons.clear();
        leftButtons.clear();
        
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
                setAssignForcesUI(gbc, false);
                break;
            default:
                setAssignForcesUI(gbc, true);
                break;
        }

        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);
        pack();
        validate();
    }
    
    private void setInstructions(GridBagConstraints gbc) {
        JLabel lblInfo = new JLabel(currentScenario.getInfo(false));
        getContentPane().add(lblInfo, gbc);
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
    
    private void setAssignForcesUI(GridBagConstraints gbc, boolean reinforcements) {
        // generate a lance selector with the following parameters:
        // all forces assigned to the current track that aren't already assigned elsewhere
        // max number of items that can be selected = current scenario required lances
        int controlSetIndex = 0;
        
        List<ScenarioForceTemplate> eligibleForceTemplates = reinforcements ?
                currentScenario.getScenarioTemplate().getAllPlayerReinforcementForces() :
                    currentScenario.getScenarioTemplate().getAllPrimaryPlayerForces();
        
        for(ScenarioForceTemplate forceTemplate : eligibleForceTemplates) {
            JList<Force> availableForceList = addAvailableForceList(gbc, controlSetIndex, forceTemplate);
            addSelectedBVLabel(gbc);
            
            gbc.gridx = 1;
            gbc.gridy++;
            gbc.anchor = GridBagConstraints.SOUTH;
            gbc.fill = GridBagConstraints.NONE;
            
            JButton btnRight = new JButton(">>>");
            btnRight.setActionCommand(String.format("%s|%d", CMD_MOVE_RIGHT, controlSetIndex));
            btnRight.addActionListener(this);       
            rightButtons.add(btnRight);
            getContentPane().add(btnRight, gbc);
            
            gbc.gridy++;
            gbc.anchor = GridBagConstraints.NORTH;
            
            JButton btnLeft = new JButton("<<<");
            btnLeft.setActionCommand(String.format("%s|%d", CMD_MOVE_LEFT, controlSetIndex));
            btnLeft.addActionListener(this);
            leftButtons.add(btnLeft);
            getContentPane().add(btnLeft, gbc);
            
            gbc.gridx = 2;
            gbc.gridy -= 2;
            JList<Force> assignedForceList = addAssignedForceList(gbc, controlSetIndex);
            
            availableForceLists.add(availableForceList);
            assignedForceLists.add(assignedForceList);
            controlSetIndex++;
        }
    }

    private JList<Force> addAvailableForceList(GridBagConstraints gbc, int index, ScenarioForceTemplate forceTemplate) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        // if we're waiting to assign primary forces, we can only do so from the current track 
        lanceModel = new ScenarioWizardLanceModel(campaign, StratconRulesManager.getAvailableForceIDs(forceTemplate, campaign));
        
        JList<Force> availableForceList = new JList<>();
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
        return availableForceList;
    }
    
    private JList<Force> addAssignedForceList(GridBagConstraints gbc, int index) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign, currentScenario.getAssignedForces());
        
        JList<Force> assignedForceList = new JList<>();
        assignedForceList.setModel(lanceModel);
        assignedForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        assignedForceList.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                assignedForceSelectorChanged(e, index);
            }
        });

        forceListContainer.setViewportView(assignedForceList);

        gbc.gridheight = 3;
        getContentPane().add(forceListContainer, gbc);
        return assignedForceList;
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

        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        getContentPane().add(btnCommit, gbc);
    }
    
    /**
     * Event handler for when the user clicks the 'commit' button.
     * Behaves differently depending on the state of the scenario
     * @param e
     */
    private void btnCommitClicked(ActionEvent e) {
        // gather up all the forces on the right side, and assign them to the scenario
        Set<Integer> forceIDs = new HashSet<>(); 
        
        for(JList<Force> assignedForceList : assignedForceLists) {
            DefaultListModel<Force> assignedListModel = (DefaultListModel<Force>) assignedForceList.getModel();
            
            for(int x = 0; x < assignedListModel.size(); x++) {
                forceIDs.add(assignedListModel.get(x).getId());
            }
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
    private void assignedForceSelectorChanged(ListSelectionEvent e, int controlIndex) {
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
        
        leftButtons.get(controlIndex).setEnabled(enableButton);
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
        String commandString = e.getActionCommand().substring(0, e.getActionCommand().indexOf("|"));
        int commandIndex = Integer.parseInt(e.getActionCommand().substring(e.getActionCommand().indexOf("|") + 1));
        
        JList<Force> availableForceList = availableForceLists.get(commandIndex);
        JList<Force> assignedForceList = assignedForceLists.get(commandIndex);
        
        ScenarioWizardLanceModel assignedForceModel = (ScenarioWizardLanceModel) assignedForceList.getModel();
        ScenarioWizardLanceModel availableForceModel = (ScenarioWizardLanceModel) availableForceList.getModel();
        
        switch(commandString) {
        case CMD_MOVE_RIGHT:
            if(!availableForceList.isSelectionEmpty()) {
                assignedForceModel.addElement(availableForceList.getSelectedValue());
                availableForceModel.removeElement(availableForceList.getSelectedValue());
            }
            break;
        case CMD_MOVE_LEFT:
            if(!assignedForceList.isSelectionEmpty()) {
                availableForceModel.addElement(assignedForceList.getSelectedValue());
                assignedForceModel.removeElement(assignedForceList.getSelectedValue());
            }
            break;
        }
    }
}