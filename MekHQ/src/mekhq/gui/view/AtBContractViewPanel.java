/*
 * AtBContractViewPanel.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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

package mekhq.gui.view;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;

/**
 * @author Neoancient
 * 
 * A version of ContractViewPanel with additional details for
 * Against the Bot
 *
 */
public class AtBContractViewPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9190665158803529105L;
	private Campaign campaign;
	private AtBContract contract;
	
	private JPanel pnlStats;
	private JTextArea txtDesc;
	
	private JLabel lblStatus;
	private JLabel lblLocation;
	private JTextArea txtLocation;
	private JLabel lblType;
	private JTextArea txtType;
	private JLabel lblEmployer;
	private JTextArea txtEmployer;
	private JLabel lblEnemy;
	private JTextArea txtEnemy;
	private JLabel lblAllyRating;
	private JTextArea txtAllyRating;
	private JLabel lblEnemyRating;
	private JTextArea txtEnemyRating;
	private JLabel lblStartDate;
	private JTextArea txtStartDate;
	private JLabel lblEndDate;
	private JTextArea txtEndDate;
	private JLabel lblPayout;
	private JTextArea txtPayout;
	private JLabel lblCommand;
	private JTextArea txtCommand;
	private JLabel lblBLC;
	private JTextArea txtBLC;
	private JLabel lblSalvageValueMerc;
	private JTextArea txtSalvageValueMerc;
	private JLabel lblSalvageValueEmployer;
	private JTextArea txtSalvageValueEmployer;
	private JLabel lblSalvagePct1;
	private JLabel lblSalvagePct2;
	private JLabel lblMorale;
	private JTextArea txtMorale;
	private JLabel lblScore;
	private JTextArea txtScore;
	private JLabel lblSharePct;
	private JTextArea txtSharePct;
	
	public AtBContractViewPanel(AtBContract contract, Campaign campaign) {
		this.contract = contract;
		this.campaign = campaign;
		initComponents();
	}
	
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		pnlStats = new JPanel();
		txtDesc = new JTextArea();
		       
		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		pnlStats.setName("pnlStats");
		pnlStats.setBorder(BorderFactory.createTitledBorder(contract.getName()));
		pnlStats.setBackground(Color.WHITE);
		fillStats();
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(pnlStats, gridBagConstraints);
		
	}

    private void fillStats() {
    	
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};

        lblStatus = new JLabel();
    	lblLocation = new JLabel();
		txtLocation = new JTextArea();
    	lblEmployer = new JLabel();
		txtEmployer = new JTextArea();
		lblEnemy = new JLabel();
		txtEnemy = new JTextArea();
		lblType = new JLabel();
		txtType = new JTextArea();
		lblStartDate = new JLabel();
		txtStartDate = new JTextArea();
		lblEndDate = new JLabel();
		txtEndDate = new JTextArea();
		lblPayout = new JLabel();
		txtPayout = new JTextArea();
		lblCommand = new JLabel();
		txtCommand = new JTextArea();
		lblBLC = new JLabel();
		txtBLC = new JTextArea();
		lblAllyRating = new JLabel();
		txtAllyRating = new JTextArea();
		lblEnemyRating = new JLabel();
		txtEnemyRating = new JTextArea();
		lblMorale = new JLabel();
		txtMorale = new JTextArea();
		lblSharePct = new JLabel();
		txtSharePct = new JTextArea();
		lblScore = new JLabel();
		txtScore = new JTextArea();
		
		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel", new EncodeControl()); //$NON-NLS-1$

    	java.awt.GridBagConstraints gridBagConstraints;
		pnlStats.setLayout(new java.awt.GridBagLayout());
		
		SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
		
		int y = 0;
		
		lblStatus.setName("lblOwner"); // NOI18N
		lblStatus.setText("<html><b>" + contract.getStatusName() + "</b></html>");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 0.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStatus, gridBagConstraints);
		

		lblLocation.setName("lblLocation"); // NOI18N
		lblLocation.setText(resourceMap.getString("lblLocation.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblLocation, gridBagConstraints);
		
		txtLocation.setName("txtLocation"); // NOI18N
		txtLocation.setText(contract.getPlanetName());
		txtLocation.setEditable(false);
		txtLocation.setLineWrap(true);
		txtLocation.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtLocation, gridBagConstraints);
		
		lblEmployer.setName("lblEmployer"); // NOI18N
		lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEmployer, gridBagConstraints);
		
		txtEmployer.setName("txtEmployer"); // NOI18N
		txtEmployer.setText(contract.getEmployerName(campaign.getEra()));
		txtEmployer.setEditable(false);
		txtEmployer.setLineWrap(true);
		txtEmployer.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEmployer, gridBagConstraints);
		
		lblEnemy.setName("lblEnemy"); // NOI18N
		lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEnemy, gridBagConstraints);
		
		txtEnemy.setName("txtEnemy"); // NOI18N
		txtEnemy.setText(contract.getEnemyName(campaign.getEra()));
		txtEnemy.setEditable(false);
		txtEnemy.setLineWrap(true);
		txtEnemy.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEnemy, gridBagConstraints);
		
		lblType.setName("lblType"); // NOI18N
		lblType.setText(resourceMap.getString("lblType.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblType, gridBagConstraints);
		
		txtType.setName("txtType"); // NOI18N
		txtType.setText(contract.getType());
		txtType.setEditable(false);
		txtType.setLineWrap(true);
		txtType.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtType, gridBagConstraints);
		
		lblAllyRating.setName("lblAllyRating"); // NOI18N
		lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblAllyRating, gridBagConstraints);
		
		txtAllyRating.setName("txtAllyRating"); // NOI18N
		txtAllyRating.setText(skillNames[contract.getAllySkill()] + "/" +
				ratingNames[contract.getAllyQuality()]);
		txtAllyRating.setEditable(false);
		txtAllyRating.setLineWrap(true);
		txtAllyRating.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtAllyRating, gridBagConstraints);
		
		lblEnemyRating.setName("lblEnemyRating"); // NOI18N
		lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEnemyRating, gridBagConstraints);
		
		txtEnemyRating.setName("txtEnemyRating"); // NOI18N
		txtEnemyRating.setText(skillNames[contract.getEnemySkill()] + "/" +
				ratingNames[contract.getEnemyQuality()]);
		txtEnemyRating.setEditable(false);
		txtEnemyRating.setLineWrap(true);
		txtEnemyRating.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEnemyRating, gridBagConstraints);
		
		lblStartDate.setName("lblStartDate"); // NOI18N
		lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblStartDate, gridBagConstraints);
		
		txtStartDate.setName("txtStartDate"); // NOI18N
		txtStartDate.setText(shortDateFormat.format(contract.getStartDate()));
		txtStartDate.setEditable(false);
		txtStartDate.setLineWrap(true);
		txtStartDate.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtStartDate, gridBagConstraints);
		
		lblEndDate.setName("lblEndDate"); // NOI18N
		lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblEndDate, gridBagConstraints);
		
		txtEndDate.setName("txtEndDate"); // NOI18N
		txtEndDate.setText(shortDateFormat.format(contract.getEndingDate()));
		txtEndDate.setEditable(false);
		txtEndDate.setLineWrap(true);
		txtEndDate.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtEndDate, gridBagConstraints);
		
		lblPayout.setName("lblPayout"); // NOI18N
		lblPayout.setText(resourceMap.getString("lblPayout.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblPayout, gridBagConstraints);
		
		DecimalFormat numFormatter = new DecimalFormat();
		txtPayout.setName("txtPayout"); // NOI18N
		txtPayout.setText(numFormatter.format(contract.getMonthlyPayOut()) + " C-Bills");
		txtPayout.setEditable(false);
		txtPayout.setLineWrap(true);
		txtPayout.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtPayout, gridBagConstraints);
		
		lblCommand.setName("lblCommand"); // NOI18N
		lblCommand.setText(resourceMap.getString("lblCommand.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblCommand, gridBagConstraints);
		
		txtCommand.setName("txtCommand"); // NOI18N
		txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
		txtCommand.setEditable(false);
		txtCommand.setLineWrap(true);
		txtCommand.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtCommand, gridBagConstraints);
		
		lblBLC.setName("lblBLC"); // NOI18N
		lblBLC.setText(resourceMap.getString("lblBLC.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblBLC, gridBagConstraints);
		
		txtBLC.setName("txtBLC"); // NOI18N
		txtBLC.setText(contract.getBattleLossComp() + "%");
		txtBLC.setEditable(false);
		txtBLC.setLineWrap(true);
		txtBLC.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtBLC, gridBagConstraints);
		
		DecimalFormat formatter = new DecimalFormat();

		if(contract.getSalvagePct() > 0 && !contract.isSalvageExchange()) {
			lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));   	
	    	gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = y;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
	    	txtSalvageValueMerc = new JTextArea();
	    	txtSalvageValueMerc.setText(formatter.format(contract.getSalvagedByUnit()) + " C-Bills");   	
	    	txtSalvageValueMerc.setEditable(false);
	    	txtSalvageValueMerc.setLineWrap(true);
	    	txtSalvageValueMerc.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = y++;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(txtSalvageValueMerc, gridBagConstraints);

	        lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));   	
	    	gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = y;
	        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
	    	txtSalvageValueEmployer = new JTextArea();
	    	txtSalvageValueEmployer.setText(formatter.format(contract.getSalvagedByEmployer()) + " C-Bills");   	
	    	txtSalvageValueEmployer.setEditable(false);
	    	txtSalvageValueEmployer.setLineWrap(true);
	    	txtSalvageValueEmployer.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = y++;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
		}
		lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
		lblSalvagePct2 = new JLabel();

		if(contract.isSalvageExchange()) {
			lblSalvagePct2.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)"); 
		} else if(contract.getSalvagePct() == 0) {
			lblSalvagePct2.setText(resourceMap.getString("none")); 
		} else {
			lblSalvagePct1.setText(resourceMap.getString("lblSalvagePct.text"));   
			int maxSalvagePct = contract.getSalvagePct();
	    	int currentSalvagePct = (int)(100*((double)contract.getSalvagedByUnit())/(contract.getSalvagedByUnit()+contract.getSalvagedByEmployer()));
	    	String lead = "<html><font color='black'>";
	        if(currentSalvagePct > maxSalvagePct) {
	        	lead = "<html><font color='red'>";
	        }
	    	lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <font color='black'>(max " + maxSalvagePct + "%)</font></html>");   	
		}
       	
    	gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct1, gridBagConstraints); 
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct2, gridBagConstraints);

		lblMorale.setName("lblMorale"); // NOI18N
		lblMorale.setText(resourceMap.getString("lblMorale.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblMorale, gridBagConstraints);
		
		txtMorale.setName("txtMorale"); // NOI18N
		txtMorale.setText(contract.getMoraleLevelName());
		txtMorale.setEditable(false);
		txtMorale.setLineWrap(true);
		txtMorale.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtMorale, gridBagConstraints);

		if (campaign.getCampaignOptions().getUseShareSystem()) {
			lblSharePct.setName("lblSharePct"); // NOI18N
			lblSharePct.setText(resourceMap.getString("lblSharePct.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = y;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(lblSharePct, gridBagConstraints);

			txtSharePct.setName("txtSharePct"); // NOI18N
			txtSharePct.setText(contract.getSharesPct() + "%");
			txtSharePct.setEditable(false);
			txtSharePct.setLineWrap(true);
			txtSharePct.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = y++;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			pnlStats.add(txtSharePct, gridBagConstraints);
		}

		lblScore.setName("lblScore"); // NOI18N
		lblScore.setText(resourceMap.getString("lblScore.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(lblScore, gridBagConstraints);
		
		txtScore.setName("txtScore"); // NOI18N
		txtScore.setText(Integer.toString(contract.getScore()));
		txtScore.setEditable(false);
		txtScore.setLineWrap(true);
		txtScore.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtScore, gridBagConstraints);

		txtDesc.setName("txtDesc");
		txtDesc.setText(contract.getDescription());
		txtDesc.setEditable(false);
		txtDesc.setLineWrap(true);
		txtDesc.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 20);
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		pnlStats.add(txtDesc, gridBagConstraints);
		
    }
}
