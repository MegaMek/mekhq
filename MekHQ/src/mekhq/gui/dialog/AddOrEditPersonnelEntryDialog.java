/*
 * AddOrEditPersonnelEntryDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import mekhq.campaign.personnel.Person;

/**
 * @author  Taharqa
 */
public class AddOrEditPersonnelEntryDialog extends JDialog {
    //region Variable Declarations
	private static final long serialVersionUID = -8038099101234445018L;
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private final JFrame frame;
    private final int operationType;
    private final Person person;
    private LogEntry entry;
    private LocalDate date;
    private LocalDate originalDate;
    private String originalDescription;

    private JButton btnClose;
    private JButton btnOK;
    private JTextArea txtDesc;
    private JButton btnDate;
    private JPanel panBtn;
    private JPanel panMain;
    //endregion Variable Declarations

    //region Constructors
    public AddOrEditPersonnelEntryDialog(final JFrame parent, final boolean modal,
                                         final Person person, final LocalDate entryDate) {
        this(parent, modal, ADD_OPERATION, person, new PersonalLogEntry(entryDate, ""));
    }

    public AddOrEditPersonnelEntryDialog(final JFrame parent, final boolean modal, final Person person,
                                         final LogEntry entry) {
        this(parent, modal, EDIT_OPERATION, person, entry);
    }

    private AddOrEditPersonnelEntryDialog(final JFrame parent, final boolean modal, final int operationType,
                                          final Person person, final LogEntry entry) {
        super(parent, modal);
        assert entry != null;
        assert person != null;

        this.frame = parent;
        this.operationType = operationType;
        this.person = person;
        this.entry = entry;

        this.date = entry.getDate();
        this.originalDate = entry.getDate();
        this.originalDescription = entry.getDesc();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    //endregion Constructors

    //region Getters/Setter
    public Person getPerson() {
        return person;
    }
    //endregion Getters/Setter

    public Optional<LogEntry> getEntry() {
        return Optional.ofNullable(entry);
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditPersonnelEntryDialog", new EncodeControl());
    	GridBagConstraints gridBagConstraints;

        panMain = new JPanel();
        btnDate = new JButton();
        txtDesc = new JTextArea();

        panBtn = new JPanel();
        btnOK = new JButton();
        btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }
        getContentPane().setLayout(new BorderLayout());
        panBtn.setLayout(new GridLayout(0,2));
        panMain.setLayout(new GridBagLayout());

        btnDate = new JButton();
        btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(btnDate, gridBagConstraints);

        txtDesc.setText(entry.getDesc());
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Description"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtDesc.setPreferredSize(new Dimension(250,75));
        txtDesc.setMinimumSize(new Dimension(250,75));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(txtDesc, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOkay.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());

        this.setName("AddOrEditPersonnelEntryDialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(final ActionEvent evt) {
    	entry.setDate(date);
    	entry.setDesc(txtDesc.getText());
    	entry.onLogEntryEdited(originalDate, date, originalDescription, txtDesc.getText(), getPerson());
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(final ActionEvent evt) {
    	entry = null;
    	this.setVisible(false);
    }

    private void changeDate() {
        DateChooser dc = new DateChooser(frame, date);
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate();
            btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));
        }
    }
}
