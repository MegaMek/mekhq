/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
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
            SkillType.S_TECH_MEK,
            SkillType.S_TECH_MECHANIC,
            SkillType.S_TECH_BA,
            SkillType.S_TECH_AERO,
            SkillType.S_TECH_VESSEL,
    };

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

    public Renderer getRenderer() {
        return new Renderer();
    }

    public class Renderer extends BasicInfo implements TableCellRenderer {
        public Renderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = this;
            int actualRow = table.convertRowIndexToModel(row);
            setOpaque(true);
            Person tech = getTechAt(actualRow);
            setImage(tech.getPortrait().getImage(54));
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
        toReturn.append("<html><font");
        if ((null != part) && (null != part.getUnit()) && tech.getTechUnits().contains(part.getUnit())) {
            toReturn.append(" color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'><b>@");
        } else {
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

            toReturn.append("<b>")
                .append(SkillType.getColoredExperienceLevelName(skill.getExperienceLevel()))
                .append("</b> ").append(skillName);
            first = false;
        }

        toReturn.append(String.format(" (%d XP", tech.getXP()));
        // if Edge usage is allowed for techs, display remaining edge in the dialogue
        if (getCampaign().getCampaignOptions().isUseSupportEdge()
                && tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART)) {
            toReturn.append(String.format(", %d Edge)", tech.getCurrentEdge()));
        } else {
            toReturn.append(")");
        }
        toReturn.append("<br/>");

        toReturn.append(String.format("%d minutes left", tech.getMinutesLeft()));
        if (overtimeAllowed) {
            toReturn.append(String.format(" + (%d overtime)", tech.getOvertimeLeft()));
        }
        toReturn.append("</font></html>");
        return toReturn.toString();
    }
}
