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
import mekhq.campaign.report.*;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.dialog.*;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Collates important information about the campaign and displays it
 *
 */
public final class CommandCenterTab extends CampaignGuiTab {

    private JPanel panCommand;

    // basic info panel
    private JPanel panInfo;
    private JLabel lblRating;
    private JLabel lblExperience;
    private JLabel lblPersonnel;
    private JLabel lblMissionSuccess;
    private JLabel lblComposition;
    private JLabel lblRepairStatus;
    private JLabel lblTransportCapacity;
    private JLabel lblCargoSummary;



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

    // available reports
    private JPanel panReports;

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

        initInfoPanel();
        initLogPanel();
        initReportsPanel();
        initProcurementPanel();

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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panProcurement, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panReports, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panInfo, gridBagConstraints);

        setLayout(new BorderLayout());
        add(panCommand, BorderLayout.CENTER);

    }

    private void initInfoPanel() {
        panInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        int y = 0;

        /* Unit Rating */
        if (getCampaign().getCampaignOptions().useDragoonRating()) {
            JLabel lblRatingHead = new JLabel(resourceMap.getString("lblRating.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panInfo.add(lblRatingHead, gridBagConstraints);
            lblRating = new JLabel(getCampaign().getUnitRatingText());
            gridBagConstraints.gridx = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            panInfo.add(lblRating, gridBagConstraints);
            y++;
        }
        JLabel lblExperienceHead = new JLabel(resourceMap.getString("lblExperience.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblExperienceHead, gridBagConstraints);
        lblExperience = new JLabel(getCampaign().getUnitRating().getAverageExperience());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblExperience, gridBagConstraints);
        y++;
        JLabel lblMissionSuccessHead = new JLabel(resourceMap.getString("lblMissionSuccess.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblMissionSuccessHead, gridBagConstraints);
        lblMissionSuccess = new JLabel(getCampaign().getMissionSuccessString());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblMissionSuccess, gridBagConstraints);
        y++;
        JLabel lblPersonnelHead = new JLabel(resourceMap.getString("lblPersonnel.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblPersonnelHead, gridBagConstraints);
        lblPersonnel = new JLabel(getCampaign().getPersonnelSummary());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblPersonnel, gridBagConstraints);
        y++;
        JLabel lblCompositionHead = new JLabel(resourceMap.getString("lblComposition.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblCompositionHead, gridBagConstraints);
        lblComposition = new JLabel(getCampaign().getForceComposition());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblComposition, gridBagConstraints);
        y++;
        JLabel lblRepairStatusHead = new JLabel(resourceMap.getString("lblRepairStatus.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblRepairStatusHead, gridBagConstraints);
        lblRepairStatus = new JLabel(getCampaign().getForceRepairStatus());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblRepairStatus, gridBagConstraints);
        y++;
        JLabel lblTransportCapacityHead = new JLabel(resourceMap.getString("lblTransportCapacity.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblTransportCapacityHead, gridBagConstraints);
        lblTransportCapacity = new JLabel(getCampaign().getTransportCapacity());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblTransportCapacity, gridBagConstraints);
        y++;
        JLabel lblCargoSummaryHead = new JLabel(resourceMap.getString("lblCargoSummary.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panInfo.add(lblCargoSummaryHead, gridBagConstraints);
        lblCargoSummary = new JLabel(getCampaign().getCargoCapacity());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        panInfo.add(lblCargoSummary, gridBagConstraints);

        panInfo.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panInfo.title")));


    }

    private void initLogPanel() {
        panLog = new DailyReportLogPanel(getCampaignGui().getReportHLL());
        panLog.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panLog.title")));
        panLog.setMinimumSize(new java.awt.Dimension(400, 100));
        panLog.setPreferredSize(new java.awt.Dimension(400, 100));
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
        panProcurement = new JPanel(new GridBagLayout());
        panProcurement.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panProcurement.title")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5,5,5,5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panProcurement.add(panProcurementButtons, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(5,5,5,5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panProcurement.add(scrollProcurement, gridBagConstraints);
    }

    private void initReportsPanel() {
        panReports = new JPanel(new GridLayout(3, 2));
        JButton btnTransportReport = new JButton(resourceMap.getString("btnTransportReport.text"));
        btnTransportReport.addActionListener(ev -> {
            getCampaignGui().showReport(new TransportReport(getCampaign()));
        });
        panReports.add(btnTransportReport);
        JButton btnHangarOverview = new JButton(resourceMap.getString("btnHangarOverview.text"));
        btnHangarOverview.addActionListener(evt -> {
            getCampaignGui().showReport(new HangarReport(getCampaign()));
        });
        panReports.add(btnHangarOverview);
        JButton btnPersonnelOverview = new JButton(resourceMap.getString("btnPersonnelOverview.text"));
        btnPersonnelOverview.addActionListener(evt -> {
            getCampaignGui().showReport(new PersonnelReport(getCampaign()));
        });
        panReports.add(btnPersonnelOverview);
        JButton btnCargoCapacity = new JButton(resourceMap.getString("btnCargoCapacity.text"));
        btnCargoCapacity.addActionListener(evt -> {
            getCampaignGui().showReport(new CargoReport(getCampaign()));
        });
        panReports.add(btnCargoCapacity);
        JButton btnUnitRating = new JButton(resourceMap.getString("btnUnitRating.text"));
        btnUnitRating.addActionListener(evt -> {
            getCampaignGui().showReport(new RatingReport(getCampaign()));
        });
        panReports.add(btnUnitRating);
        panReports.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panReports.title")));
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.COMMAND;
    }

    @Override
    public void refreshAll() {
        refreshBasicInfo();
        refreshProcurementList();
        refreshLog();
    }

    private void refreshBasicInfo() {
        getCampaign().getUnitRating().reInitialize();
        lblRating.setText(getCampaign().getUnitRatingText());
        lblPersonnel.setText(getCampaign().getPersonnelSummary());
        lblMissionSuccess.setText(getCampaign().getMissionSuccessString());
        lblExperience.setText(getCampaign().getUnitRating().getAverageExperience());
        lblComposition.setText(getCampaign().getForceComposition());
        lblCargoSummary.setText(getCampaign().getCargoCapacity());
        lblRepairStatus.setText(getCampaign().getForceRepairStatus());
    }

    private void refreshProcurementList() {
        procurementModel.setData(getCampaign().getShoppingList().getAllShoppingItems());
    }

    private void initLog() {
        String report = getCampaign().getCurrentReportHTML();
        panLog.refreshLog(report);
        getCampaign().fetchAndClearNewReports();
    }

    synchronized private void refreshLog() {
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
    private ActionScheduler basicInfoScheduler = new ActionScheduler(this::refreshBasicInfo);

    @Subscribe
    public void handle(UnitRefitEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        procurementListScheduler.schedule();
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(ProcurementEvent ev) {
        procurementListScheduler.schedule();
    }

    @Subscribe
    public void handle(ReportEvent ev) {
        refreshLog();
    }

    @Subscribe
    public void handleNewDay(NewDayEvent evt) {
        procurementListScheduler.schedule();
        basicInfoScheduler.schedule();
        initLog();
    }

    @Subscribe
    public void handle(MissionEvent evt) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent evt) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitEvent evt) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(OptionsChangedEvent evt) {
        basicInfoScheduler.schedule();
        procurementListScheduler.schedule();
    }
}
