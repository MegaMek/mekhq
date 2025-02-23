/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.event.*;

import mekhq.MekHQ;
import mekhq.campaign.parts.PartInUse;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class PartsInUseTableModel extends DataTableModel {
    private static final DecimalFormat FORMATTER = new DecimalFormat();
    static {
        FORMATTER.setMaximumFractionDigits(3);
    }
    private static final String EMPTY_CELL = "";

    public final static int COL_PART = 0;
    public final static int COL_IN_USE = 1;
    public final static int COL_STORED = 2;
    public final static int COL_TONNAGE = 3;
    public final static int COL_REQUSTED_STOCK = 4;
    public final static int COL_IN_TRANSFER = 5;
    public final static int COL_COST = 6;
    public final static int COL_BUTTON_BUY  = 7;
    public final static int COL_BUTTON_BUY_BULK  = 8;
    public final static int COL_BUTTON_SELL = 9;
    public final static int COL_BUTTON_SELL_BULK = 10;
    public final static int COL_BUTTON_GMADD  = 11;
    public final static int COL_BUTTON_GMADD_BULK  = 12;

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.PartsInUseTableModel",
            MekHQ.getMHQOptions().getLocale());

    public PartsInUseTableModel() {
        data = new ArrayList<PartInUse>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COL_BUTTON_GMADD_BULK + 1;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_PART:
                return resourceMap.getString("part.heading");
            case COL_IN_USE:
                return resourceMap.getString("inUse.heading");
            case COL_STORED:
                return resourceMap.getString("stored.heading");
            case COL_TONNAGE:
                return resourceMap.getString("storedTonnage.heading");
            case COL_IN_TRANSFER:
                return resourceMap.getString("ordered.heading");
            case COL_COST:
                return resourceMap.getString("cost.heading");
            case COL_REQUSTED_STOCK:
                return resourceMap.getString("requestedStock.heading");
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        PartInUse partInUse = getPartInUse(row);
        switch (column) {
            case COL_PART:
                return partInUse.getDescription();
            case COL_IN_USE:
                return FORMATTER.format(partInUse.getUseCount());
            case COL_STORED:
                return (partInUse.getStoreCount() > 0) ? FORMATTER.format(partInUse.getStoreCount()) : EMPTY_CELL;
            case COL_TONNAGE:
                return (partInUse.getStoreTonnage() > 0) ? FORMATTER.format(partInUse.getStoreTonnage()) : EMPTY_CELL;
            case COL_IN_TRANSFER:
                if (partInUse.getTransferCount() > 0 && partInUse.getPlannedCount() <= 0) {
                    return FORMATTER.format(partInUse.getTransferCount());
                } else if (partInUse.getPlannedCount() > 0) {
                    return String.format("%s [+%s]",
                            FORMATTER.format(partInUse.getTransferCount()), FORMATTER.format(partInUse.getPlannedCount()));
                } else {
                    return EMPTY_CELL;
                }
            case COL_COST:
                return partInUse.getCost().toAmountAndSymbolString();
            case COL_BUTTON_BUY:
                return resourceMap.getString("buy.text");
            case COL_BUTTON_BUY_BULK:
                return resourceMap.getString("buyInBulk.text");
            case COL_BUTTON_SELL:
                return resourceMap.getString("sell.text");
            case COL_BUTTON_SELL_BULK:
                return resourceMap.getString("sellInBulk.text");
            case COL_BUTTON_GMADD:
                return resourceMap.getString("add.text");
            case COL_BUTTON_GMADD_BULK:
                return resourceMap.getString("addInBulk.text");
            case COL_REQUSTED_STOCK:
                return partInUse.getRequestedStock() + "%";
            default:
                return EMPTY_CELL;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        switch (col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_SELL:
            case COL_BUTTON_SELL_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
            case COL_REQUSTED_STOCK:
                return true;
            default:
                return false;
        }
    }

    public void setData(Set<PartInUse> data) {
        setData(new ArrayList<>(data));
    }

    @SuppressWarnings("unchecked")
    public void updateRow(int row, PartInUse partInUse) {
        ((ArrayList<PartInUse>) data).set(row, partInUse);
        fireTableRowsUpdated(row, row);
    }

    public PartInUse getPartInUse(int row) {
        if ((row < 0) || (row >= data.size())) {
            return null;
        }
        return (PartInUse) data.get(row);
    }

    public boolean isBuyable(int row) {
        return (row >= 0) && (row < data.size())
                && (null != ((PartInUse) data.get(row)).getPartToBuy());
    }

    public int getAlignment(int column) {
        switch (column) {
            case COL_PART:
                return SwingConstants.LEFT;
            case COL_IN_USE:
            case COL_STORED:
            case COL_TONNAGE:
            case COL_IN_TRANSFER:
            case COL_COST:
            case COL_REQUSTED_STOCK:
                return SwingConstants.RIGHT;
            default:
                return SwingConstants.CENTER;
        }
    }

    public int getPreferredWidth(int column) {
        return switch (column) {
            case COL_PART -> 260;
            case COL_IN_USE, COL_STORED, COL_TONNAGE, COL_IN_TRANSFER -> 15;
            case COL_COST -> 40;
            case COL_BUTTON_BUY, COL_BUTTON_SELL -> 25;
            case COL_BUTTON_GMADD -> 65;
            case COL_BUTTON_BUY_BULK, COL_BUTTON_SELL_BULK -> 65;
            case COL_REQUSTED_STOCK -> 45;
            default -> 100;
        };
    }

    public boolean hasConstantWidth(int col) {
        switch (col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_SELL:
            case COL_BUTTON_SELL_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
                return true;
            default:
                return false;
        }
    }

    public int getWidth(int col) {
        switch (col) {
            case COL_BUTTON_BUY:
            case COL_BUTTON_BUY_BULK:
            case COL_BUTTON_SELL:
            case COL_BUTTON_SELL_BULK:
            case COL_BUTTON_GMADD:
            case COL_BUTTON_GMADD_BULK:
                // Calculate from button width, respecting style
                JButton btn = new JButton(getValueAt(0, col).toString());
                return btn.getPreferredSize().width;
            default:
                return Integer.MAX_VALUE;
        }
    }

    public PartsInUseTableModel.Renderer getRenderer() {
        return new PartsInUseTableModel.Renderer();
    }

    public static class Renderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            setHorizontalAlignment(((PartsInUseTableModel) table.getModel()).getAlignment(column));
            return this;
        }
    }

    public static class ButtonColumn extends AbstractCellEditor
            implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {

        private JTable table;
        private Action action;
        private Border originalBorder;
        private Border focusBorder;

        private JButton renderButton;
        private JButton editButton;
        private Object editorValue;
        private boolean isButtonColumnEditor;
        private boolean enabled;

        public ButtonColumn(JTable table, Action action, int column) {
            this.table = table;
            this.action = action;

            renderButton = new JButton();
            editButton = new JButton();
            editButton.setFocusPainted(false);
            editButton.addActionListener(this);
            originalBorder = editButton.getBorder();
            enabled = true;

            TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(column).setCellRenderer(this);
            columnModel.getColumn(column).setCellEditor(this);
            table.addMouseListener(this);
        }

        public Border getFocusBorder() {
            return focusBorder;
        }

        public void setFocusBorder(Border focusBorder) {
            this.focusBorder = focusBorder;
            editButton.setBorder(focusBorder);
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            editButton.setEnabled(enabled);
            renderButton.setEnabled(enabled);
        }

        @Override
        public Object getCellEditorValue() {
            return editorValue;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (table.isEditing() && (this == table.getCellEditor())) {
                isButtonColumnEditor = true;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isButtonColumnEditor && table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            isButtonColumnEditor = false;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = table.convertRowIndexToModel(table.getEditingRow());
            fireEditingStopped();

            // Invoke the Action
            ActionEvent event = new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "" + row);
            action.actionPerformed(event);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            boolean buyable = ((PartsInUseTableModel) table.getModel())
                    .isBuyable(table.getRowSorter().convertRowIndexToModel(row));

            if (value == null) {
                editButton.setText(EMPTY_CELL);
                editButton.setIcon(null);
            } else if (value instanceof Icon) {
                editButton.setText(EMPTY_CELL);
                editButton.setIcon((Icon) value);
            } else {
                editButton.setText(value.toString());
                editButton.setIcon(null);
            }
            editButton.setEnabled(enabled && buyable);

            this.editorValue = value;
            return editButton;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            boolean buyable = ((PartsInUseTableModel) table.getModel())
                    .isBuyable(table.getRowSorter().convertRowIndexToModel(row));

            if (isSelected && enabled && buyable) {
                renderButton.setForeground(table.getSelectionForeground());
                renderButton.setBackground(table.getSelectionBackground());
            } else {
                renderButton.setForeground(table.getForeground());
                renderButton.setBackground(UIManager.getColor("Button.background"));
            }

            if (hasFocus && enabled && buyable) {
                renderButton.setBorder(focusBorder);
            } else {
                renderButton.setBorder(originalBorder);
            }

            if (value == null) {
                renderButton.setText(EMPTY_CELL);
                renderButton.setIcon(null);
            } else if (value instanceof Icon) {
                renderButton.setText(EMPTY_CELL);
                renderButton.setIcon((Icon) value);
            } else {
                renderButton.setText(value.toString());
                renderButton.setIcon(null);
            }
            renderButton.setEnabled(enabled && buyable);

            return renderButton;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == COL_REQUSTED_STOCK) {
            try {
                //Quick String parsing here, we ignore anything that isn't a number or a . so that a user can input a % symbol or not, it's added regardless
                double newVal = Double.parseDouble(value.toString().replaceAll("[^0-9.]", ""));
                PartInUse partInUse = getPartInUse(rowIndex);
                if (partInUse != null) {
                    partInUse.setRequestedStock(newVal);
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            } catch (NumberFormatException e) {
                
            }
        } else {
            super.setValueAt(value, rowIndex, columnIndex);
        }
    }

}
