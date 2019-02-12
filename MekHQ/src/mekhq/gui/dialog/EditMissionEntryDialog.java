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
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Person;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.ResourceBundle;

public class EditMissionEntryDialog extends JDialog {
    private Frame frame;
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

    /** Creates new form NewTeamDialog */
    public EditMissionEntryDialog(Frame parent, boolean modal, LogEntry entry, Person person) {
        super(parent, modal);

        this.frame = parent;
        this.entry = entry;
        this.person = person;

        newDate = this.entry.getDate();
        originalDate = this.entry.getDate();
        originalDescription = txtDesc.getText();

        initComponents();
        setLocationRelativeTo(parent);
    }

    public Optional<LogEntry> getEntry() {
        if (entry == null) {
            return Optional.empty();
        }

        return Optional.of(entry);
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditMissionEntryDialog", new EncodeControl()); //$NON-NLS-1$
        GridBagConstraints gridBagConstraints;

        panMain = new JPanel();
        btnDate = new JButton();
        txtDesc = new JTextField();

        panBtn = new JPanel();
        btnOK = new JButton();
        btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
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
        btnOK.addActionListener(evt -> btnOKActionPerformed(evt));
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(evt -> btnCloseActionPerformed(evt));
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
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
        entry.onLogEntryEdited(originalDate, newDate, originalDescription, txtDesc.getText(), person);
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
