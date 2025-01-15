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

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A table model for displaying scenarios
 */
public class ScenarioTableModel extends DataTableModel {
    //region Variable Declarations
    private final Campaign campaign;

    public final static int COL_NAME       = 0;
    public final static int COL_STATUS     = 1;
    public final static int COL_DATE       = 2;
    public final static int COL_ASSIGN     = 3;
    public final static int COL_SECTOR     = 4;
    public final static int N_COL          = 5;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ScenarioTableModel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public ScenarioTableModel(Campaign c) {
        data = new ArrayList<Scenario>();
        campaign = c;
    }
    //endregion Constructors

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> resources.getString("col_name.text");
            case COL_STATUS -> resources.getString("col_status.text");
            case COL_DATE -> resources.getString("col_date.text");
            case COL_ASSIGN -> resources.getString("col_assign.text");
            case COL_SECTOR -> resources.getString("col_sector.text");
            default -> resources.getString("col_unknown.text");
        };
    }

    public int getColumnWidth(int c) {
        return switch (c) {
            case COL_NAME -> 100;
            case COL_STATUS -> 50;
            default -> 20;
        };
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public Scenario getScenario(int row) {
        return (row < getRowCount()) ? (Scenario) data.get(row) : null;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (getData().isEmpty()) {
            return "";
        }
        Scenario scenario = getScenario(row);
        if (scenario == null) {
            return "?";
        }

        if (col == COL_NAME) {
            return scenario.getName();
        } else if (col == COL_STATUS) {
            return scenario.getStatus();
        } else if (col == COL_DATE) {
            if (scenario.getDate() == null) {
                return "-";
            } else {
                return MekHQ.getMHQOptions().getDisplayFormattedDate(scenario.getDate());
            }
        } else if (col == COL_ASSIGN) {
            return scenario.getForces(getCampaign()).getAllUnits(true).size();
        } else if (col == COL_SECTOR) {
            if (campaign.getCampaignOptions().isUseStratCon()) {
                if (scenario instanceof AtBScenario) {
                    AtBContract contract = ((AtBScenario) scenario).getContract(campaign);
                    StratconCampaignState campaignState = contract.getStratconCampaignState();

                    if (campaignState != null) {
                        for (StratconTrackState track : campaignState.getTracks()) {
                            for (StratconScenario stratconScenario : track.getScenarios().values()) {
                                if (Objects.equals(stratconScenario.getBackingScenario(), scenario)) {
                                    StratconCoords coords = stratconScenario.getCoords();

                                    if (coords == null) {
                                        return track.getDisplayableName();
                                    } else {
                                        return track.getDisplayableName() + '-' + coords.toBTString();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return "-";
        } else {
            return "?";
        }
    }

    public Renderer getRenderer() {
        return new Renderer();
    }

    public static class Renderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof ScenarioStatus) {
                setToolTipText(((ScenarioStatus) value).getToolTipText());
            }
            return this;
        }
    }
}
