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
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.gui.BasicInfo;

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
            toReturn.append("<b>")
                    .append(SkillType.getColoredExperienceLevelName(skill.getExperienceLevel()))
                    .append("</b> " + SkillType.S_DOCTOR);
        }

        toReturn.append(String.format(" (%d XP)", doc.getXP()));

        if (campaign.requiresAdditionalMedics()) {
            toReturn.append("</font><font size='2' color='")
                    .append(MekHQ.getMHQOptions().getFontColorNegativeHexColor()).append("'>, ")
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
