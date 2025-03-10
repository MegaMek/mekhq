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
 */
package mekhq.gui.dialog;

import java.awt.*;
import java.time.LocalDate;
import java.util.Objects;

import javax.swing.*;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;

/**
 * @author Taharqa
 */
public class AddOrEditPersonnelEntryDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
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

        this.person = person;
        this.entry = Objects.requireNonNull(entry);

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
        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()));
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
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(getDate()));
        }
    }
}
