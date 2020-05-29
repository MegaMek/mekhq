/*
 * Copyright (c) 2020 The MegaMek Team. All rights reserved.
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

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.*;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.report.*;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.dialog.*;
import mekhq.gui.model.PartsInUseTableModel;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.gui.sorter.TwoNumbersSorter;
import mekhq.service.PartsAcquisitionService;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Collates important information about the campaign and displays it
 *
 */
public final class CommandCenterTab extends CampaignGuiTab {

    private JPanel panCommand;

    // daily report
    private DailyReportLogPanel panLog;

    // procurement table
    private JPanel panProcurement;
    private JTable procurementTable;
    private ProcurementTableModel procurementModel;
    private JButton btnGetUnit;
    private JButton btnGetParts;
    private JButton btnNeededParts;
    private JButton btnPartsReport;

    /* Overview reports */
    private JTabbedPane tabOverview;
    // Overview Transport
    private JScrollPane scrollOverviewTransport;
    // Overview Cargo
    private JScrollPane scrollOverviewCargo;
    // Overview Personnel
    private JScrollPane scrollOverviewCombatPersonnel;
    private JScrollPane scrollOverviewSupportPersonnel;
    // Overview Hangar
    private JScrollPane scrollOverviewHangar;
    private JTextArea overviewHangarArea;
    // Overview Rating
    private JScrollPane scrollOverviewUnitRating;

    ResourceBundle resourceMap;

    CommandCenterTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    @Override
    public void initTab() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());

        panCommand = new JPanel(new GridBagLayout());

        initReportPanel();
        initProcurementPanel();
        initOverviewPanel();

        /* Set overall layout */
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(5,5,5,5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panLog, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panProcurement, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(tabOverview, gridBagConstraints);

        setLayout(new BorderLayout());
        add(panCommand, BorderLayout.CENTER);

    }

    private void initReportPanel() {
        panLog = new DailyReportLogPanel(getCampaignGui().getReportHLL());
        panLog.setMinimumSize(new java.awt.Dimension(300, 100));
        panLog.setPreferredSize(new java.awt.Dimension(300, 100));
    }

    private void initProcurementPanel() {

        /* shopping buttons */
        JPanel panProcurementButtons = new JPanel(new GridLayout(4, 1));
        btnGetUnit = new JButton(resourceMap.getString("btnGetUnit.text")); // NOI18N
        btnGetUnit.setToolTipText(resourceMap.getString("btnGetUnit.toolTipText")); // NOI18N
        btnGetUnit.addActionListener(ev -> getUnit());
        btnGetUnit.setEnabled(true);
        panProcurementButtons.add(btnGetUnit);
        btnGetParts = new JButton(resourceMap.getString("btnGetParts.text")); // NOI18N
        btnGetParts.setToolTipText(resourceMap.getString("btnGetParts.toolTipText")); // NOI18N
        btnGetParts.addActionListener(ev -> getParts());
        btnGetParts.setEnabled(true);
        panProcurementButtons.add(btnGetParts);
        btnNeededParts = new JButton();
        btnNeededParts.setText(resourceMap.getString("btnNeededParts.text")); // NOI18N
        btnNeededParts.setToolTipText(resourceMap.getString("btnNeededParts.toolTipText"));
        btnNeededParts.addActionListener(ev -> {
            AcquisitionsDialog dlg = new AcquisitionsDialog(getFrame(), true, getCampaignGui());
            dlg.setVisible(true);
        });
        panProcurementButtons.add(btnNeededParts);
        btnPartsReport = new JButton();
        btnPartsReport.setText(resourceMap.getString("btnPartsReport.text")); // NOI18N
        btnPartsReport.setToolTipText(resourceMap.getString("btnPartsReport.toolTipText"));
        btnPartsReport.addActionListener(ev -> {
            PartsReportDialog dlg = new PartsReportDialog(getCampaignGui(), true);
            dlg.setVisible(true);
        });
        panProcurementButtons.add(btnPartsReport);

        /* shopping table */
        procurementModel = new ProcurementTableModel(getCampaign());
        procurementTable = new JTable(procurementModel);
        TableRowSorter<ProcurementTableModel> shoppingSorter = new TableRowSorter<>(
                procurementModel);
        shoppingSorter.setComparator(ProcurementTableModel.COL_COST, new FormattedNumberSorter());
        shoppingSorter.setComparator(ProcurementTableModel.COL_TARGET, new TargetSorter());
        procurementTable.setRowSorter(shoppingSorter);
        TableColumn column;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = procurementTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(procurementModel.getColumnWidth(i));
            column.setCellRenderer(procurementModel.getRenderer());
        }
        procurementTable.setIntercellSpacing(new Dimension(0, 0));
        procurementTable.setShowGrid(false);
        procurementTable.addMouseListener(new ProcurementTableMouseAdapter(getCampaignGui()));
        procurementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0),
                "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                "REMOVE");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
                "REMOVE");

        procurementTable.getActionMap().put("ADD", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 4958203340754214211L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (procurementTable.getSelectedRow() < 0) {
                    return;
                }
                procurementModel
                        .incrementItem(procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()));
            }
        });

        procurementTable.getActionMap().put("REMOVE", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -8377486575329708963L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (procurementTable.getSelectedRow() < 0) {
                    return;
                }
                if (procurementModel
                        .getAcquisition(procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()))
                        .getQuantity() > 0) {
                    procurementModel.decrementItem(
                            procurementTable.convertRowIndexToModel(procurementTable.getSelectedRow()));
                }
            }
        });

        JScrollPane scrollProcurement = new JScrollPane(procurementTable);
        panProcurement = new JPanel(new BorderLayout());
        //panProcurement.setBorder(BorderFactory.createTitledBorder("Active Procurement List"));
        panProcurement.add(panProcurementButtons, BorderLayout.WEST);
        panProcurement.add(scrollProcurement, BorderLayout.CENTER);
        //panProcurement.setMinimumSize(new Dimension(200, 200));
        //panProcurement.setPreferredSize(new Dimension(200, 200));
    }

    private void initOverviewPanel() {
        tabOverview = new JTabbedPane();

        JScrollPane scrollOverviewParts = new JScrollPane();
        scrollOverviewTransport = new JScrollPane();
        scrollOverviewCombatPersonnel = new JScrollPane();
        scrollOverviewSupportPersonnel = new JScrollPane();
        scrollOverviewHangar = new JScrollPane();
        overviewHangarArea = new JTextArea();
        JSplitPane splitOverviewHangar;
        scrollOverviewUnitRating = new JScrollPane();
        scrollOverviewCargo = new JScrollPane();

        scrollOverviewTransport
                .setToolTipText(resourceMap.getString("scrollOverviewTransport.TabConstraints.toolTipText")); // NOI18N
        scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
        tabOverview.addTab(resourceMap.getString("scrollOverviewTransport.TabConstraints.tabTitle"),
                scrollOverviewTransport);

        scrollOverviewCargo.setToolTipText(resourceMap.getString("scrollOverviewCargo.TabConstraints.toolTipText")); // NOI18N
        scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
        tabOverview.addTab(resourceMap.getString("scrollOverviewCargo.TabConstraints.tabTitle"),
                scrollOverviewCargo);

        scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
        scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());
        JSplitPane splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewCombatPersonnel,
                scrollOverviewSupportPersonnel);
        splitOverviewPersonnel.setOneTouchExpandable(true);
        splitOverviewPersonnel.setResizeWeight(0.5);
        tabOverview.addTab(resourceMap.getString("scrollOverviewPersonnel.TabConstraints.tabTitle"),
                splitOverviewPersonnel);

        scrollOverviewHangar.setViewportView(new HangarReport(getCampaign()).getHangarTree());
        overviewHangarArea.setLineWrap(false);
        overviewHangarArea.setFont(new Font("Courier New", Font.PLAIN, 18));
        overviewHangarArea.setText("");
        overviewHangarArea.setEditable(false);
        overviewHangarArea.setName("overviewHangarArea"); // NOI18N
        splitOverviewHangar = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewHangar, overviewHangarArea);
        splitOverviewHangar.setOneTouchExpandable(true);
        splitOverviewHangar.setResizeWeight(0.5);
        tabOverview.addTab(resourceMap.getString("scrollOverviewHangar.TabConstraints.tabTitle"),
                splitOverviewHangar);

        scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
        tabOverview.addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
                scrollOverviewUnitRating);
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.COMMAND;
    }

    @Override
    public void refreshAll() {
        refreshProcurementList();
        refreshReport();
        refreshOverview();
    }

    public void refreshOverview() {
        SwingUtilities.invokeLater(() -> {
            int drIndex = tabOverview.indexOfComponent(scrollOverviewUnitRating);
            if (!getCampaign().getCampaignOptions().useDragoonRating() && drIndex != -1) {
                tabOverview.removeTabAt(drIndex);
            } else {
                if (drIndex == -1) {
                    tabOverview.addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
                            scrollOverviewUnitRating);
                }
            }

            scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
            scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
            scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());
            scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
            scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
            HangarReport hr = new HangarReport(getCampaign());
            scrollOverviewHangar.setViewportView(hr.getHangarTree());
            overviewHangarArea.setText(hr.getHangarTotals());
        });
    }

    private void refreshProcurementList() {
        procurementModel.setData(getCampaign().getShoppingList().getAllShoppingItems());
    }

    private void initReport() {
        String report = getCampaign().getCurrentReportHTML();
        panLog.refreshLog(report);
        getCampaign().fetchAndClearNewReports();
    }

    synchronized private void refreshReport() {
        List<String> newLogEntries = getCampaign().fetchAndClearNewReports();
        panLog.appendLog(newLogEntries);
    }

    private void getUnit() {
        UnitSelectorDialog usd = new UnitSelectorDialog(getFrame(), getCampaign(), true);
        usd.setVisible(true);
    }

    private void getParts() {
        PartsStoreDialog psd = new PartsStoreDialog(true, getCampaignGui());
        psd.setVisible(true);
    }

    private ActionScheduler procurementListScheduler = new ActionScheduler(this::refreshProcurementList);
    private ActionScheduler overviewScheduler = new ActionScheduler(this::refreshOverview);

    @Subscribe
    public void handle(UnitRefitEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(ProcurementEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(ReportEvent ev) {
        refreshReport();
    }

    @Subscribe
    public void handleNewDay(NewDayEvent evt) {
        procurementListScheduler.schedule();
        overviewScheduler.schedule();
        initReport();
    }

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(DeploymentChangedEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(PartEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(PartWorkEvent ev) {
        overviewScheduler.schedule();
    }

    @Subscribe
    public void handle(LoanEvent ev) {
        overviewScheduler.schedule();
    }

}
