/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.MechSummaryCache;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.MHQOptionsChangedEvent;
import mekhq.campaign.event.*;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.reportDialogs.*;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.service.enums.MRMSMode;
import mekhq.service.mrms.MRMSService;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Collates important information about the campaign and displays it, along with some actionable buttons
 */
public final class CommandCenterTab extends CampaignGuiTab {
    private JPanel panCommand;

    // basic info panel
    private JPanel panInfo;
    private JLabel lblRatingHead;
    private JLabel lblRating;
    private JLabel lblExperience;
    private JLabel lblPersonnel;
    private JLabel lblMissionSuccess;
    private JLabel lblComposition;
    private JLabel lblRepairStatus;
    private JLabel lblTransportCapacity;
    private JLabel lblCargoSummary;

    // objectives panel
    private JPanel panObjectives;
    java.awt.List listObjectives;

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
    private JButton btnMRMSDialog;
    private JButton btnMRMSInstant;

    // available reports
    private JPanel panReports;
    private JButton btnUnitRating;

    //icon panel
    private JPanel panIcon;
    private JLabel lblIcon;

    private static final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
            MekHQ.getMHQOptions().getLocale());

    /**
     * @param gui a {@link CampaignGUI} object that this tab is a component of
     * @param name a <code>String</code> giving the name of this tab
     */
    public CommandCenterTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    //region Getters/Setters
    public DailyReportLogPanel getPanLog() {
        return panLog;
    }
    //endregion Getters/Setters

    /**
     * initialize the contents and layout of the tab
     */
    @Override
    public void initTab() {
        panCommand = new JPanel(new GridBagLayout());

        initInfoPanel();
        initLogPanel();
        initReportsPanel();
        initProcurementPanel();
        initObjectivesPanel();
        panIcon = new JPanel(new BorderLayout());
        lblIcon = new JLabel();
        lblIcon.getAccessibleContext().setAccessibleName("Player Camouflage");
        panIcon.add(lblIcon, BorderLayout.CENTER);
        lblIcon.setIcon(getCampaign().getUnitIcon().getImageIcon(150));

        /* Set overall layout */
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panLog, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panCommand.add(panProcurement, gridBagConstraints);
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panReports, gridBagConstraints);
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panObjectives, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panInfo, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panCommand.add(panIcon, gridBagConstraints);

        setLayout(new BorderLayout());
        add(panCommand, BorderLayout.CENTER);

    }

    private void initInfoPanel() {
        panInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        int y = 0;

        /* Unit Rating */
        lblRatingHead = new JLabel(resourceMap.getString("lblRating.text"));
        lblRatingHead.setVisible(getCampaign().getCampaignOptions().getUnitRatingMethod().isEnabled());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 1, 5);
        panInfo.add(lblRatingHead, gridBagConstraints);
        lblRating = new JLabel(getCampaign().getUnitRatingText());
        lblRatingHead.setLabelFor(lblRating);
        lblRating.setVisible(getCampaign().getCampaignOptions().getUnitRatingMethod().isEnabled());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblRating, gridBagConstraints);
        JLabel lblExperienceHead = new JLabel(resourceMap.getString("lblExperience.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblExperienceHead, gridBagConstraints);
        lblExperience = new JLabel(getCampaign().getUnitRating().getAverageExperience().toString());
        lblExperienceHead.setLabelFor(lblExperience);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblExperience, gridBagConstraints);

        JLabel lblMissionSuccessHead = new JLabel(resourceMap.getString("lblMissionSuccess.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblMissionSuccessHead, gridBagConstraints);
        lblMissionSuccess = new JLabel(getCampaign().getCampaignSummary().getMissionSuccessReport());
        lblMissionSuccessHead.setLabelFor(lblMissionSuccess);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblMissionSuccess, gridBagConstraints);

        JLabel lblPersonnelHead = new JLabel(resourceMap.getString("lblPersonnel.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblPersonnelHead, gridBagConstraints);
        lblPersonnel = new JLabel(getCampaign().getCampaignSummary().getPersonnelReport());
        lblPersonnelHead.setLabelFor(lblPersonnel);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblPersonnel, gridBagConstraints);

        JLabel lblCompositionHead = new JLabel(resourceMap.getString("lblComposition.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblCompositionHead, gridBagConstraints);
        lblComposition = new JLabel(getCampaign().getCampaignSummary().getForceCompositionReport());
        lblCompositionHead.setLabelFor(lblComposition);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblComposition, gridBagConstraints);

        JLabel lblRepairStatusHead = new JLabel(resourceMap.getString("lblRepairStatus.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblRepairStatusHead, gridBagConstraints);
        lblRepairStatus = new JLabel(getCampaign().getCampaignSummary().getForceRepairReport());
        lblRepairStatusHead.setLabelFor(lblRepairStatus);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblRepairStatus, gridBagConstraints);

        JLabel lblTransportCapacityHead = new JLabel(resourceMap.getString("lblTransportCapacity.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        panInfo.add(lblTransportCapacityHead, gridBagConstraints);
        lblTransportCapacity = new JLabel(getCampaign().getCampaignSummary().getTransportCapacity());
        lblTransportCapacityHead.setLabelFor(lblTransportCapacity);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblTransportCapacity, gridBagConstraints);

        JLabel lblCargoSummaryHead = new JLabel(resourceMap.getString("lblCargoSummary.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(1, 5, 5, 5);
        panInfo.add(lblCargoSummaryHead, gridBagConstraints);
        lblCargoSummary = new JLabel(getCampaign().getCampaignSummary().getCargoCapacityReport());
        lblCargoSummaryHead.setLabelFor(lblCargoSummary);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 1.0;
        panInfo.add(lblCargoSummary, gridBagConstraints);

        panInfo.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panInfo.title")));
    }

    /**
     * Initialize the panel for showing any objectives that might exist. Objectives might come from
     * different play modes.
     */
    private void initObjectivesPanel() {
        panObjectives = new JPanel(new BorderLayout());
        panObjectives.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panObjectives.title")));
        listObjectives = new java.awt.List();
        for (String objective : getCampaign().getCurrentObjectives()) {
            listObjectives.add(objective);
        }
        panObjectives.add(listObjectives, BorderLayout.CENTER);
    }

    /**
     * Initialize the panel for displaying the daily report log
     */
    private void initLogPanel() {
        panLog = new DailyReportLogPanel(getCampaignGui());
        panLog.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panLog.title")));
        panLog.setMinimumSize(new Dimension(400, 100));
        panLog.setPreferredSize(new Dimension(400, 100));
    }

    /**
     * Initialize the panel for displaying procurement information
     */
    private void initProcurementPanel() {
        /* shopping buttons */
        JPanel panProcurementButtons = new JPanel(new GridLayout(6, 1));
        panProcurementButtons.getAccessibleContext().setAccessibleName("Procurement Actions");

        btnGetUnit = new JButton(resourceMap.getString("btnGetUnit.text"));
        btnGetUnit.setToolTipText(resourceMap.getString("btnGetUnit.toolTipText"));
        btnGetUnit.addActionListener(evt -> getUnit());
        panProcurementButtons.add(btnGetUnit);

        btnGetParts = new JButton(resourceMap.getString("btnGetParts.text"));
        btnGetParts.setToolTipText(resourceMap.getString("btnGetParts.toolTipText"));
        btnGetParts.addActionListener(evt -> getParts());
        panProcurementButtons.add(btnGetParts);

        btnNeededParts = new JButton(resourceMap.getString("btnNeededParts.text"));
        btnNeededParts.setToolTipText(resourceMap.getString("btnNeededParts.toolTipText"));
        btnNeededParts.addActionListener(evt ->
                new AcquisitionsDialog(getFrame(), true, getCampaignGui()).setVisible(true));
        panProcurementButtons.add(btnNeededParts);

        btnPartsReport = new JButton(resourceMap.getString("btnPartsReport.text"));
        btnPartsReport.setToolTipText(resourceMap.getString("btnPartsReport.toolTipText"));
        btnPartsReport.addActionListener(evt ->
                new PartsReportDialog(getCampaignGui(), true).setVisible(true));
        panProcurementButtons.add(btnPartsReport);

        btnMRMSDialog = new JButton(resourceMap.getString("btnMRMSDialog.text"));
        btnMRMSDialog.setToolTipText(resourceMap.getString("btnMRMSDialog.toolTipText"));
        btnMRMSDialog.setName("btnMRMSDialog");
        btnMRMSDialog.addActionListener(evt ->
                new MRMSDialog(getFrame(), true, getCampaignGui(), null, MRMSMode.UNITS)
                        .setVisible(true));
        btnMRMSDialog.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
        panProcurementButtons.add(btnMRMSDialog);

        btnMRMSInstant = new JButton(resourceMap.getString("btnMRMSInstant.text"));
        btnMRMSInstant.setToolTipText(resourceMap.getString("btnMRMSInstant.toolTipText"));
        btnMRMSInstant.setName("btnMRMSInstant");
        btnMRMSInstant.addActionListener(evt -> {
            MRMSService.mrmsAllUnits(getCampaign());
            JOptionPane.showMessageDialog(getCampaignGui().getFrame(), "Mass Repair/Salvage complete.",
                    "Complete", JOptionPane.INFORMATION_MESSAGE);
        });
        btnMRMSInstant.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
        panProcurementButtons.add(btnMRMSInstant);

        /* shopping table */
        procurementModel = new ProcurementTableModel(getCampaign());
        procurementTable = new JTable(procurementModel);
        procurementTable.getAccessibleContext().setAccessibleName("Pending Procurements");
        TableRowSorter<ProcurementTableModel> shoppingSorter = new TableRowSorter<>(procurementModel);
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
        ProcurementTableMouseAdapter.connect(getCampaignGui(), procurementTable, procurementModel);
        procurementTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ADD");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "REMOVE");
        procurementTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "REMOVE");

        procurementTable.getActionMap().put("ADD", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final int row : procurementTable.getSelectedRows()) {
                    if (row >= 0) {
                        procurementModel.incrementItem(procurementTable.convertRowIndexToModel(row));
                    }
                }
            }
        });

        procurementTable.getActionMap().put("REMOVE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (final int rowIndex : procurementTable.getSelectedRows()) {
                    if (rowIndex < 0) {
                        continue;
                    }
                    final int row = procurementTable.convertRowIndexToModel(rowIndex);
                    if (procurementModel.getAcquisition(row).map(IAcquisitionWork::getQuantity)
                            .orElse(0) > 0) {
                        procurementModel.decrementItem(row);
                    }
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
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panProcurement.add(panProcurementButtons, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panProcurement.add(scrollProcurement, gridBagConstraints);
    }

    /**
     * Initialize the panel for displaying available reports
     */
    private void initReportsPanel() {
        panReports = new JPanel(new GridLayout(5, 1));

        JButton btnTransportReport = new JButton(resourceMap.getString("btnTransportReport.text"));
        btnTransportReport.addActionListener(ev -> new TransportReportDialog(getCampaignGui().getFrame(),
                new TransportReport(getCampaign())).setVisible(true));
        panReports.add(btnTransportReport);

        JButton btnHangarOverview = new JButton(resourceMap.getString("btnHangarOverview.text"));
        btnHangarOverview.addActionListener(evt -> new HangarReportDialog(getCampaignGui().getFrame(),
                new HangarReport(getCampaign())).setVisible(true));
        panReports.add(btnHangarOverview);

        JButton btnPersonnelOverview = new JButton(resourceMap.getString("btnPersonnelOverview.text"));
        btnPersonnelOverview.addActionListener(evt -> new PersonnelReportDialog(getCampaignGui().getFrame(),
                new PersonnelReport(getCampaign())).setVisible(true));
        panReports.add(btnPersonnelOverview);

        JButton btnCargoCapacity = new JButton(resourceMap.getString("btnCargoCapacity.text"));
        btnCargoCapacity.addActionListener(evt -> new CargoReportDialog(getCampaignGui().getFrame(),
                new CargoReport(getCampaign())).setVisible(true));
        panReports.add(btnCargoCapacity);

        btnUnitRating = new JButton(resourceMap.getString("btnUnitRating.text"));
        btnUnitRating.setVisible(getCampaign().getCampaignOptions().getUnitRatingMethod().isEnabled());
        btnUnitRating.addActionListener(evt -> new UnitRatingReportDialog(getCampaignGui().getFrame(),
                getCampaign()).setVisible(true));
        panReports.add(btnUnitRating);

        panReports.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("panReports.title")));
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.COMMAND_CENTER;
    }

    /**
     * refresh all components
     */
    @Override
    public void refreshAll() {
        refreshBasicInfo();
        refreshProcurementList();
        refreshLog();
        refreshObjectives();
    }

    /**
     * refresh the basic info panel with campaign information
     */
    private void refreshBasicInfo() {
        getCampaign().getUnitRating().reInitialize();
        getCampaign().getCampaignSummary().updateInformation();
        lblRating.setText(getCampaign().getUnitRatingText());
        lblPersonnel.setText(getCampaign().getCampaignSummary().getPersonnelReport());
        lblMissionSuccess.setText(getCampaign().getCampaignSummary().getMissionSuccessReport());
        lblExperience.setText(getCampaign().getUnitRating().getAverageExperience().toString());
        lblComposition.setText(getCampaign().getCampaignSummary().getForceCompositionReport());
        lblCargoSummary.setText(getCampaign().getCampaignSummary().getCargoCapacityReport());
        lblRepairStatus.setText(getCampaign().getCampaignSummary().getForceRepairReport());
        lblTransportCapacity.setText(getCampaign().getCampaignSummary().getTransportCapacity());
    }

    private void refreshObjectives() {
        listObjectives.removeAll();
        for (String objective : getCampaign().getCurrentObjectives()) {
            listObjectives.add(objective);
        }
    }

    /**
     * refresh the procurement list
     */
    private void refreshProcurementList() {
        procurementModel.setData(getCampaign().getShoppingList().getShoppingList());
    }

    /**
     * Initialize a new daily log report
     */
    private void initLog() {
        String report = getCampaign().getCurrentReportHTML();
        panLog.refreshLog(report);
        getCampaign().fetchAndClearNewReports();
    }

    /**
     * append new reports to the daily log report
     */
    synchronized private void refreshLog() {
        panLog.appendLog(getCampaign().fetchAndClearNewReports());
    }

    /**
     * brings up the {@link AbstractUnitSelectorDialog} or {@link UnitMarketDialog}, depending on
     * the currently selected options
     */
    private void getUnit() {
        if (MekHQ.getMHQOptions().getCommandCenterUseUnitMarket()
                && !getCampaign().getUnitMarket().getMethod().isNone()) {
            new UnitMarketDialog(getFrame(), getCampaign()).showDialog();
        } else {
            UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(getFrame());
            if (!MechSummaryCache.getInstance().isInitialized()) {
                unitLoadingDialog.setVisible(true);
            }
            AbstractUnitSelectorDialog usd = new MekHQUnitSelectorDialog(getFrame(), unitLoadingDialog,
                    getCampaign(), true);
            usd.setVisible(true);
        }
    }

    /**
     * brings up the {@link PartsStoreDialog}
     */
    private void getParts() {
        PartsStoreDialog psd = new PartsStoreDialog(true, getCampaignGui());
        psd.setVisible(true);
    }

    private ActionScheduler procurementListScheduler = new ActionScheduler(this::refreshProcurementList);
    private ActionScheduler basicInfoScheduler = new ActionScheduler(this::refreshBasicInfo);
    private ActionScheduler objectivesScheduler = new ActionScheduler(this::refreshObjectives);

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
        objectivesScheduler.schedule();
        initLog();
    }

    @Subscribe
    public void handle(MissionEvent evt) {
        basicInfoScheduler.schedule();
        objectivesScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioEvent evt) {
        objectivesScheduler.schedule();
    }

    @Subscribe
    public void handle(TransitCompleteEvent evt) {
        objectivesScheduler.schedule();
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
    public void handle(final OptionsChangedEvent evt) {
        lblRatingHead.setVisible(evt.getOptions().getUnitRatingMethod().isEnabled());
        lblRating.setVisible(evt.getOptions().getUnitRatingMethod().isEnabled());
        btnUnitRating.setVisible(evt.getOptions().getUnitRatingMethod().isEnabled());
        basicInfoScheduler.schedule();
        procurementListScheduler.schedule();
        lblIcon.setIcon(getCampaign().getUnitIcon().getImageIcon(150));
    }

    @Subscribe
    public void handle(MHQOptionsChangedEvent evt) {
        btnMRMSDialog.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
        btnMRMSInstant.setVisible(MekHQ.getMHQOptions().getCommandCenterMRMS());
    }

    @Subscribe
    public void handle(TransactionEvent ev) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(LoanEvent ev) {
        basicInfoScheduler.schedule();
    }

    @Subscribe
    public void handle(AssetEvent ev) {
        basicInfoScheduler.schedule();
    }
}
