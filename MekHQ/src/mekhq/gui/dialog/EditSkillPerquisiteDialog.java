/*
 * NewSkillPerquisiteDialog.java
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Hashtable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.SkillPerquisite;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Taharqa
 */
public class EditSkillPerquisiteDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditSkillPerquisiteDialog.class);

    private SkillPerquisite prereq;

    private JButton btnClose;
    private JButton btnOK;
    private boolean cancelled;

    private Hashtable<String, JComboBox<SkillLevel>> skillLevels = new Hashtable<>();
    private Hashtable<String, JCheckBox> skillChks = new Hashtable<>();

    public EditSkillPerquisiteDialog(final JFrame frame, final SkillPerquisite pre) {
        super(frame, true);
        cancelled = false;
        prereq = pre;
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        btnOK = new JButton();
        btnClose = new JButton();

        JPanel panMain = new JPanel(new GridLayout(SkillType.skillList.length, 2));

        for (int i = 0; i < SkillType.getSkillList().length; i++) {
            final String type = SkillType.getSkillList()[i];
            JCheckBox chkSkill = new JCheckBox(type);
            chkSkill.setSelected(prereq.getSkillLevel(type) > -1);
            chkSkill.addItemListener(evt -> changeLevelEnabled(type));
            skillChks.put(type, chkSkill);

            DefaultComboBoxModel<SkillLevel> skillLvlModel = new DefaultComboBoxModel<>();
            skillLvlModel.addElement(SkillLevel.NONE);
            skillLvlModel.addElement(SkillLevel.GREEN);
            skillLvlModel.addElement(SkillLevel.REGULAR);
            skillLvlModel.addElement(SkillLevel.VETERAN);
            skillLvlModel.addElement(SkillLevel.ELITE);
            JComboBox<SkillLevel> choiceLvl = new JComboBox<>(skillLvlModel);
            choiceLvl.setEnabled(chkSkill.isSelected());
            int lvl = prereq.getSkillLevel(type);
            if (lvl < 0) {
                lvl = 0;
            }
            choiceLvl.setSelectedIndex(lvl);

            skillLevels.put(type, choiceLvl);
            panMain.add(chkSkill);
            panMain.add(choiceLvl);
        }

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        btnOK.setText("Done");
        btnOK.addActionListener(evt -> done());

        btnClose.setText("Cancel");
        btnClose.addActionListener(evt -> cancel());

        panButtons.add(btnOK);
        panButtons.add(btnClose);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Abilities");
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(new JScrollPaneWithSpeed(panMain), BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(400, 700));

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditSkillPerquisiteDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void done() {
        prereq = new SkillPerquisite();
        for (String type : SkillType.skillList) {
            if (skillChks.get(type).isSelected()) {
                prereq.addPrereq(type, skillLevels.get(type).getSelectedIndex());
            }
        }
        this.setVisible(false);
    }

    public SkillPerquisite getPrereq() {
        return prereq;
    }

    private void cancel() {
        this.setVisible(false);
        cancelled = true;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    private void changeLevelEnabled(String type) {
        skillLevels.get(type).setEnabled(skillChks.get(type).isSelected());
    }
}
