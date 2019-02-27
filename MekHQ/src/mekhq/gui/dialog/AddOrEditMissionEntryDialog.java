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
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.ServiceLogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

import javax.swing.*;
import javax.swing.text.html.Option;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Dialog used to add or edit mission entries.
 */
public class AddOrEditMissionEntryDialog extends JDialog {
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private Frame frame;
    private int operationType;
    private Person person;
    private LogEntry entry;
    private Date originalDate;
    private String originalDescription;
    private Date newDate;

    private JPanel panMain;
    private JButton btnDate;
    private JTextField txtDesc;
    private JPanel panBtn;
    private JButton btnOK;
    private JButton btnClose;

    public AddOrEditMissionEntryDialog(Frame parent, boolean modal, Date entryDate) {
        this(parent, modal, ADD_OPERATION, new ServiceLogEntry(entryDate, ""));
    }

    public AddOrEditMissionEntryDialog(Frame parent, boolean modal, LogEntry entry) {
        this(parent, modal, EDIT_OPERATION, entry);
    }

    private AddOrEditMissionEntryDialog(Frame parent, boolean modal, int operationType, LogEntry entry) {
        super(parent, modal);

        assert entry != null;

        this.frame = parent;
        this.operationType = operationType;
        this.entry = entry;

        newDate = this.entry.getDate();
        originalDate = this.entry.getDate();
        originalDescription = this.entry.getDesc();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public Optional<LogEntry> getEntry() {
        return Optional.ofNullable(entry);
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditMissionEntryDialog", new EncodeControl()); //$NON-NLS-1$
        GridBagConstraints gridBagConstraints;

        panMain = new JPanel();
        btnDate = new JButton();
        txtDesc = new JTextField();

        panBtn = new JPanel();
        btnOK = new JButton();
        btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }
        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0,2));

        btnDate = new JButton();
        btnDate.setText(getDateAsString(newDate));
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
        txtDesc.setColumns(30);
        txtDesc.grabFocus();
        txtDesc.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(txtDesc, gridBagConstraints);

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(AddOrEditMissionEntryDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void changeDate() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(newDate);
        DateChooser dc = new DateChooser(frame, cal);
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            newDate = dc.getDate().getTime();
            btnDate.setText(getDateAsString(newDate));
        }
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        entry.setDate(newDate);
        entry.setDesc(txtDesc.getText());
        entry.onLogEntryEdited(originalDate, newDate, originalDescription, txtDesc.getText(), null);
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        entry = null;
        this.setVisible(false);
    }

    private static String getDateAsString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d yyyy");
        return dateFormat.format(date);
    }
}
