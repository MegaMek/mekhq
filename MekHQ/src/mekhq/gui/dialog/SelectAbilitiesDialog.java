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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import mekhq.MekHQ;
import mekhq.campaign.personnel.SpecialAbility;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class SelectAbilitiesDialog extends JDialog {
    private static final long serialVersionUID = -8038099101234445018L;

    private JButton btnClose;
    private JButton btnOK;
    private ArrayList<JCheckBox> chkAbil;
    private Vector<String> selected;
    private ArrayList<String> spaNames;
    private boolean cancelled;

    private Hashtable<String, SpecialAbility> allSPA;

    public SelectAbilitiesDialog(Frame parent, Vector<String> s, Hashtable<String, SpecialAbility> hash) {
        super(parent, true);
        selected = s;
        allSPA = hash;
        cancelled = false;
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        chkAbil = new ArrayList<>();
        spaNames = new ArrayList<>();

        spaNames.addAll(allSPA.keySet());

        int ncol = 3;
        JPanel panMain = new JPanel(new GridLayout((int) Math.ceil(spaNames.size() / (ncol * 1.0)),ncol));

        JCheckBox chk;
        for(String name : spaNames) {
        	chk = new JCheckBox(allSPA.get(name).getDisplayName());
        	if(selected.contains(name)) {
        		chk.setSelected(true);
        	}
        	chkAbil.add(chk);
        	panMain.add(chk);
        }

        JPanel panButtons = new JPanel(new GridLayout(0,2));
        btnOK.setText("Done");
        btnOK.addActionListener(evt -> done());

        btnClose.setText("Cancel");
        btnClose.addActionListener(evt -> cancel());

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
        PreferencesNode preferences = MekHQ.getPreferences().forClass(SelectAbilitiesDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void done() {
    	selected = new Vector<>();
    	for (int i = 0; i < spaNames.size(); i++) {
    		if (chkAbil.get(i).isSelected()) {
    			selected.add(spaNames.get(i));
    		}
    	}
    	this.setVisible(false);
    }

    public Vector<String> getSelected() {
    	return selected;
    }

    private void cancel() {
    	this.setVisible(false);
    	cancelled = true;
    }

    public boolean wasCancelled() {
    	return cancelled;
    }
}
