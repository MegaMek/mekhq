/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.scenarioTemplateEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.LineBorder;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ui.panels.abstractPanels.AbstractScrollablePanel;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.digitalGM.stratCon.StratConBiomeManifest;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;

/**
 * Handles editing, saving and loading of scenario template definitions.
 *
 * @author NickAragua
 */
public class ScenarioTemplateEditorDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioTemplateEditorDialog.class);

    private final Dimension spinnerSize = new Dimension(55, 25);

    private final static String ADD_FORCE_COMMAND = "ADD_FORCE";
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
    JCheckBox chkSubjectToRandomRemoval;
    JCheckBox chkSyncRetreatThreshold;

    JPanel panForceList;
    TemplatePropertiesPanel templatePropertiesPanel;
    MapParametersPanel mapParametersPanel;
    ModifiersPanel modifiersPanel;
    ObjectivesPanel objectivesPanel;
    JList<String> listMULs;
    JList<String> lstObjectiveLinkedForces;
    JList<MissionRole> lstRolePicker;
    JList<String> lstRoleSets;
    DefaultListModel<String> roleSetsModel;

    AbstractScrollablePanel globalPanel;

    JPanel forcedPanel;
    JScrollPane forceScrollPane;

    // the scenario template we're working on
    ScenarioTemplate scenarioTemplate = new ScenarioTemplate();

    // the ID of the force currently loaded into the force editor via "Edit", or null when adding a new force. Used to
    // update a force in place (and clean up its old key on rename) rather than orphaning or overwriting entries.
    String editedForceId = null;

    /**
     * @param parent Creates a new instance of this dialog with the given parent JFrame.
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

        JScrollPane globalScrollPane = new FastJScrollPane(globalPanel);
        globalScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        globalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        getContentPane().add(globalScrollPane);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        setupTemplateProperties(gbc);
        setupObjectiveEditUI(gbc);
        setupForceEditorHeaders(gbc);
        setupForceEditor(gbc);
        initializeForceList(gbc);
        setupMapParameters(gbc);
        setupBottomButtons(gbc);

        forceAlignmentChangeHandler();
        updateForceSyncList();
        renderForceList();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ScenarioTemplateEditorDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    /**
     * Sets up text entry boxes in the top - briefing, scenario name, labels.
     *
     */
    private void setupTemplateProperties(GridBagConstraints gridBagConstraints) {
        templatePropertiesPanel = new TemplatePropertiesPanel();
        templatePropertiesPanel.load(scenarioTemplate);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        globalPanel.add(templatePropertiesPanel, gridBagConstraints);
        gridBagConstraints.gridy++;
    }

    private void setupObjectiveEditUI(GridBagConstraints gbc) {
        objectivesPanel = new ObjectivesPanel();
        objectivesPanel.load(scenarioTemplate);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        globalPanel.add(objectivesPanel, gbc);
    }

    /**
     * Worker function that sets up top-level headers for the force template editor section.
     *
     */
    private void setupForceEditorHeaders(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;

        JLabel lblForces = new JLabel("Participating Forces:");
        gridBagConstraints.gridy++;
        globalPanel.add(lblForces, gridBagConstraints);

        JButton btnHideShow = new JButton("Hide/Show");
        btnHideShow.addActionListener(evt -> toggleForcePanelVisibility());

        gridBagConstraints.gridx++;
        int previousAnchor = gridBagConstraints.anchor;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        globalPanel.add(btnHideShow, gridBagConstraints);
        gridBagConstraints.anchor = previousAnchor;
    }

    /**
     * Worker function that sets up UI elements for the force template editor.
     *
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
        lblForceID.setToolTipText(
              "The identifier for this force. Used to synchronize the properties of other forces to this one.");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblForceID, gbc);

        txtForceName = new JTextField(10);
        gbc.gridx = 1;
        forcedPanel.add(txtForceName, gbc);

        JLabel lblSyncDeployment = new JLabel("Synchronized Deployment:");
        lblSyncDeployment.setToolTipText(
              "Whether or not, and how, to synchronize the deployment of this force with another.");
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

        listMULs = new JList<>();
        DefaultListModel<String> mulModel = new DefaultListModel<>();
        JScrollPane scrMulList = new FastJScrollPane(listMULs);
        File mulDir = new File(MHQConstants.STRAT_CON_MUL_FILES_DIRECTORY);

        if (mulDir.exists() && mulDir.isDirectory()) {
            for (String mul : Objects.requireNonNull(mulDir.list((d, s) -> s.toLowerCase().endsWith(".mul")))) {
                mulModel.addElement(mul);
            }
        }

        listMULs.setModel(mulModel);
        listMULs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gbc.gridx = 1;
        forcedPanel.add(scrMulList, gbc);

        JLabel lblObjectiveLinkedForces = new JLabel("Objective-Linked Forces:");
        lblObjectiveLinkedForces.setToolTipText(
              "Other forces this force counts toward for objective purposes (e.g. an objective to destroy a share of the enemy).");
        gbc.gridx = 0;
        gbc.gridy++;
        forcedPanel.add(lblObjectiveLinkedForces, gbc);

        lstObjectiveLinkedForces = new JList<>();
        lstObjectiveLinkedForces.setModel(new DefaultListModel<>());
        lstObjectiveLinkedForces.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrObjectiveLinkedForces = new FastJScrollPane(lstObjectiveLinkedForces);
        gbc.gridx = 1;
        forcedPanel.add(scrObjectiveLinkedForces, gbc);

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
        lblAllowedUnitTypes.setToolTipText(
              "The type of unit. If player-supplied, indicates a limitation on the type of unit the player can deploy.");
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
        lblArrivalTurn.setToolTipText(
              "The turn on which this force arrives. Enter -1 for staggered arrival, -2 for staggered arrival by lance, -3 for 'as reinforcements' arrival time.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblArrivalTurn, gbc);

        spnArrivalTurn = new JSpinner(new SpinnerNumberModel(0,
              ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS,
              100,
              1));
        gbc.gridx++;
        forcedPanel.add(spnArrivalTurn, gbc);

        JLabel lblFixedUnitCount = new JLabel("Fixed Unit Count:");
        lblFixedUnitCount.setToolTipText(
              "How many units in the force, if using the fixed unit count generation method. -1 indicates a lance, appropriate to the owner's faction.\n" +
                    "If player-supplied, indicates an upper bound on the number of units the player can deploy.");
        gbc.gridy++;
        gbc.gridx--;
        forcedPanel.add(lblFixedUnitCount, gbc);

        spnFixedUnitCount = new JSpinner(new SpinnerNumberModel(0, -1, 100, 1));
        gbc.gridx++;
        forcedPanel.add(spnFixedUnitCount, gbc);

        JLabel lblMaxWeight = new JLabel("Max Weight:");
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

        JLabel lblMinWeight = new JLabel("Min Weight:");
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
        lblGenerationOrder.setToolTipText(
              "Controls when this force will be generated related to other forces. Higher numbers will be generated later.");
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
        lblStartingAltitude.setToolTipText(
              "Starting elevation for VTOLs or altitude for aeros/ground units. Ignored in space. Use with caution as it may lead to splattering.");
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

        JLabel lblOffBoard = new JLabel("Deploy Off board:");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblOffBoard, gbc);

        chkOffBoard = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkOffBoard, gbc);

        JLabel lblSubjectToRandomRemoval = new JLabel("Subject to Random Removal:");
        lblSubjectToRandomRemoval.setToolTipText(
              "Whether this force can lose units to modifiers that randomly remove them, e.g. \"Good Intel\".");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblSubjectToRandomRemoval, gbc);

        chkSubjectToRandomRemoval = new JCheckBox();
        chkSubjectToRandomRemoval.setSelected(true);
        gbc.gridx++;
        forcedPanel.add(chkSubjectToRandomRemoval, gbc);

        JLabel lblSyncRetreatThreshold = new JLabel("Sync Retreat Threshold:");
        lblSyncRetreatThreshold.setToolTipText(
              "Whether this force shares a retreat threshold with the force it is synchronized to.");
        gbc.gridx--;
        gbc.gridy++;
        forcedPanel.add(lblSyncRetreatThreshold, gbc);

        chkSyncRetreatThreshold = new JCheckBox();
        gbc.gridx++;
        forcedPanel.add(chkSyncRetreatThreshold, gbc);

        JButton btnAdd = new JButton("Save");
        btnAdd.setActionCommand(ADD_FORCE_COMMAND);
        btnAdd.addActionListener(this);
        gbc.gridx++;
        forcedPanel.add(btnAdd, gbc);

        GridBagConstraints roleGbc = new GridBagConstraints();
        roleGbc.gridx = 6;
        roleGbc.gridy = 0;
        roleGbc.gridheight = GridBagConstraints.REMAINDER;
        roleGbc.anchor = GridBagConstraints.NORTHWEST;
        roleGbc.insets = new Insets(0, 10, 0, 0);
        forcedPanel.add(buildRoleEditorPanel(), roleGbc);

        globalPanel.add(forcedPanel, externalGBC);
        externalGBC.gridheight = 1;
    }

    /**
     * Builds the role-choices editor column for the force editor. A force's roles are stored as a list of "role sets";
     * each set is a group of {@link MissionRole}s that apply together, and one set is picked at random when the force
     * is generated. The user selects one or more roles (filtered to those valid for the force's unit type) and adds
     * them as a set; the accumulated sets are shown in the lower list.
     */
    private JPanel buildRoleEditorPanel() {
        JPanel pnlRoles = new JPanel(new GridBagLayout());
        pnlRoles.setBorder(BorderFactory.createTitledBorder("Required Roles"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        pnlRoles.add(new JLabel("Roles valid for unit type:"), gbc);

        lstRolePicker = new JList<>();
        lstRolePicker.setModel(new DefaultListModel<>());
        lstRolePicker.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstRolePicker.setCellRenderer(new RoleDisplayRenderer());
        JScrollPane scrRolePicker = new FastJScrollPane(lstRolePicker);
        scrRolePicker.setPreferredSize(new Dimension(190, 120));
        gbc.gridy++;
        pnlRoles.add(scrRolePicker, gbc);

        JButton btnAddRoleSet = new JButton("Add Role Set");
        btnAddRoleSet.addActionListener(evt -> addRoleSetHandler());
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        pnlRoles.add(btnAddRoleSet, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;
        pnlRoles.add(new JLabel("Role sets (one chosen at random):"), gbc);

        roleSetsModel = new DefaultListModel<>();
        lstRoleSets = new JList<>(roleSetsModel);
        lstRoleSets.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstRoleSets.setCellRenderer(new RoleSetDisplayRenderer());
        JScrollPane scrRoleSets = new FastJScrollPane(lstRoleSets);
        scrRoleSets.setPreferredSize(new Dimension(190, 100));
        gbc.gridy++;
        pnlRoles.add(scrRoleSets, gbc);

        JButton btnRemoveRoleSet = new JButton("Remove Role Set");
        btnRemoveRoleSet.addActionListener(evt -> removeRoleSetHandler());
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        pnlRoles.add(btnRemoveRoleSet, gbc);

        refreshRolePicker();
        return pnlRoles;
    }

    /** Renders a {@link MissionRole} with underscores shown as spaces. */
    private static class RoleDisplayRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {
            String text = (value instanceof MissionRole role) ?
                                role.toString().replace('_', ' ') :
                                String.valueOf(value);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    /** Renders a stored role-set string using its human-readable role names. */
    private static class RoleSetDisplayRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {
            String text = (value instanceof String entry) ? MissionRoleChoices.describe(entry) : String.valueOf(value);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    /**
     * Helper function that loads the given force template into the force editor interface.
     *
     * @param forceTemplate The force template.
     */
    private void loadForce(ScenarioForceTemplate forceTemplate) {
        cboAlignment.setSelectedIndex(forceTemplate.getForceAlignment());
        cboGenerationMethod.setSelectedIndex(forceTemplate.getGenerationMethod());
        spnMultiplier.setValue(forceTemplate.getForceMultiplier());
        cboDestinationZone.setSelectedIndex(DestinationZoneMapper.storedZoneToComboIndex(forceTemplate.getDestinationZone()));
        spnRetreatThreshold.setValue(forceTemplate.getRetreatThreshold());
        chkReinforce.setSelected(forceTemplate.getCanReinforceLinked());
        chkContributesToBV.setSelected(forceTemplate.getContributesToBV());
        chkContributesToUnitCount.setSelected(forceTemplate.getContributesToUnitCount());
        txtForceName.setText(forceTemplate.getForceName());
        cboSyncDeploymentType.setSelectedIndex(forceTemplate.getSyncDeploymentType().ordinal());
        cboSyncForceName.setSelectedItem(forceTemplate.getSyncedForceName());
        listMULs.setSelectedValue(forceTemplate.getFixedMul(), true);

        int[] deploymentZones = new int[forceTemplate.getDeploymentZones().size()];
        for (int x = 0; x < forceTemplate.getDeploymentZones().size(); x++) {
            deploymentZones[x] = forceTemplate.getDeploymentZones().get(x);
        }

        lstDeployZones.setSelectedIndices(deploymentZones);
        cboUnitType.setSelectedIndex(forceTemplate.getAllowedUnitType() +
                                           ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size());
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
        chkSubjectToRandomRemoval.setSelected(forceTemplate.isSubjectToRandomRemoval());
        chkSyncRetreatThreshold.setSelected(forceTemplate.getSyncRetreatThreshold());
        selectObjectiveLinkedForces(forceTemplate.getObjectiveLinkedForces());

        roleSetsModel.clear();
        for (String roleSet : forceTemplate.getRoleCollections()) {
            roleSetsModel.addElement(roleSet);
        }
    }

    /**
     * Selects the entries in the objective-linked forces list that match the given force IDs. Any listed ID no longer
     * present in the roster is simply skipped.
     */
    private void selectObjectiveLinkedForces(List<String> linkedForceIds) {
        ListModel<String> model = lstObjectiveLinkedForces.getModel();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            if (linkedForceIds.contains(model.getElementAt(i))) {
                indices.add(i);
            }
        }
        lstObjectiveLinkedForces.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Worker function called when initializing the dialog to place the force template list on the content pane.
     *
     * @param gbc Grid bag constraints.
     */
    private void initializeForceList(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        panForceList = new JPanel(new GridBagLayout());

        renderForceList();

        forceScrollPane = new FastJScrollPane(panForceList);
        forceScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        forceScrollPane.setVisible(false);

        globalPanel.add(forceScrollPane, gbc);
    }

    /**
     * Worker function called when initializing to place the map parameters.
     *
     */
    private void setupMapParameters(GridBagConstraints gridBagConstraints) {
        List<String> terrainTypeKeys = StratConBiomeManifest.getInstance()
                                             .getBiomeMapTypes()
                                             .keySet()
                                             .stream()
                                             .sorted()
                                             .toList();
        mapParametersPanel = new MapParametersPanel(terrainTypeKeys);
        mapParametersPanel.load(scenarioTemplate.mapParameters);

        JPanel pnlMapRow = new JPanel(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        pnlMapRow.add(mapParametersPanel, localGbc);

        modifiersPanel = new ModifiersPanel(AtBScenarioModifier.getOrderedModifierKeys());
        modifiersPanel.load(scenarioTemplate.scenarioModifiers);
        localGbc.gridx = 1;
        localGbc.insets = new Insets(0, 15, 0, 0);
        pnlMapRow.add(modifiersPanel, localGbc);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        globalPanel.add(pnlMapRow, gridBagConstraints);
    }

    /**
     * Worker function that sets up the buttons on the bottom of the dialog
     *
     */
    private void setupBottomButtons(GridBagConstraints gridBagConstraints) {
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;

        JButton btnSave = new JButton("Save");
        btnSave.setActionCommand(SAVE_TEMPLATE_COMMAND);
        btnSave.addActionListener(this);
        globalPanel.add(btnSave, gridBagConstraints);

        gridBagConstraints.gridx++;
        JButton btnLoad = new JButton("Load");
        btnLoad.setActionCommand(LOAD_TEMPLATE_COMMAND);
        btnLoad.addActionListener(this);
        globalPanel.add(btnLoad, gridBagConstraints);
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
        // headers
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

        JLabel lblMapSizeHeader = new JLabel("+ Map size?");
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
                      (sft.getGenerationMethod() !=
                             ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {
                lblMultiplier.setText(((Double) sft.getForceMultiplier()).toString());
                panForceList.add(lblMultiplier, gbc);
            } else if (!sft.isPlayerForce() &&
                             (sft.getGenerationMethod() ==
                                    ScenarioForceTemplate.ForceGenerationMethod.FixedUnitCount.ordinal())) {

                if (sft.getFixedUnitCount() >= 0) {
                    lblMultiplier.setText(Integer.toString(sft.getFixedUnitCount()));
                } else {
                    lblMultiplier.setText("Lance");
                }
                panForceList.add(lblMultiplier, gbc);
            }

            JLabel lblDeploymentZones = getLblDeploymentZones(sft);
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

            JLabel lblArrivalTurn = new JLabel(sft.getArrivalTurn() < 0 ?
                                                     ScenarioForceTemplate.SPECIAL_ARRIVAL_TURNS.get(sft.getArrivalTurn()) :
                                                     Integer.toString(sft.getArrivalTurn()));
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
            btnRemoveForce.setActionCommand(ForceListCommand.removeCommand(sft.getForceName()));
            btnRemoveForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnRemoveForce, gbc);

            JButton btnEditForce = new JButton("Edit");
            btnEditForce.setActionCommand(ForceListCommand.editCommand(sft.getForceName()));
            btnEditForce.addActionListener(this);
            gbc.gridx++;
            panForceList.add(btnEditForce, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
        }
    }

    private static JLabel getLblDeploymentZones(ScenarioForceTemplate sft) {
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
            dzBuilder.append(ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES[sft.getSyncDeploymentType()
                                                                                     .ordinal()]);
            dzBuilder.append(" as ");
            dzBuilder.append(sft.getSyncedForceName());
        }

        lblDeploymentZones.setText(dzBuilder.toString());
        return lblDeploymentZones;
    }

    /**
     * Event handler for when the Add force button is pressed. Adds a new force with the currently selected parameters
     * to the scenario template.
     */
    private void addForceButtonHandler() {
        String validationResult = validateAddForce();

        if (!validationResult.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  validationResult,
                  "Invalid Force Configuration",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();

        List<Integer> deploymentZones = new ArrayList<>();
        for (int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }

        int destinationZone = DestinationZoneMapper.comboIndexToStoredZone(cboDestinationZone.getSelectedIndex());
        int retreatThreshold = (int) spnRetreatThreshold.getValue();

        int allowedUnitType = cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();

        ScenarioForceTemplate sft = new ScenarioForceTemplate(forceAlignment,
              generationMethod,
              forceMultiplier,
              null,
              destinationZone,
              retreatThreshold,
              allowedUnitType);
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
        sft.setSubjectToRandomRemoval(chkSubjectToRandomRemoval.isSelected());
        sft.setSyncRetreatThreshold(chkSyncRetreatThreshold.isSelected());

        sft.setSyncDeploymentType(SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()]);

        sft.setFixedMul(listMULs.getSelectedValue());
        sft.setObjectiveLinkedForces(new ArrayList<>(lstObjectiveLinkedForces.getSelectedValuesList()));

        sft.getRoleCollections().clear();
        for (int i = 0; i < roleSetsModel.getSize(); i++) {
            sft.getRoleCollections().add(roleSetsModel.getElementAt(i));
        }

        // if we have picked "None" for synchronization, then set explicit deployment
        // zones.
        // otherwise, set the synced force name
        if (sft.getSyncDeploymentType() != SynchronizedDeploymentType.None) {
            sft.setSyncedForceName(Objects.requireNonNull(cboSyncForceName.getSelectedItem()).toString());
        } else {
            sft.setDeploymentZones(deploymentZones);
        }

        ForceRosterEditor.CommitResult commitResult = ForceRosterEditor.commit(scenarioTemplate.getScenarioForces(),
              editedForceId,
              sft);
        if (!commitResult.committed()) {
            JOptionPane.showMessageDialog(this,
                  commitResult.errorMessage(),
                  "Invalid Force Configuration",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Back to "add new" mode once the edit has been committed.
        editedForceId = null;

        updateForceSyncList();
        syncDeploymentChangeHandler();
        renderForceList();
        pack();
        repaint();
    }

    /**
     * Function that performs validation when the 'Add' button is clicked for a force and informs the user of any
     * nonsense configuration they may have specified.
     *
     * @return Validation message for display.
     */
    private String validateAddForce() {
        StringBuilder valBuilder = new StringBuilder();

        if (SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()] ==
                  SynchronizedDeploymentType.None && lstDeployZones.getSelectedIndices().length == 0) {
            valBuilder.append("Force needs to be synced or have explicit deployment zones");
        }

        if (txtForceName.getText().isBlank()) {
            if (!valBuilder.isEmpty()) {
                valBuilder.append("\n");
            }

            valBuilder.append("Force must have an ID.");
        }

        if ((cboAlignment.getSelectedIndex() != ForceAlignment.Player.ordinal()) &&
                  (cboGenerationMethod.getSelectedIndex() == ForceGenerationMethod.PlayerSupplied.ordinal())) {
            if (!valBuilder.isEmpty()) {
                valBuilder.append("\n");
            }

            valBuilder.append("Bot-controlled forces cannot be player-supplied.");
        }

        if (chkOffBoard.isSelected() && !chkUseArtillery.isSelected()) {
            if (!valBuilder.isEmpty()) {
                valBuilder.append("\n");
            }

            valBuilder.append("Non-artillery units cannot be deployed off board.");
        }

        if (cboMinWeightClass.getSelectedIndex() > cboMaxWeightClass.getSelectedIndex()) {
            if (!valBuilder.isEmpty()) {
                valBuilder.append("\n");
            }

            valBuilder.append("Min weight class is greater than max weight class.");
        }

        // Duplicate-ID and rename handling is enforced at commit time by ForceRosterEditor, which has the editing
        // context this method does not.

        return valBuilder.toString();
    }

    /**
     * Event handler for when the "Remove" button is pressed for a particular force template.
     *
     * @param command The command string containing the index of the force to remove.
     */
    private void deleteForceButtonHandler(String command) {
        String forceIndex = ForceListCommand.removeForceId(command);
        scenarioTemplate.getScenarioForces().remove(forceIndex);

        // If we just removed the force being edited, drop back to "add new" mode so a later commit does not resurrect
        // it under a stale ID.
        if (forceIndex.equals(editedForceId)) {
            editedForceId = null;
        }

        updateForceSyncList();
        renderForceList();
        pack();
        repaint();
    }

    /**
     * Event handler for when the "Edit" button is pressed for a particular force template.
     *
     * @param command The command string containing the index of the force to edit.
     */
    private void editForceButtonHandler(String command) {
        String forceIndex = ForceListCommand.editForceId(command);
        loadForce(scenarioTemplate.getScenarioForces().get(forceIndex));
        // Remember which force we are editing so committing updates it in place (and handles a rename) instead of
        // adding a duplicate.
        editedForceId = forceIndex;
    }

    /**
     * Worker method that updates the "Force Sync" list and sets the relevant dropdowns to active or inactive
     */
    private void updateForceSyncList() {
        cboSyncForceName.removeAllItems();
        DefaultListModel<String> objectiveLinkedModel = new DefaultListModel<>();
        for (String forceID : scenarioTemplate.getScenarioForces().keySet()) {
            cboSyncForceName.addItem(forceID);
            objectiveLinkedModel.addElement(forceID);
        }
        // Rebuild the objective-linked candidate list from the current roster. Selection is (re)applied by loadForce
        // when editing, and left empty when adding a new force.
        lstObjectiveLinkedForces.setModel(objectiveLinkedModel);

        boolean forcesAvailableToSync = cboSyncForceName.getItemCount() > 0;
        cboSyncForceName.setEnabled(forcesAvailableToSync);
        cboSyncDeploymentType.setEnabled(forcesAvailableToSync);
    }

    /**
     * Event handler for when the force alignment/generation method is changed. Enables or disables some controls based
     * on whether the force is a player deployed force, or an enemy force.
     */
    private void forceAlignmentChangeHandler() {
        boolean isPlayerForce = Objects.equals(cboAlignment.getSelectedItem(),
              ScenarioForceTemplate.FORCE_ALIGNMENTS[0]) &&
                                      Objects.equals(cboGenerationMethod.getSelectedItem(),
                                            ScenarioForceTemplate.FORCE_GENERATION_METHODS[0]);

        boolean isEnemyForce = (cboAlignment.getSelectedIndex() ==
                                      ScenarioForceTemplate.ForceAlignment.Opposing.ordinal()) ||
                                     (cboAlignment.getSelectedIndex() ==
                                            ScenarioForceTemplate.ForceAlignment.Third.ordinal()) ||
                                     (cboAlignment.getSelectedIndex() ==
                                            ScenarioForceTemplate.ForceAlignment.PlanetOwner.ordinal());

        spnMultiplier.setEnabled(!isPlayerForce);
        spnRetreatThreshold.setEnabled(!isPlayerForce);
        cboMaxWeightClass.setEnabled(!isPlayerForce);
        cboMinWeightClass.setEnabled(!isPlayerForce);
        chkContributesToBV.setEnabled(!isEnemyForce);
        chkContributesToBV.setSelected(!isEnemyForce);
        chkContributesToUnitCount.setEnabled(!isEnemyForce);
        chkContributesToUnitCount.setSelected(!isEnemyForce);
        chkContributesToMapSize.setSelected(true);

        spnMultiplier.setEnabled(cboGenerationMethod.getSelectedIndex() !=
                                       ForceGenerationMethod.FixedUnitCount.ordinal());
    }

    /**
     * Event handler for when the force sync dropdown changes value. Enables or disables the "force to sync" and
     * "deployment zone" UI elements as appropriate.
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
     * Event handler for when the unit type dropdown changes value. Enables or disables the "allow aero bombs" UI
     * element as appropriate.
     */
    private void unitTypeChangeHandler() {
        int selectedItem = cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();
        boolean isAero = selectedItem == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX ||
                               selectedItem == UnitType.CONV_FIGHTER ||
                               selectedItem == UnitType.AEROSPACE_FIGHTER;

        chkAllowAeroBombs.setEnabled(isAero);
        refreshRolePicker();
    }

    /**
     * The allowed unit type currently selected in the force editor (a {@link UnitType} constant, or a negative special
     * mixed-type value).
     */
    private int currentAllowedUnitType() {
        return cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();
    }

    /**
     * Repopulates the role picker with the roles selectable for the currently selected unit type. Guarded because the
     * unit-type change handler can fire before the role editor has been built.
     */
    private void refreshRolePicker() {
        if (lstRolePicker == null) {
            return;
        }

        DefaultListModel<MissionRole> model = new DefaultListModel<>();
        for (MissionRole role : MissionRoleChoices.selectableRoles(currentAllowedUnitType())) {
            model.addElement(role);
        }
        lstRolePicker.setModel(model);
    }

    /**
     * Event handler for the "Add Role Set" button. Turns the currently selected roles into a single role set and adds
     * it to the force's list, unless that exact set is already present.
     */
    private void addRoleSetHandler() {
        List<MissionRole> selectedRoles = lstRolePicker.getSelectedValuesList();
        if (selectedRoles.isEmpty()) {
            return;
        }

        String entry = MissionRoleChoices.toEntry(selectedRoles);
        if (!roleSetsModel.contains(entry)) {
            roleSetsModel.addElement(entry);
        }
        lstRolePicker.clearSelection();
    }

    /**
     * Event handler for the "Remove Role Set" button. Removes the selected role sets from the force's list.
     */
    private void removeRoleSetHandler() {
        for (String entry : lstRoleSets.getSelectedValuesList()) {
            roleSetsModel.removeElement(entry);
        }
    }

    /**
     * Event handler for the "Save" button.
     */
    private void saveTemplateButtonHandler() {
        // Validate the free-text map dimensions before mutating anything, so an invalid entry aborts the save cleanly
        // instead of throwing mid-way and leaving the template partially updated.
        String dimensionErrors = mapParametersPanel.validateInput();
        if (!dimensionErrors.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  dimensionErrors,
                  "Invalid Map Parameters",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        templatePropertiesPanel.writeInto(scenarioTemplate);
        mapParametersPanel.writeInto(scenarioTemplate.mapParameters);
        modifiersPanel.writeInto(scenarioTemplate.scenarioModifiers);

        FileDialogs.saveScenarioTemplate((JFrame) getOwner(), scenarioTemplate)
              .ifPresent(file -> scenarioTemplate.Serialize(file));
    }

    /**
     * Event handler for when the load button is cleared. Invokes deserialization functionality for user-selected file,
     * then reloads all UI elements.
     */
    private void loadTemplateButtonHandler() {
        File file = FileDialogs.openScenarioTemplate((JFrame) getOwner()).orElse(null);
        if (file == null) {
            return;
        }

        scenarioTemplate = ScenarioTemplate.Deserialize(file);

        if (scenarioTemplate == null) {
            JOptionPane.showMessageDialog(this,
                  "Error loading specified file. See log for details.",
                  "Load Error",
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // A freshly loaded template starts in "add new" mode; any prior edit context is stale.
        editedForceId = null;

        getContentPane().removeAll();
        globalPanel.removeAll();
        initComponents();
        pack();
        validate();
        // Do not re-register user preferences here. The window preference was registered once when the dialog was
        // constructed, and reloading a template rebuilds the contents but not the window itself; calling
        // setUserPreferences() again would double-manage the same JWindowPreference.
    }

    /**
     * General event handler for button clicks on this dialog. Examines the action command and invokes appropriate
     * method.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (ADD_FORCE_COMMAND.equals(e.getActionCommand())) {
            addForceButtonHandler();
        } else if (ForceListCommand.isRemove(e.getActionCommand())) {
            deleteForceButtonHandler(e.getActionCommand());
        } else if (ForceListCommand.isEdit(e.getActionCommand())) {
            editForceButtonHandler(e.getActionCommand());
        } else if (SAVE_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            saveTemplateButtonHandler();
        } else if (LOAD_TEMPLATE_COMMAND.equals(e.getActionCommand())) {
            loadTemplateButtonHandler();
        }
    }

    /**
     * Helper method that hides or reveals the force editor section.
     */
    private void toggleForcePanelVisibility() {
        forcedPanel.setVisible(!forcedPanel.isVisible());
        forceScrollPane.setVisible(!forceScrollPane.isVisible() && !scenarioTemplate.getScenarioForces().isEmpty());
    }

}
