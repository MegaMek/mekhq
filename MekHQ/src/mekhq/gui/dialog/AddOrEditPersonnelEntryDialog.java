/*
 * AddOrEditPersonnelEntryDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 *
 * @author  Taharqa
 */
public class AddOrEditPersonnelEntryDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private Frame frame;
    private int operationType;
    private LogEntry entry;
    private Date date;
    private Date originalDate;
    private String originalDescription;

    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JTextArea txtDesc;
    private javax.swing.JButton btnDate;
    private javax.swing.JPanel panBtn;
    private javax.swing.JPanel panMain;

    public AddOrEditPersonnelEntryDialog(Frame parent, boolean modal, Date entryDate) {
        this(parent, modal, ADD_OPERATION, new PersonalLogEntry(entryDate, ""));
    }

    public AddOrEditPersonnelEntryDialog(Frame parent, boolean modal, LogEntry entry) {
        this(parent, modal, EDIT_OPERATION, entry);
    }

    private AddOrEditPersonnelEntryDialog(Frame parent, boolean modal, int operationType, LogEntry entry) {
        super(parent, modal);

        assert entry != null;

        this.frame = parent;
        this.operationType = operationType;
        this.entry = entry;

        this.date = this.entry.getDate();
        this.originalDate = this.entry.getDate();
        this.originalDescription = this.entry.getDesc();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public Optional<LogEntry> getEntry() {
        return Optional.ofNullable(entry);
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditPersonnelEntryDialog", new EncodeControl()); //$NON-NLS-1$
    	java.awt.GridBagConstraints gridBagConstraints;

        panMain = new javax.swing.JPanel();
        btnDate = new javax.swing.JButton();
        txtDesc = new javax.swing.JTextArea();

        panBtn = new javax.swing.JPanel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }
        getContentPane().setLayout(new BorderLayout());
        panBtn.setLayout(new GridLayout(0,2));
        panMain.setLayout(new GridBagLayout());
        
        btnDate = new javax.swing.JButton();
        btnDate.setText(getDateAsString());
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
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
        PreferencesNode preferences = MekHQ.getPreferences().forClass(AddOrEditPersonnelEntryDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
    	entry.setDate(date);
    	entry.setDesc(txtDesc.getText());
    	entry.onLogEntryEdited(originalDate, date, originalDescription, txtDesc.getText(), null);
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
    	entry = null;
    	this.setVisible(false);
    }

    private void changeDate() {
    	GregorianCalendar cal = new GregorianCalendar();
    	cal.setTime(date);
        DateChooser dc = new DateChooser(frame, cal);
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate().getTime();
            btnDate.setText(getDateAsString());
        }
    }
    
    private String getDateAsString() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d yyyy");
        return dateFormat.format(date);
    }
}
