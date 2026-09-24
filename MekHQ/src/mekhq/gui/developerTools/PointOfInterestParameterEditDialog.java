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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;

/**
 * Modal sub-editor for a single {@link PointOfInterestParameters} - a point of interest a contract places outside its
 * strategic objectives - used by {@link ContractDefinitionEditorDialog}.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PointOfInterestParameterEditDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private final PointOfInterestParameters pointOfInterest;
    private boolean saved = false;

    // Editable, so a type registered only from code (and so not listed) can still be typed in.
    private final JComboBox<String> cboTypeId = new JComboBox<>(DeveloperToolsUI.getKnownPointOfInterestTypeIds());
    private final JSpinner spnCount = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 0.25));

    /**
     * @param parent          the owning dialog
     * @param pointOfInterest the entry to edit; changed only if the user saves
     *
     * @author Illiani
     * @since 0.51.01
     */
    PointOfInterestParameterEditDialog(JDialog parent, PointOfInterestParameters pointOfInterest) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "pointOfInterestEditor.title"), true);
        this.pointOfInterest = pointOfInterest;
        setLayout(new BorderLayout());
        add(buildForm(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        load();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the dialog and waits for it to close.
     *
     * @return {@code true} if the user saved their changes
     *
     * @author Illiani
     * @since 0.51.01
     */
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

        cboTypeId.setEditable(true);

        addRow(panel, constraints, "pointOfInterestEditor.typeId", cboTypeId);
        addRow(panel, constraints, "pointOfInterestEditor.count", spnCount);
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

    private JPanel buildButtons() {
        JPanel bar = new JPanel();
        JButton btnSave = new JButton(getTextAt(RESOURCE_BUNDLE, "button.save"));
        btnSave.addActionListener(event -> {
            // An empty type ID would save as null and list as "null", so it is refused, as the definition editor does.
            if (getTrimmedTypeId().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                      getTextAt(RESOURCE_BUNDLE, "pointOfInterestDefinitionEditor.missingTypeId.message"),
                      getTextAt(RESOURCE_BUNDLE, "pointOfInterestEditor.title"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            writeInto();
            saved = true;
            dispose();
        });
        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "button.cancel"));
        btnCancel.addActionListener(event -> dispose());
        bar.add(btnSave);
        bar.add(btnCancel);
        return bar;
    }

    private void load() {
        cboTypeId.setSelectedItem(pointOfInterest.getTypeId());
        spnCount.setValue(pointOfInterest.getCount());
    }

    private String getTrimmedTypeId() {
        Object typeId = cboTypeId.getSelectedItem();
        return (typeId == null) ? "" : typeId.toString().trim();
    }

    private void writeInto() {
        pointOfInterest.setTypeId(getTrimmedTypeId());
        pointOfInterest.setCount((double) spnCount.getValue());
    }
}
