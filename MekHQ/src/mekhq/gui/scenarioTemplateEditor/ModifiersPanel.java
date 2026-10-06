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
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import megamek.common.ui.FastJScrollPane;

/**
 * Editor panel for a scenario template's fixed scenario modifiers: a picker of the available modifier keys plus the
 * accumulated list, with add/remove.
 *
 * <p>Extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). The available modifier keys are supplied by the
 * caller, so the panel has no dependency on the modifier data files and is headless-testable. The selected modifiers
 * are held in a working model and committed to the template only on {@link #writeInto}, matching the other panels.
 */
public class ModifiersPanel extends JPanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private final JComboBox<String> modifierPicker = new JComboBox<>();
    private final DefaultListModel<String> selectedModel = new DefaultListModel<>();
    private final JList<String> selectedList = new JList<>(selectedModel);

    /**
     * @param availableModifierKeys the modifier keys to offer, in display order
     */
    public ModifiersPanel(List<String> availableModifierKeys) {
        super(new GridBagLayout());
        availableModifierKeys.forEach(modifierPicker::addItem);
        selectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.title")), constraints);
        constraints.gridwidth = 1;

        constraints.gridy++;
        modifierPicker.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.picker.tooltip")));
        add(modifierPicker, constraints);

        constraints.gridx++;
        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.add"));
        btnAdd.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.add.tooltip")));
        btnAdd.addActionListener(evt -> addSelectedModifier());
        add(btnAdd, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridheight = 3;
        selectedList.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.selectedList.tooltip")));
        FastJScrollPane scrollPane = new FastJScrollPane(selectedList);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, constraints);

        constraints.gridx++;
        constraints.gridheight = 1;
        JButton btnRemove = new JButton(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.remove"));
        btnRemove.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ModifiersPanel.remove.tooltip")));
        btnRemove.addActionListener(evt -> removeSelectedModifiers());
        add(btnRemove, constraints);
    }

    private void addSelectedModifier() {
        Object selected = modifierPicker.getSelectedItem();
        if ((selected != null) && !selectedModel.contains(selected.toString())) {
            selectedModel.addElement(selected.toString());
        }
    }

    private void removeSelectedModifiers() {
        for (String modifier : selectedList.getSelectedValuesList()) {
            selectedModel.removeElement(modifier);
        }
    }

    /**
     * Populates the selected-modifier list from the given modifiers.
     */
    public void load(List<String> modifiers) {
        selectedModel.clear();
        modifiers.forEach(selectedModel::addElement);
    }

    /**
     * Replaces the given modifier list with the currently selected modifiers.
     */
    public void writeInto(List<String> modifiers) {
        modifiers.clear();
        for (int i = 0; i < selectedModel.getSize(); i++) {
            modifiers.add(selectedModel.getElementAt(i));
        }
    }
}
