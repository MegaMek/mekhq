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

import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.JMenuHelpers;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class AssignUnitToTechMenu extends JMenu {
    //region Variable Declarations
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public AssignUnitToTechMenu(final Campaign campaign, final Unit unit) {
        this("AssignPersonToUnitMenu.Tech.title", campaign,
                unit.determineUnitTechSkillType(), unit.getMaintenanceTime(), unit);
    }

    public AssignUnitToTechMenu(final Campaign campaign, final @Nullable String skillName,
                                final int maintenanceTime, final Unit... units) {
        this("AssignPersonToUnitMenu.AssignTech.title", campaign, skillName, maintenanceTime, units);
    }

    public AssignUnitToTechMenu(final String title, final Campaign campaign,
                                final @Nullable String skillName, final int maintenanceTime,
                                final Unit... units) {
        super();
        initialize(title, campaign, skillName, maintenanceTime, units);
    }
    //endregion Constructors

    private void initialize(final String title, final Campaign campaign,
                            final @Nullable String skillName, final int maintenanceTime,
                            final Unit... units) {
        // Initialize Menu
        setText(resources.getString(title));
        setName("AssignUnitToTechMenu");

        // Default Return for Illegal Assignments - Null/Empty Skill Name or Self-Crewed Units
        // don't need techs, and if the total maintenance time is longer than the maximum for a
        // person we can just skip too
        if (StringUtil.isNullOrEmpty(skillName) || (units.length == 0)
                || Stream.of(units).anyMatch(Unit::isSelfCrewed)
                || (maintenanceTime > Person.PRIMARY_ROLE_SUPPORT_TIME)) {
            return;
        }

        // Person Assignment Menus
        final JMenu eliteMenu = new JMenu(SkillType.ELITE_NM);
        final JMenu veteranMenu = new JMenu(SkillType.VETERAN_NM);
        final JMenu regularMenu = new JMenu(SkillType.REGULAR_NM);
        final JMenu greenMenu = new JMenu(SkillType.GREEN_NM);
        final JMenu ultraGreenMenu = new JMenu(SkillType.ULTRA_GREEN_NM);

        for (final Person tech : campaign.getTechs()) {
            final boolean selected = Stream.of(units).allMatch(unit -> tech.equals(unit.getTech()));
            if (tech.hasSkill(skillName)
                    && (((tech.getMaintenanceTimeUsing() + maintenanceTime) <= Person.PRIMARY_ROLE_SUPPORT_TIME)
                    || selected)) {
                final String skillLevel = (tech.getSkillForWorkingOn(units[0]) == null) ? "Unknown"
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
                            resources.getString("miAssignTech.text"), tech.getFullTitle(),
                            tech.getMaintenanceTimeUsing()));
                    miAssignTech.setName("miAssignTech");
                    miAssignTech.setSelected(selected);
                    miAssignTech.addActionListener(evt -> {
                        for (final Unit unit : units) {
                            if (tech.equals(unit.getTech())) {
                                if (selected) {
                                    unit.remove(unit.getTech(), true);
                                }
                            } else {
                                if (unit.getTech() != null) {
                                    unit.remove(unit.getTech(), true);
                                }
                                unit.setTech(tech);
                            }
                        }
                    });
                    subMenu.add(miAssignTech);
                }
            }
        }

        JMenuHelpers.addMenuIfNonEmpty(this, eliteMenu);
        JMenuHelpers.addMenuIfNonEmpty(this, veteranMenu);
        JMenuHelpers.addMenuIfNonEmpty(this, regularMenu);
        JMenuHelpers.addMenuIfNonEmpty(this, greenMenu);
        JMenuHelpers.addMenuIfNonEmpty(this, ultraGreenMenu);

        // And finally add the ability to simply unassign
        final JMenuItem miUnassignTech = new JMenuItem(resources.getString("None.text"));
        miUnassignTech.setName("miUnassignTech");
        miUnassignTech.addActionListener(evt -> {
            for (final Unit unit : units) {
                unit.remove(unit.getTech(), true);
            }
        });
        add(miUnassignTech);
    }
}
