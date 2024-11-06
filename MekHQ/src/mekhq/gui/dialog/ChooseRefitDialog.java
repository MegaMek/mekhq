/*
 * ChooseRefitDialog.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.util.UIUtil;
import megamek.codeUtilities.StringUtility;
import megamek.common.Entity;
import megamek.common.MekFileParser;
import megamek.common.MekSummary;
import megamek.common.MekView;
import megamek.common.ViewFormatting;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.RefitStep;
import mekhq.campaign.parts.enums.RefitStepType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.utilities.ReportingUtilities;

/**
 * @author Taharqa
 */
public class ChooseRefitDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(ChooseRefitDialog.class);
    private ResourceBundle resourceMap;

    // region Variables
    private Campaign campaign;
    private Unit unit;
    private RefitTableModel refitModel;
    private RefitNeededListTableModel neededModel;
    private RefitReturnsListTableModel returnsModel;
    private RefitStepsListTableModel stepsModel;

    private List<Refit> kitRefits;
    private List<Refit> customRefits;
    private List<Refit> omniRefits;

    private JLabel noRefitsLbl;
    private JButton btnClose;
    private JButton btnGo;
    private JRadioButton radRefit;
    private JRadioButton radCustomize;
    private JRadioButton radOmni;
    private JCheckBox chkHideLeaves;
    
    private JTable refitTable;
    private JScrollPane scrRefitTable;
    private JTable neededTable;
    private JScrollPane scrNeededTable;
    private JTable returnsTable;
    private JScrollPane scrReturnsTable;
    private JTable stepsTable;
    private JScrollPane scrStepsTable;

    private JPanel leftSidePanel;

    private JSplitPane horizMainSplitter;
    private JSplitPane vertLeftListSplitter;
    private JSplitPane vertRightListSplitter;
    private JSplitPane horizReadoutSplitter;

    private JTextPane txtOldUnit;
    private JTextPane txtNewUnit;
    private JScrollPane scrOldUnit;
    private JScrollPane scrNewUnit;

    private JPanel totalsPanel;
    private JTextPane totalsText;

    private boolean confirmed = false;
    private boolean customize = false;
    // endregion Variable Declarations

    // region Constructors
    /** Creates new form EditPersonnelLogDialog */
    public ChooseRefitDialog(JFrame parent, boolean modal, Campaign c, Unit unit) {
        super(parent, modal);
        campaign = c;
        this.unit = unit;
        populateRefits();
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }
    // endregion Constructors

    // region Initialization
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseRefitDialog",
                MekHQ.getMHQOptions().getLocale());

        setTitle(resourceMap.getString("title.text") + " " + unit.getName());

        getContentPane().setLayout(new GridBagLayout());

        // region Radio Button Panel

        leftSidePanel = new JPanel(new GridBagLayout());

        JPanel radioPanel = new JPanel(new GridBagLayout());

        radRefit = new JRadioButton(resourceMap.getString("radRefit.text"));
        radRefit.addActionListener(evt -> setRefit());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0,5,0,0);
        radioPanel.add(radRefit, gridBagConstraints);

        radCustomize = new JRadioButton(resourceMap.getString("radCustomize.text"));
        radCustomize.addActionListener(evt -> setCustomize());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        radioPanel.add(radCustomize, gridBagConstraints);

        radOmni = new JRadioButton(resourceMap.getString("radOmni.text"));
        radOmni.addActionListener(evt -> setOmni());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 5, 0, 0);
        radioPanel.add(radOmni, gridBagConstraints);

        ButtonGroup group = new ButtonGroup();
        group.add(radRefit);
        group.add(radCustomize);
        group.add(radOmni);

        radioPanel.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("iWantTo.title")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);

        leftSidePanel.add(radioPanel, gridBagConstraints);


        // region Left Side Lists
        
        refitModel = new RefitTableModel();
        refitTable = new JTable(refitModel);
        TableColumn column;
        for (int i = 0; i < RefitTableModel.N_COL; i++) {
            column = refitTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(refitModel.getColumnWidth(i));
            column.setCellRenderer(refitModel.getRenderer());
        }
        refitTable.setIntercellSpacing(new Dimension(0, 0));
        refitTable.setShowGrid(false);
        refitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refitTable.getSelectionModel().addListSelectionListener(evt -> refitTableValueChanged());
        TableRowSorter<RefitTableModel> refitSorter = new TableRowSorter<>(refitModel);
        refitSorter.setComparator(RefitTableModel.COL_CLASS, new ClassSorter());
        refitSorter.setComparator(RefitTableModel.COL_COST, new FormattedNumberSorter());
        refitTable.setRowSorter(refitSorter);
        scrRefitTable = new JScrollPaneWithSpeed();
        scrRefitTable.setViewportView(refitTable);
        scrRefitTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("refitTable.title")));

        stepsModel = new RefitStepsListTableModel();
        stepsTable = new JTable(stepsModel);
        for (int i = 0; i < RefitStepsListTableModel.N_COL; i++) {
            column = stepsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(stepsModel.getColumnWidth(i));
            column.setCellRenderer(stepsModel.getRenderer());
        }
        stepsTable.setIntercellSpacing(new Dimension(0, 0));
        stepsTable.setShowGrid(false);

        scrStepsTable = new JScrollPaneWithSpeed(stepsTable);
        scrStepsTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("stepsTable.title")));

        vertLeftListSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrRefitTable, scrStepsTable);
        vertLeftListSplitter.setResizeWeight(0.5);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        leftSidePanel.add(vertLeftListSplitter, gridBagConstraints);

        noRefitsLbl = new JLabel(resourceMap.getString("noRefitsLbl.text"));
        noRefitsLbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        leftSidePanel.add(noRefitsLbl, gridBagConstraints);
        noRefitsLbl.setVisible(false);

        // region Totals Panel

        totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("totals.title")));


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        totalsText = new JTextPane();
        totalsText.setEditable(false);
        totalsText.setContentType("text/html");

   
        
        totalsPanel.add(totalsText, gridBagConstraints);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        leftSidePanel.add(totalsPanel, gridBagConstraints);
        

        // region Right Side Tab Panel
        
        txtOldUnit = new JTextPane();
        txtOldUnit.setEditable(false);
        txtOldUnit.setContentType("text/html");
        txtOldUnit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtOldUnit.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        MekView mv = new MekView(unit.getEntity(), false, true, true, ViewFormatting.HTML);
        txtOldUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMekReadout() + "</div>");
        scrOldUnit = new JScrollPaneWithSpeed(txtOldUnit);

        SwingUtilities.invokeLater(() -> scrOldUnit.getVerticalScrollBar().setValue(0));
        gridBagConstraints = new GridBagConstraints();
        txtNewUnit = new JTextPane();
        txtNewUnit.setEditable(false);
        txtNewUnit.setContentType("text/html");
        txtNewUnit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtNewUnit.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrNewUnit = new JScrollPaneWithSpeed(txtNewUnit);

        horizReadoutSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrOldUnit, scrNewUnit);
        horizReadoutSplitter.setResizeWeight(0.5);

        neededModel = new RefitNeededListTableModel();
        neededTable = new JTable(neededModel);
        for (int i = 0; i < RefitNeededListTableModel.N_COL; i++) {
            column = neededTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(neededModel.getColumnWidth(i));
            column.setCellRenderer(neededModel.getRenderer());
        }
        neededTable.setIntercellSpacing(new Dimension(0, 0));
        neededTable.setShowGrid(false);

        scrNeededTable = new JScrollPaneWithSpeed(neededTable);
        scrNeededTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("neededTable.title")));
   

        returnsModel = new RefitReturnsListTableModel();
        returnsTable = new JTable(returnsModel);
        for (int i = 0; i < RefitReturnsListTableModel.N_COL; i++) {
            column = returnsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(returnsModel.getColumnWidth(i));
            column.setCellRenderer(returnsModel.getRenderer());
        }
        returnsTable.setIntercellSpacing(new Dimension(0, 0));
        returnsTable.setShowGrid(false);

        scrReturnsTable = new JScrollPaneWithSpeed(returnsTable);
        scrReturnsTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("returnsTable.title")));
      
        vertRightListSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrNeededTable, scrReturnsTable);
        vertRightListSplitter.setResizeWeight(0.5);


        JTabbedPane tabPane = new JTabbedPane();
        tabPane.add(resourceMap.getString("tabUnitView.title"), horizReadoutSplitter);
        tabPane.add(resourceMap.getString("tabListView.title"), vertRightListSplitter);
        
        horizMainSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSidePanel, tabPane);
        horizMainSplitter.setResizeWeight(0.5);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(horizMainSplitter, gridBagConstraints);


        // region Bottom Buttons


        JPanel panBtn = new JPanel(new GridBagLayout());

        chkHideLeaves = new JCheckBox(resourceMap.getString("chkHideLeaves.text"));
        chkHideLeaves.setSelected(true);
        chkHideLeaves.addActionListener(evt -> refitTableValueChanged());
        
        panBtn.add(chkHideLeaves, new GridBagConstraints());

        btnGo = new JButton(resourceMap.getString("btnGo-Refit.text"));
        btnGo.setEnabled(false);
        btnGo.addActionListener(evt -> confirm());
        panBtn.add(btnGo, new GridBagConstraints());

        btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(evt -> cancel());
        panBtn.add(btnClose, new GridBagConstraints());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(panBtn, gridBagConstraints);

        if (unit.getEntity().isOmni() && !omniRefits.isEmpty()) {
            radOmni.setSelected(true);
            setOmni();
        } else {
            if (!kitRefits.isEmpty()) { 
                radRefit.setSelected(true);
                setRefit();
            } else if (!customRefits.isEmpty()) {
                radCustomize.setSelected(true);
                setCustomize();
            }
            else {
                vertLeftListSplitter.setVisible(false);
                noRefitsLbl.setVisible(true);
            }
        }

        radRefit.setEnabled(!kitRefits.isEmpty());
        radCustomize.setEnabled(!customRefits.isEmpty());
        radOmni.setEnabled(!omniRefits.isEmpty());

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChooseRefitDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    
    // region Acitons
    private void confirm() {
        confirmed = getSelectedRefit() != null;
        customize = radCustomize.isSelected();
        setVisible(false);
    }

    private void cancel() {
        setVisible(false);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isCustomize() {
        return customize;
    }

    private void setRefit() {
        btnGo.setText(resourceMap.getString("btnGo-Refit.text"));
        refitModel.setData(kitRefits);
    }

    private void setCustomize() {
        btnGo.setText(resourceMap.getString("btnGo-Customize.text"));
        refitModel.setData(customRefits);
    }

    private void setOmni() {
        btnGo.setText(resourceMap.getString("btnGo-Omni.text"));
        refitModel.setData(omniRefits);
    }

    public Refit getSelectedRefit() {
        int selectedRow = refitTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return refitModel.getRefitAt(refitTable.convertRowIndexToModel(selectedRow));
    }

    
    // region Refit Table Changed
    private void refitTableValueChanged() {
        Refit refit = getSelectedRefit();
        if (null == refit) {
            neededModel.setData(new ArrayList<Part>());
            returnsModel.setData(new ArrayList<Part>());
            stepsModel.setData(new ArrayList<RefitStep>());
            txtNewUnit.setText("");
            totalsText.setText(" ");
            btnGo.setEnabled(false);
            return;
        }
        
        neededModel.setData(refit.getNeededList());
        returnsModel.setData(refit.getReturnsList());

        if(chkHideLeaves.isSelected()){
            stepsModel.setData(refit.getStepsList().stream()
                    .filter(step -> step.getType() != RefitStepType.LEAVE)
                    .collect(Collectors.toList()));
        } else {
            stepsModel.setData(refit.getStepsList());
        }

        StringBuilder totals = new StringBuilder();

        totals
            .append("<html><b>").append(resourceMap.getString("totals-baseTime.text")).append("</b>: ")
            .append(refit.getUnmodifiedTime()).append('m');
        if (refit.getUnmodifiedTime() >= 480) {
            totals.append(" (").append(makeRefitTimeDisplay(refit.getUnmodifiedTime())).append(')');
        }
        totals
            .append(" &nbsp;&nbsp;<b>").append(resourceMap.getString("totals-refitClass.text")).append("</b>: ")
            .append(refit.getRefitClass().toShortName())
            .append(" &nbsp;&nbsp;<b>").append(resourceMap.getString("totals-multiplier.text")).append("</b>: ")
            .append(String.format("%.2fx", refit.getRefitMultiplier()))
            .append(" &nbsp;&nbsp;<b>").append(resourceMap.getString("totals-final.text")).append("</b>: ")
            .append(refit.getTime()).append('m');
        if (refit.getTime() >= 480) {
            totals.append(" (").append(makeRefitTimeDisplay(refit.getTime())).append(')');
        }

        totalsText.setText(totals.toString());

        MekView mv = new MekView(refit.getNewEntity(), false, true);
        txtNewUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMekReadout() + "</div>");
        SwingUtilities.invokeLater(() -> scrNewUnit.getVerticalScrollBar().setValue(0));
        btnGo.setEnabled(true);
    }


    // region Populate Refits
    private void populateRefits() {
        kitRefits = new ArrayList<Refit>();
        customRefits = new ArrayList<Refit>();
        omniRefits = new ArrayList<Refit>();

        Entity entity = unit.getEntity();
        String chassis = entity.getFullChassis();
        for (String model : Utilities.getAllVariants(entity, campaign)) {
            model = StringUtility.isNullOrBlank(model) ? "" : " " + model;
            try {
                MekSummary summary = Utilities.retrieveUnit(chassis + model);
                Entity refitEn = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                if (null != refitEn) {

                    Refit kitRefit = new Refit(unit, refitEn, false, false, false);
                    boolean canonok = !campaign.getCampaignOptions().isAllowCanonRefitOnly() 
                            || kitRefit.getNewEntity().isCanon();
                    if (canonok && !kitRefit.isOmniRefit()) {
                        kitRefits.add(kitRefit);
                    }
                    
                    Refit customRefit = new Refit(unit, refitEn, true, false, false);
                    if (customRefit.isOmniRefit()) {
                        omniRefits.add(customRefit);
                    } else {
                        customRefits.add(customRefit);
                    }
                
                }
            } catch (EntityLoadingException ex) {
                logger.error("", ex);
            }
        }
    }


    
    public static int getActualQuantity(Part part) {
        if (part instanceof Armor) {
            return ((Armor) part).getAmount();
        } else if (part instanceof AmmoStorage) {
            return ((AmmoStorage) part).getShots();
        } else {
            return part.getQuantity();
        }
    }

    public int getToOrder(Part part) {
        PartInventory inventory = campaign.getPartInventory(part);
        return Math.max(0, getActualQuantity(part)
                - inventory.getSupply()
                - inventory.getTransit()
                - inventory.getOrdered());
    }

    // region RefitTableModel
    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but
     * not exactly
     */
    public class RefitTableModel extends AbstractTableModel {
        protected String[] columnNames;
        protected List<Refit> data;

        public final static int COL_MODEL = 0;
        public final static int COL_CLASS = 1;
        public final static int COL_BV = 2;
        public final static int COL_TIME = 3;
        public final static int COL_COST = 4;
        public final static int N_COL = 5;

        public RefitTableModel() {
            data = new ArrayList<Refit>();
        }

        public RefitTableModel(List<Refit> refits) {
            data = refits;
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
            return switch (column) {
                case COL_MODEL -> "Model";
                case COL_CLASS -> "Class";
                case COL_BV -> "BV";
                case COL_TIME -> "Time";
                case COL_COST ->  "Cost";
                default -> "?";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (data.isEmpty()) {
                return "";
            }
            Refit refit = data.get(row);

            return switch(col) {            
            case COL_MODEL -> refit.getNewEntity().getModel();
            case COL_CLASS -> refit.getRefitClassName();
            case COL_BV -> refit.getNewEntity().calculateBattleValue(true, true);
            case COL_TIME -> makeRefitTimeDisplay(refit.getTime());
            case COL_COST -> refit.getCost().toAmountAndSymbolString();
            default -> "?";
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Refit getRefitAt(int row) {
            return data.get(row);
        }

        public int getColumnWidth(int col) {
            return switch (col) {
                case COL_MODEL -> 75;
                case COL_CLASS -> 110;
                case COL_COST -> 40;
                default -> 10;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_MODEL, COL_CLASS -> SwingConstants.LEFT;
                default -> SwingConstants.RIGHT;
            };
        }

        // fill table with values
        public void setData(List<Refit> refits) {
            data = refits;
            fireTableDataChanged();
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                setHorizontalAlignment(getAlignment(actualCol));

                return this;
            }
        }
    }


    // region Needed Model

    public class RefitNeededListTableModel extends AbstractTableModel {
        public final static int COL_NEEDED = 0;
        public final static int COL_NAME = 1;
        public final static int COL_TECH_BASE = 2;
        public final static int COL_STOCK = 3;
        public final static int COL_TRANSIT = 4;
        public final static int COL_ORDERED = 5;
        public final static int COL_TARGET = 6;
        public final static int COL_TOORDER = 7;
        public final static int COL_COST = 8;
        public final static int N_COL = 9;

        private List<Part> data;

        public RefitNeededListTableModel() {
            data = new ArrayList<Part>();
        }

        public RefitNeededListTableModel(List<Part> parts) {
            data = parts;
        }

        public void setData(List<Part> parts) {
            data = parts;
            fireTableDataChanged();
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
            return switch (column) {
                case COL_NAME -> "Name";
                case COL_TECH_BASE -> "Tech";
                case COL_STOCK -> "Stock";
                case COL_TRANSIT -> "Transit";
                case COL_ORDERED -> "Ordered";
                case COL_NEEDED -> "Needed";
                case COL_TARGET -> "TN";
                case COL_TOORDER -> "To Order";
                case COL_COST -> "Cost/Unit";
                default -> "?";
            };
        }



        @Override
        public Object getValueAt(int row, int col) {
            Part part;
            if (data.isEmpty()) {
                return "";
            } else {
                part = (Part) data.get(row);
            }

            return switch(col) {
                case COL_NAME -> "<html><nobr>"
                        + part.getName() + ReportingUtilities.surroundIf(" (", part.getDetails(), ")")
                        + "</nobr></html>";
                case COL_TECH_BASE -> part.getTechBaseName();
                case COL_STOCK -> campaign.getPartInventory(part).getSupply();
                case COL_TRANSIT -> campaign.getPartInventory(part).getTransit();
                case COL_ORDERED -> campaign.getPartInventory(part).getOrdered();
                case COL_NEEDED -> getActualQuantity(part);
                case COL_TOORDER -> "<html><b>" + getToOrder(part) + "</b></html>";
                case COL_COST -> part.getActualValue().multipliedBy(getToOrder(part)).toAmountAndSymbolString();
                case COL_TARGET -> campaign.getTargetForAcquisition((part.getAcquisitionWork())).getValueAsString();
                default -> "?";
            };
        }

        public Part getPartAt(int row) {
            return ((Part) data.get(row));
        }

        public int getColumnWidth(int c) {
            return switch (c) {
                case COL_NAME -> 180;
                case COL_TECH_BASE -> 26;
                case COL_STOCK, COL_TRANSIT, COL_ORDERED, COL_NEEDED, COL_TARGET, COL_TOORDER -> 20;
                case COL_COST -> 60;
                default -> 3;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_TECH_BASE, COL_STOCK, COL_TRANSIT, COL_ORDERED,
                    COL_NEEDED, COL_TARGET, COL_TOORDER -> SwingConstants.CENTER;
                case COL_COST -> SwingConstants.RIGHT;
                default -> SwingConstants.LEFT;
            };
        }

        public String getTooltip(int row, int col) {
            Part part;
            if (data.isEmpty()) {
                return null;
            } else {
                part = data.get(row);
            }
            return switch (col) {
                case COL_TARGET -> campaign.getTargetForAcquisition(part.getAcquisitionWork()).getDesc();
                default -> null;
            };
        }

        public Renderer getRenderer() {
            return new Renderer();
        }
        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                return this;
            }
        }
    }

    // region Returns Model

    public class RefitReturnsListTableModel extends AbstractTableModel {
        public final static int COL_RECEIVING = 0;
        public final static int COL_NAME = 1;
        public final static int COL_TECH_BASE = 2;
        public final static int COL_STOCK = 3;
        public final static int COL_VALUE = 4;
        public final static int N_COL = 5;

        private List<Part> data;

        public RefitReturnsListTableModel() {
            data = new ArrayList<Part>();
        }

        public RefitReturnsListTableModel(List<Part> parts) {
            data = parts;
        }

        public void setData(List<Part> parts) {
            data = parts;
            fireTableDataChanged();
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
            return switch (column) {
                case COL_NAME -> "Name";
                case COL_TECH_BASE -> "Tech";
                case COL_STOCK -> "Stock";
                case COL_RECEIVING -> "Getting Back";
                case COL_VALUE -> "Value Returned";
                default -> "?";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            Part part;
            if (data.isEmpty()) {
                return "";
            } else {
                part = (Part) data.get(row);
            }

            return switch(col) {
                case COL_NAME -> "<html><nobr>"
                        + part.getName() + ReportingUtilities.surroundIf(" (", part.getDetails(), ")")
                        + "</nobr></html>";
                case COL_TECH_BASE -> part.getTechBaseName();
                case COL_STOCK -> campaign.getPartInventory(part).getSupply();
                case COL_RECEIVING -> getActualQuantity(part);
                case COL_VALUE -> part.getActualValue().multipliedBy(part.getQuantity()).toAmountAndSymbolString();
                default -> "?";
            };
        }

        public Part getPartAt(int row) {
            return ((Part) data.get(row));
        }

        public int getColumnWidth(int c) {
            return switch (c) {
                case COL_NAME -> 180;
                case COL_TECH_BASE -> 26;
                case COL_STOCK, COL_RECEIVING -> 20;
                case COL_VALUE -> 50;
                default -> 3;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_TECH_BASE, COL_STOCK, COL_RECEIVING -> SwingConstants.CENTER;
                case COL_VALUE -> SwingConstants.RIGHT;
                default -> SwingConstants.LEFT;
            };
        }
        
        public Renderer getRenderer() {
            return new Renderer();
        }
        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                setHorizontalAlignment(getAlignment(actualCol));

                return this;
            }
        }
    }



    // region Steps Model

    public class RefitStepsListTableModel extends AbstractTableModel {
        public final static int COL_OLD_NAME = 0;
        public final static int COL_OLD_LOC = 1;
        public final static int COL_OLD_QUANTITY = 2;
        public final static int COL_REFITSTEP_TYPE = 3;
        public final static int COL_NEW_QUANTITY = 4;
        public final static int COL_NEW_LOC = 5;
        public final static int COL_NEW_NAME = 6;
        public final static int COL_NOTES = 7;
        public final static int COL_BASETIME = 8;
        public final static int COL_REFIT_CLASS = 9;
        public final static int N_COL = 10;

        private List<RefitStep> data;

        public RefitStepsListTableModel() {
            data = new ArrayList<RefitStep>();
        }

        public RefitStepsListTableModel(List<RefitStep> parts) {
            data = parts;
        }

        public void setData(List<RefitStep> parts) {
            data = parts;
            fireTableDataChanged();
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
            return switch (column) {
                case COL_OLD_NAME -> "Old Item";
                case COL_NEW_NAME -> "New Item";
                case COL_REFITSTEP_TYPE -> "Step Type";
                case COL_OLD_LOC -> "Old Location";
                case COL_NEW_LOC -> "New Location";
                case COL_OLD_QUANTITY -> "Old #";
                case COL_NEW_QUANTITY -> "New #";
                case COL_NOTES -> "Notes";
                case COL_BASETIME -> "Base Time";
                case COL_REFIT_CLASS -> "Refit Class";
                default -> "?";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            RefitStep refitStep;
            if (data.isEmpty()) {
                return "";
            } else {
                refitStep = (RefitStep) data.get(row);
            }

            return switch(col) {
                case COL_OLD_NAME -> "<html><nobr>" + refitStep.getOldPartName() + "</nobr></html>";
                case COL_NEW_NAME -> "<html><nobr>" + refitStep.getNewPartName() + "</nobr></html>";
                case COL_REFIT_CLASS -> refitStep.getRefitClass().toShortName();
                case COL_OLD_LOC -> refitStep.getOldLocName();
                case COL_NEW_LOC -> refitStep.getNewLocName();
                case COL_OLD_QUANTITY -> {
                        if(!refitStep.getOldPartName().isEmpty()) {
                            yield refitStep.getOldQuantity() != 1 ? refitStep.getOldQuantity() : "";
                        } else {
                            yield "";
                        }}
                case COL_NEW_QUANTITY -> {
                        if(!refitStep.getNewPartName().isEmpty()) {
                            yield refitStep.getNewQuantity() != 1 ? refitStep.getNewQuantity() : "";
                        } else {
                            yield "";
                        }}
                case COL_BASETIME -> makeRefitTimeDisplay(refitStep.getBaseTime());
                case COL_REFITSTEP_TYPE -> refitStep.getType().toName();
                case COL_NOTES -> (null != refitStep.getNotes() && !refitStep.getNotes().isBlank()) ? "!!!" : "";
                default -> "?";
            };
        }

        /*
        public Part getPartAt(int row) {
            return ((Part) data.get(row));
        }
        */

        public int getColumnWidth(int c) {
            return switch (c) {
                case COL_OLD_NAME, COL_NEW_NAME -> 180;
                case COL_REFITSTEP_TYPE -> 60;
                case COL_OLD_LOC, COL_NEW_LOC -> 60;
                case COL_OLD_QUANTITY, COL_NEW_QUANTITY -> 20;
                case COL_BASETIME, COL_REFIT_CLASS -> 20;
                case COL_NOTES -> 20;
                default -> 3;
            };
        }

        public int getAlignment(int col) {
            return switch (col) {
                case COL_BASETIME -> SwingConstants.RIGHT;
                case COL_REFIT_CLASS, COL_OLD_QUANTITY, COL_NEW_QUANTITY, COL_NOTES -> SwingConstants.CENTER;
                default -> SwingConstants.LEFT;
            };
        }

        public String getTooltip(int row, int col) {
            RefitStep step;
            if (data.isEmpty()) {
                return "";
            } else {
                step = data.get(row);
            }
            return switch (col) {
                case COL_NOTES -> step.getNotes();
                default -> null;
            };
        }

        public Renderer getRenderer() {
            return new Renderer();
        }

        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                int actualCol = table.convertColumnIndexToModel(column);
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                RefitStep thisStep = (RefitStep) data.get(actualRow);

                if (!isSelected) {
                    if (thisStep.getType() == RefitStepType.LEAVE) {
                        setForeground(MekHQ.getMHQOptions().getDeployedForeground());
                        setBackground(MekHQ.getMHQOptions().getDeployedBackground());
                    } else if (thisStep.getType() == RefitStepType.ERROR) {
                        setForeground(MekHQ.getMHQOptions().getHealedInjuriesForeground());
                        setBackground(MekHQ.getMHQOptions().getHealedInjuriesBackground());
                    } else {
                        setBackground(UIManager.getColor("Table.background"));
                        setForeground(UIManager.getColor("Table.foreground"));
                    }
                }
                return this;
            }
        }
    }

    
    // region Comparators
    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * 
     * @author Jay Lawson
     */
    public static class FormattedNumberSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
            // lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            int l0 = 0;
            try {
                l0 = format.parse(s0).intValue();
            } catch (ParseException e) {
                logger.error("", e);
            }
            int l1 = 0;
            try {
                l1 = format.parse(s1).intValue();
            } catch (ParseException e) {
                logger.error("", e);
            }
            return ((Comparable<Integer>) l0).compareTo(l1);
        }
    }

    /**
     * A comparator for refit classes
     * 
     * @author Jay Lawson
     */
    public static class ClassSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
        //     int r0 = Refit.NO_CHANGE;
        //     int r1 = Refit.NO_CHANGE;
        //     if (s0.contains("Omni")) {
        //         r0 = Refit.CLASS_OMNI;
        //     } else if (s0.contains("Class A")) {
        //         r0 = Refit.CLASS_A;
        //     } else if (s0.contains("Class B")) {
        //         r0 = Refit.CLASS_B;
        //     } else if (s0.contains("Class C")) {
        //         r0 = Refit.CLASS_C;
        //     } else if (s0.contains("Class D")) {
        //         r0 = Refit.CLASS_D;
        //     } else if (s0.contains("Class E")) {
        //         r0 = Refit.CLASS_E;
        //     } else if (s0.contains("Class F")) {
        //         r0 = Refit.CLASS_F;
        //     }

        //     if (s1.contains("Omni")) {
        //         r1 = Refit.CLASS_OMNI;
        //     } else if (s1.contains("Class A")) {
        //         r1 = Refit.CLASS_A;
        //     } else if (s1.contains("Class B")) {
        //         r1 = Refit.CLASS_B;
        //     } else if (s1.contains("Class C")) {
        //         r1 = Refit.CLASS_C;
        //     } else if (s1.contains("Class D")) {
        //         r1 = Refit.CLASS_D;
        //     } else if (s1.contains("Class E")) {
        //         r1 = Refit.CLASS_E;
        //     } else if (s1.contains("Class F")) {
        //         r1 = Refit.CLASS_F;
        //     }
        //     return ((Comparable<Integer>) r0).compareTo(r1);
            return 0;
        }
    }

    public static String makeRefitTimeDisplay(int minutes) {
        if (minutes == 0) {
            return "";
        } else if (minutes <= 480) {
            return String.format("%dm", minutes);
        // } else if (minutes < 480) {
        //     return String.format("%.2fh", minutes/60.0);
        } else {
            return String.format("%.2fd", minutes/480.0);
        }
    }
}
