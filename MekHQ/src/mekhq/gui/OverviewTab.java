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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.AssignmentChangedEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.rating.UnitRatingFactory;
import mekhq.campaign.report.CargoReport;
import mekhq.campaign.report.HangarReport;
import mekhq.campaign.report.PersonnelReport;
import mekhq.campaign.report.RatingReport;
import mekhq.campaign.report.TransportReport;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.dialog.PopupValueChoiceDialog;
import mekhq.gui.model.PartsInUseTableModel;
import mekhq.gui.model.PartsInUseTableModel.ButtonColumn;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TwoNumbersSorter;

/**
 *
 */
public final class OverviewTab extends CampaignGuiTab {

    private static final long serialVersionUID = -564451623308341081L;

    private JTabbedPane tabOverview;
    // Overview Parts In Use
    private JScrollPane scrollOverviewParts;
    private JPanel overviewPartsPanel;
    private JTable overviewPartsInUseTable;
    // Overview Transport
    private JScrollPane scrollOverviewTransport;
    // Overview Personnel
    private JScrollPane scrollOverviewCombatPersonnel;
    private JScrollPane scrollOverviewSupportPersonnel;
    private JSplitPane splitOverviewPersonnel;
    // Overview Hangar
    private JScrollPane scrollOverviewHangar;
    private JTextArea overviewHangarArea;
    private JSplitPane splitOverviewHangar;
    // Overview Rating
    private JScrollPane scrollOverviewUnitRating;
    private IUnitRating rating;
    // Overview Cargo
    private JScrollPane scrollOverviewCargo;

    private PartsInUseTableModel overviewPartsModel;
    private TableRowSorter<PartsInUseTableModel> partsInUseSorter;

    ResourceBundle resourceMap;

    OverviewTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setTabOverview(new JTabbedPane());
        scrollOverviewParts = new JScrollPane();
        initOverviewPartsInUse();
        scrollOverviewTransport = new JScrollPane();
        scrollOverviewCombatPersonnel = new JScrollPane();
        scrollOverviewSupportPersonnel = new JScrollPane();
        scrollOverviewHangar = new JScrollPane();
        overviewHangarArea = new JTextArea();
        splitOverviewHangar = new JSplitPane();
        scrollOverviewUnitRating = new JScrollPane();
        scrollOverviewCargo = new JScrollPane();

        // Overview tab
        setName("panelOverview"); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        getTabOverview().setToolTipText(resourceMap.getString("tabOverview.toolTipText")); // NOI18N
        getTabOverview().setMinimumSize(new java.awt.Dimension(250, 250));
        getTabOverview().setName("tabOverview"); // NOI18N
        getTabOverview().setPreferredSize(new java.awt.Dimension(800, 300));

        scrollOverviewTransport
                .setToolTipText(resourceMap.getString("scrollOverviewTransport.TabConstraints.toolTipText")); // NOI18N
        scrollOverviewTransport.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollOverviewTransport.setPreferredSize(new java.awt.Dimension(350, 400));
        scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
        getTabOverview().addTab(resourceMap.getString("scrollOverviewTransport.TabConstraints.tabTitle"),
                scrollOverviewTransport);

        scrollOverviewCargo.setToolTipText(resourceMap.getString("scrollOverviewCargo.TabConstraints.toolTipText")); // NOI18N
        scrollOverviewCargo.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollOverviewCargo.setPreferredSize(new java.awt.Dimension(350, 400));
        scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
        getTabOverview().addTab(resourceMap.getString("scrollOverviewCargo.TabConstraints.tabTitle"),
                scrollOverviewCargo);

        scrollOverviewCombatPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollOverviewCombatPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
        scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
        scrollOverviewSupportPersonnel.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollOverviewSupportPersonnel.setPreferredSize(new java.awt.Dimension(350, 400));
        scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());

        splitOverviewPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewCombatPersonnel,
                scrollOverviewSupportPersonnel);
        splitOverviewPersonnel.setName("splitOverviewPersonnel");
        splitOverviewPersonnel.setOneTouchExpandable(true);
        splitOverviewPersonnel.setResizeWeight(0.5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getTabOverview().addTab(resourceMap.getString("scrollOverviewPersonnel.TabConstraints.tabTitle"),
                splitOverviewPersonnel);

        scrollOverviewHangar.setViewportView(new HangarReport(getCampaign()).getHangarTree());
        overviewHangarArea.setName("overviewHangarArea"); // NOI18N
        overviewHangarArea.setLineWrap(false);
        overviewHangarArea.setFont(new Font("Courier New", Font.PLAIN, 18));
        overviewHangarArea.setText("");
        overviewHangarArea.setEditable(false);
        overviewHangarArea.setName("overviewHangarArea"); // NOI18N
        splitOverviewHangar = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOverviewHangar, overviewHangarArea);
        splitOverviewHangar.setName("splitOverviewHangar");
        splitOverviewHangar.setOneTouchExpandable(true);
        splitOverviewHangar.setResizeWeight(0.5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getTabOverview().addTab(resourceMap.getString("scrollOverviewHangar.TabConstraints.tabTitle"),
                splitOverviewHangar);

        overviewPartsPanel.setName("overviewPartsPanel"); // NOI18N
        scrollOverviewParts.setViewportView(overviewPartsPanel);
        getTabOverview().addTab(resourceMap.getString("scrollOverviewParts.TabConstraints.tabTitle"),
                scrollOverviewParts);

        rating = UnitRatingFactory.getUnitRating(getCampaign());
        rating.reInitialize();
        scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
        getTabOverview().addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
                scrollOverviewUnitRating);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        // gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(getTabOverview(), gridBagConstraints);
    }

    /**
     * @return the tabOverview
     */
    public JTabbedPane getTabOverview() {
        return tabOverview;
    }

    /**
     * @param tabOverview
     *            the tabOverview to set
     */
    public void setTabOverview(JTabbedPane tabOverview) {
        this.tabOverview = tabOverview;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshOverview();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public GuiTabType tabType() {
        return GuiTabType.OVERVIEW;
    }

    private void initOverviewPartsInUse() {
        overviewPartsPanel = new JPanel(new GridBagLayout());

        overviewPartsModel = new PartsInUseTableModel();
        overviewPartsInUseTable = new JTable(overviewPartsModel);
        overviewPartsInUseTable.setRowSelectionAllowed(false);
        overviewPartsInUseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
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
        partsInUseSorter = new TableRowSorter<PartsInUseTableModel>(overviewPartsModel);
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
        partsInUseSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        overviewPartsInUseTable.setRowSorter(partsInUseSorter);

        // Add buttons and actions. TODO: Only refresh the row we are working
        // on, not the whole table
        @SuppressWarnings("serial")
        Action buy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.valueOf(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().getShoppingList().addShoppingItem(partToBuy, 1, getCampaign());
                getCampaignGui().refreshReport();
                getCampaignGui().refreshAcquireList();
                getCampaignGui().refreshPartsList();
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action buyInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.valueOf(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1, 100);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().getShoppingList().addShoppingItem(partToBuy, quantity, getCampaign());
                getCampaignGui().refreshReport();
                getCampaignGui().refreshAcquireList();
                getCampaignGui().refreshPartsList();
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action add = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.valueOf(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                getCampaign().addPart((Part) partToBuy.getNewEquipment(), 0);
                getCampaignGui().refreshAcquireList();
                getCampaignGui().refreshPartsList();
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };
        @SuppressWarnings("serial")
        Action addInBulk = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = Integer.valueOf(e.getActionCommand());
                PartInUse piu = overviewPartsModel.getPartInUse(row);
                int quantity = 1;
                PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(getFrame(), true,
                        "How Many " + piu.getPartToBuy().getAcquisitionName(), quantity, 1, 100);
                pcd.setVisible(true);
                quantity = pcd.getValue();
                IAcquisitionWork partToBuy = piu.getPartToBuy();
                while (quantity > 0) {
                    getCampaign().addPart((Part) partToBuy.getNewEquipment(), 0);
                    --quantity;
                }
                getCampaignGui().refreshAcquireList();
                getCampaignGui().refreshPartsList();
                refreshOverviewSpecificPart(row, piu, partToBuy);
            }
        };

        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buy, PartsInUseTableModel.COL_BUTTON_BUY);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, buyInBulk,
                PartsInUseTableModel.COL_BUTTON_BUY_BULK);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, add, PartsInUseTableModel.COL_BUTTON_GMADD);
        new PartsInUseTableModel.ButtonColumn(overviewPartsInUseTable, addInBulk,
                PartsInUseTableModel.COL_BUTTON_GMADD_BULK);

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        overviewPartsPanel.add(new JScrollPane(overviewPartsInUseTable), gridBagConstraints);
    }

    public void refreshOverview() {
        int drIndex = getTabOverview().indexOfComponent(scrollOverviewUnitRating);
        if (!getCampaign().getCampaignOptions().useDragoonRating() && drIndex != -1) {
            getTabOverview().removeTabAt(drIndex);
        } else {
            if (drIndex == -1) {
                getTabOverview().addTab(resourceMap.getString("scrollOverviewDragoonsRating.TabConstraints.tabTitle"),
                        scrollOverviewUnitRating);
            }
        }

        scrollOverviewUnitRating.setViewportView(new RatingReport(getCampaign()).getReport());
        scrollOverviewCombatPersonnel.setViewportView(new PersonnelReport(getCampaign()).getCombatPersonnelReport());
        scrollOverviewSupportPersonnel.setViewportView(new PersonnelReport(getCampaign()).getSupportPersonnelReport());
        scrollOverviewTransport.setViewportView(new TransportReport(getCampaign()).getReport());
        scrollOverviewCargo.setViewportView(new CargoReport(getCampaign()).getReport());
        HangarReport hr = new HangarReport(getCampaign());
        overviewHangarArea.setText(hr.getHangarTotals());
        scrollOverviewHangar.setViewportView(hr.getHangarTree());
        refreshOverviewPartsInUse();
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
        PartsInUseTableModel.ButtonColumn column = (ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GMADD)
                .getCellRenderer();
        column.setEnabled(getCampaign().isGM());
        column = (ButtonColumn) tcm.getColumn(PartsInUseTableModel.COL_BUTTON_GMADD_BULK).getCellRenderer();
        column.setEnabled(getCampaign().isGM());
    }

    private ActionScheduler overviewScheduler = new ActionScheduler(this::refreshOverview);
    
    @Subscribe
    public void deploymentChanged(DeploymentChangedEvent ev) {
        overviewScheduler.schedule();
    }
    
    @Subscribe
    public void assignmentChanged(AssignmentChangedEvent ev) {
        overviewScheduler.schedule();
    }
}
