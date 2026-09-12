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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;
import megamek.common.ui.FastJScrollPane;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.gui.model.DataTableModel;
import mekhq.gui.utilities.BriefingStyle;
import mekhq.gui.utilities.MekHqTableCellRenderer;

/**
 * Against the Bot Shows how many lances are required to be deployed on active contracts and in what roles and allows
 * the player to assign units to those roles.
 *
 * @author Neoancient
 */
public class LanceAssignmentView extends JPanel {
    private static final String FLATLAF_STYLE_CLASS = "FlatLaf.styleClass";
    private static final int ASSIGNMENT_TABLE_ROW_HEIGHT = 24;

    private final Campaign campaign;
    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    private JTable tblAssignments;
    private JComboBox<AbstractContract> cbContract;
    private LanceAssignmentTableModel lanceAssignmentModel;
    private Runnable assignmentChangeListener;

    public LanceAssignmentView(Campaign c) {
        campaign = c;
        initComponents();
    }

    /**
     * Registers a listener that is invoked whenever the unit assignments change (e.g. a force's contract or role is
     * edited). This allows external views, such as the contract summary panel, to refresh derived information like
     * deployment coverage.
     *
     * @param listener the action to run when assignments change, or {@code null} to clear it
     */
    public void setAssignmentChangeListener(Runnable listener) {
        this.assignmentChangeListener = listener;
    }

    private void initComponents() {
        cbContract = new JComboBox<>();
        styleCompactComponent(cbContract);
        cbContract.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                return new JLabel((null == value) ? "None" : ((AbstractContract) value).getName());
            }
        });

        JComboBox<CombatRole> cbRole = getCbRole();

        setLayout(new BorderLayout(0, 5));

        lanceAssignmentModel = new LanceAssignmentTableModel(campaign);
        tblAssignments = new JTable(lanceAssignmentModel);
        tblAssignments.setColumnModel(new XTableColumnModel());
        tblAssignments.createDefaultColumnsFromModel();
        tblAssignments.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < LanceAssignmentTableModel.COL_NUM; i++) {
            TableColumn column = ((XTableColumnModel) tblAssignments.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(lanceAssignmentModel.getColumnWidth(i));
            column.setCellRenderer(new MekHqTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                      boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    int modelColumn = table.convertColumnIndexToModel(column);
                    setHorizontalAlignment(((LanceAssignmentTableModel) table.getModel()).getAlignment(modelColumn));
                    switch (modelColumn) {
                        case LanceAssignmentTableModel.COL_FORCE:
                            if (null != value) {
                                String forceName = (((Formation) value)).getFullName();
                                String originNodeName = ", " + campaign.getPlayerForce().getFormation(0).getName();
                                forceName = forceName.replaceAll(originNodeName, "");
                                setText(forceName);
                            } else {
                                setText("");
                            }
                            break;
                        case LanceAssignmentTableModel.COL_CONTRACT:
                            if (null == value) {
                                setText("None");
                            } else {
                                setText(((AbstractContract) value).getName());
                            }
                            break;
                        default:
                            break;
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

        RowFilter<LanceAssignmentTableModel, Integer> lanceAssignmentFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends LanceAssignmentTableModel, ? extends Integer> entry) {
                CombatTeam combatTeam = entry.getModel().getRow(entry.getIdentifier());
                return combatTeam.isEligible(campaign);
            }
        };
        final NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        TableRowSorter<LanceAssignmentTableModel> lanceAssignmentSorter = new TableRowSorter<>(
              lanceAssignmentModel);
        lanceAssignmentSorter.setRowFilter(lanceAssignmentFilter);
        lanceAssignmentSorter.setComparator(LanceAssignmentTableModel.COL_FORCE, forceComparator);
        lanceAssignmentSorter.setComparator(LanceAssignmentTableModel.COL_CONTRACT,
              (firstContract, secondContract) -> naturalOrderComparator.compare(
                    (firstContract == null) ? "" : ((AbstractContract) firstContract).getName(),
                    (secondContract == null) ? "" : ((AbstractContract) secondContract).getName()));
        lanceAssignmentSorter.setComparator(LanceAssignmentTableModel.COL_ROLE,
              (firstRole, secondRole) -> naturalOrderComparator.compare(firstRole.toString(),
                    secondRole.toString()));
        List<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(LanceAssignmentTableModel.COL_FORCE, SortOrder.ASCENDING));
        lanceAssignmentSorter.setSortKeys(sortKeys);
        tblAssignments.setRowSorter(lanceAssignmentSorter);

        tblAssignments.setIntercellSpacing(new Dimension(0, 0));
        tblAssignments.setShowGrid(false);
        tblAssignments.setFillsViewportHeight(true);
        styleAssignmentTable(tblAssignments);

        JPanel panAssignments = BriefingStyle.createSectionPanel(resourceMap.getString(
              "briefingTab.assignments.current.title"));
        JScrollPane assignmentsScrollPane = new FastJScrollPane(tblAssignments);
        assignmentsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        panAssignments.add(assignmentsScrollPane, BorderLayout.CENTER);

        add(panAssignments, BorderLayout.CENTER);

        refresh();
        tblAssignments.getModel().addTableModelListener(assignmentTableListener);
    }

    private void styleAssignmentTable(JTable table) {
        table.putClientProperty(FLATLAF_STYLE_CLASS, "small");
        table.setRowHeight(Math.max(table.getRowHeight(), ASSIGNMENT_TABLE_ROW_HEIGHT));
        if (table.getTableHeader() != null) {
            table.getTableHeader().putClientProperty(FLATLAF_STYLE_CLASS, "small");
            table.getTableHeader().setReorderingAllowed(false);
        }
    }

    private void styleCompactComponent(JComponent component) {
        component.putClientProperty(FLATLAF_STYLE_CLASS, "small");
    }

    private JComboBox<CombatRole> getCbRole() {
        JComboBox<CombatRole> cbRole = new JComboBox<>(CombatRole.values());
        cbRole.setName("cbRole");
        styleCompactComponent(cbRole);
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
        List<AbstractContract> activeContracts = campaign.getActiveContracts();
        for (AbstractContract contract : activeContracts) {
            cbContract.addItem(contract);
        }
        AbstractContract defaultContract = activeContracts.isEmpty() ? null : activeContracts.getFirst();
        for (CombatTeam combatTeam : campaign.getPlayerForce().getCombatTeamsAsMap(campaign).values()) {
            if ((combatTeam.getContract(campaign) == null) ||
                      !combatTeam.getContract(campaign).isActiveOn(campaign.getLocalDate())) {
                combatTeam.setContract(defaultContract);
            }
        }

        ((DataTableModel<CombatTeam>) tblAssignments.getModel()).setData(campaign.getPlayerForce()
                                                                               .getCombatTeamsAsList(campaign));
    }

    TableModelListener assignmentTableListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent ev) {
            if (assignmentChangeListener != null) {
                assignmentChangeListener.run();
            }
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

