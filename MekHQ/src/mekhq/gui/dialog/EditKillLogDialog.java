/*
 * EditKillLogDialog.java
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
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.common.util.EncodeControl;
import mekhq.MekHQOptions;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Taharqa
 */
public class EditKillLogDialog extends javax.swing.JDialog {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6995319032267472795L;
	
	private Frame frame;
    private Campaign campaign;
    private Person person;
    private ArrayList<Kill> kills;
    private KillTableModel killModel;
    
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnOK;
    private JTable killTable;
    private JScrollPane scrollKillTable;
    
    public EditKillLogDialog(java.awt.Frame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        this.frame = parent;
        campaign = c;
        person = p;
        kills = c.getKillsFor(p.getId());
        killModel = new KillTableModel(kills);
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelLogDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title") + " " + person.getName());
        getContentPane().setLayout(new java.awt.BorderLayout());
        
        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAdd.setText(resourceMap.getString("btnAdd.text")); // NOI18N
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addKill();
            }
        });
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEdit.text")); // NOI18N
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editKill();
            }
        });
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnDelete.text")); // NOI18N
        btnDelete.setName("btnDelete"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteKill();
            }
        });
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);
        
        killTable = new JTable(killModel);
        killTable.setName("killTable"); // NOI18N
		TableColumn column = null;
        for (int i = 0; i < KillTableModel.N_COL; i++) {
            column = killTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(killModel.getColumnWidth(i));
            column.setCellRenderer(killModel.getRenderer());
        }
        killTable.setIntercellSpacing(new Dimension(0, 0));
		killTable.setShowGrid(false);
		killTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		killTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						killTableValueChanged(evt);
					}
				});
		scrollKillTable = new JScrollPane();
		scrollKillTable.setName("scrollPartsTable"); // NOI18N
		scrollKillTable.setViewportView(killTable);
        getContentPane().add(scrollKillTable, BorderLayout.CENTER);

        
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        getContentPane().add(btnOK, BorderLayout.PAGE_END);

        pack();
    }

    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
    	this.setVisible(false);
    }
    
    private void killTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int row = killTable.getSelectedRow();
		btnDelete.setEnabled(row != -1);
		btnEdit.setEnabled(row != -1);
	}
    
    private void addKill() {
    	KillDialog ekld = new KillDialog(frame, true, new Kill(person.getId(), "", "", campaign.getDate()), person.getName());
		ekld.setVisible(true);
		if(null != ekld.getKill()) {
			campaign.addKill(ekld.getKill());
		}
		refreshTable();
    }
    
    private void editKill() {
    	Kill kill = killModel.getKillAt(killTable.getSelectedRow());
    	if(null != kill) {
    		KillDialog ekld = new KillDialog(frame, true, kill, person.getName());
    		ekld.setVisible(true);
    		refreshTable();
    	}
    }
    
    private void deleteKill() {
    	Kill kill = killModel.getKillAt(killTable.getSelectedRow());
    	campaign.removeKill(kill);
    	refreshTable();
    }
    
    private void refreshTable() {
		int selectedRow = killTable.getSelectedRow();
    	killModel.setData(campaign.getKillsFor(person.getId()));
    	if(selectedRow != -1) {
    		if(killTable.getRowCount() > 0) {
    			if(killTable.getRowCount() == selectedRow) {
    				killTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
    			} else {
    				killTable.setRowSelectionInterval(selectedRow, selectedRow);
    			}
    		}
    	}
    }
    
    /**
	 * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
	 */
	public class KillTableModel extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = -58915479895694545L;
		protected String[] columnNames;
		protected ArrayList<Kill> data;

		public final static int COL_DATE    = 0;
		public final static int COL_KILLED  = 1;
		public final static int COL_KILLER  = 2;
        public final static int N_COL       = 3;
		
		public KillTableModel(ArrayList<Kill> entries) {
			data = entries;
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
            	case COL_DATE:
            		return "Date";
            	case COL_KILLED:
                    return "Kill";
            	case COL_KILLER:
                    return "With";
                default:
                    return "?";
            }
        }

		public Object getValueAt(int row, int col) {
	        Kill kill;
	        if(data.isEmpty()) {
	        	return "";
	        } else {
	        	kill = (Kill)data.get(row);
	        }
			if(col == COL_DATE) {
				return MekHQOptions.getInstance().getDateFormatShort().format(kill.getDate());
			}
			if(col == COL_KILLED) {
				return kill.getWhatKilled();
			}
			if(col == COL_KILLER) {
				return kill.getKilledByWhat();
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

		public Kill getKillAt(int row) {
			return (Kill) data.get(row);
		}
		
		 public int getColumnWidth(int c) {
			 switch(c) {
			 case COL_DATE:
				 return 20;     
			 default:
				 return 100;
			 }
		 }
	        
		 public int getAlignment(int col) {
			 return SwingConstants.LEFT;
		 }

		 public String getTooltip(int row, int col) {
			 switch(col) {
			 default:
				 return null;
			 }
		 }
	        
		 //fill table with values
		 public void setData(ArrayList<Kill> kills) {
			 data = kills;
			 fireTableDataChanged();
		 }
	        
		 public KillTableModel.Renderer getRenderer() {
			 return new KillTableModel.Renderer();
		 }

		 public class Renderer extends DefaultTableCellRenderer {
			 

			 /**
			 * 
			 */
			private static final long serialVersionUID = -2888173457152182907L;

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
