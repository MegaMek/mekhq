/*
 * UnitSelectorDialog.java
 *
 * Created on August 21, 2009, 4:26 PM
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.AdvancedSearchDialog;
import megamek.client.ui.swing.MechTileset;
import megamek.client.ui.swing.MechViewPanel;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.ITechnology;
import megamek.common.MechFileParser;
import megamek.common.MechSearchFilter;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechView;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.unit.UnitTechProgression;

/**
 *
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 * (code borrowed heavily from MegaMekLab UnitSelectorDialog
 */
public class UnitSelectorDialog extends JDialog {
    private static final long serialVersionUID = 304389003995743004L;

    private MechSummary[] mechs;

    private MechTableModel unitModel;

    private static MechTileset mt;

    UnitOrder selectedUnit = null;

    private TableRowSorter<MechTableModel> sorter;

    private Campaign campaign;

    private DecimalFormat formatter;

    private MechSearchFilter searchFilter;
    AdvancedSearchDialog asd;

    private boolean addToCampaign;

    private JButton btnAddGM;
    private JButton btnBuy;
    private JButton btnClose;
    private JComboBox<String> comboUnitType;
    private JComboBox<String> comboWeight;
    private JLabel lblFilter;
    private JLabel lblImage;
    private JLabel lblUnitType;
    private JLabel lblWeight;
    private JPanel panelFilterBtns;
    private JPanel panelOKBtns;
    private JPanel panelLeft;
    private JScrollPane scrTableUnits;
    private MechViewPanel panelMekView;
    private JTable tableUnits;
    private JTextField txtFilter;
    private JSplitPane splitMain;
    private JButton btnAdvSearch;
    private JButton btnResetSearch;
    private JPanel panelSearchBtns;

    /** Creates new form UnitSelectorDialog */
    public UnitSelectorDialog(Frame frame, Campaign c, boolean add) {
        super(frame, true);
        unitModel = new MechTableModel();
        addToCampaign = add;

        //TODO: the proper way to do this would be to create a listener interface that has
        //methods like buyUnit, addUnit, etc. that we could register with this dialog
        //and then update when needed
        this.campaign = c;
        formatter = new DecimalFormat();
        asd = new AdvancedSearchDialog(frame, campaign.getCalendar().get(GregorianCalendar.YEAR));
        initComponents();

        MechSummary[] allMechs = MechSummaryCache.getInstance().getAllMechs();
        setMechs(allMechs);
        setLocationRelativeTo(frame);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        scrTableUnits = new JScrollPane();
        tableUnits = new JTable();
        panelMekView = new MechViewPanel();
        panelFilterBtns = new JPanel();
        panelLeft = new JPanel();
        lblWeight = new JLabel();
        comboWeight = new JComboBox<String>();
        lblUnitType = new JLabel();
        comboUnitType = new JComboBox<String>();
        txtFilter = new JTextField();
        lblFilter = new JLabel();
        lblImage = new JLabel();
        panelOKBtns = new JPanel();
        btnBuy = new JButton();
        btnClose = new JButton();
        btnAdvSearch = new JButton();
        btnResetSearch = new JButton();
        panelSearchBtns = new JPanel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitSelectorDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        getContentPane().setLayout(new BorderLayout());

        panelFilterBtns.setName("panelFilterBtns"); // NOI18N
        panelFilterBtns.setLayout(new java.awt.GridBagLayout());

        lblUnitType.setText(resourceMap.getString("lblUnitType.text")); // NOI18N
        lblUnitType.setName("lblUnitType"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(lblUnitType, gridBagConstraints);

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < UnitType.SIZE; i++) {
            unitTypeModel.addElement(UnitType.getTypeDisplayableName(i));
        }
        unitTypeModel.setSelectedItem(UnitType.getTypeName(UnitType.MEK));
        comboUnitType.setModel(unitTypeModel);
        comboUnitType.setMinimumSize(new java.awt.Dimension(200, 27));
        comboUnitType.setName("comboUnitType"); // NOI18N
        comboUnitType.setPreferredSize(new java.awt.Dimension(200, 27));
        comboUnitType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboUnitTypeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(comboUnitType, gridBagConstraints);

        lblWeight.setText(resourceMap.getString("lblWeight.text")); // NOI18N
        lblWeight.setName("lblWeight"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(lblWeight, gridBagConstraints);

        DefaultComboBoxModel<String> weightModel = new DefaultComboBoxModel<String>();
        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            weightModel.addElement(EntityWeightClass.getClassName(i));
        }
        weightModel.addElement("All");
        weightModel.setSelectedItem(EntityWeightClass.getClassName(EntityWeightClass.WEIGHT_LIGHT));
        comboWeight.setModel(weightModel);
        comboWeight.setMinimumSize(new java.awt.Dimension(200, 27));
        comboWeight.setName("comboWeight"); // NOI18N
        comboWeight.setPreferredSize(new java.awt.Dimension(200, 27));
        comboWeight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboWeightActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(comboWeight, gridBagConstraints);

        txtFilter.setText(resourceMap.getString("txtFilter.text")); // NOI18N
        txtFilter.setMinimumSize(new java.awt.Dimension(200, 28));
        txtFilter.setName("txtFilter"); // NOI18N
        txtFilter.setPreferredSize(new java.awt.Dimension(200, 28));
        txtFilter.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        filterUnits();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        filterUnits();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        filterUnits();
                    }
                });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(txtFilter, gridBagConstraints);

        lblFilter.setText(resourceMap.getString("lblFilter.text")); // NOI18N
        lblFilter.setName("lblFilter"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelFilterBtns.add(lblFilter, gridBagConstraints);

        lblImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblImage.setText(resourceMap.getString("lblImage.text")); // NOI18N
        lblImage.setName("lblImage"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelFilterBtns.add(lblImage, gridBagConstraints);

        panelSearchBtns.setLayout(new GridBagLayout());

        btnAdvSearch.setText(Messages.getString("MechSelectorDialog.AdvSearch")); //$NON-NLS-1$
        btnAdvSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFilter = asd.showDialog();
                btnResetSearch.setEnabled(searchFilter!=null);
                filterUnits();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelSearchBtns.add(btnAdvSearch, gridBagConstraints);

        btnResetSearch.setText(Messages.getString("MechSelectorDialog.Reset")); //$NON-NLS-1$
        btnResetSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                asd.clearValues();
                searchFilter=null;
                btnResetSearch.setEnabled(false);
                filterUnits();
            }
        });
        btnResetSearch.setEnabled(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelSearchBtns.add(btnResetSearch, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        panelFilterBtns.add(panelSearchBtns, gridBagConstraints);


        scrTableUnits.setMinimumSize(new java.awt.Dimension(500, 400));
        scrTableUnits.setName("scrTableUnits"); // NOI18N
        scrTableUnits.setPreferredSize(new java.awt.Dimension(500, 400));

        tableUnits.setFont(Font.decode(resourceMap.getString("tableUnits.font"))); // NOI18N
        tableUnits.setModel(unitModel);
        tableUnits.setName("tableUnits"); // NOI18N
        tableUnits.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<MechTableModel>(unitModel);
        sorter.setComparator(MechTableModel.COL_COST, new FormattedNumberSorter());
        tableUnits.setRowSorter(sorter);
        tableUnits.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                unitChanged(evt);
            }
        });
        TableColumn column = null;
        for (int i = 0; i < MechTableModel.N_COL; i++) {
            column = tableUnits.getColumnModel().getColumn(i);
            if (i == MechTableModel.COL_CHASSIS) {
                column.setPreferredWidth(125);
            }
            else if(i == MechTableModel.COL_MODEL
                    || i == MechTableModel.COL_COST) {
                column.setPreferredWidth(75);
            }
            else if(i == MechTableModel.COL_WEIGHT
                    || i == MechTableModel.COL_BV) {
                column.setPreferredWidth(50);
            }
            else {
                column.setPreferredWidth(25);
            }
            column.setCellRenderer(unitModel.getRenderer());

        }
        scrTableUnits.setViewportView(tableUnits);

        panelLeft.setLayout(new BorderLayout());
        panelLeft.add(panelFilterBtns, BorderLayout.PAGE_START);
        panelLeft.add(scrTableUnits, BorderLayout.CENTER);

        splitMain = new JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,panelLeft, panelMekView);
        splitMain.setOneTouchExpandable(true);
        splitMain.setResizeWeight(0.0);
        getContentPane().add(splitMain, BorderLayout.CENTER);

        if(addToCampaign) {

            panelOKBtns.setLayout(new java.awt.GridBagLayout());

            btnBuy.setText("Buy (TN: --)");
            btnBuy.setName("btnBuy"); // NOI18N
            btnBuy.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnBuyActionPerformed(evt);
                }
            });
            panelOKBtns.add(btnBuy, new java.awt.GridBagConstraints());

            btnAddGM = new JButton("Add (GM)");
            btnAddGM.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    addUnitGM();
                }
            });
            btnAddGM.setEnabled(campaign.isGM());
            panelOKBtns.add(btnAddGM, new java.awt.GridBagConstraints());


            btnClose.setText(resourceMap.getString("btnClose.text")); // NOI18N
            btnClose.setName("btnClose"); // NOI18N
            btnClose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    btnCloseActionPerformed(evt);
                }
            });
            panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        } else {
            //if we arent adding the unit to the campaign, then different buttons
            panelOKBtns.setLayout(new java.awt.GridBagLayout());

            btnAddGM = new JButton("Add");
            btnAddGM.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    //the actual work will be done by whatever called this
                    setVisible(false);
                }
            });
            panelOKBtns.add(btnAddGM, new java.awt.GridBagConstraints());

            btnClose.setText("Cancel"); // NOI18N
            btnClose.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    selectedUnit = null;
                    setVisible(false);
                }
            });
            panelOKBtns.add(btnClose, new java.awt.GridBagConstraints());

        }
        getContentPane().add(panelOKBtns, BorderLayout.PAGE_END);

        pack();
    }

    private void comboUnitTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboUnitTypeActionPerformed
        filterUnits();
    }

    private void comboWeightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboWeightActionPerformed
        filterUnits();
    }

    public Entity getEntity() {
        if(null == selectedUnit) {
            return null;
        }
        return selectedUnit.getEntity();
    }

    private void btnBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuyActionPerformed
        if(null != selectedUnit && null != selectedUnit.getEntity()) {
            /*if(!campaign.buyUnit(en)) {
	            JOptionPane.showMessageDialog(null,
	                    "You cannot afford to buy " + en.getDisplayName(),
	                    "You Can't Afford It",
	                    JOptionPane.ERROR_MESSAGE);
	            return;
	        }*/
            campaign.getShoppingList().addShoppingItem(selectedUnit, 1, campaign);
        }
        // Necessary if the user wants to buy the same unit twice without reselecting it
        unitChanged(null);
    }//GEN-LAST:event_btnBuyActionPerformed

    private void addUnitGM() {
        if(null != selectedUnit && null != selectedUnit.getEntity()) {
            campaign.addUnit(selectedUnit.getEntity(), false, 0);
        }
        // Necessary if the GM wants to add the same unit twice without reselecting it
        unitChanged(null);
    }

    private void btnBuySelectActionPerformed(java.awt.event.ActionEvent evt) {
        setVisible(false);
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        selectedUnit = null;
        setVisible(false);
    }//GEN-LAST:event_btnCloseActionPerformed

    private void filterUnits() {
        RowFilter<MechTableModel, Integer> unitTypeFilter = null;
        final int nClass = comboWeight.getSelectedIndex();
        final int nUnit = comboUnitType.getSelectedIndex();
        final int year = campaign.getCalendar().get(GregorianCalendar.YEAR);
        //If current expression doesn't parse, don't update.
        try {
            unitTypeFilter = new RowFilter<MechTableModel,Integer>() {
                @Override
                public boolean include(Entry<? extends MechTableModel, ? extends Integer> entry) {
                    MechTableModel mechModel = entry.getModel();
                    MechSummary mech = mechModel.getMechSummary(entry.getIdentifier());
                    ITechnology tech = UnitTechProgression.getProgression(mech, campaign.getTechFaction(), true);
                    if (
                            /*year limits*/
                            (!campaign.getCampaignOptions().limitByYear() || mech.getYear() <= year) &&
                            /*Clan/IS limits*/
                            (campaign.getCampaignOptions().allowClanPurchases() || !TechConstants.isClan(mech.getType())) &&
                            (campaign.getCampaignOptions().allowISPurchases() || TechConstants.isClan(mech.getType())) &&
                            /* Canon */
                            (mech.isCanon() || !campaign.getCampaignOptions().allowCanonOnly()) &&
                            /* Weight */
                            (mech.getWeightClass() == nClass || nClass == EntityWeightClass.SIZE) &&
                            /* Technology Level */
                            (null != tech) && campaign.isLegal(tech) &&
                            /*Unit type*/
                            (nUnit == UnitType.SIZE || mech.getUnitType().equals(UnitType.getTypeName(nUnit))) &&
                            (searchFilter==null || MechSearchFilter.isMatch(mech, searchFilter))) {
                        if(txtFilter.getText().length() > 0) {
                            String text = txtFilter.getText();
                            return mech.getName().toLowerCase().contains(text.toLowerCase());
                        }
                        return true;
                    }
                    return false;
                }
            };
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(unitTypeFilter);
    }

    private void unitChanged(javax.swing.event.ListSelectionEvent evt) {
        final String METHOD_NAME = "unitChanged(ListSelectionEvent)"; //$NON-NLS-1$

        int view = tableUnits.getSelectedRow();
        if(view < 0) {
            //selection got filtered away
            selectedUnit = null;
            refreshUnitView();
            return;
        }
        int selected = tableUnits.convertRowIndexToModel(view);
        // else
        MechSummary ms = mechs[selected];
        try {
            // For some unknown reason the base path gets screwed up after you
            // print so this sets the source file to the full path.
            Entity entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            selectedUnit = new UnitOrder(entity, campaign);
            btnBuy.setEnabled(true);
            btnBuy.setText("Buy (TN: " + campaign.getTargetForAcquisition(selectedUnit, campaign.getLogisticsPerson(), false).getValueAsString() + "+)");
            btnBuy.setToolTipText(campaign.getTargetForAcquisition(selectedUnit, campaign.getLogisticsPerson(), false).getDesc());
            refreshUnitView();
        } catch (EntityLoadingException ex) {
            selectedUnit = null;
            btnBuy.setEnabled(false);
            btnBuy.setText("Buy (TN: --)");
            btnBuy.setToolTipText(null);
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Unable to load mech: " + ms.getSourceFile() + ": " //$NON-NLS-1$
                    + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$
            MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
            refreshUnitView();
            return;
        }
    }

    void refreshUnitView() {
        final String METHOD_NAME = "refreshUnitView()"; //$NON-NLS-1$

        boolean populateTextFields = true;

        // null entity, so load a default unit.
        if (selectedUnit == null) {
            panelMekView.reset();
            lblImage.setIcon(null);
            return;
        }
        MechView mechView = null;
        try {
            mechView = new MechView(selectedUnit.getEntity(), false, true);
        } catch (Exception e) {
            e.printStackTrace();
            // error unit didn't load right. this is bad news.
            populateTextFields = false;
        }
        if (populateTextFields && (mechView != null)) {
            panelMekView.setMech(selectedUnit.getEntity(), true);
        } else {
            panelMekView.reset();
        }

        if (mt == null) {
            mt = new MechTileset(Configuration.unitImagesDir());
            try {
                mt.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
                //TODO: do something here
                return;
            }
        }// end if(null tileset)
        Image unitImage = mt.imageFor(selectedUnit.getEntity(), lblImage, -1);
        if(null != unitImage) {
            lblImage.setIcon(new ImageIcon(unitImage));
        }
    }
    /*
     public Entity getSelectedEntity() {
        return selectedUnit;

    }
     */
    public void setMechs (MechSummary [] m) {
        this.mechs = m;

        // break out if there are no units to filter
        if (mechs == null) {
            System.err.println("No units to filter!");
        } else {
            unitModel.setData(mechs);
        }
        filterUnits();
    }

    public void changeBuyBtnToSelectBtn () {
        for (ActionListener actionListener : btnBuy.getActionListeners()) {
            btnBuy.removeActionListener(actionListener);
        }

        ResourceBundle resourceMap = ResourceBundle.getBundle("UnitSelectorDialog", new EncodeControl()); //$NON-NLS-1$
        btnBuy.setText(resourceMap.getString("btnBuy.textSelect")); // NOI18N

        btnBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuySelectActionPerformed(evt);
            }
        });
    }

    public JComboBox<String> getComboUnitType() {
        return comboUnitType;
    }

    public JComboBox<String> getComboWeight() {
        return comboWeight;
    }


    /**
     * A table model for displaying work items
     */
    public class MechTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8472587304279640434L;
        private final static int COL_MODEL = 0;
        private final static int COL_CHASSIS = 1;
        private final static int COL_WEIGHT = 2;
        private final static int COL_BV = 3;
        private final static int COL_YEAR = 4;
        private final static int COL_COST = 5;
        private final static int N_COL = 6;

        private MechSummary[] data = new MechSummary[0];

        public MechTableModel() {
            //this.columnNames = new String[] {"Model", "Chassis"};
            //this.data = new MechSummary[0];
        }

        public int getRowCount() {
            return data.length;
        }

        public int getColumnCount() {
            return N_COL;
        }

        public int getAlignment(int col) {
            switch(col) {
            case COL_MODEL:
            case COL_CHASSIS:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.RIGHT;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch(column) {
            case COL_MODEL:
                return "Model";
            case COL_CHASSIS:
                return "Chassis";
            case COL_WEIGHT:
                return "Weight";
            case COL_BV:
                return "BV";
            case COL_YEAR:
                return "Year";
            case COL_COST:
                return "Price";
            default:
                return "?";
            }
        }

        @Override
        public Class<? extends Object> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public MechSummary getMechSummary(int i) {
            return data[i];
        }

        //fill table with values
        public void setData(MechSummary[] ms) {
            data = ms;
            fireTableDataChanged();
        }

        public Object getValueAt(int row, int col) {
            MechSummary ms = data[row];
            if(col == COL_MODEL) {
                return ms.getModel();
            }
            if(col == COL_CHASSIS) {
                return ms.getChassis();
            }
            if(col == COL_WEIGHT) {
                return ms.getTons();
            }
            if(col == COL_BV) {
                return ms.getBV();
            }
            if(col == COL_YEAR) {
                return ms.getYear();
            }
            if(col == COL_COST) {
                return formatter.format(getPurchasePrice(ms));
            }
            return "?";
        }

        private long getPurchasePrice(MechSummary ms) {
            long cost = ms.getCost();
            if(ms.getUnitType().equals(UnitType.getTypeName(UnitType.INFANTRY))
                    || ms.getUnitType().equals(UnitType.getTypeName(UnitType.BATTLE_ARMOR))) {
                cost = ms.getAlternateCost();
            }
            if(TechConstants.isClan(ms.getType())) {
                cost *= campaign.getCampaignOptions().getClanPriceModifier();
            }
            return cost;
        }

        public MechTableModel.Renderer getRenderer() {
            return new MechTableModel.Renderer();
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
                setHorizontalAlignment(getAlignment(actualCol));

                return this;
            }

        }

    }

    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * @author Jay Lawson
     *
     */
    public class FormattedNumberSorter implements Comparator<String> {

        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            long l0 = 0;
            try {
                l0 = format.parse(s0.replace(",", "")).longValue();
            } catch (java.text.ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            long l1 = 0;
            try {
                l1 = format.parse(s1.replace(",", "")).longValue();
            } catch (java.text.ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ((Comparable<Long>)l0).compareTo(l1);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        asd.clearValues();
        searchFilter=null;
        filterUnits();
        super.setVisible(visible);
    }

}
