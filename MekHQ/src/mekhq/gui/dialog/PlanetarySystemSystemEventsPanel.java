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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.buttons.MMButton;
import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySystemEvent;
import mekhq.campaign.universe.SourceableValue;

/** Edits nullable campaign-level planetary system events for the planetary system editor. */
final class PlanetarySystemSystemEventsPanel extends JPanel {
    private static final int PADDING = UIUtil.scaleForGUI(8);
    private static final DateTimeFormatter EVENT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JDialog owner;
    private final ResourceBundle resources;
    private final Supplier<LocalDate> currentDateSupplier;
    private final Supplier<PlanetarySystem> selectedSystemSupplier;
    private final BooleanSupplier canEditSupplier;
    private final Runnable onChange;
    private final SystemEventTableModel systemEventTableModel;
    private final JTable tblSystemEvents;
    private final JButton btnAddSystemEvent;
    private final JButton btnRemoveSystemEvent;

    PlanetarySystemSystemEventsPanel(JDialog owner, ResourceBundle resources,
          Supplier<LocalDate> currentDateSupplier, Supplier<PlanetarySystem> selectedSystemSupplier,
          BooleanSupplier canEditSupplier, Runnable onChange) {
        super(new BorderLayout(0, PADDING));
        this.owner = owner;
        this.resources = resources;
        this.currentDateSupplier = currentDateSupplier;
        this.selectedSystemSupplier = selectedSystemSupplier;
        this.canEditSupplier = canEditSupplier;
        this.onChange = onChange;
        setName("pnlPlanetarySystemSystemEvents");

        JLabel warning = new JLabel(resources.getString("PlanetarySystemEditorDialog.systemEvents.warning"));
        warning.setForeground(java.awt.Color.RED);
        warning.setBorder(BorderFactory.createEmptyBorder(0, 0, PADDING, 0));
        add(warning, BorderLayout.NORTH);

        systemEventTableModel = new SystemEventTableModel();
        tblSystemEvents = new JTable(systemEventTableModel);
        tblSystemEvents.setName("tblPlanetarySystemSystemEvents");
        tblSystemEvents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSystemEvents.setRowHeight(UIUtil.scaleForGUI(22));
        configureSystemEventChargeEditors();
        tblSystemEvents.getSelectionModel().addListSelectionListener(evt -> updateButtonState());
        add(createTitledComponentPane("PlanetarySystemEditorDialog.systemEvents.events",
              new FastJScrollPane(tblSystemEvents)), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, PADDING, 0));
        btnAddSystemEvent = new MMButton("btnAddSystemEvent",
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add"),
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add.toolTipText"),
              evt -> addSystemEvent());
        btnRemoveSystemEvent = new MMButton("btnRemoveSystemEvent",
              resources.getString("PlanetarySystemEditorDialog.systemEvents.remove"),
              resources.getString("PlanetarySystemEditorDialog.systemEvents.remove.toolTipText"),
              evt -> removeSelectedSystemEvent());
        buttons.add(btnAddSystemEvent);
        buttons.add(btnRemoveSystemEvent);
        add(buttons, BorderLayout.SOUTH);

        updateButtonState();
    }

    void refresh(LocalDate selectEventDate) {
        systemEventTableModel.setSystem(selectedSystemSupplier.get());
        if (selectEventDate != null) {
            int row = systemEventTableModel.findEventRow(selectEventDate);
            if (row >= 0) {
                tblSystemEvents.setRowSelectionInterval(row, row);
                tblSystemEvents.scrollRectToVisible(tblSystemEvents.getCellRect(row, 0, true));
            }
        }
        updateButtonState();
    }

    void updateButtonState() {
        boolean canEdit = (selectedSystemSupplier.get() != null) && canEditSupplier.getAsBoolean();
        btnAddSystemEvent.setEnabled(canEdit);
        btnRemoveSystemEvent.setEnabled(canEdit && (getSelectedSystemEvent() != null));
        tblSystemEvents.setEnabled(canEdit);
    }

    private void configureSystemEventChargeEditors() {
        String[] chargeValues = systemEventChargeValues();
        tblSystemEvents.getColumnModel().getColumn(SystemEventTableModel.COL_NADIR).setCellEditor(
              new DefaultCellEditor(new JComboBox<>(chargeValues)));
        tblSystemEvents.getColumnModel().getColumn(SystemEventTableModel.COL_ZENITH).setCellEditor(
              new DefaultCellEditor(new JComboBox<>(chargeValues)));
    }

    private String[] systemEventChargeValues() {
        return new String[] {
              systemEventChargeInheritLabel(),
              systemEventChargeYesLabel(),
              systemEventChargeNoLabel()
        };
    }

    private String systemEventChargeInheritLabel() {
        return resources.getString("PlanetarySystemEditorDialog.systemEvents.charge.inherit");
    }

    private String systemEventChargeYesLabel() {
        return resources.getString("PlanetarySystemEditorDialog.systemEvents.charge.yes");
    }

    private String systemEventChargeNoLabel() {
        return resources.getString("PlanetarySystemEditorDialog.systemEvents.charge.no");
    }

    private Component createTitledComponentPane(String titleKey, Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString(titleKey)));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void addSystemEvent() {
        PlanetarySystem selectedSystem = selectedSystemSupplier.get();
        if ((selectedSystem == null) || !canEditSupplier.getAsBoolean()) {
            return;
        }

        JTextField txtNewDate = new JTextField(formatDate(currentDateSupplier.get()), 12);
        JPanel panel = new JPanel(new GridBagLayout());
        addEventField(panel, 0, 0, "PlanetarySystemEditorDialog.eventEditor.date",
              createDateFieldPane(txtNewDate, "btnPickNewSystemEventDate"));

        int choice = JOptionPane.showConfirmDialog(this, panel,
              resources.getString("PlanetarySystemEditorDialog.systemEvents.add.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            LocalDate eventDate = parseEventDate(txtNewDate.getText());
            for (PlanetarySystemEvent event : nullToEmptyList(selectedSystem.getEvents())) {
                if ((event != null) && eventDate.equals(event.date)) {
                    JOptionPane.showMessageDialog(this, MessageFormat.format(resources.getString(
                                "PlanetarySystemEditorDialog.systemEvents.add.duplicate"), formatDate(eventDate)),
                          resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            PlanetarySystemEvent event = new PlanetarySystemEvent();
            event.date = eventDate;
            selectedSystem.putEvent(event);
            notifyChange();
            refresh(eventDate);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                  resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedSystemEvent() {
        PlanetarySystem selectedSystem = selectedSystemSupplier.get();
        PlanetarySystemEvent selectedEvent = getSelectedSystemEvent();
        if ((selectedSystem == null) || (selectedEvent == null) || !canEditSupplier.getAsBoolean()) {
            return;
        }
        selectedSystem.removeEvent(selectedEvent.date);
        notifyChange();
        refresh(null);
    }

    private PlanetarySystemEvent getSelectedSystemEvent() {
        if (tblSystemEvents.getSelectedRow() < 0) {
            return null;
        }
        return systemEventTableModel.getEventAt(
              tblSystemEvents.convertRowIndexToModel(tblSystemEvents.getSelectedRow()));
    }

    private void notifyChange() {
        if (onChange != null) {
            onChange.run();
        }
    }

    private Component createDateFieldPane(JTextField textField, String buttonName) {
        JButton btnPickDate = new MMButton(buttonName,
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickDate"),
              resources.getString("PlanetarySystemEditorDialog.eventEditor.pickDate.toolTipText"),
              evt -> chooseDate(textField));
        Dimension textFieldSize = new Dimension(UIUtil.scaleForGUI(104), textField.getPreferredSize().height);
        Dimension buttonSize = new Dimension(UIUtil.scaleForGUI(32), textField.getPreferredSize().height);
        textField.setMinimumSize(textFieldSize);
        textField.setPreferredSize(textFieldSize);
        btnPickDate.setMargin(new Insets(0, 0, 0, 0));
        btnPickDate.setMinimumSize(buttonSize);
        btnPickDate.setPreferredSize(buttonSize);
        return createInlineFieldWithButton(textField, btnPickDate);
    }

    private void chooseDate(JTextField targetField) {
        DateChooser dateChooser = new DateChooser(owner, getEventDateOrCampaignDate(targetField.getText()));
        if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
            targetField.setText(formatDate(dateChooser.getDate()));
        }
    }

    private LocalDate getEventDateOrCampaignDate(String eventDateText) {
        try {
            return parseEventDate(eventDateText);
        } catch (IllegalArgumentException ex) {
            return currentDateSupplier.get();
        }
    }

    private Component createInlineFieldWithButton(JTextField textField, JButton button) {
        JPanel panel = new JPanel(new BorderLayout(PADDING / 2, 0));
        panel.add(textField, BorderLayout.CENTER);
        panel.add(button, BorderLayout.EAST);
        return panel;
    }

    private void addEventField(JPanel panel, int row, int column, String labelKey, Component component) {
        GridBagConstraints labelConstraints = createGridBagConstraints(row, column, 1);
        labelConstraints.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(resources.getString(labelKey)), labelConstraints);

        GridBagConstraints fieldConstraints = createGridBagConstraints(row, column + 1, 1);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        panel.add(component, fieldConstraints);
    }

    private GridBagConstraints createGridBagConstraints(int row, int column, int width) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = width;
        constraints.insets = new Insets(0, PADDING / 2, PADDING / 2, PADDING / 2);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private LocalDate parseEventDate(String eventDateText) {
        String text = blankToNull(eventDateText);
        if (text == null) {
            throw new IllegalArgumentException(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.dateRequired"));
        }
        try {
            return LocalDate.parse(text, EVENT_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(MessageFormat.format(resources.getString(
                  "PlanetarySystemEditorDialog.eventEditor.invalid.date"), text), ex);
        }
    }

    private static <T> List<T> nullToEmptyList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String blankToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmedText = text.trim();
        return trimmedText.isEmpty() ? null : trimmedText;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : EVENT_DATE_FORMATTER.format(date);
    }

    private final class SystemEventTableModel extends AbstractTableModel {
        static final int COL_DATE = 0;
        static final int COL_NADIR = 1;
        static final int COL_ZENITH = 2;
        private static final int COLUMN_COUNT = 3;

        private PlanetarySystem system;
        private List<PlanetarySystemEvent> events = new ArrayList<>();

        void setSystem(PlanetarySystem system) {
            this.system = system;
            reload();
        }

        private void reload() {
            events = new ArrayList<>();
            if (system != null) {
                List<PlanetarySystemEvent> source = system.getEvents();
                if (source != null) {
                    for (PlanetarySystemEvent event : source) {
                        if ((event != null) && (event.date != null)) {
                            events.add(event);
                        }
                    }
                    events.sort(Comparator.comparing(event -> event.date));
                }
            }
            fireTableDataChanged();
        }

        PlanetarySystemEvent getEventAt(int row) {
            if ((row < 0) || (row >= events.size())) {
                return null;
            }
            return events.get(row);
        }

        int findEventRow(LocalDate date) {
            if (date == null) {
                return -1;
            }
            for (int row = 0; row < events.size(); row++) {
                if (date.equals(events.get(row).date)) {
                    return row;
                }
            }
            return -1;
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_DATE -> resources.getString("PlanetarySystemEditorDialog.eventEditor.date");
                case COL_NADIR -> resources.getString("PlanetarySystemEditorDialog.systemEvents.nadirCharge");
                case COL_ZENITH -> resources.getString("PlanetarySystemEditorDialog.systemEvents.zenithCharge");
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return (system != null) && canEditSupplier.getAsBoolean();
        }

        @Override
        public Object getValueAt(int row, int column) {
            PlanetarySystemEvent event = getEventAt(row);
            if (event == null) {
                return (column == COL_NADIR || column == COL_ZENITH) ? systemEventChargeInheritLabel() : "";
            }
            return switch (column) {
                case COL_DATE -> formatDate(event.date);
                case COL_NADIR -> formatSystemEventCharge(event.nadirCharge);
                case COL_ZENITH -> formatSystemEventCharge(event.zenithCharge);
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            PlanetarySystemEvent event = getEventAt(row);
            if ((system == null) || (event == null)) {
                return;
            }
            switch (column) {
                case COL_DATE -> updateEventDate(event, value);
                case COL_NADIR -> updateBooleanField(event, value, true);
                case COL_ZENITH -> updateBooleanField(event, value, false);
                default -> {
                    // no-op
                }
            }
        }

        private void updateEventDate(PlanetarySystemEvent event, Object value) {
            String text = value == null ? "" : value.toString();
            try {
                LocalDate newDate = parseEventDate(text);
                if (newDate.equals(event.date)) {
                    return;
                }
                if (findEventRow(newDate) >= 0) {
                    JOptionPane.showMessageDialog(PlanetarySystemSystemEventsPanel.this,
                          MessageFormat.format(resources.getString(
                                "PlanetarySystemEditorDialog.systemEvents.add.duplicate"), formatDate(newDate)),
                          resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
                LocalDate oldDate = event.date;
                event.date = newDate;
                system.removeEvent(oldDate);
                system.putEvent(event);
                notifyChange();
                refresh(newDate);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(PlanetarySystemSystemEventsPanel.this, ex.getMessage(),
                      resources.getString("PlanetarySystemEditorDialog.eventEditor.invalid.title"),
                      JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateBooleanField(PlanetarySystemEvent event, Object value, boolean nadir) {
            SourceableValue<Boolean> existing = nadir ? event.nadirCharge : event.zenithCharge;
            Boolean newValue = parseSystemEventCharge(value);
            SourceableValue<Boolean> wrapped = null;
            if (newValue != null) {
                String source = (existing == null) ? null : existing.getSource();
                String version = (existing == null) ? null : existing.getVersion();
                wrapped = SourceableValue.of(source, version, newValue);
            }
            if (nadir) {
                event.nadirCharge = wrapped;
            } else {
                event.zenithCharge = wrapped;
            }
            notifyChange();
            int row = events.indexOf(event);
            if (row >= 0) {
                fireTableRowsUpdated(row, row);
            }
        }

        private String formatSystemEventCharge(SourceableValue<Boolean> value) {
            if ((value == null) || (value.getValue() == null)) {
                return systemEventChargeInheritLabel();
            }
            return value.getValue() ? systemEventChargeYesLabel() : systemEventChargeNoLabel();
        }

        private Boolean parseSystemEventCharge(Object value) {
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            String text = value == null ? "" : value.toString();
            if (systemEventChargeYesLabel().equals(text)) {
                return true;
            }
            if (systemEventChargeNoLabel().equals(text)) {
                return false;
            }
            return null;
        }
    }
}