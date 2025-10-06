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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedText;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;

import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AddOrEditLogEntryDialog;
import mekhq.gui.model.LogTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * A control panel for editing a person's log entries.
 *
 * <p>This component provides a table view of all log entries for a person, along with buttons to add, edit,
 * and delete entries. It manages the underlying data model and handles all user interactions related to log
 * management.</p>
 *
 * @author Taharqa
 */
public class EditLogControl extends JPanel {
    private static final int PADDING = scaleForGUI(5);

    private final JFrame parent;
    private final LocalDate today;
    private final Person person;
    private final LogType logType;
    private final LogTableModel logModel;

    private JButton btnEdit;
    private JButton btnDelete;
    private JTable logsTable;

    /**
     * Represents the different types of logs that can be maintained for a person.
     *
     * <p>Each log type serves a different purpose in tracking aspects of a person's history
     * and status within the organization.</p>
     *
     * @author Illiani
     * @since 0.50.05
     */
    public enum LogType {
        /**
         * Personal logs record general events and notes related to the person. These may include personal achievements,
         * disciplinary actions, or other noteworthy events not covered by other log types.
         */
        PERSONAL_LOG,

        /**
         * Medical logs record health-related events, injuries, and other medical information pertaining to the person.
         */
        MEDICAL_LOG,

        /**
         * Medical logs record health-related treatments performed by the character.
         */
        PATIENT_LOG,

        /**
         * Assignment logs track a person's positions, deployments, transfers, and other assignment-related information
         * throughout their service.
         */
        ASSIGNMENT_LOG,

        /**
         * Performance logs are used to track and record a character's skill improvements, and SPA or XP gains.
         */
        PERFORMANCE_LOG
    }

    /**
     * Constructs a new control panel for editing a person's log.
     *
     * @param parent the parent frame for dialogs
     * @param person the person whose log is being edited
     * @param today  the current date for new entries
     */
    public EditLogControl(JFrame parent, Person person, LocalDate today, LogType logType) {
        this.parent = parent;
        this.person = person;
        this.today = today;
        this.logType = logType;

        this.logModel = switch (logType) {
            case PERSONAL_LOG -> new LogTableModel(person.getPersonalLog());
            case MEDICAL_LOG -> new LogTableModel(person.getMedicalLog());
            case PATIENT_LOG -> new LogTableModel(person.getPatientLog());
            case ASSIGNMENT_LOG -> new LogTableModel(person.getAssignmentLog());
            case PERFORMANCE_LOG -> new LogTableModel(person.getPerformanceLog());
        };

        initComponents();
    }

    /**
     * Initializes the UI components of the control panel.
     *
     * <p>Sets up the layout, creates the action buttons (add, edit, delete), configures the table for displaying log
     * entries, and sets up the scroll pane containing the table.</p>
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
        JButton button = new JButton(getFormattedText(textKey));
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

        JScrollPane scrollPane = new JScrollPaneWithSpeed();
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
     * Handles selection changes in the logs table.
     *
     * <p>Enables or disables the edit and delete buttons based on whether a row is selected in the table.</p>
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
     * Opens a dialog to add a new log entry.
     *
     * <p>If the user confirms the addition, the new entry is added to the person's log and the table is
     * refreshed.</p>
     */
    private void addEntry() {
        final AddOrEditLogEntryDialog dialog = new AddOrEditLogEntryDialog(parent, person, today);
        if (dialog.showDialog().isConfirmed()) {
            switch (logType) {
                case PERSONAL_LOG -> person.addPersonalLogEntry(dialog.getEntry());
                case MEDICAL_LOG -> person.addMedicalLogEntry(dialog.getEntry());
                case ASSIGNMENT_LOG -> person.addAssignmentLogEntry(dialog.getEntry());
                case PERFORMANCE_LOG -> person.addPerformanceLogEntry(dialog.getEntry());
            }

            refreshTable();
        }
    }

    /**
     * Opens a dialog to edit the selected log entry.
     *
     * <p>Retrieves the selected entry from the model, opens an edit dialog, and refreshes the table afterward.</p>
     */
    private void editEntry() {
        int selectedRow = logsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        final LogEntry entry = logModel.getEntry(selectedRow);
        if (entry != null) {
            new AddOrEditLogEntryDialog(parent, person, entry).showDialog();
            refreshTable();
        }
    }

    /**
     * Deletes the selected log entry.
     *
     * <p>Removes the entry from the person's log and refreshes the table.</p>
     */
    private void deleteEntry() {
        int selectedRow = logsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        switch (logType) {
            case PERSONAL_LOG -> person.getPersonalLog().remove(selectedRow);
            case MEDICAL_LOG -> person.getMedicalLog().remove(selectedRow);
            case PATIENT_LOG -> person.getPatientLog().remove(selectedRow);
            case ASSIGNMENT_LOG -> person.getAssignmentLog().remove(selectedRow);
            case PERFORMANCE_LOG -> person.getPerformanceLog().remove(selectedRow);
        }

        refreshTable();
    }

    /**
     * Refreshes the table to reflect the current state of the log.
     *
     * <p>Updates the model with fresh data, then attempts to maintain the user's selection if possible. If the
     * previously selected row no longer exists (e.g., after deletion), it selects the previous row.</p>
     */
    private void refreshTable() {
        int selectedRow = logsTable.getSelectedRow();

        switch (logType) {
            case PERSONAL_LOG -> person.getPersonalLog();
            case MEDICAL_LOG -> person.getMedicalLog();
            case PATIENT_LOG -> person.getPatientLog();
            case ASSIGNMENT_LOG -> person.getAssignmentLog();
            case PERFORMANCE_LOG -> person.getPerformanceLog();
        }

        if (selectedRow != -1 && logsTable.getRowCount() > 0) {
            // Adjust selection if the previously selected row is no longer available
            if (logsTable.getRowCount() <= selectedRow) {
                selectedRow = logsTable.getRowCount() - 1;
            }

            logsTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }
}
