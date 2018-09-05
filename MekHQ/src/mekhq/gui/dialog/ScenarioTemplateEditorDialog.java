package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioTemplate;

/**
 * Handles editing, saving and loading of scenario template definitions.
 * @author NickAragua
 *
 */
public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    
    /**
     * 
     */
    private static final long serialVersionUID = 9179434871199751998L;

    private final Dimension spinnerSize = new Dimension(55, 25);
    
    private final static String ADD_FORCE_COMMAND = "ADDFORCE"; 
    private final static String REMOVE_FORCE_COMMAND = "REMOVE_FORCE_";
    private final static String SAVE_TEMPLATE_COMMAND = "SAVE_TEMPLATE";
    private final static String LOAD_TEMPLATE_COMMAND = "LOAD_TEMPLATE";
    
    // controls which need to be accessible across the lifetime of this dialog
    JComboBox<String> cboAlignment;
    JComboBox<String> cboGenerationMethod;
    JSpinner spnMultiplier;
    JList<String> lstDeployZones;
    JComboBox<String> cboDestinationZone;
    JSpinner spnRetreatThreshold;
    JList<String> lstUnitTypes;
    JCheckBox chkReinforce;
    JCheckBox chkContributesToBV;
    JCheckBox chkContributesToUnitCount;
    JTextField txtGroupID;
    JPanel panForceList;
    JTextField txtScenarioName;
    JTextArea txtScenarioBriefing;
    JTextArea txtLongBriefing;
    JTextField txtBaseWidth;    
    JList<String> lstAllowedTerrainTypes;
    JTextField txtBaseHeight;
    JTextField txtXIncrement;
    JTextField txtYIncrement;
    JCheckBox chkAllowRotation;
    JCheckBox chkUseAtBSizing;
    
    JPanel globalPanel;
    
    JPanel forcedPanel;
    JScrollPane forceScrollPane;
    
    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate(); 
    
    /**
     * Constructor. Creates a new instance of this dialog with the given parent JFrame.
     * @param parent
     */
    public ScenarioTemplateEditorDialog(Frame parent) {
        super(parent, true);
        initComponents();
        pack();
        validate();
    }
    
    /**
     * Initialize dialog components.
     */
    protected void initComponents() {
        this.setTitle("Scenario Template Editor");
        getContentPane().setLayout(new GridBagLayout());
        
        globalPanel = new JPanel();
        globalPanel.setLayout(new GridBagLayout());
        
        JScrollPane globalScrollPane = new JScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(globalScrollPane);        
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupTopFluff(gbc);
        setupForceEditorHeaders(gbc);
        setupForceEditor(gbc);
        initializeForceList(gbc);
        setupMapParameters(gbc);
        setupBottomButtons(gbc);
        
        forceAlignmentChangeHandler();
    }
    
    /**
     * Sets up text entry boxes in the top - briefing, scenario name, labels.
     * @param gbc
     */
    private void setupTopFluff(GridBagConstraints gbc) {
        JLabel lblScenarioName = new JLabel("Scenario Name:");

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        globalPanel.add(lblScenarioName, gbc);
        
        txtScenarioName = new JTextField(80);
        txtScenarioName.setText(scenarioTemplate.name);
        gbc.gridy++;
        globalPanel.add(txtScenarioName, gbc);
        
        JLabel lblScenarioBriefing = new JLabel("Short Briefing:");
        gbc.gridy++;
        globalPanel.add(lblScenarioBriefing, gbc);
        
        txtScenarioBriefing = new JTextArea(3, 80);
        txtScenarioBriefing.setEditable(true);
        txtScenarioBriefing.setLineWrap(true);
        txtScenarioBriefing.setText(scenarioTemplate.shortBriefing);
        JScrollPane scrScenarioBriefing = new JScrollPane(txtScenarioBriefing);
        gbc.gridy++;
        globalPanel.add(scrScenarioBriefing, gbc);
        
        JLabel lblLongBriefing = new JLabel("Detailed Briefing:");
        gbc.gridy++;
        globalPanel.add(lblLongBriefing, gbc);
        
        txtLongBriefing = new JTextArea(5, 80);
        txtLongBriefing.setEditable(true);
        txtLongBriefing.setLineWrap(true);
        txtLongBriefing.setText(scenarioTemplate.detailedBriefing);
        JScrollPane scrLongBriefing = new JScrollPane(txtLongBriefing);
        gbc.gridy++;
        globalPanel.add(scrLongBriefing, gbc);
    }
    
    private void setupForceGroupEditorHeaders(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        
        JLabel lblForces = new JLabel("Force Groups:");
        gbc.gridy++;
        globalPanel.add(lblForces, gbc);
        
        JButton btnHideShow = new JButton("Hide/Show");
        btnHideShow.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleForcePanelVisibility();
            }
            
        });        
        
        gbc.gridx++;
        int previousAnchor = gbc.anchor;
        gbc.anchor = GridBagConstraints.EAST;
        globalPanel.add(btnHideShow, gbc);
        gbc.anchor = previousAnchor;
    }
    
    private void setupForceGroupEditor(GridBagConstraints gbc) {
        
    }
    
    /**
     * Worker function that sets up top-level headers for the force template editor section.
     * @param gbc
     */
    private void setupForceEditorHeaders(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        
        JLabel lblForces = new JLabel("Participating Forces:");
        gbc.gridy++;
        globalPanel.add(lblForces, gbc);
        
        JButton btnHideShow = new JButton("Hide/Show");
        btnHideShow.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleForcePanelVisibility();
            }
            
        });        
        
        gbc.gridx++;
        int previousAnchor = gbc.anchor;
        gbc.anchor = GridBagConstraints.EAST;
        globalPanel.add(btnHideShow, gbc);
        gbc.anchor = previousAnchor;
    }
  
    /**
     * Worker function that sets up UI elements for the force template editor.
     * Really goddamn ugly for now.
     * @param gbc
     */
    private void setupForceEditor(GridBagConstraints externalGBC) {
        forcedPanel = new JPanel();
        forcedPanel.setLayout(new GridBagLayout());
        forcedPanel.setBorder(new LineBorder(Color.BLACK));
        externalGBC.gridx = 0;
        externalGBC.gridy++;
        externalGBC.gridwidth = GridBagConstraints.REMAINDER;
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lblForceAlignment = new JLabel("Force Alignment:");
        forcedPanel.add(lblForceAlignment, gbc);
        
        ItemListener dropdownChangeListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                forceAlignmentChangeHandler();
            }
            
        };
        
        cboAlignment = new JComboBox<String>(ScenarioForceTemplate.FORCE_ALIGNMENTS);
        cboAlignment.addItemListener(dropdownChangeListener);
        gbc.gridx = 1;
        forcedPanel.add(cboAlignment, gbc);
        
        JLabel lblGenerationMethod = new JLabel("Generation Method:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblGenerationMethod, gbc);
        
        cboGenerationMethod = new JComboBox<String>(ScenarioForceTemplate.FORCE_GENERATION_METHODS);
        cboGenerationMethod.addItemListener(dropdownChangeListener);
        gbc.gridx = 1;
        forcedPanel.add(cboGenerationMethod, gbc);
        
        JLabel lblMultiplier = new JLabel("Scaling Multiplier:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblMultiplier, gbc);
        
        spnMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 2.0, .05));
        spnMultiplier.setPreferredSize(spinnerSize);
        gbc.gridx = 1;
        forcedPanel.add(spnMultiplier, gbc);
        
        JLabel lblDestinationZones = new JLabel("Destination Zone:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblDestinationZones, gbc);
        
        cboDestinationZone = new JComboBox<String>(ScenarioForceTemplate.BOT_DESTINATION_ZONES);
        gbc.gridx = 1;
        forcedPanel.add(cboDestinationZone, gbc);
        
        JLabel lblRetreatThreshold = new JLabel("Retreat Threshold:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblRetreatThreshold, gbc);
        
        spnRetreatThreshold = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
        spnRetreatThreshold.setPreferredSize(spinnerSize);
        gbc.gridx = 1;
        forcedPanel.add(spnRetreatThreshold, gbc);
        
        JLabel lblCanReinforceLinked = new JLabel("Reinforce subsequent scenarios:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblCanReinforceLinked, gbc);
        
        chkReinforce = new JCheckBox();
        gbc.gridx = 1;
        forcedPanel.add(chkReinforce, gbc);
        
        JLabel lblContributesToBV = new JLabel("Contributes to BV:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblContributesToBV, gbc);
        
        chkContributesToBV = new JCheckBox();
        gbc.gridx = 1;
        forcedPanel.add(chkContributesToBV, gbc);
        
        JLabel lblContributesToUnitCount = new JLabel("Contributes to Unit Count:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblContributesToUnitCount, gbc);
        
        chkContributesToUnitCount = new JCheckBox();
        gbc.gridx = 1;
        forcedPanel.add(chkContributesToUnitCount, gbc);
        
        JLabel lblGroupID = new JLabel("Force Group ID:");
        lblGroupID.setToolTipText("The group to which this force belongs. Forces in the same group will deploy in the same zone.");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblGroupID, gbc);
        
        txtGroupID = new JTextField(2);
        gbc.gridx = 1;
        forcedPanel.add(txtGroupID, gbc);
        
        DefaultListModel<String> zoneModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s); 
        }
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        
        JLabel lblDeploymentZones = new JLabel("Possible Deployment Zones");
        forcedPanel.add(lblDeploymentZones, gbc);
        
        JLabel lblAllowedUnitTypes = new JLabel("Allowed Unit Types");
        gbc.gridx++;
        forcedPanel.add(lblAllowedUnitTypes, gbc);
        
        lstDeployZones = new JList<String>();
        lstDeployZones.setModel(zoneModel);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        forcedPanel.add(lstDeployZones, gbc);
        
        DefaultListModel<String> unitTypeModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.UNIT_TYPES) {
            unitTypeModel.addElement(s); 
        }
        
        lstUnitTypes = new JList<String>();
        lstUnitTypes.setModel(unitTypeModel);
        gbc.gridx++;
        forcedPanel.add(lstUnitTypes, gbc);
        
        JButton btnAdd = new JButton("Add");
        btnAdd.setActionCommand(ADD_FORCE_COMMAND);
        btnAdd.addActionListener(this);
        gbc.gridx++;
        forcedPanel.add(btnAdd, gbc);
        
        globalPanel.add(forcedPanel, externalGBC);
        externalGBC.gridheight = 1;
    }
        
    /**
     * Worker function called when initializing the dialog to place the force template list on the content pane.
     * @param gbc Grid bag constraints.
     */
    private void initializeForceList(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        panForceList = new JPanel(new GridBagLayout());

        renderForceList();
        
        forceScrollPane = new JScrollPane(panForceList);
        forceScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        forceScrollPane.setPreferredSize(new Dimension(800, 200));
        
        forceScrollPane.setVisible(false);
        
        globalPanel.add(forceScrollPane, gbc);
    }
    
    /**
     * Worker function called when initializing to place the map parameters.
     * @param gbc
     */
    private void setupMapParameters(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        
        JLabel lblMapParameters = new JLabel("Scenario Map Parameters");
        globalPanel.add(lblMapParameters, gbc);
        
        gbc.gridy++;
        gbc.gridwidth = 1;
        JLabel lblBaseWidth = new JLabel("Base Width:");
        globalPanel.add(lblBaseWidth, gbc);
        
        gbc.gridx++;
        txtBaseWidth = new JTextField(4);
        txtBaseWidth.setText(String.valueOf(scenarioTemplate.mapParameters.baseWidth));
        globalPanel.add(txtBaseWidth, gbc);
        
        gbc.gridx++;
        JLabel lblAllowedTerrainTypes = new JLabel("Allowed Map Types:");
        globalPanel.add(lblAllowedTerrainTypes, gbc);
        
        gbc.gridx++;
        gbc.gridheight = GridBagConstraints.RELATIVE;
        lstAllowedTerrainTypes = new JList<>();
        DefaultListModel<String> terrainTypeModel = new DefaultListModel<String>();
        for(String terrainType : AtBScenario.terrainTypes) {
            terrainTypeModel.addElement(terrainType);
        }
        lstAllowedTerrainTypes.setModel(terrainTypeModel);
        lstAllowedTerrainTypes.setSelectedIndices(scenarioTemplate.mapParameters.getAllowedTerrainTypeArray());        
        globalPanel.add(lstAllowedTerrainTypes, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridheight = 1;
        JLabel lblBaseHeight = new JLabel("Base Height:");
        globalPanel.add(lblBaseHeight, gbc);
        
        gbc.gridx++;
        txtBaseHeight = new JTextField(4);
        txtBaseHeight.setText(String.valueOf(scenarioTemplate.mapParameters.baseHeight));
        globalPanel.add(txtBaseHeight, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lblXIncrement = new JLabel("Scaled Width Increment:");
        globalPanel.add(lblXIncrement, gbc);
        
        gbc.gridx++;
        txtXIncrement = new JTextField(4);
        txtXIncrement.setText(String.valueOf(scenarioTemplate.mapParameters.widthScalingIncrement));
        globalPanel.add(txtXIncrement, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lblYIncrement = new JLabel("Scaled Height Increment:");
        globalPanel.add(lblYIncrement, gbc);
        
        gbc.gridx++;
        txtYIncrement = new JTextField(4);
        txtYIncrement.setText(String.valueOf(scenarioTemplate.mapParameters.heightScalingIncrement));
        globalPanel.add(txtYIncrement, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lblAllowRotation = new JLabel("Allow 90 Degree Rotation:");
        globalPanel.add(lblAllowRotation, gbc);
        
        gbc.gridx++;
        chkAllowRotation = new JCheckBox();
        chkAllowRotation.setSelected(scenarioTemplate.mapParameters.allowRotation);
        globalPanel.add(chkAllowRotation, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lblUseAtBSizing = new JLabel("Use AtB Base Dimensions:");
        lblUseAtBSizing.setToolTipText("Use the AtB Map Sizes table to determine the base width and height of the map.");
        globalPanel.add(lblUseAtBSizing, gbc);
        
        gbc.gridx++;
        chkUseAtBSizing = new JCheckBox();
        chkUseAtBSizing.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                atbSizingCheckboxChangeHandler();
            }
            
        });
        chkUseAtBSizing.setSelected(scenarioTemplate.mapParameters.useStandardAtBSizing);
        globalPanel.add(chkUseAtBSizing, gbc);
    }
    
    /**
     * Worker function that sets up the buttons on the bottom of the dialog
     * @param gbc
     */
    private void setupBottomButtons(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy += 2;
        
        JButton btnSave = new JButton("Save");
        btnSave.setActionCommand(SAVE_TEMPLATE_COMMAND);
        btnSave.addActionListener(this);
        globalPanel.add(btnSave, gbc);
        
        gbc.gridx++;
        JButton btnLoad = new JButton("Load");
        btnLoad.setActionCommand(LOAD_TEMPLATE_COMMAND);
        btnLoad.addActionListener(this);
        globalPanel.add(btnLoad, gbc);
    }
    
    /**
     * Worker function to re-draw the force template list.
     */
    private void renderForceList() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.ipadx = 5;
        gbc.ipady = 5;
        
        panForceList.removeAll();
        
        if(forceScrollPane != null) {
            forceScrollPane.setVisible(!scenarioTemplate.scenarioForces.isEmpty());
        }
        
        for(int forceIndex = 0; forceIndex < scenarioTemplate.scenarioForces.size(); forceIndex++) {
            ScenarioForceTemplate sft = scenarioTemplate.scenarioForces.get(forceIndex);
            JLabel lblGroupID = new JLabel(sft.getForceGroupID());
            panForceList.add(lblGroupID, gbc);
            
            JLabel lblForceAlignment = new JLabel(ScenarioForceTemplate.FORCE_ALIGNMENTS[sft.getForceAlignment()]);
            gbc.gridx++;
            panForceList.add(lblForceAlignment, gbc);
            
            JLabel lblGenerationMethod = new JLabel(ScenarioForceTemplate.FORCE_GENERATION_METHODS[sft.getGenerationMethod()]);
            gbc.gridx++;
            panForceList.add(lblGenerationMethod, gbc);
            
            JLabel lblMultiplier = new JLabel(((Double) sft.getForceMultiplier()).toString());
            gbc.gridx++;
            panForceList.add(lblMultiplier, gbc);
            
            JLabel lblDeploymentZones = new JLabel();
            StringBuilder dzBuilder = new StringBuilder();
            dzBuilder.append("<html>");
            for(int zone : sft.getDeploymentZones()) {
                dzBuilder.append(ScenarioForceTemplate.DEPLOYMENT_ZONES[zone]);
                dzBuilder.append("<br/>");
            }
            dzBuilder.append("</html>");      
            lblDeploymentZones.setText(dzBuilder.toString());
            gbc.gridx++;
            panForceList.add(lblDeploymentZones, gbc);
            
            JLabel lblDestinationZones = new JLabel(ScenarioForceTemplate.BOT_DESTINATION_ZONES[sft.getDestinationZone()]);
            gbc.gridx++;
            panForceList.add(lblDestinationZones, gbc);
            
            JLabel lblRetreatThreshold = new JLabel(((Double) sft.getRetreatThreshold()).toString());
            gbc.gridx++;
            panForceList.add(lblRetreatThreshold, gbc);
            
            JLabel lblAllowedUnitTypes = new JLabel();
            StringBuilder autBuilder = new StringBuilder();
            autBuilder.append("<html>");
            for(int unitType : sft.getAllowedUnitTypes()) {
                autBuilder.append(ScenarioForceTemplate.UNIT_TYPES[unitType]);
                autBuilder.append("<br/>");
            }
            autBuilder.append("</html>");
            lblAllowedUnitTypes.setText(autBuilder.toString());
            gbc.gridx++;
            panForceList.add(lblAllowedUnitTypes, gbc);
            
            JLabel lblReinforceLinked = new JLabel(sft.getCanReinforceLinked() ? "Can Reinforce" : "Cannot Reinforce");
            gbc.gridx++;
            panForceList.add(lblReinforceLinked, gbc);
            
            JLabel lblContributesToBV = new JLabel(sft.getContributesToBV() ? "Contributes to BV" : "No BV Contribution");
            gbc.gridx++;
            panForceList.add(lblContributesToBV, gbc);
            
            JLabel lblContributesToUnitCount = new JLabel(sft.getContributesToUnitCount() ? "Contributes to Unit Count" : "No Unit Count Contribution");
            gbc.gridx++;
            panForceList.add(lblContributesToUnitCount, gbc);
            
            JButton btnRemoveForce = new JButton("Remove");
            btnRemoveForce.setActionCommand(String.format("%s%s", REMOVE_FORCE_COMMAND, forceIndex));
            btnRemoveForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnRemoveForce, gbc);
            
            gbc.gridy++;
            gbc.gridx = 0;
        }
    }
    
    /** 
     * Event handler for when the Add force button is pressed.
     * Adds a new force with the currently selected parameters to the scenario template.
     */
    private void addForceButtonHandler() {
        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();
        
        List<Integer> deploymentZones = new ArrayList<>();
        for(int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }
        
        int destinationZone = cboDestinationZone.getSelectedIndex();
        double retreatThreshold = (int) spnRetreatThreshold.getValue() / 100.0;
        
        List<Integer> allowedUnitTypes = new ArrayList<>();
        for(int x : lstUnitTypes.getSelectedIndices()) {
            allowedUnitTypes.add(x);
        }
        
        ScenarioForceTemplate sft = new ScenarioForceTemplate(forceAlignment, generationMethod, forceMultiplier,
                deploymentZones, destinationZone, retreatThreshold, allowedUnitTypes);
        sft.setCanReinforceLinked(chkReinforce.isSelected());
        sft.setContributesToBV(chkContributesToBV.isSelected());
        sft.setContributesToUnitCount(chkContributesToUnitCount.isSelected());
        sft.setForceGroupID(txtGroupID.getText());
        scenarioTemplate.scenarioForces.add(sft);
        
        renderForceList();
        pack();
        repaint();
    }
    
    /**
     * Event handler for when the "Remove" button is pressed for a particular force template.
     * @param command The command string containing the index of the force to remove.
     */
    private void deleteForceButtonHandler(String command) {
        int forceIndex = Integer.parseInt(command.substring(REMOVE_FORCE_COMMAND.length()));
        scenarioTemplate.scenarioForces.remove(forceIndex);
        renderForceList();
        pack();
        repaint();
    }
    
    /**
     * Event handler for when the "Use AtB sizing" checkbox's state changes. Disables the base height/width
     * textboxes if it is selected, as their values are meaningless in that situation.
     */
    private void atbSizingCheckboxChangeHandler() {
        txtBaseWidth.setEnabled(!chkUseAtBSizing.isSelected());
        txtBaseHeight.setEnabled(!chkUseAtBSizing.isSelected());
    }
    
    /**
     * Event handler for when the force alignment/generation method is changed.
     * Enables or disables some controls based on whether or not the force is a player deployed force,
     * or an enemy force.
     */
    private void forceAlignmentChangeHandler() {
        boolean isPlayerForce = (cboAlignment.getSelectedItem() == ScenarioForceTemplate.FORCE_ALIGNMENTS[0]) &&
                (cboGenerationMethod.getSelectedItem() == ScenarioForceTemplate.FORCE_GENERATION_METHODS[0]);
        
        boolean isEnemyForce = (cboAlignment.getSelectedItem() == ScenarioForceTemplate.FORCE_ALIGNMENTS[2]) ||
                (cboAlignment.getSelectedItem() == ScenarioForceTemplate.FORCE_ALIGNMENTS[3]);
        
        spnMultiplier.setEnabled(!isPlayerForce);
        spnRetreatThreshold.setEnabled(!isPlayerForce);
        lstUnitTypes.setEnabled(!isPlayerForce);
        chkContributesToBV.setEnabled(!isEnemyForce);
        chkContributesToUnitCount.setEnabled(!isEnemyForce);
    }
    
    /**
     * Event handler for the "Save" button.
     */
    private void saveTemplateButtonHandler() {
        scenarioTemplate.name = txtScenarioName.getText();
        scenarioTemplate.shortBriefing = txtScenarioBriefing.getText();
        scenarioTemplate.detailedBriefing = txtLongBriefing.getText();
        
        scenarioTemplate.mapParameters.allowedTerrainTypes.clear();
        for(int terrainType : lstAllowedTerrainTypes.getSelectedIndices()) {
            scenarioTemplate.mapParameters.allowedTerrainTypes.add(terrainType);
        }
        scenarioTemplate.mapParameters.baseHeight = Integer.parseInt(txtBaseHeight.getText());
        scenarioTemplate.mapParameters.baseWidth = Integer.parseInt(txtBaseWidth.getText());
        scenarioTemplate.mapParameters.heightScalingIncrement = Integer.parseInt(txtYIncrement.getText());
        scenarioTemplate.mapParameters.widthScalingIncrement = Integer.parseInt(txtXIncrement.getText());
        scenarioTemplate.mapParameters.allowRotation = chkAllowRotation.isSelected();
        scenarioTemplate.mapParameters.useStandardAtBSizing = chkUseAtBSizing.isSelected();
        
        JFileChooser saveTemplateChooser = new JFileChooser("./");
        saveTemplateChooser.setDialogTitle("Save Scenario Template");
        int returnVal = saveTemplateChooser.showSaveDialog(this);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (saveTemplateChooser.getSelectedFile() == null)) {
            return;
        }

        File file = saveTemplateChooser.getSelectedFile();
        scenarioTemplate.Serialize(file);
    }
    
    private void loadTemplateButtonHandler() {
        JFileChooser loadTemplateChooser = new JFileChooser("./");
        loadTemplateChooser.setDialogTitle("Load Scenario Template");
        int returnVal = loadTemplateChooser.showOpenDialog(this);

        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (loadTemplateChooser.getSelectedFile() == null)) {
            return;
        }

        File file = loadTemplateChooser.getSelectedFile();
        scenarioTemplate = ScenarioTemplate.Deserialize(file);
        globalPanel.removeAll();
        initComponents();
        pack();
        validate();
    }

    /**
     * General event handler for button clicks on this dialog.
     * Examines the action command and invokes appropriate method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand() == ADD_FORCE_COMMAND) {
            addForceButtonHandler();
        } else if(e.getActionCommand().contains(REMOVE_FORCE_COMMAND)) {
            deleteForceButtonHandler(e.getActionCommand());
        } else if(e.getActionCommand() == SAVE_TEMPLATE_COMMAND) {
            saveTemplateButtonHandler();
        } else if(e.getActionCommand() == LOAD_TEMPLATE_COMMAND) {
            loadTemplateButtonHandler();
        }
    }
    
    public void toggleForcePanelVisibility() {
        forcedPanel.setVisible(!forcedPanel.isVisible());
        forceScrollPane.setVisible(!forceScrollPane.isVisible() && !scenarioTemplate.scenarioForces.isEmpty());
    }
}
