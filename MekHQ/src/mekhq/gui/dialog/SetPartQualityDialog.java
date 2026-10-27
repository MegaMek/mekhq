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

import static megamek.client.ui.util.UIUtil.scaleForGUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import mekhq.MekHQ;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;

/**
 * A GM dialog for setting the quality of each part installed on a unit individually, rather than setting every part
 * to the same quality as the unit-wide "Set Quality..." action does. This supports the Campaign Operations rule where
 * each part tracks its own quality.
 *
 * <p>Missing parts have no quality and are not listed, matching {@link Unit#setQuality(PartQuality)}. Changes are
 * only applied when the dialog is confirmed.</p>
 */
public class SetPartQualityDialog extends AbstractButtonDialog {
    private final Unit unit;
    private final boolean reverseQualityNames;

    private PartQualityTableModel tableModel;
    private JTable partsTable;
    private JComboBox<PartQuality> cboSelectedQuality;

    /**
     * @param frame               the parent frame
     * @param unit                the unit whose parts are being edited
     * @param reverseQualityNames whether quality names are reversed, per the campaign option
     */
    public SetPartQualityDialog(final JFrame frame, final Unit unit, final boolean reverseQualityNames) {
        super(frame, true, MekHQ.getDefaultResourceBundle(), "SetPartQualityDialog", "SetPartQualityDialog.title");
        this.unit = unit;
        this.reverseQualityNames = reverseQualityNames;
        initialize();
        setTitle(String.format(resources.getString("SetPartQualityDialog.Unit.title"), unit.getName()));
    }

    @Override
    protected Container createCenterPane() {
        tableModel = new PartQualityTableModel(getEditableParts(unit));

        partsTable = new JTable(tableModel);
        partsTable.setName("partsTable");
        partsTable.setAutoCreateRowSorter(true);
        partsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        partsTable.setRowHeight(Math.max(partsTable.getRowHeight(), new JComboBox<>().getPreferredSize().height));

        JComboBox<PartQuality> cboCellQuality = createQualityComboBox();
        partsTable.getColumnModel()
              .getColumn(PartQualityTableModel.COL_QUALITY)
              .setCellEditor(new DefaultCellEditor(cboCellQuality));
        partsTable.getColumnModel()
              .getColumn(PartQualityTableModel.COL_QUALITY)
              .setCellRenderer(new DefaultTableCellRenderer() {
                  @Override
                  protected void setValue(Object value) {
                      setText((value instanceof PartQuality quality) ? quality.toName(reverseQualityNames) : "");
                  }
              });
        partsTable.getColumnModel().getColumn(PartQualityTableModel.COL_LOCATION).setPreferredWidth(scaleForGUI(120));
        partsTable.getColumnModel().getColumn(PartQualityTableModel.COL_PART).setPreferredWidth(scaleForGUI(280));
        partsTable.getColumnModel().getColumn(PartQualityTableModel.COL_QUALITY).setPreferredWidth(scaleForGUI(60));

        JScrollPane scrollPane = new JScrollPane(partsTable);
        scrollPane.setPreferredSize(scaleForGUI(500, 400));

        // Lets the GM set a block of parts (e.g. a whole location) at once
        cboSelectedQuality = createQualityComboBox();
        cboSelectedQuality.setName("cboSelectedQuality");
        cboSelectedQuality.setSelectedItem(PartQuality.QUALITY_D);

        JButton btnApplyToSelected = new JButton(resources.getString("SetPartQualityDialog.btnApplyToSelected.text"));
        btnApplyToSelected.setName("btnApplyToSelected");
        btnApplyToSelected.setToolTipText(resources.getString("SetPartQualityDialog.btnApplyToSelected.toolTipText"));
        btnApplyToSelected.addActionListener(evt -> applyToSelectedRows());

        JPanel bulkPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        bulkPanel.add(new JLabel(resources.getString("SetPartQualityDialog.lblSelectedQuality.text")));
        bulkPanel.add(cboSelectedQuality);
        bulkPanel.add(btnApplyToSelected);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("partQualityPanel");
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bulkPanel, BorderLayout.PAGE_END);
        return panel;
    }

    private JComboBox<PartQuality> createQualityComboBox() {
        JComboBox<PartQuality> comboBox = new JComboBox<>(PartQuality.values());
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                  boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PartQuality quality) {
                    setText(quality.toName(reverseQualityNames));
                }
                return this;
            }
        });
        return comboBox;
    }

    private void applyToSelectedRows() {
        if (partsTable.isEditing()) {
            partsTable.getCellEditor().stopCellEditing();
        }

        PartQuality quality = (PartQuality) cboSelectedQuality.getSelectedItem();
        if (quality == null) {
            return;
        }

        for (int viewRow : partsTable.getSelectedRows()) {
            tableModel.setValueAt(quality, partsTable.convertRowIndexToModel(viewRow),
                  PartQualityTableModel.COL_QUALITY);
        }
    }

    @Override
    protected void okAction() {
        if (partsTable.isEditing()) {
            partsTable.getCellEditor().stopCellEditing();
        }

        boolean anyChanged = false;
        for (PartQualityRow row : tableModel.getRows()) {
            if (row.part.getQuality() != row.quality) {
                row.part.setQuality(row.quality);
                MekHQ.triggerEvent(new PartChangedEvent(row.part));
                anyChanged = true;
            }
        }

        if (anyChanged) {
            MekHQ.triggerEvent(new UnitChangedEvent(unit));
        }
    }

    @Override
    protected void setPreferences() throws Exception {
        setPreferences(MekHQ.getMHQPreferences().forClass(getClass()));
    }

    /**
     * @param unit the unit to read parts from
     *
     * @return the unit's parts that carry a quality, ordered by location and then name
     */
    static List<Part> getEditableParts(Unit unit) {
        List<Part> parts = new ArrayList<>();
        for (Part part : unit.getParts()) {
            if (!(part instanceof MissingPart)) {
                parts.add(part);
            }
        }
        parts.sort(Comparator.comparing((Part part) -> getLocationLabel(unit, part))
                         .thenComparing(Part::getName, String.CASE_INSENSITIVE_ORDER));
        return parts;
    }

    private static String getLocationLabel(Unit unit, Part part) {
        String location = part.getLocationName();
        return (location == null) ? unit.getName() : location;
    }

    private static class PartQualityRow {
        private final Part part;
        private PartQuality quality;

        private PartQualityRow(Part part) {
            this.part = part;
            this.quality = part.getQuality();
        }
    }

    /**
     * Holds the pending quality for each part; nothing is written to the parts until the dialog is confirmed.
     */
    private class PartQualityTableModel extends AbstractTableModel {
        private static final int COL_LOCATION = 0;
        private static final int COL_PART = 1;
        private static final int COL_QUALITY = 2;

        private final List<PartQualityRow> rows = new ArrayList<>();

        private PartQualityTableModel(List<Part> parts) {
            for (Part part : parts) {
                rows.add(new PartQualityRow(part));
            }
        }

        private List<PartQualityRow> getRows() {
            return rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_LOCATION -> resources.getString("SetPartQualityDialog.colLocation.text");
                case COL_PART -> resources.getString("SetPartQualityDialog.colPart.text");
                case COL_QUALITY -> resources.getString("SetPartQualityDialog.colQuality.text");
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return (column == COL_QUALITY) ? PartQuality.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_QUALITY;
        }

        @Override
        public Object getValueAt(int row, int column) {
            PartQualityRow partRow = rows.get(row);
            return switch (column) {
                case COL_LOCATION -> getLocationLabel(unit, partRow.part);
                case COL_PART -> partRow.part.getName();
                case COL_QUALITY -> partRow.quality;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if ((column == COL_QUALITY) && (value instanceof PartQuality quality)) {
                rows.get(row).quality = quality;
                fireTableCellUpdated(row, column);
            }
        }
    }
}
