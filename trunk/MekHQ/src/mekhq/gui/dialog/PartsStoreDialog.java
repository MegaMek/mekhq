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
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.TargetRoll;
import megamek.common.WeaponType;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.FireControlSystem;
import mekhq.campaign.parts.LandingGear;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.VeeSensor;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;

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
	private static final int SG_COCKPIT  = 11;
	private static final int SG_NUM      = 12;
	
    private Frame frame;
    private Campaign campaign;
    private CampaignGUI campaignGUI;
    private DecimalFormat formatter;
    private PartsTableModel partsModel;
	private TableRowSorter<PartsTableModel> partsSorter;
    
    private JTable partsTable;
    private JScrollPane scrollPartsTable;
    private JPanel panFilter;
    private JLabel lblFilter;
    private javax.swing.JTextField txtFilter;
    private JComboBox choiceParts;
	private JLabel lblPartsChoice;
    private JPanel panButtons;
    private JButton btnAdd;
    private JButton btnBuyBulk;
    private JButton btnBuy;
    private JButton btnClose;
    
    /** Creates new form NewTeamDialog */
    public PartsStoreDialog(boolean modal, CampaignGUI gui) {
        super(gui.getFrame(), modal);
        this.frame = gui.getFrame();  
        this.campaignGUI = gui;
        campaign = campaignGUI.getCampaign();
        formatter = new DecimalFormat();
        partsModel = new PartsTableModel(campaign.getPartsStore().getInventory());
        initComponents();
        filterParts();
        setLocationRelativeTo(frame);
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
		partsSorter.setComparator(PartsTableModel.COL_TARGET, new TargetSorter());
        partsSorter.setComparator(PartsTableModel.COL_COST, campaignGUI.new FormattedNumberSorter());
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
		c.insets = new Insets(5,5,5,5);
		panFilter.add(lblPartsChoice, c);
		c.gridx = 1;
		c.weightx = 1.0;
		panFilter.add(choiceParts, c);
		
		lblFilter = new JLabel(resourceMap.getString("lblFilter.text")); // NOI18N
        lblFilter.setName("lblFilter"); // NOI18N
        c.gridx = 0;
        c.gridy = 1;
		c.weightx = 0.0;
		panFilter.add(lblFilter, c);
        txtFilter = new javax.swing.JTextField();
        txtFilter.setText(""); // NOI18N
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setName("txtFilter"); // NOI18N
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(
            new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    filterParts();
                }
                public void insertUpdate(DocumentEvent e) {
                    filterParts();
                }
                public void removeUpdate(DocumentEvent e) {
                    filterParts();
                }
            });
        c.gridx = 1;
        c.gridy = 1;
		c.weightx = 1.0;
		panFilter.add(txtFilter, c);
		getContentPane().add(panFilter, BorderLayout.PAGE_START);

		panButtons = new JPanel();
		btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
		btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPart(false, false);
            }
        });
		btnAdd.setEnabled(campaignGUI.getCampaign().isGM());
		btnBuyBulk = new JButton(resourceMap.getString("btnBuyBulk.text"));
		btnBuyBulk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	addPart(true, true);
            	partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(partsTable.getSelectedRow()), PartsTableModel.COL_QUEUE);
            }
        });
		btnBuy = new JButton(resourceMap.getString("btnBuy.text"));
		btnBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPart(true, false);
                partsModel.fireTableCellUpdated(partsTable.convertRowIndexToModel(partsTable.getSelectedRow()), PartsTableModel.COL_QUEUE);
            }
        });
		btnClose = new JButton(resourceMap.getString("btnClose.text"));
		btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
		panButtons.setLayout(new GridBagLayout());
		panButtons.add(btnBuyBulk, new GridBagConstraints());
		panButtons.add(btnBuy, new GridBagConstraints());
		panButtons.add(btnAdd, new GridBagConstraints());
		panButtons.add(btnClose, new GridBagConstraints());
		getContentPane().add(panButtons, BorderLayout.PAGE_END);		
		this.setPreferredSize(new Dimension(650,600));
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
        		if(txtFilter.getText().length() > 0 && !part.getName().toLowerCase().contains(txtFilter.getText().toLowerCase())) {
                    return false;
                }
    			if(part.getTechBase() == Part.T_CLAN && !campaign.getCampaignOptions().allowClanPurchases()) {
    				return false;
    			}
    			if(part.getTechBase() == Part.T_IS && !campaign.getCampaignOptions().allowISPurchases()) {
    				return false;
    			}
    			if(campaign.getCampaignOptions().getTechLevel() < Utilities.getSimpleTechLevel(part.getTechLevel())) {
    				return false;
    			}
    			//TODO: limit by year
        		if(nGroup == SG_ALL) {
        			return true;
        		} else if(nGroup == SG_ARMOR) {
        			return part instanceof Armor;
        		} else if(nGroup == SG_SYSTEM) {
        			return part instanceof MekLifeSupport
        				|| part instanceof MekSensor
        				|| part instanceof LandingGear
        				|| part instanceof Avionics
        				|| part instanceof FireControlSystem
        				|| part instanceof AeroSensor
        				|| part instanceof VeeSensor
        				|| part instanceof VeeStabiliser;
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
        		} else if(nGroup == SG_COCKPIT) {
        			return part instanceof MekCockpit;
        		} 
        		return false;
        	}
        };
        partsSorter.setRowFilter(partsTypeFilter);
    }

    
    private void addPart(boolean purchase, boolean bulk) {
    	int row = partsTable.getSelectedRow();
		if(row < 0) {
			return;
		}
		Part selectedPart = partsModel.getPartAt(partsTable.convertRowIndexToModel(row));
		int quantity = 1;
		if(bulk) {
			PopupValueChoiceDialog pcd = new PopupValueChoiceDialog(campaignGUI.getFrame(), true, "How Many " + selectedPart.getName(), quantity, 1, 100);
			pcd.setVisible(true);
			quantity = pcd.getValue();
		}
		
		if(purchase) {
		    campaign.getShoppingList().addShoppingItem(selectedPart.clone(), quantity);
		} else {
		while(quantity > 0) {
		        campaign.addPart(selectedPart.clone());
		        quantity--;
		    }
		}
		campaignGUI.refreshReport();
		campaignGUI.refreshAcquireList();
		campaignGUI.refreshPartsList();
		campaignGUI.refreshFinancialTransactions();
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
    	case SG_COCKPIT:
    		return "Cockpits";
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
		protected ArrayList<Part> data;

		public final static int COL_NAME    =    0;
		public final static int COL_DETAIL   =   1;
		public final static int COL_TECH_BASE  = 2;
		public final static int COL_COST     =   3;
		public final static int COL_TON       =  4;
	    public final static int COL_TARGET    =  5;
	    public final static int COL_QUEUE    =  6;
        public final static int N_COL          = 7;
		
		public PartsTableModel(ArrayList<Part> inventory) {
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
            	case COL_DETAIL:
                    return "Detail";
                case COL_COST:
                    return "Cost";
                case COL_TON:
                    return "Ton";
                case COL_TECH_BASE:
                    return "Tech";
                case COL_TARGET:
                    return "Target";
                case COL_QUEUE:
                    return "# Queue";
                default:
                    return "?";
            }
        }

		public Object getValueAt(int row, int col) {
	        Part part;
	        if(data.isEmpty()) {
	        	return "";
	        } else {
	        	part = (Part)data.get(row);
	        }
			if(col == COL_NAME) {
				return part.getName();
			}
			if(col == COL_DETAIL) {
				return part.getDetails();
			}
			if(col == COL_COST) {
				return formatter.format(part.getActualValue());
			}
			if(col == COL_TON) {
				return Math.round(part.getTonnage() * 100) / 100.0;
			}
			if(col == COL_TECH_BASE) {
				return part.getTechBaseName();
			}
			if(col == COL_TARGET) {
			    IAcquisitionWork shoppingItem = (MissingPart)part.getMissingPart();
		        if(null == shoppingItem && part instanceof IAcquisitionWork) {
		            shoppingItem = (IAcquisitionWork)part;
		        }
	            if(null != shoppingItem) {
	                TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, campaign.getLogisticsPerson());
	                String value = target.getValueAsString();
	                if(target.getValue() != TargetRoll.IMPOSSIBLE && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
	                    value += "+";
	                }
	                return value;
	            } else {
	                return "-";
	            }
			}
			if(col == COL_QUEUE) {
			    IAcquisitionWork acquire = campaign.getShoppingList().getShoppingItem(part);
			    if(null != acquire) {
			        return acquire.getQuantity();
			    } else {
			        return 0;
			    }
			}
			return "?";
		}
		
		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}
		
		@Override
		public Class<? extends Object> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		public Part getPartAt(int row) {
			return ((Part) data.get(row));
		}

		public Part[] getPartstAt(int[] rows) {
			Part[] parts = new Part[rows.length];
			for (int i = 0; i < rows.length; i++) {
				int row = rows[i];
				parts[i] = ((Part) data.get(row));
			}
			return parts;
		}
		
		 public int getColumnWidth(int c) {
	            switch(c) {
	            case COL_NAME:
	            case COL_DETAIL:
	        		return 120;
	            case COL_COST:
	            case COL_TARGET:
	                return 40;        
	            default:
	                return 15;
	            }
	        }
	        
	        public int getAlignment(int col) {
	            switch(col) {
	            case COL_COST:
	            case COL_TON:
	            	return SwingConstants.RIGHT;
	            case COL_TARGET:
	                return SwingConstants.CENTER;
	            default:
	            	return SwingConstants.LEFT;
	            }
	        }

	        public String getTooltip(int row, int col) {
	            Part part;
	            if(data.isEmpty()) {
	                return null;
	            } else {
	                part = (Part)data.get(row);
	            }
	        	switch(col) {
	        	case COL_TARGET:
	        	    IAcquisitionWork shoppingItem = (MissingPart)part.getMissingPart();
	                if(null == shoppingItem && part instanceof IAcquisitionWork) {
	                    shoppingItem = (IAcquisitionWork)part;
	                }
	                if(null != shoppingItem) {
	                    TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, campaign.getLogisticsPerson());
	                    return target.getDesc();
	                } else {
	                    return null;
	                }
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
	
	/**
     * A comparator for target numbers written as strings
     * @author Jay Lawson
     *
     */
    public class TargetSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            s0 = s0.replaceAll("\\+", "");
            s1 = s1.replaceAll("\\+", "");
            int r0 = 0;
            int r1 = 0;
            if(s0.equals("Impossible")) {
                r0 = Integer.MAX_VALUE;
            }
            else if(s0.equals("Automatic Failure")) {
                r0 = Integer.MAX_VALUE-1;
            }
            else if(s0.equals("Automatic Success")) {
                r0 = Integer.MIN_VALUE;
            } else {
                r0 = Integer.parseInt(s0);
            }   
            if(s1.equals("Impossible")) {
                r1 = Integer.MAX_VALUE;
            }
            else if(s1.equals("Automatic Failure")) {
                r1 = Integer.MAX_VALUE-1;
            }
            else if(s1.equals("Automatic Success")) {
                r1 = Integer.MIN_VALUE;
            } else {
                r1 = Integer.parseInt(s1);
            }   
            return ((Comparable<Integer>)r0).compareTo(r1);

        }
    }
}
