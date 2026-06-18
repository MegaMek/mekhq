/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.panes;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import megamek.client.ui.baseComponents.SpinnerCellEditor;
import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.common.ui.FastJScrollPane;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.RankSystemType;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQScrollPane;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.dialog.CustomRankSystemCreationDialog;
import mekhq.gui.model.RankTableModel;

public class RankSystemsPane extends AbstractMHQScrollPane {
    private static final MMLogger LOGGER = MMLogger.create(RankSystemsPane.class);

    // region Variable Declarations
    private final Campaign campaign;
    private RankSystem selectedRankSystem;
    private boolean changed;

    // Rank System Panel
    private SortedComboBoxModel<RankSystem> rankSystemModel;
    private MMComboBox<RankSystem> comboRankSystems;
    private DefaultComboBoxModel<RankSystemType> rankSystemTypeModel;
    private MMComboBox<RankSystemType> comboRankSystemType;

    // Ranks Table Panel
    private JTable ranksTable;
    private JTable ranksRowHeaderTable;
    private RankTableModel ranksTableModel;
    /**
     * Single source of truth for column widths (model column index -> pixel width). Defaults to the model's wide
     * standalone widths; callers embedding this pane in a narrower layout (e.g. the Campaign Options dialog) inject
     * their own provider via {@link #setColumnWidthProvider(IntUnaryOperator)} so there is never a tug-of-war between
     * competing width listeners.
     */
    private transient IntUnaryOperator columnWidthProvider;
    // endregion Variable Declarations

    // region Constructors
    public RankSystemsPane(final JFrame frame, final Campaign campaign) {
        super(frame, "RankSystemsPane");
        this.campaign = campaign;
        setChanged(false);
        initialize();
    }
    // endregion Constructors

    // region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public @Nullable RankSystem getSelectedRankSystem() {
        return selectedRankSystem;
    }

    public void setSelectedRankSystem(final @Nullable RankSystem selectedRankSystem) {
        this.selectedRankSystem = selectedRankSystem;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(final boolean changed) {
        this.changed = changed;
    }

    // region Rank System Panel
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
    // endregion Rank System Panel

    // region Ranks Table Panel
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

    /**
     * @return the JTable that renders the frozen Rate column as a row header, or {@code null} if the table pane has
     *         not been built yet.
     */
    public @Nullable JTable getRanksRowHeaderTable() {
        return ranksRowHeaderTable;
    }

    /**
     * Sets the single source of truth for column widths (model column index -> pixel width) and immediately re-applies
     * it. Callers that resize the table to fit a narrower layout should use this instead of poking column widths
     * directly, so structure-changed events (rank system switches) and the "Restore default column widths" header menu
     * item all honour the same widths.
     *
     * @param provider a function mapping a model column index to a preferred width, or {@code null} to fall back to the
     *                 model's standalone widths
     */
    public void setColumnWidthProvider(final @Nullable IntUnaryOperator provider) {
        this.columnWidthProvider = provider;
        applyColumnWidthsAndRenderers();
    }
    // endregion Ranks Table Panel
    // endregion Getters/Setters

    // region Initialization

    /**
     * No Preferences are required here, so we don't call setPreferences. SelectedRankSystem will be non-null for the
     * initialization based on how the logic is set up, so we don't need to check to ensure that is the case
     */
    @Override
    protected void initialize() {
        // First, we have to initialize the selected rank system.
        setSelectedRankSystem(getCampaign().getRankSystem().getType().isCampaign()
                                    ? new RankSystem(getCampaign().getRankSystem())
                                    : getCampaign().getRankSystem());

        // Then, we can start creating the actual panel
        final AbstractMHQScrollablePanel rankSystemsPanel = new DefaultMHQScrollablePanel(getFrame(),
              "rankSystemsPanel", new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

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
                                                  ? rankSystem
                                                  : new RankSystem(rankSystem));
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
              resources.getString("btnCreateCustomRankSystem.text"),
              resources.getString("btnCreateCustomRankSystem.toolTipText"),
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
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                    .addComponent(lblRankSystem)
                                    .addComponent(getComboRankSystems())
                                    .addComponent(getComboRankSystemType())
                                    .addComponent(btnCreateCustomRankSystem, Alignment.LEADING)));

        layout.setHorizontalGroup(
              layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                                    .addComponent(lblRankSystem)
                                    .addComponent(getComboRankSystems())
                                    .addComponent(getComboRankSystemType())
                                    .addComponent(btnCreateCustomRankSystem)));

        return panel;
    }

    private JScrollPane createRanksTablePane() {
        // Create Model
        setRanksTableModel(new RankTableModel(getSelectedRankSystem()));

        // Main table. The custom JTableHeader surfaces RankTableModel.getToolTip(...) per column so users get the
        // profession/category description by hovering the header. The getToolTipText override also falls back to that
        // same per-column tooltip for the empty area below the last rank when the table fills its viewport.
        setRanksTable(new JTable(getRanksTableModel()) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return createColumnTooltipHeader(this);
            }

            @Override
            public String getToolTipText(MouseEvent event) {
                if (rowAtPoint(event.getPoint()) < 0) {
                    int viewIndex = columnAtPoint(event.getPoint());
                    if (viewIndex >= 0) {
                        int modelIndex = convertColumnIndexToModel(viewIndex);
                        return getRanksTableModel().getToolTip(modelIndex);
                    }
                }
                return super.getToolTipText(event);
            }
        });
        final JTable mainTable = getRanksTable();
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        mainTable.setRowSelectionAllowed(false);
        mainTable.setColumnSelectionAllowed(false);
        mainTable.setCellSelectionEnabled(true);
        mainTable.setIntercellSpacing(new Dimension(0, 0));
        mainTable.setShowGrid(false);

        // Build the frozen Rate column as a row-header table that scrolls vertically with the main table but stays
        // pinned horizontally. Auto-create-columns-from-model is disabled so the manually inserted Rate column
        // survives a structure-changed event (rank system switch) without being clobbered.
        ranksRowHeaderTable = createRanksRowHeaderTable(getRanksTableModel(), mainTable);

        // Apply default widths + renderers, then remove the Rate column from the main table so it only shows in the
        // row header. The TableModelListener re-applies these steps after every structure change (rank system switch).
        applyColumnWidthsAndRenderers();
        hideRateColumnFromMainTable();
        getRanksTableModel().addTableModelListener(event -> {
            if (event.getFirstRow() == TableModelEvent.HEADER_ROW) {
                SwingUtilities.invokeLater(() -> {
                    hideRateColumnFromMainTable();
                    applyColumnWidthsAndRenderers();
                });
            }
        });

        // Header right-click menu (auto-fit / restore defaults) on both the main header and the frozen Rate header.
        final JPopupMenu headerMenu = createHeaderPopupMenu();
        mainTable.getTableHeader().setComponentPopupMenu(headerMenu);
        ranksRowHeaderTable.getTableHeader().setComponentPopupMenu(headerMenu);

        // Scroll pane with the row header view + corner so the Rate header strip lines up with the rest of the headers.
        final JScrollPane pane = new FastJScrollPane(mainTable);
        pane.setRowHeaderView(ranksRowHeaderTable);
        pane.setCorner(JScrollPane.UPPER_LEFT_CORNER, ranksRowHeaderTable.getTableHeader());
        pane.setName("ranksTableScrollPane");
        pane.setMinimumSize(new Dimension(1200, 400));
        pane.setPreferredSize(new Dimension(1200, 500));

        return pane;
    }

    private JTableHeader createColumnTooltipHeader(final JTable forTable) {
        return new JTableHeader(forTable.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent event) {
                final TableColumnModel cm = getColumnModel();
                final int viewIndex = cm.getColumnIndexAtX(event.getPoint().x);
                if (viewIndex < 0) {
                    return null;
                }
                final int modelIndex = cm.getColumn(viewIndex).getModelIndex();
                return getRanksTableModel().getToolTip(modelIndex);
            }
        };
    }

    private JTable createRanksRowHeaderTable(final RankTableModel model, final JTable mainTable) {
        final JTable rh = new JTable(model) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return createColumnTooltipHeader(this);
            }
        };
        rh.setAutoCreateColumnsFromModel(false);
        rh.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        rh.setRowSelectionAllowed(false);
        rh.setColumnSelectionAllowed(false);
        rh.setCellSelectionEnabled(false);
        rh.setIntercellSpacing(new Dimension(0, 0));
        rh.setShowGrid(false);
        rh.setFocusable(false);

        // Replace the auto-created column set with a single column bound to the Rate model index. We do this on the
        // row header (not the main table) so the wide profession columns survive auto-rebuilds after rank-system
        // changes without us having to re-add the Rate TableColumn each time.
        final TableColumnModel cm = rh.getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0));
        }
        final TableColumn rateColumn = new TableColumn(RankTableModel.COL_NAME_RATE);
        rateColumn.setHeaderValue(model.getColumnName(RankTableModel.COL_NAME_RATE));
        rateColumn.setPreferredWidth(columnWidthFor(RankTableModel.COL_NAME_RATE));
        rateColumn.setCellRenderer(model.getRenderer());
        rh.addColumn(rateColumn);

        rh.setRowHeight(mainTable.getRowHeight());
        mainTable.addPropertyChangeListener("rowHeight", evt -> rh.setRowHeight(mainTable.getRowHeight()));
        return rh;
    }

    private void hideRateColumnFromMainTable() {
        final JTable table = getRanksTable();
        if (table == null) {
            return;
        }
        final TableColumnModel cm = table.getColumnModel();
        for (int viewIndex = 0; viewIndex < cm.getColumnCount(); viewIndex++) {
            final TableColumn column = cm.getColumn(viewIndex);
            if (column.getModelIndex() == RankTableModel.COL_NAME_RATE) {
                table.removeColumn(column);
                return;
            }
        }
    }

    /**
     * @param modelIndex a model column index
     *
     * @return the configured width for that column, using the injected {@link #columnWidthProvider} when present and
     *         otherwise the model's standalone width
     */
    private int columnWidthFor(final int modelIndex) {
        if (columnWidthProvider != null) {
            return columnWidthProvider.applyAsInt(modelIndex);
        }
        return getRanksTableModel().getColumnWidth(modelIndex);
    }

    private void applyColumnWidthsAndRenderers() {
        final RankTableModel model = getRanksTableModel();
        final JTable table = getRanksTable();
        if ((model == null) || (table == null)) {
            return;
        }
        final TableColumnModel cm = table.getColumnModel();
        for (int viewIndex = 0; viewIndex < cm.getColumnCount(); viewIndex++) {
            final TableColumn column = cm.getColumn(viewIndex);
            final int modelIndex = column.getModelIndex();
            final int width = columnWidthFor(modelIndex);
            column.setPreferredWidth(width);
            column.setMinWidth(0);
            column.setCellRenderer(model.getRenderer());
            if (modelIndex == RankTableModel.COL_PAY_MULTI) {
                column.setCellEditor(new SpinnerCellEditor(new SpinnerNumberModel(1.0, 0.0, 10.0, 0.1), true));
            }
        }
        if (ranksRowHeaderTable != null) {
            final TableColumn rateColumn = ranksRowHeaderTable.getColumnModel().getColumn(0);
            final int rateWidth = columnWidthFor(RankTableModel.COL_NAME_RATE);
            rateColumn.setPreferredWidth(rateWidth);
            rateColumn.setMinWidth(rateWidth);
            rateColumn.setMaxWidth(rateWidth);
            rateColumn.setCellRenderer(model.getRenderer());
            ranksRowHeaderTable.setPreferredScrollableViewportSize(
                  new Dimension(rateWidth, ranksRowHeaderTable.getPreferredSize().height));
            ranksRowHeaderTable.revalidate();
        }
    }

    private JPopupMenu createHeaderPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();

        final JMenuItem autoFit = new JMenuItem(resources.getString("ranksTable.headerMenu.autoFit.text"));
        autoFit.addActionListener(evt -> autoFitAllColumns());
        menu.add(autoFit);

        final JMenuItem reset = new JMenuItem(resources.getString("ranksTable.headerMenu.reset.text"));
        reset.addActionListener(evt -> applyColumnWidthsAndRenderers());
        menu.add(reset);

        return menu;
    }

    private void autoFitAllColumns() {
        autoFitColumnWidths(getRanksTable());
        if (ranksRowHeaderTable != null) {
            autoFitColumnWidths(ranksRowHeaderTable);
            ranksRowHeaderTable.revalidate();
        }
    }

    private static void autoFitColumnWidths(final JTable table) {
        if (table == null) {
            return;
        }
        final TableColumnModel cm = table.getColumnModel();
        final JTableHeader header = table.getTableHeader();
        for (int viewIndex = 0; viewIndex < cm.getColumnCount(); viewIndex++) {
            final TableColumn column = cm.getColumn(viewIndex);
            TableCellRenderer headerRenderer = column.getHeaderRenderer();
            if (headerRenderer == null) {
                headerRenderer = header.getDefaultRenderer();
            }
            final Component headerComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(),
                  false, false, -1, viewIndex);
            int width = headerComp.getPreferredSize().width;
            for (int row = 0; row < table.getRowCount(); row++) {
                final TableCellRenderer cellRenderer = table.getCellRenderer(row, viewIndex);
                final Component cellComp = table.prepareRenderer(cellRenderer, row, viewIndex);
                width = Math.max(width, cellComp.getPreferredSize().width);
            }
            // Small padding so glyphs don't visually touch the column border.
            column.setPreferredWidth(width + 6);
        }
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
                getSelectedRankSystem()
                      .writeToFile(FileDialogs.saveIndividualRankSystem(getFrame()).orElse(null));
            }
        }));

        panel.add(new MMButton("btnExportUserDataRankSystems", resources.getString("btnExportUserDataRankSystems.text"),
              resources.getString("btnExportUserDataRankSystems.toolTipText"),
              evt -> exportUserDataRankSystems(true)));

        panel.add(new MMButton("btnExportRankSystems", resources.getString("btnExportRankSystems.text"),
              resources.getString("btnExportRankSystems.toolTipText"), evt -> {
            updateRankSystem();
            final List<RankSystem> rankSystems = new ArrayList<>();
            for (int i = 0; i < getRankSystemModel().getSize(); i++) {
                rankSystems.add(getRankSystemModel().getElementAt(i));
            }
            Ranks.exportRankSystemsToFile(FileDialogs.saveRankSystems(getFrame()).orElse(null), rankSystems);
        }));

        panel.add(
              new MMButton("btnImportIndividualRankSystem", resources.getString("btnImportIndividualRankSystem.text"),
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

        panel.add(
              new MMButton("btnRefreshRankSystemsFromFile", resources.getString("btnRefreshRankSystemsFromFile.text"),
                    resources.getString("btnRefreshRankSystemsFromFile.toolTipText"),
                    evt -> refreshRankSystems()));

        return panel;
    }
    // endregion Initialization

    // region Button Actions
    private void comboRankSystemChanged() {
        updateRankSystem();

        if ((getSelectedRankSystem() != null) && !getSelectedRankSystem().getType().isDefault()) {
            getRankSystemTypeModel().addElement(RankSystemType.DEFAULT);
        }

        // Then update the selected rank system, with null protection (although it
        // shouldn't be null)
        setSelectedRankSystem(getRankSystemModel().getSelectedItem());
        if (getSelectedRankSystem() == null) {
            LOGGER.error("The selected rank system is null. Not changing the ranks, just returning.");
            getComboRankSystemType().setEnabled(false);
            return;
        }

        // Update the model with the new rank data. setRankSystem fires a structure-changed event that triggers JTable
        // to rebuild its columns from scratch (which re-adds the Rate column). Re-hide it from the main table and
        // re-apply widths/renderers/editors synchronously here; the TableModelListener registered in
        // createRanksTablePane() will run the same steps again via invokeLater as a safety net.
        getRanksTableModel().setRankSystem(getSelectedRankSystem());
        hideRateColumnFromMainTable();
        applyColumnWidthsAndRenderers();

        if (getSelectedRankSystem().getType().isDefault()) {
            getComboRankSystemType().setEnabled(false);
        } else {
            getRankSystemTypeModel().removeElement(RankSystemType.DEFAULT);
            getComboRankSystemType().setEnabled(true);
        }

        getComboRankSystemType().setSelectedItem(getSelectedRankSystem().getType());
    }

    private void updateRankSystem() {
        setChanged(true);
        // Update the now old rank system with the changes done to it in the model
        if ((getSelectedRankSystem() != null) && !getSelectedRankSystem().getType().isDefault()) {
            getSelectedRankSystem().setType(getComboRankSystemType().getSelectedItem());
            getSelectedRankSystem().setRanks(getRanksTableModel().getRanks());
        }
    }

    private void createCustomRankSystem() {
        // We need to get the current Rank Systems from the rank system combo box for to
        // ensure
        // the data's uniqueness
        final List<RankSystem> rankSystems = new ArrayList<>();
        for (int i = 0; i < getRankSystemModel().getSize(); i++) {
            rankSystems.add(getRankSystemModel().getElementAt(i));
        }

        // Now we can show the dialog and check if it was confirmed
        final CustomRankSystemCreationDialog dialog = new CustomRankSystemCreationDialog(getFrame(),
              rankSystems, getRanksTableModel().getRanks());
        if (dialog.showDialog().isConfirmed() && (dialog.getRankSystem() != null)) {
            // We've made changes
            setChanged(true);
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
        Ranks.exportRankSystemsToFile(new File(MHQConstants.USER_RANKS_FILE_PATH), rankSystems);
        if (refresh) {
            refreshRankSystems();
        }
    }

    private void refreshRankSystems() {
        // If this occurs, something has changed
        setChanged(true);

        // Clear the selected rank system and reinitialize
        setSelectedRankSystem(null);
        Ranks.reinitializeRankSystems(getCampaign());

        // Then collect all the campaign-type rank systems into a set, so we don't
        // just throw
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

        // Revalidate all the Campaign Rank Systems before adding, as we need to
        // ensure no duplicate keys
        final RankValidator rankValidator = new RankValidator();
        for (final RankSystem rankSystem : campaignRankSystems) {
            // Validating against the core ranks is fine here, as we know all the rank
            // systems
            // we want to check against have been loaded there
            if (rankValidator.validate(rankSystem, true)) {
                getRankSystemModel().addElement(rankSystem);
            }
        }

        // Set the selected item
        getComboRankSystems().setSelectedItem(getCampaign().getRankSystem());
    }
    // endregion Button Actions

    public void applyToCampaign() {
        exportUserDataRankSystems(false);
        Ranks.reinitializeRankSystems(getCampaign());
        getCampaign().setRankSystem(getSelectedRankSystem());
    }
}
