/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * TODO : Add Validation
 */
public class CustomRankSystemCreationDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private final List<RankSystem> rankSystems;
    private RankSystem rankSystem;
    private final List<Rank> ranks;

    private JTextField txtRankSystemCode;
    private JTextField txtRankSystemName;
    private JComboBox<RankSystemType> comboRankSystemType;
    private JCheckBox chkSwapToRankSystem;
    //endregion Variable Declarations

    //region Constructors
    protected CustomRankSystemCreationDialog(final JFrame frame, final List<RankSystem> rankSystems,
                                             final List<Rank> ranks) {
        super(frame, "CustomRankSystemCreationDialog", "CustomRankSystemCreationDialog.title");
        this.rankSystems = rankSystems;
        setRankSystem(null);
        this.ranks = ranks;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public List<RankSystem> getRankSystems() {
        return rankSystems;
    }

    public RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }

    public List<Rank> getRanks() {
        return ranks;
    }

    public JTextField getTxtRankSystemCode() {
        return txtRankSystemCode;
    }

    public void setTxtRankSystemCode(final JTextField txtRankSystemCode) {
        this.txtRankSystemCode = txtRankSystemCode;
    }

    public JTextField getTxtRankSystemName() {
        return txtRankSystemName;
    }

    public void setTxtRankSystemName(final JTextField txtRankSystemName) {
        this.txtRankSystemName = txtRankSystemName;
    }

    public JComboBox<RankSystemType> getComboRankSystemType() {
        return comboRankSystemType;
    }

    public void setComboRankSystemType(final JComboBox<RankSystemType> comboRankSystemType) {
        this.comboRankSystemType = comboRankSystemType;
    }

    public JCheckBox getChkSwapToRankSystem() {
        return chkSwapToRankSystem;
    }

    public void setChkSwapToRankSystem(final JCheckBox chkSwapToRankSystem) {
        this.chkSwapToRankSystem = chkSwapToRankSystem;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        final JLabel lblRankSystemCode = new JLabel(resources.getString("lblRankSystemCode.text"));
        lblRankSystemCode.setToolTipText(resources.getString("lblRankSystemCode.toolTipText"));
        lblRankSystemCode.setName("lblRankSystemCode");

        setTxtRankSystemCode(new JTextField());
        getTxtRankSystemCode().setToolTipText(resources.getString("lblRankSystemCode.toolTipText"));
        getTxtRankSystemCode().setName("txtRankSystemCode");

        final JLabel lblRankSystemName = new JLabel(resources.getString("lblRankSystemName.text"));
        lblRankSystemName.setToolTipText(resources.getString("lblRankSystemName.toolTipText"));
        lblRankSystemName.setName("lblRankSystemName");

        setTxtRankSystemName(new JTextField());
        getTxtRankSystemName().setToolTipText(resources.getString("lblRankSystemName.toolTipText"));
        getTxtRankSystemName().setName("txtRankSystemName");

        final JLabel lblRankSystemType = new JLabel(resources.getString("lblRankSystemType.text"));
        lblRankSystemType.setToolTipText(resources.getString("lblRankSystemType.toolTipText"));
        lblRankSystemType.setName("lblRankSystemType");

        final DefaultComboBoxModel<RankSystemType> rankSystemTypeModel = new DefaultComboBoxModel<>(RankSystemType.values());
        rankSystemTypeModel.removeElement(RankSystemType.DEFAULT);
        setComboRankSystemType(new JComboBox<>(rankSystemTypeModel));
        getComboRankSystemType().setToolTipText(resources.getString("lblRankSystemType.toolTipText"));
        getComboRankSystemType().setName("comboRankSystemType");

        setChkSwapToRankSystem(new JCheckBox(resources.getString("chkSwapToRankSystem.text")));
        getChkSwapToRankSystem().setToolTipText(resources.getString("chkSwapToRankSystem.toolTipText"));
        getChkSwapToRankSystem().setName("chkSwapToRankSystem");

        // Programmatically Assign Accessibility Labels
        lblRankSystemCode.setLabelFor(getTxtRankSystemCode());
        lblRankSystemName.setLabelFor(getTxtRankSystemName());
        lblRankSystemType.setLabelFor(getComboRankSystemType());

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setName("customRankSystemCreationPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystemCode)
                                .addComponent(getTxtRankSystemCode(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystemName)
                                .addComponent(getTxtRankSystemName(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystemType)
                                .addComponent(getComboRankSystemType(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkSwapToRankSystem())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemCode)
                                .addComponent(getTxtRankSystemCode()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemName)
                                .addComponent(getTxtRankSystemName()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemType)
                                .addComponent(getComboRankSystemType()))
                        .addComponent(getChkSwapToRankSystem())
        );

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JComboBoxPreference(getComboRankSystemType()));
        preferences.manage(new JToggleButtonPreference(getChkSwapToRankSystem()));
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okAction() {
        super.okAction();
        setRankSystem(new RankSystem(getTxtRankSystemCode().getText(), getTxtRankSystemName().getText(),
                (RankSystemType) getComboRankSystemType().getSelectedItem()));
    }
    //endregion Button Actions
}
