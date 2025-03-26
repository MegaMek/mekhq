/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package mekhq.gui.dialog;

import static mekhq.campaign.randomEvents.GrayMonday.isGrayMonday;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialTerm;

/**
 * @author Taharqa
 */
public class NewLoanDialog extends JDialog implements ActionListener, ChangeListener {
    private static final MMLogger logger = MMLogger.create(NewLoanDialog.class);

    private NumberFormatter numberFormatter;
    private JFrame          frame;
    private Loan            loan;
    private Campaign        campaign;
    private int             rating;
    private Money           maxCollateralValue;

    private JPanel     panMain;
    private JPanel     panInfo;
    private JPanel     panBtn;
    private JButton    btnCancel;
    private JButton    btnAdd;
    private JTextField txtName;
    private JTextField txtNumber;

    private JButton btnPlusHundredMillion;
    private JButton btnMinusHundredMillion;
    private JButton btnPlusTenMillion;
    private JButton btnMinusTenMillion;
    private JButton btnPlusMillion;
    private JButton btnMinusMillion;
    private JButton btnPlusHundredK;
    private JButton btnMinusHundredK;
    private JButton btnPlusTenK;
    private JButton btnMinusTenK;

    private JFormattedTextField       txtPrincipal;
    private JSlider                   sldInterest;
    private JSlider                   sldCollateral;
    private JSlider                   sldLength;
    private MMComboBox<FinancialTerm> choiceSchedule;

    private JLabel lblAPR;
    private JLabel lblCollateralPct;
    private JLabel lblYears;
    private JLabel lblSchedule;

    private                 JLabel         lblPrincipal;
    private                 JLabel         lblFirstPayment;
    private                 JLabel         lblPayAmount;
    private                 JLabel         lblNPayment;
    private                 JLabel         lblTotalPayment;
    private                 JLabel         lblCollateralAmount;
    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewLoanDialog",
          MekHQ.getMHQOptions().getLocale());

    public NewLoanDialog(final JFrame frame, final boolean modal, final Campaign campaign) {
        super(frame, modal);
        this.frame           = frame;
        this.campaign        = campaign;
        this.numberFormatter = new NumberFormatter(NumberFormat.getInstance());

        rating             = campaign.getAtBUnitRatingMod();
        loan               = Loan.getBaseLoan(rating,
              this.campaign.getCampaignOptions().isSimulateGrayMonday(),
              this.campaign.getLocalDate());
        maxCollateralValue = this.campaign.getFinances().getMaxCollateral(this.campaign);
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {

        panMain             = new JPanel();
        panInfo             = new JPanel();
        panBtn              = new JPanel();
        lblAPR              = new JLabel();
        lblCollateralPct    = new JLabel();
        lblYears            = new JLabel();
        lblSchedule         = new JLabel();
        lblPrincipal        = new JLabel();
        lblFirstPayment     = new JLabel();
        lblPayAmount        = new JLabel();
        lblNPayment         = new JLabel();
        lblTotalPayment     = new JLabel();
        lblCollateralAmount = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0, 2));

        if (isGrayMonday(campaign.getLocalDate(), campaign.getCampaignOptions().isSimulateGrayMonday())) {
            txtName = new JTextField(resourceMap.getString("lblName.grayMonday"));
        } else {
            txtName = new JTextField(loan.getInstitution());
        }
        txtName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                changeInstitution();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changeInstitution();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changeInstitution();
            }

            public void changeInstitution() {
                loan.setInstitution(txtName.getText());
            }
        });
        txtNumber = new JTextField(loan.getReferenceNumber());
        txtNumber.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changeRefNumber();
            }

            public void changeRefNumber() {
                loan.setReferenceNumber(txtNumber.getText());
            }
        });

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblName.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(txtName, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblReference.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(txtNumber, gridBagConstraints);

        txtPrincipal = new JFormattedTextField();
        txtPrincipal.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
        txtPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
        txtPrincipal.setEditable(false);
        btnPlusHundredMillion  = new JButton(resourceMap.getString("btnPlus100mil.text"));
        btnMinusHundredMillion = new JButton(resourceMap.getString("btnMinus100mil.text"));
        btnPlusTenMillion      = new JButton(resourceMap.getString("btnPlus10mil.text"));
        btnMinusTenMillion     = new JButton(resourceMap.getString("btnMinus10mil.text"));
        btnPlusMillion         = new JButton(resourceMap.getString("btnPlus1mil.text"));
        btnMinusMillion        = new JButton(resourceMap.getString("btnMinus1mil.text"));
        btnPlusHundredK        = new JButton(resourceMap.getString("btnPlus100k.text"));
        btnMinusHundredK       = new JButton(resourceMap.getString("btnMinus100k.text"));
        btnPlusTenK            = new JButton(resourceMap.getString("btnPlus10k.text"));
        btnMinusTenK           = new JButton(resourceMap.getString("btnMinus10k.text"));
        checkMinusButtons();
        btnPlusHundredMillion.addActionListener(evt -> adjustPrincipal(Money.of(100_000_000)));
        btnMinusHundredMillion.addActionListener(evt -> adjustPrincipal(Money.of(-100_000_000)));
        btnPlusTenMillion.addActionListener(evt -> adjustPrincipal(Money.of(10_000_000)));
        btnMinusTenMillion.addActionListener(evt -> adjustPrincipal(Money.of(-10_000_000)));
        btnPlusMillion.addActionListener(evt -> adjustPrincipal(Money.of(1_000_000)));
        btnMinusMillion.addActionListener(evt -> adjustPrincipal(Money.of(-1_000_000)));
        btnPlusHundredK.addActionListener(evt -> adjustPrincipal(Money.of(100_000)));
        btnMinusHundredK.addActionListener(evt -> adjustPrincipal(Money.of(-100_000)));
        btnPlusTenK.addActionListener(evt -> adjustPrincipal(Money.of(10_000)));
        btnMinusTenK.addActionListener(evt -> adjustPrincipal(Money.of(-10_000)));

        JPanel plusPanel = new JPanel(new GridLayout(2, 5));
        plusPanel.add(btnPlusHundredMillion);
        plusPanel.add(btnPlusTenMillion);
        plusPanel.add(btnPlusMillion);
        plusPanel.add(btnPlusHundredK);
        plusPanel.add(btnPlusTenK);
        plusPanel.add(btnMinusHundredMillion);
        plusPanel.add(btnMinusTenMillion);
        plusPanel.add(btnMinusMillion);
        plusPanel.add(btnMinusHundredK);
        plusPanel.add(btnMinusTenK);

        setSliders();
        sldInterest.addChangeListener(this);
        sldCollateral.addChangeListener(this);
        sldLength.addChangeListener(this);

        choiceSchedule = new MMComboBox<>("choiceSchedule", FinancialTerm.values());
        choiceSchedule.setSelectedItem(loan.getFinancialTerm());
        choiceSchedule.addActionListener(this);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblPrincipal.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 2;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(txtPrincipal, gridBagConstraints);

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.gridy  = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panMain.add(plusPanel, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblAnnualInterest.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(sldInterest, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblCollateral.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(sldCollateral, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblLengthYears.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 6;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.weightx   = 1.0;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(sldLength, gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblPaymentSchedule.text")), gridBagConstraints);

        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridy     = 7;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill      = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(choiceSchedule, gridBagConstraints);

        setUpInfo();
        gridBagConstraints           = new GridBagConstraints();
        gridBagConstraints.gridx     = 0;
        gridBagConstraints.gridy     = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill      = GridBagConstraints.BOTH;
        gridBagConstraints.anchor    = GridBagConstraints.WEST;
        gridBagConstraints.insets    = new Insets(5, 5, 5, 5);
        panMain.add(panInfo, gridBagConstraints);

        btnAdd = new JButton(resourceMap.getString("btnOkay.text"));
        btnAdd.setName("btnOK");
        btnAdd.addActionListener(evt -> addLoan());
        panBtn.add(btnAdd);

        btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnClose");
        btnCancel.addActionListener(evt -> cancel());
        gridBagConstraints.gridx     = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor    = GridBagConstraints.CENTER;
        gridBagConstraints.insets    = new Insets(5, 5, 0, 0);
        panBtn.add(btnCancel);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);

        pack();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     *
     * @since 0.50.04
     * @deprecated Move to Suite Constants / Suite Options Setup
     */
    @Deprecated(since = "0.50.04")
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewLoanDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void setUpInfo() {
        panInfo.setLayout(new GridLayout());
        panInfo.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("detailsTitle.text")));
        refreshValues();

        JPanel panLeft  = new JPanel(new GridBagLayout());
        JPanel panRight = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = 0;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panLeft.add(new JLabel(resourceMap.getString("lblAPR.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panLeft.add(lblAPR, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblCollateralPct.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panLeft.add(lblCollateralPct, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblLength.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panLeft.add(lblYears, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panLeft.add(new JLabel(resourceMap.getString("lblSchedule.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panLeft.add(lblSchedule, gridBagConstraints);

        gridBagConstraints        = new GridBagConstraints();
        gridBagConstraints.gridx  = 0;
        gridBagConstraints.gridy  = 0;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panRight.add(new JLabel(resourceMap.getString("lblPrincipalAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblPrincipal, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblFirstPayment.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblFirstPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblInstallmentAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblPayAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblNumberPayments.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblNPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblTotalAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblTotalPayment, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblCollateralAmount.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(lblCollateralAmount, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill   = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panRight.add(new JLabel(resourceMap.getString("lblMaxCollateral.text")), gridBagConstraints);
        gridBagConstraints.gridx  = 1;
        gridBagConstraints.fill   = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panRight.add(new JLabel(maxCollateralValue.toAmountAndSymbolString()), gridBagConstraints);

        panInfo.add(panLeft);
        panInfo.add(panRight);
    }

    private void refreshLoan(Money principal) {
        // Modify loan settings
        loan.setInstitution(txtName.getText());
        loan.setReferenceNumber(txtNumber.getText());
        loan.setPrincipal(principal);
        loan.setRate(sldInterest.getValue());
        loan.setYears(sldLength.getValue());
        loan.setFinancialTerm(choiceSchedule.getSelectedItem());
        loan.setCollateral(sldCollateral.getValue());
        loan.setNextPayment(loan.getFinancialTerm().nextValidDate(campaign.getLocalDate()));

        // Recalculate information based on settings
        loan.calculateAmortization();

        // Refresh dialog values
        refreshValues();
    }

    private void refreshValues() {
        try {
            txtPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
            lblAPR.setText(loan.getRate() + "%");
            lblCollateralPct.setText(loan.getCollateral() + "%");
            lblYears.setText(loan.getYears() + " years");
            lblSchedule.setText(loan.getFinancialTerm().toString());
            lblPrincipal.setText(loan.getPrincipal().toAmountAndSymbolString());
            lblFirstPayment.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(loan.getNextPayment()));
            lblPayAmount.setText(loan.getPaymentAmount().toAmountAndSymbolString());
            lblNPayment.setText(numberFormatter.valueToString(loan.getRemainingPayments()));
            lblTotalPayment.setText(loan.determineRemainingValue().toAmountAndSymbolString());
            lblCollateralAmount.setText(loan.determineCollateralAmount().toAmountAndSymbolString());
        } catch (Exception ex) {
            logger.error("", ex);
        }
    }

    private void addLoan() {
        if (maxCollateralValue.isLessThan(loan.determineCollateralAmount())) {
            JOptionPane.showMessageDialog(frame,
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
        boolean isGrayMonday = isGrayMonday(campaign.getLocalDate(),
              campaign.getCampaignOptions().isSimulateGrayMonday());

        if (campaign.getCampaignOptions().isUseLoanLimits()) {
            int[] interest = Loan.getInterestBracket(rating);
            sldInterest = new JSlider(interest[0], interest[2], loan.getRate());
            sldInterest.setEnabled(!isGrayMonday);

            if (interest[2] - interest[0] > 30) {
                sldInterest.setMajorTickSpacing(10);
            } else {
                sldInterest.setMajorTickSpacing(5);
            }

            sldInterest.setPaintTicks(true);
            sldInterest.setPaintLabels(true);

            int[] collateral = Loan.getCollateralBracket(rating);
            sldCollateral = new JSlider(collateral[0], collateral[2], loan.getCollateral());

            if (collateral[2] - collateral[0] > 50) {
                sldCollateral.setMajorTickSpacing(20);
            } else {
                sldCollateral.setMajorTickSpacing(10);
            }

            sldCollateral.setPaintTicks(true);
            sldCollateral.setPaintLabels(true);
            sldCollateral.setEnabled(!isGrayMonday);

            sldLength = new JSlider(1, Loan.getMaxYears(rating), loan.getYears());
        } else {
            sldInterest = new JSlider(0, 100, loan.getRate());
            sldInterest.setMajorTickSpacing(10);
            sldInterest.setPaintTicks(true);
            sldInterest.setPaintLabels(true);
            sldInterest.setEnabled(!isGrayMonday);

            sldCollateral = new JSlider(0, 300, loan.getCollateral());
            sldCollateral.setMajorTickSpacing(50);
            sldCollateral.setPaintTicks(true);
            sldCollateral.setPaintLabels(true);
            sldCollateral.setEnabled(!isGrayMonday);

            sldLength = new JSlider(1, 10, loan.getYears());
        }

        sldLength.setMajorTickSpacing(1);
        sldLength.setPaintTicks(true);
        sldLength.setPaintLabels(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refreshLoan(loan.getPrincipal());
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (campaign.getCampaignOptions().isUseLoanLimits()) {
            if (Objects.equals(e.getSource(), sldInterest)) {
                sldCollateral.removeChangeListener(this);
                sldCollateral.setValue(Loan.recalculateCollateralFromInterest(rating, sldInterest.getValue()));
                sldCollateral.addChangeListener(this);
            } else if (Objects.equals(e.getSource(), sldCollateral)) {
                sldInterest.removeChangeListener(this);
                sldInterest.setValue(Loan.recalculateInterestFromCollateral(rating, sldCollateral.getValue()));
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
        btnMinusHundredMillion.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(100_000_000)));
        btnMinusTenMillion.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(10_000_000)));
        btnMinusMillion.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(1_000_000)));
        btnMinusHundredK.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(100_000)));
        btnMinusTenK.setEnabled(loan.getPrincipal().isGreaterThan(Money.of(10_000)));
    }
}
