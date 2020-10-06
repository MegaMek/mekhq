/*
 * ContractSummaryPanel.java
 *
 * Copyright (c) 2018, 2020 - The MegaMek Team
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

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.mission.Contract;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Contract payment breakdown, showing all incomes and expenses, finishing with the estimated profit.
 *
 * @author Miguel Azevedo
 */
public class ContractPaymentBreakdown {
    private JPanel mainPanel;
    private Campaign campaign;
    private Contract contract;

    private ResourceBundle resourceMap = ResourceBundle.getBundle(
            "mekhq.resources.ContractPaymentBreakdown", new EncodeControl());

    private static final String indentation = "    ";
    private JLabel lblBaseAmount2;
    private JLabel lblOverheadAmount2;
    private JLabel lblSupportAmount2;
    private JLabel lblTransportAmount2;
    private JLabel lblTransitAmount2;
    private JLabel lblNetIncome2;
    private JLabel lblAdvanceNetIncome1;
    private JLabel lblAdvanceNetIncome2;
    private JLabel lblSignBonusAmount2;
    private JLabel lblMonthlyNetIncome1;
    private JLabel lblMonthlyNetIncome2;
    private JLabel lblFeeAmount2;
    private JLabel lblOverheadExp2;
    private JLabel lblMaintenanceExp2;
    private JLabel lblPayrollExp2;
    private JLabel lblTotalAdvanceMoney2;
    private JLabel lblTotalMonthlyMoney2;
    private JLabel lblTransportationExpenses2;
    private JLabel lblEstimatedProfit2;

    /**
     * @param mainPanel panel where the elements will be appended
     * @param contract  that it is displaying
     * @param campaign  loaded
     */
    public ContractPaymentBreakdown(JPanel mainPanel, Contract contract, Campaign campaign) {
        this.mainPanel = mainPanel;
        this.campaign = campaign;
        this.contract = contract;
    }

    /**
     * Draws and fill all the elements for the contract payment breakdown
     *
     * @param y         gridBagConstraint.gridy, in case it is appending to an existing grid
     * @param gridWidth the gridBagConstraint.gridWidth to use for text, in case it is appending to an
     *                  existing grid
     */
    public void display(int y, int gridWidth) {
        //region Variable Declarations and Initializations
        Font f;

        // Initializing the GridBagConstraint used for Labels
        // To use this you MUST AND ONLY overwrite gridy
        GridBagConstraints gridBagConstraintsLabels = new GridBagConstraints();
        gridBagConstraintsLabels.gridx = 0;
        gridBagConstraintsLabels.gridwidth = 1;
        gridBagConstraintsLabels.weightx = 1.0;
        gridBagConstraintsLabels.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraintsLabels.anchor = GridBagConstraints.WEST;
        gridBagConstraintsLabels.insets = new Insets(2, 2, 2, 2);

        // Initializing the GridBagConstraint used for Text
        // To use this you MUST AND ONLY overwrite gridy
        GridBagConstraints gridBagConstraintsText = new GridBagConstraints();
        gridBagConstraintsText.gridx = 1;
        gridBagConstraintsText.gridwidth = gridWidth;
        gridBagConstraintsText.weightx = 1.0;
        gridBagConstraintsText.fill = GridBagConstraints.NONE;
        gridBagConstraintsText.anchor = GridBagConstraints.EAST;
        gridBagConstraintsText.insets = new Insets(2, 2, 2, 2);
        //endregion Variable Declarations and Initializations

        JLabel lblIncome = new JLabel(resourceMap.getString("lblIncome.text"));
        f = lblIncome.getFont();
        lblIncome.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gridBagConstraintsLabels.gridy = y;
        mainPanel.add(lblIncome, gridBagConstraintsLabels);

        lblNetIncome2 = new JLabel();
        setLblNetIncome2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblNetIncome2, gridBagConstraintsText);

        JLabel lblBaseAmount1 = new JLabel(indentation
                + resourceMap.getString("lblBaseAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblBaseAmount1, gridBagConstraintsLabels);

        lblBaseAmount2 = new JLabel();
        setLblBaseAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblBaseAmount2, gridBagConstraintsText);

        JLabel lblOverheadAmount1 = new JLabel(indentation
                + resourceMap.getString("lblOverheadAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblOverheadAmount1, gridBagConstraintsLabels);

        lblOverheadAmount2 = new JLabel();
        setLblOverheadAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblOverheadAmount2, gridBagConstraintsText);

        JLabel lblSupportAmount1 = new JLabel(indentation
                + resourceMap.getString("lblSupportAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblSupportAmount1, gridBagConstraintsLabels);

        lblSupportAmount2 = new JLabel();
        setLblSupportAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblSupportAmount2, gridBagConstraintsText);

        JLabel lblTransportAmount1 = new JLabel(indentation
                + resourceMap.getString("lblTransportAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblTransportAmount1, gridBagConstraintsLabels);

        lblTransportAmount2 = new JLabel();
        setLblTransportAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblTransportAmount2, gridBagConstraintsText);

        JLabel lblTransitAmount1 = new JLabel(indentation
                + resourceMap.getString("lblTransitAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblTransitAmount1, gridBagConstraintsLabels);

        lblTransitAmount2 = new JLabel();
        setLblTransitAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblTransitAmount2, gridBagConstraintsText);

        JLabel lblFeeAmount1;
        if (contract.payMRBCFee()) {
            lblFeeAmount1 = new JLabel(indentation + resourceMap.getString("lblFeeAmount1.text")
                    + " (-" + contract.getMrbcFeePercentage() + "% "
                    + resourceMap.getString("lblOfGrossIncome.text") + ")");
        } else {
            lblFeeAmount1 = new JLabel(indentation + resourceMap.getString("lblFeeAmount1.text"));
        }
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblFeeAmount1, gridBagConstraintsLabels);

        lblFeeAmount2 = new JLabel();
        setLblFeeAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblFeeAmount2, gridBagConstraintsText);

        JLabel sep1 = new JLabel("");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(sep1, gridBagConstraintsLabels);

        JLabel sep2 = new JLabel("");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(sep2, gridBagConstraintsText);

        JLabel lblAdvanceCashflow1 = new JLabel(resourceMap.getString("lblAdvanceCashflow.text"));
        f = lblAdvanceCashflow1.getFont();
        lblAdvanceCashflow1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblAdvanceCashflow1, gridBagConstraintsLabels);

        lblTotalAdvanceMoney2 = new JLabel();
        setLblTotalAdvanceMoney2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblTotalAdvanceMoney2, gridBagConstraintsText);

        lblAdvanceNetIncome1 = new JLabel();
        setLblAdvanceNetIncome1();
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblAdvanceNetIncome1, gridBagConstraintsLabels);

        lblAdvanceNetIncome2 = new JLabel();
        setLblAdvanceNetIncome2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblAdvanceNetIncome2, gridBagConstraintsText);

        JLabel lblSignBonusAmount1 = new JLabel(indentation + resourceMap.getString("lblSignBonusAmount1.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblSignBonusAmount1, gridBagConstraintsLabels);

        lblSignBonusAmount2 = new JLabel();
        setLblSignBonusAmount2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblSignBonusAmount2, gridBagConstraintsText);

        JLabel lblMonthlyCashFlow1 = new JLabel(resourceMap.getString("lblMonthlyCashFlow.text"));
        f = lblMonthlyCashFlow1.getFont();
        lblMonthlyCashFlow1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblMonthlyCashFlow1, gridBagConstraintsLabels);

        lblTotalMonthlyMoney2 = new JLabel();
        setLblTotalMonthlyMoney2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblTotalMonthlyMoney2, gridBagConstraintsText);

        lblMonthlyNetIncome1 = new JLabel();
        setLblMonthlyNetIncome1();
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblMonthlyNetIncome1, gridBagConstraintsLabels);

        lblMonthlyNetIncome2 = new JLabel();
        setLblMonthlyNetIncome2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblMonthlyNetIncome2, gridBagConstraintsText);

        JLabel lblOverheadExp = new JLabel(indentation + resourceMap.getString("lblEstimatedOverheadExpenses.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblOverheadExp, gridBagConstraintsLabels);

        lblOverheadExp2 = new JLabel();
        setLblOverheadExp2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblOverheadExp2, gridBagConstraintsText);

        JLabel lblMaintenanceExp = new JLabel(indentation + resourceMap.getString("lblEstimatedMaintenanceExpenses.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblMaintenanceExp, gridBagConstraintsLabels);

        lblMaintenanceExp2 = new JLabel();
        setLblMaintenanceExp2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblMaintenanceExp2, gridBagConstraintsText);

        JLabel lblPayrollExp = new JLabel(indentation + resourceMap.getString("lblEstimatedPayrollExpenses.text"));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblPayrollExp, gridBagConstraintsLabels);

        lblPayrollExp2 = new JLabel();
        setLblPayrollExp2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblPayrollExp2, gridBagConstraintsText);

        JLabel lblTransportationExpenses1 = new JLabel(resourceMap.getString("lblTransportationExpenses.text"));
        f = lblTransportationExpenses1.getFont();
        lblTransportationExpenses1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblTransportationExpenses1, gridBagConstraintsLabels);

        lblTransportationExpenses2 = new JLabel();
        setLblTransportationExpenses2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblTransportationExpenses2, gridBagConstraintsText);

        JLabel lblEstimatedProfit1 = new JLabel(resourceMap.getString("lblEstimatedProfit.text"));
        f = lblEstimatedProfit1.getFont();
        lblEstimatedProfit1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblEstimatedProfit1, gridBagConstraintsLabels);

        lblEstimatedProfit2 = new JLabel();
        setLblEstimatedProfit2();
        gridBagConstraintsText.gridy = y;
        mainPanel.add(lblEstimatedProfit2, gridBagConstraintsText);
    }

    /**
     * Refreshes all of the values
     */
    public void refresh() {
        setLblBaseAmount2();
        setLblOverheadAmount2();
        setLblSupportAmount2();
        setLblTransportAmount2();
        setLblTransitAmount2();
        setLblFeeAmount2();
        setLblNetIncome2();

        setLblAdvanceNetIncome1();
        setLblAdvanceNetIncome2();
        setLblSignBonusAmount2();

        setLblMonthlyNetIncome1();
        setLblMonthlyNetIncome2();
        setLblOverheadExp2();
        setLblMaintenanceExp2();
        setLblPayrollExp2();

        setLblTotalAdvanceMoney2();
        setLblTotalMonthlyMoney2();
        setLblTransportationExpenses2();

        setLblEstimatedProfit2();
    }

    private void setLblBaseAmount2() {
        lblBaseAmount2.setText(contract.getBaseAmount().toAmountAndSymbolString());
    }

    private void setLblOverheadAmount2() {
        lblOverheadAmount2.setText(contract.getOverheadAmount().toAmountAndSymbolString());
    }

    private void setLblSupportAmount2() {
        lblSupportAmount2.setText(contract.getSupportAmount().toAmountAndSymbolString());
    }

    private void setLblTransportAmount2() {
        lblTransportAmount2.setText(contract.getTransportAmount().toAmountAndSymbolString());
    }

    private void setLblTransitAmount2() {
        lblTransitAmount2.setText(contract.getTransitAmount().toAmountAndSymbolString());
    }

    private void setLblFeeAmount2() {
        lblFeeAmount2.setText("-" + contract.getFeeAmount().toAmountAndSymbolString());
    }

    private void setLblNetIncome2() {
        lblNetIncome2.setText(contract.getTotalAmountPlusFees().toAmountAndSymbolString());
    }

    private void setLblAdvanceNetIncome1() {
        lblAdvanceNetIncome1.setText(indentation + contract.getAdvancePct() + "% "
                + resourceMap.getString("lblOfNetIncome.text") + ":");
    }

    private void setLblAdvanceNetIncome2() {
        lblAdvanceNetIncome2.setText(contract.getAdvanceAmount().toAmountAndSymbolString());
    }

    private void setLblSignBonusAmount2() {
        lblSignBonusAmount2.setText(contract.getSigningBonusAmount().toAmountAndSymbolString());
    }

    private void setLblMonthlyNetIncome1() {
        lblMonthlyNetIncome1.setText(indentation + (100 - contract.getAdvancePct()) + "% "
                + resourceMap.getString("lblOfNetIncome.text") + ":");
    }

    private void setLblMonthlyNetIncome2() {
        lblMonthlyNetIncome2.setText(generateMonthlyHeader(contract.getLength())
                + contract.getMonthlyPayOut().toAmountAndSymbolString());
    }

    private void setLblOverheadExp2() {
        lblOverheadExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + new Accountant(campaign).getOverheadExpenses().toAmountAndSymbolString());
    }

    private void setLblMaintenanceExp2() {
        lblMaintenanceExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + new Accountant(campaign).getMaintenanceCosts().toAmountAndSymbolString());
    }

    private void setLblPayrollExp2() {
        lblPayrollExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + contract.getEstimatedPayrollExpenses(campaign).toAmountAndSymbolString());
    }

    private void setLblTotalAdvanceMoney2() {
        lblTotalAdvanceMoney2.setText(contract.getTotalAdvanceAmount().toAmountAndSymbolString());
    }

    private void setLblTotalMonthlyMoney2() {
        lblTotalMonthlyMoney2.setText(contract.getTotalMonthlyPayOut(campaign).toAmountAndSymbolString());
    }

    private void setLblTransportationExpenses2() {
        lblTransportationExpenses2.setText("-" + contract.getTotalTransportationFees(campaign).toAmountAndSymbolString());
    }

    private void setLblEstimatedProfit2() {
        lblEstimatedProfit2.setText(contract.getEstimatedTotalProfit(campaign).toAmountAndSymbolString());
    }

    private String generateMonthlyHeader(int length) {
        if (length > 1) {
            return length + " " + resourceMap.getString("lblMonths.text") + " @ ";
        } else {
            return length + " " + resourceMap.getString("lblMonth.text") + " @ ";
        }
    }
}
