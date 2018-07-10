/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.AssetEvent;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.LoanEvent;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.event.MissionNewEvent;
import mekhq.campaign.event.PartEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.TransactionEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.Contract;
import mekhq.gui.adapter.FinanceTableMouseAdapter;
import mekhq.gui.adapter.LoanTableMouseAdapter;
import mekhq.gui.dialog.AddFundsDialog;
import mekhq.gui.dialog.ManageAssetsDialog;
import mekhq.gui.dialog.NewLoanDialog;
import mekhq.gui.model.FinanceTableModel;
import mekhq.gui.model.LoanTableModel;

/**
 * Shows record of financial transactions.
 */
public final class FinancesTab extends CampaignGuiTab {

    private static final long serialVersionUID = -3203920871646865885L;
    
    private ResourceBundle resourceMap;

    private JTable financeTable;
    private JTable loanTable;
    private JTextArea areaNetWorth;
    private JButton btnAddFunds;
    private JButton btnManageAssets;

    private FinanceTableModel financeModel;
    private LoanTableModel loanModel;

    FinancesTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }
    
    private enum GraphType {
    	BALANCE_AMOUNT, MONTHLY_FINANCES;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
    	resourceMap = ResourceBundle.getBundle("mekhq.resources.FinancesTab", new EncodeControl()); //$NON-NLS-1$
    	
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());
        ChartPanel financeAmountPanel = (ChartPanel) createGraphPanel(GraphType.BALANCE_AMOUNT);
        ChartPanel financeMonthlyPanel = (ChartPanel) createGraphPanel(GraphType.MONTHLY_FINANCES);

        financeModel = new FinanceTableModel();
        financeTable = new JTable(financeModel);
        financeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        financeTable.addMouseListener(new FinanceTableMouseAdapter(getCampaignGui(),
                financeTable, financeModel));
        financeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
        for (int i = 0; i < FinanceTableModel.N_COL; i++) {
            column = financeTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(financeModel.getColumnWidth(i));
            column.setCellRenderer(financeModel.getRenderer());
        }
        financeTable.setIntercellSpacing(new Dimension(0, 0));
        financeTable.setShowGrid(false);

        loanModel = new LoanTableModel();
        loanTable = new JTable(loanModel);
        loanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        loanTable.addMouseListener(new LoanTableMouseAdapter(getCampaignGui(),
                loanTable, loanModel));
        loanTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        column = null;
        for (int i = 0; i < LoanTableModel.N_COL; i++) {
            column = loanTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(loanModel.getColumnWidth(i));
            column.setCellRenderer(loanModel.getRenderer());
        }
        loanTable.setIntercellSpacing(new Dimension(0, 0));
        loanTable.setShowGrid(false);
        JScrollPane scrollLoanTable = new JScrollPane(loanTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        JPanel panBalance = new JPanel(new GridBagLayout());
        panBalance.add(new JScrollPane(financeTable), gridBagConstraints);
        panBalance.setMinimumSize(new java.awt.Dimension(350, 100));
        panBalance.setBorder(BorderFactory.createTitledBorder("Balance Sheet"));
        JPanel panLoan = new JPanel(new GridBagLayout());
        panLoan.add(scrollLoanTable, gridBagConstraints);

        JTabbedPane financeTab = new JTabbedPane();
        financeTab.setMinimumSize(new java.awt.Dimension(450, 300));
        financeTab.setPreferredSize(new java.awt.Dimension(450, 300));
        
        JSplitPane splitFinances = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panBalance, financeTab);
		splitFinances.setOneTouchExpandable(true);
		splitFinances.setContinuousLayout(true);
		splitFinances.setResizeWeight(1.0);
		splitFinances.setName("splitFinances");
        
        financeTab.addTab(resourceMap.getString("activeLoans.text"), panLoan);
        financeTab.addTab(resourceMap.getString("cbillsBalanceTime.text"), financeAmountPanel);
        financeTab.addTab(resourceMap.getString("monthlyRevenueExpenditures.text"), financeMonthlyPanel);
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitFinances, gridBagConstraints);

        JPanel panelFinanceRight = new JPanel(new BorderLayout());

        JPanel pnlFinanceBtns = new JPanel(new GridLayout(2, 2));
        btnAddFunds = new JButton("Add Funds (GM)");
        btnAddFunds.addActionListener(ev -> addFundsActionPerformed());
        btnAddFunds.setEnabled(getCampaign().isGM());
        pnlFinanceBtns.add(btnAddFunds);
        JButton btnGetLoan = new JButton("Get Loan");
        btnGetLoan.addActionListener(e -> showNewLoanDialog());
        pnlFinanceBtns.add(btnGetLoan);

        btnManageAssets = new JButton("Manage Assets (GM)");
        btnManageAssets.addActionListener(e -> manageAssets());
        btnManageAssets.setEnabled(getCampaign().isGM());
        pnlFinanceBtns.add(btnManageAssets);

        panelFinanceRight.add(pnlFinanceBtns, BorderLayout.NORTH);

        areaNetWorth = new JTextArea();
        areaNetWorth.setLineWrap(true);
        areaNetWorth.setWrapStyleWord(true);
        areaNetWorth.setFont(new Font("Courier New", Font.PLAIN, 12));
        areaNetWorth.setText(getCampaign().getFinancialReport());
        areaNetWorth.setEditable(false);

        JScrollPane descriptionScroll = new JScrollPane(areaNetWorth);
        panelFinanceRight.add(descriptionScroll, BorderLayout.CENTER);
        areaNetWorth.setCaretPosition(0);
        descriptionScroll.setMinimumSize(new Dimension(300, 200));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        add(panelFinanceRight, gridBagConstraints);
    }
    
    private XYDataset setupFinanceDataset() {
    	TimeSeries s1 = new TimeSeries("C-Bills"); // NOI18N
    	ArrayList<Transaction> transactions = getCampaign().getFinances().getAllTransactions();
    	Calendar cal = Calendar.getInstance();
    	
    	long balance = 0;
    	for (int i = 0; i < transactions.size(); i++) {
    		balance += transactions.get(i).getAmount();
    		cal.setTime(transactions.get(i).getDate());
    		// since there may be more than one entry per day and the dataset for the graph can only have one entry per day
    		// we use addOrUpdate() which assumes transactions are in sequential order by date so we always have the most
    		// up-to-date entry for each day
    		s1.addOrUpdate(new Day(cal.get(Calendar.DAY_OF_MONTH),
    				cal.get(Calendar.MONTH)+1, // Gregorian and Julian calendars start at 0: https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html#MONTH
    				cal.get(Calendar.YEAR)), 
    				balance);
    	}
                
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        
        return dataset;
    }
    
    private CategoryDataset setupMonthlyDataset() {
    	SimpleDateFormat df = new SimpleDateFormat("MMM-yyyy");
    	DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    	ArrayList<Transaction> transactions = getCampaign().getFinances().getAllTransactions();
    	Calendar cal = Calendar.getInstance();
    	
    	String pastMonthYear = "";
    	long monthlyRevenue = 0;
    	long monthlyExpenditures = 0;
    	for (int i = 0; i < transactions.size(); i++) {
    		cal.setTime(transactions.get(i).getDate());
    		
    		if (pastMonthYear.equals(df.format(cal.getTime()))) {
    			if (transactions.get(i).getAmount() > 0) {
    				monthlyRevenue += transactions.get(i).getAmount();
    			} else {
    				monthlyExpenditures += Math.abs(transactions.get(i).getAmount());
    			}
    		} else {
    			// as long as we're not at the first transaction, add the previous month and reset
    			if (i != 0) {
    				dataset.addValue(monthlyRevenue, resourceMap.getString("graphMonthlyRevenue.text"), pastMonthYear);
    				dataset.addValue(monthlyExpenditures, resourceMap.getString("graphMonthlyExpenditures.text"), pastMonthYear);
    				monthlyRevenue = 0;
    				monthlyExpenditures = 0;
    			}
    			pastMonthYear = df.format(cal.getTime());
    			if (transactions.get(i).getAmount() > 0) {
    				monthlyRevenue = transactions.get(i).getAmount();
    			} else {
    				monthlyExpenditures = Math.abs(transactions.get(i).getAmount());
    			}
    		}
    		
    		// if we're at the last transaction, save it off
			if (i == transactions.size()-1) {
				dataset.addValue(monthlyRevenue, resourceMap.getString("graphMonthlyRevenue.text"), pastMonthYear);
				dataset.addValue(monthlyExpenditures, resourceMap.getString("graphMonthlyExpenditures.text"), pastMonthYear);
			}
    	}
        
        return dataset;
    }
    
    private JPanel createGraphPanel(GraphType gt) {
        JFreeChart chart = null;
    	if (gt.equals(GraphType.BALANCE_AMOUNT)) {
    		chart = createAmountChart(setupFinanceDataset());
    	} else if (gt.equals(GraphType.MONTHLY_FINANCES)) {
    		chart = createMonthlyChart(setupMonthlyDataset());
    	}
        ChartPanel panel = new ChartPanel(chart, false);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        return panel;
    }
    
    private JFreeChart createAmountChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "", // title
            resourceMap.getString("graphDate.text"), // x-axis label
            resourceMap.getString("graphCBills.text"), // y-axis label
            dataset);

        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setDefaultShapesVisible(true);
            renderer.setDefaultShapesFilled(true);
            renderer.setDrawSeriesLineAsPath(true);
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
        
        chart.removeLegend();

        return chart;
    }
    
    private JFreeChart createMonthlyChart(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
            "", // title
            resourceMap.getString("graphDate.text"), // x-axis label
            resourceMap.getString("graphCBills.text"), // y-axis label
            dataset);

        chart.setBackgroundPaint(Color.WHITE);
        
        chart.getLegend().setPosition(RectangleEdge.TOP);

        return chart;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshFinancialTransactions();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public GuiTabType tabType() {
        return GuiTabType.FINANCES;
    }

    private void addFundsActionPerformed() {
        AddFundsDialog addFundsDialog = new AddFundsDialog(getFrame(), true);
        addFundsDialog.setVisible(true);
        if (addFundsDialog.getClosedType() == JOptionPane.OK_OPTION) {
            long funds = addFundsDialog.getFundsQuantity();
            String description = addFundsDialog.getFundsDescription();
            int category = addFundsDialog.getCategory();
            getCampaign().addFunds(funds, description, category);
        }
    }

    private void showNewLoanDialog() {
        NewLoanDialog nld = new NewLoanDialog(getFrame(), true, getCampaign());
        nld.setVisible(true);
    }

    private void manageAssets() {
        ManageAssetsDialog mad = new ManageAssetsDialog(getFrame(), getCampaign());
        mad.setVisible(true);
    }

    public void refreshFinancialTransactions() {
        SwingUtilities.invokeLater(() -> { 
            financeModel.setData(getCampaign().getFinances().getAllTransactions());
            loanModel.setData(getCampaign().getFinances().getAllLoans());
            refreshFinancialReport();
        });
    }

    public void refreshFinancialReport() {
        SwingUtilities.invokeLater(() -> { 
            areaNetWorth.setText(getCampaign().getFinancialReport());
            areaNetWorth.setCaretPosition(0);
        });
    }
    
    ActionScheduler financialTransactionsScheduler = new ActionScheduler(this::refreshFinancialTransactions);
    ActionScheduler financialReportScheduler = new ActionScheduler(this::refreshFinancialReport);

    @Subscribe
    public void handle(GMModeEvent ev) {
        btnAddFunds.setEnabled(ev.isGMMode());
        btnManageAssets.setEnabled(ev.isGMMode());
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        financialTransactionsScheduler.schedule();
    }

    @Subscribe
    public void handle(MissionNewEvent ev) {
        if (ev.getMission() instanceof Contract) {
            financialReportScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(MissionChangedEvent ev) {
        if (ev.getMission() instanceof Contract) {
            financialReportScheduler.schedule();
        }
    }
    
    @Subscribe
    public void handle(AcquisitionEvent ev) {
        financialTransactionsScheduler.schedule();
    }
    
    @Subscribe
    public void handle(TransactionEvent ev) {
        financialTransactionsScheduler.schedule();
    }
    
    @Subscribe
    public void handle(LoanEvent ev) {
        financialTransactionsScheduler.schedule();
    }
    
    @Subscribe
    public void handle(UnitEvent ev) {
        financialReportScheduler.schedule();
    }
    
    @Subscribe
    public void handle(PartEvent ev) {
        financialReportScheduler.schedule();
    }
    
    @Subscribe
    public void handle(AssetEvent ev) {
        financialReportScheduler.schedule();
    }
}
