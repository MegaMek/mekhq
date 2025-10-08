/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * A table model for displaying parts
 */
public class PartsTableModel extends DataTableModel<Part> {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PartsTableModel";

    public final static int COL_QUANTITY = 0;
    public final static int COL_COL_IN_USE = 1;
    public final static int COL_NAME = 2;
    public final static int COL_DETAIL = 3;
    public final static int COL_TECH_BASE = 4;
    public final static int COL_QUALITY = 5;
    public final static int COL_STATUS = 6;
    public final static int COL_REPAIR = 7;
    public final static int COL_COST = 8;
    public final static int COL_TOTAL_COST = 9;
    public final static int COL_TON = 10;
    public final static int N_COL = 11;

    Map<Part, Integer> partsUseData = new HashMap<>();


    /**
     * Default constructor that initializes the table model with no predefined part data. Creates an empty data source
     * for parts.
     *
     * <p><b>Usage:</b> This constructor was predominantly created for {@link mekhq.gui.dialog.MRMSDialog}, for most
     * other use-cases you probably want the full constructor.</p>
     */
    public PartsTableModel() {
        new PartsTableModel(null);
    }

    /**
     * Constructs a table model with a predefined set of parts in use. This constructor maps each part in the provided
     * set to its respective usage count, storing this information for later access or manipulation.
     *
     * @param partsInUse a {@link Set} of {@link PartInUse} objects. Each object represents a part being used and
     *                   contains metadata about the part and its usage count. If {@code null}, the table model is
     *                   initialized with an empty data source.
     */
    public PartsTableModel(@Nullable Set<PartInUse> partsInUse) {
        data = new ArrayList<>();

        if (partsInUse != null) {
            for (PartInUse partInUse : partsInUse) {
                IAcquisitionWork description = partInUse.getPartToBuy();
                Part acquisitionPart = description.getAcquisitionPart();

                partsUseData.put(acquisitionPart, partInUse.getUseCount());
            }
        }
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
            case COL_COL_IN_USE -> getFormattedTextAt(RESOURCE_BUNDLE, "label.COL_IN_USE");
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
            part = data.get(row);
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
        if (col == COL_COL_IN_USE) {
            int useCount = 0;

            for (Part comparisonPart : partsUseData.keySet()) {
                if (comparisonPart.isSamePartType(part)) {
                    useCount = partsUseData.get(comparisonPart);
                    break;
                }
            }

            return useCount;
        }
        if (col == COL_QUALITY) {
            String append;

            if (part.isBrandNew()) {
                append = getFormattedTextAt(RESOURCE_BUNDLE, "addendum.brandNew");
            } else {
                append = getFormattedTextAt(RESOURCE_BUNDLE, "addendum.used");
            }
            return part.getQualityName() + " (" + append + ')';
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
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        return switch (c) {
            case COL_NAME, COL_DETAIL -> 120;
            case COL_REPAIR -> 140;
            case COL_STATUS -> 40;
            case COL_TECH_BASE, COL_COST, COL_TOTAL_COST -> 20;
            default -> 3;
        };
    }

    public int getAlignment(int col) {
        return switch (col) {
            case COL_QUALITY -> SwingConstants.CENTER;
            case COL_COST, COL_TOTAL_COST, COL_TON -> SwingConstants.RIGHT;
            default -> SwingConstants.LEFT;
        };
    }

    public @Nullable String getTooltip(int row, int col) {
        return null;
    }

    public PartsTableModel.Renderer getRenderer() {
        return new PartsTableModel.Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
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
