/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static megamek.client.ui.WrapLayout.wordWrap;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.model.DataTableModel;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * Against the Bot Shows how many lances are required to be deployed on active contracts and in what roles and allows
 * the player to assign units to those roles.
 *
 * @author Neoancient
 */
public class LanceAssignmentView extends JPanel {
    private final Campaign campaign;

    private JTable tblRequiredLances;
    private JTable tblAssignments;
    private JPanel panRequiredLances;
    private JComboBox<AtBContract> cbContract;

    public LanceAssignmentView(Campaign c) {
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        cbContract = new JComboBox<>();
        cbContract.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                return new JLabel((null == value) ? "None" : ((AtBContract) value).getName());
            }
        });

        JComboBox<CombatRole> cbRole = getCbRole();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        RequiredLancesTableModel rlModel = new RequiredLancesTableModel(campaign);
        tblRequiredLances = new JTable(rlModel);
        tblRequiredLances.setColumnModel(new XTableColumnModel());
        tblRequiredLances.createDefaultColumnsFromModel();
        tblRequiredLances.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column;
        for (int i = 0; i < RequiredLancesTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel) tblRequiredLances.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(rlModel.getColumnWidth(i));
            column.setCellRenderer(new MekHqTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                      boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(((RequiredLancesTableModel) table.getModel()).getAlignment(table.convertColumnIndexToModel(
                          column)));
                    if (table.convertColumnIndexToModel(column) > RequiredLancesTableModel.COL_CONTRACT) {
                        if (((String) value).indexOf('/') >= 0) {
                            setForeground(MekHQ.getMHQOptions().getBelowContractMinimumForeground());
                        }
                    }
                    return this;
                }
            });
        }
        TableRowSorter<RequiredLancesTableModel> sorter = new TableRowSorter<>(rlModel);
        tblRequiredLances.setRowSorter(sorter);

        tblRequiredLances.setIntercellSpacing(new Dimension(0, 0));
        tblRequiredLances.setShowGrid(false);

        LanceAssignmentTableModel laModel = new LanceAssignmentTableModel(campaign);
        tblAssignments = new JTable(laModel);
        tblAssignments.setColumnModel(new XTableColumnModel());
        tblAssignments.createDefaultColumnsFromModel();
        tblAssignments.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < LanceAssignmentTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel) tblAssignments.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(rlModel.getColumnWidth(i));
            column.setCellRenderer(new MekHqTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                      boolean hasFocus, int row, int column) {
                    switch (column) {
                        case LanceAssignmentTableModel.COL_FORCE:
                            if (null != value) {
                                String forceName = (((Formation) value)).getFullName();
                                String originNodeName = ", " + campaign.getFormation(0).getName();
                                forceName = forceName.replaceAll(originNodeName, "");
                                setText(forceName);
                            }
                            break;
                        case LanceAssignmentTableModel.COL_CONTRACT:
                            if (null == value) {
                                setText("None");
                            } else {
                                setText(((AtBContract) value).getName());
                            }
                            break;
                        default:
                            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    }
                    return this;
                }
            });

            if (i == LanceAssignmentTableModel.COL_CONTRACT) {
                column.setCellEditor(new DefaultCellEditor(cbContract));
            }

            if (i == LanceAssignmentTableModel.COL_ROLE) {
                column.setCellEditor(new DefaultCellEditor(cbRole));
            }
        }

        RowFilter<LanceAssignmentTableModel, Integer> laFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends LanceAssignmentTableModel, ? extends Integer> entry) {
                CombatTeam combatTeam = entry.getModel().getRow(entry.getIdentifier());
                return combatTeam.isEligible(campaign);
            }
        };
        final NaturalOrderComparator noc = new NaturalOrderComparator();
        TableRowSorter<LanceAssignmentTableModel> laSorter = new TableRowSorter<>(laModel);
        laSorter.setRowFilter(laFilter);
        laSorter.setComparator(LanceAssignmentTableModel.COL_FORCE, forceComparator);
        laSorter.setComparator(LanceAssignmentTableModel.COL_CONTRACT,
              (c1, c2) -> noc.compare(((AtBContract) c1).getName(), ((AtBContract) c2).getName()));
        laSorter.setComparator(LanceAssignmentTableModel.COL_ROLE,
              (r1, r2) -> noc.compare(r1.toString(), r2.toString()));
        List<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(LanceAssignmentTableModel.COL_FORCE, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        tblAssignments.setRowSorter(laSorter);

        tblAssignments.setIntercellSpacing(new Dimension(0, 0));
        tblAssignments.setShowGrid(false);

        panRequiredLances = new JPanel();
        panRequiredLances.setLayout(new BoxLayout(panRequiredLances, BoxLayout.Y_AXIS));
        panRequiredLances.setBorder(RoundedLineBorder.createRoundedLineBorder("Deployment Requirements"));
        panRequiredLances.add(tblRequiredLances.getTableHeader());
        panRequiredLances.add(tblRequiredLances);
        add(panRequiredLances);

        JPanel panAssignments = new JPanel();
        panAssignments.setLayout(new BoxLayout(panAssignments, BoxLayout.Y_AXIS));
        panAssignments.setBorder(RoundedLineBorder.createRoundedLineBorder("Current Assignments"));
        panAssignments.add(tblAssignments.getTableHeader());
        panAssignments.add(tblAssignments);
        add(panAssignments);

        refresh();
        tblAssignments.getModel().addTableModelListener(assignmentTableListener);
    }

    private JComboBox<CombatRole> getCbRole() {
        JComboBox<CombatRole> cbRole = new JComboBox<>(CombatRole.values());
        cbRole.setName("cbRole");
        cbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                  final boolean isSelected, final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CombatRole) {
                    list.setToolTipText(wordWrap(((CombatRole) value).getToolTipText()));
                }
                return this;
            }
        });
        return cbRole;
    }

    public void refresh() {
        cbContract.removeAllItems();
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts();
        for (AtBContract contract : activeContracts) {
            cbContract.addItem(contract);
        }
        AtBContract defaultContract = activeContracts.isEmpty() ? null : activeContracts.get(0);
        for (CombatTeam combatTeam : campaign.getCombatTeamsAsMap().values()) {
            if ((combatTeam.getContract(campaign) == null) ||
                      !combatTeam.getContract(campaign).isActiveOn(campaign.getLocalDate(), true)) {
                combatTeam.setContract(defaultContract);
            }
        }

        ((DataTableModel<AtBContract>) tblRequiredLances.getModel()).setData(activeContracts);
        ((DataTableModel<CombatTeam>) tblAssignments.getModel()).setData(campaign.getCombatTeamsAsList());
        panRequiredLances.setVisible(tblRequiredLances.getRowCount() > 0);
    }

    TableModelListener assignmentTableListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent ev) {
            ((RequiredLancesTableModel) tblRequiredLances.getModel()).fireTableDataChanged();
        }
    };

    /**
     * Sorts Force objects according to where they appear on the TO&amp;E
     */
    public Comparator<Formation> forceComparator = (f1, f2) -> {
        /* Check whether they are the same or one is an ancestor of the other */
        if (f1.getId() == f2.getId()) {
            return 0;
        }
        if (f1.isAncestorOf(f2)) {
            return -1;
        }
        if (f2.isAncestorOf(f1)) {
            return 1;
        }

        // Find the closest common ancestor. They must be either from the same force or descend from
        // different subForces of this one.
        Formation f = f1;
        while (!f.isAncestorOf(f2)) {
            f = f.getParentFormation();
        }
        for (Formation sf : f.getSubFormations()) {
            if (sf.isAncestorOf(f1) || sf.getId() == f1.getId()) {
                return -1;
            }

            if (sf.isAncestorOf(f2) || sf.getId() == f2.getId()) {
                return 1;
            }
        }
        /* We should never get here. */
        return 0;
    };
}

