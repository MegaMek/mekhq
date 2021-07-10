/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.menus;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

import javax.swing.*;
import java.util.stream.Stream;

/**
 * This is a standard menu that takes either a unit or multiple units, and allows the user to
 * assign or remove a tech from them.
 */
public class AssignUnitToTechMenu extends JScrollableMenu {
    //region Constructors
    /**
     * @param campaign the campaign the unit is a part of
     * @param units the units in question
     */
    public AssignUnitToTechMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToTechMenu");
        initialize(campaign, units);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Unit... units) {
        // Default Return for Illegal or Impossible Assignments
        // 1) No units to be assigned
        // 2) Self-Crewed units can't be assigned a tech
        if ((units.length == 0) || Stream.of(units).anyMatch(Unit::isSelfCrewed)) {
            return;
        }

        // Initialize Menu
        setText(resources.getString("AssignPersonToUnitMenu.title"));

        // Person Assignment Menus
        final JMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillType.ELITE_NM);
        final JMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillType.VETERAN_NM);
        final JMenu regularMenu = new JScrollableMenu("regularMenu", SkillType.REGULAR_NM);
        final JMenu greenMenu = new JScrollableMenu("greenMenu", SkillType.GREEN_NM);
        final JMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillType.ULTRA_GREEN_NM);

        // Boolean Parsing Values
        final boolean allShareTech = Stream.of(units).allMatch(unit -> (units[0].getTech() == null)
                ? (unit.getTech() == null) : units[0].getTech().equals(unit.getTech()));

        // 2) Null/Empty Skill Name
        // 4) More maintenance time required than a person can supply
        // StringUtil.isNullOrEmpty(skillName) || (maintenanceTime > Person.PRIMARY_ROLE_SUPPORT_TIME)

/*
        String skill = unit.determineUnitTechSkillType();
        int maintenanceTime = 0;
        if (!StringUtil.isNullOrEmpty(skill)) {
            if (skill.equals(u.determineUnitTechSkillType())) {
                maintenanceTime += u.getMaintenanceTime();
                if (maintenanceTime > Person.PRIMARY_ROLE_SUPPORT_TIME) {
                    skill = ""; // little performance saving hack
                }
            } else {
                allRequireSameTechType = false;
                skill = ""; // little performance saving hack
            }
        }
 */

        for (final Person tech : campaign.getTechs()) {
            if (allShareTech && tech.equals(units[0].getTech())) {
                continue;
            }

            if (tech.hasSkill(skillName)
                    && ((tech.getMaintenanceTimeUsing() + maintenanceTime) <= Person.PRIMARY_ROLE_SUPPORT_TIME)) {
                final String skillLevel = (tech.getSkillForWorkingOn(units[0]) == null) ? ""
                        : SkillType.getExperienceLevelName(tech.getSkillForWorkingOn(units[0]).getExperienceLevel());

                final JMenu subMenu;
                switch (skillLevel) {
                    case SkillType.ELITE_NM:
                        subMenu = eliteMenu;
                        break;
                    case SkillType.VETERAN_NM:
                        subMenu = veteranMenu;
                        break;
                    case SkillType.REGULAR_NM:
                        subMenu = regularMenu;
                        break;
                    case SkillType.GREEN_NM:
                        subMenu = greenMenu;
                        break;
                    case SkillType.ULTRA_GREEN_NM:
                        subMenu = ultraGreenMenu;
                        break;
                    default:
                        subMenu = null;
                        break;
                }

                if (subMenu != null) {
                    final JMenuItem miAssignTech = new JCheckBoxMenuItem(String.format(
                            resources.getString("miAssignTech.text"), tech.getFullTitle(), tech.getMaintenanceTimeUsing()));
                    miAssignTech.setName("miAssignTech");
                    miAssignTech.addActionListener(evt -> {
                        for (final Unit unit : units) {
                            if (tech.equals(unit.getTech())) {
                                continue;
                            } else if (unit.getTech() != null) {
                                unit.remove(unit.getTech(), true);
                            }
                            unit.setTech(tech);
                        }
                    });
                    subMenu.add(miAssignTech);
                }
            }
        }

        add(eliteMenu);
        add(veteranMenu);
        add(regularMenu);
        add(greenMenu);
        add(ultraGreenMenu);

        // And finally add the ability to simply unassign
        final JMenuItem miUnassignTech = new JMenuItem(resources.getString("miUnassignTech.text"));
        miUnassignTech.setName("miUnassignTech");
        miUnassignTech.addActionListener(evt -> {
            for (final Unit unit : units) {
                unit.remove(unit.getTech(), true);
            }
        });
        add(miUnassignTech);
    }
    //endregion Initialization
}
