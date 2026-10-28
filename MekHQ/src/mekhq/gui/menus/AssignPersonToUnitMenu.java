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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.swing.JMenuItem;

import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.CrewSeatLayout;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.utilities.StaticChecks;

/**
 * This is a standard menu that takes either a person or multiple people, and allows the user to assign them to a unit
 * or remove them from their unit(s), including tech assignments.
 *
 * <p>Which seats are offered comes from {@link CrewSeatLayout} and
 * {@link mekhq.campaign.unit.crewAssignment.SeatEligibility}, the same rules the crew assignment dialog uses. A seat
 * that holds one person is offered only when one person is selected; a seat that holds several takes as many of the
 * selected people as it has room for.</p>
 */
public class AssignPersonToUnitMenu extends JScrollableMenu {
    //region Constructors
    public AssignPersonToUnitMenu(final Campaign campaign, final Person... people) {
        super("AssignPersonToUnitMenu");
        initialize(campaign, people);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Person... people) {
        // Immediate Return for Illegal Assignment
        // 1) No people to assign
        // 2) Any of the people you are trying to assign are currently deployed
        if ((people.length == 0) ||
                  Stream.of(people).anyMatch(Person::isDeployed) ||
                  !StaticChecks.areAllEmployed(people)) {
            return;
        }

        // Initialize Menu
        setText(resources.getString("AssignPersonToUnitMenu.title"));

        // Impossible Assignments:
        // 1) All people must be active
        // 2) All people must be non-prisoners (bondsmen should be assignable to units)
        // 3) All people must share one of their professions
        boolean assign = Stream.of(people)
                               .noneMatch(person -> !person.getStatus().isActive() ||
                                                          person.getPrisonerStatus().isCurrentPrisoner());

        if (assign) {
            final Profession basePrimaryProfession = Profession.getProfessionFromPersonnelRole(people[0].getPrimaryRole());
            final Profession baseSecondaryProfession = Profession.getProfessionFromPersonnelRole(people[0].getSecondaryRole());
            for (final Person person : people) {
                final Profession primaryProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
                if ((primaryProfession == basePrimaryProfession) || (primaryProfession == baseSecondaryProfession)) {
                    continue;
                }
                final Profession secondaryProfession = Profession.getProfessionFromPersonnelRole(person.getSecondaryRole());
                if (secondaryProfession.isCivilian() ||
                          ((secondaryProfession != basePrimaryProfession) &&
                                 (secondaryProfession != baseSecondaryProfession))) {
                    assign = false;
                    break;
                }
            }
        }

        if (assign) {
            addSeatMenus(campaign, people);

            // Add the tech menu if there is only a single person to assign
            if (people.length == 1) {
                add(new AssignTechToUnitMenu(campaign, people[0]));
            }
        }

        // Finally, add the ability to simply unassign if there's a person assigned to anything
        if (Stream.of(people).anyMatch(person -> (person.getUnit() != null) || !person.getTechUnits().isEmpty())) {
            final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
            miUnassignPerson.setName("miUnassignPerson");
            miUnassignPerson.addActionListener(evt -> {
                for (final Person person : people) {
                    if (person.getUnit() != null) {
                        person.getUnit().remove(person, true);
                    }

                    if (!person.getTechUnits().isEmpty()) {
                        for (final Unit unit : new ArrayList<>(person.getTechUnits())) {
                            unit.remove(person, true);
                        }
                        person.clearTechUnits();
                    }
                }
            });
            add(miUnassignPerson);
        }
    }

    /**
     * Adds one menu per kind of seat, each grouping the units with that seat free by unit type and then weight class.
     */
    private void addSeatMenus(final Campaign campaign, final Person... people) {
        // Use the hangar at the persons' effective location: base hangar when at a base, campaign hangar when in the
        // main force. A unit at a different location is refused when the assignment is applied.
        final AbstractBase effectiveBase = LocationUtils.findEffectiveBase(people[0]);
        final LocalHangar sourceHangar = (effectiveBase != null) ?
                                               effectiveBase.getBaseHangar() :
                                               campaign.getPlayerForce().getHangar();
        final List<Unit> units = HangarSorter.defaultSorting()
                                       .sort(sourceHangar.getUnitsStream().filter(Unit::isAvailable))
                                       .toList();

        // Seat menu -> unit type -> weight class -> one item per unit, kept in the hangar's sorted order
        final Map<CrewSeatMenu, Map<String, Map<String, List<JMenuItem>>>> items = new EnumMap<>(CrewSeatMenu.class);
        final boolean singlePerson = people.length == 1;
        for (final Unit unit : units) {
            final Entity entity = unit.getEntity();
            for (final CrewSeat seat : CrewSeatLayout.forUnit(unit)) {
                if (seat.isFull()
                          || (!singlePerson && (seat.capacity() <= 1))
                          || !CrewMenuAssignment.allQualify(campaign, unit, seat.type(), people)) {
                    continue;
                }
                final JMenuItem item = new JMenuItem(unit.getName());
                item.setName("mi" + seat.type().name());
                item.addActionListener(evt -> CrewMenuAssignment.assign(campaign, unit, seat.type(), people));
                items.computeIfAbsent(CrewSeatMenu.of(seat.type(), entity), key -> new LinkedHashMap<>())
                      .computeIfAbsent(UnitType.getTypeDisplayableName(entity.getUnitType()),
                            key -> new LinkedHashMap<>())
                      .computeIfAbsent(EntityWeightClass.getClassName(entity.getWeightClass(), entity),
                            key -> new ArrayList<>())
                      .add(item);
            }
        }

        for (final CrewSeatMenu seatMenu : CrewSeatMenu.values()) {
            final JScrollableMenu menu = new JScrollableMenu(seatMenu.name().toLowerCase(Locale.ROOT) + "Menu",
                  resources.getString(seatMenu.getPersonToUnitKey()));
            for (final Map.Entry<String, Map<String, List<JMenuItem>>> unitType :
                  items.getOrDefault(seatMenu, Map.of()).entrySet()) {
                final JScrollableMenu unitTypeMenu = new JScrollableMenu("unitTypeMenu", unitType.getKey());
                for (final Map.Entry<String, List<JMenuItem>> weightClass : unitType.getValue().entrySet()) {
                    final JScrollableMenu weightClassMenu = new JScrollableMenu("entityWeightMenu",
                          weightClass.getKey());
                    weightClass.getValue().forEach(weightClassMenu::add);
                    unitTypeMenu.add(weightClassMenu);
                }
                menu.add(unitTypeMenu);
            }
            add(menu);
        }
    }
    //endregion Initialization
}
