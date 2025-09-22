/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.SkillPrerequisite;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * @author Taharqa
 */
public class EditSpecialAbilityDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(EditSpecialAbilityDialog.class);

    private final SpecialAbility ability;

    private JSpinner spnXP;

    private Vector<String> prerequisiteAbilities;
    private Vector<SkillPrerequisite> prerequisiteSkills;
    private Vector<String> invalidAbilities;
    private Vector<String> removeAbilities;

    private final Map<String, SpecialAbility> allSPAs;

    private JLabel lblPrerequisiteAbility;
    private JLabel lblInvalidAbility;
    private JLabel lblRemoveAbility;

    private boolean cancelled;
    private int currentXP;

    // region Constructors
    @SuppressWarnings("unchecked")
    public EditSpecialAbilityDialog(JFrame parent, SpecialAbility spa, Map<String, SpecialAbility> hash) {
        super(parent, true);
        this.ability = spa;
        this.allSPAs = hash;

        // FIXME: Java is broken, so we had to suppress unchecked warnings for these 4 lines Basically, Vector<E>
        //  .clone() returns an Object instead of a new Vector<E> - DOH!
        prerequisiteAbilities = (Vector<String>) ability.getPrereqAbilities().clone();
        invalidAbilities = (Vector<String>) ability.getInvalidAbilities().clone();
        removeAbilities = (Vector<String>) ability.getRemovedAbilities().clone();
        prerequisiteSkills = (Vector<SkillPrerequisite>) ability.getPrereqSkills().clone();
        cancelled = false;
        currentXP = ability.getCost();
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    // endregion Constructors

    // region Initialization
    private void initComponents() {
        JButton btnOK = new JButton();
        JButton btnClose = new JButton();

        spnXP = new JSpinner(new SpinnerNumberModel(currentXP, -100000, 100000, 1));

        JPanel panXP = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP Cost:"), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panXP.add(spnXP, gridBagConstraints);

        JPanel panAbility = new JPanel(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(new JLabel("<html><b>Prerequisite Abilities</b></html>"), gridBagConstraints);
        JButton btnEditPreparerAbility = getBtnEditPreparerAbility();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(btnEditPreparerAbility, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblPrerequisiteAbility = new JLabel("<html>" + getPrerequisiteAbilityDesc() + "</html>");
        panAbility.add(lblPrerequisiteAbility, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(new JLabel("<html><b>Invalid Abilities</b></html>"), gridBagConstraints);
        JButton btnEditInvalid = new JButton("Edit Invalid Abilities");
        btnEditInvalid.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, invalidAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                invalidAbilities = sad.getSelected();
                lblInvalidAbility.setText("<html>" + getInvalidDesc() + "</html>");
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(btnEditInvalid, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblInvalidAbility = new JLabel("<html>" + getInvalidDesc() + "</html>");
        panAbility.add(lblInvalidAbility, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(new JLabel("<html><b>Removed Abilities</b></html>"), gridBagConstraints);
        JButton btnEditRemove = new JButton("Edit Removed Abilities");
        btnEditRemove.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, removeAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                removeAbilities = sad.getSelected();
                lblRemoveAbility.setText("<html>" + getRemovedDesc() + "</html>");
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbility.add(btnEditRemove, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblRemoveAbility = new JLabel("<html>" + getRemovedDesc() + "</html>");
        panAbility.add(lblRemoveAbility, gridBagConstraints);

        JPanel panMain = new JPanel(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(panXP, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(panAbility, gridBagConstraints);

        JPanel panSkill = createSkillPanel();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(panSkill, gridBagConstraints);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit " + ability.getDisplayName());
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(panMain, BorderLayout.CENTER);

        JPanel panButton = new JPanel(new GridLayout(0, 2));

        btnOK.setText("OK");
        btnOK.setName("btnOK");
        btnOK.addActionListener(evt -> edit());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panButton.add(btnOK, gridBagConstraints);

        btnClose.setText("Cancel");
        btnClose.setName("btnClose");
        btnClose.addActionListener(evt -> cancel());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panButton.add(btnClose, gridBagConstraints);

        getContentPane().add(panButton, BorderLayout.SOUTH);

        pack();
    }

    private JButton getBtnEditPreparerAbility() {
        JButton btnEditPreparerAbility = new JButton("Edit Prerequisite Abilities");
        btnEditPreparerAbility.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, prerequisiteAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                prerequisiteAbilities = sad.getSelected();
                lblPrerequisiteAbility.setText("<html>" + getPrerequisiteAbilityDesc() + "</html>");
                refreshGUI();
            }
        });
        return btnEditPreparerAbility;
    }

    private JPanel createSkillPanel() {
        JPanel panSkill = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panSkill.add(new JLabel("<html><b>Prerequisite Skill Sets</b></html>"), gridBagConstraints);

        JButton btnAddSkillPrerequisite = getBtnAddSkillPrerequisite();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnAddSkillPrerequisite, gridBagConstraints);

        JButton btnClearPrerequisiteSkills = new JButton("Clear Skill Prerequisites");
        btnClearPrerequisiteSkills.addActionListener(evt -> {
            prerequisiteSkills = new Vector<>();
            refreshGUI();
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnClearPrerequisiteSkills, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        JPanel panSkPre;
        JButton btnRemoveSkill;
        JButton btnEditSkill;
        for (int i = 0; i < prerequisiteSkills.size(); i++) {
            SkillPrerequisite skillPrerequisite = prerequisiteSkills.get(i);
            panSkPre = new JPanel(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);
            c.fill = GridBagConstraints.BOTH;
            panSkPre.add(new JLabel("<html>" + skillPrerequisite.toString() + "</html>"), c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.insets = new Insets(2, 2, 2, 2);
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            btnEditSkill = new JButton("Edit");
            btnEditSkill.setActionCommand(Integer.toString(i));
            btnEditSkill.addActionListener(new EditSkillListener());
            panSkPre.add(btnEditSkill, c);

            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 2, 2, 2);
            c.fill = GridBagConstraints.HORIZONTAL;
            btnRemoveSkill = new JButton("Remove");
            btnRemoveSkill.setActionCommand(Integer.toString(i));
            btnRemoveSkill.addActionListener(new RemoveSkillListener());
            panSkPre.add(btnRemoveSkill, c);

            panSkPre.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            if (i >= (prerequisiteSkills.size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            panSkill.add(panSkPre, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        return panSkill;
    }

    private JButton getBtnAddSkillPrerequisite() {
        JButton btnAddSkillPrerequisite = new JButton("Add Skill Prerequisite");
        btnAddSkillPrerequisite.addActionListener(evt -> {
            EditSkillPreRequisiteDialog newSkillPrerequisiteDialog = new EditSkillPreRequisiteDialog(null,
                  new SkillPrerequisite());
            newSkillPrerequisiteDialog.setVisible(true);
            if (!newSkillPrerequisiteDialog.wasCancelled() && !newSkillPrerequisiteDialog.getPrereq().isEmpty()) {
                prerequisiteSkills.add(newSkillPrerequisiteDialog.getPrereq());
                refreshGUI();
            }
        });
        return btnAddSkillPrerequisite;
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditSpecialAbilityDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
    // endregion Initialization

    // region Getters/Setters
    public Map<String, SpecialAbility> getAllSPAs() {
        return allSPAs;
    }

    public Vector<String> getPrerequisiteAbilities() {
        return prerequisiteAbilities;
    }

    public Vector<String> getInvalidAbilities() {
        return invalidAbilities;
    }

    public Vector<String> getRemoveAbilities() {
        return removeAbilities;
    }

    // endregion Getters/Setters

    private void edit() {
        ability.setCost((Integer) spnXP.getModel().getValue());
        ability.setPrereqAbilities(prerequisiteAbilities);
        ability.setInvalidAbilities(invalidAbilities);
        ability.setRemovedAbilities(removeAbilities);
        ability.setPrereqSkills(prerequisiteSkills);
        this.setVisible(false);
    }

    private void cancel() {
        cancelled = true;
        this.setVisible(false);
    }

    private String getPrerequisiteAbilityDesc() {
        StringBuilder toReturn = new StringBuilder();
        for (String prerequisite : getPrerequisiteAbilities()) {
            SpecialAbility prerequisiteAbility = getAllSPAs().get(prerequisite);
            prerequisiteAbility = (prerequisiteAbility == null) ?
                                        SpecialAbility.getOption(prerequisite) :
                                        prerequisiteAbility;
            if (prerequisiteAbility != null) {
                toReturn.append(prerequisiteAbility.getDisplayName()).append("<br>");
            }
        }
        return (toReturn.isEmpty()) ? "None" : toReturn.toString();
    }

    private String getInvalidDesc() {
        StringBuilder toReturn = new StringBuilder();
        for (String invalid : getInvalidAbilities()) {
            SpecialAbility invalidAbility = getAllSPAs().get(invalid);
            invalidAbility = (invalidAbility == null) ? SpecialAbility.getOption(invalid) : invalidAbility;
            if (invalidAbility != null) {
                toReturn.append(invalidAbility.getDisplayName()).append("<br>");
            }
        }
        return (toReturn.isEmpty()) ? "None" : toReturn.toString();
    }

    private String getRemovedDesc() {
        StringBuilder removedDescription = new StringBuilder();
        for (String remove : getRemoveAbilities()) {
            SpecialAbility removeAbility = getAllSPAs().get(remove);
            removeAbility = (removeAbility == null) ? SpecialAbility.getOption(remove) : removeAbility;
            if (removeAbility != null) {
                removedDescription.append(removeAbility.getDisplayName()).append("<br>");
            }
        }
        return (removedDescription.isEmpty()) ? "None" : removedDescription.toString();
    }

    private void refreshGUI() {
        currentXP = (Integer) spnXP.getModel().getValue();
        getContentPane().removeAll();
        initComponents();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void removeSkillPreRequisite(int i) {
        prerequisiteSkills.remove(i);
    }

    private void editSkillPreRequisite(int i) {
        EditSkillPreRequisiteDialog newSkillPrerequisiteDialog = new EditSkillPreRequisiteDialog(null,
              prerequisiteSkills.get(i));
        newSkillPrerequisiteDialog.setVisible(true);
        if (!newSkillPrerequisiteDialog.wasCancelled() && !newSkillPrerequisiteDialog.getPrereq().isEmpty()) {
            prerequisiteSkills.set(i, newSkillPrerequisiteDialog.getPrereq());
            refreshGUI();
        }
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    private class RemoveSkillListener implements ActionListener {

        public RemoveSkillListener() {
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            int id = Integer.parseInt(evt.getActionCommand());
            removeSkillPreRequisite(id);
            refreshGUI();
        }
    }

    private class EditSkillListener implements ActionListener {

        public EditSkillListener() {
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            int id = Integer.parseInt(evt.getActionCommand());
            editSkillPreRequisite(id);
            refreshGUI();
        }
    }
}
