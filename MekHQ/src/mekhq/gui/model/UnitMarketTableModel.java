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
import mekhq.campaign.market.UnitMarket;

/**
 * Model for displaying offers on the UnitMarket
 *
 * Code borrowed heavily from PersonnelTableModel
 *
 * @author Neoancient
 */
public class UnitMarketTableModel extends DataTableModel {
	private static final long serialVersionUID = -6275443301484277495L;

	public static final int COL_MARKET = 0;
	public static final int COL_UNITTYPE = 1;
	public static final int COL_WEIGHTCLASS = 2;
	public static final int COL_UNIT = 3;
	public static final int COL_PRICE = 4;
	public static final int COL_PERCENT = 5;
	public static final int COL_NUM = 6;

	public UnitMarketTableModel() {
        columnNames = new String[] { "Market", "Type", "Weight Class", "Unit", "Price", "Percent" };
        data = new ArrayList<UnitMarket.MarketOffer>();
	}

    public int getColumnWidth(int c) {
        switch (c) {
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

    public int getAlignment(int col) {
        switch (col) {
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

    public UnitMarket.MarketOffer getOffer(int i) {
        if (i >= data.size()) {
            return null;
        } else {
            return (UnitMarket.MarketOffer) data.get(i);
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (data.isEmpty()) {
            return "";
        }
        UnitMarket.MarketOffer o = getOffer(row);
        if (o == null) {
            return "?";
        }

        if (col == COL_MARKET) {
            return o.market;
        } else if (col == COL_UNITTYPE) {
            return UnitType.getTypeName(o.unitType);
        } else if (col == COL_WEIGHTCLASS) {
            if (o.unit != null) {
                return EntityWeightClass.getClassName(o.unitWeight,
                        o.unit.getUnitType(), o.unit.isSupport());
            }
        } else if (col == COL_UNIT) {
        	if (o.unit != null) {
        		return o.unit.getName();
        	} else {
                return "";
            }
        } else if (col == COL_PRICE) {
        	if (null == o.unit) {
        		return "";
        	}
            return Money.of((double) o.unit.getCost())
                    .multipliedBy(o.pct)
                    .dividedBy(100)
                    .toAmountAndSymbolString();
        } else if (col == COL_PERCENT) {
       		return o.pct + "%";
        }

        return "?";
	}
}
