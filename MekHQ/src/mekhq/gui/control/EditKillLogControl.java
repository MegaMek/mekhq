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
import mekhq.campaign.Kill;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AddOrEditKillEntryDialog;
import mekhq.gui.model.KillTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

public class EditKillLogControl extends JPanel {
    private JFrame parent;
    private Campaign campaign;
    private Person person;
    private KillTableModel killModel;

    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JTable killTable;
    private JScrollPane scrollKillTable;

    public EditKillLogControl(JFrame parent, Campaign campaign, Person person) {
        this.parent = parent;
        this.campaign = campaign;
        this.person = person;

        this.killModel = new KillTableModel(campaign.getKillsFor(this.person.getId()));

        initComponents();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditKillLogControl",
              MekHQ.getMHQOptions().getLocale());

        setName(resourceMap.getString("control.name"));
        this.setLayout(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));

        btnAdd = new JButton();
        btnAdd.setText(resourceMap.getString("btnAdd.text"));
        btnAdd.setName("btnAdd");
        btnAdd.addActionListener(evt -> addKill());
        panBtns.add(btnAdd);

        btnEdit = new JButton();
        btnEdit.setText(resourceMap.getString("btnEdit.text"));
        btnEdit.setName("btnEdit");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editKill());
        panBtns.add(btnEdit);

        btnDelete = new JButton();
        btnDelete.setText(resourceMap.getString("btnDelete.text"));
        btnDelete.setName("btnDelete");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteKill());
        panBtns.add(btnDelete);
        this.add(panBtns, BorderLayout.PAGE_START);

        killTable = new JTable(killModel);
        killTable.setName("killTable");
        TableColumn column;
        for (int i = 0; i < KillTableModel.N_COL; i++) {
            column = killTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(killModel.getColumnWidth(i));
            column.setCellRenderer(killModel.getRenderer());
        }
        killTable.setIntercellSpacing(new Dimension(0, 0));
        killTable.setShowGrid(false);
        killTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        killTable.getSelectionModel().addListSelectionListener(this::killTableValueChanged);

        scrollKillTable = new JScrollPaneWithSpeed();
        scrollKillTable.setName("scrollPartsTable");
        scrollKillTable.setViewportView(killTable);
        this.add(scrollKillTable, BorderLayout.CENTER);
    }

    private void killTableValueChanged(ListSelectionEvent evt) {
        int row = killTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addKill() {
        AddOrEditKillEntryDialog dialog = new AddOrEditKillEntryDialog(parent, true,
              person.getId(), "", campaign.getLocalDate(), campaign);
        dialog.setVisible(true);
        if (dialog.getKill().isPresent()) {
            campaign.addKill(dialog.getKill().get());
        }
        refreshTable();
    }

    private void editKill() {
        Kill kill = killModel.getKillAt(killTable.getSelectedRow());
        if (null != kill) {
            AddOrEditKillEntryDialog dialog = new AddOrEditKillEntryDialog(parent, true, kill, campaign);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteKill() {
        Kill kill = killModel.getKillAt(killTable.getSelectedRow());
        campaign.removeKill(kill);
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = killTable.getSelectedRow();
        killModel.setData(campaign.getKillsFor(person.getId()));
        if (selectedRow != -1) {
            if (killTable.getRowCount() > 0) {
                if (killTable.getRowCount() == selectedRow) {
                    killTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                } else {
                    killTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }
}
