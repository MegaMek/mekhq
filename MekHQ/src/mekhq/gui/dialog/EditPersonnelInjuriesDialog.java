/*
 * EditPersonnelInjuriesDialog.java
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
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.BodyLocation;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;

/**
 *
 * @author  Ralgith
 */
public class EditPersonnelInjuriesDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private Frame frame;
    /*private Campaign campaign;
    private int days;*/
    private Person person;
    private ArrayList<Injury> injuries;
    private InjuryTableModel injuryModel;
    
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnOK;
    private JTable injuriesTable;
    private JScrollPane scrollInjuryTable;
    
    /** Creates new form EditPersonnelInjuriesDialog */
    public EditPersonnelInjuriesDialog(java.awt.Frame parent, boolean modal, Campaign c, Person p) {
        super(parent, modal);
        this.frame = parent;
        //campaign = c;
        person = p;
        injuries = p.getInjuries();
        injuryModel = new InjuryTableModel(injuries);
        initComponents();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {

        btnOK = new javax.swing.JButton();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditPersonnelInjuriesDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title") + " " + person.getName());
        getContentPane().setLayout(new java.awt.BorderLayout());
        
        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAdd.setText(resourceMap.getString("btnAdd.text")); // NOI18N
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addEntry();
            }
        });
        panBtns.add(btnAdd);
        btnEdit.setText(resourceMap.getString("btnEdit.text")); // NOI18N
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editEntry();
            }
        });
        panBtns.add(btnEdit);
        btnDelete.setText(resourceMap.getString("btnDelete.text")); // NOI18N
        btnDelete.setName("btnDelete"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEntry();
            }
        });
        panBtns.add(btnDelete);
        getContentPane().add(panBtns, BorderLayout.PAGE_START);
        
        injuriesTable = new JTable(injuryModel);
        injuriesTable.setName("injuriesTable"); // NOI18N
		TableColumn column = null;
		int width = 0;
        for (int i = 0; i < InjuryTableModel.N_COL; i++) {
            column = injuriesTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(injuryModel.getColumnWidth(i));
            column.setCellRenderer(injuryModel.getRenderer());
            width += injuryModel.getColumnWidth(i);
        }
        setMinimumSize(new Dimension(width, 500));
        injuriesTable.setIntercellSpacing(new Dimension(0, 0));
		injuriesTable.setShowGrid(false);
		injuriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		injuriesTable.getSelectionModel().addListSelectionListener(
				new javax.swing.event.ListSelectionListener() {
					@Override
                    public void valueChanged(
							javax.swing.event.ListSelectionEvent evt) {
						injuriesTableValueChanged(evt);
					}
				});
		scrollInjuryTable = new JScrollPane();
		scrollInjuryTable.setName("scrollInjuryTable"); // NOI18N
		scrollInjuryTable.setViewportView(injuriesTable);
        getContentPane().add(scrollInjuryTable, BorderLayout.CENTER);

        
        btnOK.setText(resourceMap.getString("btnOK.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            @Override
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
    
    private void injuriesTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
		int row = injuriesTable.getSelectedRow();
		btnDelete.setEnabled(row != -1);
		btnEdit.setEnabled(row != -1);
	}
    
    private void addEntry() {
    	EditInjuryEntryDialog eied = new EditInjuryEntryDialog(frame, true, new Injury());
		eied.setVisible(true);
		if(null != eied.getEntry()) {
			person.addInjury(eied.getEntry());
		}
		refreshTable();
    }
    
    private void editEntry() {
    	Injury entry = injuryModel.getEntryAt(injuriesTable.getSelectedRow());
    	if(null != entry) {
    		EditInjuryEntryDialog eied = new EditInjuryEntryDialog(frame, true, entry);
    		eied.setVisible(true);
    		refreshTable();
    	}
    }
    
    private void deleteEntry() {
    	person.getInjuries().remove(injuriesTable.getSelectedRow());
    	refreshTable();
    }
    
    private void refreshTable() {
		int selectedRow = injuriesTable.getSelectedRow();
    	injuryModel.setData(person.getInjuries());
    	if(selectedRow != -1) {
    		if(injuriesTable.getRowCount() > 0) {
    			if(injuriesTable.getRowCount() == selectedRow) {
    				injuriesTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
    			} else {
    				injuriesTable.setRowSelectionInterval(selectedRow, selectedRow);
    			}
    		}
    	}
    }
    
    /**
	 * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
	 */
	public class InjuryTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 534443424190075264L;

		protected String[] columnNames;
		protected ArrayList<Injury> data;

		public final static int COL_DAYS	=	0;
		public final static int COL_LOCATION =	1;
		public final static int COL_TYPE	=	2;
		public final static int COL_FLUFF	=	3;
		public final static int COL_HITS	=	4;
		public final static int COL_PERMANENT =	5;
		public final static int COL_WORKEDON =	6;
		public final static int COL_EXTENDED =	7;
        public final static int N_COL		=	8;
		
		public InjuryTableModel(ArrayList<Injury> entries) {
			data = entries;
		}
		
		@Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
        	case COL_DAYS:
        		return "Days Remaining";
        	case COL_LOCATION:
        		return "Location on Body";
        	case COL_TYPE:
        		return "Type of Injury";
        	case COL_FLUFF:
                return "Fluff Message";
        	case COL_HITS:
        		return "Number of Hits";
        	case COL_PERMANENT:
        		return "Is Permanent";
        	case COL_WORKEDON:
        		return "Doctor Has Worked On";
        	case COL_EXTENDED:
        		return "Was Extended Time";
                default:
                    return "?";
            }
        }

		@Override
        public Object getValueAt(int row, int col) {
	        Injury entry;
	        if(data.isEmpty()) {
	        	return "";
	        } else {
	        	entry = (Injury)data.get(row);
	        }
	        if(col == COL_DAYS) {
				return Integer.toString(entry.getTime());
			}
	        if(col == COL_LOCATION) {
				return entry.getLocationName();
			}
	        if(col == COL_TYPE) {
				return entry.getType().getName(entry.getLocation(), entry.getHits());
			}
			if(col == COL_FLUFF) {
				return entry.getFluff();
			}
			if(col == COL_HITS) {
				return Integer.toString(entry.getHits());
			}
			if(col == COL_PERMANENT) {
				return Boolean.toString(entry.isPermanent());
			}
			if(col == COL_WORKEDON) {
				return Boolean.toString(entry.isWorkedOn());
			}
			if(col == COL_EXTENDED) {
				return Boolean.toString(entry.getExtended());
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

		public Injury getEntryAt(int row) {
			return (Injury) data.get(row);
		}
		
		 public int getColumnWidth(int c) {
            switch(c) {
            case COL_DAYS:
            case COL_HITS:
            case COL_PERMANENT:
            case COL_WORKEDON:
            case COL_EXTENDED:
            	return 110;
            case COL_TYPE:
            	return 150;
            case COL_FLUFF:
            case COL_LOCATION:
        		return 200;     
            default:
                return 50;
            }
        }
        
        public int getAlignment(int col) {
        	switch(col) {
        	case COL_DAYS:
        	case COL_HITS:
        	case COL_PERMANENT:
        	case COL_WORKEDON:
        	case COL_EXTENDED:
        		return SwingConstants.CENTER;
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
        
        //fill table with values
        public void setData(ArrayList<Injury> entries) {
            data = entries;
            fireTableDataChanged();
        }
        
        public InjuryTableModel.Renderer getRenderer() {
			return new InjuryTableModel.Renderer();
		}

		public class Renderer extends DefaultTableCellRenderer {

			private static final long serialVersionUID = 9054581142945717303L;

			@Override
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
