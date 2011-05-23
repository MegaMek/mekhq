/*
 * NewContractDialog.java
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

package mekhq;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;

/**
 *
 * @author  Taharqa
 */
public class NewContractDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Contract contract;
    private Campaign campaign;
    private boolean newMission;
    private DecimalFormat formatter;
    
    /** Creates new form NewTeamDialog */
    public NewContractDialog(java.awt.Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.frame = parent;  
        campaign = c;
        contract = new Contract("New Contract", "New Employer");
        contract.calculateContract(campaign);
        formatter = new DecimalFormat();
        initComponents();
    }

    private void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(mekhq.MekHQApp.class).getContext().getResourceMap(CustomizeMissionDialog.class);
        getContentPane().setLayout(new java.awt.GridBagLayout());
      
    	descPanel = new JPanel();
		descPanel.setLayout(new java.awt.GridBagLayout());
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(descPanel, gridBagConstraints);
		
		contractPanel = new JPanel();
		contractPanel.setLayout(new java.awt.GridBagLayout());
		contractPanel.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Contract Parameters"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(contractPanel, gridBagConstraints);
        
		totalsPanel = new JPanel();
		totalsPanel.setLayout(new java.awt.GridBagLayout());
		totalsPanel.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder("Calculated Totals"),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(totalsPanel, gridBagConstraints);
        
        txtName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        scrDesc = new javax.swing.JScrollPane();
        txtDesc = new javax.swing.JTextArea();
          
        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblName, gridBagConstraints);
        
        txtName.setText(contract.getName());
        txtName.setName("txtName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtName, gridBagConstraints);
 
        txtDesc.setText(contract.getDescription());
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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        descPanel.add(scrDesc, gridBagConstraints);
        
        lblBaseAmount1 = new JLabel("Base Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblBaseAmount1, gridBagConstraints);
        lblBaseAmount2 = new JLabel(formatter.format(contract.getBaseAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblBaseAmount2, gridBagConstraints);
        
        lblOverheadAmount1 = new JLabel("Overhead Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblOverheadAmount1, gridBagConstraints);
        lblOverheadAmount2 = new JLabel("+" + formatter.format(contract.getOverheadAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblOverheadAmount2, gridBagConstraints);
       
        lblSupportAmount1 = new JLabel("Straight Support Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblSupportAmount1, gridBagConstraints);
        lblSupportAmount2 = new JLabel("+" + formatter.format(contract.getSupportAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblSupportAmount2, gridBagConstraints);
        
        lblTotalAmount1 = new JLabel("Total Contract Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblTotalAmount1, gridBagConstraints);
        lblTotalAmount2 = new JLabel(formatter.format(contract.getTotalAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblTotalAmount2, gridBagConstraints);
        
        lblSignBonusAmount1 = new JLabel("Signing Bonus:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblSignBonusAmount1, gridBagConstraints);
        lblSignBonusAmount2 = new JLabel("+" + formatter.format(contract.getSigningBonusAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblSignBonusAmount2, gridBagConstraints);
        
        lblFeeAmount1 = new JLabel("MRBC Fee:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblFeeAmount1, gridBagConstraints);
        lblFeeAmount2 = new JLabel("-" + formatter.format(contract.getFeeAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblFeeAmount2, gridBagConstraints);
        
        lblTotalAmountPlus1 = new JLabel("Net Contract Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblTotalAmountPlus1, gridBagConstraints);
        lblTotalAmountPlus2 = new JLabel(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblTotalAmountPlus2, gridBagConstraints);
        
        lblAdvanceMoney1 = new JLabel("Advance Money:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblAdvanceMoney1, gridBagConstraints);
        lblAdvanceMoney2 = new JLabel(formatter.format(contract.getTotalAdvanceMonies()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblAdvanceMoney2, gridBagConstraints);
        
        lblMonthlyAmount1 = new JLabel("Monthly Amount:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;       
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblMonthlyAmount1, gridBagConstraints);
        lblMonthlyAmount2 = new JLabel(formatter.format(contract.getMonthlyPayOut()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblMonthlyAmount2, gridBagConstraints);
        
        lblProfit1 = new JLabel("Estimated Total Profit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;       
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblProfit1, gridBagConstraints);
        lblProfit2 = new JLabel(formatter.format(contract.getEstimatedTotalProfit(campaign)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        totalsPanel.add(lblProfit2, gridBagConstraints);
        
        lblLength = new JLabel("Congtract Length (months):");
        lblMultiplier = new JLabel("Payment Mulitiplier");
        lblOverhead = new JLabel("Overhead Compensation:");
        lblCommandRights = new JLabel("Command Rights:");
        lblStraightSupport = new JLabel("Straight Support %");
        lblBattleLossComp = new JLabel("Battle Loss Compensation %");
        lblSignBonus = new JLabel("Signing Bonus %");
        lblAdvance = new JLabel("Advance %");
        
        checkMRBC = new JCheckBox("Pay MRBC Fee");
        checkMRBC.setSelected(contract.payMRBCFee());
        checkMRBC.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent evt) {
				contract.setMRBCFee(checkMRBC.isSelected());
				contract.calculateContract(campaign);
				refreshTotals();
			}
        });
        
        spnLength = new JSpinner(new SpinnerNumberModel(contract.getLength(), 1, 120, 1));
        spnLength.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setLength((Integer)spnLength.getModel().getValue());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        
        spnMultiplier = new JSpinner(new SpinnerNumberModel(contract.getMultiplier(), 1.0, 10.0, 0.1));
        spnMultiplier.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setMultiplier((Double)spnMultiplier.getModel().getValue());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        
        DefaultComboBoxModel overheadModel = new DefaultComboBoxModel();
		for (int i = 0; i < Contract.OH_NUM; i++) {
			overheadModel.addElement(Contract.getOverheadCompName(i));
		}
		choiceOverhead = new JComboBox(overheadModel);
		choiceOverhead.setSelectedIndex(contract.getOverheadComp());
        choiceOverhead.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				contract.setOverheadComp(choiceOverhead.getSelectedIndex());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        
        DefaultComboBoxModel commandModel = new DefaultComboBoxModel();
		for (int i = 0; i < Contract.COM_NUM; i++) {
			commandModel.addElement(Contract.getCommandRightsName(i));
		}
		choiceCommand = new JComboBox(commandModel);
		choiceCommand.setSelectedIndex(contract.getCommandRights());
		choiceOverhead.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				contract.setCommandRights(choiceCommand.getSelectedIndex());
			}
		});
		
        spnStraightSupport = new JSpinner(new SpinnerNumberModel(contract.getStraightSupport(), 0, 100, 10));
        spnStraightSupport.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setStraightSupport((Integer)spnStraightSupport.getModel().getValue());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        
        spnBattleLossComp = new JSpinner(new SpinnerNumberModel(contract.getBattleLossComp(), 0, 100, 10));
        spnBattleLossComp.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setBattleLossComp((Integer)spnBattleLossComp.getModel().getValue());
			}
		});
        
        spnSignBonus = new JSpinner(new javax.swing.SpinnerNumberModel(contract.getSigningBonusPct(), 0, 10, 1));
        spnSignBonus.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setSigningBonusPct((Integer)spnSignBonus.getModel().getValue());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        spnAdvance = new JSpinner(new javax.swing.SpinnerNumberModel(contract.getAdvancePct(), 0, 25, 5));
        spnAdvance.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent evt) {
				contract.setAdvancePct((Integer)spnAdvance.getModel().getValue());
				contract.calculateContract(campaign);
				refreshTotals();
			}
		});
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(checkMRBC, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblLength, gridBagConstraints);
   
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnLength, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblMultiplier, gridBagConstraints);
   
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnMultiplier, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblCommandRights, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(choiceCommand, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblOverhead, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(choiceOverhead, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblBattleLossComp, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnBattleLossComp, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblStraightSupport, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnStraightSupport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblSignBonus, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnSignBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(lblAdvance, gridBagConstraints);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        contractPanel.add(spnAdvance, gridBagConstraints);
        
     
        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	contract.setName(txtName.getText());
    	contract.setDesc(txtDesc.getText());
    	if(newMission) {
    		campaign.addMission(contract);
    	}
    	this.setVisible(false);
    }
    
    public int getContractId() {
    	return contract.getId();
    }
    
    private void refreshTotals() {
    	lblBaseAmount2.setText(formatter.format(contract.getBaseAmount()));
    	lblOverheadAmount2.setText("+" + formatter.format(contract.getOverheadAmount()));
    	lblSupportAmount2.setText("+" + formatter.format(contract.getSupportAmount()));
    	lblTotalAmount2.setText(formatter.format(contract.getTotalAmount()));
    	lblSignBonusAmount2.setText("+" + formatter.format(contract.getSigningBonusAmount()));
    	lblFeeAmount2.setText("-" + formatter.format(contract.getFeeAmount()));
    	lblTotalAmountPlus2.setText(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
    	lblAdvanceMoney2.setText(formatter.format(contract.getTotalAdvanceMonies()));
    	lblMonthlyAmount2.setText(formatter.format(contract.getMonthlyPayOut()));
    	lblProfit2.setText(formatter.format(contract.getEstimatedTotalProfit(campaign)));
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
    	this.setVisible(false);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel descPanel;
    private JPanel contractPanel;
    private JPanel totalsPanel;
    
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextArea txtDesc;
    private javax.swing.JScrollPane scrDesc;
    
    private JComboBox choiceOverhead;
    private JComboBox choiceCommand;
    private JLabel lblLength;
    private JLabel lblMultiplier;
    private JLabel lblOverhead;
    private JLabel lblCommandRights;
    private JLabel lblBattleLossComp;
    private JLabel lblStraightSupport;
    private JLabel lblSignBonus;
    private JLabel lblAdvance;
    private JSpinner spnLength;
    private JSpinner spnMultiplier;
    private JSpinner spnStraightSupport;
    private JSpinner spnBattleLossComp;
    private JSpinner spnSignBonus;
    private JSpinner spnAdvance;
    private JCheckBox checkMRBC;
    
    private javax.swing.JLabel lblBaseAmount1;
    private javax.swing.JLabel lblOverheadAmount1;
    private javax.swing.JLabel lblSupportAmount1;
    private javax.swing.JLabel lblTotalAmount1;
    private javax.swing.JLabel lblSignBonusAmount1;
    private javax.swing.JLabel lblFeeAmount1;
    private javax.swing.JLabel lblTotalAmountPlus1;
    private javax.swing.JLabel lblAdvanceMoney1;
    private javax.swing.JLabel lblMonthlyAmount1;
    private javax.swing.JLabel lblProfit1;
    
    private javax.swing.JLabel lblBaseAmount2;
    private javax.swing.JLabel lblOverheadAmount2;
    private javax.swing.JLabel lblSupportAmount2;
    private javax.swing.JLabel lblTotalAmount2;
    private javax.swing.JLabel lblSignBonusAmount2;
    private javax.swing.JLabel lblFeeAmount2;
    private javax.swing.JLabel lblTotalAmountPlus2;
    private javax.swing.JLabel lblAdvanceMoney2;
    private javax.swing.JLabel lblMonthlyAmount2;
    private javax.swing.JLabel lblProfit2;
    
    // End of variables declaration//GEN-END:variables

}
