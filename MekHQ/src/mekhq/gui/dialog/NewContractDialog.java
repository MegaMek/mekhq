/*
 * NewContractDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.campaign.universe.Systems;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.gui.view.ContractPaymentBreakdown;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author Taharqa
 */
public class NewContractDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    protected JFrame frame;
    protected Contract contract;
    protected Campaign campaign;
    private JComboBox<Person> cboNegotiator;

    private ContractPaymentBreakdown contractPaymentBreakdown;

    /** Creates new form NewTeamDialog */
    public NewContractDialog(JFrame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        campaign = c;
        contract = new Contract("New Contract", "New Employer");
        contract.calculateContract(campaign);
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    protected void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewContractDialog", new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));

        JPanel newContractPanel = new JPanel(new java.awt.GridBagLayout());

        JPanel descPanel = new JPanel();
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
        newContractPanel.add(descPanel, gridBagConstraints);

        JPanel contractPanel = new JPanel();
		contractPanel.setLayout(new java.awt.GridBagLayout());
		contractPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("contractPanel.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        newContractPanel.add(contractPanel, gridBagConstraints);

        JPanel totalsPanel = new JPanel();
		totalsPanel.setLayout(new java.awt.GridBagLayout());
		totalsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("totalsPanel.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        newContractPanel.add(totalsPanel, gridBagConstraints);

        initDescPanel(resourceMap, descPanel);

        initPaymentBreakdownPanel(totalsPanel);

        initContractPanel(resourceMap, contractPanel);

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        newContractPanel.add(btnOK, gridBagConstraints);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        newContractPanel.add(btnClose, gridBagConstraints);

        JScrollPane scrollPane = new JScrollPane(newContractPanel);

        getContentPane().add(scrollPane);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(NewContractDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

	protected void initDescPanel(ResourceBundle resourceMap, JPanel descPanel) {
		java.awt.GridBagConstraints gridBagConstraints;
		txtName = new javax.swing.JTextField();
        JLabel lblName = new JLabel();
        txtEmployer = new javax.swing.JTextField();
        JLabel lblEmployer = new JLabel();
        cboNegotiator = new JComboBox<>();
        txtType = new javax.swing.JTextField();
        JLabel lblType = new JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        txtDesc = new MarkdownEditorPanel("Contract Description");
        JLabel lblPlanetName = new JLabel();

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

        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text")); // NOI18N
        lblPlanetName.setName("lblPlanetName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblPlanetName, gridBagConstraints);

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        /*suggestPlanet.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				contract.setPlanetName(suggestPlanet.getText());
				//reset the start date so this can be recalculated
				contract.setStartDate(campaign.getDate());
				contract.calculateContract(campaign);
				btnDate.setText(dateFormatter.format(contract.getStartDate()));
				refreshTotals();
			}
		});*/
        suggestPlanet.addFocusListener(contractUpdateFocusListener);
        suggestPlanet.addActionListener(contractUpdateActionListener);


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(suggestPlanet, gridBagConstraints);

        lblType.setText(resourceMap.getString("lblType.text")); // NOI18N
        lblType.setName("lblType"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblType, gridBagConstraints);

        txtType.setText(contract.getType());
        txtType.setName("txtType"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtType, gridBagConstraints);

        lblEmployer.setText(resourceMap.getString("lblEmployer.text")); // NOI18N
        lblEmployer.setName("lblEmployer"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblEmployer, gridBagConstraints);

        txtEmployer.setText(contract.getEmployer());
        txtEmployer.setName("txtEmployer"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtEmployer, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(new JLabel("Negotiator"), gridBagConstraints);

        cboNegotiator.setName("cboNegotiator");
        // Add negotiators
        for (Person p : campaign.getActivePersonnel()) {
            if (p.hasSkill(SkillType.S_NEG)) {
                cboNegotiator.addItem(p);
            }
        }
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cboNegotiator, gridBagConstraints);

        txtDesc.setText(contract.getDescription());
        txtDesc.setPreferredSize(new Dimension(400, 200));
        txtDesc.setMinimumSize(new Dimension(400, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtDesc, gridBagConstraints);
	}

	protected void initPaymentBreakdownPanel(JPanel totalsPanel) {
        contractPaymentBreakdown = new ContractPaymentBreakdown(totalsPanel, contract, campaign);
        contractPaymentBreakdown.display(0, 1);
	}

	protected void initContractPanel(ResourceBundle resourceMap, JPanel contractPanel) {
		java.awt.GridBagConstraints gridBagConstraints;
		JLabel lblDate = new JLabel(resourceMap.getString("lblDate.text"));
        JLabel lblLength = new JLabel(resourceMap.getString("lblLength.text"));
        JLabel lblMultiplier = new JLabel(resourceMap.getString("lblMultiplier.text"));
        JLabel lblOverhead = new JLabel(resourceMap.getString("lblOverhead.text"));
        JLabel lblCommandRights = new JLabel(resourceMap.getString("lblCommand.text"));
        JLabel lblTransport = new JLabel(resourceMap.getString("lblTransport.text"));
        JLabel lblSalvageRights = new JLabel(resourceMap.getString("lblSalvageRights.text"));
        JLabel lblStraightSupport = new JLabel(resourceMap.getString("lblStraightSupport.text"));
        JLabel lblBattleLossComp = new JLabel(resourceMap.getString("lblBattleLossComp.text"));
        JLabel lblSignBonus = new JLabel(resourceMap.getString("lblSignBonus.text"));
        JLabel lblAdvance = new JLabel(resourceMap.getString("lblAdvance.text"));


        btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        btnDate.setName("btnDate");
        btnDate.addActionListener(evt -> changeStartDate());

        checkMRBC = new JCheckBox(resourceMap.getString("checkMRBC.text"));
        checkMRBC.setSelected(contract.payMRBCFee());
        checkMRBC.addItemListener(contractUpdateItemListener);


        checkSalvageExchange = new JCheckBox(resourceMap.getString("checkSalvageExchange.text"));
        checkSalvageExchange.setSelected(contract.isSalvageExchange());
        checkSalvageExchange.addItemListener(contractUpdateItemListener);

        spnLength = new JSpinner(new SpinnerNumberModel(contract.getLength(), 1, 120, 1));
        spnLength.addChangeListener(contractUpdateChangeListener);

        spnMultiplier = new JSpinner(new SpinnerNumberModel(contract.getMultiplier(), 0.5, 10.0, 0.1));
        spnMultiplier.addChangeListener(contractUpdateChangeListener);

        DefaultComboBoxModel<String> overheadModel = new DefaultComboBoxModel<>();
		for (int i = 0; i < Contract.OH_NUM; i++) {
			overheadModel.addElement(Contract.getOverheadCompName(i));
		}
		choiceOverhead = new JComboBox<>(overheadModel);
		choiceOverhead.setSelectedIndex(contract.getOverheadComp());
        choiceOverhead.addActionListener(contractUpdateActionListener);
        choiceOverhead.addFocusListener(contractUpdateFocusListener);

        DefaultComboBoxModel<String> commandModel = new DefaultComboBoxModel<>();
		for (int i = 0; i < Contract.COM_NUM; i++) {
			commandModel.addElement(Contract.getCommandRightsName(i));
		}
		choiceCommand = new JComboBox<>(commandModel);
		choiceCommand.setSelectedIndex(contract.getCommandRights());
		choiceCommand.addActionListener(contractUpdateActionListener);

		spnTransport = new JSpinner(new SpinnerNumberModel(contract.getTransportComp(), 0, 100, 10));
		spnTransport.addChangeListener(contractUpdateChangeListener);

		spnSalvageRights = new JSpinner(new SpinnerNumberModel(contract.getSalvagePct(), 0, 100, 10));
		spnSalvageRights.addChangeListener(contractUpdateChangeListener);

        spnStraightSupport = new JSpinner(new SpinnerNumberModel(contract.getStraightSupport(), 0, 100, 10));
        spnStraightSupport.addChangeListener(contractUpdateChangeListener);

        spnBattleLossComp = new JSpinner(new SpinnerNumberModel(contract.getBattleLossComp(), 0, 100, 10));
        spnBattleLossComp.addChangeListener(contractUpdateChangeListener);

        spnSignBonus = new JSpinner(new javax.swing.SpinnerNumberModel(contract.getSigningBonusPct(), 0, 10, 1));
        spnSignBonus.addChangeListener(contractUpdateChangeListener);
        spnAdvance = new JSpinner(new javax.swing.SpinnerNumberModel(contract.getAdvancePct(), 0, 25, 5));
        spnAdvance.addChangeListener(contractUpdateChangeListener);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(checkMRBC, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblDate, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(btnDate, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblLength, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnLength, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblMultiplier, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnMultiplier, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblCommandRights, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(choiceCommand, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblOverhead, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(choiceOverhead, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblTransport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnTransport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblSalvageRights, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnSalvageRights, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(checkSalvageExchange, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblBattleLossComp, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnBattleLossComp, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblStraightSupport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnStraightSupport, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblSignBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnSignBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(lblAdvance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        contractPanel.add(spnAdvance, gridBagConstraints);
	}

   protected void btnOKActionPerformed(ActionEvent evt) {
        if (!btnOK.equals(evt.getSource())) {
            return;
        }

        String chosenName = txtName.getText();
    	for (Mission m : campaign.getMissions()) {
    		if (m.getName().equals(chosenName)) {
    			JOptionPane.showMessageDialog(frame,
                        "There is already a mission with the name " + chosenName,
    				    "Duplicate Mission Name",
    				    JOptionPane.ERROR_MESSAGE);
    			return;
    		}
    	}

    	contract.setName(txtName.getText());
    	//contract.setPlanetName(suggestPlanet.getText());
    	contract.setEmployer(txtEmployer.getText());
    	contract.setType(txtType.getText());
    	contract.setDesc(txtDesc.getText());
    	contract.setCommandRights(choiceCommand.getSelectedIndex());
    	campaign.getFinances().credit(contract.getTotalAdvanceAmount(), Transaction.C_CONTRACT,
                "Advance monies for " + contract.getName(), campaign.getLocalDate());
    	
    	campaign.addMission(contract);

    	// Negotiator XP
    	Person negotiator = (Person) cboNegotiator.getSelectedItem();
    	if ((negotiator != null) && (campaign.getCampaignOptions().getContractNegotiationXP() > 0)) {
    	    negotiator.awardXP(campaign.getCampaignOptions().getContractNegotiationXP());
    	}

    	this.setVisible(false);
    }

    private void changeStartDate() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, contract.getStartDate());
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
        	if (campaign.getLocalDate().isAfter(dc.getDate())) {
        		JOptionPane.showMessageDialog(frame,
        			    "You cannot choose a start date before the current date.",
        			    "Invalid date",
        			    JOptionPane.ERROR_MESSAGE);
        		return;
        	}
            contract.setStartDate(dc.getDate());
            contract.calculateContract(campaign);
            btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        }
    }

    public int getContractId() {
    	return contract.getId();
    }

    /*
    private void refreshTotals() {
    	lblBaseAmount2.setText(formatter.format(contract.getBaseAmount()));
    	lblOverheadAmount2.setText("+" + formatter.format(contract.getOverheadAmount()));
    	lblSupportAmount2.setText("+" + formatter.format(contract.getSupportAmount()));
    	lblTransitAmount2.setText("+" + formatter.format(contract.getTransitAmount()));
    	lblTransportAmount2.setText("+" + formatter.format(contract.getTransportAmount()));
    	lblTotalAmount2.setText(formatter.format(contract.getTotalAmount()));
    	lblSignBonusAmount2.setText("+" + formatter.format(contract.getSigningBonusAmount()));
    	lblFeeAmount2.setText("-" + formatter.format(contract.getFeeAmount()));
    	lblTotalAmountPlus2.setText(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
    	lblAdvanceMoney2.setText(formatter.format(contract.getTotalAdvanceAmount()));
    	lblMonthlyAmount2.setText(formatter.format(contract.getMonthlyPayOut()));
    	lblProfit2.setText(formatter.format(contract.getEstimatedTotalProfit(campaign)));
    }*/

    private void btnCloseActionPerformed(ActionEvent evt) {
        if (!btnClose.equals(evt.getSource())) {
            return;
        }
    	setVisible(false);
    }

    protected javax.swing.JButton btnClose;
    protected javax.swing.JButton btnOK;
    protected javax.swing.JTextField txtName;
    protected javax.swing.JTextField txtEmployer;
    protected javax.swing.JTextField txtType;
    protected MarkdownEditorPanel txtDesc;
    protected JSuggestField suggestPlanet;

    protected javax.swing.JButton btnDate;
    protected JComboBox<String> choiceOverhead;
    protected JComboBox<String> choiceCommand;
    protected JSpinner spnLength;
    protected JSpinner spnMultiplier;
    protected JSpinner spnTransport;
    protected JSpinner spnSalvageRights;
    protected JCheckBox checkSalvageExchange;
    protected JSpinner spnStraightSupport;
    protected JSpinner spnBattleLossComp;
    protected JSpinner spnSignBonus;
    protected JSpinner spnAdvance;
    protected JCheckBox checkMRBC;

    protected FocusListener contractUpdateFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            //unused
        }

        @Override
        public void focusLost(FocusEvent e) {
            doUpdateContract(e.getSource());
        }
    };

    protected ActionListener contractUpdateActionListener = e -> doUpdateContract(e.getSource());

    protected ItemListener contractUpdateItemListener = e -> doUpdateContract(e.getSource());

    protected ChangeListener contractUpdateChangeListener = e -> doUpdateContract(e.getSource());

    protected void doUpdateContract(Object source) {
        if (suggestPlanet.equals(source)) {
            contract.setSystemId((Systems.getInstance().getSystemByName(suggestPlanet.getText(),
                    campaign.getLocalDate())).getId());
            //reset the start date as null so we recalculate travel time
            contract.setStartDate(null);
        } else if (choiceOverhead.equals(source)) {
            contract.setOverheadComp(choiceOverhead.getSelectedIndex());
        } else if (choiceCommand.equals(source)) {
            contract.setCommandRights(choiceCommand.getSelectedIndex());
        } else if (checkMRBC.equals(source)) {
            contract.setMRBCFee(checkMRBC.isSelected());
        } else if (checkSalvageExchange.equals(source)) {
            contract.setSalvageExchange(checkSalvageExchange.isSelected());
        } else if (spnLength.equals(source)) {
            contract.setLength((Integer)spnLength.getModel().getValue());
        } else if (spnMultiplier.equals(source)) {
            contract.setMultiplier((Double)spnMultiplier.getModel().getValue());
        } else if (spnTransport.equals(source)) {
            contract.setTransportComp((Integer)spnTransport.getModel().getValue());
        } else if (spnSalvageRights.equals(source)) {
            contract.setSalvagePct((Integer)spnSalvageRights.getModel().getValue());
        } else if (spnStraightSupport.equals(source)) {
            contract.setStraightSupport((Integer)spnStraightSupport.getModel().getValue());
        } else if (spnBattleLossComp.equals(source)) {
            contract.setBattleLossComp((Integer)spnBattleLossComp.getModel().getValue());
        } else if (spnSignBonus.equals(source)) {
            contract.setSigningBonusPct((Integer)spnSignBonus.getModel().getValue());
        } else if (spnAdvance.equals(source)) {
            contract.setAdvancePct((Integer)spnAdvance.getModel().getValue());
        }

        contract.calculateContract(campaign);
        contractPaymentBreakdown.refresh();
        btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(contract.getStartDate()));
    }
}
