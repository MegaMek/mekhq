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

package mekhq.gui.baseComponents.tables;

import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.annotations.Nullable;
import mekhq.utilities.ReportingUtilities;

/**
 * An abstract base class for table models within the MekHQ UI. This class maps a list of data objects
 * to table rows and a list of {@link MHQTableColumn} definitions to table columns.
 *
 * @param <DataModel>   The type of the object representing a single row of data
 * @param <ColumnModel> The type of the column definition, which must extend {@link MHQTableColumn}
 *
 * @author Hokk
 * @since 0.51.01
 */
public abstract class MHQTableModel<DataModel, ColumnModel extends MHQTableColumn> extends AbstractTableModel {


    /** The underlying data for the table, where each element represents a row.
     */
    protected List<DataModel> data = Collections.emptyList();

    /** Columns defined for the table model.
     */
    protected final List<ColumnModel> columns;

    private Map<ColumnModel, SortOrder> defaultSortOrder = new HashMap<>();

    /**
     * Constructs a new MHQTableModel with the specified columns.
     *
     * @param columns A list of column definitions for this table
     */
    public MHQTableModel(List<ColumnModel> columns) {
        this.columns = columns;
    }

    /**
     * Retrieves the custom cell renderer for this table model.
     *
     * @return The {@link TableCellRenderer} used to render the cells in this table
     */
    protected abstract TableCellRenderer getRenderer();

    /**
     * Retrieves the value to be displayed in a specific cell, determined by the intersection of the
     * provided row data and column definition.
     *
     * @param data   The data object representing the row
     * @param column The column definition representing the column
     *
     * @return The object value to be rendered in the cell
     */
    protected abstract Object getCellValue(DataModel data, ColumnModel column);

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex).toString();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DataModel row = getRow(rowIndex);
        if (row == null) {
            return "?";
        }
        ColumnModel column = columns.get(columnIndex);
        return getCellValue(row, column);
    }

    /**
     * Sets the underlying data list for this table model. Does not automatically fire a table data changed event.
     *
     * @param data The new list of row data objects
     */
    public void setData(List<DataModel> data) {
        this.data = data;
    }

    public Map<ColumnModel, SortOrder> getDefaultSortOrder() {
        return defaultSortOrder;
    }

    /**
     * Sets the default sorting order for the columns in this model.
     *
     * @param defaultSortOrder A map linking column models to their desired {@link SortOrder}
     */
    public void setDefaultSortOrder(Map<ColumnModel, SortOrder> defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }

    /**
     * Retrieves the data object for a specific row index.
     *
     * @param rowIndex The index of the row to retrieve
     *
     * @return The data object for the row, or null if the index is out of bounds
     */
    @Nullable
    public DataModel getRow(int rowIndex) {
        return ((0 <= rowIndex) && (rowIndex < data.size())) ? data.get(rowIndex) : null;
    }

    /**
     * Retrieves the list of all column definitions used by this model.
     *
     * @return A list of {@code ColumnModel} objects
     */
    public List<ColumnModel> getAllColumns() {
        return columns;
    }

    /**
     * A custom {@link DefaultTableCellRenderer}. Handles text rendering based on the {@link MHQTableColumn}
     * definitions and provides support for dynamic text highlighting (mainly intended for search support).
     */
    public class Renderer extends DefaultTableCellRenderer {

        private String highlight = "";

        public void setHighlight(String highlight) {
            this.highlight = highlight;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int rowIndex, int columnIndex) {
            if (table == null) {
                return this;
            }

            if (getFont() != table.getFont()) {
                setFont(table.getFont());
            }
            ColumnModel column = getAllColumns().get(table.convertColumnIndexToModel(columnIndex));
            String text = applyHighlighting(column.getText(value));
            setText(text);
            setHorizontalAlignment(column.getAlignment());

            return this;
        }

        /**
         * Applies HTML-based visual highlighting to the specified string.
         *
         * @param text The original cell text
         *
         * @return An HTML-formatted string with the target text highlighted
         */
        private String applyHighlighting(String text) {
            if (highlight.isEmpty()) {
                return text;
            }
            String highlightedText =
                  ReportingUtilities.messageSurroundedBySpanWithColor(ReportingUtilities.getPositiveColor(), "$1");
            text = text.replaceAll("(?i)(" + java.util.regex.Pattern.quote(highlight) + ")(?![^<>]*>)", highlightedText);
            if (text.startsWith("<html>")) {
                return text;
            }
            return "<html>" + text + "</html>";
        }

    }
}
