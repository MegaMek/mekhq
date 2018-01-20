/*
 * NewSkillPrereqDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mekhq.campaign.personnel.SkillPrereq;
import mekhq.campaign.personnel.SkillType;


/**
 *
 * @author  Taharqa
 */
public class EditSkillPrereqDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    private SkillPrereq prereq;

    private JButton btnClose;
    private JButton btnOK;
    private boolean cancelled;

    private Hashtable<String, JComboBox<String>> skillLevels = new Hashtable<String, JComboBox<String>>();
    private Hashtable<String, JCheckBox> skillChks = new Hashtable<String, JCheckBox>();

    public EditSkillPrereqDialog(Frame parent, SkillPrereq pre) {
        super(parent, true);
        cancelled = false;
        prereq = pre;
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        JPanel panMain = new JPanel(new GridLayout(SkillType.skillList.length,2));

        JCheckBox chkSkill;
        JComboBox<String> choiceLvl;
        DefaultComboBoxModel<String> skillLvlModel;
        for(int i = 0; i < SkillType.getSkillList().length; i++) {
        	final String type = SkillType.getSkillList()[i];
        	chkSkill = new JCheckBox(type);
        	chkSkill.setSelected(prereq.getSkillLevel(type) > -1);
        	skillChks.put(type, chkSkill);
        	chkSkill.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					changeLevelEnabled(type);
				}
    		});

        	skillLvlModel = new DefaultComboBoxModel<String>();
            skillLvlModel.addElement("None");
            skillLvlModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_GREEN));
            skillLvlModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_REGULAR));
            skillLvlModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_VETERAN));
            skillLvlModel.addElement(SkillType.getExperienceLevelName(SkillType.EXP_ELITE));
    		choiceLvl = new JComboBox<String>(skillLvlModel);
    		choiceLvl.setEnabled(chkSkill.isSelected());
    		int lvl = prereq.getSkillLevel(type);
    		if(lvl < 0) {
    			lvl = 0;
    		}
    		choiceLvl.setSelectedIndex(lvl);

            skillLevels.put(type, choiceLvl);
            panMain.add(chkSkill);
            panMain.add(choiceLvl);
        }

        JPanel panButtons = new JPanel(new GridLayout(0,2));
        btnOK.setText("Done"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                done();
            }
        });

        btnClose.setText("Cancel"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });

        panButtons.add(btnOK);
        panButtons.add(btnClose);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Select Abilities");
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(new JScrollPane(panMain), BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(400,700));

        pack();
    }

    private void done() {
    	prereq = new SkillPrereq();
    	for(String type : SkillType.skillList) {
    		if(skillChks.get(type).isSelected()) {
    			prereq.addPrereq(type, skillLevels.get(type).getSelectedIndex());
    		}
    	}
    	this.setVisible(false);
    }

    public SkillPrereq getPrereq() {
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
