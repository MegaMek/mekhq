/*
 * ScenarioTemplateEditorDialog.java
 *
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ui.baseComponents.AbstractScrollablePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.stratcon.StratconBiomeManifest;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles editing, saving and loading of scenario template definitions.
 * @author NickAragua
 */
public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    // this maps indexes in the destination zone drop down to CardinalEdge enum values and special cases in the scenario force template
    private static Map<Integer, Integer> destinationZoneMapping;

    private final Dimension spinnerSize = new Dimension(55, 25);

    private final static String ADD_FORCE_COMMAND = "ADDFORCE";
    private final static String REMOVE_FORCE_COMMAND = "REMOVE_FORCE_";
    private final static String EDIT_FORCE_COMMAND = "EDIT_FORCE_";
    private final static String SAVE_TEMPLATE_COMMAND = "SAVE_TEMPLATE";
    private final static String LOAD_TEMPLATE_COMMAND = "LOAD_TEMPLATE";

    private final JFrame frame;

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
    JComboBox<String> cboMinWeightClass;
    JCheckBox chkContributesToMapSize;
    JSpinner spnGenerationOrder;
    JCheckBox chkAllowAeroBombs;
    JCheckBox chkUseArtillery;
    JSpinner spnStartingAltitude;
    JCheckBox chkOffBoard;

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
    JRadioButton btnAllowAllMapTypes;
    JRadioButton btnUseSpaceMap;
    JRadioButton btnUseSpecificMapTypes;
    JRadioButton btnUseLowAtmosphereMap;
    JComboBox<String> modifierBox;
    JList<String> selectedModifiersList;
    JList<ScenarioObjective> objectiveList;
    JScrollPane objectiveScrollPane;
    JButton btnRemoveObjective;
    JList<String> lstMuls;

    AbstractScrollablePanel globalPanel;

    JPanel forcedPanel;
    JScrollPane forceScrollPane;

    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate();

    static {
        destinationZoneMapping = new HashMap<>();
        destinationZoneMapping.put(0, CardinalEdge.NORTH.getIndex());
        destinationZoneMapping.put(1, CardinalEdge.EAST.getIndex());
        destinationZoneMapping.put(2, CardinalEdge.SOUTH.getIndex());
        destinationZoneMapping.put(3, CardinalEdge.WEST.getIndex());
        destinationZoneMapping.put(4, CardinalEdge.NEAREST.getIndex());
        destinationZoneMapping.put(5, CardinalEdge.NONE.getIndex());
        destinationZoneMapping.put(6, ScenarioForceTemplate.DESTINATION_EDGE_OPPOSITE_DEPLOYMENT);
        destinationZoneMapping.put(7, ScenarioForceTemplate.DESTINATION_EDGE_RANDOM);
    }

    /**
     * Constructor. Creates a new instance of this dialog with the given parent JFrame.
     * @param parent
     */
    public ScenarioTemplateEditorDialog(JFrame parent) {
        super(parent, true);
        frame = parent;
        initComponents();
        pack();
        validate();
        setUserPreferences();
    }

    /**
     * Initialize dialog components.
     */
    protected void initComponents() {
        this.setTitle("Scenario Template Editor");
        getContentPane().setLayout(new GridLayout());

        globalPanel = new DefaultMHQScrollablePanel(frame, "globalPanel", new GridBagLayout());

        JScrollPane globalScrollPane = new JScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        globalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(globalScrollPane);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupTopFluff(gbc);
        setupObjectiveEditUI(gbc);
        setupForceEditorHeaders(gbc);
        setupForceEditor(gbc);
        initializeForceList(gbc);
        setupMapParameters(gbc);
        setupBottomButtons(gbc);

        forceAlignmentChangeHandler();
        updateForceSyncList();
        renderForceList();
        updateObjectiveList();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ScenarioTemplateEditorDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
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

    private void setupObjectiveEditUI(GridBagConstraints gbc) {
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        JPanel pnlObjectiveEdit = new JPanel();
        pnlObjectiveEdit.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.insets = new Insets(0, 0, 0, 5);

        ScenarioTemplateEditorDialog parent = this;

        JButton btnAddEditObjective = new JButton("Add/Edit Objective");
        btnAddEditObjective.addActionListener(evt -> {
            ObjectiveEditPanel oep;
            if (objectiveList.getSelectedValue() != null) {
                oep = new ObjectiveEditPanel(scenarioTemplate, objectiveList.getSelectedValue(), parent);
            } else {
                oep = new ObjectiveEditPanel(scenarioTemplate, parent);
            }
            oep.setModal(true);
            oep.requestFocus();
            oep.setVisible(true);
        });

        objectiveList = new JList<>();
        objectiveList.addListSelectionListener(e -> btnRemoveObjective.setEnabled(!objectiveList.getSelectedValuesList().isEmpty()));
        objectiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectiveList.setVisibleRowCount(5);
        objectiveList.setFixedCellWidth(400);
        objectiveScrollPane = new JScrollPane();
        objectiveScrollPane.setViewportView(objectiveList);

        btnRemoveObjective = new JButton("Remove");
        btnRemoveObjective.addActionListener(e -> this.removeObjective());

        JLabel lblObjectives = new JLabel("Objectives:");

        localGbc.gridx = 0;
        localGbc.gridy = 0;
        pnlObjectiveEdit.add(lblObjectives, localGbc);

        localGbc.gridx = 1;
        localGbc.gridy = 1;
        localGbc.gridheight = GridBagConstraints.REMAINDER;
        pnlObjectiveEdit.add(objectiveScrollPane, localGbc);

        localGbc.gridx = 2;
        localGbc.gridy = 2;
        localGbc.gridheight = 1;
        pnlObjectiveEdit.add(btnAddEditObjective, localGbc);
        localGbc.gridy = 3;
        pnlObjectiveEdit.add(btnRemoveObjective, localGbc);


        gbc.gridx = 0;
        gbc.gridy++;

        globalPanel.add(pnlObjectiveEdit, gbc);
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
        btnHideShow.addActionListener(evt -> toggleForcePanelVisibility());

        gbc.gridx++;
        int previousAnchor = gbc.anchor;
        gbc.anchor = GridBagConstraints.WEST;
        globalPanel.add(btnHideShow, gbc);
        gbc.anchor = previousAnchor;
    }

    /**
     * Worker function that sets up UI elements for the force template editor.
     * @param externalGBC
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

        ItemListener dropdownChangeListener = evt -> forceAlignmentChangeHandler();

        cboAlignment = new JComboBox<>(ScenarioForceTemplate.FORCE_ALIGNMENTS);
        cboAlignment.addItemListener(dropdownChangeListener);
        gbc.gridx = 1;
        forcedPanel.add(cboAlignment, gbc);

        JLabel lblGenerationMethod = new JLabel("Generation Method:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblGenerationMethod, gbc);

        cboGenerationMethod = new JComboBox<>(ScenarioForceTemplate.FORCE_GENERATION_METHODS);
        cboGenerationMethod.addItemListener(dropdownChangeListener);
        gbc.gridx = 1;
        forcedPanel.add(cboGenerationMethod, gbc);

        JLabel lblMultiplier = new JLabel("Scaling Multiplier:");
        lblMultiplier.setToolTipText("For scaling force generation methods, multiplies the metric being scaled against.");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblMultiplier, gbc);

        spnMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 4.0, .05));
        spnMultiplier.setPreferredSize(spinnerSize);
        gbc.gridx = 1;
        forcedPanel.add(spnMultiplier, gbc);

        JLabel lblDestinationZones = new JLabel("Destination Zone:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblDestinationZones, gbc);

        cboDestinationZone = new JComboBox<>(ScenarioForceTemplate.BOT_DESTINATION_ZONES);
        cboDestinationZone.setSelectedIndex(CardinalEdge.NONE.getIndex());
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

        ItemListener syncDeploymentChangeListener = evt -> syncDeploymentChangeHandler();
        cboSyncDeploymentType.addItemListener(syncDeploymentChangeListener);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        forcedPanel.add(cboSyncDeploymentType, gbc);

        cboSyncForceName = new JComboBox<>();
        gbc.gridy++;
        gbc.gridx = 1;
        forcedPanel.add(cboSyncForceName, gbc);

        JLabel lblFixedMul = new JLabel("Fixed MUL:");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblFixedMul, gbc);

        lstMuls = new JList<>();
        DefaultListModel<String> mulModel = new DefaultListModel<>();
        JScrollPane scrMulList = new JScrollPane(lstMuls);
        File mulDir = new File(MHQConstants.STRATCON_MUL_FILES_DIRECTORY);

        if (mulDir.exists() && mulDir.isDirectory()) {
            for (String mul : mulDir.list((d, s) -> {
                        return s.toLowerCase().endsWith(".mul");
                      })) {
                mulModel.addElement(mul);
            }
        }

        lstMuls.setModel(mulModel);
        lstMuls.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gbc.gridx = 1;
        forcedPanel.add(scrMulList, gbc);

        DefaultListModel<String> zoneModel = new DefaultListModel<>();
        for (String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s);
        }

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblDeploymentZones = new JLabel("Possible Deployment Zones");
        forcedPanel.add(lblDeploymentZones, gbc);

        lstDeployZones = new JList<>();
        lstDeployZones.setModel(zoneModel);
        gbc.gridy = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        forcedPanel.add(lstDeployZones, gbc);

        JLabel lblAllowedUnitTypes = new JLabel("Unit Type:");
        lblAllowedUnitTypes.setToolTipText("The type of unit. If player-supplied, indicates a limitation on the type of unit the player can deploy.");
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        forcedPanel.add(lblAllowedUnitTypes, gbc);

        cboUnitType = new JComboBox<>();
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX));
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX));
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS));

        for (int unitTypeID = 0; unitTypeID < UnitType.SIZE; unitTypeID++) {
            cboUnitType.addItem(UnitType.getTypeDisplayableName(unitTypeID));
        }

        gbc.gridx++;
        forcedPanel.add(cboUnitType, gbc);

        ItemListener unitTypeChangeListener = evt -> unitTypeChangeHandler();
        cboUnitType.addItemListener(unitTypeChangeListener);

        JLabel lblArrivalTurn = new JLabel("Arrival Turn:");
        lblArrivalTurn.setToolTipText("The turn on which this force arrives. Enter -1 for staggered arrival, -2 for staggered arrival by lance, -3 for 'as reinforcements' arrival time.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblArrivalTurn, gbc);

        spnArrivalTurn = new JSpinner(new SpinnerNumberModel(0, ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnArrivalTurn, gbc);

        JLabel lblFixedUnitCount = new JLabel("Fixed Unit Count:");
        lblFixedUnitCount.setToolTipText("How many units in the force, if using the fixed unit count generation method. -1 indicates a lance, appropriate to the owner's faction.\n" +
                "If player-supplied, indicates an upper bound on the number of units the player can deploy.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblFixedUnitCount, gbc);

        spnFixedUnitCount = new JSpinner(new SpinnerNumberModel(0, -1, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnFixedUnitCount, gbc);

        JLabel lblMaxWeight= new JLabel("Max Weight:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblMaxWeight, gbc);

        cboMaxWeightClass = new JComboBox<>();
        for (int x = EntityWeightClass.WEIGHT_ULTRA_LIGHT; x <= EntityWeightClass.WEIGHT_ASSAULT; x++) {
            cboMaxWeightClass.addItem(EntityWeightClass.getClassName(x));
        }
        cboMaxWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_ASSAULT);
        gbc.gridx++;
        forcedPanel.add(cboMaxWeightClass, gbc);

        JLabel lblMinWeight= new JLabel("Min Weight:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblMinWeight, gbc);

        cboMinWeightClass = new JComboBox<>();
        for (int x = EntityWeightClass.WEIGHT_ULTRA_LIGHT; x <= EntityWeightClass.WEIGHT_ASSAULT; x++) {
            cboMinWeightClass.addItem(EntityWeightClass.getClassName(x));
        }
        cboMinWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_LIGHT);
        gbc.gridx++;
        forcedPanel.add(cboMinWeightClass, gbc);

        JLabel lblContributesToMapSize = new JLabel("Contributes to Map Size:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblContributesToMapSize, gbc);

        chkContributesToMapSize = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkContributesToMapSize, gbc);

        JLabel lblGenerationOrder = new JLabel("Generation Order:");
        lblGenerationOrder.setToolTipText("Controls when this force will be generated related to other forces. Higher numbers will be generated later.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblGenerationOrder, gbc);

        spnGenerationOrder = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnGenerationOrder, gbc);

        JLabel lblAllowAeroBombs = new JLabel("Allow Aero Bombs:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblAllowAeroBombs, gbc);

        chkAllowAeroBombs = new JCheckBox();
        chkAllowAeroBombs.setEnabled(false);
        gbc.gridx++;
        forcedPanel.add(chkAllowAeroBombs, gbc);

        JLabel lblStartingAltitude = new JLabel("Start Altitude:");
        lblStartingAltitude.setToolTipText("Starting elevation for VTOLs or altitude for aeros/ground units. Ignored in space. Use with caution as it may lead to splattering.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblStartingAltitude, gbc);

        spnStartingAltitude = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        gbc.gridx++;
        forcedPanel.add(spnStartingAltitude, gbc);

        JLabel lblUseArtillery = new JLabel("Is Artillery:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblUseArtillery, gbc);

        chkUseArtillery = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkUseArtillery, gbc);

        JLabel lblOffboard = new JLabel("Deploy Offboard:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblOffboard, gbc);

        chkOffBoard = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkOffBoard, gbc);

        JButton btnAdd = new JButton("Save");
        btnAdd.setActionCommand(ADD_FORCE_COMMAND);
        btnAdd.addActionListener(this);
        gbc.gridx++;
        forcedPanel.add(btnAdd, gbc);

        globalPanel.add(forcedPanel, externalGBC);
        externalGBC.gridheight = 1;
    }

    /**
     * Helper function that loads the given force template into the force editor interface.
     * @param forceTemplate The force template.
     */
    private void loadForce(ScenarioForceTemplate forceTemplate) {
        cboAlignment.setSelectedIndex(forceTemplate.getForceAlignment());
        cboGenerationMethod.setSelectedIndex(forceTemplate.getGenerationMethod());
        spnMultiplier.setValue(forceTemplate.getForceMultiplier());
        cboDestinationZone.setSelectedIndex(forceTemplate.getDestinationZone());
        spnRetreatThreshold.setValue(forceTemplate.getRetreatThreshold());
        chkReinforce.setSelected(forceTemplate.getCanReinforceLinked());
        chkContributesToBV.setSelected(forceTemplate.getContributesToBV());
        chkContributesToUnitCount.setSelected(forceTemplate.getContributesToUnitCount());
        txtForceName.setText(forceTemplate.getForceName());
        cboSyncDeploymentType.setSelectedIndex(forceTemplate.getSyncDeploymentType().ordinal());
        cboSyncForceName.setSelectedItem(forceTemplate.getSyncedForceName());
        lstMuls.setSelectedValue(forceTemplate.getFixedMul(), true);

        int[] deploymentZones = new int[forceTemplate.getDeploymentZones().size()];
        for (int x = 0; x < forceTemplate.getDeploymentZones().size(); x++) {
            deploymentZones[x] = forceTemplate.getDeploymentZones().get(x);
        }

        lstDeployZones.setSelectedIndices(deploymentZones);
        cboUnitType.setSelectedIndex(forceTemplate.getAllowedUnitType() + ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size());
        spnArrivalTurn.setValue(forceTemplate.getArrivalTurn());
        spnFixedUnitCount.setValue(forceTemplate.getFixedUnitCount());
        cboMaxWeightClass.setSelectedIndex(forceTemplate.getMaxWeightClass());
        cboMinWeightClass.setSelectedIndex(forceTemplate.getMinWeightClass());
        chkContributesToMapSize.setSelected(forceTemplate.getContributesToMapSize());
        spnGenerationOrder.setValue(forceTemplate.getGenerationOrder());
        chkAllowAeroBombs.setSelected(forceTemplate.getAllowAeroBombs());
        chkOffBoard.setSelected(forceTemplate.getDeployOffboard());
        spnStartingAltitude.setValue(forceTemplate.getStartingAltitude());
        chkUseArtillery.setSelected(forceTemplate.getUseArtillery());
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

        JPanel pnlMapParameters = new JPanel();
        pnlMapParameters.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.WEST;

        JLabel lblMapParameters = new JLabel("Scenario Map Parameters:");
        pnlMapParameters.add(lblMapParameters, localGbc);

        // the first two columns
        localGbc.gridy++;
        localGbc.gridwidth = 1;
        JLabel lblBaseWidth = new JLabel("Base Width:");
        pnlMapParameters.add(lblBaseWidth, localGbc);

        localGbc.gridx++;
        txtBaseWidth = new JTextField(4);
        txtBaseWidth.setText(String.valueOf(scenarioTemplate.mapParameters.getBaseWidth()));
        pnlMapParameters.add(txtBaseWidth, localGbc);

        localGbc.gridx = 0;
        localGbc.gridy++;
        JLabel lblBaseHeight = new JLabel("Base Height:");
        pnlMapParameters.add(lblBaseHeight, localGbc);

        localGbc.gridx++;
        txtBaseHeight = new JTextField(4);
        txtBaseHeight.setText(String.valueOf(scenarioTemplate.mapParameters.getBaseHeight()));
        pnlMapParameters.add(txtBaseHeight, localGbc);

        localGbc.gridy++;
        localGbc.gridx = 0;
        JLabel lblXIncrement = new JLabel("Scaled Width Increment:");
        pnlMapParameters.add(lblXIncrement, localGbc);

        localGbc.gridx++;
        txtXIncrement = new JTextField(4);
        txtXIncrement.setText(String.valueOf(scenarioTemplate.mapParameters.getWidthScalingIncrement()));
        pnlMapParameters.add(txtXIncrement, localGbc);

        localGbc.gridy++;
        localGbc.gridx = 0;
        JLabel lblYIncrement = new JLabel("Scaled Height Increment:");
        pnlMapParameters.add(lblYIncrement, localGbc);

        localGbc.gridx++;
        txtYIncrement = new JTextField(4);
        txtYIncrement.setText(String.valueOf(scenarioTemplate.mapParameters.getHeightScalingIncrement()));
        pnlMapParameters.add(txtYIncrement, localGbc);

        localGbc.gridy++;
        localGbc.gridx = 0;
        JLabel lblAllowRotation = new JLabel("Allow 90 Degree Rotation:");
        pnlMapParameters.add(lblAllowRotation, localGbc);

        localGbc.gridx++;
        chkAllowRotation = new JCheckBox();
        chkAllowRotation.setSelected(scenarioTemplate.mapParameters.isAllowRotation());
        pnlMapParameters.add(chkAllowRotation, localGbc);

        localGbc.gridy++;
        localGbc.gridx = 0;
        JLabel lblUseAtBSizing = new JLabel("Use AtB Base Dimensions:");
        lblUseAtBSizing.setToolTipText("Use the AtB Map Sizes table to determine the base width and height of the map.");
        pnlMapParameters.add(lblUseAtBSizing, localGbc);

        localGbc.gridx++;
        chkUseAtBSizing = new JCheckBox();
        chkUseAtBSizing.addItemListener(evt -> atbSizingCheckboxChangeHandler());
        chkUseAtBSizing.setSelected(scenarioTemplate.mapParameters.isUseStandardAtBSizing());
        pnlMapParameters.add(chkUseAtBSizing, localGbc);

        localGbc.gridy = 1;
        localGbc.gridx = 2;

        // the allowed map types columns
        JLabel lblAllowedTerrainTypes = new JLabel("Allowed Map Types:");
        pnlMapParameters.add(lblAllowedTerrainTypes, localGbc);

        localGbc.gridy++;
        btnAllowAllMapTypes = new JRadioButton();
        btnAllowAllMapTypes.setText("Any Ground Map");
        btnAllowAllMapTypes.addItemListener(evt -> mapTypeChangeHandler());
        pnlMapParameters.add(btnAllowAllMapTypes, localGbc);

        localGbc.gridy++;
        btnUseSpaceMap = new JRadioButton();
        btnUseSpaceMap.setText("Use Space Map");
        btnUseSpaceMap.addItemListener(evt -> mapTypeChangeHandler());
        pnlMapParameters.add(btnUseSpaceMap, localGbc);

        localGbc.gridy++;
        btnUseLowAtmosphereMap = new JRadioButton();
        btnUseLowAtmosphereMap.setText("Use Low Atmo Map");
        btnUseLowAtmosphereMap.addItemListener(evt -> mapTypeChangeHandler());
        pnlMapParameters.add(btnUseLowAtmosphereMap, localGbc);

        localGbc.gridy++;
        btnUseSpecificMapTypes = new JRadioButton();
        btnUseSpecificMapTypes.setText("Specific Map Types");
        btnUseSpecificMapTypes.addItemListener(evt -> mapTypeChangeHandler());
        pnlMapParameters.add(btnUseSpecificMapTypes, localGbc);

        ButtonGroup mapTypeGroup = new ButtonGroup();
        mapTypeGroup.add(btnAllowAllMapTypes);
        mapTypeGroup.add(btnUseSpaceMap);
        mapTypeGroup.add(btnUseLowAtmosphereMap);
        mapTypeGroup.add(btnUseSpecificMapTypes);

        if (scenarioTemplate.mapParameters.getMapLocation() != null) {
            switch (scenarioTemplate.mapParameters.getMapLocation()) {
                case AllGroundTerrain:
                    btnAllowAllMapTypes.setSelected(true);
                    break;
                case Space:
                    btnUseSpaceMap.setSelected(true);
                    break;
                case LowAtmosphere:
                    btnUseLowAtmosphereMap.setSelected(true);
                    break;
                case SpecificGroundTerrain:
                    btnUseSpecificMapTypes.setSelected(true);
                    break;
            }
        }

        localGbc.gridx++;
        localGbc.gridy = 1;
        localGbc.gridheight = GridBagConstraints.RELATIVE;
        lstAllowedTerrainTypes = new JList<>();
        DefaultListModel<String> terrainTypeModel = new DefaultListModel<>();
        Map<String, StratconBiomeManifest.MapTypeList> mapTypes = StratconBiomeManifest.getInstance().getBiomeMapTypes();
        List<String> keys = mapTypes.keySet().stream().sorted().collect(Collectors.toList());
        List<Integer> indexes = new ArrayList<>();
        int i = 0;
        for (String terrainType : keys) {
            terrainTypeModel.addElement(terrainType);
            if (scenarioTemplate.mapParameters.getAllowedTerrainType().contains(terrainType)) {
                indexes.add(i);
            }
            i++;
        }
        lstAllowedTerrainTypes.setModel(terrainTypeModel);
        lstAllowedTerrainTypes.setSelectedIndices(indexes.stream().mapToInt(Integer::intValue).toArray());
        mapTypeChangeHandler();
        pnlMapParameters.add(lstAllowedTerrainTypes, localGbc);

        // the fixed events columns
        localGbc.gridy = 1;
        localGbc.gridx++;
        localGbc.gridheight = 1;
        pnlMapParameters.add(new JLabel("Fixed Modifiers"), localGbc);

        localGbc.gridy++;
        modifierBox = new JComboBox<>();
        for (String modifierKey : AtBScenarioModifier.getOrderedModifierKeys()) {
            modifierBox.addItem(modifierKey);
        }
        pnlMapParameters.add(modifierBox, localGbc);

        localGbc.gridx++;
        JButton btnAddModifier = new JButton("Add");
        btnAddModifier.addActionListener(evt -> addModifierHandler());
        pnlMapParameters.add(btnAddModifier, localGbc);

        localGbc.gridx--;
        localGbc.gridy++;
        localGbc.gridheight = 3;

        selectedModifiersList = new JList<>();
        selectedModifiersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        reloadSelectedModifiers();

        JScrollPane modifierScrollPane = new JScrollPane(selectedModifiersList);
        modifierScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pnlMapParameters.add(modifierScrollPane, localGbc);

        localGbc.gridx++;
        JButton btnRemoveModifier = new JButton("Remove");
        btnRemoveModifier.addActionListener(evt -> removeModifierHandler());
        pnlMapParameters.add(btnRemoveModifier, localGbc);

        gbc.gridy++;
        globalPanel.add(pnlMapParameters, gbc);
    }

    /**
     * Worker function that sets up the buttons on the bottom of the dialog
     * @param gbc
     */
    private void setupBottomButtons(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;

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

        if (forceScrollPane != null) {
            forceScrollPane.setVisible(!scenarioTemplate.getScenarioForces().isEmpty());
        }

        gbc.gridy++;
        gbc.gridx = 0;
        //headers
        JLabel lblGenerationOrder = new JLabel("Order");
        lblGenerationOrder.setBorder(new LineBorder(Color.GRAY));
        panForceList.add(lblGenerationOrder, gbc);

        JLabel lblForceNameHeader = new JLabel("Force ID");
        gbc.gridx++;
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

        JLabel lblMultiplierHeader = new JLabel("<html>Multiplier /<br/> Unit Count</html>");
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

        JLabel lblWeightClassHeader = new JLabel("Max Wt Class");
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

        List<ScenarioForceTemplate> forceTemplateList = new ArrayList<>(scenarioTemplate.getAllScenarioForces());
        Collections.sort(forceTemplateList);

        for (ScenarioForceTemplate sft : forceTemplateList) {
            JLabel lblForceOrder = new JLabel(Integer.toString(sft.getGenerationOrder()));
            panForceList.add(lblForceOrder, gbc);

            JLabel lblForceName = new JLabel(sft.getForceName());
            gbc.gridx++;
            panForceList.add(lblForceName, gbc);

            JLabel lblForceAlignment = new JLabel(ScenarioForceTemplate.FORCE_ALIGNMENTS[sft.getForceAlignment()]);
            gbc.gridx++;
            panForceList.add(lblForceAlignment, gbc);

            JLabel lblGenerationMethod = new JLabel(ScenarioForceTemplate.FORCE_GENERATION_METHODS[sft.getGenerationMethod()]);
            gbc.gridx++;
            panForceList.add(lblGenerationMethod, gbc);

            JLabel lblMultiplier = new JLabel();
            gbc.gridx++;

            if (!sft.isPlayerForce() &&
                    (sft.getGenerationMethod() != ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {
                lblMultiplier.setText(((Double) sft.getForceMultiplier()).toString());
                panForceList.add(lblMultiplier, gbc);
            } else if (!sft.isPlayerForce() &&
                (sft.getGenerationMethod() == ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {

                if (sft.getFixedUnitCount() >= 0) {
                    lblMultiplier.setText(Integer.toString(sft.getFixedUnitCount()));
                } else {
                    lblMultiplier.setText("Lance");
                }
                panForceList.add(lblMultiplier, gbc);
            }

            JLabel lblDeploymentZones = new JLabel();
            StringBuilder dzBuilder = new StringBuilder();

            if (!sft.getDeploymentZones().isEmpty()) {
                dzBuilder.append("<html>");
                for (int zone : sft.getDeploymentZones()) {
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

            JLabel lblRetreatThreshold = new JLabel(Integer.toString(sft.getRetreatThreshold()));
            gbc.gridx++;
            panForceList.add(lblRetreatThreshold, gbc);

            JLabel lblAllowedUnitTypes = new JLabel(sft.getAllowedUnitTypeName());
            gbc.gridx++;
            if (!sft.isPlayerForce()) {
                panForceList.add(lblAllowedUnitTypes, gbc);
            }

            JLabel lblWeightClass = new JLabel(EntityWeightClass.getClassName(sft.getMaxWeightClass()));
            gbc.gridx++;
            if (!sft.isPlayerForce()) {
                panForceList.add(lblWeightClass, gbc);
            }

            JLabel lblArrivalTurn = new JLabel(sft.getArrivalTurn() < 0
                    ? ScenarioForceTemplate.SPECIAL_ARRIVAL_TURNS.get(sft.getArrivalTurn())
                    : Integer.toString(sft.getArrivalTurn()));
            gbc.gridx++;
            panForceList.add(lblArrivalTurn, gbc);

            JLabel lblReinforceLinked = new JLabel(sft.getCanReinforceLinked() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblReinforceLinked, gbc);

            JLabel lblContributesToBV = new JLabel(sft.getContributesToBV() ? "Yes" : "No");
            gbc.gridx++;
            if (!(sft.isEnemyBotForce() || (sft.getForceAlignment() == ForceAlignment.PlanetOwner.ordinal()))) {
                panForceList.add(lblContributesToBV, gbc);
            }

            JLabel lblContributesToUnitCount = new JLabel(sft.getContributesToUnitCount() ? "Yes" : "No");
            gbc.gridx++;
            if (!(sft.isEnemyBotForce() || (sft.getForceAlignment() == ForceAlignment.PlanetOwner.ordinal()))) {
                panForceList.add(lblContributesToUnitCount, gbc);
            }

            JLabel lblMapSize = new JLabel(sft.getContributesToMapSize() ? "Yes" : "No");
            gbc.gridx++;
            panForceList.add(lblMapSize, gbc);

            JButton btnRemoveForce = new JButton("Remove");
            btnRemoveForce.setActionCommand(String.format("%s%s", REMOVE_FORCE_COMMAND, sft.getForceName()));
            btnRemoveForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnRemoveForce, gbc);

            JButton btnEditForce = new JButton("Edit");
            btnEditForce.setActionCommand(String.format("%s%s", EDIT_FORCE_COMMAND, sft.getForceName()));
            btnEditForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnEditForce, gbc);

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

        if (!validationResult.isEmpty()) {
            JOptionPane.showMessageDialog(this, validationResult, "Invalid Force Configuration", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();

        List<Integer> deploymentZones = new ArrayList<>();
        for (int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }

        int destinationZone = destinationZoneMapping.get(cboDestinationZone.getSelectedIndex());
        int retreatThreshold = (int) spnRetreatThreshold.getValue();

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
        sft.setMinWeightClass(cboMinWeightClass.getSelectedIndex());
        sft.setGenerationOrder((int) spnGenerationOrder.getValue());
        sft.setAllowAeroBombs(chkAllowAeroBombs.isSelected());
        sft.setStartingAltitude((int) spnStartingAltitude.getValue());
        sft.setUseArtillery(chkUseArtillery.isSelected());
        sft.setDeployOffboard(chkOffBoard.isSelected());

        sft.setSyncDeploymentType(SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()]);

        sft.setFixedMul(lstMuls.getSelectedValue());

        // if we have picked "None" for synchronization, then set explicit deployment zones.
        // otherwise, set the synced force name
        if (sft.getSyncDeploymentType() != SynchronizedDeploymentType.None) {
            sft.setSyncedForceName(cboSyncForceName.getSelectedItem().toString());
        } else {
            sft.setDeploymentZones(deploymentZones);
        }

        scenarioTemplate.getScenarioForces().put(txtForceName.getText(), sft);

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

        if (SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()] == SynchronizedDeploymentType.None &&
                lstDeployZones.getSelectedIndices().length == 0) {
            valBuilder.append("Force needs to be synced or have explicit deployment zones");
        }

        if (txtForceName.getText().isBlank()) {
            if (valBuilder.length() > 0) {
                valBuilder.append("\n");
            }

            valBuilder.append("Force must have an ID.");
        }

        if ((cboAlignment.getSelectedIndex() != ForceAlignment.Player.ordinal()) &&
                (cboGenerationMethod.getSelectedIndex() == ForceGenerationMethod.PlayerSupplied.ordinal())) {
            if (valBuilder.length() > 0) {
                valBuilder.append("\n");
            }

            valBuilder.append("Bot-controlled forces cannot be player-supplied.");
        }

        if (chkOffBoard.isSelected() && !chkUseArtillery.isSelected()) {
            if (valBuilder.length() > 0) {
                valBuilder.append("\n");
            }

            valBuilder.append("Non-artillery units cannot be deployed off board.");
        }

        if (cboMinWeightClass.getSelectedIndex() > cboMaxWeightClass.getSelectedIndex()) {
            if (valBuilder.length() > 0) {
                valBuilder.append("\n");
            }

            valBuilder.append("Min weight class is greater than max weight class.");
        }

        /*if (scenarioTemplate.scenarioForces.containsKey(txtForceName.getText())) {
            if (valBuilder.length() > 0) {
                valBuilder.append("\n");
            }

            valBuilder.append("Force with this key already exists!");
        }*/

        return valBuilder.toString();
    }

    /**
     * Event handler for when the "Remove" button is pressed for a particular force template.
     * @param command The command string containing the index of the force to remove.
     */
    private void deleteForceButtonHandler(String command) {
        String forceIndex = command.substring(REMOVE_FORCE_COMMAND.length());
        scenarioTemplate.getScenarioForces().remove(forceIndex);

        updateForceSyncList();
        renderForceList();
        pack();
        repaint();
    }

    /**
     * Event handler for when the "Edit" button is pressed for a particular force template.
     * @param command The command string containing the index of the force to edit.
     */
    private void editForceButtonHandler(String command) {
        String forceIndex = command.substring(EDIT_FORCE_COMMAND.length());
        loadForce(scenarioTemplate.getScenarioForces().get(forceIndex));
    }

    /**
     * Worker method that updates the "Force Sync" list
     * and sets the relevant dropdowns to active or inactive
     */
    private void updateForceSyncList() {
        cboSyncForceName.removeAllItems();
        for (String forceID : scenarioTemplate.getScenarioForces().keySet()) {
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
     * Event handler for when the "allow all map types" checkbox's state changes. Disables/enables the
     * explicit map type selector.
     */
    private void mapTypeChangeHandler() {
        if (lstAllowedTerrainTypes != null) {
            lstAllowedTerrainTypes.setEnabled(btnUseSpecificMapTypes.isSelected());
        }
    }

    /**
     * Event handler for when the force alignment/generation method is changed.
     * Enables or disables some controls based on whether or not the force is a player deployed force,
     * or an enemy force.
     */
    private void forceAlignmentChangeHandler() {
        boolean isPlayerForce = Objects.equals(cboAlignment.getSelectedItem(), ScenarioForceTemplate.FORCE_ALIGNMENTS[0]) &&
                Objects.equals(cboGenerationMethod.getSelectedItem(), ScenarioForceTemplate.FORCE_GENERATION_METHODS[0]);

        boolean isEnemyForce = (cboAlignment.getSelectedIndex() == ScenarioForceTemplate.ForceAlignment.Opposing.ordinal()) ||
                (cboAlignment.getSelectedIndex() == ScenarioForceTemplate.ForceAlignment.Third.ordinal()) ||
                (cboAlignment.getSelectedIndex() == ScenarioForceTemplate.ForceAlignment.PlanetOwner.ordinal());

        spnMultiplier.setEnabled(!isPlayerForce);
        spnRetreatThreshold.setEnabled(!isPlayerForce);
        cboMaxWeightClass.setEnabled(!isPlayerForce);
        cboMinWeightClass.setEnabled(!isPlayerForce);
        chkContributesToBV.setEnabled(!isEnemyForce);
        chkContributesToBV.setSelected(!isEnemyForce);
        chkContributesToUnitCount.setEnabled(!isEnemyForce);
        chkContributesToUnitCount.setSelected(!isEnemyForce);
        chkContributesToMapSize.setSelected(true);

        spnMultiplier.setEnabled(cboGenerationMethod.getSelectedIndex() != ForceGenerationMethod.FixedUnitCount.ordinal());
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
        if (!lstDeployZones.isEnabled()) {
            lstDeployZones.clearSelection();
        }
    }

    /**
     * Event handler for when the unit type dropdown changes value.
     * Enables or disables the "allow aero bombs" UI element as appropriate.
     */
    private void unitTypeChangeHandler() {
        int selectedItem = cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();
        boolean isAero = selectedItem == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX ||
                        selectedItem == UnitType.CONV_FIGHTER ||
                        selectedItem == UnitType.AEROSPACEFIGHTER;

        chkAllowAeroBombs.setEnabled(isAero);
    }

    /**
     * Event handler for the "Save" button.
     */
    private void saveTemplateButtonHandler() {
        scenarioTemplate.name = txtScenarioName.getText();
        scenarioTemplate.shortBriefing = txtScenarioBriefing.getText();
        scenarioTemplate.detailedBriefing = txtLongBriefing.getText();

        scenarioTemplate.mapParameters.allowedTerrainTypes.clear();
        for (int index : lstAllowedTerrainTypes.getSelectedIndices()) {
            String terrainType = lstAllowedTerrainTypes.getModel().getElementAt(index);
            scenarioTemplate.mapParameters.allowedTerrainTypes.add(terrainType);
        }
        scenarioTemplate.mapParameters.setBaseHeight(Integer.parseInt(txtBaseHeight.getText()));
        scenarioTemplate.mapParameters.setBaseWidth(Integer.parseInt(txtBaseWidth.getText()));
        scenarioTemplate.mapParameters.setHeightScalingIncrement(Integer.parseInt(txtYIncrement.getText()));
        scenarioTemplate.mapParameters.setWidthScalingIncrement(Integer.parseInt(txtXIncrement.getText()));
        scenarioTemplate.mapParameters.setAllowRotation(chkAllowRotation.isSelected());
        scenarioTemplate.mapParameters.setUseStandardAtBSizing(chkUseAtBSizing.isSelected());

        if (btnAllowAllMapTypes.isSelected()) {
            scenarioTemplate.mapParameters.setMapLocation(ScenarioMapParameters.MapLocation.AllGroundTerrain);
        } else if (btnUseSpaceMap.isSelected()) {
            scenarioTemplate.mapParameters.setMapLocation(ScenarioMapParameters.MapLocation.Space);
        } else if (btnUseLowAtmosphereMap.isSelected()) {
            scenarioTemplate.mapParameters.setMapLocation(ScenarioMapParameters.MapLocation.LowAtmosphere);
        } else if (btnUseSpecificMapTypes.isSelected()) {
            scenarioTemplate.mapParameters.setMapLocation(ScenarioMapParameters.MapLocation.SpecificGroundTerrain);
        }

        File file = FileDialogs.saveScenarioTemplate((JFrame) getOwner(), scenarioTemplate).orElse(null);
        if (file != null) {
            scenarioTemplate.Serialize(file);
        }
    }

    /**
     * Event handler for when the load button is cleared. Invokes deserialization functionality
     * for user-selected file, then reloads all UI elements.
     */
    private void loadTemplateButtonHandler() {
        File file = FileDialogs.openScenarioTemplate((JFrame) getOwner()).orElse(null);
        if (file == null) {
            return;
        }

        scenarioTemplate = ScenarioTemplate.Deserialize(file);

        if (scenarioTemplate == null) {
            JOptionPane.showMessageDialog(this, "Error loading specified file. See log for details.", "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        getContentPane().removeAll();
        globalPanel.removeAll();
        initComponents();
        pack();
        validate();
        setUserPreferences();
    }

    /**
     * General event handler for button clicks on this dialog.
     * Examines the action command and invokes appropriate method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ADD_FORCE_COMMAND.equals(e.getActionCommand())) {
            addForceButtonHandler();
        } else if (e.getActionCommand().contains(REMOVE_FORCE_COMMAND)) {
            deleteForceButtonHandler(e.getActionCommand());
        } else if (e.getActionCommand().contains(EDIT_FORCE_COMMAND)) {
            editForceButtonHandler(e.getActionCommand());
        } else if (SAVE_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            saveTemplateButtonHandler();
        } else if (LOAD_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            loadTemplateButtonHandler();
        }
    }

    /**
     * Event handler for the "Add" button next to the fixed modifier dropdown.
     */
    public void addModifierHandler() {
        scenarioTemplate.scenarioModifiers.add(modifierBox.getSelectedItem().toString());
        reloadSelectedModifiers();
    }

    /**
     * Event handler for the "Remove" button next to the "selected modifiers" list.
     */
    public void removeModifierHandler() {
        for (String selectedModifier : selectedModifiersList.getSelectedValuesList()) {
            scenarioTemplate.scenarioModifiers.remove(selectedModifier);
        }
        reloadSelectedModifiers();
    }

    /**
     * Re-reads the data source for the "selected modifiers" list.
     */
    public void reloadSelectedModifiers() {
        DefaultListModel<String> selectedModifierModel = new DefaultListModel<>();
        for (String selectedModifier : scenarioTemplate.scenarioModifiers) {
            selectedModifierModel.addElement(selectedModifier);
        }
        selectedModifiersList.setModel(selectedModifierModel);
    }

    /**
     * Helper method that hides or reveals the force editor section.
     */
    private void toggleForcePanelVisibility() {
        forcedPanel.setVisible(!forcedPanel.isVisible());
        forceScrollPane.setVisible(!forceScrollPane.isVisible() && !scenarioTemplate.getScenarioForces().isEmpty());
    }

    private void removeObjective() {
        for (ScenarioObjective objective : objectiveList.getSelectedValuesList()) {
            scenarioTemplate.scenarioObjectives.remove(objective);
        }

        btnRemoveObjective.setEnabled(false);
        updateObjectiveList();
    }

    public void updateObjectiveList() {
        DefaultListModel<ScenarioObjective> objectiveModel = new DefaultListModel<>();
        for (ScenarioObjective currentObjective : scenarioTemplate.scenarioObjectives) {
            objectiveModel.addElement(currentObjective);
        }

        objectiveList.setModel(objectiveModel);

        validate();
        pack();
    }
}
