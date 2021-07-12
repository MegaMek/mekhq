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

import megamek.common.util.StringUtil;
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
        // Default Return for Illegal Assignment
        // 1) No Units to assign
        // 2) Any self crewed units
        if ((units.length == 0) || Stream.of(units).anyMatch(Unit::isSelfCrewed)) {
            return;
        }


        // Initialize Menu
        setText(resources.getString("AssignUnitToTechMenu.title"));

        // Initial Parsing Values
        final int maintenanceTime = Stream.of(units).mapToInt(Unit::getMaintenanceTime).sum();
        final String skillName = units[0].determineUnitTechSkillType();
        final boolean assign = (maintenanceTime < Person.PRIMARY_ROLE_SUPPORT_TIME)
                && !StringUtil.isNullOrEmpty(skillName)
                && Stream.of(units).allMatch(unit -> skillName.equalsIgnoreCase(unit.determineUnitTechSkillType()));

        if (assign) {
            // Person Assignment Menus
            final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillType.ELITE_NM);
            final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillType.VETERAN_NM);
            final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillType.REGULAR_NM);
            final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillType.GREEN_NM);
            final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillType.ULTRA_GREEN_NM);

            // Boolean Parsing Values
            final boolean allShareTech = Stream.of(units).allMatch(unit -> (units[0].getTech() == null)
                    ? (unit.getTech() == null) : units[0].getTech().equals(unit.getTech()));

            for (final Person tech : campaign.getTechs()) {
                if (allShareTech && tech.equals(units[0].getTech())) {
                    continue;
                }

                if (tech.hasSkill(skillName)
                        && ((tech.getMaintenanceTimeUsing() + maintenanceTime) <= Person.PRIMARY_ROLE_SUPPORT_TIME)) {
                    final String skillLevel = (tech.getSkillForWorkingOn(units[0]) == null) ? ""
                            : SkillType.getExperienceLevelName(tech.getSkillForWorkingOn(units[0]).getExperienceLevel());

                    final JScrollableMenu subMenu;
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
                        final JMenuItem miAssignTech = new JMenuItem(String.format(
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
        }

        // And finally add the ability to simply unassign, provided at least one unit has a tech
        if (Stream.of(units).anyMatch(unit -> unit.getTech() != null)) {
            final JMenuItem miUnassignTech = new JMenuItem(resources.getString("miUnassignTech.text"));
            miUnassignTech.setName("miUnassignTech");
            miUnassignTech.addActionListener(evt -> Stream.of(units).forEach(unit -> unit.remove(unit.getTech(), true)));
            add(miUnassignTech);
        }
    }
    //endregion Initialization
}
