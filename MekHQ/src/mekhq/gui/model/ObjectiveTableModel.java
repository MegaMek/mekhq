package mekhq.gui.model;

import megamek.common.Entity;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.parts.Part;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectiveTableModel extends AbstractTableModel {
    //region Variable Declarations
    protected String[] columnNames;
    protected List<ScenarioObjective> data;

    public final static int COL_DESC    = 0;

    public final static int N_COL       = 1;
    //endregion Variable Declarations

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
            case COL_DESC:
                return "Description";
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
            case COL_DESC:
                return objective.getDescription();
            default:
                return "?";
        }
    }

    public ScenarioObjective getObjectiveAt(int row) {
        return data.get(row);
    }

    public int getColumnWidth(int c) {
        switch (c) {
            case COL_DESC:
                return 100;
            default:
                return 20;
        }
    }

    public int getAlignment(int col) {
        return SwingConstants.LEFT;
    }

    public String getTooltip(int row, int col) {
        switch (col) {
            default:
                return null;
        }
    }

    //fill table with values
    public void setData(List<ScenarioObjective> objectives) {
        data = objectives;
        fireTableDataChanged();
    }

    public ObjectiveTableModel.Renderer getRenderer() {
        return new ObjectiveTableModel.Renderer();
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
