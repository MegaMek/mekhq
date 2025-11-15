/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.models.XTableColumnModel;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.AcquisitionEvent;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.events.OvertimeModeEvent;
import mekhq.campaign.events.RepairStatusChangedEvent;
import mekhq.campaign.events.parts.PartEvent;
import mekhq.campaign.events.parts.PartWorkEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.events.units.UnitNewEvent;
import mekhq.campaign.events.units.UnitRemovedEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.gui.adapter.UnitTableMouseAdapter;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.PersonTitleStringSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.gui.sorter.WeightClassSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.view.UnitViewPanel;

/**
 * Displays a table of all units in the force.
 */
public final class HangarTab extends CampaignGuiTab {
    private static final MMLogger LOGGER = MMLogger.create(HangarTab.class);

    public static final int UNIT_VIEW_WIDTH = 600;

    // unit views
    private static final int UV_GRAPHIC = 0;
    private static final int UV_GENERAL = 1;
    private static final int UV_DETAILS = 2;
    private static final int UV_STATUS = 3;
    private static final int UV_TRANSPORT = 4;
    private static final int UV_NUM = 5;


    private JSplitPane splitUnit;
    private JTable unitTable;
    private JComboBox<String> choiceUnit;
    private JComboBox<String> choiceUnitView;
    private JCheckBox chkHideMothballed;
    private JScrollPane scrollUnitView;

    private UnitTableModel unitModel;
    private TableRowSorter<UnitTableModel> unitSorter;

    private final IPreferenceChangeListener scalingChangeListener = e -> changeUnitView();

    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    // region Constructors
    public HangarTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
        GUIPreferences.getInstance().addPreferenceChangeListener(scalingChangeListener);
    }
    // endregion Constructors

    @Override
    public MHQTabType tabType() {
        return MHQTabType.HANGAR;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblUnitChoice.text")), gridBagConstraints);

        DefaultComboBoxModel<String> unitGroupModel = new DefaultComboBoxModel<>();
        unitGroupModel.addElement("All Units");
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitGroupModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        unitGroupModel.addElement(resourceMap.getString("choiceUnit.ActiveUnits.filter"));
        unitGroupModel.addElement(resourceMap.getString("choiceUnit.MothballedUnits.filter"));
        unitGroupModel.addElement(resourceMap.getString("choiceUnit.UnmaintainedUnits.filter"));
        choiceUnit = new JComboBox<>(unitGroupModel);
        choiceUnit.setSelectedIndex(0);
        choiceUnit.addActionListener(ev -> filterUnits());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(choiceUnit, gridBagConstraints);

        chkHideMothballed = new JCheckBox(resourceMap.getString("chkHideMothballed.text"));
        chkHideMothballed.setToolTipText(resourceMap.getString("chkHideMothballed.toolTipText"));
        chkHideMothballed.addActionListener(ev -> filterUnits());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(chkHideMothballed, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblUnitView.text")), gridBagConstraints);

        DefaultComboBoxModel<String> unitViewModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < UV_NUM; i++) {
            unitViewModel.addElement(getUnitViewName(i));
        }
        choiceUnitView = new JComboBox<>(unitViewModel);
        choiceUnitView.setSelectedIndex(UV_GENERAL);
        choiceUnitView.addActionListener(ev -> changeUnitView());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(choiceUnitView, gridBagConstraints);

        unitModel = new UnitTableModel(getCampaign());
        unitTable = new JTable(unitModel);
        unitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        XTableColumnModel unitColumnModel = new XTableColumnModel();
        unitTable.setColumnModel(unitColumnModel);
        unitTable.createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<>(unitModel);
        unitSorter.setComparator(UnitTableModel.COL_NAME, new NaturalOrderComparator());
        unitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        unitSorter.setComparator(UnitTableModel.COL_WEIGHT_CLASS, new WeightClassSorter());
        unitSorter.setComparator(UnitTableModel.COL_COST, new FormattedNumberSorter());
        unitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        unitSorter.setComparator(UnitTableModel.COL_PILOT, new PersonTitleStringSorter(getCampaign()));
        unitSorter.setComparator(UnitTableModel.COL_TECH_CRW, new PersonTitleStringSorter(getCampaign()));
        unitSorter.setComparator(UnitTableModel.COL_MAINTAIN, new FormattedNumberSorter());
        unitSorter.setComparator(UnitTableModel.COL_MAINTAIN_CYCLE, new NaturalOrderComparator());
        unitTable.setRowSorter(unitSorter);
        List<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(UnitTableModel.COL_TYPE, SortOrder.DESCENDING));
        sortKeys.add(new SortKey(UnitTableModel.COL_WEIGHT_CLASS, SortOrder.DESCENDING));
        unitSorter.setSortKeys(sortKeys);
        unitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = unitTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(unitModel.getColumnWidth(i));
            column.setCellRenderer(unitModel.getRenderer(choiceUnitView.getSelectedIndex() == UV_GRAPHIC));
        }
        unitTable.setIntercellSpacing(new Dimension(0, 0));
        unitTable.setShowGrid(false);
        changeUnitView();
        unitTable.getSelectionModel().addListSelectionListener(ev -> refreshUnitView());

        JScrollPane scrollUnitTable = new JScrollPaneWithSpeed(unitTable);
        scrollUnitTable.setFocusable(false);
        scrollUnitTable.setBorder(RoundedLineBorder.createRoundedLineBorder());

        JPanel pnlTutorial = new TutorialHyperlinkPanel("hangarTab");

        JPanel tableAndInfoPanel = new JPanel(new BorderLayout());
        tableAndInfoPanel.add(scrollUnitTable, BorderLayout.CENTER);
        tableAndInfoPanel.add(pnlTutorial, BorderLayout.SOUTH);

        scrollUnitView = new JScrollPaneWithSpeed();
        scrollUnitView.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollUnitView.setMinimumSize(new Dimension(UNIT_VIEW_WIDTH, 600));
        scrollUnitView.setPreferredSize(new Dimension(UNIT_VIEW_WIDTH, 600));
        scrollUnitView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollUnitView.setViewportView(null);

        splitUnit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableAndInfoPanel, scrollUnitView);
        splitUnit.setOneTouchExpandable(true);
        splitUnit.setResizeWeight(1.0);
        splitUnit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, pce -> refreshUnitView());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitUnit, gridBagConstraints);

        UnitTableMouseAdapter.connect(getCampaignGui(), unitTable, unitModel, splitUnit);
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(HangarTab.class);

            choiceUnit.setName("unitType");
            preferences.manage(new JComboBoxPreference(choiceUnit));

            chkHideMothballed.setName("hideMothballed");
            preferences.manage(new JToggleButtonPreference(chkHideMothballed));

            choiceUnitView.setName("unitView");
            preferences.manage(new JComboBoxPreference(choiceUnitView));

            unitTable.setName("unitTable");
            preferences.manage(new JTablePreference(unitTable));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
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
        final int nGroup = choiceUnit.getSelectedIndex() - 1;
        final boolean hideMothballed = chkHideMothballed.isSelected();
        RowFilter<UnitTableModel, Integer> unitTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends UnitTableModel, ? extends Integer> entry) {
                UnitTableModel unitModel = entry.getModel();
                Unit unit = unitModel.getUnit(entry.getIdentifier());

                // Apply "Hide Mothballed" checkbox filter first
                if (hideMothballed && unit.isMothballed()) {
                    return false;
                }

                // If "All Units" is selected, show all (unless filtered by checkbox above)
                if (nGroup < 0) {
                    return true;
                }

                if (nGroup < UnitType.SIZE) {
                    Entity en = unit.getEntity();
                    int type = -1;
                    if (en != null) {
                        type = en.getUnitType();
                    }
                    return type == nGroup;
                } else if (resourceMap.getString("choiceUnit.ActiveUnits.filter")
                                 .equals(choiceUnit.getSelectedItem())) {
                    return !unit.isMothballed();
                } else if (resourceMap.getString("choiceUnit.MothballedUnits.filter")
                                 .equals(choiceUnit.getSelectedItem())) {
                    return unit.isMothballed();
                } else if (resourceMap.getString("choiceUnit.UnmaintainedUnits.filter")
                                 .equals(choiceUnit.getSelectedItem())) {
                    return unit.isUnmaintained();
                } else {
                    return false;
                }
            }
        };
        unitSorter.setRowFilter(unitTypeFilter);
        refreshUnitView();
    }

    public static String getUnitViewName(int group) {
        return switch (group) {
            case UV_GRAPHIC -> "Graphic";
            case UV_GENERAL -> "General";
            case UV_DETAILS -> "Details";
            case UV_STATUS -> "Status";
            case UV_TRANSPORT -> "Transport";
            default -> "?";
        };
    }

    public void changeUnitView() {
        int view = choiceUnitView.getSelectedIndex();
        XTableColumnModel columnModel = (XTableColumnModel) unitTable.getColumnModel();
        unitTable.setRowHeight(UIUtil.scaleForGUI(15));

        // set the renderer
        TableColumn column;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(unitModel.getRenderer(choiceUnitView.getSelectedIndex() == UV_GRAPHIC));
            if (i == UnitTableModel.COL_WEIGHT_CLASS) {
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
            unitTable.setRowHeight(UIUtil.scaleForGUI(60));
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT_CLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CONDITION), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW_STATE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_FORCE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN_CYCLE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SHIP_TRANSPORT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TAC_TRANSPORT), false);
        } else if (view == UV_GENERAL) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT_CLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CONDITION), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW_STATE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_FORCE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN_CYCLE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SHIP_TRANSPORT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TAC_TRANSPORT), false);
        } else if (view == UV_DETAILS) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT_CLASS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CONDITION), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW_STATE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_FORCE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN_CYCLE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS),
                  getCampaign().getCampaignOptions().isUseQuirks());
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SHIP_TRANSPORT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TAC_TRANSPORT), false);
        } else if (view == UV_STATUS) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT_CLASS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CONDITION), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW_STATE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_FORCE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN),
                  getCampaign().getCampaignOptions().isPayForMaintain());
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN_CYCLE),
                  getCampaign().getCampaignOptions().isCheckMaintenance());
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SHIP_TRANSPORT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TAC_TRANSPORT), false);
        } else if (view == UV_TRANSPORT) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TYPE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT_CLASS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_WEIGHT), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_COST), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_STATUS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CONDITION), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW_STATE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUALITY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PILOT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_FORCE), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_CREW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TECH_CRW), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MAINTAIN_CYCLE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_BV), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_REPAIR), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_PARTS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SITE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_QUIRKS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_MODE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_SHIP_TRANSPORT), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(UnitTableModel.COL_TAC_TRANSPORT), true);
        }
    }

    @Override
    public void disposeTab() {
        super.disposeTab();
        GUIPreferences.getInstance().removePreferenceChangeListener(scalingChangeListener);
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
        scrollUnitView.setViewportView(new UnitViewPanel(selectedUnit, getCampaign()));
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        SwingUtilities.invokeLater(() -> scrollUnitView.getVerticalScrollBar().setValue(0));
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
        unitModel.setData(new ArrayList<>(getCampaign().getHangar().getUnits()));
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
    }

    private final ActionScheduler unitListScheduler = new ActionScheduler(this::refreshUnitList);
    private final ActionScheduler filterUnitScheduler = new ActionScheduler(this::filterUnits);

    @Subscribe
    public void handle(DeploymentChangedEvent ev) {
        filterUnitScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonChangedEvent ev) {
        filterUnitScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        unitListScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitChangedEvent ev) {
        filterUnitScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitNewEvent ev) {
        unitListScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitRemovedEvent ev) {
        unitListScheduler.schedule();
    }

    @Subscribe
    public void handle(RepairStatusChangedEvent ev) {
        filterUnitScheduler.schedule();
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        if (ev.getAcquisition() instanceof UnitOrder) {
            unitListScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(PartEvent ev) {
        if (ev.getPart().getUnit() != null) {
            filterUnitScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(PartWorkEvent ev) {
        if (ev.getPartWork().getUnit() != null) {
            filterUnitScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(OvertimeModeEvent ev) {
        filterUnitScheduler.schedule();
    }
}
