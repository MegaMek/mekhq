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
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;
import mekhq.campaign.digitalGM.stratCon.facility.IStratConFacilityEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityDefinition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.MonthlySupportPointsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.PreventAerospaceEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.RevealTrackEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScanRangeEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.ScenarioOddsEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.SharedModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.UnknownEffect;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityJson;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityProfile;
import mekhq.gui.FileDialogs;

/**
 * A developer tool for editing StratCon facility definition files (JSON), with New / Load / Save, mirroring the other
 * Developer Tools editors. A definition carries one profile for when the player's side holds the facility and one for
 * when the enemy does; each is edited on its own tab. Biomes are edited through a modal sub-editor.
 *
 * <p>Loading a file in the format used before 0.51.01 opens it as a one-sided definition; saving writes the current
 * format.</p>
 */
public class StratConFacilityEditorDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(StratConFacilityEditorDialog.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private static final String FACILITY_MANIFEST_FILE_NAME = "facilitymanifest.json";

    private final JFrame frame;
    private StratConFacilityDefinition definition = new StratConFacilityDefinition();
    // The file the current definition was last loaded from or saved to; null until then. Registering with the
    // manifest needs a concrete file name, so it stays disabled until this is set.
    private File currentFile;

    private final JTextField txtId = new JTextField(30);
    private final JTextField txtDisplayableName = new JTextField(30);
    private final JComboBox<FacilityType> cboFacilityType = new JComboBox<>(FacilityType.values());
    private final DefaultListModel<StratConBiome> biomeModel = new DefaultListModel<>();
    private final JList<StratConBiome> lstBiomes = new JList<>(biomeModel);
    private final ProfilePanel alliedProfilePanel = new ProfilePanel();
    private final ProfilePanel hostileProfilePanel = new ProfilePanel();
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));

    public StratConFacilityEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, "facilityEditor.title"));
        setLayout(new BorderLayout());
        add(new FastJScrollPane(buildForm()), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
        load(definition);
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

        lstBiomes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstBiomes.setVisibleRowCount(4);

        addRow(panel, constraints, "facilityEditor.id", txtId);
        addRow(panel, constraints, "facilityEditor.displayableName", txtDisplayableName);
        addRow(panel, constraints, "facilityEditor.facilityType", cboFacilityType);

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
            int selectedIndex = lstBiomes.getSelectedIndex();
            if (selectedIndex >= 0) {
                biomeModel.remove(selectedIndex);
            }
        });
        biomeButtons.add(btnAdd);
        biomeButtons.add(btnEdit);
        biomeButtons.add(btnRemove);
        constraints.gridx = 2;
        panel.add(biomeButtons, constraints);

        JTabbedPane profileTabs = new JTabbedPane();
        profileTabs.addTab(getTextAt(RESOURCE_BUNDLE, "facilityEditor.alliedProfile"), alliedProfilePanel);
        profileTabs.addTab(getTextAt(RESOURCE_BUNDLE, "facilityEditor.hostileProfile"), hostileProfilePanel);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(profileTabs, constraints);

        return panel;
    }

    private static void addRow(JPanel panel, GridBagConstraints constraints, String labelKey,
          java.awt.Component control) {
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
            definition = new StratConFacilityDefinition();
            currentFile = null;
            load(definition);
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

    /**
     * @return how many effects the definition has that this version does not know, which saving will drop
     */
    private int load(StratConFacilityDefinition source) {
        txtId.setText(nullToEmpty(source.getId()));
        txtDisplayableName.setText(nullToEmpty(source.getDisplayableName()));
        cboFacilityType.setSelectedItem(source.getFacilityType());
        biomeModel.clear();
        source.getBiomes().forEach(biomeModel::addElement);
        return alliedProfilePanel.load(source.getAlliedProfile()) + hostileProfilePanel.load(source.getHostileProfile());
    }

    private void writeInto(StratConFacilityDefinition target) {
        target.setId(emptyToNull(txtId.getText()));
        target.setDisplayableName(emptyToNull(txtDisplayableName.getText()));
        target.setFacilityType((FacilityType) cboFacilityType.getSelectedItem());
        List<StratConBiome> biomes = new ArrayList<>();
        for (int index = 0; index < biomeModel.size(); index++) {
            biomes.add(biomeModel.get(index));
        }
        target.setBiomes(biomes);
        target.setAlliedProfile(alliedProfilePanel.toProfile());
        target.setHostileProfile(hostileProfilePanel.toProfile());
    }

    private void loadFromFile() {
        File file = FileDialogs.openStratConFacility(frame).orElse(null);
        if (file == null) {
            return;
        }

        StratConFacilityDefinition loaded;
        try {
            loaded = StratConFacilityJson.fromFile(file).definition();
        } catch (Exception e) {
            LOGGER.error("Error loading facility definition {}", file.getPath(), e);
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "loadError.message"),
                  getTextAt(RESOURCE_BUNDLE, "loadError.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        definition = loaded;
        currentFile = file;
        int unknownEffectCount = load(definition);
        updateManifestButtonState();

        if (unknownEffectCount > 0) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "facilityEditor.unknownEffects.message", unknownEffectCount),
                  getTextAt(RESOURCE_BUNDLE, "facilityEditor.unknownEffects.title"),
                  JOptionPane.WARNING_MESSAGE);
        }
    }

    private void saveToFile() {
        writeInto(definition);
        FileDialogs.saveStratConFacility(frame, definition).ifPresent(file -> {
            try {
                StratConFacilityJson.toFile(definition, file);
            } catch (Exception e) {
                LOGGER.error("Error saving facility definition {}", file.getPath(), e);
                return;
            }
            currentFile = file;
            updateManifestButtonState();
            StratConFacilityFactory.reloadFacilities();
        });
    }

    /**
     * Registers the current definition's file name in the facility manifest that sits alongside it, so the game will
     * load it. Reads the sibling {@code facilitymanifest.json} (creating a fresh one only if there is none; one that
     * exists but cannot be read is left unchanged), appends the file name if it is not already listed, and writes the
     * manifest back.
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), FACILITY_MANIFEST_FILE_NAME);

        // Start a fresh manifest only when there is none. One that exists but will not load is left alone: writing a
        // new one over it would silently drop every other entry.
        StratConFacilityManifest manifest;
        if (manifestFile.exists()) {
            manifest = StratConFacilityManifest.deserialize(manifestFile.getPath());
            if (manifest == null) {
                JOptionPane.showMessageDialog(this,
                      getFormattedTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.loadError.message",
                            manifestFile.getPath()),
                      getTextAt(RESOURCE_BUNDLE, "facilityEditor.manifest.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
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
            StratConFacilityFactory.reloadFacilities();
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

    /**
     * Edits one profile of a definition: whether it exists, its description, and the effects this editor knows. An
     * effect this version does not know loads without its data, so it cannot be written back; saving drops it.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static class ProfilePanel extends JPanel {
        private final JCheckBox chkHasProfile = new JCheckBox();
        private final JTextArea txtDescription = new JTextArea(4, 40);
        private final JTextArea txtLocalModifiers = new JTextArea(3, 40);
        private final JTextArea txtSharedModifiers = new JTextArea(3, 40);
        private final JSpinner spnScanRange = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        private final JSpinner spnScenarioOddsModifier = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
        private final JSpinner spnMonthlySupportPoints = new JSpinner(new SpinnerNumberModel(0, -1000, 1000, 1));
        private final JCheckBox chkRevealTrack = new JCheckBox();
        private final JCheckBox chkPreventAerospace = new JCheckBox();

        ProfilePanel() {
            super(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(3, 5, 3, 5);

            txtDescription.setLineWrap(true);
            txtDescription.setWrapStyleWord(true);

            addRow(this, constraints, "facilityEditor.hasProfile", chkHasProfile);
            addRow(this, constraints, "facilityEditor.description", new FastJScrollPane(txtDescription));
            addRow(this, constraints, "facilityEditor.localModifiers", new FastJScrollPane(txtLocalModifiers));
            addRow(this, constraints, "facilityEditor.sharedModifiers", new FastJScrollPane(txtSharedModifiers));
            addRow(this, constraints, "facilityEditor.scanRange", spnScanRange);
            addRow(this, constraints, "facilityEditor.scenarioOddsModifier", spnScenarioOddsModifier);
            addRow(this, constraints, "facilityEditor.monthlySPModifier", spnMonthlySupportPoints);
            addRow(this, constraints, "facilityEditor.revealTrack", chkRevealTrack);
            addRow(this, constraints, "facilityEditor.preventAerospace", chkPreventAerospace);
        }

        /**
         * @return how many of the profile's effects this version does not know; they carry none of their data, so
         *       saving drops them
         */
        int load(StratConFacilityProfile profile) {
            chkHasProfile.setSelected(profile != null);
            StratConFacilityProfile source = (profile == null) ? new StratConFacilityProfile() : profile;

            txtDescription.setText(nullToEmpty(source.getDescription()));
            txtLocalModifiers.setText(String.join("\n", source.getLocalModifierIds()));
            txtSharedModifiers.setText(String.join("\n", source.getSharedModifierIds()));
            spnScanRange.setValue(source.getScanRangeIncrease());
            spnScenarioOddsModifier.setValue(source.getScenarioOddsModifier());
            spnMonthlySupportPoints.setValue(source.getMonthlySupportPoints());
            chkRevealTrack.setSelected(source.isRevealingTrack());
            chkPreventAerospace.setSelected(source.isPreventingAerospace());

            int unknownEffectCount = 0;
            for (IStratConFacilityEffect effect : source.getEffects()) {
                if (effect instanceof UnknownEffect) {
                    unknownEffectCount++;
                }
            }
            return unknownEffectCount;
        }

        StratConFacilityProfile toProfile() {
            if (!chkHasProfile.isSelected()) {
                return null;
            }

            List<IStratConFacilityEffect> effects = new ArrayList<>();
            List<String> localModifiers = parseLines(txtLocalModifiers.getText());
            if (!localModifiers.isEmpty()) {
                effects.add(new LocalModifiersEffect(localModifiers));
            }
            List<String> sharedModifiers = parseLines(txtSharedModifiers.getText());
            if (!sharedModifiers.isEmpty()) {
                effects.add(new SharedModifiersEffect(sharedModifiers));
            }
            int scanRange = (int) spnScanRange.getValue();
            if (scanRange != 0) {
                effects.add(new ScanRangeEffect(scanRange));
            }
            int scenarioOddsModifier = (int) spnScenarioOddsModifier.getValue();
            if (scenarioOddsModifier != 0) {
                effects.add(new ScenarioOddsEffect(scenarioOddsModifier));
            }
            int monthlySupportPoints = (int) spnMonthlySupportPoints.getValue();
            if (monthlySupportPoints != 0) {
                effects.add(new MonthlySupportPointsEffect(monthlySupportPoints));
            }
            if (chkRevealTrack.isSelected()) {
                effects.add(new RevealTrackEffect());
            }
            if (chkPreventAerospace.isSelected()) {
                effects.add(new PreventAerospaceEffect());
            }

            return new StratConFacilityProfile(emptyToNull(txtDescription.getText()), effects);
        }
    }
}
