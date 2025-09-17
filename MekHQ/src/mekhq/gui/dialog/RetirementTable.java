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

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import megamek.client.ui.models.XTableColumnModel;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.RetirementTableModel;

class RetirementTable extends JTable {
    private static class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        final private JSpinner spinner;

        public SpinnerEditor() {
            spinner = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setEditable(false);
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
              int column) {
            spinner.setValue(value);
            return spinner;
        }
    }

    public RetirementTable(RetirementTableModel model, CampaignGUI hqView) {
        super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        XTableColumnModel columnModel = new XTableColumnModel();
        setColumnModel(columnModel);
        createDefaultColumnsFromModel();
        TableColumn column;
        for (int i = 0; i < RetirementTableModel.N_COL; i++) {
            column = getColumnModel().getColumn(convertColumnIndexToView(i));
            column.setPreferredWidth(model.getColumnWidth(i));
            if ((i != RetirementTableModel.COL_PAY_BONUS) && (i != RetirementTableModel.COL_MISC_MOD)) {
                column.setCellRenderer(model.getRenderer(i));
            }
        }

        setRowHeight(50);
        setIntercellSpacing(new Dimension(0, 0));
        setShowGrid(false);

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_PAY_BONUS))
              .setCellEditor(new DefaultCellEditor(new JCheckBox()));

        getColumnModel().getColumn(convertColumnIndexToView(RetirementTableModel.COL_MISC_MOD))
              .setCellEditor(new SpinnerEditor());
    }

    public void setGeneralMod(int mod) {
        ((RetirementTableModel) getModel()).setGeneralMod(mod);
    }
}
