/*
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.time.LocalDate;

import javax.swing.*;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

/**
 * @author  Taharqa
 */
public class AddOrEditPersonnelEntryDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
	private static final long serialVersionUID = -8038099101234445018L;
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private final Person person;
    private final LogEntry entry;
    private LocalDate date;

    private JTextArea txtDescription;
    private JButton btnDate;
    //endregion Variable Declarations

    //region Constructors
    public AddOrEditPersonnelEntryDialog(final JFrame parent, final @Nullable Person person,
                                         final LocalDate entryDate) {
        this(parent, ADD_OPERATION, person, new PersonalLogEntry(entryDate, ""));
    }

    public AddOrEditPersonnelEntryDialog(final JFrame parent, final @Nullable Person person,
                                         final LogEntry entry) {
        this(parent, EDIT_OPERATION, person, entry);
    }

    private AddOrEditPersonnelEntryDialog(final JFrame parent, final int operationType,
                                          final @Nullable Person person, final LogEntry entry) {
        super(parent, "AddOrEditPersonnelEntryDialog", (operationType == ADD_OPERATION)
                ? "AddOrEditPersonnelEntryDialog.AddEntry.title" : "AddOrEditPersonnelEntryDialog.EditEntry.title");
        assert entry != null;

        this.person = person;
        this.entry = entry;

        setDate(entry.getDate());

        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable Person getPerson() {
        return person;
    }

    public LogEntry getEntry() {
        return entry;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        // Create Panel Components
        btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate()));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> changeDate());

        txtDescription = new JTextArea(getEntry().getDesc());
        txtDescription.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resources.getString("txtDescription.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        txtDescription.setName("txtDescription");
        txtDescription.setMinimumSize(new Dimension(250,75));
        txtDescription.setPreferredSize(new Dimension(250,75));
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

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(btnDate)
                        .addComponent(txtDescription)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(btnDate)
                        .addComponent(txtDescription)
        );

        return panel;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        final LocalDate originalDate = getEntry().getDate();
        final String originalDescription = getEntry().getDesc();
        getEntry().setDate(getDate());
        getEntry().setDesc(txtDescription.getText());
        getEntry().onLogEntryEdited(originalDate, getDate(), originalDescription,
                txtDescription.getText(), getPerson());
    }

    private void changeDate() {
        DateChooser dc = new DateChooser(getFrame(), getDate());
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            setDate(dc.getDate());
            btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(getDate()));
        }
    }
}
