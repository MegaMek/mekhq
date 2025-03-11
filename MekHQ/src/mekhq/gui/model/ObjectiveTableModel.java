/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveAmountType;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;

/**
 * TableModel for displaying information about a scenario objective
 */
public class ObjectiveTableModel extends AbstractTableModel {
    // region Variable Declarations
    protected String[] columnNames;
    protected List<ScenarioObjective> data;

    public static final int COL_CRITERION = 0;
    public static final int COL_AMOUNT = 1;
    public static final int COL_TIME = 2;
    public static final int COL_SUCCESS_EFFECT = 3;
    public static final int COL_FAILURE_EFFECT = 4;
    public static final int N_COL = 5;
    // endregion Variable Declarations

    public ObjectiveTableModel(List<ScenarioObjective> entries) {
        data = entries;
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
            case COL_CRITERION:
                return "Type";
            case COL_AMOUNT:
                return "Amount";
            case COL_TIME:
                return "Time limits";
            case COL_SUCCESS_EFFECT:
                return "On Success";
            case COL_FAILURE_EFFECT:
                return "On Failure";
            default:
                return "?";
        }
    }

    public void addObjective(ScenarioObjective objective) {
        data.add(objective);
        fireTableDataChanged();
    }

    @Override
    public Object getValueAt(int row, int col) {
        ScenarioObjective objective;
        if (data.isEmpty()) {
            return "";
        } else {
            objective = getObjectiveAt(row);
        }

        switch (col) {
            case COL_CRITERION:
                return objective.getObjectiveCriterion().toString();
            case COL_AMOUNT:
                return objective.getAmountType().equals(ObjectiveAmountType.Percentage)
                        ? objective.getPercentage() + "%"
                        : objective.getAmount() + " units";
            case COL_TIME:
                if (objective.getTimeLimitType().equals(TimeLimitType.None)) {
                    return "None";
                }
                String timeDirection = objective.isTimeLimitAtMost() ? "At most " : "At least ";
                return objective.getTimeLimitType().equals(TimeLimitType.Fixed)
                        ? timeDirection + objective.getTimeLimit() + " turns"
                        : timeDirection + '(' + objective.getTimeLimitScaleFactor() + "x unit count) turns";
            case COL_SUCCESS_EFFECT:
                return objective.getSuccessEffects().size() + " Effect(s)";
            case COL_FAILURE_EFFECT:
                return objective.getFailureEffects().size() + " Effect(s)";
            default:
                return "?";
        }
    }

    public ScenarioObjective getObjectiveAt(int row) {
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        switch (c) {
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

    public String getTooltip(int row, int col) {
        ScenarioObjective objective;
        if (data.isEmpty()) {
            return null;
        } else {
            objective = getObjectiveAt(row);
        }
        StringBuilder sb;

        switch (col) {
            case COL_CRITERION:
                return "<html>" + String.join("<br>", objective.getAssociatedForceNames()) + "</html>";
            case COL_SUCCESS_EFFECT:
                sb = new StringBuilder();
                sb.append("<html>");
                for (ObjectiveEffect effect : objective.getSuccessEffects()) {
                    sb.append(effect.toString()).append("<br>");
                }
                sb.append("</html>");
                return sb.toString();
            case COL_FAILURE_EFFECT:
                sb = new StringBuilder();
                sb.append("<html>");
                for (ObjectiveEffect effect : objective.getFailureEffects()) {
                    sb.append(effect.toString()).append("<br>");
                }
                sb.append("</html>");
                return sb.toString();
            default:
                return null;
        }
    }

    // fill table with values
    public void setData(List<ScenarioObjective> objectives) {
        data = objectives;
        fireTableDataChanged();
    }

    public Renderer getRenderer() {
        return new Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }
    }
}
