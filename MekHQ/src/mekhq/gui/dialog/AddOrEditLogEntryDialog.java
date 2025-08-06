/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedText;

import java.awt.Container;
import java.time.LocalDate;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

/**
 * A dialog for adding or editing medical log entries for a person.
 *
 * <p>This dialog supports both creating new medical log entries and modifying existing ones. It provides fields for
 * the date and description of the entry.</p>
 *
 * @author Taharqa
 */
public class AddOrEditLogEntryDialog extends AbstractMHQButtonDialog {
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private static final int PADDING = scaleForGUI(5);

    private final Person person;
    private final LogEntry entry;
    private LocalDate date;
    private JTextArea txtDescription;
    private JButton btnDate;

    /**
     * Constructs a dialog for adding a new medical log entry.
     *
     * @param parent    the parent frame for this dialog
     * @param person    the person for whom the entry is being added. Can be null
     * @param entryDate the date for the new entry
     */
    public AddOrEditLogEntryDialog(final JFrame parent, final @Nullable Person person, final LocalDate entryDate) {
        this(parent, ADD_OPERATION, person, new PersonalLogEntry(entryDate, ""));
    }

    /**
     * Constructs a dialog for editing an existing medical log entry.
     *
     * @param parent the parent frame for this dialog
     * @param person the person whose entry is being edited, may be null
     * @param entry  the log entry to edit
     */
    public AddOrEditLogEntryDialog(final JFrame parent, final @Nullable Person person, final LogEntry entry) {
        this(parent, EDIT_OPERATION, person, entry);
    }

    /**
     * Private constructor used by the public constructors to initialize the dialog.
     *
     * @param parent        the parent frame for this dialog
     * @param operationType the type of operation (ADD_OPERATION or EDIT_OPERATION)
     * @param person        the person whose entry is being added or edited, may be null
     * @param entry         the log entry to add or edit
     */
    private AddOrEditLogEntryDialog(final JFrame parent, final int operationType, final @Nullable Person person,
          final LogEntry entry) {
        super(parent,
              "AddOrEditPersonnelEntryDialog",
              operationType == ADD_OPERATION ? "logController.btnAdd.text" : "logController.btnEdit.text");

        this.person = person;
        this.entry = Objects.requireNonNull(entry);

        date = entry.getDate();

        initialize();
    }

    public LogEntry getEntry() {
        return entry;
    }

    /**
     * Creates the central panel of the dialog with the date button and description text area.
     *
     * @return the container with the dialog's main components
     */
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> changeDate());

        txtDescription = new JTextArea(entry.getDesc());
        txtDescription.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(getFormattedText(
                    "logController.txtDescription.title")),
              BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)));
        txtDescription.setName("txtDescription");
        txtDescription.setMinimumSize(scaleForGUI(250, 75));
        txtDescription.setPreferredSize(scaleForGUI(250, 75));
        txtDescription.setEditable(true);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setName("entryPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(btnDate).addComponent(txtDescription));

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(btnDate)
                                        .addComponent(txtDescription));

        return panel;
    }

    @Override
    protected void okAction() {
        final LocalDate originalDate = entry.getDate();
        final String originalDescription = entry.getDesc();
        entry.setDate(date);
        entry.setDesc(txtDescription.getText());
        entry.onLogEntryEdited(originalDate, date, originalDescription, txtDescription.getText(), person);
    }

    /**
     * Opens a date chooser dialog to change the entry date.
     *
     * <p>Updates the button text and stored date if a new date is selected.</p>
     */
    private void changeDate() {
        DateChooser dateChooser = new DateChooser(getFrame(), date);
        if (dateChooser.showDateChooser() == DateChooser.OK_OPTION) {
            date = dateChooser.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        }
    }
}
