/*
 * UnitMarketPreferences.java
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

import mekhq.gui.model.UnitMarketTableModel;

import javax.swing.*;
import java.util.Properties;

/**
 * Stores user preferences for the Unit Market dialog.
 * Preferences are stored as string, but they are exposed as strongly typed.
 */
public class UnitMarketPreferences {
    private final String UNIT_MARKET_SHOW_MEKS = "UnitMarketShowMeks";
    private final String UNIT_MARKET_SHOW_VEHICLES = "UnitMarketShowVehicles";
    private final String UNIT_MARKET_SHOW_AEROSPACE = "UnitMarketShowAerospace";
    private final String UNIT_MARKET_SHOW_ONLY_CHECK = "UnitMarketShowOnlyCheck";
    private final String UNIT_MARKET_SHOW_ONLY_PERCENTAGE = "UnitMarketShowOnlyPercentage";
    private final String UNIT_MARKET_SORT_COLUMN_INDEX = "UnitMarketSortColumnIndex";
    private final String UNIT_MARKET_SORT_ORDER = "UnitMarketSortOrder";

    private final boolean DEFAULT_UNIT_MARKET_SHOW_MEKS = true;
    private final boolean DEFAULT_UNIT_MARKET_SHOW_VEHICLES = true;
    private final boolean DEFAULT_UNIT_MARKET_SHOW_AEROSPACE = false;
    private final boolean DEFAULT_UNIT_MARKET_SHOW_ONLY_CHECK = false;
    private final int DEFAULT_UNIT_MARKET_SHOW_ONLY_PERCENTAGE = 120;
    private final int DEFAULT_UNIT_MARKET_SORT_COLUMN_INDEX = UnitMarketTableModel.COL_MARKET;
    private final SortOrder DEFAULT_UNIT_MARKET_SORT_ORDER = SortOrder.DESCENDING;

    private final Properties preferences;

    public UnitMarketPreferences(Properties preferences) {
        assert preferences != null;

        this.preferences = preferences;

        // Set default values
        this.preferences.setProperty(UNIT_MARKET_SHOW_MEKS, Boolean.toString(DEFAULT_UNIT_MARKET_SHOW_MEKS));
        this.preferences.setProperty(UNIT_MARKET_SHOW_VEHICLES, Boolean.toString(DEFAULT_UNIT_MARKET_SHOW_VEHICLES));
        this.preferences.setProperty(UNIT_MARKET_SHOW_AEROSPACE, Boolean.toString(DEFAULT_UNIT_MARKET_SHOW_AEROSPACE));
        this.preferences.setProperty(UNIT_MARKET_SHOW_ONLY_CHECK, Boolean.toString(DEFAULT_UNIT_MARKET_SHOW_ONLY_CHECK));
        this.preferences.setProperty(UNIT_MARKET_SHOW_ONLY_PERCENTAGE, Integer.toString(DEFAULT_UNIT_MARKET_SHOW_ONLY_PERCENTAGE));
        this.preferences.setProperty(UNIT_MARKET_SORT_COLUMN_INDEX, Integer.toString(DEFAULT_UNIT_MARKET_SORT_COLUMN_INDEX));
        this.preferences.setProperty(UNIT_MARKET_SORT_ORDER, DEFAULT_UNIT_MARKET_SORT_ORDER.toString());
    }

    public boolean getShowMeks() {
        return Boolean.parseBoolean(this.preferences.getProperty(UNIT_MARKET_SHOW_MEKS));
    }

    public void setShowMeks(boolean show) {
        this.preferences.setProperty(UNIT_MARKET_SHOW_MEKS, Boolean.toString(show));
    }

    public boolean getShowVehicles() {
        return Boolean.parseBoolean(this.preferences.getProperty(UNIT_MARKET_SHOW_VEHICLES));
    }

    public void setShowVehicles(boolean show) {
        this.preferences.setProperty(UNIT_MARKET_SHOW_VEHICLES, Boolean.toString(show));
    }

    public boolean getShowAerospace() {
        return Boolean.parseBoolean(this.preferences.getProperty(UNIT_MARKET_SHOW_AEROSPACE));
    }

    public void setShowAerospace(boolean show) {
        this.preferences.setProperty(UNIT_MARKET_SHOW_AEROSPACE, Boolean.toString(show));
    }

    public boolean getShowOnlyCheck() {
        return Boolean.parseBoolean(this.preferences.getProperty(UNIT_MARKET_SHOW_ONLY_CHECK));
    }

    public void setShowOnlyCheck(boolean show) {
        this.preferences.setProperty(UNIT_MARKET_SHOW_ONLY_CHECK, Boolean.toString(show));
    }

    public int getShowOnlyPercentage() {
        return Integer.parseInt(this.preferences.getProperty(UNIT_MARKET_SHOW_ONLY_PERCENTAGE));
    }

    public void setShowOnlyPercentage(int percentage) {
        assert percentage >= 0;
        this.preferences.setProperty(UNIT_MARKET_SHOW_ONLY_PERCENTAGE, Integer.toString(percentage));
    }

    public int getSortColumn() {
        return Integer.parseInt(this.preferences.getProperty(UNIT_MARKET_SORT_COLUMN_INDEX));
    }

    public void setSortColumn(int columnIndex) {
        assert columnIndex > -1 && columnIndex < UnitMarketTableModel.N_COL;
        this.preferences.setProperty(UNIT_MARKET_SORT_COLUMN_INDEX, Integer.toString(columnIndex));
    }

    public SortOrder getSortOrder() {
        return SortOrder.valueOf(this.preferences.getProperty(UNIT_MARKET_SORT_ORDER));
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.preferences.setProperty(UNIT_MARKET_SORT_ORDER, sortOrder.toString());
    }
}
