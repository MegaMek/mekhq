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

import static mekhq.gui.model.ProcurementTableModel.FORMATTER;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Dialog for reviewing the campaign shopping list and adjusting procurement priority.
 *
 * <p>The dialog displays the current {@link ShoppingList} in a table and provides controls for moving the selected
 * item to the top, up one position, down one position, or to the bottom of the list. Row order shows procurement
 * priority: items nearer the top are attempted first.</p>
 *
 * <p>Changes are applied directly to the campaign's shopping list as items are moved.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class ShoppingListPriorityDialog extends JDialog {
    private final ShoppingList shoppingList;
    private final ShoppingListTableModel tableModel;
    private final JTable shoppingTable;

    private static final int DIALOG_WIDTH = UIUtil.scaleForGUI(800);
    private static final int DIALOG_HEIGHT = UIUtil.scaleForGUI(400);

    private static final MMLogger LOGGER = MMLogger.create(ShoppingListPriorityDialog.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ShoppingListPriorityDialog";

    /**
     * Creates and displays a modal shopping list priority dialog for the supplied campaign.
     *
     * @param owner    the parent frame used for modality and positioning
     * @param campaign the campaign whose shopping list is displayed and reordered
     *
     * @author Illiani
     * @since 0.51.0
     */
    public ShoppingListPriorityDialog(Frame owner, Campaign campaign) {
        super(owner);

        // needs to be static to expose it to the table
        this.shoppingList = campaign.getShoppingList();
        this.tableModel = new ShoppingListTableModel(campaign, shoppingList);
        this.shoppingTable = new JTable(tableModel);

        initialize();
    }

    /**
     * Initializes the dialog layout, table behavior, renderers, column widths, action panel, preferences, and window
     * placement.
     *
     * <p>This method also displays the dialog once initialization is complete.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
    private void initialize() {
        setLayout(new BorderLayout());

        shoppingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shoppingTable.setAutoCreateRowSorter(false);
        shoppingTable.getTableHeader().setReorderingAllowed(false);

        shoppingTable.setDefaultRenderer(Object.class, tableModel.new Renderer());
        shoppingTable.setDefaultRenderer(Integer.class, tableModel.new Renderer());

        for (int column = 0; column < shoppingTable.getColumnCount(); column++) {
            int modelColumn = shoppingTable.convertColumnIndexToModel(column);
            shoppingTable.getColumnModel()
                  .getColumn(column)
                  .setPreferredWidth(tableModel.getColumnWidth(modelColumn));
        }

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

    /**
     * Creates the panel containing the shopping list priority controls and close button.
     *
     * @return a centered button panel wired to the dialog's row movement actions
     *
     * @author Illiani
     * @since 0.51.0
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        RoundedJButton moveTopButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveToTop"));
        RoundedJButton moveUpButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveUp"));
        RoundedJButton moveDownButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveDown"));
        RoundedJButton moveBottomButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.moveToBottom"));
        RoundedJButton closeButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE,
              "shoppingListPriorityDialog.button.close"));

        moveTopButton.addActionListener(evt -> moveSelectedItemToTop());
        moveUpButton.addActionListener(evt -> moveSelectedItemUp());
        moveDownButton.addActionListener(evt -> moveSelectedItemDown());
        moveBottomButton.addActionListener(evt -> moveSelectedItemToBottom());
        closeButton.addActionListener(evt -> dispose());

        buttonPanel.add(moveTopButton);
        buttonPanel.add(moveUpButton);
        buttonPanel.add(moveDownButton);
        buttonPanel.add(moveBottomButton);
        buttonPanel.add(closeButton);

        return buttonPanel;
    }

    /**
     * Moves the currently selected shopping list item to the highest priority position.
     *
     * <p>If no row is selected, this method does nothing.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
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

    /**
     * Moves the currently selected shopping list item up by one priority position.
     *
     * <p>If no row is selected, or the selected item is already first, this method does nothing.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
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

    /**
     * Moves the currently selected shopping list item down by one priority position.
     *
     * <p>If no row is selected, or the selected item is already last, this method does nothing.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
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

    /**
     * Moves the currently selected shopping list item to the lowest priority position.
     *
     * <p>If no row is selected, this method does nothing.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
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

    /**
     * Gets the currently selected row index in model coordinates.
     *
     * @return the selected model row, or {@code -1} if no row is selected
     *
     * @author Illiani
     * @since 0.51.0
     */
    private int getSelectedModelRow() {
        int selectedViewRow = shoppingTable.getSelectedRow();
        if (selectedViewRow < 0) {
            return -1;
        }

        return shoppingTable.convertRowIndexToModel(selectedViewRow);
    }

    /**
     * Selects and scrolls to the supplied model row.
     *
     * <p>If the supplied row is outside the table model bounds, this method does nothing.</p>
     *
     * @param modelRow the row index in model coordinates to select
     *
     * @author Illiani
     * @since 0.51.0
     */
    private void selectRow(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return;
        }

        int viewRow = shoppingTable.convertRowIndexToView(modelRow);
        shoppingTable.setRowSelectionInterval(viewRow, viewRow);
        shoppingTable.scrollRectToVisible(shoppingTable.getCellRect(viewRow, 0, true));
    }

    /**
     * Table model for displaying shopping list acquisition work and calculated procurement information.
     *
     * <p>The model exposes display columns for priority, item name, item type, cost, total cost, acquisition target,
     * queued quantity, and next attempt timing.</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static class ShoppingListTableModel extends AbstractTableModel {
        private static final int COL_PRIORITY = 0;
        private static final int COL_NAME = 1;
        private static final int COL_TYPE = 2;
        private static final int COL_COST = 3;
        private static final int COL_TOTAL_COST = 4;
        private static final int COL_TARGET = 5;
        private static final int COL_QUEUE = 6;
        private static final int COL_NEXT = 7;
        private static final int N_COL = 8;

        private final Campaign campaign;
        private final ShoppingList shoppingList;

        /**
         * Creates a table model backed by the supplied shopping list.
         *
         * @param campaign     the campaign context
         * @param shoppingList the shopping list to display
         *
         * @author Illiani
         * @since 0.51.0
         */
        private ShoppingListTableModel(Campaign campaign, ShoppingList shoppingList) {
            this.campaign = campaign;
            this.shoppingList = shoppingList;
        }

        @Override
        public int getRowCount() {
            return shoppingList.getShoppingList().size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_PRIORITY -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.priority");
                case COL_NAME -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.name");
                case COL_TYPE -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.type");
                case COL_COST -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.cost");
                case COL_TOTAL_COST -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.totalCost");
                case COL_TARGET -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.target");
                case COL_QUEUE -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.queue");
                case COL_NEXT -> getTextAt(RESOURCE_BUNDLE,
                      "shoppingListPriorityDialog.column.next");
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            List<IAcquisitionWork> items = shoppingList.getShoppingList();
            IAcquisitionWork item = items.get(rowIndex);

            return switch (columnIndex) {
                case COL_NAME -> item.getAcquisitionName();
                case COL_TYPE -> switch (item) {
                    case UnitOrder ignored -> getText("Unit.text");
                    case Part ignored -> getText("Part.text");
                    default -> "?";
                };
                case COL_COST -> item.getBuyCost().toAmountAndSymbolString();
                case COL_TOTAL_COST -> item.getTotalBuyCost().toAmountAndSymbolString();
                case COL_TARGET -> {
                    final TargetRoll target = campaign.getTargetForAcquisition(item, true);

                    String value = target.getValueAsString();

                    if (IntStream.of(
                                TargetRoll.IMPOSSIBLE,
                                TargetRoll.AUTOMATIC_SUCCESS,
                                TargetRoll.AUTOMATIC_FAIL)
                              .noneMatch(i -> target.getValue() == i)) {
                        value += "+";
                    }

                    yield value;
                }
                case COL_NEXT -> {
                    final int days = item.getDaysToWait();

                    yield "%d %s".formatted(
                          days,
                          getText(days == 1 ? "Day.text" : "Days.text"));
                }
                case COL_QUEUE -> "%s [+%s]".formatted(
                      FORMATTER.format(item.getQuantity()),
                      FORMATTER.format(item.getTotalQuantity()));
                case COL_PRIORITY -> rowIndex + 1;
                default -> "?";
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == COL_PRIORITY) {
                return Integer.class;
            } else {
                return String.class;
            }
        }

        /**
         * Gets the preferred display width for the supplied model column.
         *
         * @param column the column index in model coordinates
         *
         * @return the preferred column width, in pixels
         *
         * @author Illiani
         * @since 0.51.0
         */
        public int getColumnWidth(final int column) {
            return switch (column) {
                case COL_NAME -> 200;
                case COL_COST, COL_TOTAL_COST, COL_TARGET, COL_NEXT -> 40;
                default -> 15;
            };
        }

        /**
         * Gets the horizontal alignment used when rendering cells in the supplied model column.
         *
         * @param column the column index in model coordinates
         *
         * @return one of the {@link SwingConstants} horizontal alignment constants
         *
         * @author Illiani
         * @since 0.51.0
         */
        public int getAlignment(final int column) {
            if (column == COL_NAME) {
                return SwingConstants.LEFT;
            } else {
                return SwingConstants.CENTER;
            }
        }

        /**
         * Cell renderer that applies per-column horizontal alignment for shopping list table values.
         *
         * @author Illiani
         * @since 0.51.0
         */
        public class Renderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                  final boolean isSelected,
                  final boolean hasFocus, final int row, final int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setOpaque(true);
                final int actualCol = table.convertColumnIndexToModel(column);
                setHorizontalAlignment(getAlignment(actualCol));
                return this;
            }
        }
    }

    /**
     * Registers this dialog with MekHQ window preferences.
     *
     * <p>This ensures that size and position preferences are tracked under MekHQ preferences rather than MegaMek
     * preferences.</p>
     *
     * @author Illiani
     * @since 0.51.0
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
