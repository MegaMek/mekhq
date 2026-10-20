/*
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.TechRepairAbilities;
import mekhq.campaign.work.TechTaskEstimate;
import mekhq.campaign.work.WorkTimeEstimate;
import mekhq.gui.BasicInfo;
import mekhq.gui.CampaignGUI;
import mekhq.gui.ITechWorkPanel;
import mekhq.utilities.ReportingUtilities;

/**
 * A table model for displaying work items
 */
public class TechTableModel extends DataTableModel<Person> {

    /** Contains the skill levels to be displayed in a tech's description */
    private static final String[] DISPLAYED_SKILL_LEVELS = new String[] {
          SkillType.S_TECH_MEK,
          SkillType.S_TECH_VEHICLE,
          SkillType.S_TECH_BA,
          SkillType.S_TECH_AERO,
          SkillType.S_TECH_VESSEL,
          };

    private final CampaignGUI tab;
    private final ITechWorkPanel panel;

    public TechTableModel(CampaignGUI tab, ITechWorkPanel panel) {
        columnNames = new String[] { "Techs" };
        data = new ArrayList<>();
        this.tab = tab;
        this.panel = panel;
    }

    @Override
    public Object getValueAt(int row, int col) {
        return getTechAt(row);
    }

    public Person getTechAt(int row) {
        return data.get(row);
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
            setImage(tech.getPortraitImageIconWithFallback(true, 54).getImage());
            setHtmlText(getTechDescription(tech, getCampaign().isOvertimeAllowed(), panel.getSelectedTask()));
            String abilityEffects = TechRepairAbilities.describeEffects(tech);
            setToolTipText((abilityEffects == null) ? null : ("<html>" + abilityEffects + "</html>"));
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

    /**
     * @deprecated Use {@link #getTechDescription(Person, boolean, IPartWork)} instead.
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String getTechDesc(Person tech, boolean overtimeAllowed, IPartWork part) {
        return getTechDescription(tech, overtimeAllowed, part);
    }

    /**
     * Generates an HTML-formatted description of a technician, summarizing key professional information and status.
     *
     * <p>The description includes the technician's full title (with possible color highlighting), primary skills and
     * their experience levels, total experience points, edge (if applicable), available and overtime minutes, and
     * special technician traits or flaws. If a specific part is provided and associated with a unit the technician can
     * service, the name may be highlighted.</p>
     *
     * @param tech            the {@link Person} representing the technician
     * @param overtimeAllowed whether overtime is permitted for this technician
     * @param part            the {@link IPartWork} part or repair task being considered, or {@code null} if not
     *                        applicable
     *
     * @return a formatted HTML {@link String} describing the technician's skills, time availability, and special traits
     */
    public String getTechDescription(Person tech, boolean overtimeAllowed, IPartWork part) {
        StringBuilder toReturn = new StringBuilder(128);
        toReturn.append("<html><font");
        if ((null != part) && (null != part.getUnit()) && tech.getTechUnits().contains(part.getUnit())) {
            toReturn.append(" color='").append(ReportingUtilities.getPositiveColor()).append("'><b>@");
        } else {
            toReturn.append("><b>");
        }
        toReturn.append(tech.getFullTitle()).append("</b><br/>");

        SkillModifierData skillModifierData = tech.getSkillModifierData();
        Skill taskSkill = (null == part) ? null : tech.getSkillForWorkingOn(part);
        if (null != taskSkill) {
            // With a task selected, only show the skill that will actually be used for it
            appendSkillLevel(toReturn, taskSkill, skillModifierData);
        } else {
            boolean first = true;
            for (String skillName : DISPLAYED_SKILL_LEVELS) {
                Skill skill = tech.getSkill(skillName);
                if (null == skill) {
                    continue;
                } else if (!first) {
                    toReturn.append("; ");
                }

                appendSkillLevel(toReturn, skill, skillModifierData);
                first = false;
            }
        }

        toReturn.append(String.format(" (%d XP", tech.getXP()));
        // if Edge usage is allowed for techs, display remaining edge in the dialogue
        if (getCampaign().getCampaignOptions().get(CampaignOption.USE_EDGE) &&
                  tech.getOptions().booleanOption(PersonnelOptions.EDGE_REPAIR_BREAK_PART)) {
            toReturn.append(String.format(", %d Edge)", tech.getCurrentEdge()));
        } else {
            toReturn.append(')');
        }
        toReturn.append("<br/>");

        // The abilities that change repair work, so a tech worth saving for a weapon or armor job stands out
        String abilities = TechRepairAbilities.describeNames(tech);
        if (!abilities.isEmpty()) {
            toReturn.append("<i>").append(abilities).append("</i><br/>");
        }

        toReturn.append(WorkTimeEstimate.describeTechDay(getCampaign(), tech, overtimeAllowed));
        if (null != part) {
            TechTaskEstimate estimate = TechTaskEstimate.estimate(getCampaign(), part, tech);
            toReturn.append("<br/>").append(WorkTimeEstimate.describeTechJob(estimate));
            String finish = WorkTimeEstimate.describeTechFinish(getCampaign(), estimate);
            if (!finish.isEmpty()) {
                toReturn.append("<br/>").append(finish);
            }
        }

        toReturn.append("</font></html>");
        return toReturn.toString();
    }

    /**
     * @param estimate the tech's estimate for a task
     *
     * @return one line with what the task asks of the tech, or why the tech cannot do it
     *
     * @deprecated Use {@link WorkTimeEstimate#describeTechJob(TechTaskEstimate)} and
     *       {@link WorkTimeEstimate#describeTechFinish(Campaign, TechTaskEstimate)} instead.
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public static String describeEstimate(TechTaskEstimate estimate) {
        return WorkTimeEstimate.describeTechJob(estimate);
    }

    /**
     * Appends a skill's colored experience level and name (e.g. "<b>Veteran</b> Tech/Aeronautics") to the builder.
     *
     * @param builder           the description being built
     * @param skill             the skill to describe
     * @param skillModifierData the tech's skill modifiers, used to determine the effective experience level
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void appendSkillLevel(StringBuilder builder, Skill skill, SkillModifierData skillModifierData) {
        int experienceLevel = skill.getExperienceLevel(skillModifierData);
        builder.append("<b>")
              .append(SkillType.getColoredExperienceLevelName(experienceLevel))
              .append("</b> ").append(skill.getType().getName());
    }
}
