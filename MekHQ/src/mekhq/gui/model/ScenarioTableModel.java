/*
 * Copyright (c) 2013, 2020 - The MegaMek Team. All Rights Reserved.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;

/**
 * A table model for displaying scenarios
 */
public class ScenarioTableModel extends DataTableModel {
    private static final long serialVersionUID = 534443424190075264L;

    private Campaign campaign;

    public final static int COL_NAME       = 0;
    public final static int COL_STATUS     = 1;
    public final static int COL_DATE       = 2;
    public final static int COL_ASSIGN     = 3;
    public final static int N_COL          = 4;

    public ScenarioTableModel(Campaign c) {
        data = new ArrayList<Scenario>();
        campaign = c;
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
        switch (column) {
            case COL_NAME:
                return "Scenario Name";
            case COL_STATUS:
                return "Resolution";
            case COL_DATE:
                return "Date";
            case COL_ASSIGN:
                return "# Units";
            default:
                return "?";
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        Scenario scenario = getScenario(row);
        if (col == COL_NAME) {
            return scenario.getName();
        } else if (col == COL_STATUS) {
            return scenario.getStatusName();
        } else if (col == COL_DATE) {
            if (null == scenario.getDate()) {
                return "-";
            } else {
                SimpleDateFormat shortDateFormat = new SimpleDateFormat(
                        getCampaign().getCampaignOptions().getDisplayDateFormat());
                return shortDateFormat.format(scenario.getDate());
            }
        } else if (col == COL_ASSIGN) {
            return scenario.getForces(getCampaign()).getAllUnits().size();
        } else {
            return "?";
        }
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_NAME:
                return 100;
            case COL_STATUS:
                return 50;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        switch (col) {
            default:
                return SwingConstants.LEFT;
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

    public Scenario getScenario(int row) {
        return (Scenario) data.get(row);
    }

    private Campaign getCampaign() {
        return campaign;
    }
}
