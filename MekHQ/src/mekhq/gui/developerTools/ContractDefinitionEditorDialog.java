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
import java.util.Map;
import javax.swing.*;

import megamek.codeUtilities.MathUtility;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.gui.FileDialogs;

/**
 * A developer tool for editing StratCon contract definition files (JSON). Exposes the persisted fields with New / Load
 * / Save, mirroring the scenario template editor's role for its data type.
 */
public class ContractDefinitionEditorDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private static final String CONTRACT_MANIFEST_FILE_NAME = "ContractDefinitionManifest.json";

    private final JFrame frame;
    private StratConContractDefinition definition = new StratConContractDefinition();
    // The file the current definition was last loaded from or saved to; null until then. Registering with the manifest
    // needs a concrete file name, so the button stays disabled until this is set.
    private File currentFile;
    private final JButton btnAddToManifest = new JButton(getTextAt(RESOURCE_BUNDLE, "button.addToManifest"));

    private final JTextField txtContractTypeName = new JTextField(30);
    private final JTextArea txtBriefing = new JTextArea(4, 40);
    private final JSpinner spnAlliedFacilityCount = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 0.25));
    private final JSpinner spnHostileFacilityCount = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 0.25));
    private final JCheckBox chkAllowEarlyVictory = new JCheckBox();
    private final JTextField txtScenarioOdds = new JTextField(30);
    private final JTextField txtDeploymentTimes = new JTextField(30);
    private final JTextArea txtGlobalModifiers = new JTextArea(3, 40);
    private final DefaultListModel<ObjectiveParameters> objectiveModel = new DefaultListModel<>();
    private final JList<ObjectiveParameters> lstObjectives = new JList<>(objectiveModel);

    public ContractDefinitionEditorDialog(JFrame parent) {
        super(parent, true);
        this.frame = parent;
        setTitle(getTextAt(RESOURCE_BUNDLE, "contractEditor.title"));
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.CENTER);
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

        txtBriefing.setLineWrap(true);
        txtBriefing.setWrapStyleWord(true);
        txtGlobalModifiers.setLineWrap(true);
        lstObjectives.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstObjectives.setVisibleRowCount(5);

        addRow(panel, constraints, "contractEditor.contractTypeName", txtContractTypeName);
        addRow(panel, constraints, "contractEditor.briefing", new FastJScrollPane(txtBriefing));
        addRow(panel, constraints, "contractEditor.alliedFacilityCount", spnAlliedFacilityCount);
        addRow(panel, constraints, "contractEditor.hostileFacilityCount", spnHostileFacilityCount);
        addRow(panel, constraints, "contractEditor.allowEarlyVictory", chkAllowEarlyVictory);
        addRow(panel, constraints, "contractEditor.scenarioOdds", txtScenarioOdds);
        addRow(panel, constraints, "contractEditor.deploymentTimes", txtDeploymentTimes);
        addRow(panel, constraints, "contractEditor.globalScenarioModifiers", new FastJScrollPane(txtGlobalModifiers));

        // objectives list with add/edit/remove
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "contractEditor.objectiveParameters")), constraints);
        constraints.gridx = 1;
        panel.add(new FastJScrollPane(lstObjectives), constraints);

        JPanel objButtons = new JPanel();
        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "button.add"));
        btnAdd.addActionListener(e -> editObjective(null));
        JButton btnEdit = new JButton(getTextAt(RESOURCE_BUNDLE, "button.edit"));
        btnEdit.addActionListener(e -> {
            ObjectiveParameters selected = lstObjectives.getSelectedValue();
            if (selected != null) {
                editObjective(selected);
            }
        });
        JButton btnRemove = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
        btnRemove.addActionListener(e -> {
            int idx = lstObjectives.getSelectedIndex();
            if (idx >= 0) {
                objectiveModel.remove(idx);
            }
        });
        objButtons.add(btnAdd);
        objButtons.add(btnEdit);
        objButtons.add(btnRemove);
        constraints.gridx = 2;
        panel.add(objButtons, constraints);

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
            definition = new StratConContractDefinition();
            currentFile = null;
            load(definition);
            updateManifestButtonState();
        });
        JButton btnLoad = new JButton(getTextAt(RESOURCE_BUNDLE, "button.load"));
        btnLoad.addActionListener(e -> loadFromFile());
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(e -> saveToFile());
        btnAddToManifest.addActionListener(e -> addToManifest());
        btnAddToManifest.setToolTipText(getTextAt(RESOURCE_BUNDLE, "contractEditor.addToManifest.tooltip"));
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

    private void load(StratConContractDefinition source) {
        txtContractTypeName.setText(nullToEmpty(source.getContractTypeName()));
        txtBriefing.setText(nullToEmpty(source.getBriefing()));
        spnAlliedFacilityCount.setValue(source.getAlliedFacilityCount());
        spnHostileFacilityCount.setValue(source.getHostileFacilityCount());
        chkAllowEarlyVictory.setSelected(source.isAllowEarlyVictory());
        txtScenarioOdds.setText(joinInts(source.getScenarioOdds()));
        txtDeploymentTimes.setText(joinInts(source.getDeploymentTimes()));
        txtGlobalModifiers.setText(String.join("\n", nullToEmptyList(source.getGlobalScenarioModifiers())));
        objectiveModel.clear();
        if (source.getObjectiveParameters() != null) {
            source.getObjectiveParameters().forEach(objectiveModel::addElement);
        }
    }

    private void writeInto(StratConContractDefinition target) {
        target.setContractTypeName(emptyToNull(txtContractTypeName.getText()));
        target.setBriefing(txtBriefing.getText());
        target.setAlliedFacilityCount((double) spnAlliedFacilityCount.getValue());
        target.setHostileFacilityCount((double) spnHostileFacilityCount.getValue());
        target.setAllowEarlyVictory(chkAllowEarlyVictory.isSelected());
        target.setScenarioOdds(parseInts(txtScenarioOdds.getText()));
        target.setDeploymentTimes(parseInts(txtDeploymentTimes.getText()));
        target.setGlobalScenarioModifiers(parseLines(txtGlobalModifiers.getText()));
        List<ObjectiveParameters> objectives = new ArrayList<>();
        for (int i = 0; i < objectiveModel.size(); i++) {
            objectives.add(objectiveModel.get(i));
        }
        target.setObjectiveParameters(objectives);
    }

    private void loadFromFile() {
        File file = FileDialogs.openContractDefinition(frame).orElse(null);
        if (file == null) {
            return;
        }
        StratConContractDefinition loaded = StratConContractDefinition.Deserialize(file);
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
        writeInto(definition);
        FileDialogs.saveContractDefinition(frame, definition).ifPresent(file -> {
            definition.Serialize(file);
            currentFile = file;
            updateManifestButtonState();
        });
    }

    /**
     * Registers the current definition's file name in the {@code ContractDefinitionManifest.json} that sits alongside
     * it, so the game will use it. The manifest maps each {@link ContractObjectiveType} to a file name, so the user is
     * asked which contract type this file should serve; the chosen mapping is then written (overwriting any prior
     * mapping for that type, after confirmation).
     */
    private void addToManifest() {
        if (currentFile == null) {
            return;
        }

        String fileName = currentFile.getName();
        File manifestFile = new File(currentFile.getParentFile(), CONTRACT_MANIFEST_FILE_NAME);

        ContractObjectiveType type = (ContractObjectiveType) JOptionPane.showInputDialog(this,
              getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.pickType.message"),
              getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.title"),
              JOptionPane.QUESTION_MESSAGE, null, ContractObjectiveType.values(), ContractObjectiveType.GARRISON_DUTY);
        if (type == null) {
            return;
        }

        Map<ContractObjectiveType, String> mapping = StratConContractDefinition.readManifestMapping(manifestFile);

        if (fileName.equals(mapping.get(type))) {
            showManifestResult("contractEditor.manifest.alreadyPresent.message", fileName, type,
                  JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String existing = mapping.get(type);
        if (existing != null) {
            int choice = JOptionPane.showConfirmDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.overwrite.message", type.toString(),
                        existing, fileName),
                  getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.title"), JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        mapping.put(type, fileName);
        if (StratConContractDefinition.writeManifestMapping(manifestFile, mapping)) {
            showManifestResult("contractEditor.manifest.added.message",
                  fileName,
                  type,
                  JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.error.message"),
                  getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showManifestResult(String messageKey, String fileName, ContractObjectiveType type, int messageType) {
        JOptionPane.showMessageDialog(this,
              getFormattedTextAt(RESOURCE_BUNDLE, messageKey, fileName, type.toString()),
              getTextAt(RESOURCE_BUNDLE, "contractEditor.manifest.title"), messageType);
    }

    private void editObjective(ObjectiveParameters existing) {
        ObjectiveParameters target = (existing != null) ? existing : new ObjectiveParameters();
        boolean saved = new ObjectiveParameterEditDialog(this, target).showDialog();
        if (saved && existing == null) {
            objectiveModel.addElement(target);
        } else if (saved) {
            lstObjectives.repaint();
        }
    }

    private static String nullToEmpty(String value) {
        return (value == null) ? "" : value;
    }

    private static List<String> nullToEmptyList(List<String> value) {
        return (value == null) ? List.of() : value;
    }

    private static String emptyToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static String joinInts(List<Integer> values) {
        if (values == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private static List<Integer> parseInts(String text) {
        List<Integer> result = new ArrayList<>();
        for (String token : text.split("[,\\s]+")) {
            if (!token.isBlank()) {
                result.add(MathUtility.parseInt(token.trim(), 0));
            }
        }
        return result;
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
