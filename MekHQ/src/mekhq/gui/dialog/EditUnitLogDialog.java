/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.unit.Unit;
import mekhq.gui.control.EditUnitLogControl;
import mekhq.gui.control.EditUnitLogControl.UnitLogType;

/**
 * A dialog for editing a unit's log.
 *
 * <p>This dialog provides an interface for viewing and modifying the history of the provided unit. It uses the
 * {@link EditUnitLogControl} to handle the actual editing functionality. It is the unit-oriented counterpart to
 * {@link EditLogDialog}.</p>
 */
public class EditUnitLogDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(EditUnitLogDialog.class);

    private final JFrame frame;
    private final Unit unit;
    private final LocalDate today;
    private final UnitLogType logType;

    /**
     * Constructs a new dialog for editing a unit's log.
     *
     * @param parent  the parent frame for this dialog
     * @param today   the current campaign date
     * @param unit    the unit whose log is being edited
     * @param logType which of the unit's logs to edit
     */
    public EditUnitLogDialog(JFrame parent, LocalDate today, Unit unit, UnitLogType logType) {
        super(parent, true);

        this.frame = parent;
        this.today = today;
        this.unit = unit;
        this.logType = logType;

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    /**
     * Initializes the dialog components.
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("EditUnitLogDialog");
        setTitle(getFormattedText("editUnitLog.dialog.title", unit.getName()));
        getContentPane().setLayout(new BorderLayout());

        EditUnitLogControl editUnitLogControl = new EditUnitLogControl(frame, unit, today, logType);
        getContentPane().add(editUnitLogControl, BorderLayout.CENTER);

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
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditUnitLogDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
