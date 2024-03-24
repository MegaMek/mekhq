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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Taharqa
 */
public class ChooseRefitDialog extends JDialog {
    //region Variable Declarations
    private Campaign campaign;
    private Unit unit;
    private RefitTableModel refitModel;

    private JButton btnClose;
    private JButton btnOK;
    private JTable refitTable;
    private JScrollPane scrRefitTable;
    private JList<String> lstShopping;
    private JScrollPane scrShoppingList;
    private JTextPane txtOldUnit;
    private JTextPane txtNewUnit;
    private JScrollPane scrOldUnit;
    private JScrollPane scrNewUnit;

    private boolean confirmed = false;
    //endregion Variable Declarations

    //region Constructors
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
    //endregion Constructors

    //region Initialization
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ChooseRefitDialog",
                MekHQ.getMHQOptions().getLocale());

        setTitle(resourceMap.getString("title.text") + " " + unit.getName());

        getContentPane().setLayout(new GridBagLayout());

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
        scrRefitTable = new JScrollPane();
        scrRefitTable.setViewportView(refitTable);
        scrRefitTable.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("refitTable.title")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(scrRefitTable, gridBagConstraints);

        scrShoppingList = new JScrollPane();
        scrShoppingList.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder(resourceMap.getString("shoppingList.title")),
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        scrShoppingList.setMinimumSize(new Dimension(300, 200));
        scrShoppingList.setPreferredSize(new Dimension(300, 200));
        getContentPane().add(scrShoppingList, gridBagConstraints);

        txtOldUnit = new JTextPane();
        txtOldUnit.setEditable(false);
        txtOldUnit.setContentType("text/html");
        txtOldUnit.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder(resourceMap.getString("txtOldUnit.title")),
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        MechView mv = new MechView(unit.getEntity(), false, true, true, true);
        txtOldUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMechReadout() + "</div>");
        scrOldUnit = new JScrollPane(txtOldUnit);
        scrOldUnit.setMinimumSize(new Dimension(300, 400));
        scrOldUnit.setPreferredSize(new Dimension(300, 400));
        SwingUtilities.invokeLater(() -> scrOldUnit.getVerticalScrollBar().setValue(0));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 0.0;
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
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        scrNewUnit = new JScrollPane(txtNewUnit);
        scrNewUnit.setMinimumSize(new Dimension(300, 400));
        scrNewUnit.setPreferredSize(new Dimension(300, 400));
        gridBagConstraints.gridx = 2;
        getContentPane().add(scrNewUnit, gridBagConstraints);

        JPanel panBtn = new JPanel(new GridBagLayout());
        btnOK = new JButton(resourceMap.getString("btnOK.text"));
        btnOK.setEnabled(false);
        btnOK.addActionListener(evt -> confirm());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panBtn.add(btnOK, gridBagConstraints);

        btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(evt -> cancel());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panBtn.add(btnClose, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(panBtn, gridBagConstraints);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ChooseRefitDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }
    //endregion Initialization

    private void confirm() {
        confirmed = getSelectedRefit() != null;
        setVisible(false);
    }

    private void cancel() {
        setVisible(false);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Refit getSelectedRefit() {
        int selectedRow = refitTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return refitModel.getRefitAt(refitTable.convertRowIndexToModel(selectedRow));
    }

    private void refitTableValueChanged() {
        Refit r = getSelectedRefit();
        if (null == r) {
            scrShoppingList.setViewportView(null);
            txtNewUnit.setText("");
            btnOK.setEnabled(false);
            return;
        }
        btnOK.setEnabled(true);
        lstShopping = new JList<>(r.getShoppingListDescription());
        scrShoppingList.setViewportView(lstShopping);
        MechView mv = new MechView(r.getNewEntity(), false, true);
        txtNewUnit.setText("<div style='font: 12pt monospaced'>" + mv.getMechReadout() + "</div>");
        SwingUtilities.invokeLater(() -> scrNewUnit.getVerticalScrollBar().setValue(0));
    }


    private void populateRefits() {
        List<Refit> refits = new ArrayList<>();
        Entity e = unit.getEntity();
        for (String model : Utilities.getAllVariants(e, campaign)) {
            MechSummary summary = MechSummaryCache.getInstance().getMech(e.getFullChassis() + " " + model);
            if (null == summary) {
                // Attempt to deal with new naming scheme directly
                summary = MechSummaryCache.getInstance().getMech(e.getChassis() + " (" + e.getClanChassisName() + ") " + model);
                if (null == summary) {
                    continue;
                }
            }
            try {
                Entity refitEn = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                if (null != refitEn) {
                    Refit r = new Refit(unit, refitEn, false, false);
                    if (null == r.checkFixable()) {
                        refits.add(r);
                    }
                }
            } catch (EntityLoadingException ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        refitModel = new RefitTableModel(refits);
    }

    /**
     * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
     */
    public class RefitTableModel extends AbstractTableModel {
        protected String[] columnNames;
        protected List<Refit> data;

        public final static int COL_MODEL = 0;
        public final static int COL_CLASS = 1;
        public final static int COL_BV = 2;
        public final static int COL_TIME = 3;
        public final static int COL_NPART = 4;
        public final static int COL_TARGET = 5;
        public final static int COL_COST = 6;
        public final static int N_COL = 7;

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
                case COL_NPART:
                    return "# Parts";
                case COL_COST:
                    return "Cost";
                case COL_TARGET:
                    return "Kit TN";
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
            } else if (col == COL_NPART) {
                return r.getShoppingList().size();
            } else if (col == COL_COST) {
                return r.getCost().toAmountAndSymbolString();
            } else if (col == COL_TARGET) {
                return campaign.getTargetForAcquisition(r).getValueAsString();
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
                case COL_TARGET:
                    return campaign.getTargetForAcquisition(r).getDesc();
                default:
                    return null;
            }
        }

        //fill table with values
        public void setData(ArrayList<Refit> refits) {
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

    /**
     * A comparator for numbers that have been formatted with DecimalFormat
     * @author Jay Lawson
     */
    public static class FormattedNumberSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
            //lets find the weight class integer for each name
            DecimalFormat format = new DecimalFormat();
            int l0 = 0;
            try {
                l0 = format.parse(s0).intValue();
            } catch (ParseException e) {
                LogManager.getLogger().error("", e);
            }
            int l1 = 0;
            try {
                l1 = format.parse(s1).intValue();
            } catch (ParseException e) {
                LogManager.getLogger().error("", e);
            }
            return ((Comparable<Integer>) l0).compareTo(l1);
        }
    }

    /**
     * A comparator for refit classes
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
