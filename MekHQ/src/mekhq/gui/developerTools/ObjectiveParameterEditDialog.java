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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;

/**
 * Modal sub-editor for a single {@link ObjectiveParameters}, used by {@link ContractDefinitionEditorDialog}.
 */
class ObjectiveParameterEditDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private final ObjectiveParameters objective;
    private boolean saved = false;

    private final JComboBox<StrategicObjectiveType> cboType = new JComboBox<>(StrategicObjectiveType.values());
    private final JSpinner spnCount = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 0.25));
    private final JTextArea txtScenarios = new JTextArea(4, 30);
    private final JTextArea txtModifiers = new JTextArea(4, 30);

    ObjectiveParameterEditDialog(JDialog parent, ObjectiveParameters objective) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "objectiveEditor.title"), true);
        this.objective = objective;
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        cboType.addActionListener(e -> syncCountEnablement());
        load();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Enables or disables the objective-count field for the selected objective type. Specific-scenario objectives no
     * longer take a count here: how many appear is driven by the contract's scenario schedule (the Track Intensity
     * Table), so the field is disabled and pinned to zero for that type.
     */
    private void syncCountEnablement() {
        boolean specificScenarioVictory = cboType.getSelectedItem() == StrategicObjectiveType.SpecificScenarioVictory;
        if (specificScenarioVictory) {
            spnCount.setValue(0.0);
        }
        spnCount.setEnabled(!specificScenarioVictory);
    }

    boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5);

        txtScenarios.setLineWrap(false);
        txtModifiers.setLineWrap(false);

        addRow(panel, gbc, "objectiveEditor.objectiveType", cboType);
        addRow(panel, gbc, "objectiveEditor.objectiveCount", spnCount);
        addRow(panel, gbc, "objectiveEditor.objectiveScenarios", new FastJScrollPane(txtScenarios));
        addRow(panel, gbc, "objectiveEditor.objectiveScenarioModifiers", new FastJScrollPane(txtModifiers));
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, String labelKey, java.awt.Component control) {
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel label = new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey));
        DeveloperToolsUI.applyRowTooltip(RESOURCE_BUNDLE, labelKey, label, control);
        panel.add(label, gbc);
        gbc.gridx = 1;
        panel.add(control, gbc);
    }

    private JPanel buildButtons() {
        JPanel bar = new JPanel();
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(e -> {
            writeInto();
            saved = true;
            dispose();
        });
        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "button.cancel"));
        btnCancel.addActionListener(e -> dispose());
        bar.add(btnSave);
        bar.add(btnCancel);
        return bar;
    }

    private void load() {
        cboType.setSelectedItem(objective.getObjectiveType());
        spnCount.setValue(objective.getObjectiveCount());
        txtScenarios.setText(String.join("\n", objective.getObjectiveScenarios()));
        txtModifiers.setText(String.join("\n", objective.getObjectiveScenarioModifiers()));
        // Apply after loading the raw value so a specific-scenario objective is pinned to zero and disabled.
        syncCountEnablement();
    }

    private void writeInto() {
        objective.setObjectiveType((StrategicObjectiveType) cboType.getSelectedItem());
        objective.setObjectiveCount((double) spnCount.getValue());
        replaceContents(objective.getObjectiveScenarios(), parseLines(txtScenarios.getText()));
        replaceContents(objective.getObjectiveScenarioModifiers(), parseLines(txtModifiers.getText()));
    }

    private static void replaceContents(List<String> target, List<String> values) {
        target.clear();
        target.addAll(values);
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
