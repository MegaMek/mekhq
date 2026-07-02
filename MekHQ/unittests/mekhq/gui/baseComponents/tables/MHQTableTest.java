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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.junit.jupiter.api.Test;

class MHQTableTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testConstructor() {
        MHQTableColumn column = mock(MHQTableColumn.class);
        when(column.getIndex()).thenReturn(0);
        MHQTableModel tableModel = new MHQTableModel(List.of(column)) {
            @Override protected TableCellRenderer getRenderer() { return null; }
            @Override protected Object getCellValue(Object data, MHQTableColumn column) { return null; }
        };
        tableModel.setDefaultSortOrder(Collections.singletonMap(column, SortOrder.ASCENDING));

        MHQTable table = new MHQTable(tableModel);

        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertEquals(0, table.getIntercellSpacing().width);
        assertEquals(0, table.getIntercellSpacing().height);
        assertNotNull(table.getRowSorter());
        assertNotNull(table.getColumnModel());
        assertEquals(tableModel, table.getModel());
        assertNotNull(table.getTableHeader());
        assertEquals(table.getColumnModel(), table.getTableHeader().getColumnModel());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testSetView() {
        MHQTableColumn column = mock(MHQTableColumn.class);
        when(column.getPreferredWidth()).thenReturn(150);
        MHQTableModel tableModel = new MHQTableModel(List.of(column)) {
            @Override protected TableCellRenderer getRenderer() { return null; }
            @Override protected Object getCellValue(Object data, MHQTableColumn column) { return null; }
        };

        MHQTable table = new MHQTable(tableModel);
        table.setView(Set.of(column));

        verify(column).getPreferredWidth();
        assertEquals(150, table.getColumnModel().getColumnByModelIndex(0).getPreferredWidth());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testSetRowFilter() {
        MHQTableModel tableModel = new MHQTableModel(Collections.emptyList()) {
            @Override protected TableCellRenderer getRenderer() { return null; }
            @Override protected Object getCellValue(Object data, MHQTableColumn column) { return null; }
        };
        RowFilter rowFilter = mock(RowFilter.class);

        MHQTable table = new MHQTable(tableModel);
        table.setRowFilter(rowFilter);

        TableRowSorter sorter = (TableRowSorter) table.getRowSorter();
        assertEquals(rowFilter, sorter.getRowFilter());
    }

    @Test
    void testRenderer() {
        MHQTableColumn column = mock(MHQTableColumn.class);
        when(column.getText("RawValue")).thenReturn("FormattedText");
        when(column.getAlignment()).thenReturn(0);
        MHQTableModel<Object, MHQTableColumn> tableModel = new MHQTableModel<>(List.of(column)) {
            @Override protected TableCellRenderer getRenderer() { return null; }
            @Override protected Object getCellValue(Object data, MHQTableColumn col) { return null; }
        };
        MHQTableModel<Object, MHQTableColumn>.Renderer renderer = tableModel.new Renderer();
        JTable table = mock(JTable.class);
        when(table.convertColumnIndexToModel(0)).thenReturn(0);

        Component component = renderer.getTableCellRendererComponent(table, "RawValue", false, false, 0, 0);

        verify(column).getText("RawValue");
        assertInstanceOf(JLabel.class, component);
        assertEquals("FormattedText", ((JLabel) component).getText());
    }

    @Test
    void testGetValueAt() {
        MHQTableColumn column = mock(MHQTableColumn.class);
        Object validRow = new Object();
        MHQTableModel<Object, MHQTableColumn> tableModel = new MHQTableModel<>(List.of(column)) {
            @Override protected TableCellRenderer getRenderer() { return null; }
            @Override
            protected Object getCellValue(Object data, MHQTableColumn col) {
                return (data == validRow && col == column) ? "ExpectedValue" : "Unexpected";
            }
        };
        tableModel.setData(Arrays.asList(validRow, null));

        Object validResult = tableModel.getValueAt(0, 0);
        Object nullResult = tableModel.getValueAt(1, 0);

        assertEquals("ExpectedValue", validResult);
        assertEquals("?", nullResult);
    }

}
