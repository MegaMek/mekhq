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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioTemplate;

/**
 * Editor panel for a scenario template's objectives: the objective list plus add/edit (via {@link ObjectiveEditPanel})
 * and remove.
 *
 * <p>Extracted from {@code ScenarioTemplateEditorDialog} (Phase 4). Objectives are edited in place on the bound
 * template (the child {@link ObjectiveEditPanel} adds them directly), so this panel exposes {@link #load} to bind a
 * template and {@link #refresh}/{@link #removeSelectedObjectives} for list management - all headless-testable. Only the
 * add/edit action opens the child {@code JDialog}, which is not.
 */
public class ObjectivesPanel extends JPanel {

    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(
          "mekhq.resources.ScenarioTemplateEditorDialog");

    private final JList<ScenarioObjective> objectiveList = new JList<>();
    private final JButton btnRemoveObjective = new JButton(RESOURCES.getString("ObjectivesPanel.remove"));

    private ScenarioTemplate template;

    public ObjectivesPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        objectiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectiveList.setVisibleRowCount(5);
        objectiveList.setFixedCellWidth(400);
        objectiveList.addListSelectionListener(
              e -> btnRemoveObjective.setEnabled(!objectiveList.getSelectedValuesList().isEmpty()));

        JButton btnAddEditObjective = new JButton(RESOURCES.getString("ObjectivesPanel.addEdit"));
        btnAddEditObjective.addActionListener(evt -> openObjectiveEditor());

        btnRemoveObjective.addActionListener(e -> removeSelectedObjectives());
        btnRemoveObjective.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel(RESOURCES.getString("ObjectivesPanel.title")), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(new FastJScrollPane(objectiveList), gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridheight = 1;
        add(btnAddEditObjective, gbc);
        gbc.gridy = 3;
        add(btnRemoveObjective, gbc);
    }

    /**
     * Binds the panel to a template and refreshes the objective list.
     */
    public void load(ScenarioTemplate template) {
        this.template = template;
        refresh();
    }

    /**
     * Rebuilds the objective list from the bound template.
     */
    public void refresh() {
        DefaultListModel<ScenarioObjective> model = new DefaultListModel<>();
        if (template != null) {
            template.scenarioObjectives.forEach(model::addElement);
        }
        objectiveList.setModel(model);
    }

    /**
     * @return the number of objectives currently shown in the list (exposed for testing)
     */
    public int getObjectiveListModelSize() {
        return objectiveList.getModel().getSize();
    }

    /**
     * Removes the selected objectives from the bound template.
     */
    public void removeSelectedObjectives() {
        if (template == null) {
            return;
        }
        template.scenarioObjectives.removeAll(objectiveList.getSelectedValuesList());
        btnRemoveObjective.setEnabled(false);
        refresh();
    }

    private void openObjectiveEditor() {
        if (template == null) {
            return;
        }
        ObjectiveEditPanel editPanel = (objectiveList.getSelectedValue() != null)
                                             ?
                                             new ObjectiveEditPanel(template,
                                                   objectiveList.getSelectedValue(),
                                                   this,
                                                   this::refresh)
                                             :
                                             new ObjectiveEditPanel(template, this, this::refresh);
        editPanel.setModal(true);
        editPanel.requestFocus();
        editPanel.setVisible(true);
    }
}
