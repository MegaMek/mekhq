/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.swing.JMenuItem;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.CrewSeatLayout;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.sorter.PersonTitleSorter;

/**
 * This is a standard menu that takes either a unit or multiple units that require the same tech type, and allows the
 * user to assign or remove a tech from them.
 *
 * <p>For a single unit it also offers each of the unit's free seats, listing the unassigned people at the unit's
 * location who qualify for it, grouped by experience level. The seats and who qualifies come from
 * {@link CrewSeatLayout} and {@link SeatEligibility}, the same rules the crew assignment dialog uses.</p>
 */
public class AssignUnitToPersonMenu extends JScrollableMenu {
    /** The experience-level submenus, best first; people with no level for the seat come last. */
    private static final List<SkillLevel> SKILL_ORDER = List.of(SkillLevel.LEGENDARY, SkillLevel.HEROIC,
          SkillLevel.ELITE, SkillLevel.VETERAN, SkillLevel.REGULAR, SkillLevel.GREEN, SkillLevel.ULTRA_GREEN,
          SkillLevel.NONE);

    // region Constructors
    public AssignUnitToPersonMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToPersonMenu");
        initialize(campaign, units);
    }
    // endregion Constructors

    // region Initialization
    private void initialize(final Campaign campaign, final Unit... units) {
        // Immediate Return for Illegal Assignments:
        // 1) No units to assign
        // 2) Any units are currently unavailable
        if ((units.length == 0) || Stream.of(units).anyMatch(unit -> !unit.isAvailable())) {
            return;
        }

        for (Unit unit : units) {
            Entity entity = unit.getEntity();

            if (entity == null || isUnsupportedEntity(entity)) {
                return;
            }
        }

        // Initialize Menu
        setText(resources.getString("AssignUnitToPersonMenu.title"));

        // Only assign non-tech personnel if the following is met:
        // 1) Only a single unit is selected
        if (units.length == 1) {
            createPersonAssignmentMenus(campaign, units[0]);
        }

        // Assign Tech to Unit Menu
        add(new AssignUnitToTechMenu(campaign, units));

        // Finally, add the ability to simply unassign... provided at least one of the
        // units has
        // any crew or a tech
        if (Stream.of(units).anyMatch(unit -> (unit.getTech() != null) || !unit.getCrew().isEmpty())) {
            String key = "miUnassignCrew";
            boolean keepTechs = false;
            removeCrew(units, key, keepTechs);
        }

        if (Stream.of(units).anyMatch(unit -> !unit.getCrew().isEmpty())) {
            String key = "miUnassignNonTechCrew";
            boolean keepTechs = true;
            removeCrew(units, key, keepTechs);
        }
    }

    private void removeCrew(Unit[] units, String key, boolean keepTechs) {
        final JMenuItem menuItem = new JMenuItem(resources.getString(key + ".text"));
        menuItem.setName(key);
        menuItem.addActionListener(evt -> {
            for (final Unit unit : units) {
                unit.clearCrew(keepTechs);
            }
        });
        add(menuItem);
    }

    private void createPersonAssignmentMenus(final Campaign campaign, final Unit unit) {
        // Only people without a unit, at the unit's location, sorted by their full title
        final List<Person> personnel = campaign.getPlayerForce().getPersonnel().values()
                                             .stream()
                                             .filter(person -> person.getUnit() == null)
                                             .filter(person -> LocationUtils.areSameEffectiveLocation(unit, person))
                                             .sorted(new PersonTitleSorter().reversed())
                                             .toList();
        if (personnel.isEmpty()) {
            return;
        }

        final Entity entity = unit.getEntity();
        final Map<CrewSeatMenu, JScrollableMenu> seatMenus = new EnumMap<>(CrewSeatMenu.class);
        for (final CrewSeat seat : CrewSeatLayout.forUnit(unit)) {
            if (seat.isFull()) {
                continue;
            }

            final Map<SkillLevel, JScrollableMenu> skillMenus = new EnumMap<>(SkillLevel.class);
            for (final Person person : personnel) {
                if (!CrewMenuAssignment.allQualify(campaign, unit, seat.type(), person)) {
                    continue;
                }
                // Judge the person by the role that qualifies them for this seat
                final boolean bySecondaryRole = !SeatEligibility.qualifiesByPrimaryRole(person, unit, seat.type());
                final SkillLevel level = person.getSkillLevel(campaign, bySecondaryRole, true);
                final JMenuItem item = new JMenuItem(person.getFullTitleAndProfessions());
                item.setName("mi" + seat.type().name());
                item.addActionListener(evt -> CrewMenuAssignment.assign(campaign, unit, seat.type(), person));
                skillMenus.computeIfAbsent(SKILL_ORDER.contains(level) ? level : SkillLevel.NONE,
                      key -> new JScrollableMenu(key.name().toLowerCase(Locale.ROOT) + "Menu",
                            key.toString())).add(item);
            }

            final JScrollableMenu seatMenu = seatMenus.computeIfAbsent(CrewSeatMenu.of(seat.type(), entity),
                  key -> new JScrollableMenu(key.name().toLowerCase(Locale.ROOT) + "Menu",
                        resources.getString(key.getUnitToPersonKey())));
            for (final SkillLevel level : SKILL_ORDER) {
                final JScrollableMenu skillMenu = skillMenus.get(level);
                if (skillMenu != null) {
                    seatMenu.add(skillMenu);
                }
            }
        }

        for (final CrewSeatMenu seatMenu : CrewSeatMenu.values()) {
            final JScrollableMenu menu = seatMenus.get(seatMenu);
            if (menu != null) {
                add(menu);
            }
        }
    }
    // endregion Initialization
}
