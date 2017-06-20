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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.model.DataTableModel;
import mekhq.gui.model.UnitMarketTableModel;
import mekhq.gui.model.XTableColumnModel;

/**
 * Against the Bot
 * Shows how many lances are required to be deployed on active contracts and
 * in what roles and allows the player to assign units to those roles. 
 * 
 * @author Neoancient
 *
 */
public class LanceAssignmentView extends JPanel {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7280552346074838142L;
	public static final int ROLE_NONE = 0;
	public static final int ROLE_FIGHT = 1;
	public static final int ROLE_DEFEND = 2;
	public static final int ROLE_SCOUT = 3;
	public static final int ROLE_TRAINING = 4;
	public static final int ROLE_NUM = 5;
	
	private Campaign campaign;
	
	private JTable tblRequiredLances;
	private JTable tblAssignments;
	private JPanel panRequiredLances;
	private JPanel panAssignments;
	private JComboBox<AtBContract> cbContract;
	private JComboBox<String> cbRole;

	public LanceAssignmentView(Campaign c) {
		campaign = c;
		
		initComponents();
	}
	
	@SuppressWarnings("serial")
	private void initComponents() {
		cbContract = new JComboBox<AtBContract>();
		cbContract.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				return new JLabel((null == value)?"None":((AtBContract)value).getName());
			}
		});
		
		cbRole = new JComboBox<String>(Lance.roleNames);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		RequiredLancesTableModel rlModel = new RequiredLancesTableModel(campaign); 
		tblRequiredLances = new JTable(rlModel);
        tblRequiredLances.setColumnModel(new XTableColumnModel());
        tblRequiredLances.createDefaultColumnsFromModel();
        tblRequiredLances.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableColumn column = null;
        for (int i = 0; i < UnitMarketTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel)tblRequiredLances.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(rlModel.getColumnWidth(i));
            column.setCellRenderer(new DefaultTableCellRenderer() {
				public Component getTableCellRendererComponent(JTable table,
                        Object value, boolean isSelected, boolean hasFocus,
                        int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected,
                            hasFocus, row, column);
                    setHorizontalAlignment(((RequiredLancesTableModel)table.getModel()).
                    		getAlignment(table.convertColumnIndexToModel(column)));
                    if (table.convertColumnIndexToModel(column) > RequiredLancesTableModel.COL_CONTRACT) {
                    	if (((String)value).indexOf('/') >= 0) {
                    		setForeground(Color.RED);
                    	} else {
                    		setForeground(Color.BLACK);
                    	}
                    }
                    return this;
                }
            });
        }
        TableRowSorter<RequiredLancesTableModel>sorter = new TableRowSorter<RequiredLancesTableModel>(rlModel);
        tblRequiredLances.setRowSorter(sorter);
        
        tblRequiredLances.setIntercellSpacing(new Dimension(0, 0));
        tblRequiredLances.setShowGrid(false);
        
		LanceAssignmentTableModel laModel = new LanceAssignmentTableModel(campaign); 
		tblAssignments = new JTable(laModel);
		tblAssignments.setColumnModel(new XTableColumnModel());
		tblAssignments.createDefaultColumnsFromModel();
		tblAssignments.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        column = null;
        for (int i = 0; i < LanceAssignmentTableModel.COL_NUM; i++) {
            column = ((XTableColumnModel)tblAssignments.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(rlModel.getColumnWidth(i));
            column.setCellRenderer(new DefaultTableCellRenderer() {
				public Component getTableCellRendererComponent(JTable table,
                        Object value, boolean isSelected, boolean hasFocus,
                        int row, int column) {
                    switch (column) {
                    case LanceAssignmentTableModel.COL_FORCE:
                    	setText((((Force)value)).getFullName());
                    	break;
                    case LanceAssignmentTableModel.COL_CONTRACT:
                    	if (null == value) {
                    		setText("None");
                    	} else {
                    		setText(((AtBContract)value).getName());
                    	}
                    	break;
                    default:
                        super.getTableCellRendererComponent(table, value, isSelected,
                                hasFocus, row, column);
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
    	RowFilter<LanceAssignmentTableModel, Integer> laFilter = null;
    	laFilter = new RowFilter<LanceAssignmentTableModel, Integer>() {
    		@Override
    		public boolean include(Entry<? extends LanceAssignmentTableModel, ? extends Integer> entry) {
    			Lance l = entry.getModel().getRow(entry.getIdentifier());
    			return l.isEligible(campaign);
    		}
    	};
        TableRowSorter<LanceAssignmentTableModel>laSorter = new TableRowSorter<LanceAssignmentTableModel>(laModel);
        laSorter.setRowFilter(laFilter);
        laSorter.setComparator(LanceAssignmentTableModel.COL_FORCE, forceComparator);
        ArrayList<RowSorter.SortKey>sortKeys = new ArrayList<RowSorter.SortKey>();
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
        if (campaign.getFlaggedCommander() != null &&
        		campaign.getFlaggedCommander().getSkill(SkillType.S_STRATEGY) != null) {
        	cmdrStrategy = campaign.getFlaggedCommander().
        			getSkill(SkillType.S_STRATEGY).getLevel();
        }
        int maxDeployedLances = 
        		campaign.getCampaignOptions().getBaseStrategyDeployment() +
        		campaign.getCampaignOptions().getAdditionalStrategyDeployment() *
        		cmdrStrategy;
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
		ArrayList<AtBContract> activeContracts = new ArrayList<AtBContract>();
		for (Mission m : campaign.getMissions()) {
			if (m instanceof AtBContract && m.isActive() &&
					!((AtBContract)m).getStartDate().after(campaign.getDate())) {
				activeContracts.add((AtBContract)m);
				cbContract.addItem((AtBContract)m);
			}
		}
		AtBContract defaultContract = null;
		if (activeContracts.size() > 0) {
			defaultContract = activeContracts.get(0);
		}
		for (Lance l : campaign.getLances().values()) {
			if (null == l.getContract(campaign) || !l.getContract(campaign).isActive()) {
				l.setContract(defaultContract);
			}
		}
		((DataTableModel)tblRequiredLances.getModel()).setData(activeContracts);
		((DataTableModel)tblAssignments.getModel()).setData(campaign.getLanceList());
		panRequiredLances.setVisible(tblRequiredLances.getRowCount() > 0);
	}
	
	TableModelListener assignmentTableListener = new TableModelListener() {
		public void tableChanged(TableModelEvent ev) {
			((RequiredLancesTableModel)tblRequiredLances.getModel()).fireTableDataChanged();
		}
	};
	
	/** 
	 * Sorts Force objects according to where they appear on the TO&E
	 */
	
	public Comparator<Force> forceComparator = new Comparator<Force>() {
		@Override
		public int compare(Force f1, Force f2) {

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
		}
	};
	
}

class RequiredLancesTableModel extends DataTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5007787884549927503L;
	
	public static final int COL_CONTRACT = 0;
	public static final int COL_TOTAL = 1;
	public static final int COL_FIGHT = 2;
	public static final int COL_DEFEND = 3;
	public static final int COL_SCOUT = 4;
	public static final int COL_TRAINING = 5;
	public static final int COL_NUM = 6;

    protected String[] columnNames = {"Contract", "Total", "Fight", "Defend", "Scout", "Training"};
    
    private Campaign campaign;
    
    public RequiredLancesTableModel(Campaign campaign) {
    	this.campaign = campaign;
    	data = new ArrayList<AtBContract>();
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
			return ((AtBContract)data.get(row)).getName();
		}
		if (data.get(row) instanceof AtBContract) {
			AtBContract contract = (AtBContract)data.get(row);
			if (column == COL_TOTAL) {
				int t = 0;
				for (Lance l : campaign.getLanceList()) {
					if (l.getContract(campaign) == data.get(row)
							&& l.getRole() > Lance.ROLE_UNASSIGNED
							&& l.isEligible(campaign)) {
						t++;
					}
				}
				if (t < contract.getRequiredLances()) {
					return t + "/" + contract.getRequiredLances();
				}
				return Integer.toString(contract.getRequiredLances());
			} else if (contract.getRequiredLanceType() == column - 1) {
				int t = 0;
				for (Lance l : campaign.getLanceList()) {
					if (l.getContract(campaign) == data.get(row)
							&& l.getRole() == l.getContract(campaign).getRequiredLanceType()
							&& l.isEligible(campaign)) {
						t++;
					}
				}
				// we want to retain the behavior where we round up "less than one" required lance to one
				// but 0 lances should be 0.
				int required = (int) Math.ceil(contract.getRequiredLances() / 2);
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
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2688617737510762878L;
	
	public static final int COL_FORCE = 0;
	public static final int COL_WEIGHT_CLASS = 1;
	public static final int COL_CONTRACT = 2;
	public static final int COL_ROLE = 3;
	public static final int COL_NUM = 4;

    protected String[] columnNames = {"Force", "Wt", "Mission", "Role"};
    private Campaign campaign;
    
    public LanceAssignmentTableModel(Campaign campaign) {
    	this.campaign = campaign;
    	data = new ArrayList<Lance>();
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
    		return String.class;
    	}
        return String.class;
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
			return campaign.getForce(((Lance)data.get(row)).getForceId());
		case COL_WEIGHT_CLASS:
			return WEIGHT_CODES[((Lance)data.get(row)).getWeightClass(campaign)];
		case COL_CONTRACT:
			return (AtBContract)campaign.getMission(((Lance)data.get(row)).getMissionId());
		case COL_ROLE:
			return Lance.roleNames[((Lance)data.get(row)).getRole()];
		default:
			return "?";
		}
	}

	public void setValueAt(Object value, int row, int col) {
		if (col == COL_CONTRACT) {
			((Lance)data.get(row)).setContract((AtBContract)value);
		}
		if (col == COL_ROLE) {
			for (int i = 0; i < Lance.ROLE_NUM; i++) {
				if (((String)value).equals(Lance.roleNames[i])) {
					((Lance)data.get(row)).setRole(i);
				}
			}
		}
		fireTableDataChanged();
	}
}
