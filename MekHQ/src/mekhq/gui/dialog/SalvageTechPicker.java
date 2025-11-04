/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.camOpsSalvage.SalvageTechData;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Modal dialog that lets the user pick one or more salvage technicians from a tabular list. The table supports sorting,
 * pre-selection, and exposes results via {@link #wasConfirmed()} and {@link #getSelectedTechs()}.
 *
 * <p>The dialog shows an instructions panel, a scrollable table when tech data exists, and Cancel/Confirm buttons.
 * The Confirm button is only present when at least one tech is available.</p>
 *
 * <p>Use {@link #wasConfirmed()} to check whether the user confirmed and {@link #getSelectedTechs()} to retrieve
 * the chosen techs after the dialog closes.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class SalvageTechPicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SalvageTechPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageTechPicker";

    private static final Dimension DIMENSION = scaleForGUI(800, 600);
    private static final int WIDTH_60 = scaleForGUI(60);
    private static final int WIDTH_80 = scaleForGUI(80);
    private static final int WIDTH_100 = scaleForGUI(100);

    private boolean wasConfirmed;
    private SalvageTechTableModel tableModel;

    /**
     * Checks whether the user confirmed their tech selection.
     *
     * @return {@code true} if the user pressed Confirm; {@code false} if they canceled or closed the dialog.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean wasConfirmed() {
        return wasConfirmed;
    }

    /**
     * Returns all selected person {@link UUID}s from the table. If the table was never constructed (e.g., no techs were
     * provided), returns an empty list.
     *
     * @return a list of selected person UUIDs (never {@code null})
     *
     * @author Illiani
     * @since 0.50.10
     */
    public List<UUID> getSelectedTechs() {
        if (tableModel == null) {
            return new ArrayList<>();
        }
        return tableModel.getSelectedTechs();
    }

    /**
     * Creates and shows a modal picker dialog for salvage technicians.
     *
     * @param techs                list of available technicians to display. When {@code null} or empty, only
     *                             instructions and a Cancel button are shown.
     * @param alreadySelectedTechs list of tech UUIDs that should start as pre-selected.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvageTechPicker(List<SalvageTechData> techs, List<UUID> alreadySelectedTechs) {
        boolean hasTechs = techs != null && !techs.isEmpty();

        setTitle(getText("accessingTerminal.title"));
        setModal(true);
        setLayout(new BorderLayout());

        // Instructions at the top
        JPanel instructionsPanel = new JPanel();
        JTextArea instructionsLabel = new JTextArea(getInstructions());
        instructionsLabel.setLineWrap(true);
        instructionsLabel.setWrapStyleWord(true);
        instructionsLabel.setEditable(false);
        instructionsLabel.setOpaque(false);
        instructionsLabel.setColumns(60);
        instructionsLabel.setRows(0);
        instructionsPanel.add(instructionsLabel);
        add(instructionsPanel, BorderLayout.NORTH);

        // Table in the center
        if (hasTechs) {
            tableModel = new SalvageTechTableModel(techs, alreadySelectedTechs);
            JTable table = new JTable(tableModel);
            table.setAutoCreateRowSorter(true);

            formatSorters(table);

            @SuppressWarnings("unchecked")
            TableRowSorter<SalvageTechTableModel> sorter =
                  (TableRowSorter<SalvageTechTableModel>) table.getRowSorter();
            List<javax.swing.RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new javax.swing.RowSorter.SortKey(
                  SalvageTechTableModel.COL_SELECT,
                  javax.swing.SortOrder.DESCENDING));
            sorter.setSortKeys(sortKeys);

            assignWidths(table);
            setRenderers(table);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(DIMENSION);
            add(scrollPane, BorderLayout.CENTER);
        }

        // Buttons at the bottom
        JPanel buttonPanel = new JPanel();
        getButtons(hasTechs, buttonPanel);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Installs a checkbox renderer for the Select column so that boolean values are shown as centered checkboxes.
     *
     * @param table the table to update
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void setRenderers(JTable table) {
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_SELECT).setCellRenderer(
              new javax.swing.table.DefaultTableCellRenderer() {
                  private final JCheckBox checkBox = new JCheckBox();

                  @Override
                  public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {
                      checkBox.setSelected(value != null && (Boolean) value);
                      checkBox.setHorizontalAlignment(
                            javax.swing.JLabel.CENTER);
                      checkBox.setBackground(
                            isSelected ? table.getSelectionBackground()
                                  : table.getBackground());
                      return checkBox;
                  }
              });
    }

    /**
     * Applies preferred widths to all columns for a readable layout.
     *
     * @param table the table to update
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void assignWidths(JTable table) {
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_SELECT)
              .setPreferredWidth(WIDTH_60);
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_RANK)
              .setPreferredWidth(WIDTH_100);
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_FIRST_NAME)
              .setPreferredWidth(WIDTH_100);
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_LAST_NAME)
              .setPreferredWidth(WIDTH_100);
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_SKILL_LEVEL)
              .setPreferredWidth(WIDTH_80);
        table.getColumnModel().getColumn(SalvageTechTableModel.COL_INJURIES)
              .setPreferredWidth(WIDTH_60);
        table.getColumnModel().getColumn(
                    SalvageTechTableModel.COL_MINUTES_AVAILABLE)
              .setPreferredWidth(WIDTH_80);
    }

    /**
     * Configures column comparators to provide intuitive sorting for boolean, rank (by numeric value), natural string
     * order, and integer columns.
     *
     * @param table the table whose sorter will be configured
     *
     * @implNote Rank sorting looks up the underlying row's numeric rank value to avoid lexicographic errors
     *       (e.g., "Sergeant" vs "Private").
     * @author Illiani
     * @since 0.50.10
     */
    private static void formatSorters(JTable table) {
        try {
            @SuppressWarnings("unchecked")
            TableRowSorter<SalvageTechTableModel> sorter =
                  (TableRowSorter<SalvageTechTableModel>) table.getRowSorter();

            // Table sorting
            sorter.setComparator(SalvageTechTableModel.COL_SELECT, (b1, b2) ->
                                                                         Boolean.compare(((Boolean) b1),
                                                                               ((Boolean) b2)));

            // Precompute a map from rank string to numeric value for fast lookup
            SalvageTechTableModel model = sorter.getModel();
            java.util.Map<String, Integer> rankToNumeric = new java.util.HashMap<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object rankObj = model.getValueAt(i, SalvageTechTableModel.COL_RANK);
                if (rankObj != null) {
                    rankToNumeric.put(rankObj.toString(), model.getRankNumeric(i));
                }
            }

            sorter.setComparator(SalvageTechTableModel.COL_RANK, (o1, o2) -> {
                Integer n1 = rankToNumeric.get(o1 != null ? o1.toString() : null);
                Integer n2 = rankToNumeric.get(o2 != null ? o2.toString() : null);
                if (n1 != null && n2 != null) {
                    return Integer.compare(n1, n2);
                }
                return 0;
            });

            sorter.setComparator(SalvageTechTableModel.COL_FIRST_NAME,
                  new NaturalOrderComparator());
            sorter.setComparator(SalvageTechTableModel.COL_LAST_NAME,
                  new NaturalOrderComparator());
            sorter.setComparator(SalvageTechTableModel.COL_SKILL_LEVEL,
                  new NaturalOrderComparator());
            sorter.setComparator(SalvageTechTableModel.COL_INJURIES,
                  Comparator.comparingInt(i -> ((int) i)));
            sorter.setComparator(
                  SalvageTechTableModel.COL_MINUTES_AVAILABLE,
                  Comparator.comparingInt(i -> ((int) i)));
        } catch (ClassCastException e) {
            // There's a lot of class casting, so we want to catch anything that
            // is malformed. For example, if the underlying data structure in
            // the table changes.
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Localized instructional text for the dialog header.
     *
     * @return the localized instructions string
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getInstructions() {
        return getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.instructions");
    }

    /**
     * Adds Cancel (always) and Confirm (only if techs exist) buttons to the provided panel and wires up their actions
     * to close the dialog and set {@link #wasConfirmed}.
     *
     * @param hasTechs    whether any techs were provided (controls Confirm visibility)
     * @param buttonPanel panel to populate
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void getButtons(boolean hasTechs, JPanel buttonPanel) {
        RoundedJButton btnCancel = new RoundedJButton(getText("Cancel.text"));
        btnCancel.addActionListener(evt -> {
            wasConfirmed = false;
            dispose();
        });

        RoundedJButton btnConfirm = new RoundedJButton(getText("Confirm.text"));
        btnConfirm.addActionListener(evt -> {
            wasConfirmed = true;
            dispose();
        });

        buttonPanel.add(btnCancel);

        if (hasTechs) {
            buttonPanel.add(btnConfirm);
        }
    }

    /**
     * Table model backing the salvage tech selection grid. Provides typed columns, pre-selection, and helpers for
     * retrieving selected tech IDs and for comparing ranks by numeric strength.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static class SalvageTechTableModel extends AbstractTableModel {
        /** Column index for Select (checkbox). */
        private static final int COL_SELECT = 0;
        /** Column index for textual Rank. */
        private static final int COL_RANK = 1;
        /** Column index for first name. */
        private static final int COL_FIRST_NAME = 2;
        /** Column index for last name. */
        private static final int COL_LAST_NAME = 3;
        /** Column index for skill level name. */
        private static final int COL_SKILL_LEVEL = 4;
        /** Column index for injury count. */
        private static final int COL_INJURIES = 5;
        /** Column index for available minutes. */
        private static final int COL_MINUTES_AVAILABLE = 6;

        private final List<SalvageTechData> techs;
        private final boolean[] selected;

        private static final String[] COLUMN_NAMES = {
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.select"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.rank"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.firstName"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.lastName"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.skill"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.injuries"),
              getTextAt(RESOURCE_BUNDLE, "SalvageTechPicker.column.minutes")
        };

        /**
         * Constructs the model.
         *
         * @param techs                list of rows to display (required, not {@code null})
         * @param alreadySelectedTechs UUIDs to pre-select; may be {@code null}
         *
         * @author Illiani
         * @since 0.50.10
         */
        public SalvageTechTableModel(List<SalvageTechData> techs, List<UUID> alreadySelectedTechs) {
            this.techs = techs;
            this.selected = new boolean[techs.size()];

            // Pre-select checkboxes for techs that are already selected
            for (int i = 0; i < techs.size(); i++) {
                UUID techId = techs.get(i).tech().getId();
                if (alreadySelectedTechs.contains(techId)) {
                    selected[i] = true;
                }
            }
        }

        @Override
        public int getRowCount() {
            return techs.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_SELECT -> Boolean.class;
                case COL_RANK, COL_FIRST_NAME, COL_LAST_NAME, COL_SKILL_LEVEL -> String.class;
                case COL_INJURIES, COL_MINUTES_AVAILABLE -> Integer.class;
                default -> Object.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_SELECT;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SalvageTechData data = techs.get(rowIndex);

            return switch (columnIndex) {
                case COL_SELECT -> selected[rowIndex];
                case COL_RANK -> data.rank();
                case COL_FIRST_NAME -> data.firstName();
                case COL_LAST_NAME -> data.lastName();
                case COL_SKILL_LEVEL -> data.skillLevelName();
                case COL_INJURIES -> data.injuries();
                case COL_MINUTES_AVAILABLE -> data.minutesAvailable();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == COL_SELECT) {
                selected[rowIndex] = (Boolean) value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        /**
         * Returns the {@link UUID} instances corresponding to rows whose Select checkbox is enabled.
         *
         * @return list of selected tech UUIDs (never {@code null})
         *
         * @author Illiani
         * @since 0.50.10
         */
        public List<UUID> getSelectedTechs() {
            List<UUID> selectedTechs = new ArrayList<>();
            for (int i = 0; i < techs.size(); i++) {
                if (selected[i]) {
                    selectedTechs.add(techs.get(i).tech().getId());
                }
            }
            return selectedTechs;
        }

        /**
         * Returns the numeric rank value for the tech at the specified row. Useful for comparator logic to ensure
         * correct ordering.
         *
         * @param rowIndex the row index
         *
         * @return the numeric rank value
         *
         * @author Illiani
         * @since 0.50.10
         */
        public int getRankNumeric(int rowIndex) {
            return techs.get(rowIndex).rankNumeric();
        }
    }
}
