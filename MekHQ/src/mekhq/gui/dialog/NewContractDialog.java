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

package mekhq.gui.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.utilities.JSuggestField;

/**
 *
 * @author  Taharqa
 */
public class NewContractDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    protected Frame frame;
    protected Contract contract;
    protected Campaign campaign;
    protected DecimalFormat formatter;
    protected SimpleDateFormat dateFormatter;
    private JComboBox<Person> cboNegotiator;

    
    /** Creates new form NewTeamDialog */
    public NewContractDialog(java.awt.Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.frame = parent;  
        campaign = c;
        contract = new Contract("New Contract", "New Employer");
        contract.calculateContract(campaign);
        formatter = new DecimalFormat();
        dateFormatter = new SimpleDateFormat("EEEE, MMMM d yyyy");
        initComponents();
        setLocationRelativeTo(parent);
    }

    protected void initComponents() {
    	java.awt.GridBagConstraints gridBagConstraints;

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewContractDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        
        getContentPane().setLayout(new java.awt.GridBagLayout());

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
        getContentPane().add(descPanel, gridBagConstraints);

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
        getContentPane().add(contractPanel, gridBagConstraints);

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
        getContentPane().add(totalsPanel, gridBagConstraints);
        
        initDescPanel(resourceMap, descPanel);

        initTotalsPanel(resourceMap, totalsPanel);

        initContractPanel(resourceMap, contractPanel);
        
     
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(btnClose, gridBagConstraints);

        pack();
    }

	protected void initDescPanel(ResourceBundle resourceMap, JPanel descPanel) {
		java.awt.GridBagConstraints gridBagConstraints;
		txtName = new javax.swing.JTextField();
        JLabel lblName = new JLabel();
        txtEmployer = new javax.swing.JTextField();
        JLabel lblEmployer = new JLabel();
        cboNegotiator = new JComboBox<Person>();
        txtType = new javax.swing.JTextField();
        JLabel lblType = new JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        JScrollPane scrDesc = new JScrollPane();
        txtDesc = new javax.swing.JTextArea();
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
        
        suggestPlanet = new JSuggestField(this, campaign.getPlanetNames());
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
        for (Person p : campaign.getPersonnel()) {
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
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(true);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setBorder(BorderFactory.createCompoundBorder(
	   			 BorderFactory.createTitledBorder(resourceMap.getString("txtDesc.title")),
	   			 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrDesc.setViewportView(txtDesc);
        scrDesc.setPreferredSize(new Dimension(400, 200));
        scrDesc.setMinimumSize(new Dimension(400, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        descPanel.add(scrDesc, gridBagConstraints);
	}
    
	protected void initTotalsPanel(ResourceBundle resourceMap,
			JPanel totalsPanel) {
		java.awt.GridBagConstraints gridBagConstraints;
		JLabel lblBaseAmount1 = new JLabel(resourceMap.getString("lblBaseAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblBaseAmount1, gridBagConstraints);
        lblBaseAmount2 = new JLabel(formatter.format(contract.getBaseAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblBaseAmount2, gridBagConstraints);
        lblBaseAmount2.addFocusListener(contractUpdateFocusListener);

        JLabel lblOverheadAmount1 = new JLabel(resourceMap.getString("lblOverheadAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblOverheadAmount1, gridBagConstraints);
        lblOverheadAmount2 = new JLabel("+" + formatter.format(contract.getOverheadAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblOverheadAmount2, gridBagConstraints);
        lblOverheadAmount2.addFocusListener(contractUpdateFocusListener);

        JLabel lblSupportAmount1 = new JLabel(resourceMap.getString("lblSupportAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblSupportAmount1, gridBagConstraints);
        lblSupportAmount2 = new JLabel("+" + formatter.format(contract.getSupportAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblSupportAmount2, gridBagConstraints);
        lblSupportAmount2.addFocusListener(contractUpdateFocusListener);

        JLabel lblTransitAmount1 = new JLabel(resourceMap.getString("lblTransitAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTransitAmount1, gridBagConstraints);
        lblTransitAmount2 = new JLabel("+" + formatter.format(contract.getTransitAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTransitAmount2, gridBagConstraints);
        lblTransitAmount2.addFocusListener(contractUpdateFocusListener);

        JLabel lblTransportAmount1 = new JLabel(resourceMap.getString("lblTransportAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTransportAmount1, gridBagConstraints);
        lblTransportAmount2 = new JLabel("+" + formatter.format(contract.getTransportAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTransportAmount2, gridBagConstraints);
        lblTransportAmount2.addFocusListener(contractUpdateFocusListener);

        JLabel lblTotalAmount1 = new JLabel(resourceMap.getString("lblTotalAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTotalAmount1, gridBagConstraints);
        lblTotalAmount2 = new JLabel(formatter.format(contract.getTotalAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTotalAmount2, gridBagConstraints);

        JLabel lblSignBonusAmount1 = new JLabel(resourceMap.getString("lblSignBonusAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblSignBonusAmount1, gridBagConstraints);
        lblSignBonusAmount2 = new JLabel("+" + formatter.format(contract.getSigningBonusAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblSignBonusAmount2, gridBagConstraints);

        JLabel lblFeeAmount1 = new JLabel(resourceMap.getString("lblFeeAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblFeeAmount1, gridBagConstraints);
        lblFeeAmount2 = new JLabel("-" + formatter.format(contract.getFeeAmount()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblFeeAmount2, gridBagConstraints);

        JLabel lblTotalAmountPlus1 = new JLabel(resourceMap.getString("lblTotalAmountPlus1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTotalAmountPlus1, gridBagConstraints);
        lblTotalAmountPlus2 = new JLabel(formatter.format(contract.getTotalAmountPlusFeesAndBonuses()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblTotalAmountPlus2, gridBagConstraints);

        JLabel lblAdvanceMoney1 = new JLabel(resourceMap.getString("lblAdvanceMoney1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblAdvanceMoney1, gridBagConstraints);
        lblAdvanceMoney2 = new JLabel(formatter.format(contract.getTotalAdvanceMonies()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblAdvanceMoney2, gridBagConstraints);

        JLabel lblMonthlyAmount1 = new JLabel(resourceMap.getString("lblMonthlyAmount1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 1;       
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblMonthlyAmount1, gridBagConstraints);
        lblMonthlyAmount2 = new JLabel(formatter.format(contract.getMonthlyPayOut()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblMonthlyAmount2, gridBagConstraints);

        JLabel lblProfit1 = new JLabel(resourceMap.getString("lblProfit1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;       
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblProfit1, gridBagConstraints);
        lblProfit2 = new JLabel(formatter.format(contract.getEstimatedTotalProfit(campaign)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        totalsPanel.add(lblProfit2, gridBagConstraints);
	}

	protected void initContractPanel(ResourceBundle resourceMap,
			JPanel contractPanel) {
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
        
        
        btnDate = new javax.swing.JButton();
        btnDate.setText(dateFormatter.format(contract.getStartDate()));
        //btnDate.setMinimumSize(new java.awt.Dimension(400, 30));
        btnDate.setName("btnDate"); // NOI18N
        //btnDate.setPreferredSize(new java.awt.Dimension(400, 30));
        btnDate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeStartDate();
            }
        });

        checkMRBC = new JCheckBox(resourceMap.getString("checkMRBC.text"));
        checkMRBC.setSelected(contract.payMRBCFee());
        checkMRBC.addItemListener(contractUpdateItemListener);
        

        checkSalvageExchange = new JCheckBox(resourceMap.getString("checkSalvageExchange.text"));
        checkSalvageExchange.setSelected(contract.isSalvageExchange());
        checkSalvageExchange.addItemListener(contractUpdateItemListener);
        
        spnLength = new JSpinner(new SpinnerNumberModel(contract.getLength(), 1, 120, 1));
        spnLength.addChangeListener(contractUpdateChangeListener);
        
        spnMultiplier = new JSpinner(new SpinnerNumberModel(contract.getMultiplier(), 1.0, 10.0, 0.1));
        spnMultiplier.addChangeListener(contractUpdateChangeListener);
        
        DefaultComboBoxModel<String> overheadModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i < Contract.OH_NUM; i++) {
			overheadModel.addElement(Contract.getOverheadCompName(i));
		}
		choiceOverhead = new JComboBox<String>(overheadModel);
		choiceOverhead.setSelectedIndex(contract.getOverheadComp());
        choiceOverhead.addActionListener(contractUpdateActionListener);
        choiceOverhead.addFocusListener(contractUpdateFocusListener);
        
        DefaultComboBoxModel<String> commandModel = new DefaultComboBoxModel<String>();
		for (int i = 0; i < Contract.COM_NUM; i++) {
			commandModel.addElement(Contract.getCommandRightsName(i));
		}
		choiceCommand = new JComboBox<String>(commandModel);
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

   protected void btnOKActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
        if (!btnOK.equals(evt.getSource())) {
            return;
        }
        
        String chosenName = txtName.getText();
    	for(Mission m : campaign.getMissions()) {
    		if(m.getName().equals(chosenName)) {
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
    	campaign.getFinances().credit(contract.getTotalAdvanceMonies(), Transaction.C_CONTRACT, "Advance monies for " + contract.getName(), campaign.getCalendar().getTime());
    	campaign.addMission(contract);
    	
    	// Negotiator XP
    	Person negotiator = (Person)cboNegotiator.getSelectedItem();
    	if (negotiator != null && campaign.getCampaignOptions().getContractNegotiationXP() > 0) {
    	    negotiator.awardXP(campaign.getCampaignOptions().getContractNegotiationXP());
    	}
    	
    	this.setVisible(false);
    }
    
    private void changeStartDate() {
        // show the date chooser
    	GregorianCalendar cal = new GregorianCalendar();
    	cal.setTime(contract.getStartDate());
        DateChooser dc = new DateChooser(frame, cal);
        // user can eiter choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
        	if(campaign.getCalendar().getTime().after(dc.getDate().getTime())) {
        		JOptionPane.showMessageDialog(frame,
        			    "You cannot choose a start date before the current date.",
        			    "Invalid date",
        			    JOptionPane.ERROR_MESSAGE);
        		return;
        	}
            contract.setStartDate(dc.getDate().getTime());
            contract.calculateContract(campaign);
            btnDate.setText(dateFormatter.format(contract.getStartDate()));
        }
    }
    
    public int getContractId() {
    	return contract.getId();
    }
    
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
    	lblAdvanceMoney2.setText(formatter.format(contract.getTotalAdvanceMonies()));
    	lblMonthlyAmount2.setText(formatter.format(contract.getMonthlyPayOut()));
    	lblProfit2.setText(formatter.format(contract.getEstimatedTotalProfit(campaign)));
    }

    private void btnCloseActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
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
    protected javax.swing.JTextArea txtDesc;
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

    protected javax.swing.JLabel lblBaseAmount2;
    protected javax.swing.JLabel lblOverheadAmount2;
    protected javax.swing.JLabel lblSupportAmount2;
    protected javax.swing.JLabel lblTransitAmount2;
    protected javax.swing.JLabel lblTransportAmount2;
    protected javax.swing.JLabel lblTotalAmount2;
    protected javax.swing.JLabel lblSignBonusAmount2;
    protected javax.swing.JLabel lblFeeAmount2;
    protected javax.swing.JLabel lblTotalAmountPlus2;
    protected javax.swing.JLabel lblAdvanceMoney2;
    protected javax.swing.JLabel lblMonthlyAmount2;
    protected javax.swing.JLabel lblProfit2;

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

    protected ActionListener contractUpdateActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            doUpdateContract(e.getSource());
        }
    };

    protected ItemListener contractUpdateItemListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            doUpdateContract(e.getSource());
        }
    };

    protected ChangeListener contractUpdateChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            doUpdateContract(e.getSource());
        }
    };

    protected void doUpdateContract(Object source) {
        if (suggestPlanet.equals(source)) {
            contract.setPlanetName(suggestPlanet.getText());
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
        refreshTotals();
        btnDate.setText(dateFormatter.format(contract.getStartDate()));
    }

    // End of variables declaration//GEN-END:variables

}
