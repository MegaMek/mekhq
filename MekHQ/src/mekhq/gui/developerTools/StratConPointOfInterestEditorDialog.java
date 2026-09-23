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
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestBehaviors;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.gui.FileDialogs;

/**
 * A developer tool for editing StratCon point of interest definition files (JSON). Exposes the persisted fields with
 * New / Load / Save / Add to Manifest, mirroring the facility editor.
 *
 * <p>Saved files are not loaded into the running game's registry; the game reads them on its next start, once they
 * are in the manifest.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConPointOfInterestEditorDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private static final String POINT_OF_INTEREST_MANIFEST_FILE_NAME = "pointofinterestmanifest.json";

    private final JFrame frame;
    private StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
    // The file the current definition was last loaded from or saved to; null until then. Registering with the
    // manifest needs a concrete file name, so the button stays disabled until this is set.
    private File currentFile;

    private final JTextField txtTypeId = new JTextField(30);
    private final JTextField txtDisplayableName = new JTextField(30);
    private final JTextArea txtDescription = new JTextArea(4, 40);
    private final JTextField txtImagePath = new JTextField(30);
    // Editable, so a behavior registered only from outside code (and so not listed) can still be typed in.
    private final JComboBox<String> cboBehaviorId = new JComboBox<>(
          StratConPointOfInterestBehaviors.getBehaviorIds().toArray(new String[0]));
    private final JComboBox<ForceAlignment> cboDefaultOwner = new JComboBox<>(buildOwnerModel());
    private final JCheckBox chkOccupiesHex = new JCheckBox();
    private final JCheckBox chkHiddenUntilScouted = new JCheckBox();
    private final JSpinner spnLifespanDays = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    private final JSpinner spnLifespanDieSides = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JCheckBox chkRemoveOnExpiry = new JCheckBox();
    private final JSpinner spnScenarioOddsModifier = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JSpinner spnScanRangeIncrease = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
    private final JCheckBox chkLandOnly = new JCheckBox();
    private final JCheckBox chkAvoidCities = new JCheckBox();
    private final JTextArea txtAllowedTerrainCategories = new JTextArea(3, 40);
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));

    /**
     * @param parent the owning frame
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConPointOfInterestEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.title"));
        setLayout(new BorderLayout());
        add(new FastJScrollPane(buildForm()), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
        load(definition);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Builds the owner choices: neutral (a {@code null} owner) first, then every alignment.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static DefaultComboBoxModel<ForceAlignment> buildOwnerModel() {
        DefaultComboBoxModel<ForceAlignment> ownerModel = new DefaultComboBoxModel<>();
        ownerModel.addElement(null);
        for (ForceAlignment alignment : ForceAlignment.values()) {
            ownerModel.addElement(alignment);
        }
        return ownerModel;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 5, 3, 5);

        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtAllowedTerrainCategories.setLineWrap(false);
        cboBehaviorId.setEditable(true);
        cboDefaultOwner.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                Object shownValue = (value == null) ?
                                          getTextAt(RESOURCE_BUNDLE,
                                                "pointOfInterestDefinitionEditor.defaultOwner.neutral") :
                                          value;
                return super.getListCellRendererComponent(list, shownValue, index, isSelected, cellHasFocus);
            }
        });

        addRow(panel, constraints, "pointOfInterestDefinitionEditor.typeId", txtTypeId);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.displayableName", txtDisplayableName);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.description",
              new FastJScrollPane(txtDescription));
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.imagePath", txtImagePath);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.behaviorId", cboBehaviorId);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.defaultOwner", cboDefaultOwner);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.occupiesHex", chkOccupiesHex);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.hiddenUntilScouted", chkHiddenUntilScouted);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.lifespanDays", spnLifespanDays);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.lifespanDieSides", spnLifespanDieSides);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.removeOnExpiry", chkRemoveOnExpiry);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.scenarioOddsModifier", spnScenarioOddsModifier);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.scanRangeIncrease", spnScanRangeIncrease);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.landOnly", chkLandOnly);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.avoidCities", chkAvoidCities);
        addRow(panel, constraints, "pointOfInterestDefinitionEditor.allowedTerrainCategories",
              new FastJScrollPane(txtAllowedTerrainCategories));
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, String labelKey, Component control) {
        constraints.gridx = 0;
        constraints.gridy++;
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        DeveloperToolsUI.applyRowTooltip(RESOURCE_BUNDLE, labelKey, label, control);
        panel.add(label, constraints);
        constraints.gridx = 1;
        panel.add(control, constraints);
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel();
        JButton btnNew = new JButton(getTextAt(RESOURCE_BUNDLE, "button.new"));
        btnNew.addActionListener(e -> {
            definition = new StratConPointOfInterestDefinition();
            currentFile = null;
            load(definition);
            updateManifestButtonState();
        });
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.addActionListener(e -> loadFromFile());
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(e -> saveToFile());
        btnAddToManifest.addActionListener(e -> addToManifest());
        btnAddToManifest.setToolTipText(getTextAt(RESOURCE_BUNDLE,
              "pointOfInterestDefinitionEditor.addToManifest.tooltip"));
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "button.close"));
        btnClose.addActionListener(e -> dispose());
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

    private void load(StratConPointOfInterestDefinition source) {
        txtTypeId.setText(nullToEmpty(source.getTypeId()));
        txtDisplayableName.setText(nullToEmpty(source.getDisplayableName()));
        txtDescription.setText(nullToEmpty(source.getDescription()));
        txtImagePath.setText(nullToEmpty(source.getImagePath()));
        cboBehaviorId.setSelectedItem(source.getBehaviorId());
        cboDefaultOwner.setSelectedItem(source.getDefaultOwner());
        chkOccupiesHex.setSelected(source.isOccupiesHex());
        chkHiddenUntilScouted.setSelected(source.isHiddenUntilScouted());
        spnLifespanDays.setValue(Math.max(0, source.getLifespanDays()));
        spnLifespanDieSides.setValue(Math.max(0, source.getLifespanDieSides()));
        chkRemoveOnExpiry.setSelected(source.isRemoveOnExpiry());
        spnScenarioOddsModifier.setValue(source.getScenarioOddsModifier());
        spnScanRangeIncrease.setValue(source.getScanRangeIncrease());
        chkLandOnly.setSelected(source.isLandOnly());
        chkAvoidCities.setSelected(source.isAvoidCities());
        txtAllowedTerrainCategories.setText(String.join("\n", source.getAllowedTerrainCategories()));
    }

    private void writeInto(StratConPointOfInterestDefinition target) {
        target.setTypeId(emptyToNull(txtTypeId.getText()));
        target.setDisplayableName(emptyToNull(txtDisplayableName.getText()));
        target.setDescription(emptyToNull(txtDescription.getText()));
        target.setImagePath(emptyToNull(txtImagePath.getText()));
        Object behaviorId = cboBehaviorId.getSelectedItem();
        String trimmedBehaviorId = (behaviorId == null) ? "" : behaviorId.toString().trim();
        target.setBehaviorId(trimmedBehaviorId.isEmpty() ?
                                   StratConPointOfInterestBehaviors.DEFAULT_BEHAVIOR_ID :
                                   trimmedBehaviorId);
        target.setDefaultOwner((ForceAlignment) cboDefaultOwner.getSelectedItem());
        target.setOccupiesHex(chkOccupiesHex.isSelected());
        target.setHiddenUntilScouted(chkHiddenUntilScouted.isSelected());
        target.setLifespanDays((int) spnLifespanDays.getValue());
        target.setLifespanDieSides((int) spnLifespanDieSides.getValue());
        target.setRemoveOnExpiry(chkRemoveOnExpiry.isSelected());
        target.setScenarioOddsModifier((int) spnScenarioOddsModifier.getValue());
        target.setScanRangeIncrease((int) spnScanRangeIncrease.getValue());
        target.setLandOnly(chkLandOnly.isSelected());
        target.setAvoidCities(chkAvoidCities.isSelected());
        target.setAllowedTerrainCategories(parseLines(txtAllowedTerrainCategories.getText()));
    }

    private void loadFromFile() {
        File file = FileDialogs.openStratConPointOfInterest(frame).orElse(null);
        if (file == null) {
            return;
        }
        StratConPointOfInterestDefinition loaded = StratConPointOfInterestDefinition.deserialize(file.getPath());
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "loadError.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        definition = loaded;
        currentFile = file;
        load(definition);
        updateManifestButtonState();
    }

    private void saveToFile() {
        // The registry skips a definition without a type ID, so saving one would only produce a file the game ignores.
        if (txtTypeId.getText().isBlank()) {
            JOptionPane.showMessageDialog(this,
                  getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.missingTypeId.message"),
                  getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        writeInto(definition);
        FileDialogs.saveStratConPointOfInterest(frame, definition).ifPresent(file -> {
            if (!definition.serialize(file)) {
                JOptionPane.showMessageDialog(this,
                      getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.saveError.message"),
                      getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.title"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentFile = file;
            updateManifestButtonState();
        });
    }

    /**
     * Registers the current definition's file name in the point of interest manifest that sits alongside it, so the
     * game will load it. Reads the sibling {@code pointofinterestmanifest.json} (creating a fresh one if absent),
     * appends the file name if it is not already listed, and writes the manifest back.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), POINT_OF_INTEREST_MANIFEST_FILE_NAME);
        String manifestTitle = getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.manifest.title");

        StratConPointOfInterestManifest manifest = StratConPointOfInterestManifest.deserialize(
              manifestFile.getPath());
        if (manifest == null) {
            manifest = new StratConPointOfInterestManifest();
        }

        if (manifest.pointOfInterestFileNames.contains(fileName)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "pointOfInterestDefinitionEditor.manifest.alreadyPresent.message",
                        fileName),
                  manifestTitle, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        manifest.pointOfInterestFileNames.add(fileName);
        if (manifest.serialize(manifestFile)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.manifest.added.message",
                        fileName),
                  manifestTitle, JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                  getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.manifest.error.message"),
                  manifestTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String nullToEmpty(String value) {
        return (value == null) ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }

    private static List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                result.add(line.trim());
            }
        }
        return result;
    }
}
