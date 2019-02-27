/*
 * NewKillDialog.java
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

import java.awt.Frame;
import java.text.SimpleDateFormat;
import java.util.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Kill;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;


/**
 *
 * @author  Taharqa
 */
public class AddOrEditKillEntryDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private static final int ADD_OPERATION = 1;
    private static final int EDIT_OPERATION = 2;

    private Frame frame;
    private int operationType;
    private Kill kill;
    private Date date;

    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblKill;
    private javax.swing.JTextField txtKill;
    private javax.swing.JLabel lblKiller;
    private javax.swing.JTextField txtKiller;
    private javax.swing.JButton btnDate;

    public AddOrEditKillEntryDialog(Frame parent, boolean modal, UUID killerPerson, String killerUnit, Date entryDate) {
        this(parent, modal, ADD_OPERATION, new Kill(killerPerson, "?", killerUnit, entryDate));
    }

    public AddOrEditKillEntryDialog(Frame parent, boolean modal, Kill kill) {
        this(parent, modal, EDIT_OPERATION, kill);
    }

    private AddOrEditKillEntryDialog(Frame parent, boolean modal, int operationType, Kill kill) {
        super(parent, modal);

        assert kill != null;

        this.frame = parent;
        this.kill = kill;
        this.date = this.kill.getDate();
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public Optional<Kill> getKill() {
        return Optional.empty().ofNullable(kill);
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtKill = new javax.swing.JTextField();
        lblKill = new javax.swing.JLabel();
        txtKiller = new javax.swing.JTextField();
        lblKiller = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        btnDate = new javax.swing.JButton();
    
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AddOrEditKillEntryDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        if (this.operationType == ADD_OPERATION) {
            setTitle(resourceMap.getString("dialogAdd.title"));
        } else {
            setTitle(resourceMap.getString("dialogEdit.title"));
        }
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        lblKill.setText(resourceMap.getString("lblKill.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblKill, gridBagConstraints);
        
        txtKill.setText(kill.getWhatKilled());
        txtKill.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtKill, gridBagConstraints);
        
        lblKiller.setText(resourceMap.getString("lblKiller.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblKiller, gridBagConstraints);
        
        txtKiller.setText(kill.getKilledByWhat());
        txtKiller.setMinimumSize(new java.awt.Dimension(150, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(txtKiller, gridBagConstraints);
 
        btnDate.setText(getDateAsString());
        btnDate.setName("btnDate"); // NOI18N
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(btnDate, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(AddOrEditKillEntryDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	kill.setWhatKilled(txtKill.getText());
    	kill.setKilledByWhat(txtKiller.getText());
    	kill.setDate(date);
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	kill = null;
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
