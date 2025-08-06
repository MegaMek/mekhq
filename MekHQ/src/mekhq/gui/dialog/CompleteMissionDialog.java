/*
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
import java.awt.Container;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

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

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

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
