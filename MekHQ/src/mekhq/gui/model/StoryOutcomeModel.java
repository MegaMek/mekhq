package mekhq.gui.model;

import megamek.common.Entity;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.parts.Part;
import mekhq.campaign.storyarc.StoryArc;
import mekhq.campaign.storyarc.StoryOutcome;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class StoryOutcomeModel extends DataTableModel {

    protected String[] columnNames;
    protected List<StoryOutcome> data;
    StoryArc storyArc;

    public final static int COL_RESULT   = 0;
    public final static int COL_NEXT     = 1;
    public final static int COL_TRIGGERS = 2;
    public final static int N_COL        = 3;
    //endregion Variable Declarations

    public StoryOutcomeModel(List<StoryOutcome> outcomes, StoryArc arc) {
        data = outcomes;
        storyArc = arc;
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
            case COL_RESULT:
                return "Result";
            case COL_NEXT:
                return "Next Story Point";
            case COL_TRIGGERS:
                return "Story Triggers";
            default:
                return "?";
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        StoryOutcome outcome;
        if (data.isEmpty()) {
            return "";
        } else {
            outcome = getOutcomeAt(row);
        }

        switch (col) {
            case COL_RESULT:
                return outcome.getResult();
            case COL_NEXT:
                return outcome.getNextStoryPointId() == null ? "" : storyArc.getStoryPoint(outcome.getNextStoryPointId()).getName();
            case COL_TRIGGERS:
                return outcome.getStoryTriggers().size();
            default:
                return "?";
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public StoryOutcome getOutcomeAt(int row) {
        return data.get(row);
    }

    public void addOutcome(StoryOutcome outcome) {
        data.add(outcome);
        fireTableDataChanged();
    }

    public List<StoryOutcome> getAllOutcomes() {
        return data;
    }

    public int getColumnWidth(int c) {
        switch (c) {
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

    public StoryOutcomeModel.Renderer getRenderer() {
        return new StoryOutcomeModel.Renderer();
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
