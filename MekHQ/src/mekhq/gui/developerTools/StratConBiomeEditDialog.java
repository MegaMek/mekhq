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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiome;

/**
 * Modal sub-editor for a single {@link StratConBiome}, used by {@link StratConFacilityEditorDialog}.
 */
class StratConBiomeEditDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private final StratConBiome biome;
    private boolean saved = false;

    private final JTextField txtBiomeCategory = new JTextField(20);
    private final JSpinner spnLowerBound = new JSpinner(new SpinnerNumberModel(0, -1000, 100000, 1));
    private final JSpinner spnUpperBound = new JSpinner(new SpinnerNumberModel(0, -1000, 100000, 1));
    private final JTextArea txtTerrainTypes = new JTextArea(4, 20);

    StratConBiomeEditDialog(JDialog parent, StratConBiome biome) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "biomeEditor.title"), true);
        this.biome = biome;
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        load();
        pack();
        setLocationRelativeTo(parent);
    }

    boolean showDialog() {
        setVisible(true);
        return saved;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 5, 3, 5);

        addRow(panel, constraints, "biomeEditor.biomeCategory", txtBiomeCategory);
        addRow(panel, constraints, "biomeEditor.allowedTemperatureLowerBound", spnLowerBound);
        addRow(panel, constraints, "biomeEditor.allowedTemperatureUpperBound", spnUpperBound);
        addRow(panel, constraints, "biomeEditor.allowedTerrainTypes", new FastJScrollPane(txtTerrainTypes));
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints constraints, String labelKey, java.awt.Component control) {
        constraints.gridx = 0;
        constraints.gridy++;
        panel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, labelKey)), constraints);
        constraints.gridx = 1;
        panel.add(control, constraints);
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
        txtBiomeCategory.setText((biome.biomeCategory == null) ? "" : biome.biomeCategory);
        spnLowerBound.setValue(biome.allowedTemperatureLowerBound);
        spnUpperBound.setValue(biome.allowedTemperatureUpperBound);
        txtTerrainTypes.setText((biome.allowedTerrainTypes == null) ?
                                      "" :
                                      String.join("\n", biome.allowedTerrainTypes));
    }

    private void writeInto() {
        biome.biomeCategory = txtBiomeCategory.getText().isBlank() ? null : txtBiomeCategory.getText().trim();
        biome.allowedTemperatureLowerBound = (int) spnLowerBound.getValue();
        biome.allowedTemperatureUpperBound = (int) spnUpperBound.getValue();
        biome.allowedTerrainTypes = parseLines(txtTerrainTypes.getText());
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
