/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.control;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AddOrEditMissionEntryDialog;
import mekhq.gui.model.LogTableModel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ResourceBundle;

public class EditMissionLogControl extends JPanel {
    private Frame parent;
    private Campaign campaign;
    private Person person;
    private LogTableModel logModel;

    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JTable logsTable;
    private JScrollPane scrollLogsTable;

    public EditMissionLogControl(Frame parent, Campaign campaign, Person person) {
        this.parent = parent;
        this.campaign = campaign;
        this.person = person;

        this.logModel = new LogTableModel(this.person.getMissionLog());

        initComponents();
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditMissionLogControl", new EncodeControl()); //$NON-NLS-1$

        setName(resourceMap.getString("control.name")); // NOI18N
        this.setLayout(new java.awt.BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));

        btnAdd = new JButton();
        btnAdd.setText(resourceMap.getString("btnAdd.text")); // NOI18N
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(evt -> addEntry());
        panBtns.add(btnAdd);

        btnEdit = new JButton();
        btnEdit.setText(resourceMap.getString("btnEdit.text")); // NOI18N
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editEntry());
        panBtns.add(btnEdit);

        btnDelete = new JButton();
        btnDelete.setText(resourceMap.getString("btnDelete.text")); // NOI18N
        btnDelete.setName("btnDelete"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteEntry());
        panBtns.add(btnDelete);
        this.add(panBtns, BorderLayout.PAGE_START);

        logsTable = new JTable(logModel);
        logsTable.setName(resourceMap.getString("logsTable.name")); // NOI18N
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

        scrollLogsTable = new JScrollPane();
        scrollLogsTable.setName(resourceMap.getString("scrollLogsTable.name")); // NOI18N
        scrollLogsTable.setViewportView(logsTable);
        this.add(scrollLogsTable, BorderLayout.CENTER);
    }

    private void logTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = logsTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addEntry() {
        AddOrEditMissionEntryDialog dialog = new AddOrEditMissionEntryDialog(parent, true, campaign.getDate());
        dialog.setVisible(true);
        if (dialog.getEntry().isPresent()) {
            person.addMissionLogEntry(dialog.getEntry().get());
        }
        refreshTable();
    }

    private void editEntry() {
        LogEntry entry = logModel.getEntry(logsTable.getSelectedRow());
        if (null != entry) {
            AddOrEditMissionEntryDialog dialog = new AddOrEditMissionEntryDialog(parent, true, entry);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteEntry() {
        person.getMissionLog().remove(logsTable.getSelectedRow());
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = logsTable.getSelectedRow();
        logModel.setData(person.getMissionLog());
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
