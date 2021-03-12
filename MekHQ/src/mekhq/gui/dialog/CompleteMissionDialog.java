/*
 * Copyright (c) 2010-2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

public class CompleteMissionDialog extends javax.swing.JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 8376874926997734492L;

    private MissionStatus status;
    //endregion Variable Declarations

    //region Constructors
    public CompleteMissionDialog(JFrame parent, boolean modal, Mission mission) {
        super(parent, modal);
        this.status = mission.getStatus();
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    //endregion Constructors

    //region Initialization
    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CompleteMissionDialog", new EncodeControl());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new GridBagLayout());

        int gridx = 0;
        int gridy = 0;

        JLabel lblOutcome = new JLabel(resourceMap.getString("lblOutcome.text"));
        lblOutcome.setName("lblOutcome");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(lblOutcome, gridBagConstraints);

        DefaultComboBoxModel<MissionStatus> outcomeModel = new DefaultComboBoxModel<>(MissionStatus.values());
        JComboBox<MissionStatus> choiceOutcome = new JComboBox<>(outcomeModel);
        choiceOutcome.setName("choiceOutcome");
        choiceOutcome.setSelectedItem(getStatus());
        gridBagConstraints.gridx = gridx--;
        getContentPane().add(choiceOutcome, gridBagConstraints);

        JButton btnDone = new JButton(resourceMap.getString("btnDone.text"));
        btnDone.setName("btnDone");
        btnDone.addActionListener(evt -> {
            status = (MissionStatus) choiceOutcome.getSelectedItem();
            setVisible(false);
        });
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        getContentPane().add(btnDone, gridBagConstraints);

        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(evt -> {
            status = MissionStatus.ACTIVE;
            setVisible(false);
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = gridx;
        getContentPane().add(btnCancel, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CompleteMissionDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }
    //endregion Initialization

    //region Getters/Setters
    public MissionStatus getStatus() {
        return status;
    }
    //endregion Getters/Setters
}
