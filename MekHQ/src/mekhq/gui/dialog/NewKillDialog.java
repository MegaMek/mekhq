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
import java.util.Date;
import java.util.ResourceBundle;

import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.Unit;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Taharqa
 */
public class NewKillDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Campaign campaign;
    private Date date;
    private Person person;
    
    /** Creates new form NewTeamDialog */
    public NewKillDialog(java.awt.Frame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        this.frame = parent;
        campaign = c;
        date = campaign.getCalendar().getTime();
        person = p;
        initComponents();
        setLocationRelativeTo(parent);
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
    
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewKillDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title") + " " + person.getName());
        getContentPane().setLayout(new java.awt.GridBagLayout());
        
        lblKill.setText(resourceMap.getString("lblKill.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(lblKill, gridBagConstraints);
        
        txtKill.setText("");
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
        
        Unit u = campaign.getUnit(person.getUnitId());
        if(null != u) {
        	txtKiller.setText(u.getName());
        } else {
        	txtKiller.setText("Bare hands?");
        }
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
        btnDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	btnDateActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        getContentPane().add(btnDate, gridBagConstraints);
        
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	campaign.addKill(new Kill(person.getId(), txtKill.getText(), txtKiller.getText(), date));  	
    	this.setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	this.setVisible(false);
    }
    
    private void btnDateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDateActionPerformed
        // show the date chooser
        DateChooser dc = new DateChooser(frame, campaign.getCalendar());
        // user can eiter choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            date = dc.getDate().getTime();
            btnDate.setText(getDateAsString());
        }
    }
    
    private String getDateAsString() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d yyyy");
        return dateFormat.format(date);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblKill;
    private javax.swing.JTextField txtKill;
    private javax.swing.JLabel lblKiller;
    private javax.swing.JTextField txtKiller;
    private javax.swing.JButton btnDate;


    // End of variables declaration//GEN-END:variables

}
