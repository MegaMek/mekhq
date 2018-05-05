/*
 * ContractSummaryPanel.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.Planets;

/**
 * Contract summary view for ContractMarketDialog
 * 
 * @author Neoancient
 *
 */
public class ContractSummaryPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8773615661962644614L;
	private Campaign campaign;
	private Contract contract;
	private boolean allowRerolls;
	private int cmdRerolls;
	private int logRerolls;
	private int tranRerolls;

	private JPanel mainPanel;

	private JLabel lblName;
	private JTextField txtName;
	private JLabel lblLocation;
	private JTextArea txtLocation;
	private JLabel lblDistance;
	private JTextArea txtDistance;
	private JLabel lblMissionType;
	private JTextArea txtMissionType;
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
	private JLabel lblLength;
	private JTextArea txtLength;
	
	private JLabel lblCommand;
	private JTextArea txtCommand;
	private JLabel lblTransport;
	private JTextArea txtTransport;
	private JLabel lblOverhead;
	private JTextArea txtOverhead;
	private JLabel lblStraightSupport;
	private JTextArea txtStraightSupport;
	private JLabel lblBattleLossComp;
	private JLabel lblRequiredLances;
	private JTextArea txtBattleLossComp;
	private JTextArea txtRequiredLances;
	private JLabel lblSalvageRights;
	private JTextArea txtSalvageRights;

    protected JLabel lblBaseAmount2;
    protected JLabel lblOverheadAmount2;
    protected JLabel lblSupportAmount2;
    protected JLabel lblTransportAmount2;
    protected JLabel lblTotalAmount2;
    protected JLabel lblSignBonusAmount2;
    protected JLabel lblFeeAmount2;
    protected JLabel lblTotalAmountPlus2;
    protected JLabel lblAdvanceMoney2;
    protected JLabel lblMonthlyAmount2;
    protected JLabel lblProfit2;

    public ContractSummaryPanel(Contract contract, Campaign campaign,
    		boolean allowRerolls) {
		this.contract = contract;
		this.campaign = campaign;
		this.allowRerolls = allowRerolls;
		if (allowRerolls) {
			Person admin = campaign.findBestInRole(Person.T_ADMIN_COM, SkillType.S_ADMIN, SkillType.S_NEG);
			cmdRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)?
					0 : admin.getSkill(SkillType.S_NEG).getLevel();
			admin = campaign.findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN, SkillType.S_NEG);
			logRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)?
					0 : admin.getSkill(SkillType.S_NEG).getLevel();
			admin = campaign.findBestInRole(Person.T_ADMIN_TRA, SkillType.S_ADMIN, SkillType.S_NEG);
			tranRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)?
					0 : admin.getSkill(SkillType.S_NEG).getLevel();
		}
		initComponents();
	}

	private void initComponents() {
		java.awt.GridBagConstraints gbc;

		mainPanel = new JPanel();

		setLayout(new java.awt.GridBagLayout());

		setBackground(Color.WHITE);

		mainPanel.setName("pnlStats");
		mainPanel.setBorder(BorderFactory.createTitledBorder(contract.getName()));
		mainPanel.setBackground(Color.WHITE);
		fillStats();
		gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.insets = new java.awt.Insets(5, 5, 5, 20);
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;	
		add(mainPanel, gbc);

	}

	private void fillStats() {

		String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
		String[] ratingNames = {"F", "D", "C", "B", "A"};

		lblName = new JLabel();
		txtName = new JTextField();
		lblLocation = new JLabel();
		txtLocation = new JTextArea();
		lblDistance = new JLabel();
		txtDistance = new JTextArea();
		lblEmployer = new JLabel();
		txtEmployer = new JTextArea();
		lblEnemy = new JLabel();
		txtEnemy = new JTextArea();
		lblMissionType = new JLabel();
		txtMissionType = new JTextArea();
		lblStartDate = new JLabel();
		txtStartDate = new JTextArea();
		lblLength = new JLabel();
		txtLength = new JTextArea();
		lblAllyRating = new JLabel();
		txtAllyRating = new JTextArea();
		lblEnemyRating = new JLabel();
		txtEnemyRating = new JTextArea();
		
		lblOverhead = new JLabel();
		txtOverhead = new JTextArea();
		lblCommand = new JLabel();
		txtCommand = new JTextArea();
		lblTransport = new JLabel();
		txtTransport = new JTextArea();
		lblStraightSupport = new JLabel();
		txtStraightSupport = new JTextArea();
		lblBattleLossComp = new JLabel();
		txtBattleLossComp = new JTextArea();
		lblRequiredLances = new JLabel();
		txtRequiredLances = new JTextArea();
		lblSalvageRights = new JLabel();
		txtSalvageRights = new JTextArea();

		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractMarketDialog", new EncodeControl()); //$NON-NLS-1$

		java.awt.GridBagConstraints gridBagConstraints;
		mainPanel.setLayout(new java.awt.GridBagLayout());

		SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");

		int y = 0;

		lblName.setName("lblName"); // NOI18N
		lblName.setText(resourceMap.getString("lblName.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblName, gridBagConstraints);

		txtName.setName("txtName"); // NOI18N
		txtName.setText(contract.getName());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtName, gridBagConstraints);

		lblEmployer.setName("lblEmployer"); // NOI18N
		lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblEmployer, gridBagConstraints);

		txtEmployer.setName("txtEmployer"); // NOI18N
		txtEmployer.setText(contract.getEmployer());
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
		mainPanel.add(txtEmployer, gridBagConstraints);

		if (contract instanceof AtBContract) {
			lblEnemy.setName("lblEnemy"); // NOI18N
			lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = y;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(lblEnemy, gridBagConstraints);

			txtEnemy.setName("txtEnemy"); // NOI18N
			txtEnemy.setText(((AtBContract)contract).getEnemyName(campaign.getGameYear()));
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
			mainPanel.add(txtEnemy, gridBagConstraints);
		}

		lblMissionType.setName("lblMissionType"); // NOI18N
		lblMissionType.setText(resourceMap.getString("lblMissionType.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblMissionType, gridBagConstraints);

		txtMissionType.setName("txtMissionType"); // NOI18N
		txtMissionType.setText(contract.getType());
		txtMissionType.setEditable(false);
		txtMissionType.setLineWrap(true);
		txtMissionType.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtMissionType, gridBagConstraints);

		lblLocation.setName("lblLocation"); // NOI18N
		lblLocation.setText(resourceMap.getString("lblLocation.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblLocation, gridBagConstraints);

		txtLocation.setName("txtLocation"); // NOI18N
        txtLocation.setText(contract.getPlanet().getName(Utilities.getDateTimeDay(campaign.getCalendar())));
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
		mainPanel.add(txtLocation, gridBagConstraints);

		if(Planets.getInstance().getPlanets().get(contract.getPlanetId(null)) != null) {
			lblDistance.setName("lblDistance"); // NOI18N
			lblDistance.setText(resourceMap.getString("lblDistance.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = y;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(lblDistance, gridBagConstraints);

			txtDistance.setName("txtDistance"); // NOI18N
			JumpPath path = campaign.calculateJumpPath(campaign.getCurrentPlanet(), contract.getPlanet());
			int days = (int)Math.ceil((path).getTotalTime(Utilities.getDateTimeDay(contract.getStartDate()), campaign.getLocation().getTransitTime()));
			int jumps = path.getJumps();
            if (campaign.getCurrentPlanet().getId().equals(contract.getPlanetId(null))
                    && campaign.getLocation().isOnPlanet()) {
				days = 0;
				jumps = 0;
			}
			txtDistance.setText(days + "(" + jumps + ")");
			txtDistance.setEditable(false);
			txtDistance.setLineWrap(true);
			txtDistance.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = y++;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(txtDistance, gridBagConstraints);
		}

		lblAllyRating.setName("lblAllyRating"); // NOI18N
		lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblAllyRating, gridBagConstraints);

		if (contract instanceof AtBContract) {
			txtAllyRating.setName("txtAllyRating"); // NOI18N
			txtAllyRating.setText(skillNames[((AtBContract)contract).getAllySkill()] + "/" +
					ratingNames[((AtBContract)contract).getAllyQuality()]);
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
			mainPanel.add(txtAllyRating, gridBagConstraints);

			lblEnemyRating.setName("lblEnemyRating"); // NOI18N
			lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = y;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(lblEnemyRating, gridBagConstraints);

			txtEnemyRating.setName("txtEnemyRating"); // NOI18N
			txtEnemyRating.setText(skillNames[((AtBContract)contract).getEnemySkill()] + "/" +
					ratingNames[((AtBContract)contract).getEnemyQuality()]);
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
			mainPanel.add(txtEnemyRating, gridBagConstraints);
		}

		lblStartDate.setName("lblStartDate"); // NOI18N
		lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblStartDate, gridBagConstraints);

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
		mainPanel.add(txtStartDate, gridBagConstraints);

		lblLength.setName("lblLength"); // NOI18N
		lblLength.setText(resourceMap.getString("lblLength.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblLength, gridBagConstraints);

		txtLength.setName("txtLength"); // NOI18N
		txtLength.setText(Integer.toString(contract.getLength()));
		txtLength.setEditable(false);
		txtLength.setLineWrap(true);
		txtLength.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtLength, gridBagConstraints);

		lblOverhead.setName("lblOverhead"); // NOI18N
		lblOverhead.setText(resourceMap.getString("lblOverhead.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblOverhead, gridBagConstraints);

		txtOverhead.setName("txtOverhead"); // NOI18N
		txtOverhead.setText(Contract.getOverheadCompName(contract.getOverheadComp()));
		txtOverhead.setEditable(false);
		txtOverhead.setLineWrap(true);
		txtOverhead.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtOverhead, gridBagConstraints);

		lblCommand.setName("lblCommand"); // NOI18N
		lblCommand.setText(resourceMap.getString("lblCommand.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblCommand, gridBagConstraints);

		txtCommand.setName("txtCommand"); // NOI18N
		txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
		txtCommand.setEditable(false);
		txtCommand.setLineWrap(true);
		txtCommand.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtCommand, gridBagConstraints);
		
		/* Only allow command clause rerolls for merc and pirates; house units are always integrated */
		if (allowRerolls && (campaign.getFactionCode().equals("MERC") || campaign.getFactionCode().equals("PIR")) &&
				campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_COMMAND) < cmdRerolls) {
			JButton btnCommand = new JButton("Renegotiate (" +
					(cmdRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_COMMAND)) + ")");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = y;
			mainPanel.add(btnCommand, gridBagConstraints);
			btnCommand.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					JButton btn = null;
					if(ev.getSource() instanceof JButton) {
						btn = (JButton)ev.getSource();
					}
					if(null == btn) {
						return;
					}
					if (contract instanceof AtBContract) {
						campaign.getContractMarket().rerollClause((AtBContract)contract,
								ContractMarket.CLAUSE_COMMAND, campaign);
						((JButton)ev.getSource()).setText("Renegotiate (" +
								(cmdRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_COMMAND)) + ")");
						txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
						if (campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_COMMAND) >= cmdRerolls) {
							btn.setEnabled(false);
						}
						refreshAmounts();
					}
				}
			});
		}
		y++;

		lblTransport.setName("lblTransport"); // NOI18N
		lblTransport.setText(resourceMap.getString("lblTransport.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblTransport, gridBagConstraints);

		txtTransport.setName("txtTransport"); // NOI18N
		txtTransport.setText(contract.getTransportComp() + "%");
		txtTransport.setEditable(false);
		txtTransport.setLineWrap(true);
		txtTransport.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtTransport, gridBagConstraints);

		if (allowRerolls && campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_TRANSPORT) < tranRerolls) {
			JButton btnTransport = new JButton("Renegotiate (" +
					(tranRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_TRANSPORT)) + ")");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = y;
			mainPanel.add(btnTransport, gridBagConstraints);
			btnTransport.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					JButton btn = null;
					if(ev.getSource() instanceof JButton) {
						btn = (JButton)ev.getSource();
					}
					if(null == btn) {
						return;
					}
					if (contract instanceof AtBContract) {
						campaign.getContractMarket().rerollClause((AtBContract)contract,
								ContractMarket.CLAUSE_TRANSPORT, campaign);
						((JButton)ev.getSource()).setText("Renegotiate (" +
								(tranRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_TRANSPORT)) + ")");
						txtTransport.setText(contract.getTransportComp() + "%");
						if (campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_TRANSPORT) >= tranRerolls) {
							btn.setEnabled(false);
						}
						refreshAmounts();
					}
				}
			});
		}
		y++;

		lblSalvageRights.setName("lblSalvageRights"); // NOI18N
		lblSalvageRights.setText(resourceMap.getString("lblSalvageRights.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblSalvageRights, gridBagConstraints);

		txtSalvageRights.setName("txtSalvageRights"); // NOI18N
		txtSalvageRights.setText(contract.getSalvagePct() + "%" +
				(contract.isSalvageExchange()?" (Exchange)": ""));
		txtSalvageRights.setEditable(false);
		txtSalvageRights.setLineWrap(true);
		txtSalvageRights.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtSalvageRights, gridBagConstraints);

		lblStraightSupport.setName("lblStraightSupport"); // NOI18N
		lblStraightSupport.setText(resourceMap.getString("lblStraightSupport.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblStraightSupport, gridBagConstraints);

		txtStraightSupport.setName("txtStraightSupport"); // NOI18N
		txtStraightSupport.setText(contract.getStraightSupport() + "%");
		txtStraightSupport.setEditable(false);
		txtStraightSupport.setLineWrap(true);
		txtStraightSupport.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtStraightSupport, gridBagConstraints);

		if (allowRerolls && campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_SUPPORT) < logRerolls) {
			JButton btnSupport = new JButton("Renegotiate (" +
					(logRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_SUPPORT)) + ")");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = y;
			mainPanel.add(btnSupport, gridBagConstraints);
			btnSupport.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					JButton btn = null;
					if(ev.getSource() instanceof JButton) {
						btn = (JButton)ev.getSource();
					}
					if(null == btn) {
						return;
					}
					if (contract instanceof AtBContract) {
						campaign.getContractMarket().rerollClause((AtBContract)contract,
								ContractMarket.CLAUSE_SUPPORT, campaign);
						((JButton)ev.getSource()).setText("Renegotiate (" +
								(logRerolls - campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_SUPPORT)) + ")");
						txtStraightSupport.setText(contract.getStraightSupport() + "%");
						txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
						if (campaign.getContractMarket().getRerollsUsed(contract, ContractMarket.CLAUSE_SUPPORT) >= logRerolls) {
							btn.setEnabled(false);
						}
						refreshAmounts();
					}
				}
			});
		}
		y++;

		lblBattleLossComp.setName("lblBattleLossComp"); // NOI18N
		lblBattleLossComp.setText(resourceMap.getString("lblBattleLossComp.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(lblBattleLossComp, gridBagConstraints);

		txtBattleLossComp.setName("txtBattleLossComp"); // NOI18N
		txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
		txtBattleLossComp.setEditable(false);
		txtBattleLossComp.setLineWrap(true);
		txtBattleLossComp.setWrapStyleWord(true);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.weightx = 0.5;
		gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		mainPanel.add(txtBattleLossComp, gridBagConstraints);
		
		if(contract instanceof AtBContract) {
			lblRequiredLances.setName("lblRequiredLances"); // NOI18N
			lblRequiredLances.setText(resourceMap.getString("lblRequiredLances.text"));
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = y;
			gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(lblRequiredLances, gridBagConstraints);
			
			txtRequiredLances.setName("txtRequiredLances"); // NOI18N
			txtRequiredLances.setText(((AtBContract)contract).getRequiredLances() + " Lance(s)");
			txtRequiredLances.setEditable(false);
			txtRequiredLances.setLineWrap(true);
			txtRequiredLances.setWrapStyleWord(true);
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = y++;
			gridBagConstraints.weightx = 0.5;
			gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
			mainPanel.add(txtRequiredLances, gridBagConstraints);
		}

		DecimalFormat formatter = new DecimalFormat();

		JLabel lblBaseAmount1 = new JLabel(resourceMap.getString("lblBaseAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblBaseAmount1, gridBagConstraints);
		lblBaseAmount2 = new JLabel(formatter.format(contract.getBaseAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblBaseAmount2, gridBagConstraints);

		JLabel lblOverheadAmount1 = new JLabel(resourceMap.getString("lblOverheadAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblOverheadAmount1, gridBagConstraints);
		lblOverheadAmount2 = new JLabel("+" + formatter.format(contract.getOverheadAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblOverheadAmount2, gridBagConstraints);

		JLabel lblSupportAmount1 = new JLabel(resourceMap.getString("lblSupportAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblSupportAmount1, gridBagConstraints);
		lblSupportAmount2 = new JLabel("+" + formatter.format(contract.getSupportAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblSupportAmount2, gridBagConstraints);

		JLabel lblTransportAmount1 = new JLabel(resourceMap.getString("lblTransportAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTransportAmount1, gridBagConstraints);
		lblTransportAmount2 = new JLabel("+" + formatter.format(contract.getTransportAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTransportAmount2, gridBagConstraints);

		JLabel lblTotalAmount1 = new JLabel(resourceMap.getString("lblTotalAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTotalAmount1, gridBagConstraints);
		lblTotalAmount2 = new JLabel(formatter.format(contract.getTotalAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTotalAmount2, gridBagConstraints);

		JLabel lblSignBonusAmount1 = new JLabel(resourceMap.getString("lblSignBonusAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblSignBonusAmount1, gridBagConstraints);
		lblSignBonusAmount2 = new JLabel("+" + formatter.format(contract.getSigningBonusAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblSignBonusAmount2, gridBagConstraints);

		JLabel lblFeeAmount1 = new JLabel(resourceMap.getString("lblFeeAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblFeeAmount1, gridBagConstraints);
		lblFeeAmount2 = new JLabel("-" + formatter.format(contract.getFeeAmount()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblFeeAmount2, gridBagConstraints);

		JLabel lblTotalAmountPlus1 = new JLabel(resourceMap.getString("lblTotalAmountPlus1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTotalAmountPlus1, gridBagConstraints);
		lblTotalAmountPlus2 = new JLabel(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblTotalAmountPlus2, gridBagConstraints);

		JLabel lblAdvanceMoney1 = new JLabel(resourceMap.getString("lblAdvanceMoney1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblAdvanceMoney1, gridBagConstraints);
		lblAdvanceMoney2 = new JLabel(formatter.format(contract.getTotalAdvanceMonies()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblAdvanceMoney2, gridBagConstraints);

		JLabel lblMonthlyAmount1 = new JLabel(resourceMap.getString("lblMonthlyAmount1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;       
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblMonthlyAmount1, gridBagConstraints);
		lblMonthlyAmount2 = new JLabel(formatter.format(contract.getMonthlyPayOut()));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblMonthlyAmount2, gridBagConstraints);

		JLabel lblProfit1 = new JLabel(resourceMap.getString("lblProfit1.text"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = y;
		gridBagConstraints.gridwidth = 1;       
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblProfit1, gridBagConstraints);
		lblProfit2 = new JLabel(formatter.format(contract.getEstimatedTotalProfit(campaign)));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = y++;
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
		mainPanel.add(lblProfit2, gridBagConstraints);

	}

	public void refreshAmounts() {
		DecimalFormat formatter = new DecimalFormat();

		lblBaseAmount2.setText(formatter.format(contract.getBaseAmount()));
		lblOverheadAmount2.setText("+" + formatter.format(contract.getOverheadAmount()));
		lblSupportAmount2.setText("+" + formatter.format(contract.getSupportAmount()));
		lblTransportAmount2.setText("+" + formatter.format(contract.getTransportAmount()));
		lblTotalAmount2.setText(formatter.format(contract.getTotalAmount()));
		lblSignBonusAmount2.setText("+" + formatter.format(contract.getSigningBonusAmount()));
		lblFeeAmount2.setText("-" + formatter.format(contract.getFeeAmount()));
		lblTotalAmountPlus2.setText(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
		lblAdvanceMoney2.setText(formatter.format(contract.getTotalAdvanceMonies()));
		lblMonthlyAmount2.setText(formatter.format(contract.getMonthlyPayOut()));
		lblProfit2.setText(formatter.format(contract.getEstimatedTotalProfit(campaign)));
	}

	public String getContractName() {
		return txtName.getText();
	}
}
