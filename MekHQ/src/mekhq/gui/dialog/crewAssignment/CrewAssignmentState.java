/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.crewAssignment;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.CrewSeat;
import mekhq.campaign.unit.crewAssignment.SeatType;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.UnitEntry;

/**
 * What the player has chosen in the {@link CrewAssignmentDialog}: each page's filters, the units and people selected,
 * and the row being looked at. Selections are kept here rather than in the lists, so they survive filtering and
 * switching pages; that is how a bulk assignment is built up across the Units and Personnel pages.
 *
 * <p>The filter rules are here too, as plain methods, so the pages only show their results.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewAssignmentState {
    /** Which units the Units page shows, by how well they are crewed once the plan is applied. */
    enum UnitStatus {
        NEEDS_CREW, UNMANNED, FULLY_CREWED, ALL
    }

    /** Which people the Personnel page shows, by whether they crew a unit once the plan is applied. */
    enum Assignment {
        UNASSIGNED, ASSIGNED, EVERYONE
    }

    /** A seat type on one unit, used to show only the people who qualify for it. */
    record SeatFilter(Unit unit, SeatType seat) {}

    CrewAssignmentPage page = CrewAssignmentPage.UNITS;

    // Units page
    UnitStatus unitStatus = UnitStatus.NEEDS_CREW;
    @Nullable String unitType;
    @Nullable String formation;
    String unitQuery = "";
    /** When set, the Units page shows only units with a seat this person could take. */
    @Nullable Person seatsForPerson;

    // Personnel page
    Assignment assignment = Assignment.UNASSIGNED;
    @Nullable PersonnelRole role;
    String peopleQuery = "";
    /** When set, the Personnel page shows only people who could take this seat. */
    @Nullable SeatFilter qualifiedFor;

    // Selections, kept in the order they were made
    final Set<Unit> selectedUnits = new LinkedHashSet<>();
    final Set<Person> selectedPeople = new LinkedHashSet<>();

    /** The seat auto-fill fills on the selected units, or {@code null} to pick the first one with room. */
    @Nullable SeatType targetSeat;

    // The row whose details the inspector shows; at most one is set
    @Nullable Unit focusedUnit;
    @Nullable Person focusedPerson;

    void focus(Unit unit) {
        focusedUnit = unit;
        focusedPerson = null;
    }

    void focus(Person person) {
        focusedPerson = person;
        focusedUnit = null;
    }

    // region Filters

    /**
     * @param entry a unit
     * @param plan  the staged plan, whose changes count toward how well the unit is crewed
     *
     * @return {@code true} if the Units page should list the unit
     */
    boolean shows(UnitEntry entry, AssignmentPlan plan) {
        Unit unit = entry.unit();
        if ((unitType != null) && !unitType.equals(entry.typeName())) {
            return false;
        }
        if ((formation != null) && !formation.equals(entry.formationName())) {
            return false;
        }
        if ((seatsForPerson != null) && (plan.seatFor(seatsForPerson, unit) == null)) {
            return false;
        }
        if (!matches(entry.searchText(), unitQuery)) {
            return false;
        }
        return matches(crewing(unit, plan), unitStatus);
    }

    /**
     * @param entry a person
     * @param plan  the staged plan, whose changes count toward whether the person crews a unit
     *
     * @return {@code true} if the Personnel page should list the person
     */
    boolean shows(PersonEntry entry, AssignmentPlan plan) {
        Person person = entry.person();
        boolean isAssigned = plan.plannedUnitOf(person) != null;
        if (((assignment == Assignment.UNASSIGNED) && isAssigned)
                  || ((assignment == Assignment.ASSIGNED) && !isAssigned)) {
            return false;
        }
        if ((role != null) && !person.hasRole(role)) {
            return false;
        }
        if ((qualifiedFor != null) && !plan.canStage(person, qualifiedFor.unit(), qualifiedFor.seat())
                  && !qualifiedFor.unit().equals(plan.plannedUnitOf(person))) {
            return false;
        }
        return matches(entry.searchText(), peopleQuery);
    }

    /** How well a unit is crewed once the plan is applied. */
    enum Crewing {
        EMPTY, PARTIAL, FULL
    }

    static Crewing crewing(Unit unit, AssignmentPlan plan) {
        int capacity = 0;
        int open = 0;
        for (CrewSeat seat : plan.seatsOf(unit)) {
            capacity += seat.capacity();
            open += Math.max(0, plan.openSeats(unit, seat.type()));
        }
        if (open == 0) {
            return Crewing.FULL;
        }
        return (open >= capacity) ? Crewing.EMPTY : Crewing.PARTIAL;
    }

    static boolean matches(Crewing crewing, UnitStatus status) {
        return switch (status) {
            case NEEDS_CREW -> crewing != Crewing.FULL;
            case UNMANNED -> crewing == Crewing.EMPTY;
            case FULLY_CREWED -> crewing == Crewing.FULL;
            case ALL -> true;
        };
    }

    /**
     * @param searchText the lower-case text a row can be found by
     * @param query      what the player typed; every word must appear
     *
     * @return {@code true} if the row matches
     */
    static boolean matches(String searchText, String query) {
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!word.isEmpty() && !searchText.contains(word)) {
                return false;
            }
        }
        return true;
    }

    // endregion Filters
}
