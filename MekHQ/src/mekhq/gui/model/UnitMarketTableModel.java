/*
 * UnitMarketTableModel.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.campaign.market.unitMarket.UnitMarketOffer;

/**
 * Model for displaying offers on the UnitMarket
 *
 * Code borrowed heavily from PersonnelTableModel
 *
 * @author Neoancient
 */
public class UnitMarketTableModel extends DataTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = -6275443301484277495L;

    public static final int COL_MARKET = 0;
    public static final int COL_UNITTYPE = 1;
    public static final int COL_WEIGHTCLASS = 2;
    public static final int COL_UNIT = 3;
    public static final int COL_PRICE = 4;
    public static final int COL_PERCENT = 5;
    public static final int COL_DELIVERY = 6;
    public static final int COL_NUM = 7;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public UnitMarketTableModel(final List<UnitMarketOffer> offers) {
        columnNames = resources.getString("UnitMarketTableModel.columnNames").split(",");
        setData(offers);
    }
    //endregion Constructors

    public int getColumnWidth(final int column) {
        switch (column) {
            case COL_MARKET:
            case COL_PRICE:
                return 90;
            case COL_UNITTYPE:
                return 15;
            case COL_UNIT:
                return 175;
            case COL_WEIGHTCLASS:
                return 50;
            default:
                return 20;
        }
    }

    public int getAlignment(final int column) {
        switch (column) {
            case COL_PRICE:
            case COL_PERCENT:
            case COL_DELIVERY:
                return SwingConstants.RIGHT;
            case COL_MARKET:
            case COL_UNITTYPE:
            case COL_WEIGHTCLASS:
            case COL_UNIT:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
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
        switch (column) {
            case COL_MARKET:
                return offer.getMarketType();
            case COL_UNITTYPE:
                return UnitType.getTypeName(offer.getUnitType());
            case COL_WEIGHTCLASS:
                return EntityWeightClass.getClassName(offer.getUnit().getWeightClass(),
                        offer.getUnit().getUnitType(), offer.getUnit().isSupport());
            case COL_UNIT:
                return offer.getUnit().getName();
            case COL_PRICE:
                return offer.getPrice().toAmountAndSymbolString();
            case COL_PERCENT:
                return offer.getPercent() + "%";
            case COL_DELIVERY:
                return offer.getTransitDuration();
            default:
                return "?";
        }
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
