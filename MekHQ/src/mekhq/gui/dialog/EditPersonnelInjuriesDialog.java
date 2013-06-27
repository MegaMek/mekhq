/*
 * EditPersonnelLogDialog.java
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
import java.util.Date;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import mekhq.campaign.LogEntry;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Taharqa
 */
public class EditPersonnelInjuriesDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    private Campaign campaign;
    private Date date;
    private Person person;
    private ArrayList<LogEntry> log;
    private LogTableModel logModel;
    
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnOK;
    private JTable logTable;
    private JScrollPane scrollLogTable;
    
    /** Creates new form EditPersonnelLogDialog */
    public EditPersonnelInjuriesDialog(java.awt.Frame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        this.frame = parent;
        campaign = c;
        person = p;
        log = p.getPersonnelLog();
        logModel = new LogTableModel(log);
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelLogDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title") + " " + person.getName());
        getContentPane().setLayout(new java.awt.BorderLayout());
        
        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAdd.setText(resourceMap.getString("btnAdd.text")); // NOI18N
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEntry();
            }
        });
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEdit.text")); // NOI18N
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editEntry();
            }
        });
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnDelete.text")); // NOI18N
        btnDelete.setName("btnDelete"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEntry();
            }
        });
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);
        
        logTable = new JTable(logModel);
        logTable.setName("logTable"); // NOI18N
		TableColumn column = null;
        for (int i = 0; i < LogTableModel.N_COL; i++) {
            column = logTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(logModel.getColumnWidth(i));
            column.setCellRenderer(logModel.getRenderer());
        }
        logTable.setIntercellSpacing(new Dimension(0, 0));
		logTable.setShowGrid(false);
		logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		logTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						logTableValueChanged(evt);
					}
				});
		scrollLogTable = new JScrollPane();
		scrollLogTable.setName("scrollPartsTable"); // NOI18N
		scrollLogTable.setViewportView(logTable);
        getContentPane().add(scrollLogTable, BorderLayout.CENTER);

        
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
    
    private void logTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int row = logTable.getSelectedRow();
		btnDelete.setEnabled(row != -1);
		btnEdit.setEnabled(row != -1);
	}
    
    private void addEntry() {
    	EditLogEntryDialog eeld = new EditLogEntryDialog(frame, true, new LogEntry(campaign.getDate(), ""));
		eeld.setVisible(true);
		if(null != eeld.getEntry()) {
			person.addLogEntry(eeld.getEntry());
		}
		refreshTable();
    }
    
    private void editEntry() {
    	LogEntry entry = logModel.getEntryAt(logTable.getSelectedRow());
    	if(null != entry) {
    		EditLogEntryDialog eeld = new EditLogEntryDialog(frame, true, entry);
    		eeld.setVisible(true);
    		refreshTable();
    	}
    }
    
    private void deleteEntry() {
    	person.getPersonnelLog().remove(logTable.getSelectedRow());
    	refreshTable();
    }
    
    private void refreshTable() {
		int selectedRow = logTable.getSelectedRow();
    	logModel.setData(person.getPersonnelLog());
    	if(selectedRow != -1) {
    		if(logTable.getRowCount() > 0) {
    			if(logTable.getRowCount() == selectedRow) {
    				logTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
    			} else {
    				logTable.setRowSelectionInterval(selectedRow, selectedRow);
    			}
    		}
    	}
    }
    
    /**
	 * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
	 */
	public class LogTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 534443424190075264L;

		protected String[] columnNames;
		protected ArrayList<LogEntry> data;

		public final static int COL_DATE    =    0;
		public final static int COL_DESC     =   1;
        public final static int N_COL          = 2;
		
		public LogTableModel(ArrayList<LogEntry> entries) {
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
            	case COL_DESC:
                    return "Description";
                default:
                    return "?";
            }
        }

		public Object getValueAt(int row, int col) {
	        LogEntry entry;
	        if(data.isEmpty()) {
	        	return "";
	        } else {
	        	entry = (LogEntry)data.get(row);
	        }
			if(col == COL_DATE) {
				SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
				return shortDateFormat.format(entry.getDate());
			}
			if(col == COL_DESC) {
				return entry.getDesc();
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

		public LogEntry getEntryAt(int row) {
			return (LogEntry) data.get(row);
		}
		
		 public int getColumnWidth(int c) {
	            switch(c) {
	            case COL_DESC:
	        		return 200;     
	            default:
	                return 10;
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
	        public void setData(ArrayList<LogEntry> entries) {
	            data = entries;
	            fireTableDataChanged();
	        }
	        
	        public LogTableModel.Renderer getRenderer() {
				return new LogTableModel.Renderer();
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
