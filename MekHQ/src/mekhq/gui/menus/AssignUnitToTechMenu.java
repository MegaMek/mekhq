/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.menus;

import java.util.stream.Stream;
import javax.swing.JMenuItem;

import megamek.codeUtilities.StringUtility;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

/**
 * This is a standard menu that takes either a unit or multiple units, and allows the user to assign or remove a tech
 * from them.
 */
public class AssignUnitToTechMenu extends JScrollableMenu {
    // region Constructors

    /**
     * @param campaign the campaign the unit is a part of
     * @param units    the units in question
     */
    public AssignUnitToTechMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToTechMenu");
        initialize(campaign, units);
    }
    // endregion Constructors

    // region Initialization
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
        final boolean assign = (maintenanceTime < Person.PRIMARY_ROLE_SUPPORT_TIME) &&
                                     !StringUtility.isNullOrBlank(skillName) &&
                                     Stream.of(units)
                                           .allMatch(unit -> skillName.equalsIgnoreCase(unit.determineUnitTechSkillType()));

        if (assign) {
            // Person Assignment Menus
            final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
            final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
            final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
            final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
            final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
            final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
            final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                  SkillLevel.ULTRA_GREEN.toString());

            // Boolean Parsing Values
            final boolean allShareTech = Stream.of(units)
                                               .allMatch(unit -> (units[0].getTech() == null) ?
                                                                       (unit.getTech() == null) :
                                                                       units[0].getTech().equals(unit.getTech()));

            for (final Person tech : campaign.getTechs()) {
                if (allShareTech && tech.equals(units[0].getTech())) {
                    continue;
                }

                if (tech.hasSkill(skillName) &&
                          ((tech.getMaintenanceTimeUsing() + maintenanceTime) <= Person.PRIMARY_ROLE_SUPPORT_TIME)) {
                    SkillModifierData skillModifierData = tech.getSkillModifierData();

                    final SkillLevel skillLevel = (tech.getSkillForWorkingOn(units[0]) == null) ?
                                                        SkillLevel.NONE :
                                                        tech.getSkillForWorkingOn(units[0])
                                                              .getSkillLevel(skillModifierData);

                    final JScrollableMenu subMenu = switch (skillLevel) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        int dailyTime = tech.getDailyAvailableTechTime(campaign.getCampaignOptions()
                                                                             .isTechsUseAdministration());
                        int dailyTimeUsing = tech.getMaintenanceTimeUsing();
                        int available = dailyTime - dailyTimeUsing;

                        final JMenuItem miAssignTech = new JMenuItem(String.format(resources.getString(
                              "miAssignTech.text"), tech.getFullTitle(), available));
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

            add(legendaryMenu);
            add(heroicMenu);
            add(eliteMenu);
            add(veteranMenu);
            add(regularMenu);
            add(greenMenu);
            add(ultraGreenMenu);
        }

        // And finally add the ability to simply unassign, provided at least one unit
        // has a tech
        if (Stream.of(units).anyMatch(unit -> unit.getTech() != null)) {
            final JMenuItem miUnassignTech = new JMenuItem(resources.getString("miUnassignTech.text"));
            miUnassignTech.setName("miUnassignTech");
            miUnassignTech.addActionListener(evt -> Stream.of(units)
                                                          .forEach(unit -> unit.remove(unit.getTech(), true)));
            add(miUnassignTech);
        }
    }
    // endregion Initialization
}
