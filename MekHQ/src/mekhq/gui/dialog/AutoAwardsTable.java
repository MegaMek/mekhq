/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import megamek.client.ui.models.XTableColumnModel;
import mekhq.gui.model.AutoAwardsTableModel;

class AutoAwardsTable extends JTable {
    public AutoAwardsTable(AutoAwardsTableModel model) {
        super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel columnModel = new XTableColumnModel();
        setColumnModel(columnModel);
        createDefaultColumnsFromModel();
        TableColumn column;
        for (int columnIndex = 0; columnIndex < AutoAwardsTableModel.N_COL; columnIndex++) {
            column = getColumnModel().getColumn(convertColumnIndexToView(columnIndex));
            column.setPreferredWidth(model.getColumnWidth(columnIndex));
            if (columnIndex != AutoAwardsTableModel.COL_AWARD) {
                column.setCellRenderer(model.getRenderer(columnIndex));
            }
        }

        setRowHeight(50);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        getColumnModel().getColumn(convertColumnIndexToView(AutoAwardsTableModel.COL_AWARD))
              .setCellEditor(new DefaultCellEditor(new JCheckBox()));
    }
}
