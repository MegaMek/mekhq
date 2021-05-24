/*
 * SelectAbilitiesDialog.java
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
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.campaign.personnel.SpecialAbility;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 *
 * @author  Taharqa
 */
public class SelectUnusedAbilityDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    private JButton btnClose;
    private JButton btnOK;
    private ButtonGroup group;
    private Vector<String> choices;
    private boolean cancelled;
    private Hashtable<String, SpecialAbility> currentSPA;

    public SelectUnusedAbilityDialog(Frame parent, Vector<String> unused, Hashtable<String, SpecialAbility> c) {
        super(parent, true);
        choices = unused;
        currentSPA = c;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        group = new ButtonGroup();

        int ncol = 2;
        JPanel panMain = new JPanel(new GridLayout((int)Math.ceil(choices.size() / (ncol*1.0)),ncol));

        JRadioButton chk;
        for(String name : choices) {
        	chk = new JRadioButton(SpecialAbility.getDefaultAbility(name) == null
        	        ? getDisplayName(name) : SpecialAbility.getDefaultAbility(name).getDisplayName());
        	chk.setActionCommand(name);
        	chk.setToolTipText(SpecialAbility.getDefaultAbility(name) == null
        	        ? this.getDesc(name) : SpecialAbility.getDefaultAbility(name).getDescription());
        	group.add(chk);
        	panMain.add(chk);
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

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panButtons, BorderLayout.SOUTH);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(SelectUnusedAbilityDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void done() {
    	if(null != group.getSelection()) {
    		String name = group.getSelection().getActionCommand();
    		String displayName = "";
    		String desc = "";

    		PilotOptions poptions = new PilotOptions();
        	for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
        		IOptionGroup group = i.nextElement();

        		if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
        			continue;
        		}

        		for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
        			IOption option = j.nextElement();
        			if(option.getName().equals(name)) {
        				displayName = option.getDisplayableName();
        				desc = option.getDescription();
        			}
        		}
        	}

            SpecialAbility spa;
        	if (null != SpecialAbility.getDefaultAbility(name)) {
        	    spa = SpecialAbility.getDefaultAbility(name).clone();
        	} else {
        	    spa = new SpecialAbility(name, displayName, desc);
        	}
        	EditSpecialAbilityDialog esad = new EditSpecialAbilityDialog(null, spa, currentSPA);
        	esad.setVisible(true);
        	if(!esad.wasCancelled()) {
        		currentSPA.put(spa.getName(), spa);
        	}
    	}
    	this.setVisible(false);
    }

    private void cancel() {
    	this.setVisible(false);
    	cancelled = true;
    }

    public boolean wasCancelled() {
    	return cancelled;
    }

    private String getDisplayName(String lookup) {
    	PilotOptions poptions = new PilotOptions();
    	for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
    		IOptionGroup group = i.nextElement();

    		if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
    			continue;
    		}

    		for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
    			IOption option = j.nextElement();
    			if(option.getName().equals(lookup)) {
    				return(option.getDisplayableName());
    			}
    		}
    	}
    	return "??";
    }

    private String getDesc(String lookup) {
    	PilotOptions poptions = new PilotOptions();
    	for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
    		IOptionGroup group = i.nextElement();

    		if (!group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)) {
    			continue;
    		}

    		for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
    			IOption option = j.nextElement();
    			if(option.getName().equals(lookup)) {
    				return(option.getDescription());
    			}
    		}
    	}
    	return "??";
    }
}
