/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.control;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AddOrEditScenarioEntryDialog;
import mekhq.gui.model.LogTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

public class EditScenarioLogControl extends JPanel {
    private JFrame parent;
    private Campaign campaign;
    private Person person;
    private LogTableModel logModel;

    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JTable logsTable;
    private JScrollPane scrollLogsTable;

    public EditScenarioLogControl(JFrame parent, Campaign campaign, Person person) {
        this.parent = parent;
        this.campaign = campaign;
        this.person = person;

        this.logModel = new LogTableModel(person.getScenarioLog());

        initComponents();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditScenarioLogControl",
              MekHQ.getMHQOptions().getLocale());

        setName(resourceMap.getString("control.name"));
        this.setLayout(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));

        btnAdd = new JButton();
        btnAdd.setText(resourceMap.getString("btnAdd.text"));
        btnAdd.setName("btnAdd");
        btnAdd.addActionListener(evt -> addEntry());
        panBtns.add(btnAdd);

        btnEdit = new JButton();
        btnEdit.setText(resourceMap.getString("btnEdit.text"));
        btnEdit.setName("btnEdit");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editEntry());
        panBtns.add(btnEdit);

        btnDelete = new JButton();
        btnDelete.setText(resourceMap.getString("btnDelete.text"));
        btnDelete.setName("btnDelete");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteEntry());
        panBtns.add(btnDelete);
        this.add(panBtns, BorderLayout.PAGE_START);

        logsTable = new JTable(logModel);
        logsTable.setName(resourceMap.getString("logsTable.name"));
        TableColumn column;
        for (int i = 0; i < LogTableModel.N_COL; i++) {
            column = logsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(logModel.getColumnWidth(i));
            column.setCellRenderer(logModel.getRenderer());
        }
        logsTable.setIntercellSpacing(new Dimension(0, 0));
        logsTable.setShowGrid(false);
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logsTable.getSelectionModel().addListSelectionListener(this::logTableValueChanged);

        scrollLogsTable = new JScrollPaneWithSpeed();
        scrollLogsTable.setName(resourceMap.getString("scrollLogsTable.name"));
        scrollLogsTable.setViewportView(logsTable);
        this.add(scrollLogsTable, BorderLayout.CENTER);
    }

    private void logTableValueChanged(ListSelectionEvent evt) {
        int row = logsTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addEntry() {
        AddOrEditScenarioEntryDialog dialog = new AddOrEditScenarioEntryDialog(parent, true, campaign.getLocalDate());
        dialog.setVisible(true);
        if (dialog.getEntry().isPresent()) {
            person.addScenarioLogEntry(dialog.getEntry().get());
        }
        refreshTable();
    }

    private void editEntry() {
        LogEntry entry = logModel.getEntry(logsTable.getSelectedRow());
        if (null != entry) {
            AddOrEditScenarioEntryDialog dialog = new AddOrEditScenarioEntryDialog(parent, true, entry);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteEntry() {
        person.getScenarioLog().remove(logsTable.getSelectedRow());
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = logsTable.getSelectedRow();
        logModel.setData(person.getScenarioLog());
        if (selectedRow != -1) {
            if (logsTable.getRowCount() > 0) {
                if (logsTable.getRowCount() == selectedRow) {
                    logsTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                } else {
                    logsTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }
}
