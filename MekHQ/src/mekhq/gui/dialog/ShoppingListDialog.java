/*
 * ShoppingListDialog.java
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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
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
public class ShoppingListDialog extends javax.swing.JDialog { 
    /**
     * 
     */
    private static final long serialVersionUID = -5610859410692908738L;
   
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
    private AcquireTableModel acquireModel;
    private TableRowSorter<AcquireTableModel> acquireSorter;
    
    private JTable acquireTable;
    private JScrollPane scrollAcquireTable;
    private JPanel panFilter;
    private JLabel lblFilter;
    private javax.swing.JTextField txtFilter;
    private JComboBox choiceParts;
    private JLabel lblPartsChoice;
    private JTextArea txtInstructions;
    private JPanel panButtons;
    private JButton btnClose;
    
    /** Creates new form NewTeamDialog */
    public ShoppingListDialog(boolean modal, CampaignGUI gui) {
        super(gui.getFrame(), modal);
        this.frame = gui.getFrame();  
        this.campaignGUI = gui;
        campaign = campaignGUI.getCampaign();
        formatter = new DecimalFormat();
        acquireModel = new AcquireTableModel(campaign.getShoppingList().getList());
        initComponents();
        filterParts();
        setLocationRelativeTo(frame);
    }

    private void initComponents() {
        
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ShoppingListDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("Form.title"));
        
        getContentPane().setLayout(new BorderLayout());
      
        acquireTable = new JTable(acquireModel);
        acquireTable.setName("partsTable"); // NOI18N
        acquireSorter = new TableRowSorter<AcquireTableModel>(acquireModel);
        acquireSorter.setComparator(AcquireTableModel.COL_COST, campaignGUI.new FormattedNumberSorter());
        acquireSorter.setComparator(AcquireTableModel.COL_TARGET, new TargetSorter());
        acquireTable.setRowSorter(acquireSorter);
        TableColumn column = null;
        for (int i = 0; i < AcquireTableModel.N_COL; i++) {
            column = acquireTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(acquireModel.getColumnWidth(i));
            column.setCellRenderer(acquireModel.getRenderer());
        }
        acquireTable.setIntercellSpacing(new Dimension(0, 0));
        acquireTable.setShowGrid(false);
        
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "DELETE");
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "DELETE");
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ADD");
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "ADD");
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "REMOVE");
        acquireTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "REMOVE");

        acquireTable.getActionMap().put("DELETE", new AbstractAction() {
           public void actionPerformed(ActionEvent e) {
               if(acquireTable.getSelectedRow() < 0) {
                   return;
               }
               if(0 != JOptionPane.showConfirmDialog(null,
                       "Are you sure you want to remove all requests for\n" + acquireModel.getNewPartAt(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow())).getName() + " from the shopping list?"
                   , "Delete Shopping Item?",
                       JOptionPane.YES_NO_OPTION)) {
                   return;
               }
               acquireModel.removeRow(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow()));
           }
        });
        
        acquireTable.getActionMap().put("ADD", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(acquireTable.getSelectedRow() < 0) {
                    return;
                }
                acquireModel.incrementItem(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow()));
            }
         });
        
        acquireTable.getActionMap().put("REMOVE", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(acquireTable.getSelectedRow() < 0) {
                    return;
                }
                if(acquireModel.getAcquisition(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow())).getQuantity() <= 1) {
                    if(0 != JOptionPane.showConfirmDialog(null,
                            "Are you sure you want to remove the only request for\n" + acquireModel.getNewPartAt(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow())).getName() + " from the shopping list?"
                        , "Delete Shopping Item?",
                            JOptionPane.YES_NO_OPTION)) {
                        return;
                    }
                    acquireModel.removeRow(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow()));
                } else {
                    acquireModel.decrementItem(acquireTable.convertRowIndexToModel(acquireTable.getSelectedRow()));
                }
            }
         });
        
        scrollAcquireTable = new JScrollPane();
        scrollAcquireTable.setName("scrollPartsTable"); // NOI18N
        scrollAcquireTable.setViewportView(acquireTable);
        getContentPane().add(scrollAcquireTable, BorderLayout.CENTER);

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
        c.weightx = 0.0;
        panFilter.add(choiceParts, c);
        
        lblFilter = new JLabel(resourceMap.getString("lblFilter.text")); // NOI18N
        lblFilter.setName("lblFilter"); // NOI18N
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
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
        c.weightx = 0.0;
        c.weighty = 1.0;
        panFilter.add(txtFilter, c);
        
        txtInstructions = new javax.swing.JTextArea();
        txtInstructions.setText(resourceMap.getString("txtInstructions.text"));
        txtInstructions.setName("txtInstructions");
        txtInstructions.setEditable(false);
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructions.setOpaque(false);
        
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        panFilter.add(txtInstructions, c);
        
        getContentPane().add(panFilter, BorderLayout.PAGE_START);

        
        
        panButtons = new JPanel();
        btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
        panButtons.setLayout(new GridBagLayout());
        panButtons.add(btnClose, new GridBagConstraints());
        getContentPane().add(panButtons, BorderLayout.PAGE_END);        
        this.setPreferredSize(new Dimension(650,600));
        pack();
    }
    
    public void filterParts() {
        RowFilter<AcquireTableModel, Integer> partsTypeFilter = null;
        final int nGroup = choiceParts.getSelectedIndex();
        partsTypeFilter = new RowFilter<AcquireTableModel,Integer>() {
            @Override
            public boolean include(Entry<? extends AcquireTableModel, ? extends Integer> entry) {
                AcquireTableModel acquireModel = entry.getModel();
                Part part = acquireModel.getNewPartAt(entry.getIdentifier());
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
        acquireSorter.setRowFilter(partsTypeFilter);
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
    public class AcquireTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 534443424190075264L;

        protected String[] columnNames;
        protected ArrayList<IAcquisitionWork> data;

        public final static int COL_NAME    =    0;
        public final static int COL_DETAIL   =   1;
        public final static int COL_TECH_BASE  = 2;
        public final static int COL_COST     =   3;
        public final static int COL_TON       =  4;
        public final static int COL_TARGET    =  5;
        public final static int COL_QUEUE     =  6;
        public final static int N_COL          = 7;
        
        public AcquireTableModel(ArrayList<IAcquisitionWork> shoppingList) {
            data = shoppingList;
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
                    return "Quantity";
                default:
                    return "?";
            }
        }
        
        public void incrementItem(int row) {
            ((IAcquisitionWork)data.get(row)).incrementQuantity();
            this.fireTableCellUpdated(row, COL_QUEUE);
        }
        
        public void decrementItem(int row) {
            ((IAcquisitionWork)data.get(row)).decrementQuantity();
            this.fireTableCellUpdated(row, COL_QUEUE);
        }
        
        public void removeRow(int row) {
            campaign.getShoppingList().removeItem(getNewPartAt(row));
            data = campaign.getShoppingList().getList();
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            Part part;
            IAcquisitionWork shoppingItem;
            if(data.isEmpty()) {
                return "";
            } else {
                part = getNewPartAt(row);
                shoppingItem = getAcquisition(row);
            }
            if(null == part || null == shoppingItem) {
                return "?";
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
                TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, campaign.getLogisticsPerson(), false);
                String value = target.getValueAsString();
                if(target.getValue() != TargetRoll.IMPOSSIBLE && target.getValue() != TargetRoll.AUTOMATIC_SUCCESS && target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                    value += "+";
                }
                return value;
            }
            if(col == COL_QUEUE) {
                return shoppingItem.getQuantity();
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

        public Part getNewPartAt(int row) {
            return ((IAcquisitionWork) data.get(row)).getNewPart();
        }
        
        public IAcquisitionWork getAcquisition(int row) {
            return (IAcquisitionWork) data.get(row);
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
                IAcquisitionWork shoppingItem;
                if(data.isEmpty()) {
                    return null;
                } else {
                    part = getNewPartAt(row);
                    shoppingItem = getAcquisition(row);
                }
                if(null == part || null ==shoppingItem) {
                    return null;
                }
                switch(col) {
                case COL_TARGET:                    
                    TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, campaign.getLogisticsPerson(), false);
                    return target.getDesc();
                default:
                    return null;
                }
            }
            public AcquireTableModel.Renderer getRenderer() {
                return new AcquireTableModel.Renderer();
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
