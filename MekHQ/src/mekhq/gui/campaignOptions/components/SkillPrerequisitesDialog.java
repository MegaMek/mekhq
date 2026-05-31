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
package mekhq.gui.campaignOptions.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSkillPreRequisiteDialog;

/**
 * A compact editor for a special ability's prerequisite skill sets.
 *
 * <p>Each row is one skill set (an AND/OR group of skill + minimum level), shown with edit and remove controls. New
 * sets are added through the existing {@link EditSkillPreRequisiteDialog}. The working copy is written back to the
 * ability only when the user confirms.</p>
 */
public class SkillPrerequisitesDialog extends JDialog {
    private final Vector<SkillPrerequisite> prerequisiteSkills;
    private final JPanel listPanel = new JPanel();

    private boolean changed;

    /**
     * Opens a modal editor for the supplied ability's prerequisite skill sets.
     *
     * @param parent  the window to center on, may be {@code null}
     * @param ability the ability whose prerequisite skills are edited
     */
    @SuppressWarnings("unchecked")
    public SkillPrerequisitesDialog(Window parent, SpecialAbility ability) {
        super(parent, "Edit Skill Requirements", ModalityType.APPLICATION_MODAL);
        // Work on a clone so a cancel leaves the ability untouched.
        this.prerequisiteSkills = (Vector<SkillPrerequisite>) ability.getPrereqSkills().clone();
        this.changed = false;
        initComponents(ability);
        setLocationRelativeTo(parent);
    }

    private void initComponents(SpecialAbility ability) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        JButton btnAdd = new JButton("Add Skill Set");
        btnAdd.addActionListener(evt -> addSkillSet());
        JButton btnClear = new JButton("Clear All");
        btnClear.addActionListener(evt -> {
            prerequisiteSkills.clear();
            rebuildList();
        });
        JPanel topPanel = new JPanel();
        topPanel.add(btnAdd);
        topPanel.add(btnClear);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8)));
        rebuildList();
        getContentPane().add(new FastJScrollPane(listPanel), BorderLayout.CENTER);

        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(evt -> {
            ability.setPrereqSkills(prerequisiteSkills);
            changed = true;
            dispose();
        });
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(evt -> dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(UIUtil.scaleForGUI(420), UIUtil.scaleForGUI(360)));
        pack();
    }

    private void rebuildList() {
        listPanel.removeAll();

        if (prerequisiteSkills.isEmpty()) {
            listPanel.add(new JLabel("No skill requirements."));
        }

        for (int i = 0; i < prerequisiteSkills.size(); i++) {
            final int index = i;
            SkillPrerequisite skillPrerequisite = prerequisiteSkills.get(i);

            JPanel row = new JPanel(new BorderLayout(UIUtil.scaleForGUI(8), 0));
            row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                  BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4),
                        UIUtil.scaleForGUI(4),
                        UIUtil.scaleForGUI(4),
                        UIUtil.scaleForGUI(4))));
            row.add(new JLabel("<html>" + skillPrerequisite + "</html>"), BorderLayout.CENTER);

            JButton btnEdit = new JButton("Edit");
            btnEdit.addActionListener(evt -> editSkillSet(index));
            JButton btnRemove = new JButton("Remove");
            btnRemove.addActionListener(evt -> {
                prerequisiteSkills.remove(index);
                rebuildList();
            });
            JPanel rowButtons = new JPanel();
            rowButtons.add(btnEdit);
            rowButtons.add(btnRemove);
            row.add(rowButtons, BorderLayout.EAST);

            listPanel.add(row);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addSkillSet() {
        EditSkillPreRequisiteDialog dialog = new EditSkillPreRequisiteDialog(null, new SkillPrerequisite());
        dialog.setVisible(true);
        if (!dialog.wasCancelled() && !dialog.getPrereq().isEmpty()) {
            prerequisiteSkills.add(dialog.getPrereq());
            rebuildList();
        }
    }

    private void editSkillSet(int index) {
        EditSkillPreRequisiteDialog dialog = new EditSkillPreRequisiteDialog(null, prerequisiteSkills.get(index));
        dialog.setVisible(true);
        if (!dialog.wasCancelled() && !dialog.getPrereq().isEmpty()) {
            prerequisiteSkills.set(index, dialog.getPrereq());
            rebuildList();
        }
    }

    /**
     * @return {@code true} if the user confirmed changes that were written back to the ability
     */
    public boolean wasChanged() {
        return changed;
    }
}
