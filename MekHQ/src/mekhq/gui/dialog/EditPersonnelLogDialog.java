/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.control.EditPersonnelLogControl;

/**
 * @author Taharqa
 */
public class EditPersonnelLogDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditPersonnelLogDialog.class);

    private JFrame   frame;
    private Campaign campaign;
    private Person   person;

    private EditPersonnelLogControl editPersonnelLogControl;
    private JButton                 btnOK;

    /** Creates new form EditPersonnelLogDialog */
    public EditPersonnelLogDialog(JFrame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        Objects.requireNonNull(c);
        Objects.requireNonNull(p);

        this.frame = parent;
        campaign   = c;
        person     = p;

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelLogDialog",
              MekHQ.getMHQOptions().getLocale());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName(resourceMap.getString("dialog.name"));
        setTitle(resourceMap.getString("dialog.title") + " " + person.getFullName());
        getContentPane().setLayout(new BorderLayout());

        editPersonnelLogControl = new EditPersonnelLogControl(frame, campaign, person);
        getContentPane().add(editPersonnelLogControl, BorderLayout.CENTER);

        btnOK = new JButton();
        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(x -> this.setVisible(false));
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditPersonnelLogDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }
}
