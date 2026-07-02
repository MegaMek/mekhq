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
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.ui.models.XTableColumnModel;


/**
 * A specialized extension of {@link JTable} designed to work with {@link MHQTableModel} and
 * {@link MHQTableColumn}. It automatically handles common UI boilerplate for MekHQ tables, such as:
 * <ul>
 *   <li>Managing column visibility via {@link XTableColumnModel}.</li>
 *   <li>Setting up row sorting and applying default sort orders based on the model.</li>
 *   <li>Providing smart tooltips on column headers that only appear if the header text is truncated.</li>
 *   <li>Applying default UI settings.</li>
 * </ul>
 *
 * @param <DataModel>   The type of the object representing a single row of data
 * @param <ColumnModel> The type of the column definition, which must extend {@link MHQTableColumn}
 * @param <TableModel>  The specific {@link MHQTableModel} implementation used by this table
 *
 * @author Hokk
 * @since 0.51.01
 */
public class MHQTable<DataModel,
                      ColumnModel extends MHQTableColumn,
                      TableModel extends MHQTableModel<DataModel, ColumnModel>
                     > extends JTable {

    private final TableRowSorter<TableModel> sorter;

    /**
     * Constructs a new MHQTable using the provided model. Initializes default visual settings, attaches
     * custom cell renderers, and configures the row sorter based on the model's properties.
     *
     * @param tableModel The underlying table model containing the data and column definitions
     */
    public MHQTable(TableModel tableModel) {
        super(tableModel, new XTableColumnModel());
        createDefaultColumnsFromModel();
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        sorter = new TableRowSorter<>(tableModel);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();

        Map<ColumnModel, SortOrder> defaultSortOrder = getModel().getDefaultSortOrder();
        for (ColumnModel column : tableModel.getAllColumns()) {
            TableColumn tableColumn = getColumnModel().getColumnByModelIndex(column.getIndex());
            tableColumn.setCellRenderer(tableModel.getRenderer());

            Comparator<?> comparator = column.getComparator();
            sorter.setComparator(column.getIndex(), comparator);
            SortOrder sortOrder = defaultSortOrder.get(column);
            if (sortOrder != null) {
                sortKeys.add(new RowSorter.SortKey(column.getIndex(), sortOrder));
            }
        }

        sorter.setSortKeys(sortKeys);
        sorter.setSortsOnUpdates(true);
        setRowSorter(sorter);
    }

    @Override
    public XTableColumnModel getColumnModel() {
        return (XTableColumnModel) super.getColumnModel();
    }

    @Override
    public TableModel getModel() {
        return (TableModel) super.getModel();
    }

    /**
     * Updates the visibility and preferred widths of the table's columns.
     *
     * @param visibleColumns A set containing the {@code ColumnModel} definitions that should be visible
     */
    public void setView(Set<ColumnModel> visibleColumns) {
        XTableColumnModel columnModel = getColumnModel();
        // replace the model with a dummy to suspend UI repaints
        setColumnModel(new DefaultTableColumnModel());

        for (ColumnModel column : getModel().getAllColumns()) {
            TableColumn tableColumn = columnModel.getColumnByModelIndex(column.getIndex());
            Integer width = column.getPreferredWidth();
            if (width != null) {
                tableColumn.setPreferredWidth(width);
            }
            columnModel.setColumnVisible(tableColumn, visibleColumns.contains(column));
        }
        // reattach the updated model
        setColumnModel(columnModel);
    }

    /**
     * Creates and returns a custom table header. The overridden header logic provides a tooltip that displays the
     * full column name only if the column's current width is too small to display the entire header text.
     *
     * @return A customized {@link JTableHeader}
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int index = columnModel.getColumnIndexAtX(event.getPoint().x);
                if (index == -1) {
                    return super.getToolTipText(event);
                }

                TableColumn column = columnModel.getColumn(index);
                Object headerValue = column.getHeaderValue();
                if (headerValue == null) {
                    // no header
                    return super.getToolTipText(event);
                }

                TableCellRenderer renderer = column.getHeaderRenderer();
                if (renderer == null) {
                    renderer = getDefaultRenderer();
                }
                Component header = renderer.getTableCellRendererComponent(
                      getTable(), headerValue, false, false, -1, index);

                int preferredWidth = header.getPreferredSize().width;
                if (preferredWidth > column.getWidth()) {
                    return headerValue.toString();
                }

                return super.getToolTipText(event);
            }
        };
    }

    /**
     * Applies a row filter.
     *
     * @param rowFilter The {@link RowFilter} to apply, or null to clear the filter
     */
    public void setRowFilter(RowFilter<TableModel, Integer> rowFilter) {
        sorter.setRowFilter(rowFilter);
    }

    /**
     * Forces the row sorter to re-sort the table data. Useful when the underlying data has
     * changed in a way that affects sorting, but no table events were fired.
     */
    public void refresh() {
        sorter.sort();
    }
}
