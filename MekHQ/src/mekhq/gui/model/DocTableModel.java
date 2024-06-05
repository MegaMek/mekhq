package mekhq.gui.model;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.BasicInfo;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;

/**
 * A table model for displaying doctors
 */
public class DocTableModel extends DataTableModel {
    private final Campaign campaign;

    public DocTableModel(Campaign c) {
        columnNames = new String[] { "Doctors" };
        data = new ArrayList<Person>();
        campaign = c;
    }

    @Override
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

        toReturn.append(String.format(" (%d XP)", doc.getXP()));

        if (campaign.requiresAdditionalMedics()) {
            toReturn.append("</font><font size='2' color='Orange'>, ")
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

    public DocTableModel.Renderer getRenderer() {
        return new DocTableModel.Renderer();
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setImage(getDoctorAt(row).getPortrait().getImage(54));
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
