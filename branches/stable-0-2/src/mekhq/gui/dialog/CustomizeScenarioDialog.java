/*
 * CustomizeScenarioDialog.java
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;

/**
 *
 * @author  Taharqa
 */
public class CustomizeScenarioDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Scenario scenario;
    private Mission mission;
    private Campaign campaign;
    private boolean newScenario;
    private Date date;
    private SimpleDateFormat dateFormatter;

    /** Creates new form NewTeamDialog */
    public CustomizeScenarioDialog(java.awt.Frame parent, boolean modal, Scenario s, Mission m, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        this.mission = m;
        if(null == s) {
        	scenario = new Scenario("New Scenario");
        	newScenario = true;
        } else {
        	scenario = s;
        	newScenario = false;
        }
        campaign = c;
        date = scenario.getDate();
        if(null == date) {
        	date = campaign.getCalendar().getTime();
        }
        dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	 java.awt.GridBagConstraints gridBagConstraints;

        txtName = new javax.swing.JTextField();
        txtDesc = new javax.swing.JTextArea();
        txtReport = new javax.swing.JTextArea();
        lblName = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        scrDesc = new javax.swing.JScrollPane();
        scrReport = new javax.swing.JScrollPane();
        choiceStatus = new javax.swing.JComboBox();
        lblStatus = new javax.swing.JLabel();  
        panMain = new javax.swing.JPanel();
        panBtn = new javax.swing.JPanel();
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeScenarioDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.new"));

        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0,2));
        
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(lblName, gridBagConstraints);
        
        txtName.setText(scenario.getName());
        txtName.setName("txtName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtName, gridBagConstraints);
 
        if(!scenario.isCurrent()) {
	        lblStatus.setText(resourceMap.getString("lblStatus.text"));
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy++;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	        panMain.add(lblStatus, gridBagConstraints);
	        
	        DefaultComboBoxModel statusModel = new DefaultComboBoxModel();
			for (int i = 1; i < Scenario.S_NUM; i++) {
				statusModel.addElement(Scenario.getStatusName(i));
			}
			choiceStatus.setModel(statusModel);
			choiceStatus.setName("choiceStatus"); // NOI18N
			choiceStatus.setSelectedIndex(scenario.getStatus()-1);     
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	        panMain.add(choiceStatus, gridBagConstraints);
	        
	        btnDate = new javax.swing.JButton();
	        btnDate.setText(dateFormatter.format(date));
	        btnDate.addActionListener(new java.awt.event.ActionListener() {
	            public void actionPerformed(java.awt.event.ActionEvent evt) {
	                changeDate();
	            }
	        });
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy++;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.weightx = 1.0;
	        gridBagConstraints.weighty = 0.0;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	        panMain.add(btnDate, gridBagConstraints);

        }
        
        txtDesc.setText(scenario.getDescription());
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Description"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDesc.setViewportView(txtDesc);
        scrDesc.setPreferredSize(new Dimension(400,200));
        scrDesc.setMinimumSize(new Dimension(400,200));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panMain.add(scrDesc, gridBagConstraints);
        
        if(!scenario.isCurrent()) {
	        txtReport.setText(scenario.getReport());
	        txtReport.setName("txtReport");
	        txtReport.setEditable(true);
	        txtReport.setLineWrap(true);
	        txtReport.setWrapStyleWord(true);
	        txtReport.setBorder(BorderFactory.createCompoundBorder(
		   			 BorderFactory.createTitledBorder("After-Action Report"),
		   			 BorderFactory.createEmptyBorder(5,5,5,5)));
	        scrReport.setViewportView(txtReport);
	        scrReport.setPreferredSize(new Dimension(400,200));
	        scrReport.setMinimumSize(new Dimension(400,200));
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy++;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.weightx = 1.0;
	        gridBagConstraints.weighty = 1.0;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
	        panMain.add(scrReport, gridBagConstraints);
        }
        
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        
        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	scenario.setName(txtName.getText());
    	scenario.setDesc(txtDesc.getText());
    	if(!scenario.isCurrent()) {
    		scenario.setReport(txtReport.getText());
    		scenario.setStatus(choiceStatus.getSelectedIndex()+1);
    		scenario.setDate(date);
    	}
    	if(newScenario) {
    		campaign.addScenario(scenario, mission);
    	}
    	this.setVisible(false);
    }
    
    public int getMissionId() {
    	return mission.getId();
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	this.setVisible(false);
    }
    
    private void changeDate() {
        // show the date chooser
    	GregorianCalendar cal = new GregorianCalendar();
    	cal.setTime(date);
        DateChooser dc = new DateChooser(frame, cal);
        // user can eiter choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
        	if(campaign.getCalendar().getTime().after(campaign.getCalendar().getTime())) {
        		JOptionPane.showMessageDialog(frame,
        			    "You cannot choose a date after the current date.",
        			    "Invalid date",
        			    JOptionPane.ERROR_MESSAGE);
        		return;
        	}
            date = dc.getDate().getTime();
            btnDate.setText(dateFormatter.format(date));
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel panMain;
    private javax.swing.JPanel panBtn;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextArea txtDesc;
    private javax.swing.JScrollPane scrDesc;
    private javax.swing.JTextArea txtReport;
    private javax.swing.JScrollPane scrReport;
    private javax.swing.JComboBox choiceStatus;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JButton btnDate;




    // End of variables declaration//GEN-END:variables

}
