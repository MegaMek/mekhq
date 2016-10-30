package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.IconPackage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying work items
 */
public class AcquisitionTableModel extends DataTableModel {
    private static final long serialVersionUID = -6256038046416893994L;

    public AcquisitionTableModel() {
        columnNames = new String[] { "Parts Needed" };
        data = new ArrayList<IAcquisitionWork>();
    }

    public Object getValueAt(int row, int col) {
        return ((IAcquisitionWork) data.get(row)).getAcquisitionDesc();
    }

    public IAcquisitionWork getAcquisitionAt(int row) {
        return (IAcquisitionWork) data.get(row);
    }

    public AcquisitionTableModel.Renderer getRenderer(IconPackage icons) {
        return new AcquisitionTableModel.Renderer(icons);
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
            int actualRow = table.convertRowIndexToModel(row);
            //MissingPart task = getAcquisitionAt(row);
            
            IAcquisitionWork aw = getAcquisitionAt(actualRow);
            String imgPath = "";
            
            if (aw instanceof Part) {
            	imgPath = Part.findPartImage((Part)aw, true);
            } else {
            	imgPath = "data/images/misc/repair/equipment.png";
            }
            
            Image imgTool = getToolkit().getImage(imgPath); //$NON-NLS-1$
            this.setImage(imgTool);
            setOpaque(true);
            setText(getValueAt(row, column).toString(), "black");
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }

            c.setBackground(new Color(220, 220, 220));
            return c;
        }

    }
}