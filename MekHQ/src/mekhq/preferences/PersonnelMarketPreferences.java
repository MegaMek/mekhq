/*
 * PersonnelMarketPreferences.java
 *
 * Copyright (c) 2019 MekHQ team. All rights reserved.
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

package mekhq.preferences;

import mekhq.gui.model.PersonnelTableModel;

import javax.swing.*;
import java.util.Properties;

/**
 * Stores user preferences for the Personnel Market dialog.
 * Preferences are stored as string, but they are exposed as strongly typed.
 */
public class PersonnelMarketPreferences {
    private final String PERSONNEL_MARKET_FILTER_INDEX = "PersonnelMarketFilterIndex";
    private final String PERSONNEL_MARKET_SORT_COLUMN_INDEX = "PersonnelMarketSortColumnIndex";
    private final String PERSONNEL_MARKET_SORT_ORDER = "PersonnelMarketSortOrder";

    private final int DEFAULT_PERSONNEL_MARKET_FILTER_INDEX = 0;
    private final int DEFAULT_PERSONNEL_MARKET_SORT_COLUMN_INDEX = PersonnelTableModel.COL_SKILL;
    private final SortOrder DEFAULT_PERSONNEL_MARKET_SORT_ORDER = SortOrder.DESCENDING;

    private final Properties preferences;

    public PersonnelMarketPreferences(Properties preferences) {
        assert preferences != null;

        this.preferences = preferences;

        // Set default values
        this.preferences.setProperty(PERSONNEL_MARKET_FILTER_INDEX, Integer.toString(DEFAULT_PERSONNEL_MARKET_FILTER_INDEX));
        this.preferences.setProperty(PERSONNEL_MARKET_SORT_COLUMN_INDEX, Integer.toString(DEFAULT_PERSONNEL_MARKET_SORT_COLUMN_INDEX));
        this.preferences.setProperty(PERSONNEL_MARKET_SORT_ORDER, DEFAULT_PERSONNEL_MARKET_SORT_ORDER.toString());
    }

    public int getFilterIndex() {
        return Integer.parseInt(this.preferences.getProperty(PERSONNEL_MARKET_FILTER_INDEX));
    }

    public void setFilterIndex(int filterIndex) {
        this.preferences.setProperty(PERSONNEL_MARKET_FILTER_INDEX, Integer.toString(filterIndex));
    }

    public int getSortColumn() {
        return Integer.parseInt(this.preferences.getProperty(PERSONNEL_MARKET_SORT_COLUMN_INDEX));
    }

    public void setSortColumn(int columnIndex) {
        assert columnIndex > -1 && columnIndex < PersonnelTableModel.N_COL;
        this.preferences.setProperty(PERSONNEL_MARKET_SORT_COLUMN_INDEX, Integer.toString(columnIndex));
    }

    public SortOrder getSortOrder() {
        return SortOrder.valueOf(this.preferences.getProperty(PERSONNEL_MARKET_SORT_ORDER));
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.preferences.setProperty(PERSONNEL_MARKET_SORT_ORDER, sortOrder.toString());
    }
}
