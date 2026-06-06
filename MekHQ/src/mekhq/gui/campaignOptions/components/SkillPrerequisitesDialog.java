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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSkillPreRequisiteDialog;

/**
 * A compact editor for a special ability's prerequisite skill sets.
 *
 * <p>
 * The skill sets are shown in a single selectable list &mdash; each entry is
 * one set (an OR group of skill plus
 * minimum level) rendered on its own multi-line row. A shared toolbar adds,
 * edits, removes, and clears sets; edit and
 * remove act on the selected row, and double-clicking a row edits it.
 * Individual sets are created and changed through
 * the existing {@link EditSkillPreRequisiteDialog}. Edits are made on a working
 * copy and written back to the ability
 * only when the user confirms.
 * </p>
 */
public class SkillPrerequisitesDialog extends JDialog {
    private static final String CARD_LIST = "list";
    private static final String CARD_EMPTY = "empty";

    private final Vector<SkillPrerequisite> prerequisiteSkills;
    private final DefaultListModel<SkillPrerequisite> skillSetModel = new DefaultListModel<>();
    private final JList<SkillPrerequisite> skillSetList = new JList<>(skillSetModel);
    private final CardLayout listCardLayout = new CardLayout();
    private final JPanel listCardPanel = new JPanel(listCardLayout);

    private JButton editButton;
    private JButton removeButton;
    private JButton clearButton;
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

        getContentPane().add(createTopPanel(), BorderLayout.NORTH);
        getContentPane().add(createListPanel(), BorderLayout.CENTER);
        getContentPane().add(createConfirmationPanel(ability), BorderLayout.SOUTH);

        reloadSkillSets(-1);

        setPreferredSize(new Dimension(UIUtil.scaleForGUI(460), UIUtil.scaleForGUI(420)));
        pack();
    }

    /**
     * Builds the header: a short, muted explanation of the AND/OR matching rules
     * above a single action toolbar
     * (add, edit, remove, clear). Edit and remove are selection-driven, so they
     * live next to the list they act on.
     */
    private JPanel createTopPanel() {
        JLabel hint = new JLabel("<html><div style='width:" + UIUtil.scaleForGUI(420) + "px'>"
                + "A character qualifies when they satisfy <b>every</b> skill set listed below. Within a set, meeting "
                + "<b>any one</b> of its skills is enough.</div></html>");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));

        JButton addButton = new JButton("Add Skill Set");
        addButton.addActionListener(evt -> addSkillSet());
        editButton = new JButton("Edit");
        editButton.addActionListener(evt -> editSelectedSkillSet());
        removeButton = new JButton("Remove");
        removeButton.addActionListener(evt -> removeSelectedSkillSet());
        clearButton = new JButton("Clear All");
        clearButton.addActionListener(evt -> clearSkillSets());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, UIUtil.scaleForGUI(4), 0));
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(removeButton);
        toolbar.add(clearButton);

        JPanel topPanel = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(8)));
        topPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10),
                UIUtil.scaleForGUI(10),
                0,
                UIUtil.scaleForGUI(10)));
        topPanel.add(hint, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);
        return topPanel;
    }

    /**
     * Builds the scrollable skill-set list inside a single bordered frame, with an
     * empty-state message shown in its
     * place while there are no skill sets. Each set renders as its own multi-line
     * OR group; double-clicking one edits
     * it and the look and feel supplies the selection highlight, so there are no
     * per-row borders or buttons.
     */
    private JPanel createListPanel() {
        skillSetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skillSetList.setCellRenderer(new SkillSetRenderer());
        skillSetList.addListSelectionListener(evt -> updateActionStates());
        skillSetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = skillSetList.locationToIndex(evt.getPoint());
                    if (index >= 0 && skillSetList.getCellBounds(index, index).contains(evt.getPoint())) {
                        editSkillSet(index);
                    }
                }
            }
        });

        FastJScrollPane scrollPane = new FastJScrollPane(skillSetList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JLabel emptyLabel = new JLabel("No skill sets yet \u2014 use \u201CAdd Skill Set\u201D to create one.");
        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setOpaque(false);
        emptyPanel.add(emptyLabel);

        // A single line border framing both the list and the empty state replaces the
        // old per-row etched borders.
        listCardPanel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        listCardPanel.add(scrollPane, CARD_LIST);
        listCardPanel.add(emptyPanel, CARD_EMPTY);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, UIUtil.scaleForGUI(10), 0, UIUtil.scaleForGUI(10)));
        wrapper.add(listCardPanel, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Builds the trailing OK/Cancel row. OK writes the working copy back to the
     * ability and is the dialog's default
     * button.
     */
    private JPanel createConfirmationPanel(SpecialAbility ability) {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(evt -> {
            ability.setPrereqSkills(prerequisiteSkills);
            changed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(evt -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING,
                UIUtil.scaleForGUI(4),
                UIUtil.scaleForGUI(8)));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        getRootPane().setDefaultButton(okButton);
        return buttonPanel;
    }

    /**
     * Rebuilds the list model from the working copy, swaps in the empty state when
     * needed, and restores a selection.
     *
     * @param selectedIndex the row to select afterwards, clamped to the model;
     *                      {@code -1} leaves nothing selected
     */
    private void reloadSkillSets(int selectedIndex) {
        skillSetModel.clear();
        for (SkillPrerequisite prerequisite : prerequisiteSkills) {
            skillSetModel.addElement(prerequisite);
        }

        listCardLayout.show(listCardPanel, skillSetModel.isEmpty() ? CARD_EMPTY : CARD_LIST);

        if (selectedIndex >= 0 && !skillSetModel.isEmpty()) {
            int clampedIndex = Math.min(selectedIndex, skillSetModel.size() - 1);
            skillSetList.setSelectedIndex(clampedIndex);
            skillSetList.ensureIndexIsVisible(clampedIndex);
        }

        updateActionStates();
    }

    private void updateActionStates() {
        boolean hasSelection = skillSetList.getSelectedIndex() >= 0;
        editButton.setEnabled(hasSelection);
        removeButton.setEnabled(hasSelection);
        clearButton.setEnabled(!skillSetModel.isEmpty());
    }

    private void addSkillSet() {
        EditSkillPreRequisiteDialog dialog = new EditSkillPreRequisiteDialog(null, new SkillPrerequisite());
        dialog.setVisible(true);
        if (!dialog.wasCancelled() && !dialog.getPrereq().isEmpty()) {
            prerequisiteSkills.add(dialog.getPrereq());
            reloadSkillSets(prerequisiteSkills.size() - 1);
        }
    }

    private void editSelectedSkillSet() {
        int index = skillSetList.getSelectedIndex();
        if (index >= 0) {
            editSkillSet(index);
        }
    }

    private void editSkillSet(int index) {
        EditSkillPreRequisiteDialog dialog = new EditSkillPreRequisiteDialog(null, prerequisiteSkills.get(index));
        dialog.setVisible(true);
        if (!dialog.wasCancelled() && !dialog.getPrereq().isEmpty()) {
            prerequisiteSkills.set(index, dialog.getPrereq());
            reloadSkillSets(index);
        }
    }

    private void removeSelectedSkillSet() {
        int index = skillSetList.getSelectedIndex();
        if (index >= 0) {
            prerequisiteSkills.remove(index);
            reloadSkillSets(index);
        }
    }

    private void clearSkillSets() {
        prerequisiteSkills.clear();
        reloadSkillSets(-1);
    }

    /**
     * Strips the model's enclosing braces so a skill set reads as a plain OR group
     * in the list.
     *
     * @param prerequisite the skill set to describe
     *
     * @return the skill set's text without its surrounding braces
     */
    private static String describeSkillSet(SkillPrerequisite prerequisite) {
        String text = prerequisite.toString().trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    /**
     * Renders each skill set as a padded, multi-line OR group, leaving selection
     * colors to the look and feel.
     */
    private static final class SkillSetRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SkillPrerequisite prerequisite) {
                setText("<html>" + describeSkillSet(prerequisite) + "</html>");
            }
            setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(6),
                    UIUtil.scaleForGUI(8),
                    UIUtil.scaleForGUI(6),
                    UIUtil.scaleForGUI(8)));
            return this;
        }
    }

    /**
     * @return {@code true} if the user confirmed changes that were written back to the ability
     */
    public boolean wasChanged() {
        return changed;
    }
}
