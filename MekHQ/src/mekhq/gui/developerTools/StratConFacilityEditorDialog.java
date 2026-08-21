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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityManifest;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.gui.FileDialogs;

/**
 * A developer tool for editing StratCon facility files (JSON). Exposes the persisted fields with New / Load / Save,
 * mirroring the other Developer Tools editors. Biomes are edited through a modal sub-editor.
 */
public class StratConFacilityEditorDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private static final String FACILITY_MANIFEST_FILE_NAME = "facilitymanifest.json";

    private final JFrame frame;
    private StratConFacility facility = new StratConFacility();
    // The file the current facility was last loaded from or saved to; null until then. Registering with the manifest
    // needs a concrete file name, so it stays disabled until this is set.
    private File currentFile;

    private final JComboBox<ForceAlignment> cboOwner = new JComboBox<>(ForceAlignment.values());
    private final JComboBox<FacilityType> cboFacilityType = new JComboBox<>(FacilityType.values());
    private final JTextField txtDisplayableName = new JTextField(30);
    private final JTextArea txtUserDescription = new JTextArea(4, 40);
    private final JCheckBox chkVisible = new JCheckBox();
    private final JCheckBox chkIsAvailable = new JCheckBox();
    private final JTextArea txtSharedModifiers = new JTextArea(3, 40);
    private final JTextArea txtLocalModifiers = new JTextArea(3, 40);
    private final JTextField txtCapturedDefinition = new JTextField(30);
    private final JCheckBox chkRevealTrack = new JCheckBox();
    private final JCheckBox chkIncreaseScanRange = new JCheckBox();
    private final JSpinner spnScenarioOddsModifier = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JSpinner spnMonthlySPModifier = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
    private final JCheckBox chkPreventAerospace = new JCheckBox();
    private final JCheckBox chkStrategicObjective = new JCheckBox();
    private final DefaultListModel<StratConBiome> biomeModel = new DefaultListModel<>();
    private final JList<StratConBiome> lstBiomes = new JList<>(biomeModel);
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));

    public StratConFacilityEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, "facilityEditor.title"));
        setLayout(new BorderLayout());
        add(new FastJScrollPane(buildForm()), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
        load(facility);
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 5, 3, 5);

        txtUserDescription.setLineWrap(true);
        txtUserDescription.setWrapStyleWord(true);
        txtSharedModifiers.setLineWrap(false);
        txtLocalModifiers.setLineWrap(false);
        lstBiomes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstBiomes.setVisibleRowCount(4);

        addRow(panel, constraints, "facilityEditor.owner", cboOwner);
        addRow(panel, constraints, "facilityEditor.displayableName", txtDisplayableName);
        addRow(panel, constraints, "facilityEditor.facilityType", cboFacilityType);
        addRow(panel, constraints, "facilityEditor.userDescription", new FastJScrollPane(txtUserDescription));
        addRow(panel, constraints, "facilityEditor.visible", chkVisible);
        addRow(panel, constraints, "facilityEditor.isAvailable", chkIsAvailable);
        addRow(panel, constraints, "facilityEditor.sharedModifiers", new FastJScrollPane(txtSharedModifiers));
        addRow(panel, constraints, "facilityEditor.localModifiers", new FastJScrollPane(txtLocalModifiers));
        addRow(panel, constraints, "facilityEditor.capturedDefinition", txtCapturedDefinition);
        addRow(panel, constraints, "facilityEditor.revealTrack", chkRevealTrack);
        addRow(panel, constraints, "facilityEditor.increaseScanRange", chkIncreaseScanRange);
        addRow(panel, constraints, "facilityEditor.scenarioOddsModifier", spnScenarioOddsModifier);
        addRow(panel, constraints, "facilityEditor.monthlySPModifier", spnMonthlySPModifier);
        addRow(panel, constraints, "facilityEditor.preventAerospace", chkPreventAerospace);
        addRow(panel, constraints, "facilityEditor.strategicObjective", chkStrategicObjective);

        // biomes list with add/edit/remove
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "facilityEditor.biomes")), constraints);
        constraints.gridx = 1;
        panel.add(new FastJScrollPane(lstBiomes), constraints);

        JPanel biomeButtons = new JPanel();
        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "button.add"));
        btnAdd.addActionListener(e -> editBiome(null));
        JButton btnEdit = new JButton(getTextAt(RESOURCE_BUNDLE, "button.edit"));
        btnEdit.addActionListener(e -> {
            StratConBiome selected = lstBiomes.getSelectedValue();
            if (selected != null) {
                editBiome(selected);
            }
        });
        JButton btnRemove = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
        btnRemove.addActionListener(e -> {
            int idx = lstBiomes.getSelectedIndex();
            if (idx >= 0) {
                biomeModel.remove(idx);
            }
        });
        biomeButtons.add(btnAdd);
        biomeButtons.add(btnEdit);
        biomeButtons.add(btnRemove);
        constraints.gridx = 2;
        panel.add(biomeButtons, constraints);

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, String labelKey, java.awt.Component control) {
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
            facility = new StratConFacility();
            currentFile = null;
            load(facility);
            updateManifestButtonState();
        });
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.addActionListener(e -> loadFromFile());
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(e -> saveToFile());
        btnAddToManifest.addActionListener(e -> addToManifest());
        btnAddToManifest.setToolTipText(getTextAt(RESOURCE_BUNDLE, "facilityEditor.addToManifest.tooltip"));
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

    private void load(StratConFacility source) {
        cboOwner.setSelectedItem(source.getOwner());
        txtDisplayableName.setText(nullToEmpty(source.getDisplayableName()));
        cboFacilityType.setSelectedItem(source.getFacilityType());
        txtUserDescription.setText(nullToEmpty(source.getUserDescription()));
        chkVisible.setSelected(source.getVisible());
        chkIsAvailable.setSelected(source.getIsAvailable());
        txtSharedModifiers.setText(String.join("\n", source.getSharedModifiers()));
        txtLocalModifiers.setText(String.join("\n", source.getLocalModifiers()));
        txtCapturedDefinition.setText(nullToEmpty(source.getCapturedDefinition()));
        chkRevealTrack.setSelected(source.getRevealTrack());
        chkIncreaseScanRange.setSelected(source.getIncreaseScanRange());
        spnScenarioOddsModifier.setValue(source.getScenarioOddsModifier());
        spnMonthlySPModifier.setValue(source.getMonthlySPModifier());
        chkPreventAerospace.setSelected(source.preventAerospace());
        chkStrategicObjective.setSelected(source.isStrategicObjective());
        biomeModel.clear();
        source.getBiomes().forEach(biomeModel::addElement);
    }

    private void writeInto(StratConFacility target) {
        target.setOwner((ForceAlignment) cboOwner.getSelectedItem());
        target.setDisplayableName(emptyToNull(txtDisplayableName.getText()));
        target.setFacilityType((FacilityType) cboFacilityType.getSelectedItem());
        target.setUserDescription(emptyToNull(txtUserDescription.getText()));
        target.setVisible(chkVisible.isSelected());
        target.setIsAvailable(chkIsAvailable.isSelected());
        target.setSharedModifiers(parseLines(txtSharedModifiers.getText()));
        target.setLocalModifiers(parseLines(txtLocalModifiers.getText()));
        target.setCapturedDefinition(emptyToNull(txtCapturedDefinition.getText()));
        target.setRevealTrack(chkRevealTrack.isSelected());
        target.setIncreaseScanRange(chkIncreaseScanRange.isSelected());
        target.setScenarioOddsModifier((int) spnScenarioOddsModifier.getValue());
        target.setMonthlySPModifier((int) spnMonthlySPModifier.getValue());
        target.setPreventAerospace(chkPreventAerospace.isSelected());
        target.setStrategicObjective(chkStrategicObjective.isSelected());
        List<StratConBiome> biomes = new ArrayList<>();
        for (int i = 0; i < biomeModel.size(); i++) {
            biomes.add(biomeModel.get(i));
        }
        target.setBiomes(biomes);
    }

    private void loadFromFile() {
        File file = FileDialogs.openStratConFacility(frame).orElse(null);
        if (file == null) {
            return;
        }
        StratConFacility loaded = StratConFacility.deserialize(file.getPath());
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "loadError.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        facility = loaded;
        currentFile = file;
        load(facility);
        updateManifestButtonState();
    }

    private void saveToFile() {
        writeInto(facility);
        FileDialogs.saveStratConFacility(frame, facility).ifPresent(file -> {
            facility.Serialize(file);
            currentFile = file;
            updateManifestButtonState();
        });
    }

    /**
     * Registers the current facility's file name in the facility manifest that sits alongside it, so the game will load
     * it. Reads the sibling {@code facilitymanifest.json} (creating a fresh one if absent), appends the file name if it
     * is not already listed, and writes the manifest back.
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), FACILITY_MANIFEST_FILE_NAME);

        StratConFacilityManifest manifest = StratConFacilityManifest.deserialize(manifestFile.getPath());
        if (manifest == null) {
            manifest = new StratConFacilityManifest();
        }

        if (manifest.facilityFileNames.contains(fileName)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.alreadyPresent.message", fileName),
                  getTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        manifest.facilityFileNames.add(fileName);
        if (manifest.serialize(manifestFile)) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.added.message", fileName),
                  getTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.title"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.error.message"),
                  getTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editBiome(StratConBiome existing) {
        StratConBiome target = (existing != null) ? existing : new StratConBiome();
        boolean saved = new StratConBiomeEditDialog(this, target).showDialog();
        if (saved && existing == null) {
            biomeModel.addElement(target);
        } else if (saved) {
            lstBiomes.repaint();
        }
    }

    private static String nullToEmpty(String value) {
        return (value == null) ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value.isBlank() ? null : value;
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
