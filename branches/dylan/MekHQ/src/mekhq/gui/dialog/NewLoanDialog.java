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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingFactory;

/**
 * @author Taharqa
 */
public class NewLoanDialog extends javax.swing.JDialog implements ActionListener, ChangeListener {
    private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Loan loan;
    private Campaign campaign;
    private DecimalFormat formatter;
    private int rating;
    private long maxCollateralValue;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel panMain;
    private JPanel panInfo;
    private JPanel panBtn;
    private JButton btnCancel;
    private JButton btnAdd;
    private JTextField txtName;
    private JTextField txtNumber;

    private JTextField txtPrincipal;
    private JButton btnPlusTenMillion;
    private JButton btnMinusTenMillion;
    private JButton btnPlusMillion;
    private JButton btnMinusMillion;
    private JButton btnPlusHundredK;
    private JButton btnMinusHundredK;
    private JButton btnPlusTenK;
    private JButton btnMinusTenK;

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
        campaign = c;
        IUnitRating unitRating = UnitRatingFactory.getUnitRating(campaign);
        unitRating.reInitialize();
        rating = unitRating.getModifier();
        loan = Loan.getBaseLoanFor(rating, campaign.getCalendar());
        maxCollateralValue = campaign.getFinances().getMaxCollateral(campaign);
        formatter = new DecimalFormat();
        initComponents();
        setLocationRelativeTo(parent);
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

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewLoanDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title"));

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
        panMain.add(new JLabel("Reference Number:"), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtNumber, gridBagConstraints);

        txtPrincipal = new javax.swing.JTextField(formatter.format(loan.getPrincipal()));
        txtPrincipal.setEditable(false);
        btnPlusTenMillion = new JButton("+10mil");
        btnMinusTenMillion = new JButton("-10mil");
        btnPlusMillion = new JButton("+1mil");
        btnMinusMillion = new JButton("-1mil");
        btnPlusHundredK = new JButton("+100K");
        btnMinusHundredK = new JButton("-100K");
        btnPlusTenK = new JButton("+10K");
        btnMinusTenK = new JButton("-10K");
        checkMinusButtons();
        btnPlusTenMillion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(10000000);
            }
        });
        btnMinusTenMillion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(-10000000);
            }
        });
        btnPlusMillion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(1000000);
            }
        });
        btnMinusMillion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(-1000000);
            }
        });
        btnPlusHundredK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(100000);
            }
        });
        btnMinusHundredK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(-100000);
            }
        });
        btnPlusTenK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(10000);
            }
        });
        btnMinusTenK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adjustPrincipal(-10000);
            }
        });

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

        DefaultComboBoxModel<String> scheduleModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < Finances.SCHEDULE_NUM; i++) {
            scheduleModel.addElement(Finances.getScheduleName(i));
        }
        choiceSchedule = new JComboBox<String>(scheduleModel);
        choiceSchedule.setSelectedIndex(loan.getPaymentSchedule());

        choiceSchedule.addActionListener(this);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(new JLabel("Principal:"), gridBagConstraints);

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
        panMain.add(new JLabel("Annual Interest:"), gridBagConstraints);

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
        panMain.add(new JLabel("Collateral:"), gridBagConstraints);

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
        panMain.add(new JLabel("Length (years):"), gridBagConstraints);

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
        panMain.add(new JLabel("Payment Schedule:"), gridBagConstraints);

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
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addLoan();
            }
        });
        panBtn.add(btnAdd);

        btnCancel = new JButton(resourceMap.getString("btnCancel.text")); // NOI18N
        btnCancel.setName("btnClose"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel();
            }
        });
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panBtn.add(btnCancel);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);

        pack();
    }

    private void setUpInfo() {
        panInfo.setLayout(new GridLayout());
        panInfo.setBorder(BorderFactory.createTitledBorder("Loan Details"));
        refreshValues();

        JPanel panLeft = new JPanel(new GridBagLayout());
        JPanel panRight = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panLeft.add(new JLabel("APR:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblAPR, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel("Collateral %:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblCollateralPct, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel("Length:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panLeft.add(lblYears, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panLeft.add(new JLabel("Schedule:"), gridBagConstraints);
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
        panRight.add(new JLabel("Principal Amount:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblPrincipal, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("First Payment Due:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblFirstPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("Installment Amount:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblPayAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("Number of Payments:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblNPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("Total Amount:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblTotalPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("Collateral Amount:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(lblCollateralAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panRight.add(new JLabel("Max Collateral Value:"), gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        panRight.add(new JLabel(formatter.format(maxCollateralValue)), gridBagConstraints);

        panInfo.add(panLeft);
        panInfo.add(panRight);
    }

    private void refreshLoan(long principal) {
    	// Modify loan settings
    	loan.setPrincipal(principal);
    	loan.setRate((int) sldInterest.getValue());
    	loan.setCollateral((int) sldCollateral.getValue());
    	loan.setYears((int) sldLength.getValue());
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
        txtPrincipal.setText(formatter.format(loan.getPrincipal()));
        lblAPR.setText(loan.getInterestRate() + "%");
        lblCollateralPct.setText(loan.getCollateralPercent() + "%");
        lblYears.setText(loan.getYears() + " years");
        lblSchedule.setText(Finances.getScheduleName(loan.getPaymentSchedule()));
        lblPrincipal.setText(formatter.format(loan.getPrincipal()));
        lblFirstPayment.setText(SimpleDateFormat.getDateInstance().format(loan.getNextPayDate()));
        lblPayAmount.setText(formatter.format(loan.getPaymentAmount()));
        lblNPayment.setText(formatter.format(loan.getRemainingPayments()));
        lblTotalPayment.setText(formatter.format(loan.getRemainingValue()));
        lblCollateralAmount.setText(formatter.format(loan.getCollateralAmount()));
    }


    private void addLoan() {
        if (maxCollateralValue < loan.getCollateralAmount()) {
            JOptionPane.showMessageDialog(frame,
                                          "The collateral amount of this loan is higher than the total value of assets",
                                          "Collateral Too High",
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

    private void adjustPrincipal(long value) {
        long newPrincipal = loan.getPrincipal() + value;
        refreshLoan(newPrincipal);
        checkMinusButtons();
    }

    private void checkMinusButtons() {
        btnMinusTenMillion.setEnabled(loan.getPrincipal() > 10000000);
        btnMinusMillion.setEnabled(loan.getPrincipal() > 1000000);
        btnMinusHundredK.setEnabled(loan.getPrincipal() > 100000);
        btnMinusTenK.setEnabled(loan.getPrincipal() > 10000);
    }

}
