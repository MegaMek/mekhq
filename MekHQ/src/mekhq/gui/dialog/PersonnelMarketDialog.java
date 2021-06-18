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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.HirePersonnelUnitAction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.enums.PersonnelTabView;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.XTableColumnModel;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.view.PersonViewPanel;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 * (code borrowed heavily from MegaMekLab UnitSelectorDialog
 */
public class PersonnelMarketDialog extends JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 707579637170575313L;

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
    private ArrayList<RowSorter.SortKey> sortKeys;
    private JSplitPane splitMain;

    ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelMarketDialog", new EncodeControl());
    //endregion Variable Declarations

    /** Creates new form PersonnelMarketDialog */
    public PersonnelMarketDialog(Frame frame, CampaignGUI view, Campaign c) {
        super(frame, true);
        hqView = view;
        campaign = c;
        personnelMarket = c.getPersonnelMarket();
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
        for (PersonnelFilter filter : MekHQ.getMekHQOptions().getPersonnelFilterStyle().getFilters(true)) {
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

        if (campaign.getCampaignOptions().getUseAtB() && !campaign.hasActiveContract()) {
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
        tablePersonnel.setName("tablePersonnel"); // NOI18N
        tablePersonnel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablePersonnel.setColumnModel(new XTableColumnModel());
        tablePersonnel.createDefaultColumnsFromModel();
        sorter = new TableRowSorter<>(personnelModel);
        sorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        sorter.setComparator(PersonnelTableModel.COL_SALARY, new FormattedNumberSorter());
        tablePersonnel.setRowSorter(sorter);
        sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_SKILL, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        tablePersonnel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablePersonnel.getSelectionModel().addListSelectionListener(this::personChanged);
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = ((XTableColumnModel) tablePersonnel.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(personnelModel.getColumnWidth(i));
            column.setCellRenderer(personnelModel.getRenderer(PersonnelTabView.GENERAL));

            if (i != PersonnelTableModel.COL_GIVEN_NAME
                    && ((!campaign.getFaction().isClan() && i != PersonnelTableModel.COL_SURNAME)
                        || (campaign.getFaction().isClan() && i != PersonnelTableModel.COL_BLOODNAME))
                    && i != PersonnelTableModel.COL_TYPE && i != PersonnelTableModel.COL_SKILL
                    && i != PersonnelTableModel.COL_AGE && i != PersonnelTableModel.COL_GENDER
                    && i != PersonnelTableModel.COL_ASSIGN) {
                ((XTableColumnModel) tablePersonnel.getColumnModel()).setColumnVisible(column, false);
            }
        }

        tablePersonnel.setIntercellSpacing(new Dimension(0, 0));
        tablePersonnel.setShowGrid(false);
        column.setCellRenderer(getRenderer());
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
        btnHire.setName("btnHire"); // NOI18N
        btnHire.addActionListener(this::hirePerson);
        panelOKBtns.add(btnHire, new java.awt.GridBagConstraints());

        JButton btnAdd = new JButton("Add (GM)");
        btnAdd.addActionListener(evt -> addPerson());
        btnAdd.setEnabled(campaign.isGM());
        panelOKBtns.add(btnAdd, new java.awt.GridBagConstraints());

        JButton btnClose = new JButton(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
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

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(PersonnelMarketDialog.class);

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
    }

    public Person getPerson() {
        return selectedPerson;
    }

    private void hirePerson(ActionEvent evt) {
        if (null != selectedPerson) {
            if (campaign.getFunds().isLessThan((campaign.getCampaignOptions().payForRecruitment()
                            ? selectedPerson.getSalary().multipliedBy(2)
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
        if (pay && !campaign.getFinances().debit(unitCost, Transaction.C_UNIT,
                "Purchased " + en.getShortName(),
                campaign.getLocalDate())) {
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
        sorter.setRowFilter(new RowFilter<PersonnelTableModel, Integer>() {
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
            if (!campaign.getCampaignOptions().getUseShareSystem() &&
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
             if (StringUtil.isNullOrEmpty(unitText)) {
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
         //This odd code is to make sure that the scrollbar stays at the top
         //I cant just call it here, because it ends up getting reset somewhere later
         javax.swing.SwingUtilities.invokeLater(() -> scrollPersonnelView.getVerticalScrollBar().setValue(0));
    }

    @Override
    public void setVisible(boolean visible) {
        filterPersonnel();
        //changePersonnelView();
        super.setVisible(visible);
    }

    public TableCellRenderer getRenderer() {
        //if(choicePersonView.getSelectedIndex() == CampaignGUI.PV_GRAPHIC) {
            //return personnelModel.new VisualRenderer(hqView.getCamos(), portraits, hqView.getMechTiles());
       // }
        return personnelModel.new Renderer();
    }
}
