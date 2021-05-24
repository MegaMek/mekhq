/*
 * EditPersonnelLogDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.control.EditPersonnelLogControl;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class EditPersonnelLogDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private JFrame frame;
    private Campaign campaign;
    private Person person;

    private EditPersonnelLogControl editPersonnelLogControl;
    private javax.swing.JButton btnOK;

    /** Creates new form EditPersonnelLogDialog */
    public EditPersonnelLogDialog(JFrame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        assert campaign != null;
        assert person != null;

        this.frame = parent;
        campaign = c;
        person = p;

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelLogDialog", new EncodeControl());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName(resourceMap.getString("dialog.name")); // NOI18N
        setTitle(resourceMap.getString("dialog.title") + " " + person.getFullName());
        getContentPane().setLayout(new java.awt.BorderLayout());

        editPersonnelLogControl = new EditPersonnelLogControl(frame, campaign, person);
        getContentPane().add(editPersonnelLogControl, BorderLayout.CENTER);

        btnOK = new JButton();
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(x -> this.setVisible(false));
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(EditPersonnelLogDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
}
