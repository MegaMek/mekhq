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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.ValidationState;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomRankSystemCreationDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final List<RankSystem> rankSystems;
    private RankSystem rankSystem;
    private final List<Rank> ranks;

    private JTextField txtRankSystemCode;
    private MMComboBox<RankSystemType> comboRankSystemType;
    private JTextField txtRankSystemName;
    private JTextField txtRankSystemDescription;
    private JCheckBox chkUseROMDesignation;
    private JCheckBox chkUseManeiDomini;
    private JCheckBox chkSwapToRankSystem;
    //endregion Variable Declarations

    //region Constructors
    public CustomRankSystemCreationDialog(final JFrame frame, final List<RankSystem> rankSystems,
                                          final List<Rank> ranks) {
        super(frame, "CustomRankSystemCreationDialog", "CustomRankSystemCreationDialog.title");
        this.rankSystems = rankSystems;
        setRankSystem(null);
        this.ranks = ranks;
        initialize();
        getOkButton().setEnabled(false);
    }
    //endregion Constructors

    //region Getters/Setters
    public List<RankSystem> getRankSystems() {
        return rankSystems;
    }

    public @Nullable RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final @Nullable RankSystem rankSystem) {
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

    public MMComboBox<RankSystemType> getComboRankSystemType() {
        return comboRankSystemType;
    }

    public void setComboRankSystemType(final MMComboBox<RankSystemType> comboRankSystemType) {
        this.comboRankSystemType = comboRankSystemType;
    }

    public JTextField getTxtRankSystemName() {
        return txtRankSystemName;
    }

    public void setTxtRankSystemName(final JTextField txtRankSystemName) {
        this.txtRankSystemName = txtRankSystemName;
    }

    public JTextField getTxtRankSystemDescription() {
        return txtRankSystemDescription;
    }

    public void setTxtRankSystemDescription(final JTextField txtRankSystemDescription) {
        this.txtRankSystemDescription = txtRankSystemDescription;
    }

    public JCheckBox getChkUseROMDesignation() {
        return chkUseROMDesignation;
    }

    public void setChkUseROMDesignation(final JCheckBox chkUseROMDesignation) {
        this.chkUseROMDesignation = chkUseROMDesignation;
    }

    public JCheckBox getChkUseManeiDomini() {
        return chkUseManeiDomini;
    }

    public void setChkUseManeiDomini(final JCheckBox chkUseManeiDomini) {
        this.chkUseManeiDomini = chkUseManeiDomini;
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

        final JLabel lblRankSystemType = new JLabel(resources.getString("lblRankSystemType.text"));
        lblRankSystemType.setToolTipText(resources.getString("lblRankSystemType.toolTipText"));
        lblRankSystemType.setName("lblRankSystemType");

        final DefaultComboBoxModel<RankSystemType> rankSystemTypeModel = new DefaultComboBoxModel<>(RankSystemType.values());
        rankSystemTypeModel.removeElement(RankSystemType.DEFAULT);
        setComboRankSystemType(new MMComboBox<>("comboRankSystemType", rankSystemTypeModel));
        getComboRankSystemType().setToolTipText(resources.getString("lblRankSystemType.toolTipText"));
        getComboRankSystemType().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RankSystemType) {
                    list.setToolTipText(((RankSystemType) value).getToolTipText());
                }
                return this;
            }
        });
        getComboRankSystemType().addActionListener(evt -> setState(ValidationState.PENDING));

        setTxtRankSystemCode(new JTextField());
        getTxtRankSystemCode().setToolTipText(resources.getString("lblRankSystemCode.toolTipText"));
        getTxtRankSystemCode().setName("txtRankSystemCode");
        getTxtRankSystemCode().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void removeUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void changedUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }
        });

        final JLabel lblRankSystemName = new JLabel(resources.getString("lblRankSystemName.text"));
        lblRankSystemName.setToolTipText(resources.getString("lblRankSystemName.toolTipText"));
        lblRankSystemName.setName("lblRankSystemName");

        setTxtRankSystemName(new JTextField());
        getTxtRankSystemName().setToolTipText(resources.getString("lblRankSystemName.toolTipText"));
        getTxtRankSystemName().setName("txtRankSystemName");
        getTxtRankSystemName().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void removeUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }

            @Override
            public void changedUpdate(final DocumentEvent evt) {
                revalidateAction(null);
            }
        });

        final JLabel lblRankSystemDescription = new JLabel(resources.getString("lblRankSystemDescription.text"));
        lblRankSystemDescription.setToolTipText(resources.getString("lblRankSystemDescription.toolTipText"));
        lblRankSystemDescription.setName("lblRankSystemDescription");

        setTxtRankSystemDescription(new JTextField());
        getTxtRankSystemDescription().setToolTipText(resources.getString("lblRankSystemDescription.toolTipText"));
        getTxtRankSystemDescription().setName("txtRankSystemDescription");

        setChkUseROMDesignation(new JCheckBox(resources.getString("chkUseROMDesignation.text")));
        getChkUseROMDesignation().setToolTipText(resources.getString("chkUseROMDesignation.toolTipText"));
        getChkUseROMDesignation().setName("chkUseROMDesignation");

        setChkUseManeiDomini(new JCheckBox(resources.getString("chkUseManeiDomini.text")));
        getChkUseManeiDomini().setToolTipText(resources.getString("chkUseManeiDomini.toolTipText"));
        getChkUseManeiDomini().setName("chkUseManeiDomini");

        setChkSwapToRankSystem(new JCheckBox(resources.getString("chkSwapToRankSystem.text")));
        getChkSwapToRankSystem().setToolTipText(resources.getString("chkSwapToRankSystem.toolTipText"));
        getChkSwapToRankSystem().setName("chkSwapToRankSystem");

        // Programmatically Assign Accessibility Labels
        lblRankSystemCode.setLabelFor(getTxtRankSystemCode());
        lblRankSystemName.setLabelFor(getTxtRankSystemName());
        lblRankSystemDescription.setLabelFor(getTxtRankSystemDescription());
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
                                .addComponent(lblRankSystemType)
                                .addComponent(getComboRankSystemType(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystemName)
                                .addComponent(getTxtRankSystemName(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystemDescription)
                                .addComponent(getTxtRankSystemDescription(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkUseROMDesignation())
                        .addComponent(getChkUseManeiDomini())
                        .addComponent(getChkSwapToRankSystem())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemCode)
                                .addComponent(getTxtRankSystemCode()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemType)
                                .addComponent(getComboRankSystemType()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemName)
                                .addComponent(getTxtRankSystemName()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystemDescription)
                                .addComponent(getTxtRankSystemDescription()))
                        .addComponent(getChkUseROMDesignation())
                        .addComponent(getChkUseManeiDomini())
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

        // First, we need to create the new rank system
        setRankSystem(new RankSystem(getTxtRankSystemCode().getText().toUpperCase(Locale.ENGLISH),
                getTxtRankSystemName().getText(), getTxtRankSystemDescription().getText(),
                getComboRankSystemType().getSelectedItem()));

        getRankSystem().setUseROMDesignation(getChkUseROMDesignation().isSelected());
        getRankSystem().setUseManeiDomini(getChkUseManeiDomini().isSelected());

        // Then, we need to clone out the rank setup
        getRankSystem().setRanks(new ArrayList<>());
        for (final Rank rank : getRanks()) {
            getRankSystem().getRanks().add(new Rank(rank));
        }
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        final String text;
        if (getTxtRankSystemCode().getText().isBlank()) {
            text = resources.getString("CustomRankSystemCreationDialog.BlankRankSystemCode.text");
        } else if (getTxtRankSystemName().getText().isBlank()) {
            text = resources.getString("CustomRankSystemCreationDialog.BlankRankSystemName.text");
        } else if (getRankSystems().stream().anyMatch(rankSystem -> getTxtRankSystemCode().getText()
                .equalsIgnoreCase(rankSystem.getCode()))) {
            text = resources.getString("CustomRankSystemCreationDialog.DuplicateCode.text");
        } else {
            text = resources.getString("ValidationSuccess.text");
            getOkButton().setEnabled(true);
            getOkButton().setToolTipText(text);
            return ValidationState.SUCCESS;
        }

        if (display) {
            JOptionPane.showMessageDialog(getFrame(), text, resources.getString("ValidationFailure.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
        getOkButton().setEnabled(false);
        getOkButton().setToolTipText(text);
        return ValidationState.FAILURE;
    }
    //endregion Button Actions
}
