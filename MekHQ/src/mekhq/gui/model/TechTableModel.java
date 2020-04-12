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
import mekhq.campaign.work.IPartWork;
import mekhq.gui.BasicInfo;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ITechWorkPanel;

/**
 * A table model for displaying work items
 */
public class TechTableModel extends DataTableModel {

    /** Contains the skill levels to be displayed in a tech's description */
    private static final String[] DISPLAYED_SKILL_LEVELS = new String[] {
        SkillType.S_TECH_MECH,
        SkillType.S_TECH_MECHANIC,
        SkillType.S_TECH_BA,
        SkillType.S_TECH_AERO,
        SkillType.S_TECH_VESSEL,
    };

    private static final long serialVersionUID = 2738333372316332962L;

    private CampaignGUI tab;
    private ITechWorkPanel panel;

    public TechTableModel(CampaignGUI tab, ITechWorkPanel panel) {
        columnNames = new String[] { "Techs" };
        data = new ArrayList<Person>();
        this.tab = tab;
        this.panel = panel;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return getTechAt(row);
    }

    public Person getTechAt(int row) {
        return (Person) data.get(row);
    }

    public Campaign getCampaign() {
        return tab.getCampaign();
    }

    public TechTableModel.Renderer getRenderer(IconPackage icons) {
        return new TechTableModel.Renderer(icons);
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer(IconPackage icons) {
            super(icons);
        }

        private static final long serialVersionUID = -4951696376098422679L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualRow = table.convertRowIndexToModel(row);
            setOpaque(true);
            Person tech = getTechAt(actualRow);
            setPortrait(tech);
            setHtmlText(getTechDesc(tech, getCampaign().isOvertimeAllowed(), panel.getSelectedTask()));
            if (isSelected) {
                highlightBorder();
            } else {
                unhighlightBorder();
            }
            c.setBackground(table.getBackground());
            c.setForeground(table.getForeground());
            return c;
        }
    }

    public String getTechDesc(Person tech, boolean overtimeAllowed, IPartWork part) {
        StringBuilder toReturn = new StringBuilder(128);
        toReturn.append("<html><font size='2'");
        if (null != part && null != part.getUnit() && tech.getTechUnitIDs().contains(part.getUnit().getId())) {
            toReturn.append(" color='green'><b>@");
        }
        else {
            toReturn.append("><b>");
        }
        toReturn.append(tech.getFullTitle()).append("</b><br/>");

        boolean first = true;
        for (String skillName : DISPLAYED_SKILL_LEVELS) {
            Skill skill = tech.getSkill(skillName);
            if (null == skill) {
                continue;
            } else if (!first) {
                toReturn.append("; ");
            }

            toReturn.append(SkillType.getExperienceLevelName(skill.getExperienceLevel()));
            toReturn.append(" ").append(skillName);
            first = false;
        }

        toReturn.append(String.format(" (%d XP)<br/>", tech.getXp()))
                .append(String.format("%d minutes left", tech.getMinutesLeft()));
        if (overtimeAllowed) {
            toReturn.append(String.format(" + (%d overtime)", tech.getOvertimeLeft()));
        }
        toReturn.append("</font></html>");
        return toReturn.toString();
    }
}
