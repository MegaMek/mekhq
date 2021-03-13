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

import java.util.ArrayList;

import javax.swing.SwingConstants;

import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.finances.Money;
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
    public static final int COL_NUM = 6;
    //endregion Variable Declarations

    //region Constructors
    public UnitMarketTableModel() {
        columnNames = new String[] { "Market", "Type", "Weight Class", "Unit", "Price", "Percent" };
        data = new ArrayList<UnitMarketOffer>();
    }
    //endregion Constructors

    public int getColumnWidth(final int column) {
        switch (column) {
            case COL_MARKET:
            case COL_UNIT:
                return 100;
            case COL_UNITTYPE:
            case COL_WEIGHTCLASS:
                return 50;
            case COL_PRICE:
                return 70;
            default:
                return 20;
        }
    }

    public int getAlignment(final int column) {
        switch (column) {
            case COL_PRICE:
            case COL_PERCENT:
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

    public UnitMarketOffer getOffer(final int index) {
        return (index < getData().size()) ? (UnitMarketOffer) data.get(index) : null;
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        if (getData().isEmpty()) {
            return "";
        }
        UnitMarketOffer offer = getOffer(row);
        if (offer == null) {
            return "?";
        }

        if (column == COL_MARKET) {
            return offer.getMarketType();
        } else if (column == COL_UNITTYPE) {
            return UnitType.getTypeName(offer.getUnitType());
        } else if (column == COL_WEIGHTCLASS) {
            return EntityWeightClass.getClassName(offer.getUnit().getWeightClass(),
                    offer.getUnit().getUnitType(), offer.getUnit().isSupport());
        } else if (column == COL_UNIT) {
            return offer.getUnit().getName();
        } else if (column == COL_PRICE) {
            return Money.of((double) offer.getUnit().getCost()).multipliedBy(offer.getPercent())
                    .dividedBy(100).toAmountAndSymbolString();
        } else if (column == COL_PERCENT) {
            return offer.getPercent() + "%";
        } else {
            return "?";
        }
    }
}
