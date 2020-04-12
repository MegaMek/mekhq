package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.IconPackage;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.BasicInfo;

/**
 * A table model for displaying doctors
 */
public class DocTableModel extends DataTableModel {
    private static final long serialVersionUID = -6934834363013004894L;

    private final Campaign campaign;

    public DocTableModel(Campaign c) {
        columnNames = new String[] { "Doctors" };
        data = new ArrayList<Person>();
        campaign = c;
    }

    public Object getValueAt(int row, int col) {
        return getDocDesc((Person) data.get(row));
    }

    private String getDocDesc(Person doc) {
        StringBuilder toReturn = new StringBuilder(128);
        toReturn.append("<html><font size='2'><b>").append(doc.getFullTitle()).append("</b><br/>");

        Skill skill = doc.getSkill(SkillType.S_DOCTOR);
        if (null != skill) {
            toReturn.append(SkillType.getExperienceLevelName(skill.getExperienceLevel()))
                    .append(" " + SkillType.S_DOCTOR);
        }

        toReturn.append(String.format(" (%d XP)", doc.getXp()));

        if (campaign.getMedicsPerDoctor() < 4) {
            toReturn.append("</font><font size='2' color='red'>, ")
                    .append(campaign.getMedicsPerDoctor())
                    .append(" medics</font><font size='2'><br/>");
        } else {
            toReturn.append(String.format(", %d medics<br />", campaign.getMedicsPerDoctor()));
        }

        toReturn.append(String.format("%d patient(s)</font></html>", campaign.getPatientsFor(doc)));

        return toReturn.toString();
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
            setPortrait(getDoctorAt(row));
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
