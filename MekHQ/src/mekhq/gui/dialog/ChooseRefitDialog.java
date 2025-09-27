/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.StringUtility;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

/**
 * @author Taharqa
 */
public class ChooseRefitDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(ChooseRefitDialog.class);

    // region Variable Declarations
    private final Campaign campaign;
    private final Unit unit;
    private RefitTableModel refitModel;

    private JButton btnRefit;
    private JButton btnCustomize;
    private JTable refitTable;
    private JScrollPane scrShoppingList;
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
        JScrollPane scrRefitTable = new JScrollPaneWithSpeed();
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

        scrShoppingList = new JScrollPaneWithSpeed();
        scrShoppingList.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "shoppingList.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
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

        JTextPane txtOldUnit = new JTextPane();
        txtOldUnit.setEditable(false);
        txtOldUnit.setContentType("text/html");
        txtOldUnit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "txtOldUnit.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        EntityReadout mv = EntityReadout.createReadout(unit.getEntity(), false, true, true);
        txtOldUnit.setText("<div style='font: 12pt monospaced'>" + mv.getFullReadout() + "</div>");
        scrOldUnit = new JScrollPaneWithSpeed(txtOldUnit);
        scrOldUnit.setMinimumSize(new Dimension(300, 400));
        scrOldUnit.setPreferredSize(new Dimension(300, 400));
        SwingUtilities.invokeLater(() -> scrOldUnit.getVerticalScrollBar().setValue(0));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        getContentPane().add(scrOldUnit, gridBagConstraints);

        txtNewUnit = new JTextPane();
        txtNewUnit.setEditable(false);
        txtNewUnit.setContentType("text/html");
        txtNewUnit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(resourceMap.getString(
              "txtNewUnit.title")), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrNewUnit = new JScrollPaneWithSpeed(txtNewUnit);
        scrNewUnit.setMinimumSize(new Dimension(300, 400));
        scrNewUnit.setPreferredSize(new Dimension(300, 400));
        gridBagConstraints.gridx = 2;
        getContentPane().add(scrNewUnit, gridBagConstraints);

        JPanel panBtn = new JPanel(new GridBagLayout());

        btnRefit = new JButton(resourceMap.getString("btnRefit.text"));
        btnRefit.setEnabled(false);
        btnRefit.addActionListener(evt -> confirmRefit());
        panBtn.add(btnRefit, new GridBagConstraints());

        btnCustomize = new JButton(resourceMap.getString("btnCustomize.text"));
        btnCustomize.setEnabled(false);
        btnCustomize.addActionListener(evt -> confirmCustomize());
        panBtn.add(btnCustomize, new GridBagConstraints());

        JButton btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(evt -> cancel());
        panBtn.add(btnClose, new GridBagConstraints());

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

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
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

    private void confirmRefit() {
        confirmed = getSelectedRefit() != null;
        customize = false;
        setVisible(false);
    }

    private void confirmCustomize() {
        confirmed = getSelectedRefit() != null;
        customize = true;
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
            btnRefit.setEnabled(false);
            btnCustomize.setEnabled(false);
            return;
        }

        btnRefit.setEnabled(!campaign.getCampaignOptions().isAllowCanonRefitOnly() || r.getNewEntity().isCanon());
        btnCustomize.setEnabled(true);

        JList<String> lstShopping = new JList<>(r.getShoppingListDescription());
        scrShoppingList.setViewportView(lstShopping);
        EntityReadout mv = EntityReadout.createReadout(r.getNewEntity(), false, true);
        txtNewUnit.setText("<div style='font: 12pt monospaced'>" + mv.getFullReadout() + "</div>");
        SwingUtilities.invokeLater(() -> scrNewUnit.getVerticalScrollBar().setValue(0));
    }

    private void populateRefits() {
        List<Refit> refits = new ArrayList<>();
        Entity e = unit.getEntity();
        String chassis = e.getFullChassis();
        for (String model : Utilities.getAllVariants(e, campaign)) {
            model = StringUtility.isNullOrBlank(model) ? "" : " " + model;
            try {
                MekSummary summary = Utilities.retrieveUnit(chassis + model);
                Entity refitEn = new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                if (null != refitEn) {
                    Refit r = new Refit(unit, refitEn, false, false, false);
                    if (null == r.checkFixable()) {
                        refits.add(r);
                    }
                }
            } catch (EntityLoadingException ex) {
                logger.error("", ex);
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
        public final static int COL_NUM_PART = 4;
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
            return switch (column) {
                case COL_MODEL -> "Model";
                case COL_CLASS -> "Class";
                case COL_BV -> "BV";
                case COL_TIME -> "Time";
                case COL_NUM_PART -> "# Parts";
                case COL_COST -> "Cost";
                case COL_TARGET -> "Kit TN";
                default -> "?";
            };
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
            } else if (col == COL_NUM_PART) {
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
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Refit getRefitAt(int row) {
            return data.get(row);
        }

        public int getColumnWidth(int c) {
            return switch (c) {
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

        public String getTooltip(int row, int col) {
            Refit r;
            if (data.isEmpty()) {
                return "";
            } else {
                r = data.get(row);
            }

            if (col == COL_TARGET) {
                return campaign.getTargetForAcquisition(r).getDesc();
            }

            return null;
        }

        // fill table with values
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
     *
     * @author Jay Lawson
     */
    public static class FormattedNumberSorter implements Comparator<String> {
        @Override
        public int compare(String s0, String s1) {
            // let's find the weight class integer for each name
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
