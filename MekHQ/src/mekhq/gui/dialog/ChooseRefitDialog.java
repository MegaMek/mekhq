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
import java.util.HashMap;
import java.util.Map;

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
import megamek.common.MekSummaryCache;
import megamek.common.MekView;
import megamek.common.ViewFormatting;
import megamek.common.loaders.EntityLoadingException;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.utilities.ReportingUtilities;

/**
 * @author Taharqa
 */
public class ChooseRefitDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(ChooseRefitDialog.class);
    private ResourceBundle resourceMap;

    // region Variable Declarations
    private Campaign campaign;
    private Unit unit;
    private RefitTableModel refitModel;
    private RefitShoppingListTableModel shoppingModel;

    private List<Refit> kitRefits;
    private List<Refit> customRefits;
    private List<Refit> omniRefits;

    private JLabel noRefitsLbl;
    private JButton btnClose;
    private JButton btnGo;
    private JRadioButton radRefit;
    private JRadioButton radCustomize;
    private JRadioButton radOmni;
    private JTable refitTable;
    private JScrollPane scrRefitTable;
    private JTable shoppingTable;
    private JScrollPane scrShoppingList;
    private JTextPane txtOldUnit;
    private JTextPane txtNewUnit;
    private JScrollPane scrOldUnit;
    private JScrollPane scrNewUnit;

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

        JPanel radioPanel = new JPanel();

        radioPanel.setLayout(new GridBagLayout());

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
        getContentPane().add(radioPanel, gridBagConstraints);

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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(scrRefitTable, gridBagConstraints);

        noRefitsLbl = new JLabel(resourceMap.getString("noRefitsLbl.text"));
        noRefitsLbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(noRefitsLbl, gridBagConstraints);
        noRefitsLbl.setVisible(false);


        shoppingModel = new RefitShoppingListTableModel();
        shoppingTable = new JTable(shoppingModel);
        for (int i = 0; i < RefitShoppingListTableModel.N_COL; i++) {
            column = shoppingTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(shoppingModel.getColumnWidth(i));
        }
        shoppingTable.setIntercellSpacing(new Dimension(0, 0));
        shoppingTable.setShowGrid(false);

        scrShoppingList = new JScrollPaneWithSpeed(shoppingTable);
        scrShoppingList.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("shoppingList.title")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        scrShoppingList.setMinimumSize(UIUtil.scaleForGUI(300, 200));
        scrShoppingList.setPreferredSize(UIUtil.scaleForGUI(300, 200));
        getContentPane().add(scrShoppingList, gridBagConstraints);



        

        txtOldUnit = new JTextPane();
        txtOldUnit.setEditable(false);
        txtOldUnit.setContentType("text/html");
        txtOldUnit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtOldUnit.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        MekView mv = new MekView(unit.getEntity(), false, true, true, ViewFormatting.HTML);
        txtOldUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMekReadout() + "</div>");
        scrOldUnit = new JScrollPaneWithSpeed(txtOldUnit);
        scrOldUnit.setMinimumSize(UIUtil.scaleForGUI(300, 400));
        scrOldUnit.setPreferredSize(UIUtil.scaleForGUI(300, 400));
        SwingUtilities.invokeLater(() -> scrOldUnit.getVerticalScrollBar().setValue(0));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(scrOldUnit, gridBagConstraints);

        txtNewUnit = new JTextPane();
        txtNewUnit.setEditable(false);
        txtNewUnit.setContentType("text/html");
        txtNewUnit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtNewUnit.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrNewUnit = new JScrollPaneWithSpeed(txtNewUnit);
        scrNewUnit.setMinimumSize(UIUtil.scaleForGUI(300, 400));
        scrNewUnit.setPreferredSize(UIUtil.scaleForGUI(300, 400));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        getContentPane().add(scrNewUnit, gridBagConstraints);

        JPanel panBtn = new JPanel(new GridBagLayout());

        btnGo = new JButton(resourceMap.getString("btnGo-Refit.text"));
        btnGo.setEnabled(false);
        btnGo.addActionListener(evt -> confirm());
        panBtn.add(btnGo, new GridBagConstraints());

        btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(evt -> cancel());
        panBtn.add(btnClose, new GridBagConstraints());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
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
                scrRefitTable.setVisible(false);
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
    // endregion Initialization

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

    private void refitTableValueChanged() {
        Refit refit = getSelectedRefit();
        if (null == refit) {
            shoppingModel.setData(new ArrayList<Part>());
            txtNewUnit.setText("");
            btnGo.setEnabled(false);
            return;
        }

        Map<String,Part> shoppingList = new HashMap<String,Part>();
        for( Part newPart : refit.getShoppingList()) { 
            newPart.setUnit(null);
            if(newPart instanceof MissingPart) {
                newPart = ((MissingPart) newPart).getNewPart();
            }
            if (shoppingList.containsKey(newPart.getName() + " " + newPart.getDetails())) {
                Part oldPart = shoppingList.get(newPart.getName() + " " + newPart.getDetails());
                oldPart.setQuantity(oldPart.getQuantity() + newPart.getQuantity());
            } else {
                shoppingList.put(newPart.getName() + " " + newPart.getDetails(), newPart);
            }
        }
        
        shoppingModel.setData(new ArrayList<Part>(shoppingList.values()));

        MekView mv = new MekView(refit.getNewEntity(), false, true);
        txtNewUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMekReadout() + "</div>");
        SwingUtilities.invokeLater(() -> scrNewUnit.getVerticalScrollBar().setValue(0));
        btnGo.setEnabled(true);
    }

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
                    boolean valid = null == kitRefit.checkFixable();
                    boolean canonok = !campaign.getCampaignOptions().isAllowCanonRefitOnly() 
                            || kitRefit.getNewEntity().isCanon();
                    boolean omni = kitRefit.isOmniRefit();
                    if (valid && canonok && !omni) {
                            kitRefits.add(kitRefit);
                    }
                    
                    Refit customRefit = new Refit(unit, refitEn, true, false, false);
                    valid = null == customRefit.checkFixable();
                    omni = customRefit.isOmniRefit();
                    if (valid) {
                        if (omni) {
                            omniRefits.add(customRefit);
                        } else {
                            customRefits.add(customRefit);
                        }
                    }
                }
            } catch (EntityLoadingException ex) {
                logger.error("", ex);
            }
        }
    }

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
            switch (column) {
                case COL_MODEL:
                    return "Model";
                case COL_CLASS:
                    return "Class";
                case COL_BV:
                    return "BV";
                case COL_TIME:
                    return "Time";
                case COL_COST:
                    return "Cost";
                default:
                    return "?";
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (data.isEmpty()) {
                return "";
            }
            Refit r = data.get(row);

            if (col == COL_MODEL) {
                return r.getNewEntity().getModel();
            } else if (col == COL_CLASS) {
                return r.getRefitClassName();
            } else if (col == COL_BV) {
                return r.getNewEntity().calculateBattleValue(true, true);
            } else if (col == COL_TIME) {
                return r.getTime();
            } else if (col == COL_COST) {
                return r.getCost().toAmountAndSymbolString();
//            } else if (col == COL_TARGET) {
//                return campaign.getTargetForAcquisition(r).getValueAsString();
            } else {
                return "?";
            }
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

        public int getColumnWidth(int c) {
            switch (c) {
                case COL_MODEL:
                    return 75;
                case COL_CLASS:
                    return 110;
                case COL_COST:
                    return 40;
                default:
                    return 10;
            }
        }

        public int getAlignment(int col) {
            switch (col) {
                case COL_MODEL:
                case COL_CLASS:
                    return SwingConstants.LEFT;
                default:
                    return SwingConstants.RIGHT;
            }
        }

        public String getTooltip(int row, int col) {
            Refit r;
            if (data.isEmpty()) {
                return "";
            } else {
                r = data.get(row);
            }
            switch (col) {
                // case COL_TARGET:
                //     return campaign.getTargetForAcquisition(r).getDesc();
                default:
                    return null;
            }
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
                int actualRow = table.convertRowIndexToModel(row);
                setHorizontalAlignment(getAlignment(actualCol));
                setToolTipText(getTooltip(actualRow, actualCol));

                return this;
            }
        }
    }

    public class RefitShoppingListTableModel extends AbstractTableModel {
        public final static int COL_NAME = 0;
        public final static int COL_TECH_BASE = 1;
        public final static int COL_STOCK = 2;
        public final static int COL_TRANSIT = 3;
        public final static int COL_ORDERED = 4;
        public final static int COL_NEEDED = 5;
        public final static int COL_TARGET = 6;
        public final static int COL_TOORDER = 7;
        public final static int COL_COST = 8;
        public final static int N_COL = 9;

        private List<Part> data;

        public RefitShoppingListTableModel() {
            data = new ArrayList<Part>();
        }

        public RefitShoppingListTableModel(List<Part> parts) {
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

        public int getToOrder(Part part) {
            PartInventory inventory = campaign.getPartInventory(part);
            return Math.max(0, part.getQuantity()
                    - inventory.getSupply()
                    - inventory.getTransit()
                    - inventory.getOrdered());
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
                case COL_NEEDED -> part.getQuantity();
                case COL_TOORDER -> "<html><b>" + getToOrder(part) + "</b></html>";
                case COL_COST -> part.getActualValue().toAmountAndSymbolString();
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
    }

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
            int r0 = Refit.NO_CHANGE;
            int r1 = Refit.NO_CHANGE;
            if (s0.contains("Omni")) {
                r0 = Refit.CLASS_OMNI;
            } else if (s0.contains("Class A")) {
                r0 = Refit.CLASS_A;
            } else if (s0.contains("Class B")) {
                r0 = Refit.CLASS_B;
            } else if (s0.contains("Class C")) {
                r0 = Refit.CLASS_C;
            } else if (s0.contains("Class D")) {
                r0 = Refit.CLASS_D;
            } else if (s0.contains("Class E")) {
                r0 = Refit.CLASS_E;
            } else if (s0.contains("Class F")) {
                r0 = Refit.CLASS_F;
            }

            if (s1.contains("Omni")) {
                r1 = Refit.CLASS_OMNI;
            } else if (s1.contains("Class A")) {
                r1 = Refit.CLASS_A;
            } else if (s1.contains("Class B")) {
                r1 = Refit.CLASS_B;
            } else if (s1.contains("Class C")) {
                r1 = Refit.CLASS_C;
            } else if (s1.contains("Class D")) {
                r1 = Refit.CLASS_D;
            } else if (s1.contains("Class E")) {
                r1 = Refit.CLASS_E;
            } else if (s1.contains("Class F")) {
                r1 = Refit.CLASS_F;
            }
            return ((Comparable<Integer>) r0).compareTo(r1);
        }
    }
}
