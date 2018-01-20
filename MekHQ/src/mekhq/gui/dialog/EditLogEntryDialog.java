/*
 * EditLogEntryDialog.java
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
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

import megamek.common.util.EncodeControl;
import mekhq.campaign.LogEntry;

/**
 *
 * @author  Taharqa
 */
public class EditLogEntryDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private LogEntry entry;
    private Date date;

    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JTextArea txtDesc;
    private javax.swing.JButton btnDate;
    private javax.swing.JPanel panBtn;
    private javax.swing.JPanel panMain;

    /** Creates new form NewTeamDialog */
    public EditLogEntryDialog(java.awt.Frame parent, boolean modal, LogEntry e) {
        super(parent, modal);
        this.frame = parent;
        entry = e;
        date = e.getDate();
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

        txtDesc = new javax.swing.JTextArea();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        btnDate = new javax.swing.JButton();
        panBtn = new javax.swing.JPanel();
        panMain = new javax.swing.JPanel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditLogEntryDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        getContentPane().setLayout(new BorderLayout());
        panBtn.setLayout(new GridLayout(0,2));
        panMain.setLayout(new GridBagLayout());

        btnDate = new javax.swing.JButton();
        btnDate.setText(getDateAsString());
        btnDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeDate();
            }
        });
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
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        pack();
    }


    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
    	entry.setDate(date);
    	entry.setDesc(txtDesc.getText());
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
    	entry = null;
    	this.setVisible(false);
    }

    public LogEntry getEntry() {
    	return entry;
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
