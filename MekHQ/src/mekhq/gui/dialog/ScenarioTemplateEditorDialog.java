package mekhq.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;
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
    JComboBox<String> cboUnitType;
    JCheckBox chkReinforce;
    JCheckBox chkContributesToBV;
    JCheckBox chkContributesToUnitCount;
    JTextField txtForceName;
    JComboBox<String> cboSyncForceName;
    JComboBox<String> cboSyncDeploymentType;
    JSpinner spnArrivalTurn;
    JSpinner spnFixedUnitCount;
    JComboBox<String> cboMaxWeightClass;
    JCheckBox chkContributesToMapSize;
    
    
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
    //JPanel forcgedPanel;
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
        getContentPane().setLayout(new GridLayout());
        
        globalPanel = new JPanel();
        globalPanel.setLayout(new GridBagLayout());
        
        JScrollPane globalScrollPane = new JScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        globalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
        updateForceSyncList();
        renderForceList();
        
        globalScrollPane.setPreferredSize(new Dimension((int) globalPanel.getPreferredSize().getWidth() + 10, (int) globalPanel.getPreferredSize().getHeight()));
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
        
        JLabel lblForceID = new JLabel("Force ID:");
        lblForceID.setToolTipText("The identifier for this force. Used to synchronize the properties of other forces to this one.");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblForceID, gbc);
        
        txtForceName = new JTextField(10);
        gbc.gridx = 1;
        forcedPanel.add(txtForceName, gbc);
        
        JLabel lblSyncDeployment = new JLabel("Synchronized Deployment:");
        lblSyncDeployment.setToolTipText("Whether or not, and how, to synchronize the deployment of this force with another.");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblSyncDeployment, gbc);
        
        cboSyncDeploymentType = new JComboBox<>(ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES);
        
        ItemListener syncDeploymentChangeListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                syncDeploymentChangeHandler();
            }
        };
        cboSyncDeploymentType.addItemListener(syncDeploymentChangeListener);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        forcedPanel.add(cboSyncDeploymentType, gbc);
        
        cboSyncForceName = new JComboBox<>();
        gbc.gridy++;
        gbc.gridx = 1;
        forcedPanel.add(cboSyncForceName, gbc);
        
        
        DefaultListModel<String> zoneModel = new DefaultListModel<String>();
        for(String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s); 
        }
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel lblDeploymentZones = new JLabel("Possible Deployment Zones");
        forcedPanel.add(lblDeploymentZones, gbc);
        
        lstDeployZones = new JList<String>();
        lstDeployZones.setModel(zoneModel);
        gbc.gridy = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        forcedPanel.add(lstDeployZones, gbc);
        
        JLabel lblAllowedUnitTypes = new JLabel("Unit Type:");
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        forcedPanel.add(lblAllowedUnitTypes, gbc);
        
        cboUnitType = new JComboBox<String>();
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX));
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS));
        
        for(int unitTypeID = 0; unitTypeID < UnitType.SIZE; unitTypeID++) {
            cboUnitType.addItem(UnitType.getTypeDisplayableName(unitTypeID));
        }

        gbc.gridx++;
        forcedPanel.add(cboUnitType, gbc);
        
        JLabel lblArrivalTurn = new JLabel("Arrival Turn:");
        lblArrivalTurn.setToolTipText("The turn on which this force arrives. Enter -1 for staggered arrival, -2 for staggered arrival by lance.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblArrivalTurn, gbc);
        
        spnArrivalTurn = new JSpinner(new SpinnerNumberModel(0, -2, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnArrivalTurn, gbc);
        
        JLabel lblFixedUnitCount = new JLabel("Fixed Unit Count:");
        lblFixedUnitCount.setToolTipText("How many units in the force, if using the fixed unit count generation method.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblFixedUnitCount, gbc);
        
        spnFixedUnitCount = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnFixedUnitCount, gbc);
        
        JLabel lblMaxWeight= new JLabel("Max Weight:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblMaxWeight, gbc);
        
        cboMaxWeightClass = new JComboBox<>();
        for(int x = EntityWeightClass.WEIGHT_ULTRA_LIGHT; x <= EntityWeightClass.WEIGHT_ASSAULT; x++) {
            cboMaxWeightClass.addItem(EntityWeightClass.getClassName(x));
        }
        cboMaxWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_ASSAULT);
        gbc.gridx++;
        forcedPanel.add(cboMaxWeightClass, gbc);
        
        JLabel lblContributesToMapSize = new JLabel("Contributes to Map Size");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblContributesToMapSize, gbc);
        
        chkContributesToMapSize = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkContributesToMapSize, gbc);
        
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
        
        gbc.gridy++;
        gbc.gridx = 0;
        //headers
        JLabel lblForceNameHeader = new JLabel("Force ID");
        lblForceNameHeader.setBorder(new LineBorder(Color.GRAY));
        panForceList.add(lblForceNameHeader, gbc);
        
        JLabel lblForceAlignmentHeader = new JLabel("Alignment");
        lblForceAlignmentHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblForceAlignmentHeader, gbc);
        
        JLabel lblGenerationMethodHeader = new JLabel("Generation");
        lblGenerationMethodHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblGenerationMethodHeader, gbc);
        
        JLabel lblMultiplierHeader = new JLabel("Multiplier");
        lblMultiplierHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblMultiplierHeader, gbc);
        
        JLabel lblDeploymentZonesHeader = new JLabel("Deployment");
        lblDeploymentZonesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblDeploymentZonesHeader, gbc);
        
        JLabel lblDestinationZonesHeader = new JLabel("Destination");
        lblDestinationZonesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblDestinationZonesHeader, gbc);
        
        JLabel lblRetreatThresholdHeader = new JLabel("Retreat %");
        lblRetreatThresholdHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblRetreatThresholdHeader, gbc);
        
        JLabel lblAllowedUnitTypesHeader = new JLabel("Unit Type");
        lblAllowedUnitTypesHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblAllowedUnitTypesHeader, gbc);
        
        JLabel lblWeightClassHeader = new JLabel("Weight Class");
        lblWeightClassHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblWeightClassHeader, gbc);
        
        JLabel lblArrivalTurnHeader = new JLabel("Arrival Turn");
        lblArrivalTurnHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblArrivalTurnHeader, gbc);
        
        JLabel lblReinforceLinkedHeader = new JLabel("Reinforce?");
        lblReinforceLinkedHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblReinforceLinkedHeader, gbc);
        
        JLabel lblContributesToBVHeader = new JLabel("+ BV?");
        lblContributesToBVHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblContributesToBVHeader, gbc);
        
        JLabel lblContributesToUnitCountHeader = new JLabel("+ Unit Count?");
        lblContributesToUnitCountHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblContributesToUnitCountHeader, gbc);
        
        JLabel lblMapSizeHeader = new JLabel("+ Mapsize?");
        lblMapSizeHeader.setBorder(new LineBorder(Color.GRAY));
        gbc.gridx++;
        panForceList.add(lblMapSizeHeader, gbc);
        
        gbc.gridy++;
        gbc.gridx = 0;
        
        for(String forceIndex : scenarioTemplate.scenarioForces.keySet()) {
            ScenarioForceTemplate sft = scenarioTemplate.scenarioForces.get(forceIndex);
            JLabel lblForceName = new JLabel(sft.getForceName());
            panForceList.add(lblForceName, gbc);
            
            JLabel lblForceAlignment = new JLabel(ScenarioForceTemplate.FORCE_ALIGNMENTS[sft.getForceAlignment()]);
            gbc.gridx++;
            panForceList.add(lblForceAlignment, gbc);
            
            JLabel lblGenerationMethod = new JLabel(ScenarioForceTemplate.FORCE_GENERATION_METHODS[sft.getGenerationMethod()]);
            gbc.gridx++;
            panForceList.add(lblGenerationMethod, gbc);
            
            JLabel lblMultiplier = new JLabel(((Double) sft.getForceMultiplier()).toString());
            gbc.gridx++;
            
            if(!sft.isPlayerForce() && 
                    (sft.getGenerationMethod() != ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {
                panForceList.add(lblMultiplier, gbc);
            }
            
            JLabel lblDeploymentZones = new JLabel();
            StringBuilder dzBuilder = new StringBuilder();
            
            if(!sft.getDeploymentZones().isEmpty()) {
                dzBuilder.append("<html>");
                for(int zone : sft.getDeploymentZones()) {
                    dzBuilder.append(ScenarioForceTemplate.DEPLOYMENT_ZONES[zone]);
                    dzBuilder.append("<br/>");
                }
                dzBuilder.append("</html>");
            } else {
                dzBuilder.append(ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES[sft.getSyncDeploymentType().ordinal()]);
                dzBuilder.append(" as ");
                dzBuilder.append(sft.getSyncedForceName());
            }
            
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
            if(sft.getAllowedUnitType() > UnitType.SIZE || sft.getAllowedUnitType() < 0) {
                lblAllowedUnitTypes.setText(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(sft.getAllowedUnitType()));
            } else {
                lblAllowedUnitTypes.setText(UnitType.getTypeDisplayableName(sft.getAllowedUnitType()));
            }
            gbc.gridx++;
            if(!sft.isPlayerForce()) {
                panForceList.add(lblAllowedUnitTypes, gbc);
            }
            
            JLabel lblWeightClass = new JLabel(EntityWeightClass.getClassName(sft.getMaxWeightClass()));
            gbc.gridx++;
            if(!sft.isPlayerForce()) {
                panForceList.add(lblWeightClass, gbc);
            }
            
            JLabel lblArrivalTurn = new JLabel(sft.getArrivalTurn() < 0 ? 
                    ScenarioForceTemplate.SPECIAL_ARRIVAL_TURNS.get(sft.getArrivalTurn()) : ((Integer) sft.getArrivalTurn()).toString());
            gbc.gridx++;
            panForceList.add(lblArrivalTurn, gbc);
            
            JLabel lblReinforceLinked = new JLabel(sft.getCanReinforceLinked() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblReinforceLinked, gbc);
            
            JLabel lblContributesToBV = new JLabel(sft.getContributesToBV() ? "Yes" : "No");
            gbc.gridx++;
            if(!sft.isEnemyBotForce()) {
                panForceList.add(lblContributesToBV, gbc);
            }
            
            JLabel lblContributesToUnitCount = new JLabel(sft.getContributesToUnitCount() ? "Yes" : "No");
            gbc.gridx++;
            if(!sft.isEnemyBotForce()) {
                panForceList.add(lblContributesToUnitCount, gbc);
            }
            
            JLabel lblMapSize = new JLabel(sft.getContributesToMapSize() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblMapSize, gbc);
            
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
        String validationResult = validateAddForce();
        
        if(!validationResult.isEmpty()) {
            JOptionPane.showMessageDialog(this, validationResult, "Invalid Force Configuration", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();
        
        List<Integer> deploymentZones = new ArrayList<>();
        for(int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }
        
        int destinationZone = cboDestinationZone.getSelectedIndex();
        double retreatThreshold = (int) spnRetreatThreshold.getValue() / 100.0;
        
        int allowedUnitType = cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();
        
        ScenarioForceTemplate sft = new ScenarioForceTemplate(forceAlignment, generationMethod, forceMultiplier,
                null, destinationZone, retreatThreshold, allowedUnitType);
        sft.setCanReinforceLinked(chkReinforce.isSelected());
        sft.setContributesToBV(chkContributesToBV.isSelected());
        sft.setContributesToUnitCount(chkContributesToUnitCount.isSelected());
        sft.setForceName(txtForceName.getText());
        sft.setArrivalTurn((int) spnArrivalTurn.getValue());
        sft.setFixedUnitCount((int) spnFixedUnitCount.getValue());
        sft.setContributesToMapSize(chkContributesToMapSize.isSelected());
        sft.setMaxWeightClass(cboMaxWeightClass.getSelectedIndex());
        
        sft.setSyncDeploymentType(SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()]);
        
        // if we have picked "None" for synchronization, then set explicit deployment zones. 
        // otherwise, set the synced force name
        if(sft.getSyncDeploymentType() != SynchronizedDeploymentType.None) {
            sft.setSyncedForceName(cboSyncForceName.getSelectedItem().toString());
        } else {
            sft.setDeploymentZones(deploymentZones);
        }
        
        scenarioTemplate.scenarioForces.put(txtForceName.getText(), sft);
        
        updateForceSyncList();
        syncDeploymentChangeHandler();
        renderForceList();
        pack();
        repaint();
    }
    
    /**
     * Function that performs validation when the 'Add' button is clicked for a force
     * and informs the user of any nonsense configuration they may have specified.
     * @return Validation message for display.
     */
    private String validateAddForce() {
        StringBuilder valBuilder = new StringBuilder();
        
        if(SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()] == SynchronizedDeploymentType.None &&
                lstDeployZones.getSelectedIndices().length == 0) {
            valBuilder.append("Force needs to be synced or have explicit deployment zones");
        }
        
        if(txtForceName.getText().trim().isEmpty()) {
            if(valBuilder.length() > 0) {
                valBuilder.append("\n");
            }
            
            valBuilder.append("Force must have an ID.");
        }
        
        if((cboAlignment.getSelectedIndex() != ForceAlignment.Player.ordinal()) &&
                (cboGenerationMethod.getSelectedIndex() == ForceGenerationMethod.PlayerSupplied.ordinal())) {
            if(valBuilder.length() > 0) {
                valBuilder.append("\n");
            }
            
            valBuilder.append("Bot-controlled forces cannot be player-supplied.");
        }
        
        if(scenarioTemplate.scenarioForces.containsKey(txtForceName.getText())) {
            if(valBuilder.length() > 0) {
                valBuilder.append("\n");
            }
            
            valBuilder.append("Force with this key already exists!");
        }
        
        return valBuilder.toString();
    }
    
    /**
     * Event handler for when the "Remove" button is pressed for a particular force template.
     * @param command The command string containing the index of the force to remove.
     */
    private void deleteForceButtonHandler(String command) {
        String forceIndex = command.substring(REMOVE_FORCE_COMMAND.length());
        scenarioTemplate.scenarioForces.remove(forceIndex);
        
        updateForceSyncList();
        renderForceList();
        pack();
        repaint();
    }
    
    /**
     * Worker method that updates the "Force Sync" list
     * and sets the relevant dropdowns to active or inactive
     */
    private void updateForceSyncList() {
        cboSyncForceName.removeAllItems();
        for(String forceID : scenarioTemplate.scenarioForces.keySet()) {
            cboSyncForceName.addItem(forceID);
        }
        
        boolean forcesAvailableToSync = cboSyncForceName.getItemCount() > 0;
        cboSyncForceName.setEnabled(forcesAvailableToSync);
        cboSyncDeploymentType.setEnabled(forcesAvailableToSync);
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
        cboMaxWeightClass.setEnabled(!isPlayerForce);
        cboUnitType.setEnabled(!isPlayerForce);
        chkContributesToBV.setEnabled(!isEnemyForce);
        chkContributesToBV.setSelected(!isEnemyForce);
        chkContributesToUnitCount.setEnabled(!isEnemyForce);
        chkContributesToUnitCount.setSelected(!isEnemyForce);
        chkContributesToMapSize.setSelected(true);
        
        spnFixedUnitCount.setEnabled(cboGenerationMethod.getSelectedIndex() == ForceGenerationMethod.FixedUnitCount.ordinal());
    }
    
    /**
     * Event handler for when the force sync dropdown changes value.
     * Enables or disables the "force to sync" and "deployment zone" UI elements as appropriate.
     */
    private void syncDeploymentChangeHandler() {
        SynchronizedDeploymentType syncDeploymentType = SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()];
        boolean syncForceDeployment = syncDeploymentType != SynchronizedDeploymentType.None;
        
        cboSyncForceName.setEnabled(syncForceDeployment);
        lstDeployZones.setEnabled(!syncForceDeployment);
        if(!lstDeployZones.isEnabled()) {
            lstDeployZones.clearSelection();
        }
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
    
    /**
     * Event handler for when the load button is cleared. Invokes deserialization functionality
     * for user-selected file, then reloads all UI elements.
     */
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
        
        if(scenarioTemplate == null) {
            JOptionPane.showMessageDialog(this, "Error loading specified file. See log for details.", "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        getContentPane().removeAll();
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
    
    /**
     * Helper method that hides or reveals the force editor section.
     */
    public void toggleForcePanelVisibility() {
        forcedPanel.setVisible(!forcedPanel.isVisible());
        forceScrollPane.setVisible(!forceScrollPane.isVisible() && !scenarioTemplate.scenarioForces.isEmpty());
    }
}
