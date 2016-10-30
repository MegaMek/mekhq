package mekhq.gui.model;

import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.IconPackage;
import mekhq.campaign.parts.Part;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying work items
 */
public class TaskTableModel extends DataTableModel {
    private static final long serialVersionUID = -6256038046416893994L;

    public TaskTableModel() {
        columnNames = new String[] { "Tasks" };
        data = new ArrayList<Part>();
    }

    public Object getValueAt(int row, int col) {
        return ((Part) data.get(row)).getDesc();
    }

    public Part getTaskAt(int row) {
        return (Part) data.get(row);
    }

    public Part[] getTasksAt(int[] rows) {
        Part[] tasks = new Part[rows.length];
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            tasks[i] = (Part) data.get(row);
        }
        return tasks;
    }

    public TaskTableModel.Renderer getRenderer(IconPackage icons) {
        return new TaskTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {

        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -3052618135259621130L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            
            Part part = getTaskAt(actualRow);
            Image imgTool = getToolkit().getImage(Part.findPartImage(part, false)); //$NON-NLS-1$
            
            this.setImage(imgTool);
            setOpaque(true);
            setText("<html>" + getValueAt(actualRow, actualCol).toString() + "</html>", "black");
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            
            return c;
        }

    }
}