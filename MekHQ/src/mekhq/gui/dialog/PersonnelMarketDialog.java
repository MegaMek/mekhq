/*
 * Copyright (c) 2009-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import megamek.client.ui.models.XTableColumnModel;
import megamek.client.ui.preferences.*;
import megamek.client.ui.swing.MechViewPanel;
import megamek.codeUtilities.StringUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.HirePersonnelUnitAction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.enums.PersonnelTableModelColumn;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.view.PersonViewPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * @author  Jay Lawson (jaylawson39 at yahoo.com)
 * (code borrowed heavily from MegaMekLab UnitSelectorDialog
 */
public class PersonnelMarketDialog extends JDialog {
    //region Variable Declarations
    private PersonnelTableModel personnelModel;
    private Campaign campaign;
    private CampaignGUI hqView;
    private PersonnelMarket personnelMarket;
    private Person selectedPerson = null;
    private Money unitCost = Money.zero();

    private JComboBox<PersonnelFilter> comboPersonType;
    private JLabel lblPersonChoice;
    private JRadioButton radioNormalRoll;
    private JRadioButton radioPaidRecruitment;
    private JComboBox<PersonnelRole> comboRecruitRole;
    private JPanel panelOKBtns;
    private JPanel panelMain;
    private JPanel panelFilterBtns;
    private JTable tablePersonnel;
    private JLabel lblUnitCost;
    private JScrollPane scrollTablePersonnel;
    private JScrollPane scrollPersonnelView;
    private TableRowSorter<PersonnelTableModel> sorter;
    private JSplitPane splitMain;

    private final List<PersonnelTableModelColumn> personnelMarketColumns = List.of(
            PersonnelTableModelColumn.FIRST_NAME,
            PersonnelTableModelColumn.LAST_NAME,
            PersonnelTableModelColumn.AGE,
            PersonnelTableModelColumn.GENDER,
            PersonnelTableModelColumn.SKILL_LEVEL,
            PersonnelTableModelColumn.PERSONNEL_ROLE,
            PersonnelTableModelColumn.UNIT_ASSIGNMENT
    );

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelMarketDialog",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    public PersonnelMarketDialog(final JFrame frame, final CampaignGUI view, final Campaign campaign) {
        super(frame, true);
        hqView = view;
        this.campaign = campaign;
        personnelMarket = campaign.getPersonnelMarket();
        personnelModel = new PersonnelTableModel(campaign);
        personnelModel.setData(personnelMarket.getPersonnel());
        personnelModel.loadAssignmentFromMarket(personnelMarket);
        initComponents();
        filterPersonnel();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrollTablePersonnel = new javax.swing.JScrollPane();
        scrollPersonnelView = new javax.swing.JScrollPane();
        tablePersonnel = new javax.swing.JTable();
        panelMain = new javax.swing.JPanel();
        panelFilterBtns = new javax.swing.JPanel();
        comboPersonType = new javax.swing.JComboBox<>();
        radioNormalRoll = new javax.swing.JRadioButton();
        radioPaidRecruitment = new javax.swing.JRadioButton();
        lblUnitCost = new javax.swing.JLabel();
        panelOKBtns = new javax.swing.JPanel();
        lblPersonChoice = new javax.swing.JLabel();
        comboRecruitRole = new JComboBox<>(PersonnelRole.values());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Personnel Market");
        setName("Form");
        getContentPane().setLayout(new BorderLayout());

        panelFilterBtns.setLayout(new java.awt.GridBagLayout());

        lblPersonChoice.setText("Personnel Type:");
        lblPersonChoice.setName("lblPersonChoice");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panelFilterBtns.add(lblPersonChoice, gridBagConstraints);

        DefaultComboBoxModel<PersonnelFilter> personTypeModel = new DefaultComboBoxModel<>();
        for (PersonnelFilter filter : MekHQ.getMHQOptions().getPersonnelFilterStyle().getFilters(true)) {
            personTypeModel.addElement(filter);
        }
        comboPersonType.setSelectedItem(0);
        comboPersonType.setModel(personTypeModel);
        comboPersonType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboPersonType.setName("comboUnitType");
        comboPersonType.setPreferredSize(new java.awt.Dimension(200, 27));
        comboPersonType.addActionListener(evt -> filterPersonnel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(comboPersonType, gridBagConstraints);

        if (campaign.getCampaignOptions().isUseAtB() && !campaign.hasActiveContract()) {
            radioNormalRoll.setText("Make normal roll next week");
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panelFilterBtns.add(radioNormalRoll, gridBagConstraints);

            radioPaidRecruitment.setText("Make paid recruitment roll next week (100,000 C-bills)");
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panelFilterBtns.add(radioPaidRecruitment, gridBagConstraints);

            ButtonGroup group = new ButtonGroup();
            group.add(radioNormalRoll);
            group.add(radioPaidRecruitment);
            if (personnelMarket.getPaidRecruitment()) {
                radioPaidRecruitment.setSelected(true);
            } else {
                radioNormalRoll.setSelected(true);
            }

            final boolean isClan = campaign.getFaction().isClan();
            comboRecruitRole.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    return super.getListCellRendererComponent(list,
                            (value instanceof PersonnelRole) ? ((PersonnelRole) value).getName(isClan) : value,
                            index, isSelected, cellHasFocus);
                }
            });
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            panelFilterBtns.add(comboRecruitRole, gridBagConstraints);

            if (personnelMarket.getPaidRecruitment()) {
                radioPaidRecruitment.setSelected(true);
                comboRecruitRole.setSelectedItem(personnelMarket.getPaidRecruitRole());
            } else {
                radioNormalRoll.setSelected(true);
            }
        }

        scrollTablePersonnel.setMinimumSize(new Dimension(500, 400));
        scrollTablePersonnel.setName("srcTablePersonnel");
        scrollTablePersonnel.setPreferredSize(new Dimension(500, 400));

        tablePersonnel.setModel(personnelModel);
        tablePersonnel.setName("tablePersonnel");
        tablePersonnel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePersonnel.setColumnModel(new XTableColumnModel());
        tablePersonnel.createDefaultColumnsFromModel();
        tablePersonnel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablePersonnel.getSelectionModel().addListSelectionListener(this::personChanged);

        sorter = new TableRowSorter<>(personnelModel);

        final XTableColumnModel columnModel = (XTableColumnModel) tablePersonnel.getColumnModel();
        final ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        for (final PersonnelTableModelColumn column : PersonnelTableModel.PERSONNEL_COLUMNS) {
            final TableColumn tableColumn = columnModel.getColumnByModelIndex(column.ordinal());
            if (!personnelMarketColumns.contains(column)) {
                columnModel.setColumnVisible(tableColumn, false);
                continue;
            }

            tableColumn.setPreferredWidth(column.getWidth());
            tableColumn.setCellRenderer(getRenderer());
            columnModel.setColumnVisible(tableColumn, true);

            final Comparator<?> comparator = column.getComparator(campaign);
            if (comparator != null) {
                sorter.setComparator(column.ordinal(), comparator);
            }
            final SortOrder sortOrder = column.getDefaultSortOrder();
            if (sortOrder != null) {
                sortKeys.add(new RowSorter.SortKey(column.ordinal(), sortOrder));
            }
        }
        sorter.setSortKeys(sortKeys);
        tablePersonnel.setRowSorter(sorter);

        tablePersonnel.setIntercellSpacing(new Dimension(0, 0));
        tablePersonnel.setShowGrid(false);
        scrollTablePersonnel.setViewportView(tablePersonnel);

        scrollPersonnelView.setMinimumSize(new java.awt.Dimension(500, 600));
        scrollPersonnelView.setPreferredSize(new java.awt.Dimension(500, 600));
        scrollPersonnelView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPersonnelView.setViewportView(null);

        panelMain.setLayout(new BorderLayout());
        panelMain.add(panelFilterBtns, BorderLayout.PAGE_START);
        panelMain.add(scrollTablePersonnel, BorderLayout.CENTER);

        splitMain = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,panelMain, scrollPersonnelView);
        splitMain.setOneTouchExpandable(true);
        splitMain.setResizeWeight(0.0);
        getContentPane().add(splitMain, BorderLayout.CENTER);

        panelOKBtns.setLayout(new java.awt.GridBagLayout());

        JButton btnAdvDay = new JButton("Advance Day");
        btnAdvDay.setName("buttonAdvanceDay");
        btnAdvDay.addActionListener(evt -> {
            hqView.getCampaignController().advanceDay();
            personnelModel.setData(personnelMarket.getPersonnel());
        });
        btnAdvDay.setEnabled(hqView.getCampaignController().isHost());
        panelOKBtns.add(btnAdvDay, new GridBagConstraints());

        JButton btnHire = new JButton("Hire");
        btnHire.setName("btnHire");
        btnHire.addActionListener(this::hirePerson);
        panelOKBtns.add(btnHire, new java.awt.GridBagConstraints());

        JButton btnAdd = new JButton("Add (GM)");
        btnAdd.addActionListener(evt -> addPerson());
        btnAdd.setEnabled(campaign.isGM());
        panelOKBtns.add(btnAdd, new java.awt.GridBagConstraints());

        JButton btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        javax.swing.JPanel panel = new javax.swing.JPanel();
        panel.setLayout(new java.awt.GridLayout(1, 3));
        panel.add(lblUnitCost);
        panel.add(panelOKBtns);
        panel.add(new javax.swing.JPanel());

        getContentPane().add(panel, BorderLayout.PAGE_END);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(PersonnelMarketDialog.class);

            comboPersonType.setName("personType");
            preferences.manage(new JComboBoxPreference(comboPersonType));

            radioNormalRoll.setName("normalRoll");
            preferences.manage(new JToggleButtonPreference(radioNormalRoll));

            radioPaidRecruitment.setName("paidRecruitment");
            preferences.manage(new JToggleButtonPreference(radioPaidRecruitment));

            comboRecruitRole.setName("recruitRole");
            preferences.manage(new JComboBoxPreference(comboRecruitRole));

            tablePersonnel.setName("unitsTable");
            preferences.manage(new JTablePreference(tablePersonnel));

            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    public Person getPerson() {
        return selectedPerson;
    }

    private void hirePerson(ActionEvent evt) {
        if (null != selectedPerson) {
            if (campaign.getFunds().isLessThan((campaign.getCampaignOptions().isPayForRecruitment()
                            ? selectedPerson.getSalary(campaign).multipliedBy(2)
                            : Money.zero()).plus(unitCost))) {
                 campaign.addReport("<font color='red'><b>Insufficient funds. Transaction cancelled</b>.</font>");
            } else {
                /* Adding person to campaign changes pid; grab the old one to
                 * use as a key to any attached entity
                 */
                UUID pid = selectedPerson.getId();
                if (campaign.recruitPerson(selectedPerson)) {
                    Entity en = personnelMarket.getAttachedEntity(pid);
                    if (null != en) {
                        addUnit(en, true);
                        personnelMarket.removeAttachedEntity(pid);
                    }
                    personnelMarket.removePerson(selectedPerson);
                    personnelModel.setData(personnelMarket.getPersonnel());
                }
            }
            refreshPersonView();
        }
    }

    private void addPerson() {
        if (selectedPerson != null) {
            Entity en = personnelMarket.getAttachedEntity(selectedPerson);
            UUID pid = selectedPerson.getId();

            if (campaign.recruitPerson(selectedPerson, true)) {
                addUnit(en, false);
                personnelMarket.removePerson(selectedPerson);
                personnelModel.setData(personnelMarket.getPersonnel());
                personnelMarket.removeAttachedEntity(pid);
            }
            refreshPersonView();
        }
    }

    private void addUnit(Entity en, boolean pay) {
        if (null == en) {
            return;
        }

        if (pay && !campaign.getFinances().debit(TransactionType.UNIT_PURCHASE, campaign.getLocalDate(),
                unitCost, "Purchased " + en.getShortName())) {
            return;
        }
        Unit unit = campaign.addNewUnit(en, false, 0);
        if (unit == null) {
            // No such unit matching the entity.
            return;
        }

        if (unit.usesSoloPilot()) {
            unit.addPilotOrSoldier(selectedPerson);
            selectedPerson.setOriginalUnit(unit);
        } else if (unit.usesSoldiers()) {
            unit.addPilotOrSoldier(selectedPerson);
        } else if (selectedPerson.canDrive(en)) {
            unit.addDriver(selectedPerson);
        } else if (selectedPerson.canGun(en)) {
            unit.addGunner(selectedPerson);
        } else if (selectedPerson.getPrimaryRole().isVesselNavigator()) {
            unit.setNavigator(selectedPerson);
        } else {
            unit.addVesselCrew(selectedPerson);
        }

        HirePersonnelUnitAction hireAction = new HirePersonnelUnitAction(!pay);
        hireAction.execute(campaign, unit);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        selectedPerson = null;
        personnelMarket.setPaidRecruitment(radioPaidRecruitment.isSelected());
        if (radioPaidRecruitment.isSelected()) {
            personnelMarket.setPaidRecruitRole((PersonnelRole) comboRecruitRole.getSelectedItem());
        }
        setVisible(false);
    }

    private void filterPersonnel() {
        PersonnelFilter nGroup = (comboPersonType.getSelectedItem() != null)
                ? (PersonnelFilter) comboPersonType.getSelectedItem()
                : PersonnelFilter.ACTIVE;
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                return nGroup.getFilteredInformation(entry.getModel().getPerson(entry.getIdentifier()));
            }
        });
    }

    private void personChanged(javax.swing.event.ListSelectionEvent evt) {
        int view = tablePersonnel.getSelectedRow();
        if (view < 0) {
            //selection got filtered away
            selectedPerson = null;
            refreshPersonView();
            return;
        }
        selectedPerson = personnelModel.getPerson(tablePersonnel.convertRowIndexToModel(view));
        Entity en =  personnelMarket.getAttachedEntity(selectedPerson);
        if (null == en) {
            unitCost = Money.zero();
        } else {
            if (!campaign.getCampaignOptions().isUseShareSystem() &&
                    (en instanceof megamek.common.Mech ||
                            en instanceof megamek.common.Tank ||
                            en instanceof megamek.common.Aero)) {
                unitCost = Money.of(en.getCost(false)).dividedBy(2.0);
            } else {
                unitCost = Money.zero();
            }
        }
        refreshPersonView();
    }

     void refreshPersonView() {
         lblUnitCost.setText("");

         int row = tablePersonnel.getSelectedRow();

         if (row < 0) {
             scrollPersonnelView.setViewportView(null);
             return;
         }

         Entity en = personnelMarket.getAttachedEntity(selectedPerson);
         String unitText = "";
         if (unitCost.isPositive()) {
             unitText = "Unit cost: " + unitCost.toAmountAndSymbolString();
         }

         if (null != en) {
             if (StringUtility.isNullOrBlank(unitText)) {
                 unitText = "Unit: ";
             } else {
                 unitText += " - ";
             }

             unitText += en.getDisplayName();
         }

         lblUnitCost.setText(unitText);

         if (null != en) {
             JTabbedPane tabUnit = new JTabbedPane();
             String name = "Commander";
             if (Compute.getFullCrewSize(en) == 1) {
                 name = "Pilot";
             }
             tabUnit.add(name, new PersonViewPanel(selectedPerson, campaign, hqView));
             MechViewPanel mvp = new MechViewPanel();
             mvp.setMech(en, true);
             tabUnit.add("Unit", mvp);
             scrollPersonnelView.setViewportView(tabUnit);
         } else {
             scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson, campaign, hqView));
         }
         // This odd code is to make sure that the scrollbar stays at the top
         // I can't just call it here, because it ends up getting reset somewhere later
         javax.swing.SwingUtilities.invokeLater(() -> scrollPersonnelView.getVerticalScrollBar().setValue(0));
    }

    @Override
    public void setVisible(boolean visible) {
        filterPersonnel();
        super.setVisible(visible);
    }

    public TableCellRenderer getRenderer() {
        return personnelModel.new Renderer();
    }
}
