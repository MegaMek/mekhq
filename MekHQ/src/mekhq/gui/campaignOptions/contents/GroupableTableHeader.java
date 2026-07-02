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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions.contents;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * A {@link JTableHeader} that paints group title bands above sets of related columns, e.g. a single
 * "XP Cost per Level (0-10)" caption spanning eleven per-level columns. Add {@link ColumnGroup}s with
 * {@link #addColumnGroup(ColumnGroup)}; columns without a group keep a normal single-row header.
 *
 * <p>This is the classic Swing grouped-header pattern adapted for the Campaign Options skills table.</p>
 */
class GroupableTableHeader extends JTableHeader {
    private final List<ColumnGroup> columnGroups = new ArrayList<>();

    GroupableTableHeader(TableColumnModel model) {
        super(model);
        setReorderingAllowed(false);
        setUI(new GroupableTableHeaderUI());
    }

    void addColumnGroup(ColumnGroup group) {
        columnGroups.add(group);
    }

    /** Returns the groups (outermost first) that contain the given column, or an empty list. */
    List<ColumnGroup> getColumnGroups(TableColumn column) {
        for (ColumnGroup group : columnGroups) {
            List<ColumnGroup> found = group.getColumnGroups(column, new ArrayList<>());
            if (found != null) {
                return found;
            }
        }
        return new ArrayList<>();
    }

    /** A titled set of columns drawn as one band above the per-column headers. */
    static final class ColumnGroup {
        private final String text;
        private final List<TableColumn> columns = new ArrayList<>();

        ColumnGroup(String text) {
            this.text = text;
        }

        void add(TableColumn column) {
            columns.add(column);
        }

        List<ColumnGroup> getColumnGroups(TableColumn column, List<ColumnGroup> trail) {
            trail.add(this);
            return columns.contains(column) ? trail : null;
        }

        String getText() {
            return text;
        }

        Dimension getSize(JTable table) {
            int width = 0;
            int height = table.getTableHeader().getHeaderRect(0).height;
            for (TableColumn column : columns) {
                width += column.getWidth();
            }
            return new Dimension(width, height);
        }
    }

    /** Stacks a group band over the standard column headers and paints both. */
    private static final class GroupableTableHeaderUI extends BasicTableHeaderUI {
        @Override
        public Dimension getPreferredSize(javax.swing.JComponent component) {
            return super.getPreferredSize(component);
        }

        @Override
        public void paint(Graphics graphics, javax.swing.JComponent component) {
            GroupableTableHeader header = (GroupableTableHeader) this.header;
            int rowHeight = super.getPreferredSize(component).height;
            TableColumnModel columnModel = header.getColumnModel();
            int x = 0;
            ColumnGroup lastGroup = null;
            int groupStartX = 0;
            int groupWidth = 0;
            for (Enumeration<TableColumn> columns = columnModel.getColumns(); columns.hasMoreElements(); ) {
                TableColumn column = columns.nextElement();
                int width = column.getWidth();
                List<ColumnGroup> groups = header.getColumnGroups(column);
                ColumnGroup group = groups.isEmpty() ? null : groups.get(0);
                if (group != null && group == lastGroup) {
                    groupWidth += width;
                } else {
                    flushGroup(graphics, header, lastGroup, groupStartX, groupWidth, rowHeight);
                    lastGroup = group;
                    groupStartX = x;
                    groupWidth = width;
                }
                if (group == null) {
                    paintCell(graphics, header, 0, x, width, rowHeight, String.valueOf(column.getHeaderValue()));
                }
                x += width;
            }
            flushGroup(graphics, header, lastGroup, groupStartX, groupWidth, rowHeight);
        }

        private void flushGroup(Graphics graphics, JTableHeader header, ColumnGroup group, int x, int width,
              int rowHeight) {
            if (group != null) {
                paintCell(graphics, header, 0, x, width, rowHeight, group.getText());
            }
        }

        private void paintCell(Graphics graphics, JTableHeader header, int y, int x, int width, int height,
              String text) {
            TableCellRenderer renderer = header.getDefaultRenderer();
            Component component = renderer.getTableCellRendererComponent(header.getTable(), text, false, false, -1, 0);
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(JLabel.CENTER);
            }
            Rectangle bounds = new Rectangle(x, y, width, height);
            rendererPane.add(component);
            rendererPane.paintComponent(graphics, component, header, bounds.x, bounds.y, bounds.width, bounds.height,
                  true);
        }
    }
}
