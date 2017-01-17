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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.Entity;
import megamek.common.UnitType;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.AssignmentChangedEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.gui.adapter.ProcurementTableMouseAdapter;
import mekhq.gui.adapter.UnitTableMouseAdapter;
import mekhq.gui.model.ProcurementTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.TargetSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.gui.sorter.WeightClassSorter;
import mekhq.gui.view.UnitViewPanel;

/**
 * Displays table of all units in the force.
 *
 */
public final class HangarTab extends CampaignGuiTab {

    private static final long serialVersionUID = -5636638711420905602L;

    public static final int UNIT_VIEW_WIDTH = 450;

    // unit views
    private static final int UV_GRAPHIC = 0;
    private static final int UV_GENERAL = 1;
    private static final int UV_DETAILS = 2;
    private static final int UV_STATUS = 3;
    private static final int UV_NUM = 4;

    private JSplitPane splitUnit;
    private JTable unitTable;
    private JTable acquireUnitsTable;
    private JComboBox<String> choiceUnit;
    private JComboBox<String> choiceUnitView;
    private JScrollPane scrollUnitView;

    private UnitTableModel unitModel;
    private ProcurementTableModel acquireUnitsModel;
    private TableRowSorter<UnitTableModel> unitSorter;

    HangarTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.HANGAR;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblUnitChoice.text")), //$NON-NLS-1$ ;
                gridBagConstraints);

        DefaultComboBoxModel<String> unitGroupModel = new DefaultComboBoxModel<String>();
        unitGroupModel.addElement("All Units");
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitGroupModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        choiceUnit = new JComboBox<String>(unitGroupModel);
        choiceUnit.setSelectedIndex(0);
        choiceUnit.addActionListener(ev -> filterUnits());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(choiceUnit, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblUnitView.text")), //$NON-NLS-1$ ;
                gridBagConstraints);

        DefaultComboBoxModel<String> unitViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < UV_NUM; i++) {
            unitViewModel.addElement(getUnitViewName(i));
        }
        choiceUnitView = new JComboBox<String>(unitViewModel);
        choiceUnitView.setSelectedIndex(UV_GENERAL);
        choiceUnitView.addActionListener(ev -> changeUnitView());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(choiceUnitView, gridBagConstraints);

        unitModel = new UnitTableModel(getCampaign());
        unitTable = new JTable(unitModel);
        unitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        XTableColumnModel unitColumnModel = new XTableColumnModel();
        unitTable.setColumnModel(unitColumnModel);
        unitTable.createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<UnitTableModel>(unitModel);
        unitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        unitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        unitSorter.setComparator(UnitTableModel.COL_WCLASS, new WeightClassSorter());
        unitSorter.setComparator(UnitTableModel.COL_COST, new FormattedNumberSorter());
        unitTable.setRowSorter(unitSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_TYPE, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_WCLASS, SortOrder.DESCENDING));
        unitSorter.setSortKeys(sortKeys);
        unitTable.addMouseListener(new UnitTableMouseAdapter(getCampaignGui(),
                unitTable, unitModel) {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if ((splitUnit.getSize().width - splitUnit.getDividerLocation() + splitUnit
                            .getDividerSize()) < HangarTab.UNIT_VIEW_WIDTH) {
                        // expand
                        splitUnit.resetToPreferredSizes();
                    } else {
                        // collapse
                        splitUnit.setDividerLocation(1.0);
                    }

                }
            }            
        });
        unitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = unitTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(unitModel.getColumnWidth(i));
            column.setCellRenderer(
                    unitModel.getRenderer(choiceUnitView.getSelectedIndex() == UV_GRAPHIC, getIconPackage()));
        }
        unitTable.setIntercellSpacing(new Dimension(0, 0));
        unitTable.setShowGrid(false);
        changeUnitView();
        unitTable.getSelectionModel().addListSelectionListener(ev -> refreshUnitView());

        acquireUnitsModel = new ProcurementTableModel(getCampaign());
        acquireUnitsTable = new JTable(acquireUnitsModel);
        TableRowSorter<ProcurementTableModel> acquireUnitsSorter = new TableRowSorter<ProcurementTableModel>(
                acquireUnitsModel);
        acquireUnitsSorter.setComparator(ProcurementTableModel.COL_COST, new FormattedNumberSorter());
        acquireUnitsSorter.setComparator(ProcurementTableModel.COL_TARGET, new TargetSorter());
        acquireUnitsTable.setRowSorter(acquireUnitsSorter);
        column = null;
        for (int i = 0; i < ProcurementTableModel.N_COL; i++) {
            column = acquireUnitsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(acquireUnitsModel.getColumnWidth(i));
            column.setCellRenderer(acquireUnitsModel.getRenderer());
        }
        acquireUnitsTable.setIntercellSpacing(new Dimension(0, 0));
        acquireUnitsTable.setShowGrid(false);
        acquireUnitsTable.addMouseListener(new ProcurementTableMouseAdapter(getCampaignGui()));
        acquireUnitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0),
                "ADD");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                "REMOVE");
        acquireUnitsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
                "REMOVE");

        acquireUnitsTable.getActionMap().put("ADD", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = 4958203340754214211L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (acquireUnitsTable.getSelectedRow() < 0) {
                    return;
                }
                acquireUnitsModel
                        .incrementItem(acquireUnitsTable.convertRowIndexToModel(acquireUnitsTable.getSelectedRow()));
            }
        });

        acquireUnitsTable.getActionMap().put("REMOVE", new AbstractAction() {
            /**
             *
             */
            private static final long serialVersionUID = -8377486575329708963L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (acquireUnitsTable.getSelectedRow() < 0) {
                    return;
                }
                if (acquireUnitsModel
                        .getAcquisition(acquireUnitsTable.convertRowIndexToModel(acquireUnitsTable.getSelectedRow()))
                        .getQuantity() > 0) {
                    acquireUnitsModel.decrementItem(
                            acquireUnitsTable.convertRowIndexToModel(acquireUnitsTable.getSelectedRow()));
                }
            }
        });

        JScrollPane scrollAcquireUnitTable = new JScrollPane(acquireUnitsTable);
        JPanel panAcquireUnit = new JPanel(new GridLayout(0, 1));
        panAcquireUnit.setBorder(BorderFactory.createTitledBorder("Procurement List"));
        panAcquireUnit.add(scrollAcquireUnitTable);
        panAcquireUnit.setMinimumSize(new Dimension(200, 200));
        panAcquireUnit.setPreferredSize(new Dimension(200, 200));

        JSplitPane splitLeftUnit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(unitTable),
                panAcquireUnit);
        splitLeftUnit.setOneTouchExpandable(true);
        splitLeftUnit.setResizeWeight(1.0);

        scrollUnitView = new JScrollPane();
        scrollUnitView.setMinimumSize(new java.awt.Dimension(UNIT_VIEW_WIDTH, 600));
        scrollUnitView.setPreferredSize(new java.awt.Dimension(UNIT_VIEW_WIDTH, 600));
        scrollUnitView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollUnitView.setViewportView(null);

        splitUnit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitLeftUnit, scrollUnitView);
        splitUnit.setOneTouchExpandable(true);
        splitUnit.setResizeWeight(1.0);
        splitUnit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, pce -> refreshUnitView());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitUnit, gridBagConstraints);
    }
    
    /* For export */
    public JTable getUnitTable() {
        return unitTable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshUnitList();
    }

    public void filterUnits() {
        RowFilter<UnitTableModel, Integer> unitTypeFilter = null;
        final int nGroup = choiceUnit.getSelectedIndex() - 1;
        unitTypeFilter = new RowFilter<UnitTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends UnitTableModel, ? extends Integer> entry) {
                if (nGroup < 0) {
                    return true;
                }
                UnitTableModel unitModel = entry.getModel();
                Unit unit = unitModel.getUnit(entry.getIdentifier());
                Entity en = unit.getEntity();
                int type = -1;
                if (null != en) {
                    type = UnitType.determineUnitTypeCode(en);
                }
                return type == nGroup;
            }
        };
        unitSorter.setRowFilter(unitTypeFilter);
    }

    public static String getUnitViewName(int group) {
        switch (group) {
        case UV_GRAPHIC:
            return "Graphic";
        case UV_GENERAL:
            return "General";
        case UV_DETAILS:
            return "Details";
        case UV_STATUS:
            return "Status";
        default:
            return "?";
        }
    }

    public void changeUnitView() {

        int view = choiceUnitView.getSelectedIndex();
        XTableColumnModel columnModel = (XTableColumnModel) unitTable.getColumnModel();
        unitTable.setRowHeight(15);

        // set the renderer
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(
                    unitModel.getRenderer(choiceUnitView.getSelectedIndex() == UV_GRAPHIC, getIconPackage()));
            if (i == UnitTableModel.COL_WCLASS) {
                if (view == UV_GRAPHIC) {
                    column.setPreferredWidth(125);
                    column.setHeaderValue("Unit");
                } else {
                    column.setPreferredWidth(unitModel.getColumnWidth(i));
                    column.setHeaderValue("Weight Class");
                }
            }
        }

        if (view == UV_GRAPHIC) {
            unitTable.setRowHeight(80);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_RSTATUS), false);
        } else if (view == UV_GENERAL) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_RSTATUS), false);
        } else if (view == UV_DETAILS) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_RSTATUS), false);
        } else if (view == UV_STATUS) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WCLASS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN),
                    getCampaign().getCampaignOptions().payForMaintain());
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_RSTATUS), false);
        }
    }

    public void focusOnUnit(UUID id) {
        splitUnit.resetToPreferredSizes();
        int row = -1;
        for (int i = 0; i < unitTable.getRowCount(); i++) {
            if (unitModel.getUnit(unitTable.convertRowIndexToModel(i)).getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row == -1) {
            // try expanding the filter to all units
            choiceUnit.setSelectedIndex(0);
            for (int i = 0; i < unitTable.getRowCount(); i++) {
                if (unitModel.getUnit(unitTable.convertRowIndexToModel(i)).getId().equals(id)) {
                    row = i;
                    break;
                }
            }

        }
        if (row != -1) {
            unitTable.setRowSelectionInterval(row, row);
            unitTable.scrollRectToVisible(unitTable.getCellRect(row, 0, true));
        }
    }

    public void refreshUnitView() {
        int row = unitTable.getSelectedRow();
        if (row < 0) {
            scrollUnitView.setViewportView(null);
            return;
        }
        Unit selectedUnit = unitModel.getUnit(unitTable.convertRowIndexToModel(row));
        scrollUnitView.setViewportView(new UnitViewPanel(selectedUnit, getCampaign(), getIconPackage().getCamos(),
                getIconPackage().getMechTiles()));
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(() -> scrollUnitView.getVerticalScrollBar().setValue(0));
    }

    public void refreshUnitList() {
        UUID selectedUUID = null;
        int selectedRow = unitTable.getSelectedRow();
        if (selectedRow != -1) {
            Unit u = unitModel.getUnit(unitTable.convertRowIndexToModel(selectedRow));
            if (null != u) {
                selectedUUID = u.getId();
            }
        }
        unitModel.setData(getCampaign().getUnits());
        // try to put the focus back on same person if they are still available
        for (int row = 0; row < unitTable.getRowCount(); row++) {
            Unit u = unitModel.getUnit(unitTable.convertRowIndexToModel(row));
            if (u.getId().equals(selectedUUID)) {
                unitTable.setRowSelectionInterval(row, row);
                refreshUnitView();
                break;
            }
        }
        getCampaignGui().refreshLab();
        getCampaignGui().refreshRating();
    }
    
    private void refreshAcquisitionList() {
        acquireUnitsModel.setData(getCampaign().getShoppingList().getUnitList());
    }

    private ActionScheduler unitListScheduler = new ActionScheduler(this::refreshUnitList);
    private ActionScheduler acquisitionListScheduler = new ActionScheduler(this::refreshAcquisitionList);

    @Subscribe
    public void deploymentChanged(DeploymentChangedEvent ev) {
        unitListScheduler.schedule();;
    }
    
    @Subscribe
    public void assignmentChanged(AssignmentChangedEvent ev) {
        unitListScheduler.schedule();;
    }
    
    @Subscribe
    public void scenarioResolved(ScenarioResolvedEvent ev) {
        unitListScheduler.schedule();
    }

    @Subscribe
    public void unitHandler(UnitEvent ev) {
        unitListScheduler.schedule();
    }
    
    @Subscribe
    public void acquisition(AcquisitionEvent ev) {
        if (ev.getAcquisition() instanceof UnitOrder) {
            unitListScheduler.schedule();
        }
    }
    
    @Subscribe
    public void procurement(ProcurementEvent ev) {
        if (ev.getAcquisition() instanceof UnitOrder) {
            acquisitionListScheduler.schedule();
        }
    }
}
