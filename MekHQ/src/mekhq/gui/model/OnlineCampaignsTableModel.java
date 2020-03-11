/*
 * Copyright (c) 2020 The MegaMek Team.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import mekhq.campaign.RemoteCampaign;

public class OnlineCampaignsTableModel extends DataTableModel {

    private static final long serialVersionUID = 5792881778901299604L;

    public final static int COL_ID = 0;
    public final static int COL_NAME       = 1;
    public final static int COL_DATE       = 2;
    public final static int COL_LOCATION   = 3;
    public final static int COL_IS_GM      = 4;
    public final static int COL_IS_ACTIVE  = 5;
    public final static int N_COL          = 6;

    private final DateTimeFormatter dateFormatter = ISODateTimeFormat.date();

    public OnlineCampaignsTableModel() {
        setData(Collections.emptyList());
    }

    public OnlineCampaignsTableModel(Collection<RemoteCampaign> remoteCampaigns) {
        setData(new ArrayList<>(remoteCampaigns));
    }

    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
        case COL_ID:
            return "ID";
        case COL_NAME:
            return "Name";
        case COL_DATE:
            return "Date";
        case COL_LOCATION:
        	return "Current Location";
        case COL_IS_GM:
            return "GM?";
        case COL_IS_ACTIVE:
            return "Active?";
        default:
            return "?";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RemoteCampaign row = (RemoteCampaign)data.get(rowIndex);
        switch (columnIndex)
        {
            case COL_ID:
                return row.getId();
            case COL_NAME:
                return row.getName();
            case COL_DATE:
                return dateFormatter.print(row.getDate());
            case COL_LOCATION:
                return row.getLocation().getName(row.getDate());
            case COL_IS_GM:
                return row.isGMMode() ? "Yes" : "No";
            case COL_IS_ACTIVE:
                return "Yes";
            default:
                return null;
        }
    }
}
