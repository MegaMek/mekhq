/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.WrapLayout.wordWrap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.border.LineBorder;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.ScenarioForceTemplate.SynchronizedDeploymentType;

/**
 * Editor panel for a single {@link ScenarioForceTemplate} - the "Participating Forces" editor. Owns the ~30 input
 * controls, the internal enable/disable interactions, and the role sub-editor ({@link RoleSetEditorPanel}).
 *
 * <p>Extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). MUL file names and the set of other force IDs (for
 * the sync and objective-linked pickers) are supplied by the caller, so the panel has no data-file or roster dependency
 * and is headless-testable. {@link #loadForce}/{@link #validateInput}/{@link #buildForceTemplate} are the model
 * contract; the containing dialog owns the roster commit and force list.
 */
public class ForceEditorPanel extends JPanel {

    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(
          "mekhq.resources.ScenarioTemplateEditorDialog");

    private final Dimension spinnerSize = new Dimension(55, 25);

    private final JComboBox<String> cboAlignment = new JComboBox<>(ScenarioForceTemplate.FORCE_ALIGNMENTS);
    private final JComboBox<String> cboGenerationMethod = new JComboBox<>(ScenarioForceTemplate.FORCE_GENERATION_METHODS);
    private final JSpinner spnMultiplier = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 4.0, .05));
    private final JComboBox<String> cboDestinationZone = new JComboBox<>(ScenarioForceTemplate.BOT_DESTINATION_ZONES);
    private final JSpinner spnRetreatThreshold = new JSpinner(new SpinnerNumberModel(50, 0, 100, 5));
    private final JCheckBox chkReinforce = new JCheckBox();
    private final JCheckBox chkContributesToBV = new JCheckBox();
    private final JCheckBox chkContributesToUnitCount = new JCheckBox();
    private final JTextField txtForceName = new JTextField(10);
    private final JComboBox<String> cboSyncDeploymentType =
          new JComboBox<>(ScenarioForceTemplate.FORCE_DEPLOYMENT_SYNC_TYPES);
    private final JComboBox<String> cboSyncForceName = new JComboBox<>();
    private final JList<String> listMULs = new JList<>();
    private final JList<String> lstObjectiveLinkedForces = new JList<>();
    private final JList<String> lstDeployZones = new JList<>();
    private final JComboBox<String> cboUnitType = new JComboBox<>();
    private final JSpinner spnArrivalTurn = new JSpinner(new SpinnerNumberModel(0,
          ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS, 100, 1));
    private final JSpinner spnFixedUnitCount = new JSpinner(new SpinnerNumberModel(0, -1, 100, 1));
    private final JComboBox<String> cboMaxWeightClass = new JComboBox<>();
    private final JComboBox<String> cboMinWeightClass = new JComboBox<>();
    private final JCheckBox chkContributesToMapSize = new JCheckBox();
    private final JSpinner spnGenerationOrder = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JCheckBox chkAllowAeroBombs = new JCheckBox();
    private final JSpinner spnStartingAltitude = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
    private final JCheckBox chkUseArtillery = new JCheckBox();
    private final JCheckBox chkOffBoard = new JCheckBox();
    private final JCheckBox chkSubjectToRandomRemoval = new JCheckBox();
    private final JCheckBox chkSyncRetreatThreshold = new JCheckBox();
    private final RoleSetEditorPanel roleSetEditorPanel = new RoleSetEditorPanel();
    private final JButton btnSave = new JButton(RESOURCES.getString("ForceEditorPanel.save"));

    private Runnable onSave;

    /**
     * @param mulFileNames the fixed-MUL file names to offer, in display order (supplied so the panel does not read the
     *                     MUL directory itself)
     */
    public ForceEditorPanel(List<String> mulFileNames) {
        super(new GridBagLayout());
        setBorder(new LineBorder(Color.BLACK));
        initComponents(mulFileNames);
    }

    private void initComponents(List<String> mulFileNames) {
        cboDestinationZone.setSelectedIndex(CardinalEdge.NONE.getIndex());
        spnMultiplier.setPreferredSize(spinnerSize);
        spnRetreatThreshold.setPreferredSize(spinnerSize);

        DefaultListModel<String> mulModel = new DefaultListModel<>();
        mulFileNames.forEach(mulModel::addElement);
        listMULs.setModel(mulModel);
        listMULs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        lstObjectiveLinkedForces.setModel(new DefaultListModel<>());
        lstObjectiveLinkedForces.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        DefaultListModel<String> zoneModel = new DefaultListModel<>();
        for (String s : ScenarioForceTemplate.DEPLOYMENT_ZONES) {
            zoneModel.addElement(s);
        }
        lstDeployZones.setModel(zoneModel);

        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX));
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX));
        cboUnitType.addItem(ScenarioForceTemplate.SPECIAL_UNIT_TYPES.get(ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_CIVILIANS));
        for (int unitTypeID = 0; unitTypeID < UnitType.SIZE; unitTypeID++) {
            cboUnitType.addItem(UnitType.getTypeDisplayableName(unitTypeID));
        }

        for (int x = EntityWeightClass.WEIGHT_ULTRA_LIGHT; x <= EntityWeightClass.WEIGHT_ASSAULT; x++) {
            cboMaxWeightClass.addItem(EntityWeightClass.getClassName(x));
            cboMinWeightClass.addItem(EntityWeightClass.getClassName(x));
        }
        cboMaxWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_ASSAULT);
        cboMinWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_LIGHT);

        chkAllowAeroBombs.setEnabled(false);
        chkSubjectToRandomRemoval.setSelected(true);

        ItemListener dropdownChangeListener = evt -> forceAlignmentChanged();
        cboAlignment.addItemListener(dropdownChangeListener);
        cboGenerationMethod.addItemListener(dropdownChangeListener);
        cboSyncDeploymentType.addItemListener(evt -> syncDeploymentChanged());
        cboUnitType.addItemListener(evt -> unitTypeChanged());
        btnSave.addActionListener(evt -> {
            if (onSave != null) {
                onSave.run();
            }
        });

        layoutControls();
        applyTooltips();
        roleSetEditorPanel.setAllowedUnitType(currentAllowedUnitType());
        forceAlignmentChanged();
    }

    /**
     * Applies a wrapped, behavior-accurate tooltip to each control. Tooltips MUST be wrapped in
     * {@link megamek.client.ui.WrapLayout#wordWrap(String)} or the layout breaks, and each string describes what the
     * generator actually does with the value (see {@code AtBDynamicScenarioFactory}), not the field's older doc text.
     */
    private void applyTooltips() {
        setTip(cboAlignment, "ForceEditorPanel.forceAlignment.tooltip");
        setTip(cboGenerationMethod, "ForceEditorPanel.generationMethod.tooltip");
        setTip(spnMultiplier, "ForceEditorPanel.scalingMultiplier.tooltip");
        setTip(cboDestinationZone, "ForceEditorPanel.destinationZone.tooltip");
        setTip(spnRetreatThreshold, "ForceEditorPanel.retreatThreshold.tooltip");
        setTip(chkReinforce, "ForceEditorPanel.reinforce.tooltip");
        setTip(chkContributesToBV, "ForceEditorPanel.contributesToBV.tooltip");
        setTip(chkContributesToUnitCount, "ForceEditorPanel.contributesToUnitCount.tooltip");
        setTip(txtForceName, "ForceEditorPanel.forceId.tooltip");
        setTip(cboSyncDeploymentType, "ForceEditorPanel.syncDeployment.tooltip");
        setTip(cboSyncForceName, "ForceEditorPanel.syncForceName.tooltip");
        setTip(listMULs, "ForceEditorPanel.fixedMul.tooltip");
        setTip(lstObjectiveLinkedForces, "ForceEditorPanel.objectiveLinkedForces.tooltip");
        setTip(lstDeployZones, "ForceEditorPanel.deploymentZones.tooltip");
        setTip(cboUnitType, "ForceEditorPanel.unitType.tooltip");
        setTip(spnArrivalTurn, "ForceEditorPanel.arrivalTurn.tooltip");
        setTip(spnFixedUnitCount, "ForceEditorPanel.fixedUnitCount.tooltip");
        setTip(cboMaxWeightClass, "ForceEditorPanel.maxWeight.tooltip");
        setTip(cboMinWeightClass, "ForceEditorPanel.minWeight.tooltip");
        setTip(chkContributesToMapSize, "ForceEditorPanel.contributesToMapSize.tooltip");
        setTip(spnGenerationOrder, "ForceEditorPanel.generationOrder.tooltip");
        setTip(chkAllowAeroBombs, "ForceEditorPanel.allowAeroBombs.tooltip");
        setTip(spnStartingAltitude, "ForceEditorPanel.startAltitude.tooltip");
        setTip(chkUseArtillery, "ForceEditorPanel.isArtillery.tooltip");
        setTip(chkOffBoard, "ForceEditorPanel.deployOffBoard.tooltip");
        setTip(chkSubjectToRandomRemoval, "ForceEditorPanel.subjectToRandomRemoval.tooltip");
        setTip(chkSyncRetreatThreshold, "ForceEditorPanel.syncRetreatThreshold.tooltip");
        setTip(btnSave, "ForceEditorPanel.save.tooltip");
    }

    /** Sets a word-wrapped tooltip (from the resource bundle) on a control. */
    private static void setTip(JComponent component, String tooltipKey) {
        component.setToolTipText(wordWrap(RESOURCES.getString(tooltipKey)));
    }

    private void layoutControls() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;

        addRow(gbc, "ForceEditorPanel.forceAlignment.label", cboAlignment);
        addRow(gbc, "ForceEditorPanel.generationMethod.label", cboGenerationMethod);
        addRow(gbc, "ForceEditorPanel.scalingMultiplier.label", spnMultiplier);
        addRow(gbc, "ForceEditorPanel.destinationZone.label", cboDestinationZone);
        addRow(gbc, "ForceEditorPanel.retreatThreshold.label", spnRetreatThreshold);
        addRow(gbc, "ForceEditorPanel.reinforce.label", chkReinforce);
        addRow(gbc, "ForceEditorPanel.contributesToBV.label", chkContributesToBV);
        addRow(gbc, "ForceEditorPanel.contributesToUnitCount.label", chkContributesToUnitCount);
        addRow(gbc, "ForceEditorPanel.forceId.label", txtForceName);
        addRow(gbc, "ForceEditorPanel.syncDeployment.label", cboSyncDeploymentType);

        gbc.gridy++;
        gbc.gridx = 1;
        add(cboSyncForceName, gbc);

        addRow(gbc, "ForceEditorPanel.fixedMul.label", new FastJScrollPane(listMULs));
        addRow(gbc, "ForceEditorPanel.objectiveLinkedForces.label", new FastJScrollPane(lstObjectiveLinkedForces));

        // second column: deployment zones (full height)
        gbc.gridx = 2;
        gbc.gridy = 0;
        add(new JLabel(RESOURCES.getString("ForceEditorPanel.deploymentZones.label")), gbc);
        gbc.gridy = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(lstDeployZones, gbc);

        // third/fourth columns: unit-type-and-below controls
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        add(new JLabel(RESOURCES.getString("ForceEditorPanel.unitType.label")), gbc);
        gbc.gridx = 4;
        add(cboUnitType, gbc);

        addGridRow(gbc, "ForceEditorPanel.arrivalTurn.label", spnArrivalTurn);
        addGridRow(gbc, "ForceEditorPanel.fixedUnitCount.label", spnFixedUnitCount);
        addGridRow(gbc, "ForceEditorPanel.maxWeight.label", cboMaxWeightClass);
        addGridRow(gbc, "ForceEditorPanel.minWeight.label", cboMinWeightClass);
        addGridRow(gbc, "ForceEditorPanel.contributesToMapSize.label", chkContributesToMapSize);
        addGridRow(gbc, "ForceEditorPanel.generationOrder.label", spnGenerationOrder);
        addGridRow(gbc, "ForceEditorPanel.allowAeroBombs.label", chkAllowAeroBombs);
        addGridRow(gbc, "ForceEditorPanel.startAltitude.label", spnStartingAltitude);
        addGridRow(gbc, "ForceEditorPanel.isArtillery.label", chkUseArtillery);
        addGridRow(gbc, "ForceEditorPanel.deployOffBoard.label", chkOffBoard);
        addGridRow(gbc, "ForceEditorPanel.subjectToRandomRemoval.label", chkSubjectToRandomRemoval);
        addGridRow(gbc, "ForceEditorPanel.syncRetreatThreshold.label", chkSyncRetreatThreshold);

        gbc.gridx = 5;
        add(btnSave, gbc);

        GridBagConstraints roleGbc = new GridBagConstraints();
        roleGbc.gridx = 6;
        roleGbc.gridy = 0;
        roleGbc.gridheight = GridBagConstraints.REMAINDER;
        roleGbc.anchor = GridBagConstraints.NORTHWEST;
        roleGbc.insets = new Insets(0, 10, 0, 0);
        add(roleSetEditorPanel, roleGbc);
    }

    /** Adds a resource-keyed label/control pair in the first column pair (gridx 0/1), advancing gridy. */
    private void addRow(GridBagConstraints gbc, String labelKey, java.awt.Component control) {
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel(RESOURCES.getString(labelKey)), gbc);
        gbc.gridx = 1;
        add(control, gbc);
    }

    /** Adds a resource-keyed label/control pair in the third column pair (gridx 3/4), advancing gridy. */
    private void addGridRow(GridBagConstraints gbc, String labelKey, java.awt.Component control) {
        gbc.gridx = 3;
        gbc.gridy++;
        add(new JLabel(RESOURCES.getString(labelKey)), gbc);
        gbc.gridx = 4;
        add(control, gbc);
    }

    /**
     * Sets the callback invoked when the panel's Save button is pressed.
     */
    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    /**
     * Resets the editor to its default "add a new force" state, so it does not retain the last-edited force's values.
     */
    public void reset() {
        cboAlignment.setSelectedIndex(0);
        cboGenerationMethod.setSelectedIndex(0);
        spnMultiplier.setValue(1.0);
        cboDestinationZone.setSelectedIndex(CardinalEdge.NONE.getIndex());
        spnRetreatThreshold.setValue(50);
        chkReinforce.setSelected(false);
        chkContributesToBV.setSelected(false);
        chkContributesToUnitCount.setSelected(false);
        txtForceName.setText("");
        cboSyncDeploymentType.setSelectedIndex(0);
        cboSyncForceName.setSelectedIndex(cboSyncForceName.getItemCount() > 0 ? 0 : -1);
        listMULs.clearSelection();
        lstObjectiveLinkedForces.clearSelection();
        lstDeployZones.clearSelection();
        cboUnitType.setSelectedIndex(0);
        spnArrivalTurn.setValue(0);
        spnFixedUnitCount.setValue(0);
        cboMaxWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_ASSAULT);
        cboMinWeightClass.setSelectedIndex(EntityWeightClass.WEIGHT_LIGHT);
        chkContributesToMapSize.setSelected(false);
        spnGenerationOrder.setValue(1);
        chkAllowAeroBombs.setSelected(false);
        spnStartingAltitude.setValue(0);
        chkUseArtillery.setSelected(false);
        chkOffBoard.setSelected(false);
        chkSubjectToRandomRemoval.setSelected(true);
        chkSyncRetreatThreshold.setSelected(false);
        roleSetEditorPanel.load(List.of());
        forceAlignmentChanged();
    }

    /**
     * @return the force ID currently entered
     */
    public String getForceName() {
        return txtForceName.getText();
    }

    /**
     * Updates the "sync" and "objective-linked" candidate lists from the current set of other force IDs.
     */
    public void setAvailableForceIds(Collection<String> forceIds) {
        cboSyncForceName.removeAllItems();
        DefaultListModel<String> objectiveLinkedModel = new DefaultListModel<>();
        for (String forceId : forceIds) {
            cboSyncForceName.addItem(forceId);
            objectiveLinkedModel.addElement(forceId);
        }
        lstObjectiveLinkedForces.setModel(objectiveLinkedModel);

        boolean forcesAvailableToSync = cboSyncForceName.getItemCount() > 0;
        cboSyncForceName.setEnabled(forcesAvailableToSync);
        cboSyncDeploymentType.setEnabled(forcesAvailableToSync);
    }

    /**
     * Loads the given force template into the editor controls.
     */
    public void loadForce(ScenarioForceTemplate forceTemplate) {
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

        roleSetEditorPanel.load(forceTemplate.getRoleCollections());
    }

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
     * Validates the current editor state, returning an empty string if valid or a newline-separated list of problems.
     */
    public String validateInput() {
        StringBuilder valBuilder = new StringBuilder();

        if (SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()] ==
                  SynchronizedDeploymentType.None && lstDeployZones.getSelectedIndices().length == 0) {
            valBuilder.append("Force needs to be synced or have explicit deployment zones");
        }

        if (txtForceName.getText().isBlank()) {
            appendSeparator(valBuilder);
            valBuilder.append("Force must have an ID.");
        }

        if ((cboAlignment.getSelectedIndex() != ForceAlignment.Player.ordinal()) &&
                  (cboGenerationMethod.getSelectedIndex() == ForceGenerationMethod.PlayerSupplied.ordinal())) {
            appendSeparator(valBuilder);
            valBuilder.append("Bot-controlled forces cannot be player-supplied.");
        }

        if (chkOffBoard.isSelected() && !chkUseArtillery.isSelected()) {
            appendSeparator(valBuilder);
            valBuilder.append("Non-artillery units cannot be deployed off board.");
        }

        if (cboMinWeightClass.getSelectedIndex() > cboMaxWeightClass.getSelectedIndex()) {
            appendSeparator(valBuilder);
            valBuilder.append("Min weight class is greater than max weight class.");
        }

        return valBuilder.toString();
    }

    private static void appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
    }

    /**
     * Builds a force template from the current editor state. Call {@link #validateInput()} first.
     */
    public ScenarioForceTemplate buildForceTemplate() {
        int forceAlignment = cboAlignment.getSelectedIndex();
        int generationMethod = cboGenerationMethod.getSelectedIndex();
        double forceMultiplier = (double) spnMultiplier.getValue();

        List<Integer> deploymentZones = new ArrayList<>();
        for (int x : lstDeployZones.getSelectedIndices()) {
            deploymentZones.add(x);
        }

        int destinationZone = DestinationZoneMapper.comboIndexToStoredZone(cboDestinationZone.getSelectedIndex());
        int retreatThreshold = (int) spnRetreatThreshold.getValue();
        int allowedUnitType = currentAllowedUnitType();

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
        sft.getRoleCollections().addAll(roleSetEditorPanel.getRoleSets());

        if (sft.getSyncDeploymentType() != SynchronizedDeploymentType.None) {
            Object syncedForce = cboSyncForceName.getSelectedItem();
            sft.setSyncedForceName(syncedForce == null ? null : syncedForce.toString());
        } else {
            sft.setDeploymentZones(deploymentZones);
        }

        return sft;
    }

    private int currentAllowedUnitType() {
        return cboUnitType.getSelectedIndex() - ScenarioForceTemplate.SPECIAL_UNIT_TYPES.size();
    }

    private void forceAlignmentChanged() {
        boolean isPlayerForce = java.util.Objects.equals(cboAlignment.getSelectedItem(),
              ScenarioForceTemplate.FORCE_ALIGNMENTS[0]) &&
                                      java.util.Objects.equals(cboGenerationMethod.getSelectedItem(),
                                            ScenarioForceTemplate.FORCE_GENERATION_METHODS[0]);

        boolean isEnemyForce = (cboAlignment.getSelectedIndex() == ForceAlignment.Opposing.ordinal()) ||
                                     (cboAlignment.getSelectedIndex() == ForceAlignment.Third.ordinal()) ||
                                     (cboAlignment.getSelectedIndex() == ForceAlignment.PlanetOwner.ordinal());

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

    private void syncDeploymentChanged() {
        SynchronizedDeploymentType syncDeploymentType =
              SynchronizedDeploymentType.values()[cboSyncDeploymentType.getSelectedIndex()];
        boolean syncForceDeployment = syncDeploymentType != SynchronizedDeploymentType.None;

        cboSyncForceName.setEnabled(syncForceDeployment);
        lstDeployZones.setEnabled(!syncForceDeployment);
        if (!lstDeployZones.isEnabled()) {
            lstDeployZones.clearSelection();
        }
    }

    private void unitTypeChanged() {
        int selectedItem = currentAllowedUnitType();
        boolean isAero = selectedItem == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX ||
                               selectedItem == UnitType.CONV_FIGHTER ||
                               selectedItem == UnitType.AEROSPACE_FIGHTER;

        chkAllowAeroBombs.setEnabled(isAero);
        roleSetEditorPanel.setAllowedUnitType(selectedItem);
    }
}
