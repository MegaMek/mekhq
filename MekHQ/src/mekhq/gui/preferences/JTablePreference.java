/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class JTablePreference extends PreferenceElement implements MouseListener {
    private final WeakReference<JTable> weakRef;
    private int columnIndex;
    private SortOrder sortOrder;

    public JTablePreference(JTable table){
        super(table.getName());

        if (table.getRowSorter().getSortKeys().size() > 0) {
            this.columnIndex = table.getRowSorter().getSortKeys().get(0).getColumn();
            this.sortOrder = table.getRowSorter().getSortKeys().get(0).getSortOrder();
        } else {
            columnIndex = 0;
            sortOrder = SortOrder.ASCENDING;
        }

        this.weakRef = new WeakReference<>(table);
        table.getTableHeader().addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTable table = this.weakRef.get();
        if (table != null) {
            int uiIndex = table.getColumnModel().getColumnIndexAtX(e.getX());
            if (uiIndex == -1) {
                return;
            }

            this.columnIndex = table.getColumnModel().getColumn(uiIndex).getModelIndex();
            for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                if (key.getColumn() == this.columnIndex) {
                    this.sortOrder = key.getSortOrder();
                    break;
                }
            }
        }
    }

    @Override
    protected String getValue() {
        return String.format("%d|%s", this.columnIndex, this.sortOrder.toString());
    }

    @Override
    protected void initialize(String value) {
        assert value != null && value.trim().length() > 0;

        JTable element = weakRef.get();
        if (element != null) {
            String[] parts = value.split("\\|", -1);

            this.columnIndex = Integer.parseInt(parts[0]);
            this.sortOrder = SortOrder.valueOf(parts[1]);

            ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(this.columnIndex, this.sortOrder));

            element.getRowSorter().setSortKeys(sortKeys);
        }
    }

    @Override
    protected void dispose() {
        JTable element = weakRef.get();
        if (element != null) {
            element.removeMouseListener(this);
            weakRef.clear();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
