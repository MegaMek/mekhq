package mekhq.gui.model;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.BasicInfo;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

public class StoryPointTableModel extends DataTableModel {

    public StoryPointTableModel() {
        columnNames = new String[] { "Story Points" };
        data = new ArrayList<StoryPoint>();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getStoryPointDesc((StoryPoint) data.get(rowIndex));
    }

    private String getStoryPointDesc(StoryPoint storyPoint) {
        StringBuilder toReturn = new StringBuilder(128);
        toReturn.append("<html><b>").append(storyPoint.getName()).append("</b><br/>");
        toReturn.append(storyPoint.getClass().getSimpleName());
        toReturn.append("</html>");
        return toReturn.toString();
    }

    public StoryPoint getStoryPointAt(int row) {
        return (StoryPoint) data.get(row);
    }

    public StoryPointTableModel.Renderer getRenderer() {
        return new StoryPointTableModel.Renderer();
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setHtmlText(getValueAt(row, column).toString());
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            setBackground(table.getBackground());
            setForeground(table.getForeground());
            return this;
        }
    }
}
