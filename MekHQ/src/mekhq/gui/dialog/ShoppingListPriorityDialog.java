/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * Dialog for reviewing the campaign shopping list and changing procurement priority.
 *
 * <p>Priority is represented by row order: items nearer the top are attempted first.</p>
 */
public class ShoppingListPriorityDialog extends JDialog {
    private final ShoppingList shoppingList;
    private final ShoppingListTableModel tableModel;
    private final JTable shoppingTable;

    private static final int DIALOG_WIDTH = UIUtil.scaleForGUI(800);
    private static final int DIALOG_HEIGHT = UIUtil.scaleForGUI(400);

    private static final MMLogger LOGGER = MMLogger.create(ShoppingListPriorityDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ShoppingListPriorityDialog";

    public ShoppingListPriorityDialog(Frame owner, ShoppingList shoppingList) {
        super(owner);

        this.shoppingList = shoppingList;
        this.tableModel = new ShoppingListTableModel(shoppingList);
        this.shoppingTable = new JTable(tableModel);

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        shoppingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shoppingTable.setAutoCreateRowSorter(false);
        shoppingTable.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(shoppingTable), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setTitle(getTextAt(RESOURCE_BUNDLE, "shoppingListPriorityDialog.title"));
        setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

        setResizable(true);
        setModal(true);
        setPreferences(); // Must be before setVisible
        setLocationRelativeTo(getOwner());

        setVisible(true); // Should always be last
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton moveTopButton = new JButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveToTop"));
        JButton moveUpButton = new JButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveUp"));
        JButton moveDownButton = new JButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveDown"));
        JButton moveBottomButton = new JButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveToBottom"));
        JButton closeButton = new JButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.close"));

        moveTopButton.addActionListener(evt -> moveSelectedItemToTop());
        moveUpButton.addActionListener(evt -> moveSelectedItemUp());
        moveDownButton.addActionListener(evt -> moveSelectedItemDown());
        moveBottomButton.addActionListener(evt -> moveSelectedItemToBottom());
        closeButton.addActionListener(evt -> {
            dispose();
        });

        buttonPanel.add(moveTopButton);
        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);
        buttonPanel.add(moveBottomButton);
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    private void moveSelectedItemToTop() {
        int selectedRow = getSelectedModelRow();
        if (selectedRow < 0) {
            return;
        }

        if (shoppingList.moveItemToTop(selectedRow)) {
            tableModel.fireTableDataChanged();
            selectRow(0);
        }
    }

    private void moveSelectedItemUp() {
        int selectedRow = getSelectedModelRow();
        if (selectedRow < 0) {
            return;
        }

        if (shoppingList.moveItemUp(selectedRow)) {
            tableModel.fireTableDataChanged();
            selectRow(selectedRow - 1);
        }
    }

    private void moveSelectedItemDown() {
        int selectedRow = getSelectedModelRow();
        if (selectedRow < 0) {
            return;
        }

        if (shoppingList.moveItemDown(selectedRow)) {
            tableModel.fireTableDataChanged();
            selectRow(selectedRow + 1);
        }
    }

    private void moveSelectedItemToBottom() {
        int selectedRow = getSelectedModelRow();
        if (selectedRow < 0) {
            return;
        }

        int bottomRow = shoppingList.getShoppingList().size() - 1;
        if (shoppingList.moveItemToBottom(selectedRow)) {
            tableModel.fireTableDataChanged();
            selectRow(bottomRow);
        }
    }

    private int getSelectedModelRow() {
        int selectedViewRow = shoppingTable.getSelectedRow();
        if (selectedViewRow < 0) {
            return -1;
        }

        return shoppingTable.convertRowIndexToModel(selectedViewRow);
    }

    private void selectRow(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return;
        }

        int viewRow = shoppingTable.convertRowIndexToView(modelRow);
        shoppingTable.setRowSelectionInterval(viewRow, viewRow);
        shoppingTable.scrollRectToVisible(shoppingTable.getCellRect(viewRow, 0, true));
    }

    private static class ShoppingListTableModel extends AbstractTableModel {
        private static final int COL_PRIORITY = 0;
        private static final int COL_ITEM = 1;
        private static final int COL_QUANTITY = 2;
        private static final int COL_DAYS_TO_WAIT = 3;
        private static final int COL_COST = 4;
        private static final int COL_COUNT = 5;

        private final ShoppingList shoppingList;

        private ShoppingListTableModel(ShoppingList shoppingList) {
            this.shoppingList = shoppingList;
        }

        @Override
        public int getRowCount() {
            return shoppingList.getShoppingList().size();
        }

        @Override
        public int getColumnCount() {
            return COL_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_PRIORITY -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.priority");
                case COL_ITEM -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.item");
                case COL_QUANTITY -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.quantity");
                case COL_DAYS_TO_WAIT -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.daysToWait");
                case COL_COST -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.totalCost");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            List<IAcquisitionWork> items = shoppingList.getShoppingList();
            IAcquisitionWork item = items.get(rowIndex);

            return switch (columnIndex) {
                case COL_PRIORITY -> rowIndex + 1;
                case COL_ITEM -> item.getAcquisitionName();
                case COL_QUANTITY -> item.getQuantity();
                case COL_DAYS_TO_WAIT -> item.getDaysToWait();
                case COL_COST -> item.getTotalBuyCost().toAmountAndSymbolString();
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case COL_PRIORITY, COL_QUANTITY, COL_DAYS_TO_WAIT -> Integer.class;
                default -> String.class;
            };
        }
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ShoppingListPriorityDialog.class);
            this.setName("ShoppingListPriorityDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }
}
