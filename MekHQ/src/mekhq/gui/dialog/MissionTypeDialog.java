/*
 * Copyright (c) 2010-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author natit
 * @since Jan 6, 2010, 10:46:02 PM
 */
public class MissionTypeDialog extends JDialog {

    private boolean contract;

    /** Creates new form */
    public MissionTypeDialog(Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.MissionTypeDialog",
                MekHQ.getMHQOptions().getLocale());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new java.awt.GridLayout(2,1));

        JButton btnMission = new javax.swing.JButton(resourceMap.getString("btnMission.text"));
        btnMission.setToolTipText(resourceMap.getString("btnMission.tooltip"));
        btnMission.setName("btnMission");
        btnMission.addActionListener(ev -> {
            contract = false;
            setVisible(false);
        });
        getContentPane().add(btnMission);

        JButton btnContract = new javax.swing.JButton(resourceMap.getString("btnContract.text"));
        btnContract.setToolTipText(resourceMap.getString("btnContract.tooltip"));
        btnContract.setName("btnContract");
        btnContract.addActionListener(ev -> {
            contract = true;
            setVisible(false);
        });
        getContentPane().add(btnContract);

        setSize(250, 150);
        setUserPreferences();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(MissionTypeDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    public boolean isContract() {
        return contract;
    }
}
