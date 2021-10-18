/*
 * LanceAssignmentView.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
package mekhq.gui.view;

import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.model.DataTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Against the Bot
 * Shows how many lances are required to be deployed on active contracts and
 * in what roles and allows the player to assign units to those roles.
 *
 * @author Neoancient
 */
public class LanceAssignmentView extends JPanel {
    private static final long serialVersionUID = 7280552346074838142L;

    private final Campaign campaign;

    private JTable tblRequiredLances;
    private JTable tblAssignments;
    private JPanel panRequiredLances;
    private JPanel panAssignments;
    private JComboBox<AtBContract> cbContract;
    private JComboBox<AtBLanceRole> cbRole;

    public LanceAssignmentView(Campaign c) {
        campaign = c;
        initComponents();
    }

    private void initComponents() {
        cbContract = new JComboBox<>();
        cbContract.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return new JLabel((null == value) ? "None" : ((AtBContract) value).getName());
            }
        });

        cbRole = new JComboBox<>(AtBLanceRole.values());
        cbRole.setName("cbRole");
        cbRole.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AtBLanceRole) {
                    list.setToolTipText(((AtBLanceRole) value).getToolTipText());
                }
                return this;
            }
        });

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
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(((RequiredLancesTableModel) table.getModel()).
                            getAlignment(table.convertColumnIndexToModel(column)));
                    if (table.convertColumnIndexToModel(column) > RequiredLancesTableModel.COL_CONTRACT) {
                        if (((String) value).indexOf('/') >= 0) {
                            setForeground(MekHQ.getMekHQOptions().getBelowContractMinimumForeground());
                        }
                    }
                    return this;
                }
            });
        }
        TableRowSorter<RequiredLancesTableModel>sorter = new TableRowSorter<>(rlModel);
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
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    switch (column) {
                        case LanceAssignmentTableModel.COL_FORCE:
                            if (null != value) {
                                setText((((Force) value)).getFullName());
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
        RowFilter<LanceAssignmentTableModel, Integer> laFilter;
        laFilter = new RowFilter<LanceAssignmentTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends LanceAssignmentTableModel, ? extends Integer> entry) {
                Lance l = entry.getModel().getRow(entry.getIdentifier());
                return l.isEligible(campaign);
            }
        };
        final NaturalOrderComparator noc = new NaturalOrderComparator();
        TableRowSorter<LanceAssignmentTableModel>laSorter = new TableRowSorter<>(laModel);
        laSorter.setRowFilter(laFilter);
        laSorter.setComparator(LanceAssignmentTableModel.COL_FORCE, forceComparator);
        laSorter.setComparator(LanceAssignmentTableModel.COL_CONTRACT, (c1, c2) ->
                noc.compare(((AtBContract) c1).getName(), ((AtBContract) c2).getName()));
        laSorter.setComparator(LanceAssignmentTableModel.COL_ROLE, (r1, r2) ->
                noc.compare(r1.toString(), r2.toString()));
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(LanceAssignmentTableModel.COL_FORCE, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        tblAssignments.setRowSorter(laSorter);

        tblAssignments.setIntercellSpacing(new Dimension(0, 0));
        tblAssignments.setShowGrid(false);

        panRequiredLances = new JPanel();
        panRequiredLances.setLayout(new BoxLayout(panRequiredLances, BoxLayout.Y_AXIS));
        panRequiredLances.setBorder(BorderFactory.createTitledBorder("Deployment Requirements"));
        panRequiredLances.add(tblRequiredLances.getTableHeader());
        panRequiredLances.add(tblRequiredLances);
        add(panRequiredLances);

        int cmdrStrategy = 0;
        if ((campaign.getFlaggedCommander() != null)
                && (campaign.getFlaggedCommander().getSkill(SkillType.S_STRATEGY) != null)) {
            cmdrStrategy = campaign.getFlaggedCommander().getSkill(SkillType.S_STRATEGY).getLevel();
        }
        int maxDeployedLances = campaign.getCampaignOptions().getBaseStrategyDeployment() +
                campaign.getCampaignOptions().getAdditionalStrategyDeployment() * cmdrStrategy;
        add(new JLabel("Maximum Deployed Units: " + maxDeployedLances));

        panAssignments = new JPanel();
        panAssignments.setLayout(new BoxLayout(panAssignments, BoxLayout.Y_AXIS));
        panAssignments.setBorder(BorderFactory.createTitledBorder("Current Assignments"));
        panAssignments.add(tblAssignments.getTableHeader());
        panAssignments.add(tblAssignments);
        add(panAssignments);

        refresh();
        tblAssignments.getModel().addTableModelListener(assignmentTableListener);
    }

    public void refresh() {
        cbContract.removeAllItems();
        List<AtBContract> activeContracts = campaign.getActiveAtBContracts();
        for (AtBContract contract : activeContracts) {
            cbContract.addItem(contract);
        }
        AtBContract defaultContract = activeContracts.isEmpty() ? null : activeContracts.get(0);
        for (Lance l : campaign.getLances().values()) {
            if ((l.getContract(campaign) == null)
                    || !l.getContract(campaign).isActiveOn(campaign.getLocalDate(), true)) {
                l.setContract(defaultContract);
            }
        }
        ((DataTableModel) tblRequiredLances.getModel()).setData(activeContracts);
        ((DataTableModel) tblAssignments.getModel()).setData(campaign.getLanceList());
        panRequiredLances.setVisible(tblRequiredLances.getRowCount() > 0);
    }

    TableModelListener assignmentTableListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent ev) {
            ((RequiredLancesTableModel) tblRequiredLances.getModel()).fireTableDataChanged();
        }
    };

    /**
     * Sorts Force objects according to where they appear on the TO&E
     */
    public Comparator<Force> forceComparator = (f1, f2) -> {
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

        /* Find closest common ancestor. They must be or descend from
         * different subforces of this one. */
        Force f = f1;
        while (!f.isAncestorOf(f2)) {
            f = f.getParentForce();
        }
        for (Force sf : f.getSubForces()) {
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

class RequiredLancesTableModel extends DataTableModel {
    private static final long serialVersionUID = -5007787884549927503L;

    public static final int COL_CONTRACT = 0;
    public static final int COL_TOTAL = 1;
    public static final int COL_FIGHT = 2;
    public static final int COL_DEFEND = 3;
    public static final int COL_SCOUT = 4;
    public static final int COL_TRAINING = 5;
    public static final int COL_NUM = 6;

    private Campaign campaign;

    public RequiredLancesTableModel(final Campaign campaign) {
        this.campaign = campaign;
        data = new ArrayList<AtBContract>();
        columnNames = new String[]{"Contract", "Total", "Fight", "Defend", "Scout", "Training"};
    }

    @Override
    public int getColumnCount() {
        return COL_NUM;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnWidth(int col) {
        if (col == COL_CONTRACT) {
            return 100;
        } else {
            return 20;
        }
    }


    public int getAlignment(int col) {
        if (col == COL_CONTRACT) {
            return SwingConstants.LEFT;
        } else {
            return SwingConstants.CENTER;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public AtBContract getRow(int row) {
        return (AtBContract) data.get(row);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (row >= getRowCount()) {
            return "";
        }
        if (COL_CONTRACT == column) {
            return ((AtBContract) data.get(row)).getName();
        }
        if (data.get(row) instanceof AtBContract) {
            AtBContract contract = (AtBContract) data.get(row);
            if (column == COL_TOTAL) {
                int t = 0;
                for (Lance l : campaign.getLanceList()) {
                    if (data.get(row).equals(l.getContract(campaign))
                            && (l.getRole() != AtBLanceRole.UNASSIGNED)
                            && l.isEligible(campaign)) {
                        t++;
                    }
                }
                if (t < contract.getRequiredLances()) {
                    return t + "/" + contract.getRequiredLances();
                }
                return Integer.toString(contract.getRequiredLances());
            } else if (contract.getContractType().getRequiredLanceRole().ordinal() == column - 2) {
                int t = 0;
                for (Lance l : campaign.getLanceList()) {
                    if (data.get(row).equals(l.getContract(campaign))
                            && (l.getRole() == l.getContract(campaign).getContractType().getRequiredLanceRole())
                            && l.isEligible(campaign)) {
                        t++;
                    }
                }
                int required = Math.max(contract.getRequiredLances() / 2, 1);
                if (t < required) {
                    return t + "/" + required;
                }
                return Integer.toString(required);
            }
        }
        return "";
    }
}

class LanceAssignmentTableModel extends DataTableModel {
    private static final long serialVersionUID = -2688617737510762878L;

    public static final int COL_FORCE = 0;
    public static final int COL_WEIGHT_CLASS = 1;
    public static final int COL_CONTRACT = 2;
    public static final int COL_ROLE = 3;
    public static final int COL_NUM = 4;

    private Campaign campaign;

    public LanceAssignmentTableModel(Campaign campaign) {
        this.campaign = campaign;
        data = new ArrayList<>();
        columnNames = new String[]{"Force", "Wt", "Mission", "Role"};
    }

    @Override
    public int getColumnCount() {
        return COL_NUM;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnWidth(int col) {
        switch (col) {
            case COL_FORCE:
            case COL_CONTRACT:
                    return 100;
            case COL_WEIGHT_CLASS:
                    return 5;
            default:
                    return 50;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        switch (c) {
            case COL_FORCE:
                return Force.class;
            case COL_CONTRACT:
                return AtBContract.class;
            case COL_ROLE:
                return AtBLanceRole.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col > COL_WEIGHT_CLASS;
    }

    public Lance getRow(int row) {
        return (Lance) data.get(row);
    }

    @Override
    public Object getValueAt(int row, int column) {
        final String[] WEIGHT_CODES = {"UL", "L", "M", "H", "A", "SH"};

        if (row >= getRowCount()) {
            return "";
        }
        switch (column) {
            case COL_FORCE:
                return campaign.getForce(((Lance) data.get(row)).getForceId());
            case COL_WEIGHT_CLASS:
                return WEIGHT_CODES[((Lance) data.get(row)).getWeightClass(campaign)];
            case COL_CONTRACT:
                return campaign.getMission(((Lance) data.get(row)).getMissionId());
            case COL_ROLE:
                return ((Lance) data.get(row)).getRole();
            default:
                return "?";
        }
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == COL_CONTRACT) {
            ((Lance) data.get(row)).setContract((AtBContract) value);
        } else if (col == COL_ROLE) {
            if (value instanceof AtBLanceRole) {
                ((Lance) data.get(row)).setRole((AtBLanceRole) value);
            }
        }
        fireTableDataChanged();
    }
}
