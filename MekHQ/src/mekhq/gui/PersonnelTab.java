/*
 * Copyright (c) 2017-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.*;
import mekhq.campaign.personnel.Person;
import mekhq.gui.adapter.PersonnelTableMouseAdapter;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.enums.PersonnelTableModelColumn;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.view.PersonViewPanel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Tab for interacting with all personnel
 */
public final class PersonnelTab extends CampaignGuiTab {
    private static final long serialVersionUID = -4389102971116114869L;

    public static final int PERSONNEL_VIEW_WIDTH = 500;

    private JSplitPane splitPersonnel;
    private JTable personnelTable;
    private MMComboBox<PersonnelFilter> choicePerson;
    private MMComboBox<PersonnelTabView> choicePersonView;
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

        choicePerson = new MMComboBox<>("choicePerson", createPersonGroupModel());
        choicePerson.setSelectedItem(PersonnelFilter.ACTIVE);
        choicePerson.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PersonnelFilter) {
                    list.setToolTipText(((PersonnelFilter) value).getToolTipText());
                }
                return this;
            }
        });
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

        choicePersonView = new MMComboBox<>("choicePersonView", PersonnelTabView.values());
        choicePersonView.setSelectedItem(PersonnelTabView.GENERAL);
        choicePersonView.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PersonnelTabView) {
                    list.setToolTipText(((PersonnelTabView) value).getToolTipText());
                }
                return this;
            }
        });
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
        final ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        for (final PersonnelTableModelColumn column : PersonnelTableModel.PERSONNEL_COLUMNS) {
            final Comparator<?> comparator = column.getComparator(getCampaign());
            if (comparator != null) {
                personnelSorter.setComparator(column.ordinal(), comparator);
            }
            final SortOrder sortOrder = column.getDefaultSortOrder();
            if (sortOrder != null) {
                sortKeys.add(new RowSorter.SortKey(column.ordinal(), sortOrder));
            }
        }
        personnelSorter.setSortKeys(sortKeys);
        personnelTable.setRowSorter(personnelSorter);
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
        final DefaultComboBoxModel<PersonnelFilter> personGroupModel = new DefaultComboBoxModel<>();
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
        final PersonnelFilter filter = (choicePerson.getSelectedItem() == null)
                ? PersonnelFilter.ACTIVE : choicePerson.getSelectedItem();
        personnelSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                return filter.getFilteredInformation(entry.getModel().getPerson(entry.getIdentifier()));
            }
        });
    }

    private void changePersonnelView() {
        final PersonnelTabView view = (choicePersonView.getSelectedItem() == null)
                ? PersonnelTabView.GENERAL : choicePersonView.getSelectedItem();
        final XTableColumnModel columnModel = (XTableColumnModel) getPersonnelTable().getColumnModel();
        getPersonnelTable().setRowHeight(15);

        // set the renderer
        for (final PersonnelTableModelColumn column : PersonnelTableModel.PERSONNEL_COLUMNS) {
            final TableColumn tableColumn = columnModel.getColumnByModelIndex(column.ordinal());
            tableColumn.setCellRenderer(getPersonModel().getRenderer(choicePersonView.getSelectedItem()));
            if (column.isRank() && view.isGraphic()) {
                tableColumn.setPreferredWidth(125);
                tableColumn.setHeaderValue("Person");
            } else {
                tableColumn.setPreferredWidth(column.getWidth());
            }
            columnModel.setColumnVisible(tableColumn,
                    column.isVisible(getCampaign(), view, getPersonnelTable()));
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
