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
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.SeatEligibility;
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
    /**
     * Which units the Units page shows, by how well they are crewed once the plan is applied. Seats temporary crew
     * hold count as crewed, so a unit running on temporary crew does not need crew; {@link #ON_TEMP_CREW} finds those
     * units, to replace their temporary crew with real people.
     */
    enum UnitStatus {
        NEEDS_CREW, UNMANNED, ON_TEMP_CREW, NEEDS_TECH, FULLY_CREWED, ALL
    }

    /** Which people the Personnel page shows, by whether they crew a unit once the plan is applied. */
    enum Assignment {
        UNASSIGNED, ASSIGNED, EVERYONE
    }

    /**
     * Which people the Personnel page shows, by their health: fit for duty, wounded (any injury severity, counting
     * advanced medical injuries), or pregnant. A wounded, pregnant person counts as both, and is not fit for duty.
     *
     * @author Illiani
     * @since 0.51.01
     */
    enum Condition {
        ANY, FIT, WOUNDED, PREGNANT
    }

    /**
     * Which people the Personnel page shows, by their chassis familiarity: the familiarity steps where its bonuses
     * change. Only offered while chassis familiarity is in use.
     *
     * @author Illiani
     * @since 0.51.01
     */
    enum FamiliarityBand {
        ANY(0), UNDER_100(0), AT_LEAST_100(100), AT_LEAST_200(200), AT_LEAST_300(300);

        private final int threshold;

        FamiliarityBand(int threshold) {
            this.threshold = threshold;
        }

        /** @return the lowest familiarity this band asks for, which bands above the mode's cap could never reach */
        int getThreshold() {
            return threshold;
        }
    }

    /**
     * One entry in the Personnel page's Role filter: a single role, or a {@link RoleGroup}. A person matches if their
     * primary or secondary role does.
     *
     * @author Illiani
     * @since 0.51.01
     */
    sealed interface RoleFilter {
        /**
         * @param role a personnel role
         *
         * @return {@code true} if someone with this role should be shown under this filter
         */
        boolean matches(PersonnelRole role);
    }

    /**
     * A single role in the Role filter.
     *
     * @param role the role
     *
     * @author Illiani
     * @since 0.51.01
     */
    record SingleRole(PersonnelRole role) implements RoleFilter {
        @Override
        public boolean matches(PersonnelRole candidate) {
            return role == candidate;
        }
    }

    /**
     * A group of roles in the Role filter, covering the non-combat professions that can take a vehicle's crew seat:
     * the techs, and everyone else (medical, administrative, communications, sensor and catering staff).
     *
     * @author Illiani
     * @since 0.51.01
     */
    enum RoleGroup implements RoleFilter {
        ALL_TECHS, OTHER_PROFESSIONS;

        @Override
        public boolean matches(PersonnelRole role) {
            return switch (this) {
                case ALL_TECHS -> isVehicleTech(role);
                case OTHER_PROFESSIONS -> isOtherVehicleProfession(role);
            };
        }
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
    /** When set, the Units page shows only units this tech could take on, or already maintains. */
    @Nullable Person unitsForTech;

    // Personnel page
    Assignment assignment = Assignment.UNASSIGNED;
    @Nullable RoleFilter role;
    Condition condition = Condition.ANY;
    /** Stays {@link FamiliarityBand#ANY} while chassis familiarity is off, as the filter is not offered then. */
    FamiliarityBand familiarity = FamiliarityBand.ANY;
    String peopleQuery = "";
    /** When set, the Personnel page shows only people who could take this seat, even if it is full now. */
    @Nullable SeatFilter qualifiedFor;
    /** When set, the Personnel page shows only techs who could take this unit on, or already maintain it. */
    @Nullable Unit techFor;

    // Selections, kept in the order they were made
    final Set<Unit> selectedUnits = new LinkedHashSet<>();
    final Set<Person> selectedPeople = new LinkedHashSet<>();

    /** The seat auto-fill fills on the selected units, or {@code null} to pick the first one with room. */
    @Nullable SeatType targetSeat;
    /** {@code true} when auto-fill gives techs to the selected units instead of filling a seat. */
    boolean targetTech;

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
        if ((unitsForTech != null) && !unitsForTech.equals(plan.plannedTechOf(unit))
                  && !plan.canStageTech(unitsForTech, unit)) {
            return false;
        }

        if (!matches(entry.searchText(), unitQuery)) {
            return false;
        }
        return matchesStatus(entry, plan);
    }

    /**
     * @param entry a unit
     * @param plan  the staged plan
     *
     * @return {@code true} if the unit matches the Units page's status filter, ignoring its other filters
     */
    boolean matchesStatus(UnitEntry entry, AssignmentPlan plan) {
        Unit unit = entry.unit();
        if (unitStatus == UnitStatus.NEEDS_TECH) {
            return SeatEligibility.takesTech(unit) && (plan.plannedTechOf(unit) == null);
        }
        return matches(CrewCoverage.of(unit, plan), unitStatus);
    }

    /**
     * Whether someone counts as assigned on the Personnel page. A tech is assigned while they maintain any unit, even
     * though they crew none.
     *
     * @param person a person
     * @param plan   the staged plan, whose changes count toward whether the person crews or maintains a unit
     *
     * @return {@code true} if the person crews a unit, or is a tech who maintains one
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isAssigned(Person person, AssignmentPlan plan) {
        return (plan.plannedUnitOf(person) != null)
                     || (person.isTech() && !plan.plannedTechUnits(person).isEmpty());
    }

    /**
     * Whether the Personnel page's Role filter offers a role: the combat roles, and the tech roles (MekTech, Mechanic,
     * AeroTek and BA Tech), so techs can be found when picking someone to maintain a unit.
     *
     * @param role a personnel role
     *
     * @return {@code true} if the role has an entry in the Role filter
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isFilterableRole(PersonnelRole role) {
        return role.isCombat() || role.isTechSecondary();
    }

    /**
     * Whether a role is a tech who can take a vehicle's crew seat: MekTech, Mechanic, AeroTek, BA Tech or AsTech.
     *
     * @param role a personnel role
     *
     * @return {@code true} if the role belongs to {@link RoleGroup#ALL_TECHS}
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isVehicleTech(PersonnelRole role) {
        return role.isVehicleCrewExtended() && (role.isTechSecondary() || role.isAstech());
    }

    /**
     * Whether a role is a non-combat, non-tech profession that can take a vehicle's crew seat, such as a doctor,
     * administrator, comms operator or chef.
     *
     * @param role a personnel role
     *
     * @return {@code true} if the role belongs to {@link RoleGroup#OTHER_PROFESSIONS}
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isOtherVehicleProfession(PersonnelRole role) {
        return role.isVehicleCrewExtended() && !role.isCombat() && !isVehicleTech(role);
    }

    /**
     * @param person a person
     * @param filter a Role filter entry
     *
     * @return {@code true} if the person's primary or secondary role matches the filter
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean matches(Person person, RoleFilter filter) {
        return filter.matches(person.getPrimaryRole()) || filter.matches(person.getSecondaryRole());
    }

    /**
     * @param entry a person
     * @param plan  the staged plan, whose changes count toward whether the person crews a unit
     *
     * @return {@code true} if the Personnel page should list the person
     */
    boolean shows(PersonEntry entry, AssignmentPlan plan) {
        Person person = entry.person();
        boolean isAssigned = isAssigned(person, plan);
        if (((assignment == Assignment.UNASSIGNED) && isAssigned)
                  || ((assignment == Assignment.ASSIGNED) && !isAssigned)) {
            return false;
        }
        if ((role != null) && !matches(person, role)) {
            return false;
        }
        if (!matches(entry, condition)) {
            return false;
        }
        if ((familiarity != FamiliarityBand.ANY)
                  && !matches(familiarityScore(person, familiarityChassis()), familiarity)) {
            return false;
        }
        if ((qualifiedFor != null) && !plan.couldTake(person, qualifiedFor.unit(), qualifiedFor.seat())
                  && !qualifiedFor.unit().equals(plan.plannedUnitOf(person))) {
            return false;
        }
        if ((techFor != null) && !person.equals(plan.plannedTechOf(techFor)) && !plan.canStageTech(person, techFor)) {
            return false;
        }
        return matches(entry.searchText(), peopleQuery);
    }

    /**
     * How well a unit is crewed once the plan is applied: no one at all, some places empty, every place filled with
     * temporary crew among them, or every place filled by real people.
     */
    enum Crewing {
        EMPTY, PARTIAL, TEMP, FULL
    }

    static boolean matches(CrewCoverage.UnitCoverage coverage, UnitStatus status) {
        Crewing crewing = coverage.crewing();
        return switch (status) {
            case NEEDS_CREW -> (crewing == Crewing.EMPTY) || (crewing == Crewing.PARTIAL);
            case UNMANNED -> crewing == Crewing.EMPTY;
            case ON_TEMP_CREW -> coverage.temp() > 0;
            // Needing a tech is decided from the unit and the plan, in shows()
            case NEEDS_TECH -> true;
            case FULLY_CREWED -> (crewing == Crewing.FULL) || (crewing == Crewing.TEMP);
            case ALL -> true;
        };
    }

    /**
     * @param entry     a person, whose hits are their {@link Person#getTotalInjurySeverity()}
     * @param condition the Personnel page's condition filter
     *
     * @return {@code true} if the person matches the condition filter
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean matches(PersonEntry entry, Condition condition) {
        boolean isWounded = entry.hits() > 0;
        boolean isPregnant = entry.person().isPregnant();
        return switch (condition) {
            case ANY -> true;
            case FIT -> !isWounded && !isPregnant;
            case WOUNDED -> isWounded;
            case PREGNANT -> isPregnant;
        };
    }

    /**
     * Counts the filters on a page's side rail set away from where the page starts, for when the rail is hidden.
     * Starting choices, such as Unassigned on the Personnel page, are not counted.
     *
     * @param page the page whose rail filters to count
     *
     * @return how many of that page's rail filters are set
     *
     * @author Illiani
     * @since 0.51.01
     */
    int activeRailFilters(CrewAssignmentPage page) {
        int active = 0;
        switch (page) {
            case UNITS -> {
                if (unitType != null) {
                    active++;
                }
                if (formation != null) {
                    active++;
                }
            }
            case PERSONNEL -> {
                if (assignment != Assignment.UNASSIGNED) {
                    active++;
                }
                if (role != null) {
                    active++;
                }
                if (condition != Condition.ANY) {
                    active++;
                }
                if (familiarity != FamiliarityBand.ANY) {
                    active++;
                }
            }
            case REVIEW -> {
                // The Review page's rail has no filters
            }
        }
        return active;
    }

    /**
     * The unit whose chassis the familiarity filter measures against: the unit a seat or tech is being found for, or
     * else the one unit selected. With none of those, or several units selected, there is no one chassis to measure.
     *
     * @return the unit, or {@code null} to measure each person's best familiarity with any chassis
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nullable Unit familiarityUnit() {
        if (qualifiedFor != null) {
            return qualifiedFor.unit();
        }
        if (techFor != null) {
            return techFor;
        }
        if (selectedUnits.size() == 1) {
            return selectedUnits.iterator().next();
        }
        return null;
    }

    /**
     * @return the chassis of {@link #familiarityUnit()}, or {@code null} to measure each person's best familiarity
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nullable String familiarityChassis() {
        Unit unit = familiarityUnit();
        Entity entity = (unit == null) ? null : unit.getEntity();
        return (entity == null) ? null : entity.getChassis();
    }

    /**
     * @param person  a person
     * @param chassis the chassis to measure against, or {@code null} for the person's best familiarity with any
     *                chassis
     *
     * @return the person's familiarity with the chassis, 0 if they have none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int familiarityScore(Person person, @Nullable String chassis) {
        if (chassis != null) {
            return person.getChassisFamiliarity(chassis);
        }
        int best = 0;
        for (int score : person.getChassisFamiliarity().values()) {
            best = Math.max(best, score);
        }
        return best;
    }

    /**
     * @param score a person's familiarity
     * @param band  the Personnel page's familiarity filter
     *
     * @return {@code true} if the score falls in the band
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean matches(int score, FamiliarityBand band) {
        return switch (band) {
            case ANY -> true;
            case UNDER_100 -> score < FamiliarityBand.AT_LEAST_100.getThreshold();
            case AT_LEAST_100, AT_LEAST_200, AT_LEAST_300 -> score >= band.getThreshold();
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
