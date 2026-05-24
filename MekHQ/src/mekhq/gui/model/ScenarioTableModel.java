/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConCoords;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.gui.utilities.MekHqTableCellRenderer;
import mekhq.utilities.ReportingUtilities;

/**
 * A table model for displaying scenarios
 */
public class ScenarioTableModel extends DataTableModel<Scenario> {
    //region Variable Declarations
    private final Campaign campaign;

    public static final int COL_NAME = 0;
    public static final int COL_STATUS = 1;
    public static final int COL_DATE = 2;
    public static final int COL_ASSIGN = 3;
    public static final int COL_SECTOR = 4;
    public static final int N_COL = 5;

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ScenarioTableModel",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public ScenarioTableModel(Campaign c) {
        data = new ArrayList<>();
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
            case COL_NAME -> 150;
            case COL_STATUS -> 140;
            case COL_DATE -> 70;
            case COL_ASSIGN -> 80;
            case COL_SECTOR -> 120;
            default -> 80;
        };
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    private Campaign getCampaign() {
        return campaign;
    }

    public Scenario getScenario(int row) {
        return (row < getRowCount()) ? data.get(row) : null;
    }

    public boolean isPriorityScenario(Scenario scenario) {
        return isCrisisScenario(scenario) || isStrategicScenario(scenario) || isTurningPointScenario(scenario) ||
                     isDualScenario(scenario);
    }

    public boolean isCrisisScenario(Scenario scenario) {
        return (getStratConScenario(scenario) != null) &&
                     (scenario.isCrisis() || scenario.getStratConScenarioType().isSpecial());
    }

    public boolean isStrategicScenario(Scenario scenario) {
        StratConScenario stratconScenario = getStratConScenario(scenario);
        return (stratconScenario != null) && stratconScenario.isStrategicObjective();
    }

    public boolean isTurningPointScenario(Scenario scenario) {
        StratConScenario stratconScenario = getStratConScenario(scenario);
        return (stratconScenario != null) && stratconScenario.isTurningPoint();
    }

    public boolean isDualScenario(Scenario scenario) {
        return (getStratConScenario(scenario) != null) && scenario.getStratConScenarioType().isOfficialChallenge();
    }

    public String getScenarioToolTip(Scenario scenario) {
        if (scenario == null) {
            return null;
        }

        ArrayList<String> details = new ArrayList<>();
        details.add(scenario.getStatus().getToolTipText());
        if (isStrategicScenario(scenario)) {
            details.add(resources.getString("col_status.strategic"));
        }
        if (isTurningPointScenario(scenario)) {
            details.add(resources.getString("col_status.turningPoint"));
        }
        if (isCrisisScenario(scenario)) {
            details.add(resources.getString("col_status.crisis"));
        }
        if (isDualScenario(scenario)) {
            details.add(resources.getString("col_status.dual"));
        }
        return String.join(" ", details);
    }

    private StratConScenario getStratConScenario(Scenario scenario) {
        if (!campaign.getCampaignOptions().isUseStratCon() || !(scenario instanceof AtBScenario atBScenario)) {
            return null;
        }

        AtBContract contract = atBScenario.getContract(campaign);
        if (contract == null) {
            return null;
        }
        return atBScenario.getStratconScenario(contract, atBScenario);
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
            StratConScenario stratconScenario = getStratConScenario(scenario);

            if (stratconScenario != null) {
                // Determine attributes of the scenario
                boolean isStrategic = isStrategicScenario(scenario);
                boolean isTurningPoint = isTurningPointScenario(scenario);
                boolean isCrisis = isCrisisScenario(scenario);
                boolean isDual = isDualScenario(scenario);

                // Set the opening span color based on scenario type (Strategic, Crisis, or Turning Point)
                String openingSpan = "";
                if (isCrisis || isStrategic || isDual) {
                    openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor());
                } else if (isTurningPoint) {
                    openingSpan = spanOpeningWithCustomColor(ReportingUtilities.getWarningColor());
                }

                // Generate an appropriate label
                String scenarioSeverityText;
                if (isStrategic) {
                    scenarioSeverityText = resources.getString("col_status.strategic");
                } else if (isTurningPoint) {
                    scenarioSeverityText = resources.getString("col_status.turningPoint");
                } else if (isCrisis) {
                    scenarioSeverityText = resources.getString("col_status.crisis");
                } else if (isDual) {
                    scenarioSeverityText = resources.getString("col_status.dual");
                } else {
                    scenarioSeverityText = "";
                }

                // Add closing span tag if there is an opening span
                String closingSpan = openingSpan.isEmpty() ? "" : CLOSING_SPAN_TAG;

                // Wrap in HTML and include bold formatting for accessibility
                return String.format(
                      "<html>%s%s<b> %s</b>%s</html>",
                      scenario.getStatus().toString(),
                      openingSpan,
                      scenarioSeverityText,
                      closingSpan
                );
            }

            return scenario.getStatus().toString();
        } else if (col == COL_DATE) {
            if (scenario.getDate() == null) {
                return "-";
            } else {
                return MekHQ.getMHQOptions().getDisplayFormattedDate(scenario.getDate());
            }
        } else if (col == COL_ASSIGN) {
            return scenario.getForces(getCampaign()).getAllUnits(false).size();
        } else if (col == COL_SECTOR) {
            if (campaign.getCampaignOptions().isUseStratCon()) {
                if (scenario instanceof AtBScenario atBScenario) {
                    AtBContract contract = atBScenario.getContract(campaign);
                    if (contract == null) {
                        return "-";
                    }
                    StratConCampaignState campaignState = contract.getStratconCampaignState();
                    StratConScenario stratconScenario = atBScenario.getStratconScenario(contract, atBScenario);

                    if (campaignState != null && stratconScenario != null) {
                        StratConTrackState track = stratconScenario.getTrackForScenario(campaign, campaignState);
                        StratConCoords coords = stratconScenario.getCoords();

                        if (coords == null) {
                            return track.getDisplayableName();
                        } else {
                            return track.getDisplayableName() + '-' + coords.toBTString();
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
            if (table.getModel() instanceof ScenarioTableModel model) {
                Scenario scenario = model.getScenario(table.convertRowIndexToModel(row));
                int modelColumn = table.convertColumnIndexToModel(column);
                String valueText = ReportingUtilities.stripHtmlTags(Objects.toString(value, ""));
                String toolTipText = model.getColumnName(modelColumn) + ": " + valueText;
                if (modelColumn == COL_STATUS) {
                    String scenarioToolTip = ReportingUtilities.stripHtmlTags(model.getScenarioToolTip(scenario));
                    if ((scenarioToolTip != null) && !scenarioToolTip.isBlank()) {
                        toolTipText += " - " + scenarioToolTip;
                    }
                }
                setToolTipText(toolTipText);
                Font tableFont = table.getFont();
                setFont(model.isPriorityScenario(scenario) ? tableFont.deriveFont(Font.BOLD) : tableFont);
            }
            return this;
        }
    }
}
