/*
 * EditSpecialAbilityDialog.java
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
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.personnel.SkillPrereq;
import mekhq.campaign.personnel.SpecialAbility;


/**
 *
 * @author  Taharqa
 */
public class EditSpecialAbilityDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;
    private SpecialAbility ability;

    private JButton btnClose;
    private JButton btnOK;
    private JSpinner spnXP;

    private JButton btnEditPrereqAbil;
    private JButton btnEditInvalid;
    private JButton btnEditRemove;
    private JButton btnClearPrereqSkills;
    private JButton btnAddSkillPrereq;

    private Vector<String> prereqAbilities;
    private Vector<SkillPrereq> prereqSkills;
    private Vector<String> invalidAbilities;
    private Vector<String> removeAbilities;

    private Hashtable<String, SpecialAbility> allSPA;

    private JLabel lblPrereqAbil;
    private JLabel lblInvalidAbil;
    private JLabel lblRemoveAbil;
    
    private boolean cancelled;
    private int currentXP;


    @SuppressWarnings("unchecked")
	public EditSpecialAbilityDialog(Frame parent, SpecialAbility spa, Hashtable<String, SpecialAbility> hash) {
        super(parent, true);
        this.ability = spa;
        this.allSPA = hash;
        // FIXME: Java is broken, so we had to supress unchecked warnings for these 4 lines
        // Basically, Vector<E>.clone() returns an Object instead of a new Vector<E> - DOH!
        prereqAbilities = (Vector<String>)ability.getPrereqAbilities().clone();
        invalidAbilities = (Vector<String>)ability.getInvalidAbilities().clone();
        removeAbilities = (Vector<String>)ability.getRemovedAbilities().clone();
        prereqSkills = (Vector<SkillPrereq>)ability.getPrereqSkills().clone();
        cancelled = false;
        currentXP = ability.getCost();
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();


        spnXP = new JSpinner(new SpinnerNumberModel(currentXP, -1, 100000, 1));

        JPanel panXP = new JPanel(new GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP Cost:"), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnXP, gridBagConstraints);

        JPanel panAbil = new JPanel(new GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Prerequisite Abilities</b></html>"), gridBagConstraints);
        btnEditPrereqAbil = new javax.swing.JButton("Edit Prereq Abilities");
        btnEditPrereqAbil.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, prereqAbilities, allSPA);
                sad.setVisible(true);
                if(!sad.wasCancelled()) {
                	prereqAbilities = sad.getSelected();
                    lblPrereqAbil.setText("<html>" + getPrereqAbilDesc() + "</html>");
                    refreshGUI();
                }
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditPrereqAbil, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 1, 1);
        lblPrereqAbil = new JLabel("<html>" + getPrereqAbilDesc() + "</html>");
        panAbil.add(lblPrereqAbil, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Invalid Abilities</b></html>"), gridBagConstraints);
        btnEditInvalid = new javax.swing.JButton("Edit Invalid Abilities");
        btnEditInvalid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, invalidAbilities, allSPA);
                sad.setVisible(true);
                if(!sad.wasCancelled()) {
                	invalidAbilities = sad.getSelected();
                    lblInvalidAbil.setText("<html>" + getInvalidDesc() + "</html>");
                    refreshGUI();
                }
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditInvalid, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 1, 1);
        lblInvalidAbil = new JLabel("<html>" + getInvalidDesc() + "</html>");
        panAbil.add(lblInvalidAbil, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(new JLabel("<html><b>Removed Abilities</b></html>"), gridBagConstraints);
        btnEditRemove = new javax.swing.JButton("Edit Removed Abilities");
        btnEditRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectAbilitiesDialog sad = new SelectAbilitiesDialog(null, removeAbilities, allSPA);
                sad.setVisible(true);
                if(!sad.wasCancelled()) {
                	removeAbilities = sad.getSelected();
                    lblRemoveAbil.setText("<html>" + getRemovedDesc() + "</html>");
                    refreshGUI();
                }
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panAbil.add(btnEditRemove, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 10, 1, 1);
        lblRemoveAbil = new JLabel("<html>" + getRemovedDesc() + "</html>");
        panAbil.add(lblRemoveAbil, gridBagConstraints);

        JPanel panMain = new JPanel(new GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(panXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(panAbil, gridBagConstraints);


        JPanel panSkill = createSkillPanel();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(panSkill, gridBagConstraints);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit " + ability.getDisplayName());
        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(panMain, BorderLayout.CENTER);

        JPanel panButton = new JPanel(new GridLayout(0,2));

        btnOK.setText("OK"); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edit();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panButton.add(btnOK, gridBagConstraints);

        btnClose.setText("Cancel"); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panButton.add(btnClose, gridBagConstraints);

        getContentPane().add(panButton, BorderLayout.SOUTH);

        pack();
    }

    private JPanel createSkillPanel() {
    	JPanel panSkill = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panSkill.add(new JLabel("<html><b>Prerequisite Skillsets</b></html>"), gridBagConstraints);

        btnAddSkillPrereq = new javax.swing.JButton("Add Skill Prereq");
        btnAddSkillPrereq.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	EditSkillPrereqDialog nspd = new EditSkillPrereqDialog(null, new SkillPrereq());
            	nspd.setVisible(true);
            	if(!nspd.wasCancelled() & !nspd.getPrereq().isEmpty()) {
                	prereqSkills.add(nspd.getPrereq());
                	refreshGUI();
                }
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnAddSkillPrereq, gridBagConstraints);

        btnClearPrereqSkills = new javax.swing.JButton("Clear Skill Prereqs");
        btnClearPrereqSkills.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prereqSkills = new Vector<SkillPrereq>();
                refreshGUI();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        panSkill.add(btnClearPrereqSkills, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        gridBagConstraints.fill = GridBagConstraints.BOTH;

        JPanel panSkPre;
        JButton btnRemoveSkill;
        JButton btnEditSkill;
        for(int i = 0; i < prereqSkills.size(); i++) {
        	SkillPrereq skpre = prereqSkills.get(i);
        	panSkPre = new JPanel(new GridBagLayout());

        	GridBagConstraints c = new java.awt.GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.weightx = 0.0;
            c.weighty = 1.0;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new java.awt.Insets(2, 2, 2, 2);
            c.fill = GridBagConstraints.BOTH;
            panSkPre.add(new JLabel("<html>" + skpre.toString() + "</html>"), c);

            c.gridx = 1;
            c.gridy = 0;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.insets = new java.awt.Insets(2, 2, 2, 2);
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
            c.insets = new java.awt.Insets(2, 2, 2, 2);
            c.fill = GridBagConstraints.HORIZONTAL;
            btnRemoveSkill = new JButton("Remove");
            btnRemoveSkill.setActionCommand(Integer.toString(i));
            btnRemoveSkill.addActionListener(new RemoveSkillListener());
            panSkPre.add(btnRemoveSkill, c);

            panSkPre.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            if(i >= (prereqSkills.size() - 1)) {
            	gridBagConstraints.weighty = 1.0;
            }
        	panSkill.add(panSkPre, gridBagConstraints);
        	gridBagConstraints.gridy++;
        }

        return panSkill;
    }

    private void edit() {
    	ability.setCost((Integer)spnXP.getModel().getValue());
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

    private String getPrereqAbilDesc() {
        String toReturn = "";
        for(String prereq : prereqAbilities) {
            toReturn += allSPA.get(prereq).getDisplayName() + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    private String getInvalidDesc() {
        String toReturn = "";
        for(String invalid : invalidAbilities) {
            toReturn += allSPA.get(invalid).getDisplayName() + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    private String getRemovedDesc() {
        String toReturn = "";
        for(String remove : removeAbilities) {
            toReturn += allSPA.get(remove).getDisplayName() + "<br>";
        }
        if(toReturn.isEmpty()) {
        	toReturn = "None";
        }
        return toReturn;
    }

    private void refreshGUI() {
    	currentXP = (Integer)spnXP.getModel().getValue();
    	getContentPane().removeAll();
    	initComponents();
    	getContentPane().revalidate();
    	getContentPane().repaint();
    }

    private void removeSkillPrereq(int i) {
    	prereqSkills.remove(i);
    }

    private void editSkillPrereq(int i) {
    	EditSkillPrereqDialog nspd = new EditSkillPrereqDialog(null, prereqSkills.get(i));
    	nspd.setVisible(true);
    	if(!nspd.wasCancelled() & !nspd.getPrereq().isEmpty()) {
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
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            int id = Integer.parseInt(evt.getActionCommand());
            removeSkillPrereq(id);
            refreshGUI();
        }
    }

    private class EditSkillListener implements ActionListener {

        public EditSkillListener() {
		}

		@Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            int id = Integer.parseInt(evt.getActionCommand());
            editSkillPrereq(id);
            refreshGUI();
        }
    }
}
