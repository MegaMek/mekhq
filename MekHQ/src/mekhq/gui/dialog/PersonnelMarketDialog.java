/*
 * Copyright (c) 2009, 2013, 2020 - The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.util.fileUtils.DirectoryItems;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.PersonnelTab;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JTablePreference;
import mekhq.gui.preferences.JToggleButtonPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.view.PersonViewPanel;
import mekhq.preferences.PreferencesNode;

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
    Person selectedPerson = null;
    @SuppressWarnings("unused")
    private DirectoryItems portraits;
    private Money unitCost = Money.zero();

    private javax.swing.JComboBox<String> comboPersonType;
    private javax.swing.JLabel lblPersonChoice;
    private javax.swing.JRadioButton radioNormalRoll;
    private javax.swing.JRadioButton radioPaidRecruitment;
    private javax.swing.JComboBox<String> comboRecruitType;
    private javax.swing.JPanel panelOKBtns;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelFilterBtns;
    private javax.swing.JTable tablePersonnel;
    private javax.swing.JLabel lblUnitCost;
    private javax.swing.JScrollPane scrollTablePersonnel;
    private javax.swing.JScrollPane scrollPersonnelView;
    private TableRowSorter<PersonnelTableModel> sorter;
    ArrayList <RowSorter.SortKey> sortKeys;
    private javax.swing.JSplitPane splitMain;

    ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PersonnelMarketDialog",
            new EncodeControl());
    //endregion Variable Declarations

    /** Creates new form PersonnelMarketDialog */
    public PersonnelMarketDialog(Frame frame, CampaignGUI view, Campaign c, DirectoryItems portraits) {
        super(frame, true);
        hqView = view;
        campaign = c;
        this.portraits = portraits;
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
        comboRecruitType = new javax.swing.JComboBox<>();
        lblUnitCost = new javax.swing.JLabel();
        panelOKBtns = new javax.swing.JPanel();
        lblPersonChoice = new javax.swing.JLabel();
        //choicePersonView = new javax.swing.JComboBox();
        //lblPersonView = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Personnel Market"); // NOI18N
        setName("Form"); // NOI18N
        getContentPane().setLayout(new BorderLayout());

        panelFilterBtns.setLayout(new java.awt.GridBagLayout());

        lblPersonChoice.setText("Personnel Type:"); // NOI18N
        lblPersonChoice.setName("lblPersonChoice"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panelFilterBtns.add(lblPersonChoice, gridBagConstraints);

        DefaultComboBoxModel<String> personTypeModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < PersonnelTab.PG_RETIRE; i++) {
        	personTypeModel.addElement(getPersonnelGroupName(i));
        }
        comboPersonType.setSelectedItem(0);
        comboPersonType.setModel(personTypeModel);
        comboPersonType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboPersonType.setName("comboUnitType"); // NOI18N
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
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panelFilterBtns.add(radioNormalRoll, gridBagConstraints);

            radioPaidRecruitment.setText("Make paid recruitment roll next week (100,000 C-bills)");
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panelFilterBtns.add(radioPaidRecruitment, gridBagConstraints);

            ButtonGroup group = new ButtonGroup();
            group.add(radioNormalRoll);
            group.add(radioPaidRecruitment);
            if (personnelMarket.getPaidRecruitment()) {
                radioPaidRecruitment.setSelected(true);
            } else {
                radioNormalRoll.setSelected(true);
            }

            for (int i = 1; i < Person.T_NUM; i++) {
                comboRecruitType.addItem(Person.getRoleDesc(i, campaign.getFaction().isClan()));
            }
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            panelFilterBtns.add(comboRecruitType, gridBagConstraints);

            if (personnelMarket.getPaidRecruitment()) {
                radioPaidRecruitment.setSelected(true);
                comboRecruitType.setSelectedIndex(personnelMarket.getPaidRecruitType() - 1);
            } else {
                radioNormalRoll.setSelected(true);
            }
        }

        scrollTablePersonnel.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTablePersonnel.setName("srcTablePersonnel"); // NOI18N
        scrollTablePersonnel.setPreferredSize(new java.awt.Dimension(500, 400));

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
            column.setCellRenderer(personnelModel.getRenderer(false, null));

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

        comboRecruitType.setName("recruitType");
        preferences.manage(new JComboBoxPreference(comboRecruitType));

        tablePersonnel.setName("unitsTable");
        preferences.manage(new JTablePreference(tablePersonnel));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public Person getPerson() {
	    return selectedPerson;
	}

	private void hirePerson(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
	    if (null != selectedPerson) {
	    	if (campaign.getFunds().isLessThan(
                    (campaign.getCampaignOptions().payForRecruitment() ? selectedPerson.getSalary().multipliedBy(2) : Money.zero())
                            .plus(unitCost))) {
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
	}//GEN-LAST:event_btnHireActionPerformed

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
		campaign.addUnit(en, false, 0);
		Unit unit = null;
		for (Unit u : campaign.getUnits()) {
			if (u.getEntity() == en) {
				unit = u;
				break;
			}
        }
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
		} else if (selectedPerson.getPrimaryRole() == Person.T_NAVIGATOR) {
			unit.setNavigator(selectedPerson);
		} else {
			unit.addVesselCrew(selectedPerson);
		}

		campaign.hirePersonnelFor(unit.getId(), !pay);
	}

	private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
	    selectedPerson = null;
    	personnelMarket.setPaidRecruitment(radioPaidRecruitment.isSelected());
    	if (radioPaidRecruitment.isSelected()) {
    		personnelMarket.setPaidRecruitType(comboRecruitType.getSelectedIndex() + 1);
    	}
	    setVisible(false);
	}//GEN-LAST:event_btnCloseActionPerformed

    private void filterPersonnel() {
        RowFilter<PersonnelTableModel, Integer> personTypeFilter;
        final int nGroup = comboPersonType.getSelectedIndex();
        //If current expression doesn't parse, don't update.
        try {
            personTypeFilter = new RowFilter<PersonnelTableModel,Integer>() {
                @Override
                public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                    PersonnelTableModel personModel = entry.getModel();
                	Person person = personModel.getPerson(entry.getIdentifier());
                	int type = person.getPrimaryRole();
                    if ((nGroup == PersonnelTab.PG_ACTIVE) ||
                            (nGroup == PersonnelTab.PG_COMBAT && type <= Person.T_SPACE_GUNNER) ||
                            (nGroup == PersonnelTab.PG_SUPPORT && type > Person.T_SPACE_GUNNER) ||
                            (nGroup == PersonnelTab.PG_MW && type == Person.T_MECHWARRIOR) ||
                            (nGroup == PersonnelTab.PG_CREW && (type == Person.T_GVEE_DRIVER
                                    || type == Person.T_NVEE_DRIVER || type == Person.T_VTOL_PILOT
                                    || type == Person.T_VEE_GUNNER || type == Person.T_VEHICLE_CREW)) ||
                            (nGroup == PersonnelTab.PG_PILOT && type == Person.T_AERO_PILOT) ||
                            (nGroup == PersonnelTab.PG_CPILOT && type == Person.T_CONV_PILOT) ||
                            (nGroup == PersonnelTab.PG_PROTO && type == Person.T_PROTO_PILOT) ||
                            (nGroup == PersonnelTab.PG_BA && type == Person.T_BA) ||
                            (nGroup == PersonnelTab.PG_SOLDIER && type == Person.T_INFANTRY) ||
                            (nGroup == PersonnelTab.PG_VESSEL && (type == Person.T_SPACE_PILOT || type == Person.T_SPACE_CREW || type == Person.T_SPACE_GUNNER || type == Person.T_NAVIGATOR)) ||
                            (nGroup == PersonnelTab.PG_TECH && type >= Person.T_MECH_TECH && type < Person.T_DOCTOR) ||
                            (nGroup == PersonnelTab.PG_DOC && ((type == Person.T_DOCTOR) || (type == Person.T_MEDIC))) ||
                            (nGroup == PersonnelTab.PG_ADMIN && type > Person.T_MEDIC)
                            ) {
                        return person.isActive();
                    } else if (nGroup == PersonnelTab.PG_DEPENDENT) {
                        return person.isDependent();
                    } else if (nGroup == PersonnelTab.PG_FOUNDER) {
                        return person.isFounder();
                    } else if(nGroup == PersonnelTab.PG_RETIRE) {
                        return person.getStatus() == PersonnelStatus.RETIRED;
                    } else if(nGroup == PersonnelTab.PG_MIA) {
                        return person.getStatus() == PersonnelStatus.MIA;
                    } else if(nGroup == PersonnelTab.PG_KIA) {
                        return person.getStatus() == PersonnelStatus.KIA;
                    }
                    return false;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(personTypeFilter);
    }

    private void personChanged(javax.swing.event.ListSelectionEvent evt) {
        int view = tablePersonnel.getSelectedRow();
        if(view < 0) {
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

    	 if(row < 0) {
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

    public JComboBox<String> getComboUnitType() {
        return comboPersonType;
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

	public static String getPersonnelGroupName(int group) {
        switch(group) {
        case PersonnelTab.PG_ACTIVE:
            return "All Personnel";
        case PersonnelTab.PG_COMBAT:
            return "Combat Personnel";
        case PersonnelTab.PG_MW:
            return "Mechwarriors";
        case PersonnelTab.PG_CREW:
            return "Vehicle Crews";
        case PersonnelTab.PG_PILOT:
            return "Aerospace Pilots";
        case PersonnelTab.PG_CPILOT:
            return "Conventional Pilots";
        case PersonnelTab.PG_PROTO:
            return "Protomech Pilots";
        case PersonnelTab.PG_BA:
            return "Battle Armor Infantry";
        case PersonnelTab.PG_SOLDIER:
            return "Conventional Infantry";
        case PersonnelTab.PG_SUPPORT:
            return "Support Personnel";
        case PersonnelTab.PG_VESSEL:
            return "Large Vessel Crews";
        case PersonnelTab.PG_TECH:
            return "Techs";
        case PersonnelTab.PG_DOC:
            return "Medical Staff";
        case PersonnelTab.PG_ADMIN:
            return "Administrators";
        case PersonnelTab.PG_DEPENDENT:
            return "Dependents";
        case PersonnelTab.PG_FOUNDER:
            return "Founders";
        case PersonnelTab.PG_RETIRE:
            return "Retired Personnel";
        case PersonnelTab.PG_MIA:
            return "Personnel MIA";
        case PersonnelTab.PG_KIA:
            return "Rolls of Honor (KIA)";
        default:
            return "?";
        }
    }

}
