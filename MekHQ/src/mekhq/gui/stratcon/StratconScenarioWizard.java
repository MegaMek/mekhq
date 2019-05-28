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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;

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

public class StratconScenarioWizard extends JDialog {
    private final static int TACTICS_NOT_LOADED = -1;
    
    StratconScenario currentScenario;
    Campaign campaign;
    StratconTrackState currentTrackState;
    StratconCampaignState currentCampaignState;
    ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AtBStratCon");

    JLabel lblTotalBV = new JLabel();
    
    Map<String, JList<Force>> availableForceLists = new HashMap<>();
    Map<String, JList<Unit>> availableUnitLists = new HashMap<>();
    
    JList<Unit> availableInfantryUnits = new JList<>();
    JLabel defensiveOptionStatus = new JLabel();

    // lazy-loaded commander tactics property and backing field    
    private int commanderTactics = TACTICS_NOT_LOADED;
    private int getCommanderTactics() {
        if(commanderTactics == TACTICS_NOT_LOADED) {
            commanderTactics = currentScenario.getBackingScenario().getLanceCommanderSkill(SkillType.S_TACTICS, campaign);
        }
        
        return commanderTactics;
    }
    
    public StratconScenarioWizard(Campaign campaign) {
        this.campaign = campaign;
    }

    public void setCurrentScenario(StratconScenario scenario, StratconTrackState trackState, StratconCampaignState campaignState) {
        currentScenario = scenario;
        currentCampaignState = campaignState;
        currentTrackState = trackState;
        commanderTactics = TACTICS_NOT_LOADED;
        
        availableForceLists.clear();
        availableUnitLists.clear();
        
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
                gbc.gridy++;
                //setSupportUI(gbc, true);
                break;
        }

        gbc.gridx = 0;
        gbc.gridy++;
        setNavigationButtons(gbc);
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
            labelBuilder.append("primaryForceAssignemntInstructions.text");
            break;
        default:
            labelBuilder.append("reinforcementsAndSupportInstructions.text");
            break;
        }
        
        labelBuilder.append("<br/>");
        lblInfo.setText(labelBuilder.toString());
        getContentPane().add(lblInfo, gbc);
    }
    
    private void setAssignForcesUI(GridBagConstraints gbc, boolean reinforcements) {
        // generate a lance selector with the following parameters:
        // all forces assigned to the current track that aren't already assigned elsewhere
        // max number of items that can be selected = current scenario required lances
        
        List<ScenarioForceTemplate> eligibleForceTemplates = reinforcements ?
                currentScenario.getScenarioTemplate().getAllPlayerReinforcementForces() :
                    currentScenario.getScenarioTemplate().getAllPrimaryPlayerForces();
        
        for(ScenarioForceTemplate forceTemplate : eligibleForceTemplates) {
            JPanel forcePanel = new JPanel();
            forcePanel.setLayout(new GridBagLayout());
            GridBagConstraints localGbc = new GridBagConstraints();
            localGbc.gridx = 0;
            localGbc.gridy = 0;
            JLabel assignForceListInstructions = new JLabel("selectForceForTemplate.text");
            forcePanel.add(assignForceListInstructions, localGbc);
            
            localGbc.gridy = 1;
            JLabel selectedForceInfo = new JLabel();
            JList<Force> availableForceList = addAvailableForceList(forcePanel, localGbc, forceTemplate);
            
            availableForceList.addListSelectionListener(new ListSelectionListener() { 
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    availableForceSelectorChanged(e, selectedForceInfo, reinforcements);
                }
            });
            
            availableForceLists.put(forceTemplate.getForceName(), availableForceList);
            
            localGbc.gridx = 1;
            forcePanel.add(selectedForceInfo, localGbc);
            
            getContentPane().add(forcePanel, gbc);
        }
    }

    /**
     * Set up the UI for "defensive elements".
     * @param gbc
     */
    private void setDefensiveUI(GridBagConstraints gbc) {
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblDefensivePostureInstructions = new JLabel("lblDefensivePostureInstructions.Text");
        getContentPane().add(lblDefensivePostureInstructions, gbc);
        
        gbc.gridy++;
        availableInfantryUnits =
                addIndividualUnitSelector(StratconRulesManager.getEligibleDefensiveUnits(campaign), gbc, getCommanderTactics());
        
        
        
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel lblDefensiveMinefieldCount = new JLabel(String.format(resourceMap.getString("lblDefensiveMinefieldCount.text"), 
                getCommanderTactics()));
        
        availableInfantryUnits.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                availableInfantrySelectorChanged(e, lblDefensiveMinefieldCount);
            }
        });
        
        getContentPane().add(lblDefensiveMinefieldCount, gbc);
    }
    
    /**
     * Add an "available force list" to the given control
     * @param parent
     * @param gbc
     * @param forceTemplate
     * @return
     */
    private JList<Force> addAvailableForceList(JPanel parent, GridBagConstraints gbc, ScenarioForceTemplate forceTemplate) {
        JScrollPane forceListContainer = new JScrollPane();

        ScenarioWizardLanceModel lanceModel;
        
        // if we're waiting to assign primary forces, we can only do so from the current track 
        lanceModel = new ScenarioWizardLanceModel(campaign, StratconRulesManager.getAvailableForceIDs(forceTemplate, campaign));
        
        JList<Force> availableForceList = new JList<>();
        availableForceList.setModel(lanceModel);
        availableForceList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));
        
        forceListContainer.setViewportView(availableForceList);

        parent.add(forceListContainer, gbc);
        return availableForceList;
    }

    /**
     * Adds an individual unit selector, given a list of individual units, a global grid bag constraint set
     * and a maximum selection size.
     * @param units The list of units to use as data source.
     * @param gbc Gridbag constraints to indicate where the control will go
     * @param maxSelectionSize Maximum number of units that can be selected
     */
    private JList<Unit> addIndividualUnitSelector(List<Unit> units, GridBagConstraints gbc, int maxSelectionSize) {
        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.WEST;
        JLabel instructions = new JLabel();
        instructions.setText(String.format(resourceMap.getString("lblSelectIndividualUnits.text"), maxSelectionSize));
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
        
        JList<Unit> availableUnits = new JList<>();
        availableUnits.setModel(availableModel);
        availableUnits.setCellRenderer(new ScenarioWizardUnitRenderer(campaign));
        availableUnits.addListSelectionListener(new ListSelectionListener() { 
            @Override
            public void valueChanged(ListSelectionEvent e) {
                availableUnitSelectorChanged(e, unitSelectionLabel, unitStatusLabel, maxSelectionSize);
            }
        });
        
        JScrollPane infantryContainer = new JScrollPane();
        infantryContainer.setViewportView(availableUnits);
        localGbc.gridy++;
        unitPanel.add(infantryContainer, localGbc);
        
        // add the 'status display' control
        localGbc.gridx++;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        unitPanel.add(unitStatusLabel, localGbc);
        
        getContentPane().add(unitPanel, gbc);
        
        return availableUnits;
    }
    
    /**
     * Worker function that builds an "html-enabled" string indicating the brief status of a force
     * @param f
     * @return
     */
    private String buildForceStatus(Force f, boolean showForceCost) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(f.getFullName());
        sb.append(": ");
        if(showForceCost) {
            sb.append(buildForceCost(f));
        }
        sb.append("<br/>");
        
        for(UUID unitID : f.getUnits()) {
            Unit u = campaign.getUnit(unitID);
            sb.append(buildUnitStatus(u));
        }
        
        return sb.toString();
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
    
    private String buildForceCost(Force f) {
        StringBuilder costBuilder = new StringBuilder();
        costBuilder.append("(");
        
        switch(StratconRulesManager.getReinforcementType(f, currentTrackState, campaign)) {
        case SupportPoint:
            costBuilder.append("supportPoint.text");
            if(currentCampaignState.getSupportPoints() <= 0) {
                costBuilder.append(", ");
                if(currentCampaignState.getVictoryPoints() <= 1) {
                    costBuilder.append("<span color='red'>");
                }
                
                costBuilder.append("supportPointConvert.text");
                
                if(currentCampaignState.getVictoryPoints() <= 1) {
                    costBuilder.append("</span>");
                }
            }
            break;
        case ChainedScenario:
            costBuilder.append(String.format("fromChainedScenario.text %s <span color='red'>%d hostile units reinforcing</span>", 
                    "scenarioname", "unitsfromscenario[stratconscenarioid]"));
            break;
        case FightLance:
            costBuilder.append("lanceInFightRole.text");
            break;
        default:
            costBuilder.append("yikes");
            break;
        }
        
        costBuilder.append(")");
        return costBuilder.toString();
    }
    
    /**
     * Sets the navigation button
     * @param gbc
     */
    private void setNavigationButtons(GridBagConstraints gbc) {
        // you're on one of two screens:
        // the 'primary force selection' screen
        // the 'reinforcement selection' screen
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
        // go through all the force lists and add the selected forces to the scenario
        for(String templateID : availableForceLists.keySet()) {
            for(Force force : availableForceLists.get(templateID).getSelectedValuesList()) {
                currentScenario.addForce(force.getId(), templateID);
            }
        }
        
        for(String templateID : availableUnitLists.keySet()) {
            for(Unit unit : availableUnitLists.get(templateID).getSelectedValuesList()) {
                currentScenario.addUnit(unit.getId(), templateID);
            }
        }
        
        for(Unit unit : availableInfantryUnits.getSelectedValuesList()) {
            currentScenario.addUnit(unit.getId(), ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        }
        
        // scenarios that haven't had primary forces committed yet get those committed now
        // and the scenario gets published to the campaign and may be played immediately from the briefing room
        if(currentScenario.getCurrentState() == ScenarioState.UNRESOLVED) {
            currentScenario.commitPrimaryForces(campaign, currentCampaignState.getContract());
        }
        
        setVisible(false);
    }
    
    /**
     * Event handler for when the user makes a selection on the available force selector.
     * @param e The event fired. 
     */
    private void availableForceSelectorChanged(ListSelectionEvent e, JLabel forceStatusLabel, boolean reinforcements) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }

        JList<Force> sourceList = (JList<Force>) e.getSource();
        
        StringBuilder statusBuilder = new StringBuilder();
        StringBuilder costBuilder = new StringBuilder();
        statusBuilder.append("<html>");
        costBuilder.append("<html>");
        
        for(Force force : sourceList.getSelectedValuesList()) {
            statusBuilder.append(buildForceStatus(force, reinforcements));
        }
        
        statusBuilder.append("</html>");
        costBuilder.append("</html>");
        
        forceStatusLabel.setText(statusBuilder.toString());
        
        pack();
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
    
    /**
     * Specific event handler for logic related to available infantry units.
     * Updates the defensive minefield count
     * @param e
     */
    private void availableInfantrySelectorChanged(ListSelectionEvent e, JLabel defensiveMineCountLabel) {
        if(!(e.getSource() instanceof JList<?>)) {
            return;
        }
        
        JList<Unit> changedList = (JList<Unit>) e.getSource();
        defensiveMineCountLabel.setText(String.format(resourceMap.getString("lblDefensiveMinefieldCount.text"), 
                Math.max(0, getCommanderTactics() - changedList.getSelectedIndices().length)));        
    }
}