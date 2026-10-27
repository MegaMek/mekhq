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
package mekhq.gui.developerTools;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.StratConScenarioFactory;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifierJson;
import mekhq.gui.FileDialogs;
import mekhq.utilities.MMDataLicenseHeader;

/**
 * A developer tool for editing scenario modifier files (JSON). The force definition is edited in a sub-dialog that
 * reuses the scenario template editor's force panel. Objectives and linked modifiers can't be edited here, so they are
 * carried on the loaded modifier and saved back untouched.
 */
public class ScenarioModifierEditorDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioModifierEditorDialog.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";
    private static final String MODIFIER_MANIFEST_FILE_NAME = "modifiermanifest.json";

    private final JFrame frame;
    private AtBScenarioModifier modifier = new AtBScenarioModifier();
    // The file the current modifier was last loaded from or saved to; null until then. Registering with the manifest
    // needs a concrete file name, so the button stays disabled until this is set.
    private File currentFile;
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));
    // serialized snapshot of the last new/loaded/saved editor state, for unsaved-changes detection
    private String capturedState = "";

    // The force definition being edited. It is only written to the modifier on save, like the other fields.
    private ScenarioForceTemplate forceDefinition;
    private final JLabel lblForceDefinition = new JLabel();
    private final JButton btnEditForceDefinition = new JButton();
    private final JButton btnRemoveForceDefinition = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));

    private final JTextField txtModifierName = new JTextField(30);
    private final JTextArea txtAdditionalBriefingText = new JTextArea(3, 40);
    private final JCheckBox chkBenefitsPlayer = new JCheckBox();
    private final JCheckBox chkBlockFurtherEvents = new JCheckBox();
    private final JComboBox<EventTiming> cboEventTiming = new JComboBox<>(EventTiming.values());
    private final JComboBox<ForceAlignment> cboEventRecipient = new JComboBox<>(ForceAlignment.values());
    private final JTextField txtSkillAdjustment = new JTextField(6);
    private final JTextField txtQualityAdjustment = new JTextField(6);
    private final JTextField txtBattleDamageIntensity = new JTextField(6);
    private final JTextField txtAmmoExpenditureIntensity = new JTextField(6);
    private final JTextField txtUnitRemovalCount = new JTextField(6);
    private final JTextField txtNumExtraEvents = new JTextField(6);
    private final JTextField txtBvBudgetAdditiveMultiplier = new JTextField(6);
    private final JTextField txtReinforcementDelayReduction = new JTextField(6);
    private final JCheckBox chkUseAmbushLogic = new JCheckBox();
    private final JCheckBox chkSwitchSides = new JCheckBox();
    private final Map<MapLocation, JCheckBox> mapLocationChecks = new EnumMap<>(MapLocation.class);
    private final DefaultListModel<String> terrainTypeModel = new DefaultListModel<>();
    private final JList<String> lstTerrainTypes = new JList<>(terrainTypeModel);

    public ScenarioModifierEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, "modifierEditor.title"));
        setLayout(new BorderLayout());
        add(new FastJScrollPane(buildForm()), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
        load(modifier);
        pack();
        setLocationRelativeTo(parent);

        captureBaseline();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmDiscardChanges()) {
                    dispose();
                }
            }
        });
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);

        txtAdditionalBriefingText.setLineWrap(true);
        txtAdditionalBriefingText.setWrapStyleWord(true);
        // an unselected combo (index -1) is how a null timing/recipient is represented
        cboEventTiming.setSelectedIndex(-1);
        cboEventRecipient.setSelectedIndex(-1);

        addRow(panel, gbc, "modifierEditor.modifierName", txtModifierName);
        addRow(panel, gbc, "modifierEditor.additionalBriefingText", new FastJScrollPane(txtAdditionalBriefingText));
        addRow(panel, gbc, "modifierEditor.eventTiming", cboEventTiming);
        addRow(panel, gbc, "modifierEditor.eventRecipient", cboEventRecipient);
        addRow(panel, gbc, "modifierEditor.forceDefinition", buildForceDefinitionField());
        addRow(panel, gbc, "modifierEditor.benefitsPlayer", chkBenefitsPlayer);
        addRow(panel, gbc, "modifierEditor.blockFurtherEvents", chkBlockFurtherEvents);
        addRow(panel, gbc, "modifierEditor.skillAdjustment", txtSkillAdjustment);
        addRow(panel, gbc, "modifierEditor.qualityAdjustment", txtQualityAdjustment);
        addRow(panel, gbc, "modifierEditor.battleDamageIntensity", txtBattleDamageIntensity);
        addRow(panel, gbc, "modifierEditor.ammoExpenditureIntensity", txtAmmoExpenditureIntensity);
        addRow(panel, gbc, "modifierEditor.unitRemovalCount", txtUnitRemovalCount);
        addRow(panel, gbc, "modifierEditor.numExtraEvents", txtNumExtraEvents);
        addRow(panel, gbc, "modifierEditor.bvBudgetAdditiveMultiplier", txtBvBudgetAdditiveMultiplier);
        addRow(panel, gbc, "modifierEditor.reinforcementDelayReduction", txtReinforcementDelayReduction);
        addRow(panel, gbc, "modifierEditor.useAmbushLogic", chkUseAmbushLogic);
        addRow(panel, gbc, "modifierEditor.switchSides", chkSwitchSides);

        JPanel locationsPanel = new JPanel(new GridLayout(0, 2));
        for (MapLocation location : MapLocation.values()) {
            JCheckBox check = new JCheckBox(location.name());
            mapLocationChecks.put(location, check);
            locationsPanel.add(check);
        }
        addRow(panel, gbc, "modifierEditor.allowedMapLocations", locationsPanel);

        // Terrain-type targeting: only meaningful when SpecificGroundTerrain is an allowed location, so the list is
        // enabled by that checkbox. An empty selection means "any terrain".
        for (String terrainType : sortedTerrainTypeNames()) {
            terrainTypeModel.addElement(terrainType);
        }
        lstTerrainTypes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstTerrainTypes.setVisibleRowCount(6);
        lstTerrainTypes.setToolTipText(getTextAt(RESOURCE_BUNDLE, "modifierEditor.allowedTerrainTypes.tooltip"));
        JCheckBox specificGroundTerrainCheck = mapLocationChecks.get(MapLocation.SpecificGroundTerrain);
        specificGroundTerrainCheck.addActionListener(e -> updateTerrainListEnabled());
        addRow(panel, gbc, "modifierEditor.allowedTerrainTypes", new FastJScrollPane(lstTerrainTypes));

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JLabel("<html><i>" + getTextAt(RESOURCE_BUNDLE, "modifierEditor.advancedNote") + "</i></html>"),
              gbc);
        return panel;
    }

    /**
     * Builds the force definition row: a summary of the current force, with buttons to add or edit it in the shared
     * force editor and to remove it.
     */
    private JPanel buildForceDefinitionField() {
        btnEditForceDefinition.addActionListener(e -> editForceDefinition());
        btnRemoveForceDefinition.addActionListener(e -> {
            forceDefinition = null;
            updateForceDefinitionControls();
        });

        JPanel field = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        field.add(lblForceDefinition);
        field.add(btnEditForceDefinition);
        field.add(btnRemoveForceDefinition);
        return field;
    }

    private void editForceDefinition() {
        new ModifierForceDefinitionDialog(this, forceDefinition, StratConScenarioFactory.getAllScenarioForceIds())
              .showDialog()
              .ifPresent(editedForce -> {
                  forceDefinition = editedForce;
                  updateForceDefinitionControls();
              });
    }

    /**
     * Refreshes the force definition summary and buttons. Ambush logic only acts on the modifier's own force, so it
     * can only be set while there is one; without a force it is cleared, as it would do nothing.
     */
    private void updateForceDefinitionControls() {
        boolean hasForce = forceDefinition != null;
        lblForceDefinition.setText(describeForce(forceDefinition));
        btnEditForceDefinition.setText(getTextAt(RESOURCE_BUNDLE, hasForce ? "button.edit" : "button.add"));
        btnRemoveForceDefinition.setEnabled(hasForce);

        chkUseAmbushLogic.setEnabled(hasForce);
        if (!hasForce) {
            chkUseAmbushLogic.setSelected(false);
        }
    }

    private static String describeForce(@Nullable ScenarioForceTemplate force) {
        if (force == null) {
            return getTextAt(RESOURCE_BUNDLE, "modifierEditor.forceDefinition.none");
        }
        int alignment = force.getForceAlignment();
        String alignmentName = ((alignment >= 0) && (alignment < ScenarioForceTemplate.FORCE_ALIGNMENTS.length))
                                     ? ScenarioForceTemplate.FORCE_ALIGNMENTS[alignment] : String.valueOf(alignment);
        return getFormattedTextAt(RESOURCE_BUNDLE, "modifierEditor.forceDefinition.summary",
              force.getForceName(), alignmentName);
    }

    /** @return every declared StratCon terrain type name, sorted, for the terrain-targeting list. */
    private static List<String> sortedTerrainTypeNames() {
        List<String> names = new ArrayList<>(StratConBiomeManifest.getInstance().getTerrainTypeNames());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** The terrain list is only relevant when SpecificGroundTerrain is an allowed location. */
    private void updateTerrainListEnabled() {
        boolean enabled = mapLocationChecks.get(MapLocation.SpecificGroundTerrain).isSelected();
        lstTerrainTypes.setEnabled(enabled);
        if (!enabled) {
            lstTerrainTypes.clearSelection();
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String labelKey, java.awt.Component control) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        DeveloperToolsUI.applyRowTooltip(RESOURCE_BUNDLE, labelKey, label, control);
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(control, gbc);
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel();
        JButton btnNew = new JButton(getTextAt(RESOURCE_BUNDLE, "button.new"));
        btnNew.addActionListener(e -> {
            if (!confirmDiscardChanges()) {
                return;
            }
            modifier = new AtBScenarioModifier();
            currentFile = null;
            load(modifier);
            updateManifestButtonState();
            captureBaseline();
        });
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.addActionListener(e -> loadFromFile());
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(e -> saveToFile());
        btnAddToManifest.addActionListener(e -> addToManifest());
        btnAddToManifest.setToolTipText(getTextAt(RESOURCE_BUNDLE, "modifierEditor.addToManifest.tooltip"));
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "button.close"));
        btnClose.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                dispose();
            }
        });
        bar.add(btnNew);
        bar.add(btnLoad);
        bar.add(btnSave);
        bar.add(btnAddToManifest);
        bar.add(btnClose);
        updateManifestButtonState();
        return bar;
    }

    private void updateManifestButtonState() {
        btnAddToManifest.setEnabled(currentFile != null);
    }

    /**
     * Asks whether to throw away unsaved changes, if there are any.
     *
     * @return {@code true} if there are no unsaved changes or the user agreed to discard them
     */
    private boolean confirmDiscardChanges() {
        if (!isDirty()) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(this,
              getTextAt(RESOURCE_BUNDLE, "unsavedChanges.message"),
              getTextAt(RESOURCE_BUNDLE, "unsavedChanges.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /** Records the current editor state as the "clean" baseline. Called after a new, load or save. */
    private void captureBaseline() {
        capturedState = currentSerializedState();
    }

    /** @return whether the editor state differs from the last captured baseline */
    private boolean isDirty() {
        String currentState = currentSerializedState();
        // if the state can't be rendered, assume there are changes rather than risk losing them
        return (currentState == null) || !currentState.equals(capturedState);
    }

    /**
     * Serializes a snapshot of the current editor state - the loaded modifier plus the values in the form - so it can
     * be compared against the baseline. Uses a clone so the working modifier is not changed.
     *
     * @return the snapshot's JSON, or {@code null} if it could not be rendered
     */
    private @Nullable String currentSerializedState() {
        AtBScenarioModifier snapshot = modifier.clone();
        writeInto(snapshot);
        try {
            return AtBScenarioModifierJson.toJsonString(snapshot);
        } catch (IOException ex) {
            LOGGER.error("Could not serialize the scenario modifier for unsaved-changes detection", ex);
            return null;
        }
    }

    private void load(AtBScenarioModifier source) {
        txtModifierName.setText(nullToEmpty(source.getModifierName()));
        txtAdditionalBriefingText.setText(nullToEmpty(source.getAdditionalBriefingText()));
        chkBenefitsPlayer.setSelected(Boolean.TRUE.equals(source.getBenefitsPlayer()));
        chkBlockFurtherEvents.setSelected(Boolean.TRUE.equals(source.getBlockFurtherEvents()));
        cboEventTiming.setSelectedItem(source.getEventTiming());
        cboEventRecipient.setSelectedItem(source.getEventRecipient());
        txtSkillAdjustment.setText(numToText(source.getSkillAdjustment()));
        txtQualityAdjustment.setText(numToText(source.getQualityAdjustment()));
        txtBattleDamageIntensity.setText(numToText(source.getBattleDamageIntensity()));
        txtAmmoExpenditureIntensity.setText(numToText(source.getAmmoExpenditureIntensity()));
        txtUnitRemovalCount.setText(numToText(source.getUnitRemovalCount()));
        txtNumExtraEvents.setText(numToText(source.getNumExtraEvents()));
        txtBvBudgetAdditiveMultiplier.setText(numToText(source.getBVBudgetAdditiveMultiplier()));
        txtReinforcementDelayReduction.setText(numToText(source.getReinforcementDelayReduction()));
        forceDefinition = source.getForceDefinition();
        chkUseAmbushLogic.setSelected(Boolean.TRUE.equals(source.getUseAmbushLogic()));
        chkSwitchSides.setSelected(Boolean.TRUE.equals(source.getSwitchSides()));
        // after the ambush checkbox is loaded, so a modifier without a force clears it
        updateForceDefinitionControls();
        List<MapLocation> allowed = source.getAllowedMapLocations();
        for (Map.Entry<MapLocation, JCheckBox> entry : mapLocationChecks.entrySet()) {
            entry.getValue().setSelected((allowed != null) && allowed.contains(entry.getKey()));
        }
        List<String> allowedTerrain = source.getAllowedTerrainTypes();
        lstTerrainTypes.clearSelection();
        if (allowedTerrain != null) {
            for (int i = 0; i < terrainTypeModel.size(); i++) {
                if (allowedTerrain.contains(terrainTypeModel.get(i))) {
                    lstTerrainTypes.addSelectionInterval(i, i);
                }
            }
        }
        updateTerrainListEnabled();
    }

    private void writeInto(AtBScenarioModifier target) {
        target.setModifierName(emptyToNull(txtModifierName.getText()));
        target.setAdditionalBriefingText(emptyToNull(txtAdditionalBriefingText.getText()));
        // these two default to false and are read as plain booleans, so they are always written out explicitly
        target.setBenefitsPlayer(chkBenefitsPlayer.isSelected());
        target.setBlockFurtherEvents(chkBlockFurtherEvents.isSelected());
        target.setEventTiming((EventTiming) cboEventTiming.getSelectedItem());
        target.setEventRecipient((ForceAlignment) cboEventRecipient.getSelectedItem());
        target.setForceDefinition(forceDefinition);
        target.setSkillAdjustment(textToInt(txtSkillAdjustment.getText()));
        target.setQualityAdjustment(textToInt(txtQualityAdjustment.getText()));
        target.setBattleDamageIntensity(textToInt(txtBattleDamageIntensity.getText()));
        target.setAmmoExpenditureIntensity(textToInt(txtAmmoExpenditureIntensity.getText()));
        target.setUnitRemovalCount(textToInt(txtUnitRemovalCount.getText()));
        target.setNumExtraEvents(textToInt(txtNumExtraEvents.getText()));
        target.setBVBudgetAdditiveMultiplier(textToDouble(txtBvBudgetAdditiveMultiplier.getText()));
        target.setReinforcementDelayReduction(textToInt(txtReinforcementDelayReduction.getText()));
        // these two are optional effects whose "off" state is null, matching the data files
        target.setUseAmbushLogic(checkboxToOptionalFlag(chkUseAmbushLogic));
        target.setSwitchSides(checkboxToOptionalFlag(chkSwitchSides));
        List<MapLocation> allowed = new ArrayList<>();
        for (Map.Entry<MapLocation, JCheckBox> entry : mapLocationChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                allowed.add(entry.getKey());
            }
        }
        // no locations checked means "no restriction", which the model represents as null
        target.setAllowedMapLocations(allowed.isEmpty() ? null : allowed);
        // terrain targeting only applies when SpecificGroundTerrain is allowed; an empty selection means "any terrain"
        List<String> allowedTerrain = lstTerrainTypes.getSelectedValuesList();
        boolean terrainRelevant = mapLocationChecks.get(MapLocation.SpecificGroundTerrain).isSelected();
        target.setAllowedTerrainTypes((!terrainRelevant || allowedTerrain.isEmpty()) ? null
                                            : new ArrayList<>(allowedTerrain));
    }

    /**
     * Checks that every filled-in number field holds a valid number, so a typo is reported rather than silently saved
     * as blank.
     *
     * @return an empty string if the input is valid, or a message listing the fields to fix
     */
    private String validateInput() {
        List<String> invalidFields = new ArrayList<>();
        checkInteger(invalidFields, "modifierEditor.skillAdjustment", txtSkillAdjustment);
        checkInteger(invalidFields, "modifierEditor.qualityAdjustment", txtQualityAdjustment);
        checkInteger(invalidFields, "modifierEditor.battleDamageIntensity", txtBattleDamageIntensity);
        checkInteger(invalidFields, "modifierEditor.ammoExpenditureIntensity", txtAmmoExpenditureIntensity);
        checkInteger(invalidFields, "modifierEditor.unitRemovalCount", txtUnitRemovalCount);
        checkInteger(invalidFields, "modifierEditor.numExtraEvents", txtNumExtraEvents);
        checkInteger(invalidFields, "modifierEditor.reinforcementDelayReduction", txtReinforcementDelayReduction);
        if (!isValidDecimal(txtBvBudgetAdditiveMultiplier.getText())) {
            invalidFields.add(fieldName("modifierEditor.bvBudgetAdditiveMultiplier"));
        }

        if (invalidFields.isEmpty()) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "modifierEditor.invalidNumbers.message",
              String.join("\n", invalidFields));
    }

    private static void checkInteger(List<String> invalidFields, String labelKey, JTextField field) {
        if (!isValidInteger(field.getText())) {
            invalidFields.add(fieldName(labelKey));
        }
    }

    /** @return the field's label without its trailing colon, for listing it in a message */
    private static String fieldName(String labelKey) {
        return getTextAt(RESOURCE_BUNDLE, labelKey).replaceFirst(":\\s*$", "");
    }

    private void loadFromFile() {
        if (!confirmDiscardChanges()) {
            return;
        }
        File file = FileDialogs.openScenarioModifier(frame).orElse(null);
        if (file == null) {
            return;
        }
        AtBScenarioModifier loaded = AtBScenarioModifier.Deserialize(file.getPath());
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "loadError.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        modifier = loaded;
        currentFile = file;
        load(modifier);
        updateManifestButtonState();
        captureBaseline();
    }

    private void saveToFile() {
        // validate before changing anything, so invalid input aborts the save cleanly
        String validationErrors = validateInput();
        if (!validationErrors.isEmpty()) {
            JOptionPane.showMessageDialog(this, validationErrors,
                  getTextAt(RESOURCE_BUNDLE, "modifierEditor.invalidNumbers.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        writeInto(modifier);
        FileDialogs.saveScenarioModifier(frame, modifier).ifPresent(file -> {
            modifier.Serialize(file);
            currentFile = file;
            updateManifestButtonState();
            captureBaseline();
        });
    }

    /**
     * Registers the current modifier's file name in the {@code modifiermanifest.json} that sits alongside it, so the
     * game will load it. The manifest carries curated {@code #} comments documenting disabled modifiers, so the entry
     * is inserted with a comment-preserving text edit rather than a full JSON rewrite.
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), MODIFIER_MANIFEST_FILE_NAME);

        try {
            if (!manifestFile.exists()) {
                String content = MMDataLicenseHeader.licenseHeader(manifestFile) +
                                       "\n{\n  \"fileNameList\": [\n    \"" + fileName + "\"\n  ]\n}\n";
                Files.writeString(manifestFile.toPath(), content, StandardCharsets.UTF_8);
                showManifestResult("modifierEditor.manifest.added.message", fileName, JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String text = Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8);
            String updated = insertEntryPreservingComments(text, fileName);
            if (updated == null) {
                showManifestResult("modifierEditor.manifest.alreadyPresent.message", fileName,
                      JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Files.writeString(manifestFile.toPath(), updated, StandardCharsets.UTF_8);
            showManifestResult("modifierEditor.manifest.added.message", fileName, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "modifierEditor.manifest.error.message"),
                  getTextAt(RESOURCE_BUNDLE, "modifierEditor.manifest.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showManifestResult(String messageKey, String fileName, int messageType) {
        JOptionPane.showMessageDialog(this, getFormattedTextAt(RESOURCE_BUNDLE, messageKey, fileName),
              getTextAt(RESOURCE_BUNDLE, "modifierEditor.manifest.title"), messageType);
    }

    /**
     * Returns {@code manifestText} with {@code entry} appended to the {@code fileNameList} array, preserving the file's
     * existing comments and formatting; returns {@code null} if the entry (as a quoted token) is already present. A new
     * entry is inserted right after the last quoted element, so interspersed comment lines before the closing bracket
     * are left untouched, and the previous last element gains a trailing comma.
     */
    static String insertEntryPreservingComments(String manifestText, String entry) {
        if (manifestText.contains("\"" + entry + "\"")) {
            return null;
        }

        List<String> lines = new ArrayList<>(Arrays.asList(manifestText.split("\n", -1)));

        int closeIdx = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).trim().startsWith("]")) {
                closeIdx = i;
                break;
            }
        }
        if (closeIdx < 0) {
            return null;
        }

        int lastElementIdx = -1;
        for (int i = closeIdx - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (trimmed.matches("\"[^\"]*\",?")) {
                lastElementIdx = i;
                break;
            }
            if (trimmed.endsWith("[")) {
                break;
            }
        }

        String quotedEntry = "\"" + entry + "\"";
        if (lastElementIdx >= 0) {
            String line = lines.get(lastElementIdx);
            String indent = line.substring(0, line.indexOf('"'));
            if (!line.trim().endsWith(",")) {
                lines.set(lastElementIdx, line + ",");
            }
            lines.add(lastElementIdx + 1, indent + quotedEntry);
        } else {
            // empty array: indent one level past the closing bracket line
            String closeLine = lines.get(closeIdx);
            String closeIndent = closeLine.substring(0, closeLine.length() - closeLine.stripLeading().length());
            lines.add(closeIdx, closeIndent + "  " + quotedEntry);
        }

        return String.join("\n", lines);
    }

    private static String nullToEmpty(String value) {
        return (value == null) ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static String numToText(Number value) {
        return (value == null) ? "" : value.toString();
    }

    /** @return {@code Boolean.TRUE} if the box is checked, otherwise {@code null} (the effect is left off) */
    private static @Nullable Boolean checkboxToOptionalFlag(JCheckBox checkBox) {
        return checkBox.isSelected() ? Boolean.TRUE : null;
    }

    /** @return whether the text is blank (no value) or a whole number */
    static boolean isValidInteger(String text) {
        if (text.isBlank()) {
            return true;
        }
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** @return whether the text is blank (no value) or a finite decimal number */
    static boolean isValidDecimal(String text) {
        if (text.isBlank()) {
            return true;
        }
        try {
            return Double.isFinite(Double.parseDouble(text.trim()));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static Integer textToInt(String text) {
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Double textToDouble(String text) {
        if (text.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
