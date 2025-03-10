/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.gui.model;

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.Part;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * A table model for displaying parts
 */
public class PartsTableModel extends DataTableModel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + PartsTableModel.class.getSimpleName();

    public final static int COL_QUANTITY = 0;
    public final static int COL_NAME = 1;
    public final static int COL_DETAIL = 2;
    public final static int COL_TECH_BASE = 3;
    public final static int COL_QUALITY = 4;
    public final static int COL_STATUS = 5;
    public final static int COL_REPAIR = 6;
    public final static int COL_COST = 7;
    public final static int COL_TOTAL_COST = 8;
    public final static int COL_TON = 9;
    public final static int N_COL = 10;

    public PartsTableModel() {
        data = new ArrayList<Part>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_NAME");
            case COL_COST -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_COST");
            case COL_TOTAL_COST -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_TOTAL_COST");
            case COL_QUANTITY -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_QUANTITY");
            case COL_QUALITY -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_QUALITY");
            case COL_TON -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_TON");
            case COL_STATUS -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_STATUS");
            case COL_DETAIL -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_DETAIL");
            case COL_TECH_BASE -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_TECH_BASE");
            case COL_REPAIR -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_REPAIR");
            default -> "?";
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        Part part;
        if (data.isEmpty()) {
            return "";
        } else {
            part = (Part) data.get(row);
        }

        if (col == COL_NAME) {
            return "<html><nobr>" + part.getName() + "</nobr></html>";
        }
        if (col == COL_DETAIL) {
            return "<html><nobr>" + part.getDetails() + "</nobr></html>";
        }
        if (col == COL_COST) {
            return part.getActualValue().toAmountAndSymbolString();
        }
        if (col == COL_TOTAL_COST) {
            return part.getActualValue().multipliedBy(part.getQuantity()).toAmountAndSymbolString();
        }
        if (col == COL_QUANTITY) {
            return part.getQuantity();
        }
        if (col == COL_QUALITY) {
            String appendum;

            if (part.isBrandNew()) {
                appendum = getFormattedTextAt(RESOURCE_BUNDLE, "addendum.brandNew");
            } else {
                appendum = getFormattedTextAt(RESOURCE_BUNDLE, "addendum.used");
            }
            return part.getQualityName() + " (" + appendum + ')';
        }
        if (col == COL_TON) {
            return Math.round(part.getTonnage() * 100) / 100.0;
        }
        if (col == COL_STATUS) {
            return "<html><nobr>" + part.getStatus() + "</nobr></html>";
        }
        if (col == COL_TECH_BASE) {
            return part.getTechBaseName();
        }
        if (col == COL_REPAIR) {
            return "<html><nobr>" + part.getRepairDesc() + "</nobr></html>";
        }
        return "?";
    }

    public Part getPartAt(int row) {
        return ((Part) data.get(row));
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_NAME:
            case COL_DETAIL:
                return 120;
            case COL_REPAIR:
                return 140;
            case COL_STATUS:
                return 40;
            case COL_TECH_BASE:
            case COL_COST:
            case COL_TOTAL_COST:
                return 20;
            default:
                return 3;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            case COL_QUALITY:
                return SwingConstants.CENTER;
            case COL_COST:
            case COL_TOTAL_COST:
            case COL_TON:
                return SwingConstants.RIGHT;
            default:
                return SwingConstants.LEFT;
        }
    }

    public @Nullable String getTooltip(int row, int col) {
        switch (col) {
            default:
                return null;
        }
    }

    public PartsTableModel.Renderer getRenderer() {
        return new PartsTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }
    }
}
