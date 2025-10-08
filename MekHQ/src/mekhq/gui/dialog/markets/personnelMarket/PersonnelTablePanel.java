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
package mekhq.gui.dialog.markets.personnelMarket;

import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 * A panel displaying a table of personnel applicants within the personnel market dialog.
 *
 * <p>Supports multi-selection, sorting, and dynamic column width adjustment. Integrates with a campaign context and
 * provides convenient methods for selection listeners and applicant retrieval.</p>
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Initializes a JTable to display a list of {@link Person} objects with custom model and sorting.</li>
 *     <li>Dynamically calculates appropriate column widths.</li>
 *     <li>Supports multiple row selection and corresponding applicant retrieval.</li>
 *     <li>Provides methods to add selection listeners and access selected rows.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ul>
 *     <li>Instantiate with a {@link Campaign} and a {@link List} of {@link Person} objects.</li>
 *     <li>Attach selection listeners via {@link #addListSelectionListener(ListSelectionListener)}.</li>
 *     <li>Retrieve selected applicants or the underlying table as needed.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelTablePanel extends JPanel {
    private final List<Person> selectedApplicants = new ArrayList<>();
    private final int rowCount;
    private final JTable table;

    /**
     * Constructs a {@code PersonnelTablePanel} to list applicants.
     *
     * @param campaign the ongoing campaign context for data display
     * @param people   the list of personnel to show in the table
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelTablePanel(Campaign campaign, List<Person> people) {
        setLayout(new BorderLayout());
        PersonTableModel model = new PersonTableModel(campaign, people);
        table = new JTable(model);
        rowCount = people.size();

        if (rowCount > 0) {
            selectedApplicants.add(people.get(0));
        }

        // Sorters
        assignSorters(model, table);

        // Width
        JTableHeader header = table.getTableHeader();
        for (int i = 0; i < table.getColumnCount(); i++) {
            dynamicallyCalculateColumnWidth(table, i, header);
        }

        // Selection
        table.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
        ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.addListSelectionListener(e -> updateSelectedApplicants(people, table));

        // Return
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Returns the underlying {@link JTable} displaying the applicants.
     *
     * @return the {@link JTable} instance in this panel
     *
     * @author Illiani
     * @since 0.50.06
     */
    public JTable getTable() {
        return table;
    }

    /**
     * @return a list of selected {@link Person} (the applicants)
     *
     * @author Illiani
     * @since 0.50.06
     */
    public List<Person> getSelectedApplicants() {
        return selectedApplicants;
    }

    /**
     * @return the row count for this table
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Adds a selection listener to the table's selection model.
     *
     * @param listener the {@link ListSelectionListener} to add
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Configures column sorting for the provided model and table.
     *
     * @param model the table model to use
     * @param table the {@link JTable} to configure
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static void assignSorters(PersonTableModel model, JTable table) {
        TableRowSorter<PersonTableModel> sorter = new TableRowSorter<>(model);

        // Use getComparator from the model for each column
        for (int i = 0; i < model.getColumnCount(); i++) {
            Comparator<?> comparator = model.getComparator(i);
            if (comparator != null) {
                sorter.setComparator(i, comparator);
            }
        }
        table.setRowSorter(sorter);
    }

    /**
     * Dynamically determines and sets the preferred column width based on contents.
     *
     * @param table       the {@link JTable} to adjust
     * @param columnIndex the column index
     * @param header      the table header
     *
     * @author Illiani
     * @since 0.50.06
     */
    private static void dynamicallyCalculateColumnWidth(JTable table, int columnIndex, JTableHeader header) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        Component headerComp = headerRenderer.getTableCellRendererComponent(table,
              column.getHeaderValue(),
              false,
              false, -1, columnIndex);
        int maxWidth = headerComp.getPreferredSize().width;
        if (columnIndex == 0 || columnIndex == 1) {
            TableCellRenderer cellRenderer = table.getDefaultRenderer(String.class);
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, columnIndex);
                Component comp = cellRenderer.getTableCellRendererComponent(table,
                      value,
                      false,
                      false,
                      row,
                      columnIndex);
                int cellWidth = comp.getPreferredSize().width;
                if (cellWidth > maxWidth) {
                    maxWidth = cellWidth;
                }
            }
        }
        int preferredWidth = maxWidth + 16;
        column.setPreferredWidth(preferredWidth);
    }

    /**
     * Updates the list of selected applicants based on table selection.
     *
     * @param people the complete list of applicants
     * @param table  the {@link JTable} instance to read selection from
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void updateSelectedApplicants(List<Person> people, JTable table) {
        selectedApplicants.clear();
        int[] selectedRows = table.getSelectedRows();
        for (int viewRow : selectedRows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < people.size()) {
                selectedApplicants.add(people.get(modelRow));
            }
        }
    }
}
