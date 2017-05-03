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
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.DefaultComboBoxModel;
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
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
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
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.BonusSorter;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.sorter.RankSorter;
import mekhq.gui.view.PersonViewPanel;

/**
 * Tab for interacting with all personnel
 *
 */
public final class PersonnelTab extends CampaignGuiTab {

    private static final long serialVersionUID = -4389102971116114869L;

    // personnel filter groups
    public static final int PG_ACTIVE = 0;
    public static final int PG_COMBAT = 1;
    public static final int PG_SUPPORT = 2;
    public static final int PG_MW = 3;
    public static final int PG_CREW = 4;
    public static final int PG_PILOT = 5;
    public static final int PG_CPILOT = 6;
    public static final int PG_PROTO = 7;
    public static final int PG_BA = 8;
    public static final int PG_SOLDIER = 9;
    public static final int PG_VESSEL = 10;
    public static final int PG_TECH = 11;
    public static final int PG_DOC = 12;
    public static final int PG_ADMIN = 13;
    public static final int PG_DEPENDENT = 14;
    public static final int PG_PRISONER = 15;
    public static final int PG_RETIRE = 16;
    public static final int PG_MIA = 17;
    public static final int PG_KIA = 18;
    public static final int PG_NUM = 19;

    // personnel views
    private static final int PV_GRAPHIC = 0;
    private static final int PV_GENERAL = 1;
    private static final int PV_PILOT = 2;
    private static final int PV_INF = 3;
    private static final int PV_TACTIC = 4;
    private static final int PV_TECH = 5;
    private static final int PV_ADMIN = 6;
    private static final int PV_FLUFF = 7;
    private static final int PV_NUM = 8;

    public static final int PERSONNEL_VIEW_WIDTH = 500;

    private JSplitPane splitPersonnel;
    private JTable personnelTable;
    private JComboBox<String> choicePerson;
    private JComboBox<String> choicePersonView;
    private JScrollPane scrollPersonnelView;

    private PersonnelTableModel personModel;
    private TableRowSorter<PersonnelTableModel> personnelSorter;

    PersonnelTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
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
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblPersonChoice.text")), //$NON-NLS-1$ ;
                gridBagConstraints);

        DefaultComboBoxModel<String> personGroupModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < PG_NUM; i++) {
            personGroupModel.addElement(getPersonnelGroupName(i));
        }
        choicePerson = new JComboBox<String>(personGroupModel);
        choicePerson.setSelectedIndex(0);
        choicePerson.addActionListener(ev -> filterPersonnel());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(choicePerson, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(new JLabel(resourceMap.getString("lblPersonView.text")), gridBagConstraints);

        DefaultComboBoxModel<String> personViewModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < PV_NUM; i++) {
            personViewModel.addElement(getPersonnelViewName(i));
        }
        choicePersonView = new JComboBox<String>(personViewModel);
        choicePersonView.setSelectedIndex(PV_GENERAL);
        choicePersonView.addActionListener(ev -> changePersonnelView());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(choicePersonView, gridBagConstraints);

        personModel = new PersonnelTableModel(getCampaign());
        personnelTable = new JTable(personModel);
        personnelTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        personnelTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        XTableColumnModel personColumnModel = new XTableColumnModel();
        personnelTable.setColumnModel(personColumnModel);
        personnelTable.createDefaultColumnsFromModel();
        personnelSorter = new TableRowSorter<PersonnelTableModel>(personModel);
        personnelSorter.setComparator(PersonnelTableModel.COL_RANK, new RankSorter(getCampaign()));
        personnelSorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        for (int i = PersonnelTableModel.COL_MECH; i < PersonnelTableModel.N_COL; i++) {
            personnelSorter.setComparator(i, new BonusSorter());
        }
        personnelSorter.setComparator(PersonnelTableModel.COL_SALARY, new FormattedNumberSorter());
        personnelSorter.setComparator(PersonnelTableModel.COL_AGE, new FormattedNumberSorter());
        personnelTable.setRowSorter(personnelSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_RANK, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_SKILL, SortOrder.DESCENDING));
        personnelSorter.setSortKeys(sortKeys);
        personnelTable.addMouseListener(new PersonnelTableMouseAdapter(getCampaignGui(),
                personnelTable, personModel) {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if ((splitPersonnel.getSize().width
                            - splitPersonnel.getDividerLocation() + splitPersonnel
                                .getDividerSize()) < PersonnelTab.PERSONNEL_VIEW_WIDTH) {
                        // expand
                        splitPersonnel.resetToPreferredSizes();
                    } else {
                        // collapse
                        splitPersonnel.setDividerLocation(1.0);
                    }

                }
            }            
        });
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = personnelTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(personModel.getColumnWidth(i));
            column.setCellRenderer(
                    personModel.getRenderer(choicePersonView.getSelectedIndex() == PV_GRAPHIC, getIconPackage()));
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

        splitPersonnel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(personnelTable),
                scrollPersonnelView);
        splitPersonnel.setOneTouchExpandable(true);
        splitPersonnel.setResizeWeight(1.0);
        splitPersonnel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshPersonnelView());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitPersonnel, gridBagConstraints);

        filterPersonnel();
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

    public static String getPersonnelGroupName(int group) {
        switch (group) {
        case PG_ACTIVE:
            return "Active Personnel";
        case PG_COMBAT:
            return "Combat Personnel";
        case PG_MW:
            return "Mechwarriors";
        case PG_CREW:
            return "Vehicle Crews";
        case PG_PILOT:
            return "Aerospace Pilots";
        case PG_CPILOT:
            return "Conventional Pilots";
        case PG_PROTO:
            return "Protomech Pilots";
        case PG_BA:
            return "Battle Armor Infantry";
        case PG_SOLDIER:
            return "Conventional Infantry";
        case PG_SUPPORT:
            return "Support Personnel";
        case PG_VESSEL:
            return "Large Vessel Crews";
        case PG_TECH:
            return "Techs";
        case PG_DOC:
            return "Medical Staff";
        case PG_ADMIN:
            return "Administrators";
        case PG_DEPENDENT:
            return "Dependents";
        case PG_RETIRE:
            return "Retired Personnel";
        case PG_MIA:
            return "Personnel MIA";
        case PG_KIA:
            return "Rolls of Honor (KIA)";
        case PG_PRISONER:
            return "Prisoners";
        default:
            return "?";
        }
    }

    public static String getPersonnelViewName(int group) {
        switch (group) {
        case PV_GRAPHIC:
            return "Graphic";
        case PV_GENERAL:
            return "General";
        case PV_PILOT:
            return "Piloting/Gunnery Skills";
        case PV_INF:
            return "Infantry Skills";
        case PV_TACTIC:
            return "Tactical Skills";
        case PV_TECH:
            return "Tech Skills";
        case PV_ADMIN:
            return "Admin Skills";
        case PV_FLUFF:
            return "Fluff Information";
        default:
            return "?";
        }
    }

    public void filterPersonnel() {
        RowFilter<PersonnelTableModel, Integer> personTypeFilter = null;
        final int nGroup = choicePerson.getSelectedIndex();
        personTypeFilter = new RowFilter<PersonnelTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                PersonnelTableModel personModel = entry.getModel();
                Person person = personModel.getPerson(entry.getIdentifier());
                int type = person.getPrimaryRole();
                if (((nGroup == PG_ACTIVE) || (nGroup == PG_COMBAT && type <= Person.T_SPACE_GUNNER)
                        || (nGroup == PG_SUPPORT && type > Person.T_SPACE_GUNNER)
                        || (nGroup == PG_MW && type == Person.T_MECHWARRIOR)
                        || (nGroup == PG_CREW && (type == Person.T_GVEE_DRIVER || type == Person.T_NVEE_DRIVER
                                || type == Person.T_VTOL_PILOT || type == Person.T_VEE_GUNNER))
                        || (nGroup == PG_PILOT && type == Person.T_AERO_PILOT)
                        || (nGroup == PG_CPILOT && type == Person.T_CONV_PILOT)
                        || (nGroup == PG_PROTO && type == Person.T_PROTO_PILOT)
                        || (nGroup == PG_BA && type == Person.T_BA)
                        || (nGroup == PG_SOLDIER && type == Person.T_INFANTRY)
                        || (nGroup == PG_VESSEL && (type == Person.T_SPACE_PILOT || type == Person.T_SPACE_CREW
                                || type == Person.T_SPACE_GUNNER || type == Person.T_NAVIGATOR))
                        || (nGroup == PG_TECH && type >= Person.T_MECH_TECH && type < Person.T_DOCTOR)
                        || (nGroup == PG_DOC && ((type == Person.T_DOCTOR) || (type == Person.T_MEDIC)))
                        || (nGroup == PG_ADMIN && type > Person.T_MEDIC))
                        && !person.isPrisoner()) {
                    return person.isActive();
                } else if (nGroup == PG_DEPENDENT) {
                    return person.isDependent() == true;
                } else if (nGroup == PG_RETIRE) {
                    return person.getStatus() == Person.S_RETIRED;
                } else if (nGroup == PG_MIA) {
                    return person.getStatus() == Person.S_MIA;
                } else if (nGroup == PG_KIA) {
                    return person.getStatus() == Person.S_KIA;
                } else if (nGroup == PG_PRISONER) {
                    return person.isPrisoner();
                }
                return false;
            }
        };
        personnelSorter.setRowFilter(personTypeFilter);
    }

    private void changePersonnelView() {

        int view = choicePersonView.getSelectedIndex();
        XTableColumnModel columnModel = (XTableColumnModel) personnelTable.getColumnModel();
        personnelTable.setRowHeight(15);

        // set the renderer
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = columnModel.getColumnByModelIndex(i);
            column.setCellRenderer(
                    personModel.getRenderer(choicePersonView.getSelectedIndex() == PV_GRAPHIC, getIconPackage()));
            if (i == PersonnelTableModel.COL_RANK) {
                if (view == PV_GRAPHIC) {
                    column.setPreferredWidth(125);
                    column.setHeaderValue("Person");
                } else {
                    column.setPreferredWidth(personModel.getColumnWidth(i));
                    column.setHeaderValue("Rank");
                }
            }
        }

        if (view == PV_GRAPHIC) {
            personnelTable.setRowHeight(80);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_GENERAL) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY),
                    getCampaign().getCampaignOptions().payForSalaries());
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), true);
        } else if (view == PV_PILOT) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_INF) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_TACTIC) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_TECH) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_ADMIN) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), false);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        } else if (view == PV_FLUFF) {
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_RANK), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NAME), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_CALL), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_AGE), true);
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
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_TOUGH), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_EDGE), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NABIL), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_NIMP), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_HITS), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_SALARY), false);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_KILLS), true);
            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(PersonnelTableModel.COL_XP), false);
        }
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
        scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson, getCampaign(), getIconPackage()));
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
