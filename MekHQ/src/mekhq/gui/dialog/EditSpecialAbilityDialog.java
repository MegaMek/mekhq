/*
 * EditSpecialAbilityDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
import mekhq.campaign.personnel.SkillPerquisite;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * @author Taharqa
 */
public class EditSpecialAbilityDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditSpecialAbilityDialog.class);

    private SpecialAbility ability;

    private JButton btnClose;
    private JButton btnOK;
    private JSpinner spnXP;

    private JButton btnEditPrereqAbil;
    private JButton btnEditInvalid;
    private JButton btnEditRemove;
    private JButton btnClearPrereqSkills;
    private JButton btnAddSkillPerquisite;

    private Vector<String> prereqAbilities;
    private Vector<SkillPerquisite> prereqSkills;
    private Vector<String> invalidAbilities;
    private Vector<String> removeAbilities;

    private Map<String, SpecialAbility> allSPAs;

    private JLabel lblPrereqAbil;
    private JLabel lblInvalidAbil;
    private JLabel lblRemoveAbil;

    private boolean cancelled;
    private int currentXP;

    // region Constructors
    @SuppressWarnings("unchecked")
    public EditSpecialAbilityDialog(JFrame parent, SpecialAbility spa, Map<String, SpecialAbility> hash) {
        super(parent, true);
        this.ability = spa;
        this.allSPAs = hash;
        // FIXME: Java is broken, so we had to suppress unchecked warnings for these 4
        // lines
        // Basically, Vector<E>.clone() returns an Object instead of a new Vector<E> -
        // DOH!
        prereqAbilities = (Vector<String>) ability.getPrereqAbilities().clone();
        invalidAbilities = (Vector<String>) ability.getInvalidAbilities().clone();
        removeAbilities = (Vector<String>) ability.getRemovedAbilities().clone();
        prereqSkills = (Vector<SkillPerquisite>) ability.getPrereqSkills().clone();
        cancelled = false;
        currentXP = ability.getCost();
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    // endregion Constructors

    // region Initialization
    private void initComponents() {
        btnOK = new JButton();
        btnClose = new JButton();

        spnXP = new JSpinner(new SpinnerNumberModel(currentXP, -1, 100000, 1));

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

        JPanel panAbil = new JPanel(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Prerequisite Abilities</b></html>"), gridBagConstraints);
        btnEditPrereqAbil = new JButton("Edit Prereq Abilities");
        btnEditPrereqAbil.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, prereqAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                prereqAbilities = sad.getSelected();
                lblPrereqAbil.setText("<html>" + getPrerequisiteAbilityDesc() + "</html>");
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditPrereqAbil, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblPrereqAbil = new JLabel("<html>" + getPrerequisiteAbilityDesc() + "</html>");
        panAbil.add(lblPrereqAbil, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Invalid Abilities</b></html>"), gridBagConstraints);
        btnEditInvalid = new JButton("Edit Invalid Abilities");
        btnEditInvalid.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, invalidAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                invalidAbilities = sad.getSelected();
                lblInvalidAbil.setText("<html>" + getInvalidDesc() + "</html>");
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditInvalid, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblInvalidAbil = new JLabel("<html>" + getInvalidDesc() + "</html>");
        panAbil.add(lblInvalidAbil, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Removed Abilities</b></html>"), gridBagConstraints);
        btnEditRemove = new JButton("Edit Removed Abilities");
        btnEditRemove.addActionListener(evt -> {
            SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, removeAbilities, allSPAs);
            sad.setVisible(true);
            if (!sad.wasCancelled()) {
                removeAbilities = sad.getSelected();
                lblRemoveAbil.setText("<html>" + getRemovedDesc() + "</html>");
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditRemove, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(1, 10, 1, 1);
        lblRemoveAbil = new JLabel("<html>" + getRemovedDesc() + "</html>");
        panAbil.add(lblRemoveAbil, gridBagConstraints);

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
        panMain.add(panAbil, gridBagConstraints);

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

    private JPanel createSkillPanel() {
        JPanel panSkill = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panSkill.add(new JLabel("<html><b>Prerequisite Skillsets</b></html>"), gridBagConstraints);

        btnAddSkillPerquisite = new JButton("Add Skill Prereq");
        btnAddSkillPerquisite.addActionListener(evt -> {
            EditSkillPerquisiteDialog nspd = new EditSkillPerquisiteDialog(null, new SkillPerquisite());
            nspd.setVisible(true);
            if (!nspd.wasCancelled() && !nspd.getPrereq().isEmpty()) {
                prereqSkills.add(nspd.getPrereq());
                refreshGUI();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnAddSkillPerquisite, gridBagConstraints);

        btnClearPrereqSkills = new JButton("Clear Skill Prereqs");
        btnClearPrereqSkills.addActionListener(evt -> {
            prereqSkills = new Vector<>();
            refreshGUI();
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnClearPrereqSkills, gridBagConstraints);

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
        for (int i = 0; i < prereqSkills.size(); i++) {
            SkillPerquisite skpre = prereqSkills.get(i);
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
            panSkPre.add(new JLabel("<html>" + skpre.toString() + "</html>"), c);

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

            if (i >= (prereqSkills.size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            panSkill.add(panSkPre, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        return panSkill;
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditSpecialAbilityDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }
    // endregion Initialization

    // region Getters/Setters
    public Map<String, SpecialAbility> getAllSPAs() {
        return allSPAs;
    }

    public void setAllSPAs(Map<String, SpecialAbility> allSPAs) {
        this.allSPAs = allSPAs;
    }

    public Vector<String> getPrerequisiteAbilities() {
        return prereqAbilities;
    }

    public void setPrerequisiteAbilities(Vector<String> prerequisiteAbilities) {
        this.prereqAbilities = prerequisiteAbilities;
    }

    public Vector<String> getInvalidAbilities() {
        return invalidAbilities;
    }

    public void setInvalidAbilities(Vector<String> invalidAbilities) {
        this.invalidAbilities = invalidAbilities;
    }

    public Vector<String> getRemoveAbilities() {
        return removeAbilities;
    }

    public void setRemoveAbilities(Vector<String> removeAbilities) {
        this.removeAbilities = removeAbilities;
    }
    // endregion Getters/Setters

    private void edit() {
        ability.setCost((Integer) spnXP.getModel().getValue());
        ability.setPrereqAbilities(prereqAbilities);
        ability.setInvalidAbilities(invalidAbilities);
        ability.setRemovedAbilities(removeAbilities);
        ability.setPrereqSkills(prereqSkills);
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
            prerequisiteAbility = (prerequisiteAbility == null)
                    ? SpecialAbility.getOption(prerequisite)
                    : prerequisiteAbility;
            if (prerequisiteAbility != null) {
                toReturn.append(prerequisiteAbility.getDisplayName()).append("<br>");
            }
        }
        return (toReturn.length() == 0) ? "None" : toReturn.toString();
    }

    private String getInvalidDesc() {
        StringBuilder toReturn = new StringBuilder();
        for (String invalid : getInvalidAbilities()) {
            SpecialAbility invalidAbility = getAllSPAs().get(invalid);
            invalidAbility = (invalidAbility == null)
                    ? SpecialAbility.getOption(invalid)
                    : invalidAbility;
            if (invalidAbility != null) {
                toReturn.append(invalidAbility.getDisplayName()).append("<br>");
            }
        }
        return (toReturn.length() == 0) ? "None" : toReturn.toString();
    }

    private String getRemovedDesc() {
        StringBuilder removedDescription = new StringBuilder();
        for (String remove : getRemoveAbilities()) {
            SpecialAbility removeAbility = getAllSPAs().get(remove);
            removeAbility = (removeAbility == null)
                    ? SpecialAbility.getOption(remove)
                    : removeAbility;
            if (removeAbility != null) {
                removedDescription.append(removeAbility.getDisplayName()).append("<br>");
            }
        }
        return (removedDescription.length() == 0) ? "None" : removedDescription.toString();
    }

    private void refreshGUI() {
        currentXP = (Integer) spnXP.getModel().getValue();
        getContentPane().removeAll();
        initComponents();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void removeSkillPerquisite(int i) {
        prereqSkills.remove(i);
    }

    private void editSkillPerquisite(int i) {
        EditSkillPerquisiteDialog nspd = new EditSkillPerquisiteDialog(null, prereqSkills.get(i));
        nspd.setVisible(true);
        if (!nspd.wasCancelled() && !nspd.getPrereq().isEmpty()) {
            prereqSkills.set(i, nspd.getPrereq());
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
            removeSkillPerquisite(id);
            refreshGUI();
        }
    }

    private class EditSkillListener implements ActionListener {

        public EditSkillListener() {
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            int id = Integer.parseInt(evt.getActionCommand());
            editSkillPerquisite(id);
            refreshGUI();
        }
    }
}
