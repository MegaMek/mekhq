/*
 * Copyright (c) 2014-2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.RetirementDefectionTracker;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.enums.PersonnelFilter;
import mekhq.gui.model.RetirementTableModel;
import mekhq.gui.model.UnitAssignmentTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.PersonRankStringSorter;
import mekhq.gui.sorter.WeightClassSorter;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;

/**
 * @author Neoancient
 */
public class RetirementDefectionDialog extends JDialog {
    private static final String PAN_OVERVIEW = "PanOverview";
    private static final String PAN_RESULTS = "PanResults";

    private String currentPanel;

    private CampaignGUI hqView;
    private AtBContract contract;
    private RetirementDefectionTracker rdTracker;

    private Map<UUID, TargetRoll> targetRolls;
    private Map<UUID, UUID> unitAssignments;

    private JPanel panMain;
    private JTextArea txtInstructions;
    private CardLayout cardLayout;

    /* Overview Panel components */
    private JComboBox<PersonnelFilter> cbGroupOverview;
    private JSpinner spnGeneralMod;
    private JLabel lblTotal;
    private RetirementTable personnelTable;
    private TableRowSorter<RetirementTableModel> personnelSorter;
    private TableRowSorter<RetirementTableModel> retireeSorter;
    private JTextArea txtTargetDetails;

    /* Results Panel components */
    private JComboBox<PersonnelFilter> cbGroupResults;
    private JLabel lblPayment;
    private RetirementTable retireeTable;
    private JButton btnAddUnit;
    private JButton btnRemoveUnit;
    private JComboBox<String> cbUnitCategory;
    private JCheckBox chkShowAllUnits;
    private JTable unitAssignmentTable;
    private TableRowSorter<UnitAssignmentTableModel> unitSorter;

    /* Button Panel components */
    private JButton btnCancel;
    private JToggleButton btnEdit;
    private JButton btnRoll;
    private JButton btnDone;

    private boolean aborted = true;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.RetirementDefectionDialog",
            MekHQ.getMHQOptions().getLocale());

    public RetirementDefectionDialog (CampaignGUI gui, AtBContract contract, boolean doRetirement) {
        super(gui.getFrame(), true);
        hqView = gui;
        unitAssignments = new HashMap<>();
        this.contract = contract;
        rdTracker = hqView.getCampaign().getRetirementDefectionTracker();
        if (doRetirement) {
            targetRolls = rdTracker.calculateTargetNumbers(contract, hqView.getCampaign());
        }
        currentPanel = doRetirement?PAN_OVERVIEW:PAN_RESULTS;
        setSize(new Dimension(800, 600));
        initComponents(doRetirement);
        if (!doRetirement) {
            initResults();
            btnDone.setEnabled(unitAssignmentsComplete());
        }

        setLocationRelativeTo(gui.getFrame());
        setUserPreferences(doRetirement);
    }

    private void initComponents(boolean doRetirement) {
        setTitle(resourceMap.getString("title.text"));

        setLayout(new BorderLayout());
        cardLayout = new CardLayout();
        panMain = new JPanel(cardLayout);
        add(panMain, BorderLayout.CENTER);
        txtInstructions = new JTextArea();
        add(txtInstructions, BorderLayout.PAGE_START);
        txtInstructions.setEditable(false);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setLineWrap(true);
        if (doRetirement) {
            String instructions;
            if (hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                instructions = resourceMap.getString("txtInstructions.OverviewShare.text");
            } else {
                instructions = resourceMap.getString("txtInstructions.Overview.text");
            }
            if (null == contract) {
                instructions += "\n\nDays since last Employee Turnover check: "
                        + ChronoUnit.DAYS.between(rdTracker.getLastRetirementRoll(),
                        hqView.getCampaign().getLocalDate());
            }
            txtInstructions.setText(instructions);
        } else {
            txtInstructions.setText(resourceMap.getString("txtInstructions.Results.text"));
        }
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        /* Overview Panel */
        if (doRetirement) {
            JPanel panOverview = new JPanel(new BorderLayout());

            cbGroupOverview = new JComboBox<>();
            for (PersonnelFilter filter : MekHQ.getMHQOptions().getPersonnelFilterStyle().getFilters(true)) {
                cbGroupOverview.addItem(filter);
            }
            JPanel panTop = new JPanel();
            panTop.setLayout(new BoxLayout(panTop, BoxLayout.X_AXIS));
            panTop.add(cbGroupOverview);
            panTop.add(Box.createHorizontalGlue());

            JLabel lblGeneralMod = new JLabel(resourceMap.getString("lblGeneralMod.text"));
            spnGeneralMod = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
            spnGeneralMod.setToolTipText(resourceMap.getString("spnGeneralMod.toolTipText"));
            if (hqView.getCampaign().getCampaignOptions().isUseCustomRetirementModifiers()) {
                panTop.add(lblGeneralMod);
                panTop.add(spnGeneralMod);
                spnGeneralMod.addChangeListener(evt -> personnelTable.setGeneralMod((Integer) spnGeneralMod.getValue()));
            }

            JLabel lblTotalDesc = new JLabel();
            lblTotal = new JLabel();
            lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
            if (hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                lblTotalDesc.setText(resourceMap.getString("lblTotalShares.text"));
                lblTotal.setText(Integer.toString(getTotalShares()));
            } else {
                lblTotalDesc.setText(resourceMap.getString("lblTotalBonus.text"));
                lblTotal.setText("0");
            }
            panTop.add(lblTotalDesc);
            panTop.add(Box.createRigidArea(new Dimension(5, 0)));
            panTop.add(lblTotal);
            panOverview.add(panTop, BorderLayout.PAGE_START);

            RetirementTableModel model = new RetirementTableModel(hqView.getCampaign());
            personnelTable = new RetirementTable(model, hqView);

            personnelSorter = new TableRowSorter<>(model);
            personnelSorter.setComparator(RetirementTableModel.COL_PERSON, new PersonRankStringSorter(hqView.getCampaign()));
            personnelSorter.setComparator(RetirementTableModel.COL_PAYOUT, new FormattedNumberSorter());
            personnelSorter.setComparator(RetirementTableModel.COL_BONUS_COST, new FormattedNumberSorter());
            personnelTable.setRowSorter(personnelSorter);
            ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(RetirementTableModel.COL_PERSON, SortOrder.DESCENDING));
            personnelSorter.setSortKeys(sortKeys);

            cbGroupOverview.addActionListener(evt -> filterPersonnel(personnelSorter, cbGroupOverview, false));

            personnelTable.getSelectionModel().addListSelectionListener(ev -> {
                if (personnelTable.getSelectedRow() < 0) {
                    return;
                }
                int row = personnelTable.convertRowIndexToModel(personnelTable.getSelectedRow());
                UUID id = ((RetirementTableModel)(personnelTable.getModel())).getPerson(row).getId();
                txtTargetDetails.setText(targetRolls.get(id).getDesc() +
                        (payBonus(id)?" -2 (Bonus)":"") +
                        ((miscModifier(id) != 0)?miscModifier(id) + " (Misc)":""));
            });

            personnelTable.getColumnModel().getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)).
            setCellEditor(new DefaultCellEditor(new JCheckBox()));
            XTableColumnModel columnModel = (XTableColumnModel) personnelTable.getColumnModel();
            columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAYOUT)), false);
            columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_UNIT)), false);
            columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_RECRUIT)), false);
            if (hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_BONUS_COST)), false);
                columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)), false);
            } else {
                columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_SHARES)), false);
            }
            columnModel.setColumnVisible(columnModel.getColumn(personnelTable.convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD)),
                    hqView.getCampaign().getCampaignOptions().isUseCustomRetirementModifiers());
            model.setData(targetRolls);
            model.addTableModelListener(ev -> {
                if (!hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                    Money bonus = getTotalBonus();
                    if (bonus.isGreaterThan(hqView.getCampaign().getFinances().getBalance())) {
                        lblTotal.setText("<html><font color='red'>"
                                + getTotalBonus().toAmountAndSymbolString() + "</font></html>");
                        btnRoll.setEnabled(false);
                    } else {
                        lblTotal.setText(getTotalBonus().toAmountAndSymbolString());
                        btnRoll.setEnabled(true);
                    }
                }
            });

            JScrollPane scroll = new JScrollPane();
            scroll.setViewportView(personnelTable);
            scroll.setPreferredSize(new Dimension(500, 500));
            panOverview.add(scroll, BorderLayout.CENTER);

            txtTargetDetails = new JTextArea();
            panOverview.add(txtTargetDetails, BorderLayout.PAGE_END);

            panMain.add(panOverview, PAN_OVERVIEW);
        }

        /* Results Panel */

        JPanel panRetirees = new JPanel(new BorderLayout());

        cbGroupResults = new JComboBox<>();
        for (PersonnelFilter filter : MekHQ.getMHQOptions().getPersonnelFilterStyle().getFilters(true)) {
            cbGroupResults.addItem(filter);
        }
        JPanel panTop = new JPanel();
        panTop.setLayout(new BoxLayout(panTop, BoxLayout.X_AXIS));
        panTop.add(cbGroupResults);
        panTop.add(Box.createHorizontalGlue());

        JLabel lblFinalPayout = new JLabel(resourceMap.getString("lblFinalPayout.text"));
        lblPayment = new JLabel();
        lblPayment.setHorizontalAlignment(SwingConstants.RIGHT);
        panTop.add(lblFinalPayout);
        panTop.add(Box.createRigidArea(new Dimension(5, 0)));
        panTop.add(lblPayment);
        cbUnitCategory = new JComboBox<>();
        cbUnitCategory.addItem("All Units");
        for (int i = 0; i < UnitType.SIZE; i++) {
            cbUnitCategory.addItem(UnitType.getTypeDisplayableName(i));
        }
        cbUnitCategory.setSelectedIndex(0);
        cbUnitCategory.addActionListener(evt -> filterUnits());
        panTop.add(cbUnitCategory);
        chkShowAllUnits = new JCheckBox("Show All Units");
        chkShowAllUnits.addActionListener(evt -> {
            if (chkShowAllUnits.isSelected()) {
                cbUnitCategory.setSelectedIndex(0);
            } else {
                setUnitGroup();
            }
            filterUnits();
        });
        panTop.add(Box.createHorizontalGlue());
        panTop.add(chkShowAllUnits);

        panRetirees.add(panTop, BorderLayout.PAGE_START);

        RetirementTableModel model = new RetirementTableModel(hqView.getCampaign());
        retireeTable = new RetirementTable(model, hqView);
        retireeSorter = new TableRowSorter<>(model);
        retireeSorter.setComparator(RetirementTableModel.COL_PERSON, new PersonRankStringSorter(hqView.getCampaign()));
        retireeTable.setRowSorter(retireeSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(RetirementTableModel.COL_PERSON, SortOrder.DESCENDING));
        retireeSorter.setSortKeys(sortKeys);
        cbGroupResults.addActionListener(evt -> filterPersonnel(retireeSorter, cbGroupResults, true));

        retireeTable.getSelectionModel().addListSelectionListener(ev -> {
            enableAddRemoveButtons();
            setUnitGroup();
        });
        model.addTableModelListener(evt -> lblPayment.setText(totalPayout().toAmountAndSymbolString()));

        XTableColumnModel columnModel = (XTableColumnModel) retireeTable.getColumnModel();
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_ASSIGN)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_FORCE)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_TARGET)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_BONUS_COST)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD)), false);
        columnModel.setColumnVisible(columnModel.getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_SHARES)), false);

        UnitAssignmentTableModel unitModel = new UnitAssignmentTableModel(hqView.getCampaign());
        unitAssignmentTable = new JTable(unitModel);
        unitAssignmentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        unitAssignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        columnModel = new XTableColumnModel();
        unitAssignmentTable.setColumnModel(columnModel);
        unitAssignmentTable.createDefaultColumnsFromModel();
        unitSorter = new TableRowSorter<>(unitModel);
        unitSorter.setComparator(UnitAssignmentTableModel.COL_UNIT, new WeightClassSorter());
        unitAssignmentTable.setRowSorter(unitSorter);
        ArrayList<RowSorter.SortKey> unitSortKeys = new ArrayList<>();
        unitSortKeys.add(new RowSorter.SortKey(UnitAssignmentTableModel.COL_UNIT, SortOrder.DESCENDING));
        sortKeys.add(new RowSorter.SortKey(UnitAssignmentTableModel.COL_UNIT, SortOrder.DESCENDING));
        unitSorter.setSortKeys(unitSortKeys);
        TableColumn column;
        for (int i = 0; i < UnitAssignmentTableModel.N_COL; i++) {
            column = unitAssignmentTable.getColumnModel().getColumn(unitAssignmentTable.convertColumnIndexToView(i));
            column.setPreferredWidth(model.getColumnWidth(i));
            column.setCellRenderer(unitModel.getRenderer(i));
        }

        unitAssignmentTable.setRowHeight(80);
        unitAssignmentTable.setIntercellSpacing(new Dimension(0, 0));
        unitAssignmentTable.setShowGrid(false);
        unitAssignmentTable.getSelectionModel().addListSelectionListener(ev -> enableAddRemoveButtons());

        JPanel panResults = new JPanel();
        panResults.setLayout(new BoxLayout(panResults, BoxLayout.X_AXIS));
        JScrollPane scroll = new JScrollPane();
        scroll.setViewportView(retireeTable);
        panResults.add(scroll);
        JPanel panAddRemoveBtns = new JPanel();
        panAddRemoveBtns.setLayout(new BoxLayout(panAddRemoveBtns, BoxLayout.Y_AXIS));
        btnAddUnit = new JButton("<<<");
        btnAddUnit.setEnabled(false);
        btnAddUnit.addActionListener(ev -> addUnit());
        panAddRemoveBtns.add(btnAddUnit);
        btnRemoveUnit = new JButton(">>>");
        btnRemoveUnit.setEnabled(false);
        btnRemoveUnit.addActionListener(ev -> removeUnit());
        panAddRemoveBtns.add(btnRemoveUnit);
        panResults.add(panAddRemoveBtns);

        scroll = new JScrollPane();
        scroll.setViewportView(unitAssignmentTable);
        panResults.add(scroll);

        panRetirees.add(panResults, BorderLayout.CENTER);
        panMain.add(panRetirees, PAN_RESULTS);

        cardLayout.show(panMain, currentPanel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.addActionListener(buttonListener);
        btnEdit = new JToggleButton(resourceMap.getString("btnEdit.text"));
        btnEdit.addActionListener(buttonListener);
        btnEdit.setVisible(currentPanel.equals(PAN_RESULTS));
        btnEdit.setEnabled(hqView.getCampaign().isGM());
        btnEdit.addActionListener(evt -> {
            btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
            ((RetirementTableModel) retireeTable.getModel()).setEditPayout(btnEdit.isSelected());
        });
        btnRoll = new JButton(resourceMap.getString("btnRoll.text"));
        btnRoll.addActionListener(buttonListener);
        btnDone = new JButton(resourceMap.getString("btnDone.text"));
        btnDone.addActionListener(buttonListener);
        btnPanel.add(btnCancel);
        btnPanel.add(btnEdit);
        btnPanel.add(btnRoll);
        btnPanel.add(btnDone);
        btnRoll.setVisible(doRetirement);
        btnDone.setVisible(!doRetirement);
        add(btnPanel, BorderLayout.PAGE_END);
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences(boolean doRetirement) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(RetirementDefectionDialog.class);

            if (doRetirement) {
                cbGroupOverview.setName("group");
                preferences.manage(new JComboBoxPreference(cbGroupOverview));

                spnGeneralMod.setName("modifier");
                preferences.manage(new JIntNumberSpinnerPreference(spnGeneralMod));
            }

            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    public ActionListener buttonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev) {
            if (ev.getSource().equals(btnRoll)) {
                for (UUID id : targetRolls.keySet()) {
                    if (payBonus(id)) {
                        targetRolls.get(id).addModifier(-2, "Bonus");
                    }

                    if (miscModifier(id) != 0) {
                        targetRolls.get(id).addModifier(miscModifier(id), "Misc");
                    }
                }
                rdTracker.rollRetirement(contract, targetRolls,
                        RetirementDefectionTracker.getShareValue(hqView.getCampaign()),
                                hqView.getCampaign());
                initResults();

                btnEdit.setVisible(true);
                btnRoll.setVisible(false);
                btnDone.setVisible(true);
                btnDone.setEnabled(unitAssignmentsComplete());

                currentPanel = PAN_RESULTS;
                cardLayout.show(panMain, currentPanel);
                if (hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                    txtInstructions.setText(resourceMap.getString("txtInstructions.ResultsShare.text"));
                } else {
                    txtInstructions.setText(resourceMap.getString("txtInstructions.Results.text"));
                }
                if (getTotalBonus().isPositive()) {
                    hqView.getCampaign().getFinances().debit(TransactionType.SALARIES,
                            hqView.getCampaign().getLocalDate(), getTotalBonus(), "Bonus Payments");
                }
            } else if (ev.getSource().equals(btnDone)) {
                for (UUID pid : ((RetirementTableModel) retireeTable.getModel()).getAltPayout().keySet()) {
                    rdTracker.getPayout(pid).setPayoutAmount(((RetirementTableModel) retireeTable.getModel())
                            .getAltPayout().get(pid));
                }
                aborted = false;
                setVisible(false);
            } else if (ev.getSource().equals(btnCancel)) {
                aborted = true;
                setVisible(false);
            }
        }
    };

    private void initResults() {
        /* Find unassigned units that can be stolen */
        List<UUID> unassignedMechs = new ArrayList<>();
        List<UUID> unassignedASF = new ArrayList<>();
        ArrayList<UUID> availableUnits = new ArrayList<>();
        hqView.getCampaign().getHangar().forEachUnit(u -> {
            if (!u.isAvailable() && !u.isMothballing() && !u.isMothballed()) {
                return;
            }
            availableUnits.add(u.getId());
            if (UnitType.MEK == u.getEntity().getUnitType()) {
                if (null == u.getCommander()) {
                    unassignedMechs.add(u.getId());
                }
            }
            if (UnitType.AEROSPACEFIGHTER == u.getEntity().getUnitType()) {
                if (null == u.getCommander()) {
                    unassignedASF.add(u.getId());
                }
            }
        });

        /* Defectors who steal a unit will take either the one they were
         * piloting or one of the unassigned units (50/50, unless there
         * is only one choice)
         */
        for (UUID id : rdTracker.getRetirees(contract)) {
            Person p = hqView.getCampaign().getPerson(id);
            if (rdTracker.getPayout(id).hasStolenUnit()) {
                boolean unassignedAvailable =
                        (!unassignedMechs.isEmpty() && p.getPrimaryRole().isMechWarrior())
                        || (!unassignedASF.isEmpty() && p.getPrimaryRole().isAerospacePilot());
                /*
                 * If a unit has previously been assigned, check that it is still available
                 * and either assigned to the current player or unassigned. If so, keep
                 * the previous value.
                 */
                if ((null != rdTracker.getPayout(id).getStolenUnitId())
                        && (null != hqView.getCampaign().getUnit(rdTracker.getPayout(id).getStolenUnitId()))
                        && p.equals(hqView.getCampaign().getUnit(rdTracker.getPayout(id).getStolenUnitId()).getCommander())) {
                    continue;
                }

                if ((p.getUnit() != null) && ((Compute.d6() < 4) || !unassignedAvailable)) {
                    unitAssignments.put(id, p.getUnit().getId());
                } else if (unassignedAvailable) {
                    if (p.getPrimaryRole().isMechWarrior()) {
                        int roll = Compute.randomInt(unassignedMechs.size());
                        unitAssignments.put(id, unassignedMechs.get(roll));
                        rdTracker.getPayout(id).setStolenUnitId(unassignedMechs.get(roll));
                        availableUnits.remove(unassignedMechs.get(roll));
                        unassignedMechs.remove(roll);
                    } else if (p.getPrimaryRole().isAerospacePilot()) {
                        int roll = Compute.randomInt(unassignedASF.size());
                        unitAssignments.put(id, unassignedASF.get(roll));
                        rdTracker.getPayout(id).setStolenUnitId(unassignedASF.get(roll));
                        availableUnits.remove(unassignedASF.get(roll));
                        unassignedASF.remove(roll);
                    }
                }
            }
            /* Retirees who brought a unit will take the same unit when
             * they go if it is still around and has not been stolen.
             */
            if (hqView.getCampaign().getCampaignOptions().isTrackOriginalUnit()
                    && (null != p.getOriginalUnitId())
                    && !unitAssignments.containsValue(p.getOriginalUnitId())
                    && (hqView.getCampaign().getUnit(p.getOriginalUnitId()) != null)) {
                unitAssignments.put(id, p.getOriginalUnitId());
                if (hqView.getCampaign().getCampaignOptions().isUseShareSystem()) {
                    Money temp = rdTracker.getPayout(id).getPayoutAmount()
                            .minus(hqView.getCampaign().getUnit(p.getOriginalUnitId()).getBuyCost());

                    if (temp.isNegative()) {
                        temp = Money.zero();
                    }

                    rdTracker.getPayout(id).setPayoutAmount(temp);
                }
            }
            /*
             * For infantry, the unit commander makes a retirement roll on behalf of the
             * entire unit. Unassigned infantry can retire individually.
             */
            if ((p.getUnit() != null) && p.getPrimaryRole().isSoldierOrBattleArmour()) {
                unitAssignments.put(id, p.getUnit().getId());
            }
            ((UnitAssignmentTableModel) unitAssignmentTable.getModel()).setData(availableUnits);
        }

        ArrayList<UUID> retireeList = new ArrayList<>();
        boolean showRecruitColumn = false;
        for (UUID pid : rdTracker.getRetirees(contract)) {
            retireeList.add(pid);
            if ((hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).getDependents() > 0)
                    || hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).hasHeir()
                    || hqView.getCampaign().getRetirementDefectionTracker().getPayout(pid).hasRecruit()) {
                showRecruitColumn = true;
            }
        }
        ((XTableColumnModel) retireeTable.getColumnModel()).setColumnVisible(retireeTable.getColumnModel().getColumn(retireeTable.convertColumnIndexToView(RetirementTableModel.COL_RECRUIT)), !showRecruitColumn);
        ((RetirementTableModel) retireeTable.getModel()).setData(retireeList, unitAssignments);
        filterPersonnel(retireeSorter, cbGroupResults, true);
        lblPayment.setText(totalPayout().toAmountAndSymbolString());
    }

    private void filterPersonnel(TableRowSorter<RetirementTableModel> sorter, JComboBox<PersonnelFilter> comboBox, boolean resultsView) {
        PersonnelFilter nGroup = (comboBox.getSelectedItem() != null)
                ? (PersonnelFilter) comboBox.getSelectedItem()
                : PersonnelFilter.ACTIVE;

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends RetirementTableModel, ? extends Integer> entry) {
                Person person = entry.getModel().getPerson(entry.getIdentifier());
                if (resultsView && (rdTracker.getRetirees(contract) != null)
                        && !rdTracker.getRetirees(contract).contains(person.getId())) {
                    return false;
                } else {
                    return nGroup.getFilteredInformation(person, hqView.getCampaign().getLocalDate());
                }
            }
        });
    }

    public void filterUnits() {
        final int nGroup = cbUnitCategory.getSelectedIndex() - 1;
        RowFilter<UnitAssignmentTableModel, Integer> unitTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends UnitAssignmentTableModel, ? extends Integer> entry) {
                UnitAssignmentTableModel unitModel = entry.getModel();
                Unit unit = unitModel.getUnit(entry.getIdentifier());
                if (!chkShowAllUnits.isSelected() &&
                        retireeTable.getSelectedRow() >= 0) {
                    Person selectedPerson = ((RetirementTableModel) retireeTable.getModel()).
                            getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
                    if (null != rdTracker.getPayout(selectedPerson.getId()) &&
                            rdTracker.getPayout(selectedPerson.getId()).getWeightClass() > 0 &&
                            weightClassIndex(unit) != rdTracker.getPayout(selectedPerson.getId()).getWeightClass()) {
                        return false;
                    }
                }
                /* Can't really give a platoon as payment */
                if ((unit.getEntity().getUnitType() == UnitType.BATTLE_ARMOR)
                        || (unit.getEntity().getUnitType() == UnitType.INFANTRY)) {
                    return false;
                }
                if (unitAssignments.containsValue(unit.getId())) {
                    return false;
                }
                if (nGroup < 0) {
                    return true;
                }
                Entity en = unit.getEntity();
                int type = -1;
                if (null != en) {
                    type = unit.getEntity().getUnitType();
                }
                return type == nGroup;
            }
        };
        unitSorter.setRowFilter(unitTypeFilter);
    }

    public static int weightClassIndex(Unit u) {
        int retVal = u.getEntity().getWeightClass();
        if (u.getEntity().isClan() || (u.getEntity().getTechLevel() > megamek.common.TechConstants.T_INTRO_BOXSET)) {
            retVal++;
        }
        if (!u.isFunctional()) {
            retVal--;
        }
        return Math.max(0, retVal);
    }

    public Money totalPayout() {
        if (null == rdTracker.getRetirees(contract)) {
            return Money.zero();
        }
        Money retVal = Money.zero();
        for (UUID id : rdTracker.getRetirees(contract)) {
            if (null == rdTracker.getPayout(id)) {
                continue;
            }
            if (((RetirementTableModel) retireeTable.getModel()).getAltPayout().containsKey(id)) {
                retVal = retVal.plus(((RetirementTableModel) retireeTable.getModel()).getAltPayout().get(id));
                continue;
            }
            Money payout = rdTracker.getPayout(id).getPayoutAmount();
            /* If no unit is required as part of the payout, the unit is part or all of the
             * final payout.
             */
            if ((rdTracker.getPayout(id).getWeightClass() == 0 &&
                    null != unitAssignments.get(id) &&
                            null != hqView.getCampaign().getUnit(unitAssignments.get(id)))) {
                payout = payout.minus(hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost());
            } else if ((hqView.getCampaign().getCampaignOptions().isUseShareSystem() &&
                            hqView.getCampaign().getCampaignOptions().isTrackOriginalUnit() &&
                            hqView.getCampaign().getPerson(id).getOriginalUnitId() == unitAssignments.get(id)) &&
                    null != hqView.getCampaign().getUnit(unitAssignments.get(id))) {
                payout = payout.minus(hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost());
            }
            /*  If using the share system and tracking the original unit,
             * the payout is also reduced by the value of the unit.
             */
            if (hqView.getCampaign().getCampaignOptions().isUseShareSystem() &&
                    hqView.getCampaign().getCampaignOptions().isTrackOriginalUnit() &&
                    hqView.getCampaign().getPerson(id).getOriginalUnitId() == unitAssignments.get(id) &&
                    null != hqView.getCampaign().getUnit(unitAssignments.get(id))) {
                payout = payout.minus(hqView.getCampaign().getUnit(unitAssignments.get(id)).getBuyCost());
            }
            /* If the unit given in payment is of lower quality than required, pay
             * an additional 3M C-bills per class.
             */
            if (null != unitAssignments.get(id)) {
                payout = payout.plus(getShortfallAdjustment(
                        rdTracker.getPayout(id).getWeightClass(),
                        RetirementDefectionDialog.weightClassIndex(hqView.getCampaign().getUnit(unitAssignments.get(id)))));
            }
            /* If the pilot has stolen a unit, there is no payout */
            if (rdTracker.getPayout(id).hasStolenUnit() && (null != unitAssignments.get(id))) {
                payout = Money.zero();
            }
            // If the payout is negative just set it to zero
            if (payout.isNegative()) {
                payout = Money.zero();
            }

            retVal = retVal.plus(payout);
        }
        return retVal;
    }

    public boolean payBonus(UUID id) {
        return ((RetirementTableModel) personnelTable.getModel()).getPayBonus(id);
    }

    public int miscModifier(UUID id) {
        return ((RetirementTableModel) personnelTable.getModel()).getMiscModifier(id) +
                (Integer) spnGeneralMod.getValue();
    }

    private int getTotalShares() {
        return targetRolls.keySet().stream()
                .mapToInt(id -> hqView.getCampaign().getPerson(id)
                        .getNumShares(hqView.getCampaign(), hqView.getCampaign().getCampaignOptions().isSharesForAll()))
                .sum();
    }

    private Money getTotalBonus() {
        Money retVal = Money.zero();
        for (UUID id : targetRolls.keySet()) {
            if (((RetirementTableModel) personnelTable.getModel()).getPayBonus(id)) {
                retVal = retVal.plus(RetirementDefectionTracker.getBonusCost(hqView.getCampaign(),
                        hqView.getCampaign().getPerson(id)));
            }
        }
        return retVal;
    }

    /* It is possible that there may not be enough units of the required
     * weight/tech level for all retirees. This is not addressed by the AtB
     * rules, so I have improvised by allowing smaller units to be given,
     * but at a penalty of 3,000,000 C-bills per class difference (based
     * on the values given in IOps Beta/Creating a Force).
     */
    public static Money getShortfallAdjustment(int required, int actual) {
        if (actual >= required) {
            return Money.zero();
        } else {
            return Money.of((required - actual) * 3000000);
        }
    }

    public UUID getUnitId(UUID pid) {
        return unitAssignments.get(pid);
    }

    public Map<UUID, UUID> getUnitAssignments() {
        return unitAssignments;
    }

    public boolean wasAborted() {
        return aborted;
    }

    private boolean unitAssignmentsComplete() {
        return rdTracker.getRetirees(contract).stream()
                .noneMatch(id -> (rdTracker.getPayout(id).getWeightClass() > 0) && !unitAssignments.containsKey(id));
    }

    private void enableAddRemoveButtons() {
        if (retireeTable.getSelectedRow() < 0) {
            btnAddUnit.setEnabled(false);
            btnRemoveUnit.setEnabled(false);
        } else {
            int retireeRow = retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow());
            UUID pid = ((RetirementTableModel)(retireeTable.getModel())).getPerson(retireeRow).getId();
            if (null != rdTracker.getPayout(pid) &&
                    rdTracker.getPayout(pid).hasStolenUnit() &&
                    !btnEdit.isSelected()) {
                btnAddUnit.setEnabled(false);
                btnRemoveUnit.setEnabled(false);
            } else if (hqView.getCampaign().getPerson(pid).getPrimaryRole().isSoldierOrBattleArmour()) {
                btnAddUnit.setEnabled(false);
                btnRemoveUnit.setEnabled(false);
            } else if (unitAssignments.containsKey(pid)) {
                btnAddUnit.setEnabled(false);
                if ((hqView.getCampaign().getCampaignOptions().isTrackOriginalUnit() &&
                        unitAssignments.get(pid).equals(hqView.getCampaign().getPerson(pid).getOriginalUnitId())) &&
                        !btnEdit.isSelected()) {
                    btnRemoveUnit.setEnabled(false);
                } else {
                    btnRemoveUnit.setEnabled(true);
                }
            } else if (null != rdTracker.getPayout(pid) &&
                    rdTracker.getPayout(pid).getWeightClass() > 0) {
                if (unitAssignmentTable.getSelectedRow() < 0) {
                    btnAddUnit.setEnabled(false);
                } else if (btnEdit.isSelected()) {
                    btnAddUnit.setEnabled(true);
                } else {
                    Unit unit = ((UnitAssignmentTableModel) unitAssignmentTable.getModel())
                            .getUnit(unitAssignmentTable.convertRowIndexToModel(unitAssignmentTable.getSelectedRow()));
                    btnAddUnit.setEnabled(hqView.getCampaign().getPerson(pid).canDrive(unit.getEntity()));
                }
                btnRemoveUnit.setEnabled(false);
            } else {
                btnAddUnit.setEnabled(unitAssignmentTable.getSelectedRow() >= 0);
                btnRemoveUnit.setEnabled(false);
            }
        }
    }

    private void addUnit() {
        Person person = ((RetirementTableModel) retireeTable.getModel())
                .getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
        Unit unit = ((UnitAssignmentTableModel) unitAssignmentTable.getModel())
                .getUnit(unitAssignmentTable.convertRowIndexToModel(unitAssignmentTable.getSelectedRow()));
        unitAssignments.put(person.getId(), unit.getId());
        btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
        ((RetirementTableModel) retireeTable.getModel()).fireTableDataChanged();
        filterUnits();
    }

    private void removeUnit() {
        Person person = ((RetirementTableModel) retireeTable.getModel())
                .getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
        unitAssignments.remove(person.getId());
        btnDone.setEnabled(btnEdit.isSelected() || unitAssignmentsComplete());
        ((RetirementTableModel) retireeTable.getModel()).fireTableDataChanged();
        filterUnits();
    }

    private void setUnitGroup() {
        if (!chkShowAllUnits.isSelected() && (retireeTable.getSelectedRow() >= 0)) {
            Person p = ((RetirementTableModel) retireeTable.getModel())
                    .getPerson(retireeTable.convertRowIndexToModel(retireeTable.getSelectedRow()));
            switch (p.getPrimaryRole()) {
                case MECHWARRIOR:
                    cbUnitCategory.setSelectedIndex(UnitType.MEK + 1);
                    break;
                case GROUND_VEHICLE_DRIVER:
                case VEHICLE_GUNNER:
                    cbUnitCategory.setSelectedIndex(UnitType.TANK + 1);
                    break;
                case NAVAL_VEHICLE_DRIVER:
                    cbUnitCategory.setSelectedIndex(UnitType.NAVAL + 1);
                    break;
                case VTOL_PILOT:
                    cbUnitCategory.setSelectedIndex(UnitType.VTOL + 1);
                    break;
                case AEROSPACE_PILOT:
                    cbUnitCategory.setSelectedIndex(UnitType.AEROSPACEFIGHTER + 1);
                    break;
                case CONVENTIONAL_AIRCRAFT_PILOT:
                    cbUnitCategory.setSelectedIndex(UnitType.CONV_FIGHTER + 1);
                    break;
                case PROTOMECH_PILOT:
                    cbUnitCategory.setSelectedIndex(UnitType.PROTOMEK + 1);
                    break;
                case BATTLE_ARMOUR:
                    cbUnitCategory.setSelectedIndex(UnitType.BATTLE_ARMOR + 1);
                    break;
                case SOLDIER:
                    cbUnitCategory.setSelectedIndex(UnitType.INFANTRY + 1);
                    break;
                default:
                    cbUnitCategory.setSelectedIndex(0);
            }
            filterUnits();
        }
    }
}

class RetirementTable extends JTable {
    private static class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private JSpinner spinner;

        public SpinnerEditor() {
            spinner = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setEditable(false);
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            spinner.setValue(value);
            return spinner;
        }
    }

    public RetirementTable(RetirementTableModel model, CampaignGUI hqView) {
        super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel columnModel = new XTableColumnModel();
        setColumnModel(columnModel);
        createDefaultColumnsFromModel();
        TableColumn column;
        for (int i = 0; i < RetirementTableModel.N_COL; i++) {
            column = getColumnModel().getColumn(convertColumnIndexToView(i));
            column.setPreferredWidth(model.getColumnWidth(i));
            if ((i != RetirementTableModel.COL_PAY_BONUS) && (i != RetirementTableModel.COL_MISC_MOD)) {
                column.setCellRenderer(model.getRenderer(i));
            }
        }

        setRowHeight(80);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS))
                .setCellEditor(new DefaultCellEditor(new JCheckBox()));

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD))
                .setCellEditor(new SpinnerEditor());

        JComboBox<PersonnelRole> cbRecruitRole = new JComboBox<>(PersonnelRole.values());
        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_RECRUIT))
                .setCellEditor(new DefaultCellEditor(cbRecruitRole));
    }

    public void setGeneralMod(int mod) {
        ((RetirementTableModel) getModel()).setGeneralMod(mod);
    }
}
