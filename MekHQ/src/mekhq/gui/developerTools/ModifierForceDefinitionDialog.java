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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.gui.scenarioTemplateEditor.ForceEditorPanel;
import mekhq.gui.scenarioTemplateEditor.ScenarioTemplateEditorDialog;

/**
 * Modal sub-editor for a scenario modifier's force definition, used by {@link ScenarioModifierEditorDialog}. It hosts
 * the scenario template editor's {@link ForceEditorPanel}, so both tools edit forces with the same controls and
 * validation.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ModifierForceDefinitionDialog extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.DeveloperTools";

    private final ForceEditorPanel forceEditorPanel =
          new ForceEditorPanel(ScenarioTemplateEditorDialog.readMulFileNames());
    private ScenarioForceTemplate savedForce;

    /**
     * @param parent          the modifier editor
     * @param forceDefinition the force to edit, or {@code null} to start a new one
     * @param knownForceIds   the force IDs the scenario templates define, offered for syncing and objective links
     */
    ModifierForceDefinitionDialog(JDialog parent, @Nullable ScenarioForceTemplate forceDefinition,
          Collection<String> knownForceIds) {
        super(parent, getTextAt(RESOURCE_BUNDLE, "modifierForceEditor.title"), true);
        // closing the window is a cancel
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        forceEditorPanel.setAvailableForceIds(availableForceIds(knownForceIds, forceDefinition));
        forceEditorPanel.configureSaveButton(getTextAt(RESOURCE_BUNDLE, "modifierForceEditor.save"),
              getTextAt(RESOURCE_BUNDLE, "modifierForceEditor.save.tooltip"));
        forceEditorPanel.setOnSave(this::saveForce);
        if (forceDefinition != null) {
            forceEditorPanel.loadForce(forceDefinition);
        }

        setLayout(new BorderLayout());
        add(new FastJScrollPane(forceEditorPanel), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the dialog and waits for it to close.
     *
     * @return the edited force, or empty if the dialog was cancelled
     */
    Optional<ScenarioForceTemplate> showDialog() {
        setVisible(true);
        return Optional.ofNullable(savedForce);
    }

    private JPanel buildButtons() {
        JPanel bar = new JPanel();
        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "button.cancel"));
        btnCancel.addActionListener(e -> dispose());
        bar.add(btnCancel);
        return bar;
    }

    private void saveForce() {
        String validationResult = forceEditorPanel.validateInput();
        if (!validationResult.isEmpty()) {
            JOptionPane.showMessageDialog(this, validationResult,
                  getTextAt(RESOURCE_BUNDLE, "modifierForceEditor.invalidForce.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        savedForce = forceEditorPanel.buildForceTemplate();
        dispose();
    }

    /**
     * Builds the force IDs to offer for syncing and objective links: the known template force IDs plus the primary
     * player force, and any IDs the force already refers to. Keeping the force's own references in the list means
     * loading an unusual force and saving it again does not silently drop them.
     *
     * @param knownForceIds   the force IDs the scenario templates define
     * @param forceDefinition the force being edited, or {@code null} for a new force
     *
     * @return the distinct force IDs, sorted
     */
    static List<String> availableForceIds(Collection<String> knownForceIds,
          @Nullable ScenarioForceTemplate forceDefinition) {
        Set<String> forceIds = new TreeSet<>(knownForceIds);
        forceIds.add(ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID);
        if (forceDefinition != null) {
            addIfPresent(forceIds, forceDefinition.getSyncedForceName());
            if (forceDefinition.getObjectiveLinkedForces() != null) {
                forceDefinition.getObjectiveLinkedForces().forEach(forceId -> addIfPresent(forceIds, forceId));
            }
        }
        return new ArrayList<>(forceIds);
    }

    private static void addIfPresent(Set<String> forceIds, @Nullable String forceId) {
        if ((forceId != null) && !forceId.isBlank()) {
            forceIds.add(forceId);
        }
    }
}
