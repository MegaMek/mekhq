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
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.model.PartsInUseTableModel;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.gui.sorter.TwoNumbersSorter;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

    /* Overview reports */
    private JTabbedPane tabOverview;
    // Overview Transport
    private JScrollPane scrollOverviewTransport;
    // Overview Cargo
    private JScrollPane scrollOverviewCargo;
    // Overview Personnel
    private JScrollPane scrollOverviewCombatPersonnel;
    private JScrollPane scrollOverviewSupportPersonnel;
    private PartsInUseTableModel overviewPartsModel;
    // Overview Hangar
    private JScrollPane scrollOverviewHangar;
    private JTextArea overviewHangarArea;
    // Overview Parts In Use
    private JPanel overviewPartsPanel;
    private JTable overviewPartsInUseTable;
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

        tabOverview = new JTabbedPane();
        JScrollPane scrollProcurement = new JScrollPane(procurementTable);
        panProcurement = new JPanel(new GridLayout(0, 1));
        panProcurement.setBorder(BorderFactory.createTitledBorder("Active Procurement List"));
        panProcurement.add(scrollProcurement);
        panProcurement.setMinimumSize(new Dimension(200, 200));
        panProcurement.setPreferredSize(new Dimension(200, 200));
    }

    private void initOverviewPanel() {
        initOverviewPartsInUse();
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

        scrollOverviewParts.setViewportView(overviewPartsPanel);
        tabOverview.addTab(resourceMap.getString("scrollOverviewParts.TabConstraints.tabTitle"),
                scrollOverviewParts);

        scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
        tabOverview.addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
                scrollOverviewUnitRating);
    }

    private void initOverviewPartsInUse() {
        overviewPartsPanel = new JPanel(new BorderLayout());

        overviewPartsModel = new PartsInUseTableModel();
        overviewPartsInUseTable = new JTable(overviewPartsModel);
        overviewPartsInUseTable.setRowSelectionAllowed(false);
        overviewPartsInUseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column;
        for (int i = 0; i < overviewPartsModel.getColumnCount(); ++i) {
            column = overviewPartsInUseTable.getColumnModel().getColumn(i);
            column.setCellRenderer(overviewPartsModel.getRenderer());
            if (overviewPartsModel.hasConstantWidth(i)) {
                column.setMinWidth(overviewPartsModel.getWidth(i));
                column.setMaxWidth(overviewPartsModel.getWidth(i));
            } else {
                column.setPreferredWidth(overviewPartsModel.getPreferredWidth(i));
            }
        }
        overviewPartsInUseTable.setIntercellSpacing(new Dimension(0, 0));
        overviewPartsInUseTable.setShowGrid(false);
        TableRowSorter<PartsInUseTableModel> partsInUseSorter = new TableRowSorter<>(overviewPartsModel);
        partsInUseSorter.setSortsOnUpdates(true);
        // Don't sort the buttons
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_BUY, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_BUY_BULK, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GMADD, false);
        partsInUseSorter.setSortable(PartsInUseTableModel.COL_BUTTON_GMADD_BULK, false);
        // Numeric columns
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_USE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_STORED, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_TONNAGE, new FormattedNumberSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_IN_TRANSFER, new TwoNumbersSorter());
        partsInUseSorter.setComparator(PartsInUseTableModel.COL_COST, new FormattedNumberSorter());
        // Default starting sort
        partsInUseSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        overviewPartsInUseTable.setRowSorter(partsInUseSorter);

        // Add buttons and actions. TODO: Only refresh the row we are working
        // on, not the whole table
        @SuppressWarnings("serial")
        Action buy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().getShoppingList().addShoppingItem(partToBuy, 1, getCampaign());
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action buyInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1, CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().getShoppingList().addShoppingItem(partToBuy, quantity, getCampaign());
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action add = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().addPart((Part) partToBuy.getNewEquipment(), 0);
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action addInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.parseInt(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1, CampaignGUI.MAX_QUANTITY_SPINNER);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                while (quantity > 0) {
                    getCampaign().addPart((Part) partToBuy.getNewEquipment(), 0);
                    --quantity;
                }
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };

        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buy, PartsInUseTableModel.COL_BUTTON_BUY);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buyInBulk,
                PartsInUseTableModel.COL_BUTTON_BUY_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, add, PartsInUseTableModel.COL_BUTTON_GMADD);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, addInBulk,
                PartsInUseTableModel.COL_BUTTON_GMADD_BULK);

        overviewPartsPanel.add(new JScrollPane(overviewPartsInUseTable), BorderLayout.CENTER);
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
            refreshOverviewPartsInUse();
        });
    }

    private void refreshOverviewSpecificPart(int row, PartInUse piu, IAcquisitionWork newPart) {
        if (piu.equals(new PartInUse((Part) newPart))) {
            // Simple update
            getCampaign().updatePartInUse(piu);
            overviewPartsModel.fireTableRowsUpdated(row, row);
        } else {
            // Some other part changed; fire a full refresh to be sure
            refreshOverviewPartsInUse();
        }
    }

    public void refreshOverviewPartsInUse() {
        overviewPartsModel.setData(getCampaign().getPartsInUse());
        TableColumnModel tcm = overviewPartsInUseTable.getColumnModel();
        PartsInUseTableModel.ButtonColumn column = (PartsInUseTableModel.ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GMADD)
                .getCellRenderer();
        column.setEnabled(getCampaign().isGM());
        column = (PartsInUseTableModel.ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GMADD_BULK).getCellRenderer();
        column.setEnabled(getCampaign().isGM());
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
