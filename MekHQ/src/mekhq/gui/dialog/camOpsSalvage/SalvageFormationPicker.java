/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.camOpsSalvage;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.messageSurroundedBySpanWithColor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.camOpsSalvage.SalvageFormationData;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Modal dialog that lists available formations capable of participating in salvage operations and lets the user select one
 * or more for the current task.
 *
 * <p>The center of the dialog is a sortable {@link JTable} backed by
 * {@link SalvageFormationTableModel}. Columns include formation name/type, assigned tech (with
 * experience/rank-aware sort), cargo/tow capacities, salvage-capable unit count, and a tug availability flag. Tooltips
 * provide detailed capacity reasoning and tech status.</p>
 *
 * <p>Use {@link #wasConfirmed()} to check whether the user confirmed and {@link #getSelectedFormations()} to retrieve
 * the chosen formations after the dialog closes.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class SalvageFormationPicker extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(SalvageFormationPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.SalvageFormationPicker";

    private static final Dimension DIMENSION = scaleForGUI(800, 600);
    private static final int WIDTH_60 = scaleForGUI(60);
    private static final int WIDTH_80 = scaleForGUI(80);
    private static final int WIDTH_100 = scaleForGUI(100);
    private static final int WIDTH_150 = scaleForGUI(150);

    private boolean wasConfirmed;
    private final SalvageFormationTableModel tableModel;

    /**
     * Checks whether the user confirmed their formation selection.
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
     * Returns all selected {@link Formation}s from the table. If the table was never constructed (e.g., no formations were
     * provided), returns an empty list.
     *
     * @return a list of selected formations (never {@code null})
     *
     * @author Illiani
     * @since 0.50.10
     */
    public List<Formation> getSelectedFormations() {
        if (tableModel == null) {
            return new ArrayList<>();
        }
        return tableModel.getSelectedFormations();
    }

    /**
     * Creates and displays the salvage formation picker dialog.
     *
     * <p>The dialog builds a sortable table when any formations are provided. The tug column is shown only for space
     * operations. A read-only instruction panel is shown at the top, and Confirm/Cancel controls are placed at the
     * bottom.</p>
     *
     * @param campaign            current campaign context; used for tech labels, experience, tooltips, and hangar
     *                            lookups
     * @param formations              the candidate salvage-capable formations with precomputed stats; may be {@code null} or
     *                            empty
     * @param isSpaceOperation    {@code true} to show space-specific columns (e.g., tug availability); {@code false} to
     *                            hide them
     * @param priorSelectedFormations a list of formations that were previously selected
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvageFormationPicker(Campaign campaign, List<SalvageFormationData> formations, boolean isSpaceOperation,
                                  List<Integer> priorSelectedFormations) {
        setTitle(getText("accessingTerminal.title"));
        setModal(true);
        setLayout(new BorderLayout());

        // Instructions at the top
        JPanel instructionsPanel = new JPanel(new BorderLayout());
        JTextArea instructionsLabel = new JTextArea(getInstructions());
        instructionsLabel.setLineWrap(true);
        instructionsLabel.setWrapStyleWord(true);
        instructionsLabel.setEditable(false);
        instructionsLabel.setOpaque(false);
        instructionsLabel.setColumns(70);
        instructionsLabel.setRows(4);
        instructionsPanel.add(instructionsLabel, BorderLayout.CENTER);
        add(instructionsPanel, BorderLayout.NORTH);

        // Table in the center
        tableModel = new SalvageFormationTableModel(campaign, formations, priorSelectedFormations);
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);

        formatSorters(table);
        assignWidths(table, isSpaceOperation);
        setRenderers(campaign, table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(DIMENSION);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons at the bottom
        JPanel buttonPanel = new JPanel();
        getButtons(buttonPanel);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Installs cell renderers for the table, including a centered checkbox for selectable columns and a renderer that
     * applies status coloring to wounded techs and sets helpful tooltips.
     *
     * @param campaign campaign context for tooltips and status coloring
     * @param table    the table to configure
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void setRenderers(Campaign campaign, JTable table) {
        table.setDefaultRenderer(Object.class, new SalvageFormationTableCellRenderer(campaign));
        table.setDefaultRenderer(Boolean.class, new SalvageFormationTableCellRenderer(campaign));
        table.setDefaultRenderer(String.class, new SalvageFormationTableCellRenderer(campaign));
        table.setDefaultRenderer(Double.class, new SalvageFormationTableCellRenderer(campaign));
        table.setDefaultRenderer(Integer.class, new SalvageFormationTableCellRenderer(campaign));

        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_SELECT).setCellRenderer(
              new javax.swing.table.DefaultTableCellRenderer() {
                  private final javax.swing.JCheckBox checkBox = new javax.swing.JCheckBox();

                  @Override
                  public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                      checkBox.setSelected(value != null && (Boolean) value);
                      checkBox.setHorizontalAlignment(javax.swing.JLabel.CENTER);
                      checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                      return checkBox;
                  }
              });
    }

    /**
     * Sets preferred column widths and hides the tug column for ground operations.
     *
     * @param table            table to adjust
     * @param isSpaceOperation whether the current operation is in space (tug column visible) or ground (tug column
     *                         hidden)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void assignWidths(JTable table, boolean isSpaceOperation) {
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_SELECT).setPreferredWidth(WIDTH_60);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_FORMATION_NAME).setPreferredWidth(WIDTH_150);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_FORMATION_TYPE).setPreferredWidth(WIDTH_100);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_TOE_TECH).setPreferredWidth(WIDTH_150);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_CREW_TECHS).setPreferredWidth(WIDTH_60);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_CARGO_CAPACITY).setPreferredWidth(WIDTH_100);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_TOW_CAPACITY).setPreferredWidth(WIDTH_80);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_SALVAGE_UNITS).setPreferredWidth(WIDTH_80);
        table.getColumnModel().getColumn(SalvageFormationTableModel.COL_HAS_TUG).setPreferredWidth(WIDTH_80);

        // Hide Tug column for ground operations
        if (isSpaceOperation) {
            table.getColumnModel().getColumn(SalvageFormationTableModel.COL_HAS_TUG).setPreferredWidth(WIDTH_80);
        } else {
            table.getColumnModel().getColumn(SalvageFormationTableModel.COL_HAS_TUG).setMinWidth(0);
            table.getColumnModel().getColumn(SalvageFormationTableModel.COL_HAS_TUG).setMaxWidth(0);
            table.getColumnModel().getColumn(SalvageFormationTableModel.COL_HAS_TUG).setPreferredWidth(0);
        }
    }

    /**
     * Configures the {@link TableRowSorter} with comparators for each column:
     *
     * <ul>
     *   <li>Select/Has Tug: boolean compare</li>
     *   <li>Formation Name: natural order (human-friendly alphanumerics)</li>
     *   <li>Formation Type: prioritizes "Salvage" types, then natural order</li>
     *   <li>Tech: experience level (desc), rank (desc), then full name</li>
     *   <li>Cargo/Tow: numeric ascending</li>
     *   <li>Salvage Units: integer ascending</li>
     * </ul>
     *
     * <p>Any type mismatches are logged and ignored to avoid crashing if upstream data changes.</p>
     *
     * @param table the table whose sorter will be configured
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void formatSorters(JTable table) {
        try {
            @SuppressWarnings("unchecked")
            TableRowSorter<SalvageFormationTableModel> sorter = (TableRowSorter<SalvageFormationTableModel>) table.getRowSorter();

            // Table sorting
            sorter.setComparator(SalvageFormationTableModel.COL_SELECT, (b1, b2) ->
                                                                          Boolean.compare(((Boolean) b1),
                                                                                ((Boolean) b2)));
            sorter.setComparator(SalvageFormationTableModel.COL_FORMATION_NAME,
                  new NaturalOrderComparator());
            sorter.setComparator(SalvageFormationTableModel.COL_FORMATION_TYPE,
                  (s1, s2) -> formationTypeComparator((String) s1, (String) s2));
            // Sort by experience level, then rank numeric, then full name
            sorter.setComparator(SalvageFormationTableModel.COL_TOE_TECH,
                  (o1, o2) -> {
                      SalvageFormationTableModel model = (SalvageFormationTableModel) table.getModel();
                      int row1 = -1;
                      int row2 = -1;

                      // Find which rows have these values
                      for (int i = 0; i < model.getRowCount(); i++) {
                          if (Objects.equals(model.getValueAt(i, SalvageFormationTableModel.COL_TOE_TECH), o1)) {
                              row1 = i;
                          }
                          if (Objects.equals(model.getValueAt(i, SalvageFormationTableModel.COL_TOE_TECH), o2)) {
                              row2 = i;
                          }
                      }

                      if (row1 == -1 || row2 == -1) {
                          return 0;
                      }

                      return techComparator(model, row1, row2);
                  });
            sorter.setComparator(SalvageFormationTableModel.COL_CARGO_CAPACITY,
                  Comparator.comparingDouble(d -> ((double) d)));
            sorter.setComparator(SalvageFormationTableModel.COL_TOW_CAPACITY,
                  Comparator.comparingDouble(d -> ((double) d)));
            sorter.setComparator(SalvageFormationTableModel.COL_SALVAGE_UNITS,
                  Comparator.comparingInt(i -> ((int) i)));
            sorter.setComparator(SalvageFormationTableModel.COL_CREW_TECHS,
                  Comparator.comparingInt(i -> ((int) i)));
            sorter.setComparator(SalvageFormationTableModel.COL_HAS_TUG, (b1, b2) ->
                                                                           Boolean.compare(((Boolean) b1),
                                                                                 ((Boolean) b2)));
        } catch (ClassCastException e) {
            // There's a lot of class casting so we want to catch anything that is malformed. For example, if the
            // underlying data structure in the table changes.
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Compares two rows in the Tech column using:
     *
     * <ol>
     *   <li>Experience level (descending; more experienced first)</li>
     *   <li>Rank numeric (descending; higher rank first)</li>
     *   <li>Full name (ascending)</li>
     * </ol>
     *
     * <p>{@code null} techs sort last.</p>
     *
     * @param model backing table model
     * @param row1  first model row
     * @param row2  second model row
     *
     * @return negative if row1 &lt; row2 under the ordering, positive if row1 &gt; row2, or 0 if equal
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int techComparator(SalvageFormationTableModel model, int row1, int row2) {
        SalvageFormationData data1 = model.formations.get(row1);
        SalvageFormationData data2 = model.formations.get(row2);

        Person tech1 = data1.tech();
        Person tech2 = data2.tech();

        // Handle null techs - should sort last
        if (tech1 == null && tech2 == null) {return 0;}
        if (tech1 == null) {return 1;}
        if (tech2 == null) {return -1;}

        // Compare by experience level
        boolean isTechSecondary1 = tech1.getSecondaryRole().isTechSecondary();
        boolean isTechSecondary2 = tech2.getSecondaryRole().isTechSecondary();

        int expLevel1 = tech1.getExperienceLevel(model.campaign, isTechSecondary1, true);
        int expLevel2 = tech2.getExperienceLevel(model.campaign, isTechSecondary2, true);

        int expCompare = Integer.compare(expLevel2, expLevel1); // Reversed (highest -> lowest, more experienced first)
        if (expCompare != 0) {return expCompare;}

        // If experience levels are equal, compare by rank (lowest to highest)
        int rankCompare = Integer.compare(tech2.getRankNumeric(), tech1.getRankNumeric());
        if (rankCompare != 0) {return rankCompare;}

        // If ranks are equal, compare by full name
        return tech1.getFullName().compareTo(tech2.getFullName());
    }

    /**
     * Comparator used for the Formation Type column. Ensures types with the display name containing
     * {@link FormationType#SALVAGE} sort before others, then falls back to natural-order comparison of the type labels.
     *
     * @param s1 first type display string
     * @param s2 second type display string
     *
     * @return comparison result suitable for {@link Comparator}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int formationTypeComparator(String s1, String s2) {
        boolean isSalvage1 = s1.toLowerCase().contains(FormationType.SALVAGE.getDisplayName());
        boolean isSalvage2 = s2.toLowerCase().contains(FormationType.SALVAGE.getDisplayName());
        if (isSalvage1 && !isSalvage2) {return -1;}
        if (!isSalvage1 && isSalvage2) {return 1;}
        return new NaturalOrderComparator().compare(s1, s2);
    }

    /**
     * Loads the localized instruction string shown at the top of the dialog.
     *
     * @return localized instructions text
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getInstructions() {
        return getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.instructions");
    }

    /**
     * Adds Cancel (always) and Confirm (only if formations exist) buttons to the provided panel and wires up their actions
     * to close the dialog and set {@link #wasConfirmed}.
     *
     * @param buttonPanel panel to populate
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void getButtons(JPanel buttonPanel) {
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
        buttonPanel.add(btnConfirm);
    }

    /**
     * Table model that exposes {@link SalvageFormationData} properties to the UI and tracks which rows are selected. Also
     * formats tech labels (experience and injury highlighting).
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static class SalvageFormationTableModel extends AbstractTableModel {
        /** Column index for the selection checkbox. */
        private static final int COL_SELECT = 0;
        /** Column index for the formation name. */
        private static final int COL_FORMATION_NAME = 1;
        /** Column index for the formation type display. */
        private static final int COL_FORMATION_TYPE = 2;
        /** Column index for the assigned tech. */
        private static final int COL_TOE_TECH = 3;
        /** Column index for the techs as part of a vehicle crew. */
        private static final int COL_CREW_TECHS = 4;
        /** Column index for maximum cargo capacity. */
        private static final int COL_CARGO_CAPACITY = 5;
        /** Column index for maximum tow capacity. */
        private static final int COL_TOW_CAPACITY = 6;
        /** Column index for salvage-capable unit count. */
        private static final int COL_SALVAGE_UNITS = 7;
        /** Column index for tug availability. */
        private static final int COL_HAS_TUG = 8;

        private final Campaign campaign;
        private final List<SalvageFormationData> formations;
        private final boolean[] selected;

        private static final String[] COLUMN_NAMES = {
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.select"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.formation"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.type"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.tech"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.crewTechs"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.cargo"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.tow"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.picks"),
              getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.tug")
        };

        /**
         * Creates a new table model over the provided data.
         *
         * @param campaign            campaign context for skill/experience labels and tooltips
         * @param formations              row data; one entry per formation
         * @param priorSelectedFormations a list of formations that were previously selected
         *
         * @author Illiani
         * @since 0.50.10
         */
        public SalvageFormationTableModel(Campaign campaign, List<SalvageFormationData> formations,
              List<Integer> priorSelectedFormations) {
            this.campaign = campaign;
            this.formations = formations;
            this.selected = new boolean[formations.size()];

            // Pre-select rows where the formation ID is in priorSelectedFormations
            if (priorSelectedFormations != null && !priorSelectedFormations.isEmpty()) {
                for (int i = 0; i < formations.size(); i++) {
                    Formation f = formations.get(i).formation();
                    if (priorSelectedFormations.contains(f.getId())) {
                        selected[i] = true;
                    }
                }
            }
        }

        @Override
        public int getRowCount() {
            return formations.size();
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
                case COL_SELECT, COL_HAS_TUG -> Boolean.class;
                case COL_FORMATION_NAME, COL_FORMATION_TYPE, COL_TOE_TECH -> String.class;
                case COL_CARGO_CAPACITY, COL_TOW_CAPACITY -> Double.class;
                case COL_SALVAGE_UNITS, COL_CREW_TECHS -> Integer.class;
                default -> Object.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_SELECT;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SalvageFormationData data = formations.get(rowIndex);

            return switch (columnIndex) {
                case COL_SELECT -> selected[rowIndex];
                case COL_FORMATION_NAME -> data.formation().getName();
                case COL_FORMATION_TYPE -> data.formationType().getDisplayName();
                case COL_TOE_TECH -> getTechLabel(data.tech());
                case COL_CREW_TECHS -> getCrewTechCount(campaign.getHangar(), data.formation());
                case COL_CARGO_CAPACITY -> data.maximumCargoCapacity();
                case COL_TOW_CAPACITY -> data.maximumTowCapacity();
                case COL_SALVAGE_UNITS -> data.salvageCapableUnits();
                case COL_HAS_TUG -> data.hasTug();
                default -> null;
            };
        }

        /**
         * Builds a display label for the tech, including short skill level labels and full title. Injured techs are
         * wrapped in a negative-color span.
         *
         * @param tech the assigned tech; may be {@code null}
         *
         * @return a formatted label or {@code "-"} when no tech is assigned
         *
         * @author Illiani
         * @since 0.50.10
         */
        private String getTechLabel(Person tech) {
            if (tech == null) {
                return "-";
            }
            String name = tech.getFullTitle();
            boolean isTechSecondary = tech.getSecondaryRole().isTechSecondary();
            String skillLevel = tech.getSkillLevel(campaign, isTechSecondary, true).getShortName();
            boolean isInjured = tech.needsFixing();

            String label = "[" + skillLevel + "] " + name;
            if (isInjured) {
                return "<html>" + messageSurroundedBySpanWithColor(getNegativeColor(), label) + "</html>";
            } else {
                return label;
            }
        }

        private int getCrewTechCount(Hangar hangar, Formation formation) {
            int counter = 0;
            for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
                for (Person crew : unit.getCrew()) {
                    if (crew.isTechExpanded() && !crew.isEngineer()) {
                        counter++;
                    }
                }
            }

            return counter;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == COL_SELECT) {
                selected[rowIndex] = (Boolean) value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        /**
         * Returns the {@link Formation} instances corresponding to rows whose Select checkbox is enabled.
         *
         * @return list of selected formations (never {@code null})
         *
         * @author Illiani
         * @since 0.50.10
         */
        public List<Formation> getSelectedFormations() {
            List<Formation> selectedFormations = new ArrayList<>();
            for (int i = 0; i < formations.size(); i++) {
                if (selected[i]) {
                    selectedFormations.add(formations.get(i).formation());
                }
            }
            return selectedFormations;
        }
    }

    /**
     * Renderer for the salvage formations table. Renders booleans as centered checkboxes (not editable) and applies
     * context-aware tooltips:
     *
     * <ul>
     *   <li>Formation name: full formation name</li>
     *   <li>Tech: experience/rank and injury status</li>
     *   <li>Cargo/Tow: capacity explanation against hangar state</li>
     *   <li>Picks: localized description of what the number represents</li>
     *   <li>Tug: hover shows tug source/logic if available</li>
     * </ul>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static class SalvageFormationTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private final Campaign campaign;
        private final JCheckBox checkBox = new JCheckBox();

        /**
         * Creates a renderer bound to the provided campaign for tooltip/context data.
         *
         * @param campaign campaign context
         *
         * @since 0.50.10
         */
        public SalvageFormationTableCellRenderer(Campaign campaign) {
            this.campaign = campaign;
            checkBox.setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus,
              int row, int column) {

            SalvageFormationTableModel model = (SalvageFormationTableModel) table.getModel();
            int modelRow = table.convertRowIndexToModel(row);
            int modelColumn = table.convertColumnIndexToModel(column);
            SalvageFormationData data = model.formations.get(modelRow);

            // Handle Boolean columns with checkbox
            if (modelColumn == SalvageFormationTableModel.COL_HAS_TUG) {
                checkBox.setSelected(value != null && (Boolean) value);
                checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

                String tugTooltip = data.getTugTooltip(campaign.getHangar());
                checkBox.setToolTipText(!tugTooltip.isBlank() ? wordWrap(tugTooltip) : null);
                return checkBox;
            }

            // Handle other columns with text
            Component component = super.getTableCellRendererComponent(table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column);

            if (component instanceof JComponent jComponent) {
                String tooltip = switch (modelColumn) {
                    case SalvageFormationTableModel.COL_FORMATION_NAME -> wordWrap(data.formation().getFullName());
                    case SalvageFormationTableModel.COL_TOE_TECH -> wordWrap(data.getTechTooltip(campaign, data.tech()));
                    case SalvageFormationTableModel.COL_CREW_TECHS ->
                          wordWrap(data.getAllCrewTechTooltip(campaign, data.formation()));
                    case SalvageFormationTableModel.COL_CARGO_CAPACITY ->
                          wordWrap(data.getCargoCapacityTooltip(campaign.getHangar()));
                    case SalvageFormationTableModel.COL_TOW_CAPACITY ->
                          wordWrap(data.getTowCapacityTooltip(campaign.getHangar()));
                    case SalvageFormationTableModel.COL_SALVAGE_UNITS ->
                          wordWrap(getTextAt(RESOURCE_BUNDLE, "SalvageFormationPicker.column.picks.tooltip"));
                    default -> null;
                };

                jComponent.setToolTipText(tooltip);
            }

            return component;
        }
    }

    /**
     * This override formations the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SalvageFormationPicker.class);
            setName("SalvageFormationPicker");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
