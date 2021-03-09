/*
 * Copyright (c) 2017, 2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.*;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.MekHQOptionsChangedEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonLogEvent;
import mekhq.campaign.event.PersonNewEvent;
import mekhq.campaign.event.PersonRemovedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitRemovedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.gui.adapter.PersonnelTableMouseAdapter;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JTablePreference;
import mekhq.gui.preferences.JToggleButtonPreference;
import mekhq.gui.sorter.*;
import mekhq.gui.view.PersonViewPanel;
import mekhq.preferences.PreferencesNode;

/**
 * Tab for interacting with all personnel
 */
public final class PersonnelTab extends CampaignGuiTab {
    private static final long serialVersionUID = -4389102971116114869L;

    public static final int PERSONNEL_VIEW_WIDTH = 500;

    private JSplitPane splitPersonnel;
    private JTable personnelTable;
    private JComboBox<PersonnelFilter> choicePerson;
    private JComboBox<PersonnelTabView> choicePersonView;
    private JScrollPane scrollPersonnelView;
    private JCheckBox chkGroupByUnit;

    private PersonnelTableModel personModel;
    private TableRowSorter<PersonnelTableModel> personnelSorter;

    PersonnelTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.PERSONNEL;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblPersonChoice.text")), gridBagConstraints);

        choicePerson = new JComboBox<>(createPersonGroupModel());
        choicePerson.setSelectedIndex(0);
        choicePerson.addActionListener(ev -> filterPersonnel());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(choicePerson, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblPersonView.text")), gridBagConstraints);

        DefaultComboBoxModel<PersonnelTabView> personViewModel = new DefaultComboBoxModel<>(PersonnelTabView.values());
        choicePersonView = new JComboBox<>(personViewModel);
        choicePersonView.setSelectedIndex(PersonnelTabView.GENERAL.ordinal());
        choicePersonView.addActionListener(ev -> changePersonnelView());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        add(choicePersonView, gridBagConstraints);

        chkGroupByUnit = new JCheckBox(resourceMap.getString("chkGroupByUnit.text"));
        chkGroupByUnit.setToolTipText(resourceMap.getString("chkGroupByUnit.toolTipText"));
        chkGroupByUnit.addActionListener(e -> {
            personModel.setGroupByUnit(chkGroupByUnit.isSelected());
            personModel.refreshData();
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(chkGroupByUnit, gridBagConstraints);

        personModel = new PersonnelTableModel(getCampaign());
        personnelTable = new JTable(personModel);
        personnelTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        XTableColumnModel personColumnModel = new XTableColumnModel();
        personnelTable.setColumnModel(personColumnModel);
        personnelTable.createDefaultColumnsFromModel();
        personnelSorter = new TableRowSorter<>(personModel);
        personnelSorter.setComparator(PersonnelTableModel.COL_RANK, new RankSorter(getCampaign()));
        personnelSorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        for (int i = PersonnelTableModel.COL_MECH; i < PersonnelTableModel.N_COL; i++) {
            personnelSorter.setComparator(i, new BonusSorter());
        }
        personnelSorter.setComparator(PersonnelTableModel.COL_SALARY, new FormattedNumberSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_AGE, new FormattedNumberSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_RECRUIT_DATE, new DateStringComparator());
        personnelSorter.setComparator(PersonnelTableModel.COL_DEATH_DATE, new DateStringComparator());
        personnelSorter.setComparator(PersonnelTableModel.COL_ORIGIN_FACTION, new NaturalOrderComparator());
        personnelSorter.setComparator(PersonnelTableModel.COL_ORIGIN_PLANET, new NaturalOrderComparator());
        personnelTable.setRowSorter(personnelSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_RANK, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_SKILL, SortOrder.DESCENDING));
        personnelSorter.setSortKeys(sortKeys);
        TableColumn column;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = personnelTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(personModel.getColumnWidth(i));
            column.setCellRenderer(personModel.getRenderer(PersonnelTabView.GRAPHIC
                    .equals(choicePersonView.getSelectedItem())));
        }
        personnelTable.setIntercellSpacing(new Dimension(0, 0));
        personnelTable.setShowGrid(false);
        changePersonnelView();
        personnelTable.getSelectionModel().addListSelectionListener(ev -> refreshPersonnelView());

        scrollPersonnelView = new JScrollPane();
        scrollPersonnelView.setMinimumSize(new java.awt.Dimension(PERSONNEL_VIEW_WIDTH, 600));
        scrollPersonnelView.setPreferredSize(new java.awt.Dimension(PERSONNEL_VIEW_WIDTH, 600));
        scrollPersonnelView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPersonnelView.setViewportView(null);

        JScrollPane scrollPersonnelTable = new JScrollPane(personnelTable);
        splitPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPersonnelTable,
                scrollPersonnelView);
        splitPersonnel.setOneTouchExpandable(true);
        splitPersonnel.setResizeWeight(1.0);
        splitPersonnel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshPersonnelView());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitPersonnel, gridBagConstraints);

        PersonnelTableMouseAdapter.connect(getCampaignGui(), personnelTable, personModel, splitPersonnel);

        filterPersonnel();
    }

    private DefaultComboBoxModel<PersonnelFilter> createPersonGroupModel() {
        DefaultComboBoxModel<PersonnelFilter> personGroupModel = new DefaultComboBoxModel<>();

        for (PersonnelFilter filter : MekHQ.getMekHQOptions().getPersonnelFilterStyle().getFilters(false)) {
            personGroupModel.addElement(filter);
        }

        return personGroupModel;
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(PersonnelTab.class);

        choicePerson.setName("personnelType");
        preferences.manage(new JComboBoxPreference(choicePerson));

        choicePersonView.setName("personnelView");
        preferences.manage(new JComboBoxPreference(choicePersonView));

        chkGroupByUnit.setName("groupByUnit");
        preferences.manage(new JToggleButtonPreference(chkGroupByUnit));

        personnelTable.setName("personnelTable");
        preferences.manage(new JTablePreference(personnelTable));
    }

    /* For export */
    public JTable getPersonnelTable() {
        return personnelTable;
    }

    public PersonnelTableModel getPersonModel() {
        return personModel;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshPersonnelList();
        changePersonnelView();
    }

    public void filterPersonnel() {
        PersonnelFilter nGroup = (choicePerson.getSelectedItem() != null)
                ? (PersonnelFilter) choicePerson.getSelectedItem()
                : PersonnelFilter.ACTIVE;
        personnelSorter.setRowFilter(new RowFilter<PersonnelTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                return nGroup.getFilteredInformation(entry.getModel().getPerson(entry.getIdentifier()));
            }
        });
    }

    private void changePersonnelView() {
        Object tempView = choicePersonView.getSelectedItem();
        PersonnelTabView view = (tempView != null) ? (PersonnelTabView) tempView : PersonnelTabView.GENERAL;
        XTableColumnModel columnModel = (XTableColumnModel) personnelTable.getColumnModel();
        personnelTable.setRowHeight(15);

        // set the renderer
        TableColumn column;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(personModel.getRenderer(
                    PersonnelTabView.GRAPHIC == choicePersonView.getSelectedItem()));
            if (i == PersonnelTableModel.COL_RANK) {
                if (view == PersonnelTabView.GRAPHIC) {
                    column.setPreferredWidth(125);
                    column.setHeaderValue("Person");
                } else {
                    column.setPreferredWidth(personModel.getColumnWidth(i));
                    column.setHeaderValue("Rank");
                }
            }
        }

        switch (view) {
            case GRAPHIC: {
                personnelTable.setRowHeight(80);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case GENERAL: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), getCampaign().getCampaignOptions().payForSalaries());
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case PILOT_GUNNERY_SKILLS: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case INFANTRY_SKILLS: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case TACTICAL_SKILLS: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case TECHNICAL_SKILLS: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case ADMINISTRATIVE_SKILLS: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
            case BIOGRAPHICAL: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), getCampaignOptions().showOriginFaction());
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), getCampaignOptions().showOriginFaction());
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), true);
                break;
            }
            case FLUFF: {
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GIVEN_NAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_LAST_NAME), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SURNAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HONORIFIC), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_BLOODNAME), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STATUS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GENDER), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TYPE), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SKILL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ASSIGN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_FORCE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEPLOY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_JET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SPACE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NVEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_VTOL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_GUN_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SMALL_ARMS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ANTI_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ARTY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TACTICS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_STRATEGY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_MECH), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_AERO), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_VEE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TECH_BA), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_MEDICAL), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ADMIN), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NEG), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SCROUNGE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), true);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_FACTION), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_ORIGIN_PLANET), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RECRUIT_DATE), false);
                columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_DEATH_DATE), false);
                break;
            }
        }

        //region Export Only Columns
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
        columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_PORTRAIT), false);
        //endregion Export Only Columns
    }

    public void focusOnPerson(UUID id) {
        splitPersonnel.resetToPreferredSizes();
        int row = -1;
        for (int i = 0; i < personnelTable.getRowCount(); i++) {
            if (personModel.getPerson(personnelTable.convertRowIndexToModel(i)).getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row == -1) {
            // try expanding the filter to all units
            choicePerson.setSelectedIndex(0);
            for (int i = 0; i < personnelTable.getRowCount(); i++) {
                if (personModel.getPerson(personnelTable.convertRowIndexToModel(i)).getId().equals(id)) {
                    row = i;
                    break;
                }
            }

        }
        if (row != -1) {
            personnelTable.setRowSelectionInterval(row, row);
            personnelTable.scrollRectToVisible(personnelTable.getCellRect(row, 0, true));
        }
    }

    /**
     * Refreshes personnel table model.
     */
    public void refreshPersonnelList() {
        UUID selectedUUID = null;
        int selectedRow = personnelTable.getSelectedRow();
        if (selectedRow != -1) {
            Person p = personModel.getPerson(personnelTable.convertRowIndexToModel(selectedRow));
            if (null != p) {
                selectedUUID = p.getId();
            }
        }
        personModel.refreshData();
        // try to put the focus back on same person if they are still available
        for (int row = 0; row < personnelTable.getRowCount(); row++) {
            Person p = personModel.getPerson(personnelTable.convertRowIndexToModel(row));
            if (p.getId().equals(selectedUUID)) {
                personnelTable.setRowSelectionInterval(row, row);
                refreshPersonnelView();
                break;
            }
        }
        filterPersonnel();
    }

    public void refreshPersonnelView() {
        int row = personnelTable.getSelectedRow();
        if (row < 0) {
            scrollPersonnelView.setViewportView(null);
            return;
        }
        Person selectedPerson = personModel.getPerson(personnelTable.convertRowIndexToModel(row));
        scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson, getCampaign(), getCampaignGui()));
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(() -> scrollPersonnelView.getVerticalScrollBar().setValue(0));
    }

    private ActionScheduler personnelListScheduler = new ActionScheduler(this::refreshPersonnelList);
    private ActionScheduler filterPersonnelScheduler = new ActionScheduler(this::filterPersonnel);

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        changePersonnelView();
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(MekHQOptionsChangedEvent evt) {
        choicePerson.setModel(createPersonGroupModel());
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(DeploymentChangedEvent ev) {
        filterPersonnelScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonChangedEvent ev) {
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonNewEvent ev) {
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonRemovedEvent ev) {
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonLogEvent ev) {
        refreshPersonnelView();
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        personnelListScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitRemovedEvent ev) {
        filterPersonnelScheduler.schedule();
    }

    @Subscribe
    public void handle(PartWorkEvent ev) {
        filterPersonnelScheduler.schedule();
    }

    @Subscribe
    public void handle(OvertimeModeEvent ev) {
        filterPersonnelScheduler.schedule();
    }
}
