/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.model;

import static mekhq.campaign.personnel.skills.SkillType.getColoredExperienceLevelName;

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.BasicInfo;
import mekhq.utilities.ReportingUtilities;

/**
 * A table model for displaying doctors
 */
public class DocTableModel extends DataTableModel<Person> {
    private final Campaign campaign;

    public DocTableModel(Campaign c) {
        columnNames = new String[] { "Doctors" };
        data = new ArrayList<>();
        campaign = c;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return getDoctorDescription(data.get(row));
    }

    /**
     * Builds an HTML-formatted description of the given doctor, summarizing their professional details and current
     * responsibilities.
     *
     * <p>The description includes the doctor's full title, doctor skill experience level (if available), skill name,
     * total experience points, number of required medics, and number of assigned patients. The formatting and color
     * coding may indicate special requirements based on campaign settings.</p>
     *
     * @param doctor the {@link Person} object representing the doctor
     *
     * @return a formatted HTML {@link String} describing the doctor's qualifications and status
     */
    private String getDoctorDescription(Person doctor) {
        StringBuilder toReturn = new StringBuilder(128);
        toReturn.append("<html><font><b>").append(doctor.getFullTitle()).append("</b><br/>");

        Skill skill = doctor.getSkill(SkillType.S_SURGERY);
        if (null != skill) {
            SkillModifierData skillModifierData = doctor.getSkillModifierData();
            int experienceLevel = skill.getExperienceLevel(skillModifierData);

            toReturn.append("<b>").append(getColoredExperienceLevelName(experienceLevel))
                  .append("</b> " + SkillType.S_SURGERY);
        }

        toReturn.append(String.format(" (%d XP)", doctor.getXP()));

        if (campaign.requiresAdditionalMedics()) {
            toReturn.append("</font><font color='")
                  .append(ReportingUtilities.getNegativeColor()).append("'>, ")
                  .append(campaign.getMedicsPerDoctor())
                  .append(" medics</font><font><br/>");
        } else {
            toReturn.append(String.format(", %d medics<br />", campaign.getMedicsPerDoctor()));
        }

        toReturn.append(String.format("%d patient(s)</font></html>", campaign.getPatientsFor(doctor)));

        return toReturn.toString();
    }

    public Person getDoctorAt(int row) {
        return data.get(row);
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
