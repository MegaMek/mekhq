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
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import org.jspecify.annotations.NonNull;

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

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";

    private final JList<ScenarioObjective> objectiveList = new JList<>();
    private final JButton btnRemoveObjective = new JButton(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.remove"));

    private ScenarioTemplate template;

    public ObjectivesPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    private void initComponents() {
        objectiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectiveList.setVisibleRowCount(5);
        objectiveList.setFixedCellWidth(scaleForGUI(400));
        objectiveList.addListSelectionListener(
              evt -> btnRemoveObjective.setEnabled(!objectiveList.getSelectedValuesList().isEmpty()));
        objectiveList.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.list.tooltip")));

        JButton btnAddEditObjective = new JButton(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.addEdit"));
        btnAddEditObjective.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.addEdit.tooltip")));
        btnAddEditObjective.addActionListener(evt -> openObjectiveEditor());

        btnRemoveObjective.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.remove.tooltip")));
        btnRemoveObjective.addActionListener(evt -> removeSelectedObjectives());
        btnRemoveObjective.setEnabled(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, scaleForGUI(5));

        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectivesPanel.title")), constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        add(new FastJScrollPane(objectiveList), constraints);

        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridheight = 1;
        add(btnAddEditObjective, constraints);
        constraints.gridy = 3;
        add(btnRemoveObjective, constraints);
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
        ObjectiveEditPanel editPanel = objectiveList.getSelectedValue() != null ?
                                             createObjectiveEditPanel() :
                                             new ObjectiveEditPanel(template, this, this::refresh);
        editPanel.setModal(true);
        editPanel.requestFocus();
        editPanel.setVisible(true);
    }

    private @NonNull ObjectiveEditPanel createObjectiveEditPanel() {
        return new ObjectiveEditPanel(template, objectiveList.getSelectedValue(), this, this::refresh);
    }
}
