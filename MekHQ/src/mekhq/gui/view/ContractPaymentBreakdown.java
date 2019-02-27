package mekhq.gui.view;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Contract payment breakdown, showing all incomes and expenses, finishing with the estimated profit.
 *
 * @author Miguel Azevedo
 *
 */
public class ContractPaymentBreakdown {
    private JPanel mainPanel;
    private Campaign campaign;
    private Contract contract;

    private ResourceBundle resourceMap;

    private static final String identation = "    ";
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
     * @param contract that it is displaying
     * @param campaign loaded
     */
    public ContractPaymentBreakdown(JPanel mainPanel, Contract contract, Campaign campaign) {
        this.mainPanel = mainPanel;
        this.campaign = campaign;
        this.contract = contract;
        resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractPaymentBreakdown", new EncodeControl()); //$NON-NLS-1$
    }

    /**
     * Draws and fill all the elements for the contract payment breakdown
     * @param y gbc.gridy, in case it is appending to an existing grid
     */
    public void display(int y){
    Font f;
    GridBagConstraints gridBagConstraints;

    JLabel lblIncome = new JLabel(resourceMap.getString("lblIncome.text"));
    f = lblIncome.getFont();
    lblIncome.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblIncome, gridBagConstraints);
    lblNetIncome2 = new JLabel();
    setLblNetIncome2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblNetIncome2, gridBagConstraints);

    JLabel lblBaseAmount1 = new JLabel(identation + resourceMap.getString("lblBaseAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblBaseAmount1, gridBagConstraints);
    lblBaseAmount2 = new JLabel();
    setLblBaseAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblBaseAmount2, gridBagConstraints);

    JLabel lblOverheadAmount1 = new JLabel(identation + resourceMap.getString("lblOverheadAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblOverheadAmount1, gridBagConstraints);
    lblOverheadAmount2 = new JLabel();
    setLblOverheadAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblOverheadAmount2, gridBagConstraints);

    JLabel lblSupportAmount1 = new JLabel(identation + resourceMap.getString("lblSupportAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblSupportAmount1, gridBagConstraints);
    lblSupportAmount2 = new JLabel();
    setLblSupportAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblSupportAmount2, gridBagConstraints);

    JLabel lblTransportAmount1 = new JLabel(identation + resourceMap.getString("lblTransportAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransportAmount1, gridBagConstraints);
    lblTransportAmount2 = new JLabel();
    setLblTransportAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransportAmount2, gridBagConstraints);

    JLabel lblTransitAmount1 = new JLabel(identation + resourceMap.getString("lblTransitAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransitAmount1, gridBagConstraints);
    lblTransitAmount2 = new JLabel();
    setLblTransitAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransitAmount2, gridBagConstraints);

    JLabel lblFeeAmount1;
    if(contract.payMRBCFee()){
        lblFeeAmount1= new JLabel(identation + resourceMap.getString("lblFeeAmount1.text") + " (-" + contract.getMrbcFeePercentage() + "% " + resourceMap.getString("lblOfGrossIncome.text") + ")");
    }
    else{
        lblFeeAmount1= new JLabel(identation + resourceMap.getString("lblFeeAmount1.text"));
    }
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblFeeAmount1, gridBagConstraints);
    lblFeeAmount2 = new JLabel();
    setLblFeeAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblFeeAmount2, gridBagConstraints);

    JLabel sep1 = new JLabel("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(sep1, gridBagConstraints);
    JLabel sep2 = new JLabel("");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(sep2, gridBagConstraints);

    JLabel lblAdvanceCashflow1 = new JLabel(resourceMap.getString("lblAdvanceCashflow.text"));
    f = lblAdvanceCashflow1.getFont();
    lblAdvanceCashflow1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblAdvanceCashflow1, gridBagConstraints);
    lblTotalAdvanceMoney2 = new JLabel();
    setLblTotalAdvanceMoney2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTotalAdvanceMoney2, gridBagConstraints);

    lblAdvanceNetIncome1 = new JLabel();
    setLblAdvanceNetIncome1();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblAdvanceNetIncome1, gridBagConstraints);
    lblAdvanceNetIncome2 = new JLabel();
    setLblAdvanceNetIncome2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblAdvanceNetIncome2, gridBagConstraints);

    JLabel lblSignBonusAmount1 = new JLabel(identation + resourceMap.getString("lblSignBonusAmount1.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblSignBonusAmount1, gridBagConstraints);
    lblSignBonusAmount2 = new JLabel();
    setLblSignBonusAmount2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblSignBonusAmount2, gridBagConstraints);

    JLabel lblMonthlyCashflow1 = new JLabel(resourceMap.getString("lblMonthlyCashflow.text"));
    f = lblMonthlyCashflow1.getFont();
    lblMonthlyCashflow1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblMonthlyCashflow1, gridBagConstraints);
    lblTotalMonthlyMoney2 = new JLabel();
    setLblTotalMonthlyMoney2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTotalMonthlyMoney2, gridBagConstraints);

    lblMonthlyNetIncome1 = new JLabel();
    setLblMonthlyNetIncome1();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblMonthlyNetIncome1, gridBagConstraints);
    lblMonthlyNetIncome2 = new JLabel();
    setLblMonthlyNetIncome2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblMonthlyNetIncome2, gridBagConstraints);

    JLabel lblOverheadExp = new JLabel(identation + resourceMap.getString("lblEstimatedOverheadExpenses.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblOverheadExp, gridBagConstraints);
    lblOverheadExp2 = new JLabel();
    setLblOverheadExp2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblOverheadExp2, gridBagConstraints);

    JLabel lblMaintenanceExp = new JLabel(identation + resourceMap.getString("lblEstimatedMaintenanceExpenses.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblMaintenanceExp, gridBagConstraints);
    lblMaintenanceExp2 = new JLabel();
    setLblMaintenanceExp2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblMaintenanceExp2, gridBagConstraints);

    JLabel lblPayrollExp = new JLabel(identation + resourceMap.getString("lblEstimatedPayrollExpenses.text"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblPayrollExp, gridBagConstraints);
    lblPayrollExp2 = new JLabel();
    setLblPayrollExp2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblPayrollExp2, gridBagConstraints);

    JLabel lblTransportationExpenses1 = new JLabel(resourceMap.getString("lblTransportationExpenses.text"));
    f = lblTransportationExpenses1.getFont();
    lblTransportationExpenses1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransportationExpenses1, gridBagConstraints);
    lblTransportationExpenses2 = new JLabel();
    setLblTransportationExpenses2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblTransportationExpenses2, gridBagConstraints);

    JLabel lblEstimatedProfit1 = new JLabel(resourceMap.getString("lblEstimatedProfit.text"));
    f = lblEstimatedProfit1.getFont();
    lblEstimatedProfit1.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = y;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblEstimatedProfit1, gridBagConstraints);
    lblEstimatedProfit2 = new JLabel();
    setLblEstimatedProfit2();
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = y++;
    gridBagConstraints.gridwidth = 1;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    mainPanel.add(lblEstimatedProfit2, gridBagConstraints);
    }

    /**
     * Rrefreshes all of the values
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

    private void setLblOverheadAmount2(){
        lblOverheadAmount2.setText(contract.getOverheadAmount().toAmountAndSymbolString());
    }

    private void setLblSupportAmount2(){
        lblSupportAmount2.setText(contract.getSupportAmount().toAmountAndSymbolString());
    }

    private void setLblTransportAmount2(){
        lblTransportAmount2.setText(contract.getTransportAmount().toAmountAndSymbolString());
    }

    private void setLblTransitAmount2(){
        lblTransitAmount2.setText(contract.getTransitAmount().toAmountAndSymbolString());
    }

    private void setLblNetIncome2(){
        lblNetIncome2.setText(contract.getTotalAmountPlusFees().toAmountAndSymbolString());
    }

    private void setLblAdvanceNetIncome1(){
        lblAdvanceNetIncome1.setText(identation + contract.getAdvancePct() + "% "
                + resourceMap.getString("lblOfNetIncome.text") + ":");
    }

    private void setLblAdvanceNetIncome2(){
        lblAdvanceNetIncome2.setText(contract.getAdvanceAmount().toAmountAndSymbolString());
    }

    private void setLblSignBonusAmount2(){
        lblSignBonusAmount2.setText(contract.getSigningBonusAmount().toAmountAndSymbolString());
    }

    private void setLblMonthlyNetIncome1() {
        lblMonthlyNetIncome1.setText(identation + (100 - contract.getAdvancePct()) + "% "
                + resourceMap.getString("lblOfNetIncome.text") + ":");
    }

    private void setLblMonthlyNetIncome2(){
        lblMonthlyNetIncome2.setText(generateMonthlyHeader(contract.getLength())
                + contract.getMonthlyPayOut().toAmountAndSymbolString());
    }

    private void setLblFeeAmount2(){
        lblFeeAmount2.setText("-" + contract.getFeeAmount().toAmountAndSymbolString());
    }

    private void setLblOverheadExp2(){
        lblOverheadExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + campaign.getOverheadExpenses().toAmountAndSymbolString());
    }

    private void setLblMaintenanceExp2(){
        lblMaintenanceExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + campaign.getMaintenanceCosts().toAmountAndSymbolString());
    }
    private void setLblPayrollExp2(){
        lblPayrollExp2.setText(generateMonthlyHeader(contract.getLengthPlusTravel(campaign))
                + "-" + contract.getEstimatedPayrollExpenses(campaign).toAmountAndSymbolString());
    }

    private void setLblTotalAdvanceMoney2(){
        lblTotalAdvanceMoney2.setText(contract.getTotalAdvanceAmount().toAmountAndSymbolString());
    }

    private void setLblTotalMonthlyMoney2(){
        lblTotalMonthlyMoney2.setText(contract.getTotalMonthlyPayOut(campaign).toAmountAndSymbolString());
    }

    private void setLblTransportationExpenses2(){
        lblTransportationExpenses2.setText("-" + contract.getTotalTransportationFees(campaign).toAmountAndSymbolString());
    }

    private void setLblEstimatedProfit2(){
        lblEstimatedProfit2.setText(contract.getEstimatedTotalProfit(campaign).toAmountAndSymbolString());
    }

    private String generateMonthlyHeader(int length){
        if(length > 1) {
            return length + " " + resourceMap.getString("lblMonths.text") + " @ ";
        }

        return length + " " + resourceMap.getString("lblMonth.text") + " @ ";
    }
}
