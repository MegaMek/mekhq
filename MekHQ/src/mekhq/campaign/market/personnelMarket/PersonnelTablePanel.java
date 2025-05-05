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
package mekhq.campaign.market.personnelMarket;

import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.Collator;
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

public class PersonnelTablePanel extends JPanel {
    private List<Person> selectedApplicants = new ArrayList<>();
    private int rowCount;
    private JTable table;

    public PersonnelTablePanel(Campaign campaign, List<Person> people) {
        setLayout(new BorderLayout());
        PersonTableModel model = new PersonTableModel(people, campaign);
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

    public JTable getTable() {
        return table;
    }

    public List<Person> getSelectedApplicants() {
        return selectedApplicants;
    }

    public int getRowCount() {
        return rowCount;
    }

    private static void assignSorters(PersonTableModel model, JTable table) {
        TableRowSorter<PersonTableModel> sorter = new TableRowSorter<>(model);
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);

        sorter.setComparator(0, collator);
        sorter.setComparator(1, collator);
        sorter.setComparator(2, collator);
        sorter.setComparator(4, collator);
        sorter.setComparator(3, Comparator.comparingInt(o -> (Integer) o));
        table.setRowSorter(sorter);
    }

    private static void dynamicallyCalculateColumnWidth(JTable table, int i, JTableHeader header) {
        TableColumn column = table.getColumnModel().getColumn(i);
        TableCellRenderer headerRenderer = header.getDefaultRenderer();
        Component headerComp = headerRenderer.getTableCellRendererComponent(table,
              column.getHeaderValue(),
              false,
              false,
              -1,
              i);
        int maxWidth = headerComp.getPreferredSize().width;
        if (i == 0 || i == 1) {
            TableCellRenderer cellRenderer = table.getDefaultRenderer(String.class);
            for (int row = 0; row < table.getRowCount(); row++) {
                Object value = table.getValueAt(row, i);
                Component comp = cellRenderer.getTableCellRendererComponent(table, value, false, false, row, i);
                int cellWidth = comp.getPreferredSize().width;
                if (cellWidth > maxWidth) {
                    maxWidth = cellWidth;
                }
            }
        }
        int preferredWidth = maxWidth + 16;
        column.setPreferredWidth(preferredWidth);
    }

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

    public void addListSelectionListener(ListSelectionListener listener) {
        table.getSelectionModel().addListSelectionListener(listener);
    }
}
