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
 * A table model for displaying work items
 */
public class TechTableModel extends DataTableModel {
    private static final long serialVersionUID = 2738333372316332962L;

    private Campaign campaign;
    
    public TechTableModel(Campaign c) {
        columnNames = new String[] { "Techs" };
        data = new ArrayList<Person>();
        campaign = c;
    }

    public Object getValueAt(int row, int col) {
        return getTechAt(row);
    }

    public Person getTechAt(int row) {
        return (Person) data.get(row);
    }
    
    public Campaign getCampaign() {
        return campaign;
    }

    public TechTableModel.Renderer getRenderer(IconPackage icons) {
        return new TechTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -4951696376098422679L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualRow = table.convertRowIndexToModel(row);
            setOpaque(true);
            setPortrait(getTechAt(actualRow));
            setText(getTechAt(actualRow).getTechDesc(getCampaign().isOvertimeAllowed()), "black");
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
