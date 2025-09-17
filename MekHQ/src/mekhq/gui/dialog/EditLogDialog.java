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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog;

import static mekhq.utilities.MHQInternationalization.getFormattedText;

import java.awt.BorderLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.gui.control.EditLogControl;
import mekhq.gui.control.EditLogControl.LogType;

/**
 * A dialog for editing a person's log.
 *
 * <p>This dialog provides an interface for viewing and modifying the history of the provided character. It
 * uses the {@link EditLogControl} to handle the actual editing functionality.</p>
 *
 * @author Taharqa
 */
public class EditLogDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(EditLogDialog.class);

    private final JFrame frame;
    private final Person person;
    private final LocalDate today;
    private final LogType logType;

    /**
     * Constructs a new dialog for editing a person's log.
     *
     * @param parent the parent frame for this dialog
     * @param person the person whose log is being edited
     * @param today  the current campaign date
     */
    public EditLogDialog(JFrame parent, LocalDate today, Person person, LogType logType) {
        super(parent, true);

        this.frame = parent;
        this.today = today;
        this.person = person;
        this.logType = logType;

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    /**
     * Initializes the dialog components.
     *
     * <p>Sets up the dialog's basic properties, creates and adds the log editing control, and adds a
     * confirmation button to close the dialog.</p>
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("EditLogDialog");
        setTitle(getFormattedText("editLog.dialog.title", person.getFullTitle()));
        getContentPane().setLayout(new BorderLayout());

        EditLogControl editLogControl = new EditLogControl(frame, person, today, logType);
        getContentPane().add(editLogControl, BorderLayout.CENTER);

        JButton btnOK = new JButton();
        btnOK.setName("btnOK");
        btnOK.setText(getFormattedText("editLog.btnOK.text"));
        btnOK.addActionListener(x -> this.dispose());
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditLogDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
