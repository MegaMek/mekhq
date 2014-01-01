/*
 * UnitSelectorDialog.java
 *
 * Created on August 21, 2009, 4:26 PM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.util.DirectoryItems;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.personnel.Person;
import mekhq.gui.CampaignGUI;
import mekhq.gui.XTableColumnModel;
import mekhq.gui.model.PersonnelTableModel;
import mekhq.gui.sorter.FormattedNumberSorter;
import mekhq.gui.sorter.LevelSorter;
import mekhq.gui.view.PersonViewPanel;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 * (code borrowed heavily from MegaMekLab UnitSelectorDialog
 */
public class PersonnelMarketDialog extends JDialog {
	private static final long serialVersionUID = 707579637170575313L;
	
	private PersonnelTableModel personnelModel;
	private Campaign campaign;
	private CampaignGUI hqView;
    private PersonnelMarket personnelMarket;
    boolean addToCampaign;
    Person selectedPerson = null;
    private DirectoryItems portraits;

    private JButton btnAdd;
    private javax.swing.JButton btnHire;
    private javax.swing.JButton btnClose;
    private javax.swing.JComboBox comboPersonType;
    private javax.swing.JLabel lblPersonChoice;
    private javax.swing.JPanel panelOKBtns;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelFilterBtns;
    private javax.swing.JTable tablePersonnel;
    private javax.swing.JScrollPane scrollTablePersonnel;
    private javax.swing.JScrollPane scrollPersonnelView;
    private TableRowSorter<PersonnelTableModel> sorter;
    ArrayList <RowSorter.SortKey> sortKeys;
    private javax.swing.JSplitPane splitMain;

    /** Creates new form UnitSelectorDialog */
    public PersonnelMarketDialog(Frame frame, CampaignGUI view, Campaign c, DirectoryItems portraits) {
        super(frame, true);
        hqView = view;
        campaign = c;
        this.portraits = portraits;
        personnelMarket = c.getPersonnelMarket();
        personnelModel = new PersonnelTableModel(campaign);
        personnelModel.setData(personnelMarket.getPersonnel());
        initComponents();
        filterPersonnel();
        setLocationRelativeTo(frame);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrollTablePersonnel = new javax.swing.JScrollPane();
        scrollPersonnelView = new javax.swing.JScrollPane();
        tablePersonnel = new javax.swing.JTable();
        panelMain = new javax.swing.JPanel();
        panelFilterBtns = new javax.swing.JPanel();
        comboPersonType = new javax.swing.JComboBox();
        panelOKBtns = new javax.swing.JPanel();
        btnHire = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        lblPersonChoice = new javax.swing.JLabel();
        //choicePersonView = new javax.swing.JComboBox();
        //lblPersonView = new javax.swing.JLabel();

		ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitSelectorDialog");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        getContentPane().setLayout(new BorderLayout());

        lblPersonChoice.setText("Personnel Type:"); // NOI18N
        lblPersonChoice.setName("lblPersonChoice"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panelFilterBtns.add(lblPersonChoice, gridBagConstraints);

        DefaultComboBoxModel personTypeModel = new DefaultComboBoxModel();
        for (int i = 0; i < CampaignGUI.PG_RETIRE; i++) {
        	personTypeModel.addElement(getPersonnelGroupName(i));
        }
        comboPersonType.setSelectedItem(0);
        comboPersonType.setModel(personTypeModel);
        comboPersonType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboPersonType.setName("comboUnitType"); // NOI18N
        comboPersonType.setPreferredSize(new java.awt.Dimension(200, 27));
        comboPersonType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPersonnel();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(comboPersonType, gridBagConstraints);

        
        scrollTablePersonnel.setMinimumSize(new java.awt.Dimension(500, 400));
        scrollTablePersonnel.setName("srcTablePersonnel"); // NOI18N
        scrollTablePersonnel.setPreferredSize(new java.awt.Dimension(500, 400));

        tablePersonnel.setModel(personnelModel);
        tablePersonnel.setName("tablePersonnel"); // NOI18N
        tablePersonnel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablePersonnel.setColumnModel(new XTableColumnModel());
        tablePersonnel.createDefaultColumnsFromModel();
        sorter = new TableRowSorter<PersonnelTableModel>(personnelModel);
        sorter.setComparator(PersonnelTableModel.COL_SKILL, new LevelSorter());
        sorter.setComparator(PersonnelTableModel.COL_SALARY, new FormattedNumberSorter());
        tablePersonnel.setRowSorter(sorter);
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(PersonnelTableModel.COL_SKILL, SortOrder.DESCENDING));
        sorter.setSortKeys(sortKeys);
        tablePersonnel.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablePersonnel.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                personChanged(evt);
            }
        });
        TableColumn column = null;
        for (int i = 0; i < PersonnelTableModel.N_COL; i++) {
            column = ((XTableColumnModel)tablePersonnel.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(personnelModel.getColumnWidth(i));
            column.setCellRenderer(personnelModel.getRenderer(false, null));
            if(i != PersonnelTableModel.COL_NAME && i != PersonnelTableModel.COL_TYPE
                    && i != PersonnelTableModel.COL_SKILL && i != PersonnelTableModel.COL_AGE
                    && i != PersonnelTableModel.COL_GENDER) {
                ((XTableColumnModel)tablePersonnel.getColumnModel()).setColumnVisible(column, false);
            }
        }

        tablePersonnel.setIntercellSpacing(new Dimension(0, 0));
        tablePersonnel.setShowGrid(false);
        column.setCellRenderer(getRenderer());
        scrollTablePersonnel.setViewportView(tablePersonnel);
        
        scrollPersonnelView.setMinimumSize(new java.awt.Dimension(500, 600));
        scrollPersonnelView.setPreferredSize(new java.awt.Dimension(500, 600));
        scrollPersonnelView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPersonnelView.setViewportView(null);
 
        panelMain.setLayout(new BorderLayout());
        panelMain.add(panelFilterBtns, BorderLayout.PAGE_START);
        panelMain.add(scrollTablePersonnel, BorderLayout.CENTER);
        
        splitMain = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,panelMain, scrollPersonnelView);
        splitMain.setOneTouchExpandable(true);
        splitMain.setResizeWeight(0.0);
        getContentPane().add(splitMain, BorderLayout.CENTER);
          
        panelOKBtns.setLayout(new java.awt.GridBagLayout());
        
        btnHire.setText("Hire");
        btnHire.setName("btnHire"); // NOI18N
        btnHire.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		hirePerson(evt);
        	}
        });
        panelOKBtns.add(btnHire, new java.awt.GridBagConstraints());

        btnAdd = new JButton("Add (GM)");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPerson();
            }
        });
        btnAdd.setEnabled(campaign.isGM());
        panelOKBtns.add(btnAdd, new java.awt.GridBagConstraints());

        
        btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(new java.awt.event.ActionListener() {
        	public void actionPerformed(java.awt.event.ActionEvent evt) {
        		btnCloseActionPerformed(evt);
        	}
        });
        panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        getContentPane().add(panelOKBtns, BorderLayout.PAGE_END);

        pack();
    }

	public Person getPerson() {
	    return selectedPerson;
	}
	
	private void hirePerson(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
	    if(null != selectedPerson) {
	        if(campaign.recruitPerson(selectedPerson)) {
	    		personnelMarket.removePerson(selectedPerson);
	    		personnelModel.setData(personnelMarket.getPersonnel());
	    	}
	    	refreshHqView();
	    	refreshPersonView();
	    }
	}//GEN-LAST:event_btnHireActionPerformed
	
	private void addPerson() {
	    if(null != selectedPerson) {
	    	campaign.addPersonWithoutId(selectedPerson, true);
	    	personnelMarket.removePerson(selectedPerson);
    		personnelModel.setData(personnelMarket.getPersonnel());
	    	refreshHqView();
	    	refreshPersonView();
	    }
	}

    private void refreshHqView() {
        hqView.refreshPersonnelList();
        hqView.refreshPatientList();
        hqView.refreshTechsList();
        hqView.refreshDoctorsList();
        hqView.refreshReport();
        hqView.refreshFinancialTransactions();
    }
	
	private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
	    selectedPerson = null;
	    setVisible(false);
	}//GEN-LAST:event_btnCloseActionPerformed
	
    private void filterPersonnel() {
        RowFilter<PersonnelTableModel, Integer> personTypeFilter = null;
        final int nGroup = comboPersonType.getSelectedIndex();
        //If current expression doesn't parse, don't update.
        try {
            personTypeFilter = new RowFilter<PersonnelTableModel,Integer>() {
                @Override
                public boolean include(Entry<? extends PersonnelTableModel, ? extends Integer> entry) {
                    PersonnelTableModel personModel = entry.getModel();
                	Person person = personModel.getPerson(entry.getIdentifier());
                	int type = person.getPrimaryRole();
                    if ((nGroup == CampaignGUI.PG_ACTIVE) ||
                            (nGroup == CampaignGUI.PG_COMBAT && type <= Person.T_SPACE_GUNNER) ||
                            (nGroup == CampaignGUI.PG_SUPPORT && type > Person.T_SPACE_GUNNER) ||
                            (nGroup == CampaignGUI.PG_MW && type == Person.T_MECHWARRIOR) ||
                            (nGroup == CampaignGUI.PG_CREW && (type == Person.T_GVEE_DRIVER || type == Person.T_NVEE_DRIVER || type == Person.T_VTOL_PILOT || type == Person.T_VEE_GUNNER)) ||
                            (nGroup == CampaignGUI.PG_PILOT && type == Person.T_AERO_PILOT) ||
                            (nGroup == CampaignGUI.PG_CPILOT && type == Person.T_CONV_PILOT) ||
                            (nGroup == CampaignGUI.PG_PROTO && type == Person.T_PROTO_PILOT) ||
                            (nGroup == CampaignGUI.PG_BA && type == Person.T_BA) ||
                            (nGroup == CampaignGUI.PG_SOLDIER && type == Person.T_INFANTRY) ||
                            (nGroup == CampaignGUI.PG_VESSEL && (type == Person.T_SPACE_PILOT || type == Person.T_SPACE_CREW || type == Person.T_SPACE_GUNNER || type == Person.T_NAVIGATOR)) ||
                            (nGroup == CampaignGUI.PG_TECH && type >= Person.T_MECH_TECH && type < Person.T_DOCTOR) ||
                            (nGroup == CampaignGUI.PG_DOC && ((type == Person.T_DOCTOR) || (type == Person.T_MEDIC))) ||
                            (nGroup == CampaignGUI.PG_ADMIN && type > Person.T_MEDIC)
                            ) {
                        return person.isActive();
                    } else if(nGroup == CampaignGUI.PG_RETIRE) {
                        return person.getStatus() == Person.S_RETIRED;
                    } else if(nGroup == CampaignGUI.PG_MIA) {
                        return person.getStatus() == Person.S_MIA;
                    } else if(nGroup == CampaignGUI.PG_KIA) {
                        return person.getStatus() == Person.S_KIA;
                    }
                    return false;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(personTypeFilter);
    }

    private void personChanged(javax.swing.event.ListSelectionEvent evt) {
        int view = tablePersonnel.getSelectedRow();
        if(view < 0) {
            //selection got filtered away
            selectedPerson = null;
            refreshPersonView();
            return;
        }
        selectedPerson = personnelModel.getPerson(tablePersonnel.convertRowIndexToModel(view));
        refreshPersonView();
    }

     void refreshPersonView() {
    	 int row = tablePersonnel.getSelectedRow();
         if(row < 0) {
             scrollPersonnelView.setViewportView(null);
             return;
         }
    	 scrollPersonnelView.setViewportView(new PersonViewPanel(selectedPerson, campaign, portraits));
 		//This odd code is to make sure that the scrollbar stays at the top
 		//I cant just call it here, because it ends up getting reset somewhere later
 		javax.swing.SwingUtilities.invokeLater(new Runnable() {
 			public void run() { 
 				scrollPersonnelView.getVerticalScrollBar().setValue(0);
 			}
 		});
    }

    public JComboBox getComboUnitType() {
        return comboPersonType;
    }
	
	@Override
    public void setVisible(boolean visible) {
        filterPersonnel();
        //changePersonnelView();
        super.setVisible(visible);
    }
	
	public TableCellRenderer getRenderer() {
        //if(choicePersonView.getSelectedIndex() == CampaignGUI.PV_GRAPHIC) {
            //return personnelModel.new VisualRenderer(hqView.getCamos(), portraits, hqView.getMechTiles());
       // }
        return personnelModel.new Renderer();
    }

    public static String getPersonnelGroupName(int group) {
        switch(group) {
        case CampaignGUI.PG_ACTIVE:
            return "All Personnel";
        case CampaignGUI.PG_COMBAT:
            return "Combat Personnel";
        case CampaignGUI.PG_MW:
            return "Mechwarriors";
        case CampaignGUI.PG_CREW:
            return "Vehicle Crews";
        case CampaignGUI.PG_PILOT:
            return "Aerospace Pilots";
        case CampaignGUI.PG_CPILOT:
            return "Conventional Pilots";
        case CampaignGUI.PG_PROTO:
            return "Protomech Pilots";
        case CampaignGUI.PG_BA:
            return "Battle Armor Infantry";
        case CampaignGUI.PG_SOLDIER:
            return "Conventional Infantry";
        case CampaignGUI.PG_SUPPORT:
            return "Support Personnel";
        case CampaignGUI.PG_VESSEL:
            return "Large Vessel Crews";
        case CampaignGUI.PG_TECH:
            return "Techs";
        case CampaignGUI.PG_DOC:
            return "Medical Staff";
        case CampaignGUI.PG_ADMIN:
            return "Administrators";
        case CampaignGUI.PG_RETIRE:
            return "Retired Personnel";
        case CampaignGUI.PG_MIA:
            return "Personnel MIA";
        case CampaignGUI.PG_KIA:
            return "Rolls of Honor (KIA)";
        default:
            return "?";
        }
    }
    
}
