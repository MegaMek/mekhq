package mekhq.gui.model;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.gui.BasicInfo;
import mekhq.gui.utilities.MekHqTableCellRenderer;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutoAwardsTableModel extends AbstractTableModel {
    public final static int COL_PERSON = 0;
    public final static int COL_NAME = 1;
    public final static int COL_SET= 2;
    public final static int COL_AWARD = 3;
    public final static int COL_DESCRIPTION = 4;
    public final static int N_COL = 5;

    private final static String[] colNames = {
            "Person", "Name", "Set", "Award", "Description"
    };

    private final Campaign campaign;
    private Map<Integer,List<Object>> data;

    public AutoAwardsTableModel(Campaign c) {
        this.campaign = c;
        data = new HashMap<>();
    }

    public void setData(Map<Integer,List<Object>> map) {
        data = map;
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
        return colNames[column];
    }

    public int getColumnWidth(int column) {
        switch (column) {
            case COL_PERSON:
                return 250;
            case COL_NAME:
            case COL_DESCRIPTION:
                return 150;
            case COL_SET:
                return 40;
            case COL_AWARD:
            default:
                return 30;
        }
    }

    public int getAlignment(int column) {
        switch (column) {
            case COL_PERSON:
            case COL_DESCRIPTION:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COL_AWARD;
    }

    @Override
    public Class<?> getColumnClass(int col) {
        Class<?> retVal = Object.class;
        try {
            retVal = getValueAt(0, col).getClass();
        } catch (NullPointerException e) {
            LogManager.getLogger().error("", e);
        }
        return retVal;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Person person;
        Award award;

        if (data.isEmpty()) {
            return "";
        } else {
            person = campaign.getPerson((UUID) data.get(rowIndex).get(0));
            award = (Award) data.get(rowIndex).get(1);

        }

        switch (columnIndex) {
            case COL_PERSON:
                return person.makeHTMLRank();
            case COL_NAME:
                return award.getName();
            case COL_SET:
                return award.getSet();
            case COL_AWARD:
                return data.get(rowIndex).get(2);
            case COL_DESCRIPTION:
                return award.getDescription();
            default:
                return "?";
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int column) {
        if (column == COL_AWARD) {
            data.get(rowIndex).set(2, value);
        }

        fireTableDataChanged();
    }

    public Person getPerson(int rowIndex) {
        return campaign.getPerson((UUID) data.get(rowIndex).get(0));
    }

    public String getAwardName(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getName();
    }

    public String getAwardSet(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getSet();
    }

    public String getAwardDescription(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getDescription();
    }

    public TableCellRenderer getRenderer(int col) {
        if (col < COL_DESCRIPTION) {
            return new VisualRenderer();
        } else {
            return new TextRenderer();
        }
    }

    public class TextRenderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int rowIndex, int columnIndex) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, rowIndex, columnIndex);
            int actualColumn = table.convertColumnIndexToModel(columnIndex);
            setHorizontalAlignment(getAlignment(actualColumn));
            return this;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        public VisualRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int rowIndex, int columnIndex) {
            int actualColumn = table.convertColumnIndexToModel(columnIndex);
            int actualRow = table.convertRowIndexToModel(rowIndex);
            setText(getValueAt(actualRow, actualColumn).toString());

            switch (actualColumn) {
                case COL_PERSON:
                    setText(getPerson(actualRow).getFullDesc(campaign));
                    setImage(getPerson(actualRow).getPortrait().getImage(50));
                    break;
                case COL_NAME:
                    setText(getAwardName(actualRow));
                    break;
                case COL_SET:
                    setText(getAwardSet(actualRow));
                    break;
                case COL_DESCRIPTION:
                    setText(getAwardDescription(actualRow));
                    break;
                default:
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, rowIndex);

            return this;
        }
    }
}