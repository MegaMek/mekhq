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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */


package mekhq.gui.model;

import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.SwingConstants;

import mekhq.campaign.market.UnitMarket;
import mekhq.campaign.universe.UnitTableData;

/**
 * Model for displaying offers on the UnitMarket
 * 
 * Code borrowed heavily from PersonnelTableModel
 * 
 * @author Neoancient
 *
 */
public class UnitMarketTableModel extends DataTableModel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6275443301484277495L;
	
	public static final int COL_MARKET = 0;
	public static final int COL_UNITTYPE = 1;
	public static final int COL_WEIGHTCLASS = 2;
	public static final int COL_UNIT = 3;
	public static final int COL_PRICE = 4;
	public static final int COL_PERCENT = 5;
	public static final int COL_NUM = 6;
	
	private static final String[] colNames = {
		"Market", "Type", "Weight Class", "Unit", "Price", "Percent"
	};
	
	public UnitMarketTableModel() {
		data = new ArrayList<UnitMarket.MarketOffer>();
	}
	
    @Override
    public int getColumnCount() {
        return COL_NUM;
    }

    @Override
    public String getColumnName(int column) {
    	return colNames[column];
    }
	
    public int getColumnWidth(int c) {
        switch(c) {
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
        switch(col) {
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

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public UnitMarket.MarketOffer getOffer(int i) {
        if( i >= data.size()) {
            return null;
        }
        return (UnitMarket.MarketOffer)data.get(i);
    }

    @Override
    public Object getValueAt(int row, int col) {
        UnitMarket.MarketOffer o;
        DecimalFormat formatter = new DecimalFormat();
        if(data.isEmpty()) {
            return "";
        } else {
            o = getOffer(row);
        }
        if(col == COL_MARKET) {
            return UnitMarket.marketNames[o.market];
        }
        if(col == COL_UNITTYPE) {
            return UnitTableData.unitNames[o.unitType];
        }
        if(col == COL_WEIGHTCLASS) {
        	return UnitTableData.weightNames[o.unitWeight];
        }
        if(col == COL_UNIT) {
        	if (o.unit != null) {
        		return o.unit.getName();
        	}
            return "";
        }
        if(col == COL_PRICE) {
        	if (null == o.unit) {
        		return "";
        	}
            return formatter.format(Math.ceil(o.unit.getCost() * o.pct / 100.0));
        }
        if(col == COL_PERCENT) {
       		return o.pct + "%";
        }
        return "?";
	}
}
