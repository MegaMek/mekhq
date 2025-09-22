/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.market.unitMarket.UnitMarketOffer;

/**
 * Model for displaying offers on the UnitMarket
 * <p>
 * Code borrowed heavily from PersonnelTableModel
 *
 * @author Neoancient
 */
public class UnitMarketTableModel extends DataTableModel<UnitMarketOffer> {
    //region Variable Declarations
    public static final int COL_MARKET = 0;
    public static final int COL_UNIT_TYPE = 1;
    public static final int COL_WEIGHT_CLASS = 2;
    public static final int COL_UNIT = 3;
    public static final int COL_PRICE = 4;
    public static final int COL_PERCENT = 5;
    public static final int COL_DELIVERY = 6;
    public static final int COL_NUM = 7;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public UnitMarketTableModel(final List<UnitMarketOffer> offers) {
        columnNames = resources.getString("UnitMarketTableModel.columnNames").split(",");
        setData(offers);
    }
    //endregion Constructors

    public int getColumnWidth(final int column) {
        return switch (column) {
            case COL_MARKET, COL_PRICE -> 90;
            case COL_UNIT_TYPE -> 15;
            case COL_UNIT -> 175;
            case COL_WEIGHT_CLASS -> 50;
            default -> 20;
        };
    }

    public int getAlignment(final int column) {
        return switch (column) {
            case COL_PRICE, COL_PERCENT, COL_DELIVERY -> SwingConstants.RIGHT;
            case COL_MARKET, COL_UNIT_TYPE, COL_WEIGHT_CLASS, COL_UNIT -> SwingConstants.LEFT;
            default -> SwingConstants.CENTER;
        };
    }

    public Optional<UnitMarketOffer> getOffer(final int row) {
        return ((row >= 0) && (row < getData().size())) ? Optional.of((UnitMarketOffer) getData().get(row))
                     : Optional.empty();
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getData().isEmpty() ? "" : getOffer(row).map(o -> getValueFor(o, column)).orElse("?");
    }

    private Object getValueFor(final UnitMarketOffer offer, final int column) {
        return switch (column) {
            case COL_MARKET -> offer.getMarketType();
            case COL_UNIT_TYPE -> UnitType.getTypeName(offer.getUnitType());
            case COL_WEIGHT_CLASS -> EntityWeightClass.getClassName(offer.getUnit().getWeightClass(),
                  offer.getUnit().getUnitType(), offer.getUnit().isSupport());
            case COL_UNIT -> offer.getUnit().getName();
            case COL_PRICE -> offer.getPrice().toAmountAndSymbolString();
            case COL_PERCENT -> offer.getPercent() + "%";
            case COL_DELIVERY -> offer.getTransitDuration();
            default -> "?";
        };
    }

    public TableCellRenderer getRenderer() {
        return new Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
              final boolean isSelected, final boolean hasFocus,
              final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(getAlignment(table.convertColumnIndexToModel(column)));
            return this;
        }
    }
}
