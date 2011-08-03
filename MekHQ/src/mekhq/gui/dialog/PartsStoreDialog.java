/*
 * PartsStoreDialog.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import mekhq.campaign.Campaign;
import mekhq.campaign.PartInventory;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;

/**
 *
 * @author  Taharqa
 */
public class PartsStoreDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
	
	//parts filter groups
	private static final int SG_ALL      = 0;
	private static final int SG_ARMOR    = 1;
	private static final int SG_SYSTEM   = 2;
	private static final int SG_EQUIP    = 3;
	private static final int SG_LOC      = 4;
	private static final int SG_WEAP     = 5;
	private static final int SG_AMMO     = 6;
	private static final int SG_MISC     = 7;
	private static final int SG_ENGINE   = 8;
	private static final int SG_GYRO     = 9;
	private static final int SG_ACT      = 10;
	private static final int SG_NUM      = 11;
	
    private Frame frame;
    private Campaign campaign;
    private DecimalFormat formatter;
    private PartsTableModel partsModel;
	private TableRowSorter<PartsTableModel> partsSorter;
    
    private JTable partsTable;
    private JScrollPane scrollPartsTable;
    private JPanel panFilter;
    private JComboBox choiceParts;
	private JLabel lblPartsChoice;
    private JPanel panButtons;
    private JButton btnBuy;
    private JButton btnClose;
    
    /** Creates new form NewTeamDialog */
    public PartsStoreDialog(java.awt.Frame parent, boolean modal, Campaign c) {
        super(parent, modal);
        this.frame = parent;  
        campaign = c;
        formatter = new DecimalFormat();
        partsModel = new PartsTableModel(campaign.getPartsStore().getInventory());
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
    	
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsStoreDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        
        getContentPane().setLayout(new BorderLayout());
      
        partsTable = new JTable(partsModel);
		partsTable.setName("partsTable"); // NOI18N
		partsSorter = new TableRowSorter<PartsTableModel>(partsModel);
        partsTable.setRowSorter(partsSorter);
		TableColumn column = null;
        for (int i = 0; i < PartsTableModel.N_COL; i++) {
            column = partsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(partsModel.getColumnWidth(i));
            column.setCellRenderer(partsModel.getRenderer());
        }
        partsTable.setIntercellSpacing(new Dimension(0, 0));
		partsTable.setShowGrid(false);
		scrollPartsTable = new JScrollPane();
		scrollPartsTable.setName("scrollPartsTable"); // NOI18N
		scrollPartsTable.setViewportView(partsTable);
		getContentPane().add(scrollPartsTable, BorderLayout.CENTER);

		GridBagConstraints c = new GridBagConstraints();
		panFilter = new JPanel();
		lblPartsChoice = new JLabel(resourceMap.getString("lblPartsChoice.text")); // NOI18N
		DefaultComboBoxModel partsGroupModel = new DefaultComboBoxModel();
		for (int i = 0; i < SG_NUM; i++) {
			partsGroupModel.addElement(getPartsGroupName(i));
		}
		choiceParts = new JComboBox(partsGroupModel);
		choiceParts.setName("choiceParts"); // NOI18N
		choiceParts.setSelectedIndex(0);
		choiceParts.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				filterParts();
			}
		});
		panFilter.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.0;
		c.anchor = java.awt.GridBagConstraints.WEST;
		panFilter.add(lblPartsChoice, c);
		c.gridx = 1;
		c.weightx = 1.0;
		panFilter.add(choiceParts, c);
		getContentPane().add(panFilter, BorderLayout.PAGE_START);
		
		panButtons = new JPanel();
		btnBuy = new JButton(resourceMap.getString("btnBuy.text"));
		btnBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buyPart();
            }
        });
		btnClose = new JButton(resourceMap.getString("btnClose.text"));
		btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
		panButtons.setLayout(new GridBagLayout());
		panButtons.add(btnBuy, new GridBagConstraints());
		panButtons.add(btnClose, new GridBagConstraints());
		getContentPane().add(panButtons, BorderLayout.PAGE_END);
        pack();
    }
    
    public void filterParts() {
        RowFilter<PartsTableModel, Integer> partsTypeFilter = null;
        final int nGroup = choiceParts.getSelectedIndex();
        partsTypeFilter = new RowFilter<PartsTableModel,Integer>() {
        	@Override
        	public boolean include(Entry<? extends PartsTableModel, ? extends Integer> entry) {
        		PartsTableModel partsModel = entry.getModel();
        		Part part = partsModel.getPartAt(entry.getIdentifier());
        		if(nGroup == SG_ALL) {
        			return true;
        		} else if(nGroup == SG_ARMOR) {
        			return part instanceof Armor;
        		} else if(nGroup == SG_SYSTEM) {
        			return part instanceof MekGyro 
        				|| part instanceof EnginePart
        				|| part instanceof MekActuator
        				|| part instanceof MekLifeSupport
        				|| part instanceof MekSensor;
        		} else if(nGroup == SG_EQUIP) {
        			return part instanceof EquipmentPart;
        		} else if(nGroup == SG_LOC) {
        			return part instanceof MekLocation || part instanceof TankLocation;
        		} else if(nGroup == SG_WEAP) {
        			return part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof WeaponType;
        		} else if(nGroup == SG_AMMO) {
        			return part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof AmmoType;
        		} else if(nGroup == SG_MISC) {
        			return part instanceof EquipmentPart && ((EquipmentPart)part).getType() instanceof MiscType;
        		} else if(nGroup == SG_ENGINE) {
        			return part instanceof EnginePart;
        		} else if(nGroup == SG_GYRO) {
        			return part instanceof MekGyro;
        		} else if(nGroup == SG_ACT) {
        			return part instanceof MekActuator;
        		} 
        		return false;
        	}
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    
    private void buyPart() {
    	//IMPLEMENT ME
    }
    
    public static String getPartsGroupName(int group) {
    	switch(group) {
    	case SG_ALL:
    		return "All Parts";
    	case SG_ARMOR:
    		return "Armor";
    	case SG_SYSTEM:
    		return "System Components";
    	case SG_EQUIP:
    		return "Equipment";
    	case SG_LOC:
    		return "Locations";
    	case SG_WEAP:
    		return "Weapons";
    	case SG_AMMO:
    		return "Ammunition";
    	case SG_MISC:
    		return "Miscellaneous Equipment";
    	case SG_ENGINE:
    		return "Engines";
    	case SG_GYRO:
    		return "Gyros";
    	case SG_ACT:
    		return "Actuators";
    	default:
    		return "?";
    	}
    }
    
    /**
	 * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
	 */
	public class PartsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 534443424190075264L;

		protected String[] columnNames;
		protected ArrayList<PartInventory> data;

		public final static int COL_NAME    =    0;
		public final static int COL_TECH_BASE  = 1;
		public final static int COL_COST     =   2;
		public final static int COL_TON       =  3;
        public final static int N_COL          = 4;
		
		public PartsTableModel(ArrayList<PartInventory> inventory) {
			data = inventory;
		}
		
		public int getRowCount() {
            return data.size();
        }

        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            	case COL_NAME:
            		return "Name";
                case COL_COST:
                    return "Cost";
                case COL_TON:
                    return "Tonnage";
                case COL_TECH_BASE:
                    return "Tech Base";
                default:
                    return "?";
            }
        }

		public Object getValueAt(int row, int col) {
	        PartInventory partInventory;
	        if(data.isEmpty()) {
	        	return "";
	        } else {
	        	partInventory = (PartInventory)data.get(row);
	        }
			Part part = partInventory.getPart();
			if(col == COL_NAME) {
				return part.getName();
			}
			if(col == COL_COST) {
				return formatter.format(part.getCurrentValue());
			}
			if(col == COL_TON) {
				return Math.round(part.getTonnage() * 100) / 100.0;
			}
			if(col == COL_TECH_BASE) {
				return part.getTechBaseName();
			}
			return "?";
		}
		
		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		public Part getPartAt(int row) {
			return ((PartInventory) data.get(row)).getPart();
		}

		public Part[] getPartstAt(int[] rows) {
			Part[] parts = new Part[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				parts[i] = ((PartInventory) data.get(row)).getPart();
			}
			return parts;
		}
		
		 public int getColumnWidth(int c) {
	            switch(c) {
	            case COL_NAME:
	        		return 100;
	            case COL_TECH_BASE:
	                return 40;        
	            default:
	                return 10;
	            }
	        }
	        
	        public int getAlignment(int col) {
	            switch(col) {
	            case COL_COST:
	            case COL_TON:
	            	return SwingConstants.RIGHT;
	            default:
	            	return SwingConstants.LEFT;
	            }
	        }

	        public String getTooltip(int row, int col) {
	        	switch(col) {
	            default:
	            	return null;
	            }
	        }
	        public PartsTableModel.Renderer getRenderer() {
				return new PartsTableModel.Renderer();
			}

			public class Renderer extends DefaultTableCellRenderer {

				private static final long serialVersionUID = 9054581142945717303L;

				public Component getTableCellRendererComponent(JTable table,
						Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
					super.getTableCellRendererComponent(table, value, isSelected,
							hasFocus, row, column);
					setOpaque(true);
					int actualCol = table.convertColumnIndexToModel(column);
					int actualRow = table.convertRowIndexToModel(row);
					setHorizontalAlignment(getAlignment(actualCol));
					setToolTipText(getTooltip(actualRow, actualCol));
					
					return this;
				}

			}
	}
}
