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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.ui.FastJScrollPane;

/**
 * Editor for a force's role choices. The user selects one or more {@link MissionRole}s (filtered to those valid for the
 * force's unit type) and adds them as a "role set"; each stored role set is a comma-separated group applied together,
 * and one is chosen at random when the force is generated.
 *
 * <p>Extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). The unit type is supplied via
 * {@link #setAllowedUnitType(int)} rather than read from a combo box, so the panel is self-contained and
 * headless-testable. {@link #load}/{@link #getRoleSets} are the model contract.
 */
public class RoleSetEditorPanel extends JPanel {

    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(
          "mekhq.resources.ScenarioTemplateEditorDialog");

    private final JList<MissionRole> lstRolePicker = new JList<>();
    private final DefaultListModel<String> roleSetsModel = new DefaultListModel<>();
    private final JList<String> lstRoleSets = new JList<>(roleSetsModel);

    public RoleSetEditorPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(RESOURCES.getString("RoleSetEditorPanel.title")));

        lstRolePicker.setModel(new DefaultListModel<>());
        lstRolePicker.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstRolePicker.setCellRenderer(new RoleDisplayRenderer());
        lstRoleSets.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstRoleSets.setCellRenderer(new RoleSetDisplayRenderer());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        add(new JLabel(RESOURCES.getString("RoleSetEditorPanel.validRoles.label")), gbc);

        FastJScrollPane scrRolePicker = new FastJScrollPane(lstRolePicker);
        scrRolePicker.setPreferredSize(new Dimension(190, 120));
        gbc.gridy++;
        add(scrRolePicker, gbc);

        JButton btnAddRoleSet = new JButton(RESOURCES.getString("RoleSetEditorPanel.addRoleSet"));
        btnAddRoleSet.addActionListener(evt -> addRoleSet());
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        add(btnAddRoleSet, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;
        add(new JLabel(RESOURCES.getString("RoleSetEditorPanel.roleSets.label")), gbc);

        FastJScrollPane scrRoleSets = new FastJScrollPane(lstRoleSets);
        scrRoleSets.setPreferredSize(new Dimension(190, 100));
        gbc.gridy++;
        add(scrRoleSets, gbc);

        JButton btnRemoveRoleSet = new JButton(RESOURCES.getString("RoleSetEditorPanel.removeRoleSet"));
        btnRemoveRoleSet.addActionListener(evt -> removeRoleSet());
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        add(btnRemoveRoleSet, gbc);
    }

    /**
     * Repopulates the role picker with the roles selectable for the given allowed unit type.
     */
    public void setAllowedUnitType(int allowedUnitType) {
        DefaultListModel<MissionRole> model = new DefaultListModel<>();
        MissionRoleChoices.selectableRoles(allowedUnitType).forEach(model::addElement);
        lstRolePicker.setModel(model);
    }

    /**
     * Populates the role-set list from the given stored role-set strings.
     */
    public void load(List<String> roleSets) {
        roleSetsModel.clear();
        roleSets.forEach(roleSetsModel::addElement);
    }

    /**
     * @return the currently defined role sets, as stored strings
     */
    public List<String> getRoleSets() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < roleSetsModel.getSize(); i++) {
            result.add(roleSetsModel.getElementAt(i));
        }
        return result;
    }

    private void addRoleSet() {
        List<MissionRole> selectedRoles = lstRolePicker.getSelectedValuesList();
        if (selectedRoles.isEmpty()) {
            return;
        }
        String entry = MissionRoleChoices.toEntry(selectedRoles);
        if (!roleSetsModel.contains(entry)) {
            roleSetsModel.addElement(entry);
        }
        lstRolePicker.clearSelection();
    }

    private void removeRoleSet() {
        for (String entry : lstRoleSets.getSelectedValuesList()) {
            roleSetsModel.removeElement(entry);
        }
    }

    /** Renders a {@link MissionRole} with underscores shown as spaces. */
    private static class RoleDisplayRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            String text = (value instanceof MissionRole role) ? role.toString().replace('_', ' ')
                                : String.valueOf(value);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }

    /** Renders a stored role-set string using its human-readable role names. */
    private static class RoleSetDisplayRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            String text = (value instanceof String entry) ? MissionRoleChoices.describe(entry) : String.valueOf(value);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
        }
    }
}
