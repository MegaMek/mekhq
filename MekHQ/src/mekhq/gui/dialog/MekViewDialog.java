/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.MekView;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since July 15, 2009, 9:30 PM
 */
public class MekViewDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(MekViewDialog.class);

    private MekView mview;
    private JButton btnOkay;
    private JScrollPane jScrollPane2;
    private JTextPane txtMek;

    /** Creates new form MekViewDialog */
    public MekViewDialog(JFrame parent, boolean modal, MekView mv) {
        super(parent, modal);
        this.mview = mv;
        initComponents();
        setUserPreferences();
    }

    private void initComponents() {

        jScrollPane2 = new JScrollPane();
        txtMek = new JTextPane();
        btnOkay = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MekViewDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Unit View");

        jScrollPane2.setName("jScrollPane2");

        txtMek.setContentType(resourceMap.getString("txtMek.contentType"));
        txtMek.setEditable(false);
        txtMek.setFont(Font.decode(resourceMap.getString("txtMek.font")));
        txtMek.setName("txtMek");
        txtMek.setText(mview.getMekReadout());
        jScrollPane2.setViewportView(txtMek);

        btnOkay.setText(resourceMap.getString("btnOkay.text"));
        btnOkay.setName("btnOkay");
        btnOkay.addActionListener(this::btnOkayActionPerformed);

        getContentPane().add(jScrollPane2, BorderLayout.CENTER);
        getContentPane().add(btnOkay, BorderLayout.PAGE_END);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MekViewDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void btnOkayActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }
}
