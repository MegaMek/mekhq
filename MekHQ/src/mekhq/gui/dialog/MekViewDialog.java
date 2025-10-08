/*
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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since July 15, 2009, 9:30 PM
 * @deprecated No indicated Uses.
 */
@Deprecated(since = "0.50.06", forRemoval = true)
public class MekViewDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(MekViewDialog.class);

    private final EntityReadout mekView;

    /** Creates new form MekViewDialog */
    public MekViewDialog(JFrame parent, boolean modal, EntityReadout entityReadout) {
        super(parent, modal);
        this.mekView = entityReadout;
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {

        JScrollPane jScrollPane2 = new JScrollPaneWithSpeed();
        JTextPane txtMek = new JTextPane();
        JButton btnOkay = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekViewDialog",
              MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Unit View");

        jScrollPane2.setName("jScrollPane2");

        txtMek.setContentType(resourceMap.getString("txtMek.contentType"));
        txtMek.setEditable(false);
        txtMek.setFont(Font.decode(resourceMap.getString("txtMek.font")));
        txtMek.setName("txtMek");
        txtMek.setText(mekView.getFullReadout());
        jScrollPane2.setViewportView(txtMek);

        btnOkay.setText(resourceMap.getString("btnOkay.text"));
        btnOkay.setName("btnOkay");
        btnOkay.addActionListener(this::btnOkayActionPerformed);

        getContentPane().add(jScrollPane2, BorderLayout.CENTER);
        getContentPane().add(btnOkay, BorderLayout.PAGE_END);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekViewDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void btnOkayActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }
}
