/*
 * Copyright (C) 2009-2018 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Ralgith
 */
public class EditPersonnelInjuriesDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditPersonnelInjuriesDialog.class);

    private JFrame frame;
    private Campaign campaign;
    private Person person;
    private InjuryTableModel injuryModel;

    private JButton btnEdit;
    private JButton btnDelete;
    private JTable injuriesTable;

    public EditPersonnelInjuriesDialog(final JFrame frame, final boolean modal,
            final Campaign campaign, final Person person) {
        super(frame, modal);
        this.frame = frame;
        this.campaign = campaign;
        this.person = person;
        injuryModel = new InjuryTableModel(person.getInjuries());
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        JButton btnOK = new JButton();
        JButton btnAdd = new JButton();
        btnEdit = new JButton();
        btnDelete = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelInjuriesDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title") + " " + person.getFullName());
        getContentPane().setLayout(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));
        btnAdd.setText(resourceMap.getString("btnAdd.text"));
        btnAdd.setName("btnAdd");
        btnAdd.addActionListener(evt -> addEntry());
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEdit.text"));
        btnEdit.setName("btnEdit");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editEntry());
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnDelete.text"));
        btnDelete.setName("btnDelete");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteEntry());
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);

        injuriesTable = new JTable(injuryModel);
        injuriesTable.setName("injuriesTable");
        TableColumn column;
        int width = 0;
        for (int i = 0; i < InjuryTableModel.N_COL; i++) {
            column = injuriesTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(injuryModel.getColumnWidth(i));
            column.setCellRenderer(injuryModel.getRenderer());
            width += injuryModel.getColumnWidth(i);
        }
        setPreferredSize(new Dimension(width, 500));
        injuriesTable.setIntercellSpacing(new Dimension(0, 0));
        injuriesTable.setShowGrid(false);
        injuriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        injuriesTable.getSelectionModel().addListSelectionListener(this::injuriesTableValueChanged);

        JScrollPane scrollInjuryTable = new JScrollPaneWithSpeed();
        scrollInjuryTable.setName("scrollInjuryTable");
        scrollInjuryTable.setViewportView(injuriesTable);
        getContentPane().add(scrollInjuryTable, BorderLayout.CENTER);

        btnOK.setText(resourceMap.getString("btnOK.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(EditPersonnelInjuriesDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    private void injuriesTableValueChanged(ListSelectionEvent evt) {
        int row = injuriesTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addEntry() {
        EditInjuryEntryDialog eied = new EditInjuryEntryDialog(frame, true, new Injury(campaign.getLocalDate()));
        eied.setAlwaysOnTop(true);
        eied.setVisible(true);
        if (null != eied.getEntry()) {
            person.addInjury(eied.getEntry());
        }
        refreshTable();
    }

    private void editEntry() {
        Injury entry = injuryModel.getEntryAt(injuriesTable.getSelectedRow());
        if (null != entry) {
            EditInjuryEntryDialog eied = new EditInjuryEntryDialog(frame, true, entry);
            eied.setAlwaysOnTop(true);
            eied.setVisible(true);
            refreshTable();
        }
    }

    private void deleteEntry() {
        Injury entry = injuryModel.getEntryAt(injuriesTable.getSelectedRow());
        person.removeInjury(entry);
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = injuriesTable.getSelectedRow();
        injuryModel.setData(person.getInjuries());
        if (selectedRow != -1) {
            if (injuriesTable.getRowCount() > 0) {
                if (injuriesTable.getRowCount() == selectedRow) {
                    injuriesTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                } else {
                    injuriesTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }

    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but
     * not exactly
     */
    public static class InjuryTableModel extends AbstractTableModel {
        protected String[] columnNames;
        protected List<Injury> data;

        public final static int COL_DAYS = 0;
        public final static int COL_LOCATION = 1;
        public final static int COL_TYPE = 2;
        public final static int COL_FLUFF = 3;
        public final static int COL_HITS = 4;
        public final static int COL_PERMANENT = 5;
        public final static int COL_WORKEDON = 6;
        public final static int COL_EXTENDED = 7;
        public final static int N_COL = 8;

        public InjuryTableModel(List<Injury> entries) {
            data = entries;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_DAYS:
                    return "Days Remaining";
                case COL_LOCATION:
                    return "Location on Body";
                case COL_TYPE:
                    return "Type of Injury";
                case COL_FLUFF:
                    return "Fluff Message";
                case COL_HITS:
                    return "Number of Hits";
                case COL_PERMANENT:
                    return "Is Permanent";
                case COL_WORKEDON:
                    return "Doctor Has Worked On";
                case COL_EXTENDED:
                    return "Was Extended Time";
                default:
                    return "?";
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            Injury entry;
            if (data.isEmpty()) {
                return "";
            } else {
                entry = data.get(row);
            }

            switch (col) {
                case COL_DAYS:
                    return Integer.toString(entry.getTime());
                case COL_LOCATION:
                    return entry.getLocationName();
                case COL_TYPE:
                    return entry.getType().getName(entry.getLocation(), entry.getHits());
                case COL_FLUFF:
                    return entry.getFluff();
                case COL_HITS:
                    return Integer.toString(entry.getHits());
                case COL_PERMANENT:
                    return Boolean.toString(entry.isPermanent());
                case COL_WORKEDON:
                    return Boolean.toString(entry.isWorkedOn());
                case COL_EXTENDED:
                    return Boolean.toString(entry.getExtended());
                default:
                    return "?";
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Injury getEntryAt(int row) {
            return data.get(row);
        }

        public int getColumnWidth(int c) {
            switch (c) {
                case COL_DAYS:
                case COL_HITS:
                case COL_PERMANENT:
                case COL_WORKEDON:
                case COL_EXTENDED:
                    return 110;
                case COL_TYPE:
                    return 150;
                case COL_FLUFF:
                case COL_LOCATION:
                    return 200;
                default:
                    return 50;
            }
        }

        public int getAlignment(int col) {
            switch (col) {
                case COL_DAYS:
                case COL_HITS:
                case COL_PERMANENT:
                case COL_WORKEDON:
                case COL_EXTENDED:
                    return SwingConstants.CENTER;
                default:
                    return SwingConstants.LEFT;
            }
        }

        public String getTooltip(int row, int col) {
            return null;
        }

        // fill table with values
        public void setData(List<Injury> entries) {
            data = entries;
            fireTableDataChanged();
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                return this;
            }
        }
    }
}
