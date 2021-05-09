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
package mekhq.gui.panes;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.baseComponents.SpinnerCellEditor;
import megamek.common.annotations.Nullable;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQScrollPane;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.dialog.CustomRankSystemCreationDialog;
import mekhq.gui.model.RankTableModel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RankSystemsPane extends AbstractMHQScrollPane {
    //region Variable Declarations
    private final Campaign campaign;
    private RankSystem selectedRankSystem;

    // Rank System Panel
    private SortedComboBoxModel<RankSystem> rankSystemModel;
    private MMComboBox<RankSystem> comboRankSystems;
    private DefaultComboBoxModel<RankSystemType> rankSystemTypeModel;
    private MMComboBox<RankSystemType> comboRankSystemType;

    // Ranks Table Panel
    private JTable ranksTable;
    private RankTableModel ranksTableModel;
    //endregion Variable Declarations

    //region Constructors
    public RankSystemsPane(final JFrame frame, final Campaign campaign) {
        super(frame, "RankSystemsPane");
        this.campaign = campaign;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public @Nullable RankSystem getSelectedRankSystem() {
        return selectedRankSystem;
    }

    public void setSelectedRankSystem(final @Nullable RankSystem selectedRankSystem) {
        this.selectedRankSystem = selectedRankSystem;
    }

    //region Rank System Panel
    public SortedComboBoxModel<RankSystem> getRankSystemModel() {
        return rankSystemModel;
    }

    public void setRankSystemModel(final SortedComboBoxModel<RankSystem> rankSystemModel) {
        this.rankSystemModel = rankSystemModel;
    }

    public MMComboBox<RankSystem> getComboRankSystems() {
        return comboRankSystems;
    }

    public void setComboRankSystems(final MMComboBox<RankSystem> comboRankSystems) {
        this.comboRankSystems = comboRankSystems;
    }

    public DefaultComboBoxModel<RankSystemType> getRankSystemTypeModel() {
        return rankSystemTypeModel;
    }

    public void setRankSystemTypeModel(final DefaultComboBoxModel<RankSystemType> rankSystemTypeModel) {
        this.rankSystemTypeModel = rankSystemTypeModel;
    }

    public MMComboBox<RankSystemType> getComboRankSystemType() {
        return comboRankSystemType;
    }

    public void setComboRankSystemType(final MMComboBox<RankSystemType> comboRankSystemType) {
        this.comboRankSystemType = comboRankSystemType;
    }
    //endregion Rank System Panel

    //region Ranks Table Panel
    public JTable getRanksTable() {
        return ranksTable;
    }

    public void setRanksTable(final JTable ranksTable) {
        this.ranksTable = ranksTable;
    }

    public RankTableModel getRanksTableModel() {
        return ranksTableModel;
    }

    public void setRanksTableModel(final RankTableModel ranksTableModel) {
        this.ranksTableModel = ranksTableModel;
    }
    //endregion Ranks Table Panel
    //endregion Getters/Setters

    //region Initialization
    /**
     * No Preferences are required here, so we don't call setPreferences.
     * SelectedRankSystem will be non-null for the initialization based on how the logic is set up,
     * so we don't need to check to ensure that is the case
     */
    @Override
    protected void initialize() {
        // First, we have to initialize the selected rank system.
        setSelectedRankSystem(getCampaign().getRankSystem().getType().isCampaign()
                ? new RankSystem(getCampaign().getRankSystem()) : getCampaign().getRankSystem());

        // Then, we can start creating the actual panel
        final JPanel rankSystemsPanel = new JScrollablePanel(new GridBagLayout());
        rankSystemsPanel.setName("rankSystemsPanel");

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        final JTextArea txtInstructionsRanks = new JTextArea(resources.getString("txtInstructionsRanks.text"));
        txtInstructionsRanks.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("txtInstructionsRanks.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructionsRanks.setOpaque(false);
        txtInstructionsRanks.setMinimumSize(new Dimension(250, 120));
        txtInstructionsRanks.setEditable(false);
        txtInstructionsRanks.setLineWrap(true);
        txtInstructionsRanks.setWrapStyleWord(true);
        rankSystemsPanel.add(txtInstructionsRanks, gbc);

        gbc.gridy++;
        rankSystemsPanel.add(createRankSystemPanel(), gbc);

        gbc.gridy++;
        rankSystemsPanel.add(createRanksTablePane(), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTH;
        rankSystemsPanel.add(createRankSystemFileButtonsPanel(), gbc);

        setViewportView(rankSystemsPanel);
        setPreferredSize(new Dimension(700, 400));
    }

    private JPanel createRankSystemPanel() {
        // Create Panel Components
        final JLabel lblRankSystem = new JLabel(resources.getString("lblRankSystem.text"));
        lblRankSystem.setToolTipText(resources.getString("lblRankSystem.toolTipText"));
        lblRankSystem.setName("lblRankSystem");

        final Comparator<String> comparator = new NaturalOrderComparator();
        setRankSystemModel(new SortedComboBoxModel<>(
                (systemA, systemB) -> comparator.compare(systemA.toString(), systemB.toString())));
        for (final RankSystem rankSystem : Ranks.getRankSystems().values()) {
            getRankSystemModel().addElement(rankSystem.getType().isDefault()
                    ? rankSystem : new RankSystem(rankSystem));
        }

        if (getSelectedRankSystem().getType().isCampaign()) {
            getRankSystemModel().addElement(getSelectedRankSystem());
        } else if (!getSelectedRankSystem().getType().isDefault()) {
            // We need to fix the referenced object in this case
            for (int i = 0; i < getRankSystemModel().getSize(); i++) {
                if (getSelectedRankSystem().equals(getRankSystemModel().getElementAt(i))) {
                    setSelectedRankSystem(getRankSystemModel().getElementAt(i));
                    break;
                }
            }
        }
        setComboRankSystems(new MMComboBox<>("comboRankSystems", getRankSystemModel()));
        getComboRankSystems().setToolTipText(resources.getString("lblRankSystem.toolTipText"));
        getComboRankSystems().setSelectedItem(getSelectedRankSystem());
        getComboRankSystems().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RankSystem) {
                    list.setToolTipText(((RankSystem) value).getDescription());
                }
                return this;
            }
        });
        getComboRankSystems().addActionListener(evt -> comboRankSystemChanged());

        setRankSystemTypeModel(new DefaultComboBoxModel<>(RankSystemType.values()));
        if (!getSelectedRankSystem().getType().isDefault()) {
            getRankSystemTypeModel().removeElement(RankSystemType.DEFAULT);
        }
        setComboRankSystemType(new MMComboBox<>("comboRankSystemType", getRankSystemTypeModel()));
        getComboRankSystemType().setToolTipText(resources.getString("comboRankSystemType.toolTipText"));
        getComboRankSystemType().setSelectedItem(getSelectedRankSystem().getType());
        getComboRankSystemType().setEnabled(!getSelectedRankSystem().getType().isDefault());
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

        final JButton btnCreateCustomRankSystem = new MMButton("btnCreateCustomRankSystem",
                resources.getString("btnCreateCustomRankSystem.text"), resources.getString("btnCreateCustomRankSystem.toolTipText"),
                evt -> createCustomRankSystem());

        // Programmatically Assign Accessibility Labels
        lblRankSystem.setLabelFor(getComboRankSystems());

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("rankSystemPanel.title")));
        panel.setName("rankSystemPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblRankSystem)
                                .addComponent(getComboRankSystems())
                                .addComponent(getComboRankSystemType())
                                .addComponent(btnCreateCustomRankSystem, GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRankSystem)
                                .addComponent(getComboRankSystems())
                                .addComponent(getComboRankSystemType())
                                .addComponent(btnCreateCustomRankSystem))
        );

        return panel;
    }

    private JScrollPane createRanksTablePane() {
        // Create Model
        setRanksTableModel(new RankTableModel(getSelectedRankSystem()));

        // Create Table
        setRanksTable(new JTable(getRanksTableModel()));
        getRanksTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getRanksTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        getRanksTable().setRowSelectionAllowed(false);
        getRanksTable().setColumnSelectionAllowed(false);
        getRanksTable().setCellSelectionEnabled(true);
        getRanksTable().setIntercellSpacing(new Dimension(0, 0));
        getRanksTable().setShowGrid(false);

        for (int i = 0; i < RankTableModel.COL_NUM; i++) {
            final TableColumn column = getRanksTable().getColumnModel().getColumn(i);
            column.setPreferredWidth(getRanksTableModel().getColumnWidth(i));
            column.setCellRenderer(getRanksTableModel().getRenderer());
            if (i == RankTableModel.COL_PAYMULT) {
                column.setCellEditor(new SpinnerCellEditor(new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1), true));
            }
        }

        // Create the Scroll Pane
        final JScrollPane pane = new JScrollPane(getRanksTable());
        pane.setName("ranksTableScrollPane");
        pane.setMinimumSize(new Dimension(1200, 400));
        pane.setPreferredSize(new Dimension(1200, 500));

        return pane;
    }

    private JPanel createRankSystemFileButtonsPanel() {
        final JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("rankSystemButtonsPanel.title")));
        panel.setName("rankSystemFileButtonsPanel");

        // Create the Buttons
        panel.add(new MMButton("btnExportCurrentRankSystem", resources.getString("btnExportCurrentRankSystem.text"),
                resources.getString("btnExportCurrentRankSystem.toolTipText"), evt -> {
            if (getSelectedRankSystem() != null) {
                updateRankSystem();
                getSelectedRankSystem().writeToFile(FileDialogs.saveIndividualRankSystem(getFrame()).orElse(null));
            }
        }));

        panel.add(new MMButton("btnExportUserDataRankSystems", resources.getString("btnExportUserDataRankSystems.text"),
                resources.getString("btnExportUserDataRankSystems.toolTipText"), evt -> exportUserDataRankSystems(true)));

        panel.add(new MMButton("btnExportRankSystems", resources.getString("btnExportRankSystems.text"),
                resources.getString("btnExportRankSystems.toolTipText"), evt -> {
            updateRankSystem();
            final List<RankSystem> rankSystems = new ArrayList<>();
            for (int i = 0; i < getRankSystemModel().getSize(); i++) {
                rankSystems.add(getRankSystemModel().getElementAt(i));
            }
            Ranks.exportRankSystemsToFile(FileDialogs.saveRankSystems(getFrame()).orElse(null), rankSystems);
        }));

        panel.add(new MMButton("btnImportIndividualRankSystem", resources.getString("btnImportIndividualRankSystem.text"),
                resources.getString("btnImportIndividualRankSystem.toolTipText"), evt -> {
            final RankSystem rankSystem = RankSystem.generateIndividualInstanceFromXML(
                    FileDialogs.openIndividualRankSystem(getFrame()).orElse(null));
            // Validate on load, to ensure we don't have any display issues
            if (new RankValidator().validate(getRankSystemModel(), rankSystem, true)) {
                getRankSystemModel().addElement(rankSystem);
            }
        }));

        panel.add(new MMButton("btnImportRankSystems", resources.getString("btnImportRankSystems.text"),
                resources.getString("btnImportRankSystems.toolTipText"), evt -> {
            final List<RankSystem> rankSystems = Ranks.loadRankSystemsFromFile(
                    FileDialogs.openRankSystems(getFrame()).orElse(null), RankSystemType.CAMPAIGN);
            final RankValidator rankValidator = new RankValidator();
            for (final RankSystem rankSystem : rankSystems) {
                if (rankValidator.validate(getRankSystemModel(), rankSystem, true)) {
                    getRankSystemModel().addElement(rankSystem);
                }
            }
        }));

        panel.add(new MMButton("btnRefreshRankSystemsFromFile", resources.getString("btnRefreshRankSystemsFromFile.text"),
                resources.getString("btnRefreshRankSystemsFromFile.toolTipText"),
                evt -> refreshRankSystems()));

        return panel;
    }
    //endregion Initialization

    //region Button Actions
    private void comboRankSystemChanged() {
        updateRankSystem();

        if ((getSelectedRankSystem() != null) && !getSelectedRankSystem().getType().isDefault()) {
            getRankSystemTypeModel().addElement(RankSystemType.DEFAULT);
        }

        // Then update the selected rank system, with null protection (although it shouldn't be null)
        setSelectedRankSystem(getRankSystemModel().getSelectedItem());
        if (getSelectedRankSystem() == null) {
            MekHQ.getLogger().error("The selected rank system is null. Not changing the ranks, just returning.");
            getComboRankSystemType().setEnabled(false);
            return;
        }

        // Update the model with the new rank data
        getRanksTableModel().setRankSystem(getSelectedRankSystem());
        for (int i = 0; i < RankTableModel.COL_NUM; i++) {
            final TableColumn column = getRanksTable().getColumnModel().getColumn(i);
            column.setPreferredWidth(getRanksTableModel().getColumnWidth(i));
            column.setCellRenderer(getRanksTableModel().getRenderer());
            if (i == RankTableModel.COL_PAYMULT) {
                column.setCellEditor(new SpinnerCellEditor(new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1), true));
            }
        }

        if (getSelectedRankSystem().getType().isDefault()) {
            getComboRankSystemType().setEnabled(false);
        } else {
            getRankSystemTypeModel().removeElement(RankSystemType.DEFAULT);
            getComboRankSystemType().setEnabled(true);
        }

        getComboRankSystemType().setSelectedItem(getSelectedRankSystem().getType());
    }

    private void updateRankSystem() {
        // Update the now old rank system with the changes done to it in the model
        if ((getSelectedRankSystem() != null) && !getSelectedRankSystem().getType().isDefault()) {
            getSelectedRankSystem().setType(getComboRankSystemType().getSelectedItem());
            getSelectedRankSystem().setRanks(getRanksTableModel().getRanks());
        }
    }

    private void createCustomRankSystem() {
        // We need to get the current Rank Systems from the rank system combo box for to ensure
        // the data's uniqueness
        final List<RankSystem> rankSystems = new ArrayList<>();
        for (int i = 0; i < getRankSystemModel().getSize(); i++) {
            rankSystems.add(getRankSystemModel().getElementAt(i));
        }

        // Now we can show the dialog and check if it was confirmed
        final CustomRankSystemCreationDialog dialog = new CustomRankSystemCreationDialog(getFrame(),
                rankSystems, getRanksTableModel().getRanks());
        if (dialog.showDialog().isConfirmed() && (dialog.getRankSystem() != null)) {
            // If it was we add the new rank system to the model
            getRankSystemModel().addElement(dialog.getRankSystem());
            // And select that item if that's intended
            if (dialog.getChkSwapToRankSystem().isSelected()) {
                getComboRankSystems().setSelectedItem(dialog.getRankSystem());
            }
        }
    }

    private void exportUserDataRankSystems(final boolean refresh) {
        updateRankSystem();
        final List<RankSystem> rankSystems = new ArrayList<>();
        for (int i = 0; i < getRankSystemModel().getSize(); i++) {
            final RankSystem rankSystem = getRankSystemModel().getElementAt(i);
            if (rankSystem.getType().isUserData()) {
                rankSystems.add(rankSystem);
            }
        }
        Ranks.exportRankSystemsToFile(new File(MekHqConstants.USER_RANKS_FILE_PATH), rankSystems);
        if (refresh) {
            refreshRankSystems();
        }
    }

    private void refreshRankSystems() {
        // Clear the selected rank system and reinitialize
        setSelectedRankSystem(null);
        Ranks.reinitializeRankSystems(getCampaign());

        // Then collect all of the campaign-type rank systems into a set, so we don't just throw
        // them away
        final Set<RankSystem> campaignRankSystems = new HashSet<>();
        for (int i = 0; i < getRankSystemModel().getSize(); i++) {
            final RankSystem rankSystem = getRankSystemModel().getElementAt(i);
            if (rankSystem.getType().isCampaign()) {
                campaignRankSystems.add(rankSystem);
            }
        }

        // Update the rank system model
        getRankSystemModel().removeAllElements();
        for (final RankSystem rankSystem : Ranks.getRankSystems().values()) {
            getRankSystemModel().addElement(new RankSystem(rankSystem));
        }

        // Revalidate all of the Campaign Rank Systems before adding, as we need to ensure no duplicate keys
        final RankValidator rankValidator = new RankValidator();
        for (final RankSystem rankSystem : campaignRankSystems) {
            // Validating against the core ranks is fine here, as we know all of the rank systems
            // we want to check against have been loaded there
            if (rankValidator.validate(rankSystem, true)) {
                getRankSystemModel().addElement(rankSystem);
            }
        }

        // Set the selected item
        getComboRankSystems().setSelectedItem(getCampaign().getRankSystem());
    }
    //endregion Button Actions

    public void applyToCampaign() {
        exportUserDataRankSystems(false);
        Ranks.reinitializeRankSystems(getCampaign());
        getCampaign().setRankSystem(getSelectedRankSystem());
    }
}
