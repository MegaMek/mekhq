package mekhq.gui.model;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying doctors
 */
public class DocTableModel extends DataTableModel {
    private static final long serialVersionUID = -6934834363013004894L;

    private Campaign campaign;
    
    public DocTableModel(Campaign c) {
        columnNames = new String[] { "Doctors" };
        data = new ArrayList<Person>();
        campaign = c;
    }

    public Object getValueAt(int row, int col) {
        return ((Person) data.get(row)).getDocDesc();
    }

    public Person getDoctorAt(int row) {
        return (Person) data.get(row);
    }
    
    public Campaign getCampaign() {
        return campaign;
    }

    public DocTableModel.Renderer getRenderer(IconPackage icons) {
        return new DocTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -818080358678474607L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            setOpaque(true);
            setPortrait(getDoctorAt(row));
            setText(getValueAt(row, column).toString(), "black");
            //setToolTipText(getCampaign().getTargetFor(getDoctorAt(row), getDoctorAt(row)).getDesc());
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