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

package mekhq.gui.dialog;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.MissionLogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.model.LogTableModel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ResourceBundle;

public class EditMissionsLogDialog extends JDialog {
    private Frame frame;
    private Campaign campaign;
    private Person person;
    private LogTableModel logModel;

    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnOK;
    private JTable logTable;
    private JScrollPane scrollLogTable;

    /**
     * Creates new form EditPersonnelLogDialog
     */
    public EditMissionsLogDialog(Frame parent, boolean modal, Campaign campaign, Person person) {
        super(parent, modal);
        this.frame = parent;
        this.campaign = campaign;
        this.person = person;

        this.logModel = new LogTableModel(this.person.getMissionsLog());

        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditMissionsLogDialog", new EncodeControl()); //$NON-NLS-1$

        btnOK = new JButton();
        btnAdd = new JButton();
        btnEdit = new JButton();
        btnDelete = new JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("dialog.title") + " " + person.getName());
        getContentPane().setLayout(new java.awt.BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));
        btnAdd.setText(resourceMap.getString("btnAdd.text")); // NOI18N
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(evt -> addEntry());
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEdit.text")); // NOI18N
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editEntry());
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnDelete.text")); // NOI18N
        btnDelete.setName("btnDelete"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteEntry());
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);

        logTable = new JTable(logModel);
        logTable.setName("logTable"); // NOI18N
        TableColumn column;
        for (int i = 0; i < LogTableModel.N_COL; i++) {
            column = logTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(logModel.getColumnWidth(i));
            column.setCellRenderer(logModel.getRenderer());
        }
        logTable.setIntercellSpacing(new Dimension(0, 0));
        logTable.setShowGrid(false);
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logTable.getSelectionModel().addListSelectionListener(this::logTableValueChanged);
        scrollLogTable = new JScrollPane();
        scrollLogTable.setName("scrollPartsTable"); // NOI18N
        scrollLogTable.setViewportView(logTable);
        getContentPane().add(scrollLogTable, BorderLayout.CENTER);

        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(x -> this.setVisible(false));
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    private void logTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = logTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addEntry() {
        EditMissionEntryDialog emld = new EditMissionEntryDialog(frame, true, person, campaign.getDate());
        emld.setVisible(true);
        if (emld.getEntry().isPresent()) {
            person.addMissionLogEntry(emld.getEntry().get());
        }
        refreshTable();
    }

    private void editEntry() {
        LogEntry entry = logModel.getEntry(logTable.getSelectedRow());
        if (null != entry && entry instanceof MissionLogEntry) {
            EditMissionEntryDialog emld = new EditMissionEntryDialog(frame, true, person, (MissionLogEntry)entry);
            emld.setVisible(true);
            refreshTable();
        }
    }

    private void deleteEntry() {
        person.getMissionsLog().remove(logTable.getSelectedRow());
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = logTable.getSelectedRow();
        logModel.setData(person.getMissionsLog());
        if (selectedRow != -1) {
            if (logTable.getRowCount() > 0) {
                if (logTable.getRowCount() == selectedRow) {
                    logTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                } else {
                    logTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }
}
