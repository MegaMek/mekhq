/*
 * NewLoanDialog.java
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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.rating.IUnitRating;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * @author Taharqa
 */
public class NewLoanDialog extends javax.swing.JDialog implements ActionListener, ChangeListener {
    private static final long serialVersionUID = -8038099101234445018L;

    private ResourceBundle resourceMap;
    private NumberFormatter numberFormatter;
    private Frame frame;
    private Loan loan;
    private Campaign campaign;
    private int rating;
    private Money maxCollateralValue;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel panMain;
    private JPanel panInfo;
    private JPanel panBtn;
    private JButton btnCancel;
    private JButton btnAdd;
    private JTextField txtName;
    private JTextField txtNumber;

    private JButton btnPlusTenMillion;
    private JButton btnMinusTenMillion;
    private JButton btnPlusMillion;
    private JButton btnMinusMillion;
    private JButton btnPlusHundredK;
    private JButton btnMinusHundredK;
    private JButton btnPlusTenK;
    private JButton btnMinusTenK;

    private JFormattedTextField txtPrincipal;
    private JSlider sldInterest;
    private JSlider sldCollateral;
    private JSlider sldLength;
    private JComboBox<String> choiceSchedule;

    private JLabel lblAPR;
    private JLabel lblCollateralPct;
    private JLabel lblYears;
    private JLabel lblSchedule;

    private JLabel lblPrincipal;
    private JLabel lblFirstPayment;
    private JLabel lblPayAmount;
    private JLabel lblNPayment;
    private JLabel lblTotalPayment;
    private JLabel lblCollateralAmount;

    /**
     * Creates new form NewLoanDialog
     */
    public NewLoanDialog(java.awt.Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        this.campaign = c;
        this.numberFormatter = new NumberFormatter(NumberFormat.getInstance());
        IUnitRating unitRating = c.getUnitRating();
        rating = unitRating.getModifier();
        loan = Loan.getBaseLoanFor(rating, campaign.getCalendar());
        maxCollateralValue = campaign.getFinances().getMaxCollateral(campaign);
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panMain = new javax.swing.JPanel();
        panInfo = new javax.swing.JPanel();
        panBtn = new javax.swing.JPanel();
        lblAPR = new JLabel();
        lblCollateralPct = new JLabel();
        lblYears = new JLabel();
        lblSchedule = new JLabel();
        lblPrincipal = new JLabel();
        lblFirstPayment = new JLabel();
        lblPayAmount = new JLabel();
        lblNPayment = new JLabel();
        lblTotalPayment = new JLabel();
        lblCollateralAmount = new JLabel();

        resourceMap = ResourceBundle.getBundle("mekhq.resources.NewLoanDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0, 2));

        txtName = new javax.swing.JTextField(loan.getInstitution());
        txtName.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changeInstitution();
            }

            public void removeUpdate(DocumentEvent e) {
                changeInstitution();
            }

            public void insertUpdate(DocumentEvent e) {
                changeInstitution();
            }

            public void changeInstitution() {
                loan.setInstitution(txtName.getText());
            }
        });
        txtNumber = new javax.swing.JTextField(loan.getRefNumber());
        txtNumber.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            public void removeUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            public void insertUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            public void changeRefNumber() {
                loan.setRefNumber(txtNumber.getText());
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblName.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtName, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblReference.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtNumber, gridBagConstraints);

        txtPrincipal = new javax.swing.JFormattedTextField();
        txtPrincipal.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
        txtPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
        txtPrincipal.setEditable(false);
        btnPlusTenMillion = new JButton(resourceMap.getString("btnPlus10mil.text"));
        btnMinusTenMillion = new JButton(resourceMap.getString("btnMinus10mil.text"));
        btnPlusMillion = new JButton(resourceMap.getString("btnPlus1mil.text"));
        btnMinusMillion = new JButton(resourceMap.getString("btnMinus1mil.text"));
        btnPlusHundredK = new JButton(resourceMap.getString("btnPlus100k.text"));
        btnMinusHundredK = new JButton(resourceMap.getString("btnMinus100k.text"));
        btnPlusTenK = new JButton(resourceMap.getString("btnPlus10k.text"));
        btnMinusTenK = new JButton(resourceMap.getString("btnMinus10k.text"));
        checkMinusButtons();
        btnPlusTenMillion.addActionListener(evt -> adjustPrincipal(Money.of(10000000)));
        btnMinusTenMillion.addActionListener(evt -> adjustPrincipal(Money.of(-10000000)));
        btnPlusMillion.addActionListener(evt -> adjustPrincipal(Money.of(1000000)));
        btnMinusMillion.addActionListener(evt -> adjustPrincipal(Money.of(-1000000)));
        btnPlusHundredK.addActionListener(evt -> adjustPrincipal(Money.of(100000)));
        btnMinusHundredK.addActionListener(evt -> adjustPrincipal(Money.of(-100000)));
        btnPlusTenK.addActionListener(evt -> adjustPrincipal(Money.of(10000)));
        btnMinusTenK.addActionListener(evt -> adjustPrincipal(Money.of(-10000)));

        JPanel plusPanel = new JPanel(new GridLayout(2, 4));
        plusPanel.add(btnPlusTenMillion);
        plusPanel.add(btnPlusMillion);
        plusPanel.add(btnPlusHundredK);
        plusPanel.add(btnPlusTenK);
        plusPanel.add(btnMinusTenMillion);
        plusPanel.add(btnMinusMillion);
        plusPanel.add(btnMinusHundredK);
        plusPanel.add(btnMinusTenK);

        setSliders();
        sldInterest.addChangeListener(this);
        sldCollateral.addChangeListener(this);
        sldLength.addChangeListener(this);

        DefaultComboBoxModel<String> scheduleModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < Finances.SCHEDULE_NUM; i++) {
            scheduleModel.addElement(Finances.getScheduleName(i));
        }
        choiceSchedule = new JComboBox<>(scheduleModel);
        choiceSchedule.setSelectedIndex(loan.getPaymentSchedule());

        choiceSchedule.addActionListener(this);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblPrincipal.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtPrincipal, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(plusPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblAnnualInterest.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(sldInterest, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblCollateral.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(sldCollateral, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblLengthYears.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(sldLength, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblPaymentSchedule.text")), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(choiceSchedule, gridBagConstraints);

        setUpInfo();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(panInfo, gridBagConstraints);

        btnAdd = new JButton(resourceMap.getString("btnOkay.text")); // NOI18N
        btnAdd.setName("btnOK"); // NOI18N
        btnAdd.addActionListener(evt -> addLoan());
        panBtn.add(btnAdd);

        btnCancel = new JButton(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnClose"); // NOI18N
        btnCancel.addActionListener(evt -> cancel());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panBtn.add(btnCancel);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(NewLoanDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void setUpInfo() {
        panInfo.setLayout(new GridLayout());
        panInfo.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("detailsTitle.text")));
        refreshValues();

        JPanel panLeft = new JPanel(new GridBagLayout());
        JPanel panRight = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panLeft.add(new JLabel(resourceMap.getString("lblAPR.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblAPR, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblCollateralPct.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblCollateralPct, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblLength.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblYears, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblSchedule.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblSchedule, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panRight.add(new JLabel(resourceMap.getString("lblPrincipalAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblPrincipal, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblFirstPayment.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblFirstPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblInstallmentAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblPayAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblNumberPayments.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblNPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblTotalAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblTotalPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblCollateralAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblCollateralAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblMaxCollateral.text")), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(new JLabel(maxCollateralValue.toAmountAndSymbolString()), gridBagConstraints);

        panInfo.add(panLeft);
        panInfo.add(panRight);
    }

    private void refreshLoan(Money principal) {
    	// Modify loan settings
    	loan.setPrincipal(principal);
    	loan.setRate(sldInterest.getValue());
    	loan.setCollateral(sldCollateral.getValue());
    	loan.setYears(sldLength.getValue());
    	loan.setSchedule(choiceSchedule.getSelectedIndex());
    	loan.setInstitution(txtName.getText());
    	loan.setRefNumber(txtNumber.getText());
    	
    	// Recalculate information based on settings
    	loan.setNextPayment((GregorianCalendar)campaign.getCalendar().clone());
    	loan.setFirstPaymentDate();
    	loan.calculateAmortization();
    	
    	// Refresh dialog values
        refreshValues();
    }

    private void refreshValues() {
        final String METHOD_NAME = "refreshValues";
        try {
            txtPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
            lblAPR.setText(loan.getInterestRate() + "%");
            lblCollateralPct.setText(loan.getCollateralPercent() + "%");
            lblYears.setText(loan.getYears() + " years");
            lblSchedule.setText(Finances.getScheduleName(loan.getPaymentSchedule()));
            lblPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
            lblFirstPayment.setText(SimpleDateFormat.getDateInstance().format(loan.getNextPayDate()));
            lblPayAmount.setText(loan.getPaymentAmount().toAmountAndSymbolString());
            lblNPayment.setText(numberFormatter.valueToString(loan.getRemainingPayments()));
            lblTotalPayment.setText(loan.getRemainingValue().toAmountAndSymbolString());
            lblCollateralAmount.setText(loan.getCollateralAmount().toAmountAndSymbolString());
        } catch (Exception ex ){
            MekHQ.getLogger().error(NewLoanDialog.class, METHOD_NAME, ex);
        }
    }

    private void addLoan() {
        if (maxCollateralValue.isLessThan(loan.getCollateralAmount())) {
            JOptionPane.showMessageDialog(
                    frame,
                    resourceMap.getString("addLoanErrorMessage.text"),
                    resourceMap.getString("addLoanErrorTitle.text"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        campaign.addLoan(loan);
        this.setVisible(false);
    }

    private void cancel() {
        this.setVisible(false);
    }

    private void setSliders() {
        if (campaign.getCampaignOptions().useLoanLimits()) {
            int[] interest = Loan.getInterestBracket(rating);
            sldInterest = new JSlider(interest[0], interest[2], loan.getInterestRate());
            if (interest[2] - interest[0] > 30) {
                sldInterest.setMajorTickSpacing(10);
            } else {
                sldInterest.setMajorTickSpacing(5);
            }
            sldInterest.setPaintTicks(true);
            sldInterest.setPaintLabels(true);

            int[] collateral = Loan.getCollateralBracket(rating);
            sldCollateral = new JSlider(collateral[0], collateral[2], loan.getCollateralPercent());
            if (collateral[2] - collateral[0] > 50) {
                sldCollateral.setMajorTickSpacing(20);
            } else {
                sldCollateral.setMajorTickSpacing(10);
            }
            sldCollateral.setPaintTicks(true);
            sldCollateral.setPaintLabels(true);

            sldLength = new JSlider(1, Loan.getMaxYears(rating), loan.getYears());
            sldLength.setMajorTickSpacing(1);
            sldLength.setPaintTicks(true);
            sldLength.setPaintLabels(true);
        } else {
            sldInterest = new JSlider(0, 100, loan.getInterestRate());
            sldInterest.setMajorTickSpacing(10);
            sldInterest.setPaintTicks(true);
            sldInterest.setPaintLabels(true);

            sldCollateral = new JSlider(0, 300, loan.getCollateralPercent());
            sldCollateral.setMajorTickSpacing(50);
            sldCollateral.setPaintTicks(true);
            sldCollateral.setPaintLabels(true);

            sldLength = new JSlider(1, 10, loan.getYears());
            sldLength.setMajorTickSpacing(1);
            sldLength.setPaintTicks(true);
            sldLength.setPaintLabels(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refreshLoan(loan.getPrincipal());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (campaign.getCampaignOptions().useLoanLimits()) {
            if (e.getSource() == sldInterest) {
                sldCollateral.removeChangeListener(this);
                sldCollateral.setValue(Loan.recalculateCollateralFromInterest(sldInterest.getValue(), rating));
                sldCollateral.addChangeListener(this);
            } else if (e.getSource() == sldCollateral) {
                sldInterest.removeChangeListener(this);
                sldInterest.setValue(Loan.recalculateInterestFromCollateral(sldCollateral.getValue(), rating));
                sldInterest.addChangeListener(this);
            }
        }
        refreshLoan(loan.getPrincipal());
    }

    private void adjustPrincipal(Money value) {
        Money newPrincipal = loan.getPrincipal().plus(value);
        refreshLoan(newPrincipal);
        checkMinusButtons();
    }

    private void checkMinusButtons() {
        btnMinusTenMillion.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(10000000)));
        btnMinusMillion.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(1000000)));
        btnMinusHundredK.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(100000)));
        btnMinusTenK.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(10000)));
    }
}
