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

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

public class CompleteMissionDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private MMComboBox<MissionStatus> comboOutcomeStatus;
    //endregion Variable Declarations

    //region Constructors
    public CompleteMissionDialog(final JFrame frame) {
        super(frame, "CompleteMissionDialog", "CompleteMissionDialog.title");
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public MMComboBox<MissionStatus> getComboOutcomeStatus() {
        return comboOutcomeStatus;
    }

    public MissionStatus getStatus() {
        final MissionStatus status = getComboOutcomeStatus().getSelectedItem();
        return (status == null) ? MissionStatus.ACTIVE : status;
    }

    public void setComboOutcomeStatus(final MMComboBox<MissionStatus> comboOutcomeStatus) {
        this.comboOutcomeStatus = comboOutcomeStatus;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        final JLabel lblOutcomeStatus = new JLabel(resources.getString("lblOutcomeStatus.text"));
        lblOutcomeStatus.setToolTipText(resources.getString("lblOutcomeStatus.toolTipText"));
        lblOutcomeStatus.setName("lblOutcomeStatus");

        setComboOutcomeStatus(new MMComboBox<>("comboOutcomeStatus", MissionStatus.values()));
        getComboOutcomeStatus().setToolTipText(resources.getString("lblOutcomeStatus.toolTipText"));
        getComboOutcomeStatus().setSelectedItem(MissionStatus.SUCCESS);
        getComboOutcomeStatus().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MissionStatus) {
                    list.setToolTipText(((MissionStatus) value).getToolTipText());
                }
                return this;
            }
        });

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setName("completeMissionPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

<<<<<<< HEAD
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
=======
        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(evt -> {
            status = MissionStatus.ACTIVE;
            setVisible(false);
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx;
        getContentPane().add(btnCancel, gridBagConstraints);
>>>>>>> upstream/master

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(lblOutcomeStatus)
                        .addComponent(getComboOutcomeStatus())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(lblOutcomeStatus)
                        .addComponent(getComboOutcomeStatus())
        );

        return panel;
    }
    //endregion Initialization
}
