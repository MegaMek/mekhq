/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.UnitLogEntry;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.AddOrEditLogEntryDialog;
import mekhq.gui.model.LogTableModel;

/**
 * A control panel for editing a unit's log entries.
 *
 * <p>This component provides a table view of all log entries for a unit, along with buttons to add, edit,
 * and delete entries. It manages the underlying data model and handles all user interactions related to log management.
 * It is the unit-oriented counterpart to {@link EditLogControl}.</p>
 */
public class EditUnitLogControl extends JPanel {
    private static final int PADDING = scaleForGUI(5);

    /**
     * The different logs a unit maintains, each of which can be edited independently.
     */
    public enum UnitLogType {
        /** General ownership events: salvage, purchase, and refits. */
        UNIT_LOG,
        /** Kills scored by the unit through its pilot. */
        KILL_LOG,
        /** Crew members assigned to the unit. */
        CREW_LOG,
        /** Deployments to scenarios. */
        DEPLOYMENT_LOG,
        /** Repairs conducted on the unit. */
        REPAIR_LOG
    }

    private final JFrame parent;
    private final LocalDate today;
    private final Unit unit;
    private final UnitLogType logType;
    private final LogTableModel logModel;

    private JButton btnEdit;
    private JButton btnDelete;
    private JTable logsTable;

    /**
     * Constructs a new control panel for editing a unit's log.
     *
     * @param parent  the parent frame for dialogs
     * @param unit    the unit whose log is being edited
     * @param today   the current date for new entries
     * @param logType which of the unit's logs to edit
     */
    public EditUnitLogControl(JFrame parent, Unit unit, LocalDate today, UnitLogType logType) {
        this.parent = parent;
        this.unit = unit;
        this.today = today;
        this.logType = logType;
        this.logModel = new LogTableModel(getLog());

        initComponents();
    }

    /**
     * @return the log list corresponding to this control's {@link UnitLogType}
     */
    private List<LogEntry> getLog() {
        return switch (logType) {
            case UNIT_LOG -> unit.getUnitLog();
            case KILL_LOG -> unit.getKillLog();
            case CREW_LOG -> unit.getCrewLog();
            case DEPLOYMENT_LOG -> unit.getDeploymentLog();
            case REPAIR_LOG -> unit.getRepairLog();
        };
    }

    /**
     * Adds the given entry to the log corresponding to this control's {@link UnitLogType}.
     *
     * @param entry the entry to add
     */
    private void addToLog(LogEntry entry) {
        switch (logType) {
            case UNIT_LOG -> unit.addUnitLogEntry(entry);
            case KILL_LOG -> unit.addKillLogEntry(entry);
            case CREW_LOG -> unit.addCrewLogEntry(entry);
            case DEPLOYMENT_LOG -> unit.addDeploymentLogEntry(entry);
            case REPAIR_LOG -> unit.addRepairLogEntry(entry);
        }
    }

    /**
     * Initializes the UI components of the control panel.
     */
    private void initComponents() {
        setName("control.name");
        setLayout(new BorderLayout(PADDING, PADDING));
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        initButtonPanel();
        initLogsTable();
    }

    /**
     * Initializes the button panel with action buttons.
     */
    private void initButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 0, PADDING, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));

        JButton btnAdd = createButton("logController.btnAdd.text", "btnAdd", true, this::addEntry);
        btnEdit = createButton("logController.btnEdit.text", "btnEdit", false, this::editEntry);
        btnDelete = createButton("logController.btnDelete.text", "btnDelete", false, this::deleteEntry);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);

        add(buttonPanel, BorderLayout.PAGE_START);
    }

    /**
     * Creates a button with the specified properties.
     *
     * @param textKey resource key for button text
     * @param name    component name
     * @param enabled initial enabled state
     * @param action  action to perform when clicked
     *
     * @return configured button
     */
    private JButton createButton(String textKey, String name, boolean enabled, Runnable action) {
        JButton button = new JButton(getText(textKey));
        button.setName(name);
        button.setEnabled(enabled);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * Initializes the logs table with its scroll pane.
     */
    private void initLogsTable() {
        logsTable = new JTable(logModel);
        logsTable.setName("logsTable.name");
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logsTable.setIntercellSpacing(new Dimension(0, 0));
        logsTable.setShowGrid(false);
        logsTable.getSelectionModel().addListSelectionListener(this::logTableValueChanged);

        configureTableColumns();

        JScrollPane scrollPane = new FastJScrollPane();
        scrollPane.setName("scrollLogsTable.name");
        scrollPane.setViewportView(logsTable);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Configures all columns in the logs table with appropriate widths and renderers.
     */
    private void configureTableColumns() {
        for (int i = 0; i < LogTableModel.N_COL; i++) {
            TableColumn column = logsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(logModel.getColumnWidth(i));
            column.setCellRenderer(logModel.getRenderer());
        }
    }

    /**
     * Handles selection changes in the logs table, enabling or disabling the edit and delete buttons based on whether a
     * row is selected.
     *
     * @param evt the event that triggered this handler
     */
    private void logTableValueChanged(ListSelectionEvent evt) {
        if (evt.getValueIsAdjusting()) {
            return;
        }

        boolean hasSelection = logsTable.getSelectedRow() != -1;
        btnEdit.setEnabled(hasSelection);
        btnDelete.setEnabled(hasSelection);
    }

    /**
     * Opens a dialog to add a new log entry. If the user confirms the addition, the new entry is added to the unit's
     * log and the table is refreshed.
     */
    private void addEntry() {
        final AddOrEditLogEntryDialog dialog = new AddOrEditLogEntryDialog(parent, null, today);
        if (dialog.showDialog().isConfirmed()) {
            final LogEntry entered = dialog.getEntry();
            addToLog(new UnitLogEntry(entered.getDate(), entered.getDesc()));
            refreshTable();
        }
    }

    /**
     * Opens a dialog to edit the selected log entry, then refreshes the table.
     */
    private void editEntry() {
        int selectedRow = logsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        final LogEntry entry = logModel.getEntry(selectedRow);
        if (entry != null) {
            new AddOrEditLogEntryDialog(parent, null, entry).showDialog();
            refreshTable();
        }
    }

    /**
     * Deletes the selected log entry from the unit's log and refreshes the table.
     */
    private void deleteEntry() {
        int selectedRow = logsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        getLog().remove(selectedRow);
        refreshTable();
    }

    /**
     * Refreshes the table to reflect the current state of the unit's log, attempting to maintain the user's selection
     * where possible.
     */
    private void refreshTable() {
        int selectedRow = logsTable.getSelectedRow();

        logModel.setData(getLog());

        if (selectedRow != -1 && logsTable.getRowCount() > 0) {
            // Adjust selection if the previously selected row is no longer available
            if (logsTable.getRowCount() <= selectedRow) {
                selectedRow = logsTable.getRowCount() - 1;
            }

            logsTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }
}
