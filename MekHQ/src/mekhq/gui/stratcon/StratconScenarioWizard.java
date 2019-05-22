package mekhq.gui.stratcon;

import java.awt.Color;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
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
    
    JList<Unit> availableInfantryUnits = new JList<>();
    JLabel defensiveOptionStatus = new JLabel();

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
        
        switch(currentScenario.getCurrentState()) {        
            case UNRESOLVED:
                setAssignForcesUI(gbc, false);
                break;
            default:
                gbc.gridy++;
                setAssignForcesUI(gbc, true);
                gbc.gridy++;
                setDefensiveUI(gbc);
                break;
        }

        /*gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);*/
        pack();
        validate();
    }
    
    private void setInstructions(GridBagConstraints gbc) {
        JLabel lblInfo = new JLabel();
        StringBuilder labelBuilder = new StringBuilder();
        labelBuilder.append("<html>");
        labelBuilder.append(currentScenario.getInfo(true, true));
        
        switch(currentScenario.getCurrentState()) {
        case UNRESOLVED:
            labelBuilder.append("Assign the required number of lances to the scenario.<br/>");
            break;
        default:
            labelBuilder.append("Assign any reinforcements or make allied support requests.<br/>");
            break;
        }
        
        lblInfo.setText(labelBuilder.toString());
        getContentPane().add(lblInfo, gbc);
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
            JPanel forcePairPanel = new JPanel();
            forcePairPanel.setLayout(new GridBagLayout());
            GridBagConstraints localGbc = new GridBagConstraints();
            localGbc.gridx = 0;
            localGbc.gridy = 0;
            localGbc.gridheight = 2;
            
            JList<Force> availableForceList = addAvailableForceList(forcePairPanel, localGbc, controlSetIndex, forceTemplate);
            
            localGbc.gridx = 1;
            localGbc.gridheight = 1;
            localGbc.gridy = 0;
            localGbc.anchor = GridBagConstraints.SOUTH;
            
            JButton btnRight = new JButton(">>>");
            btnRight.setActionCommand(String.format("%s|%d", CMD_MOVE_RIGHT, controlSetIndex));
            btnRight.addActionListener(this);       
            rightButtons.add(btnRight);
            forcePairPanel.add(btnRight, localGbc);
            
            localGbc.gridy = 1;
            localGbc.anchor = GridBagConstraints.NORTH;
            
            JButton btnLeft = new JButton("<<<");
            btnLeft.setActionCommand(String.format("%s|%d", CMD_MOVE_LEFT, controlSetIndex));
            btnLeft.addActionListener(this);
            leftButtons.add(btnLeft);
            forcePairPanel.add(btnLeft, localGbc);
            
            localGbc.gridx = 2;
            localGbc.gridy = 0;
            localGbc.gridheight = 2;
            JList<Force> assignedForceList = addAssignedForceList(forcePairPanel, localGbc, controlSetIndex);
            
            availableForceLists.add(availableForceList);
            assignedForceLists.add(assignedForceList);
            
            getContentPane().add(forcePairPanel, gbc);
            controlSetIndex++;
        }
    }

    /**
     * Set up the UI for "defensive elements".
     * @param gbc
     */
    private void setDefensiveUI(GridBagConstraints gbc) {
        addIndividualUnitSelector(StratconRulesManager.getEligibleDefensiveUnits(campaign), 
                gbc, 
                currentScenario.getBackingScenario().getLanceCommanderSkill(SkillType.S_TACTICS, campaign));
    }
    
    private JList<Force> addAvailableForceList(JPanel parent, GridBagConstraints gbc, int index, ScenarioForceTemplate forceTemplate) {
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

        parent.add(forceListContainer, gbc);
        return availableForceList;
    }
    
    private JList<Force> addAssignedForceList(JPanel parent, GridBagConstraints gbc, int index) {
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

        parent.add(forceListContainer, gbc);
        return assignedForceList;
    }

    /**
     * Adds an individual unit selector, given a list of individual units, a global grid bag constraint set
     * and a maximum selection size.
     * @param units
     * @param gbc
     * @param maxSelectionSize
     */
    private void addIndividualUnitSelector(List<Unit> units, GridBagConstraints gbc, int maxSelectionSize) {
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.WEST;
        JLabel instructions = new JLabel();
        instructions.setText(String.format("Select individual units (%d max)", maxSelectionSize));
        unitPanel.add(instructions);
        
        localGbc.gridy++;        
        DefaultListModel<Unit> availableModel = new DefaultListModel<>();
        for(Unit u : units) {
            availableModel.addElement(u);
        }
        
        JLabel unitStatusLabel = new JLabel();
        
        // add the # units selected control
        JLabel unitSelectionLabel = new JLabel();
        unitSelectionLabel.setText("0 selected");
        
        localGbc.gridy++;
        unitPanel.add(unitSelectionLabel, localGbc);
        
        availableInfantryUnits.setModel(availableModel);
        availableInfantryUnits.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                availableUnitSelectorChanged(e, unitSelectionLabel, unitStatusLabel, maxSelectionSize);
            }
        });
        
        JScrollPane infantryContainer = new JScrollPane();
        infantryContainer.setViewportView(availableInfantryUnits);
        localGbc.gridy++;
        unitPanel.add(infantryContainer, localGbc);
        
        // add the 'status display' control
        localGbc.gridx++;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        unitPanel.add(unitStatusLabel, localGbc);
        
        getContentPane().add(unitPanel, gbc);
    }
    
    /**
     * Worker function that builds an "html-enabled" string indicating the brief status of an individual unit
     * @param u
     * @return
     */
    private String buildUnitStatus(Unit u) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(u.getName());
        sb.append(": ");
        sb.append(u.getStatus());
        
        int injuryCount = 0;
        
        for(Person p : u.getCrew()) {
            if(p.hasInjuries(true)) {
                injuryCount++;
            }
        }
        
        if(injuryCount > 0) {
            sb.append(String.format(", <span color='red'>%d/%d injured crew</span>", injuryCount, u.getCrew().size()));
        }
        
        sb.append("<br/>");
        return sb.toString();
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

    /**
     * Event handler for when an available unit selector's selection changes.
     * Updates the "# units selected" label and the unit status label. 
     * Also checks maximum selection size and disables commit button (TBD).
     * @param e
     * @param selectionCountLabel Which label to update with how many items are selected
     * @param unitStatusLabel Which label to update with detailed unit info
     * @param maxSelectionSize How many items can be selected at most
     */
    private void availableUnitSelectorChanged(ListSelectionEvent e, JLabel selectionCountLabel, JLabel unitStatusLabel, int maxSelectionSize) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }
        
        JList<Unit> changedList = (JList<Unit>) e.getSource();
        selectionCountLabel.setText(String.format("%d selected", changedList.getSelectedIndices().length));
        if(changedList.getSelectedIndices().length > maxSelectionSize) {
            selectionCountLabel.setForeground(Color.RED);
        } else {
            selectionCountLabel.setForeground(Color.BLACK);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        
        for(Unit u : changedList.getSelectedValuesList()) {
            sb.append(buildUnitStatus(u));
        }
        
        sb.append("</html>");
        
        unitStatusLabel.setText(sb.toString());
        pack();
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