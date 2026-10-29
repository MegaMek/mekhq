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
package mekhq.campaign.unit.crewAssignment;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import jakarta.annotation.Nullable;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * A set of crew changes the player has staged but not yet applied. Nothing here touches a real {@link Unit} or
 * {@link Person}: the plan keeps its own count of how each seat, tech and temporary crew would change, so it can be
 * built up, undone and thrown away freely. {@link CrewAssignmentService#commit} applies it.
 *
 * <p>The plan holds three kinds of change:</p>
 * <ul>
 *     <li><b>Crew moves</b> ({@link StagedAssignment}): a person into a seat, or out of their unit altogether. Each
 *     person has at most one; staging them again replaces it, so it always runs from where they are now to where they
 *     will end up, and staging them back into the seat they hold now cancels it.</li>
 *     <li><b>Tech changes</b> ({@link StagedTech}): who maintains a unit. Each unit has at most one. A tech keeps no
 *     seat and may maintain several units, as long as their daily tech time covers them all.</li>
 *     <li><b>Temporary crew changes</b> ({@link StagedTempCrew}): filling a unit with temporary ("blob") crew from the
 *     campaign's pool, or removing them. Each unit has at most one.</li>
 * </ul>
 *
 * <p>Every action (a bulk stage, an auto-fill, a removal) is one step of history, so {@link #undo()} reverts a whole
 * bulk action at once, whichever kinds of change it made.</p>
 *
 * <p>The plan reads each unit's seats once and keeps them, because the campaign is not expected to change while the
 * plan is open. Call {@link #refreshSeats()} if it may have.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class AssignmentPlan {
    // region Records

    /**
     * One staged crew move.
     *
     * @param person   the person being moved
     * @param fromUnit the unit they crew now, or {@code null} if they are unassigned
     * @param fromSeat the seat they hold now, or {@code null} if they are unassigned
     * @param toUnit   the unit they will crew, or {@code null} to take them out of their unit
     * @param toSeat   the seat they will hold, or {@code null} with a {@code null} unit
     */
    public record StagedAssignment(Person person, @Nullable Unit fromUnit, @Nullable SeatType fromSeat,
          @Nullable Unit toUnit, @Nullable SeatType toSeat) {
        /**
         * @return {@code true} if this change moves the person out of a seat they hold now
         */
        public boolean isReassignment() {
            return fromUnit != null;
        }

        /**
         * @return {@code true} if this change only takes the person out of their unit
         */
        public boolean isRemoval() {
            return toUnit == null;
        }
    }

    /**
     * One staged change of who maintains a unit.
     *
     * @param unit     the unit
     * @param fromTech the tech who maintains it now, or {@code null} for none
     * @param toTech   the tech who will maintain it, or {@code null} to leave it without one
     */
    public record StagedTech(Unit unit, @Nullable Person fromTech, @Nullable Person toTech) {}

    /**
     * One staged change of a unit's temporary crew.
     *
     * @param unit   the unit
     * @param before each role's temporary crew now, for the roles that change
     * @param after  each role's temporary crew once the change is applied, for the same roles
     */
    public record StagedTempCrew(Unit unit, Map<PersonnelRole, Integer> before, Map<PersonnelRole, Integer> after) {
        public StagedTempCrew {
            before = Map.copyOf(before);
            after = Map.copyOf(after);
        }

        /** @return how many temporary crew the change adds, negative if it removes them */
        public int change() {
            return after.values().stream().mapToInt(Integer::intValue).sum()
                         - before.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /**
     * What a bulk crew stage did.
     *
     * @param staged   the changes it made, in order
     * @param leftOver the people it could not place, because no listed unit had a free seat they qualify for
     */
    public record StageResult(List<StagedAssignment> staged, List<Person> leftOver) {
        public StageResult {
            staged = List.copyOf(staged);
            leftOver = List.copyOf(leftOver);
        }
    }

    /**
     * What a bulk tech stage did.
     *
     * @param staged   the changes it made, in order
     * @param leftOver the units it could not find a tech for
     */
    public record TechStageResult(List<StagedTech> staged, List<Unit> leftOver) {
        public TechStageResult {
            staged = List.copyOf(staged);
            leftOver = List.copyOf(leftOver);
        }
    }

    /**
     * What a temporary crew fill or removal did.
     *
     * @param staged   the changes it made
     * @param leftOver the units it could not change: a fill needs a real crew member aboard and temporary crew in the
     *                 pool, and a removal needs temporary crew to remove
     */
    public record TempCrewResult(List<StagedTempCrew> staged, List<Unit> leftOver) {
        public TempCrewResult {
            staged = List.copyOf(staged);
            leftOver = List.copyOf(leftOver);
        }
    }

    /**
     * Something that would stop a staged crew move from being applied.
     *
     * @param change the change
     * @param reason why it cannot be applied
     */
    public record Problem(StagedAssignment change, Reason reason) {}

    /**
     * Something that would stop a staged tech change from being applied.
     *
     * @param change the change
     * @param reason why it cannot be applied
     */
    public record TechProblem(StagedTech change, Reason reason) {}

    /**
     * Something that would stop a staged temporary crew change from being applied.
     *
     * @param change the change
     * @param reason why it cannot be applied
     */
    public record TempCrewProblem(StagedTempCrew change, Reason reason) {}

    /** Why a staged change cannot be applied. */
    public enum Reason {
        /** The person is no longer active, employed and free, or has been deployed. */
        NOT_ASSIGNABLE,
        /** The person is not qualified for the seat, or cannot maintain the unit. */
        NOT_QUALIFIED,
        /** The unit is deployed, refitting, mothballed or not present. */
        UNIT_UNAVAILABLE,
        /** The person and the unit are not at the same location. */
        DIFFERENT_LOCATION,
        /** More people are staged into the seat than it holds. */
        SEAT_FULL,
        /** The tech's daily time does not cover every unit staged to them. */
        NO_TECH_TIME,
        /** The person, the unit's tech or the unit's temporary crew have changed since the change was staged. */
        OUT_OF_DATE,
        /**
         * The seat had no room, or the unit refused the person, when the change was applied. The person was returned
         * to the seat they held before.
         */
        REFUSED
    }

    // endregion Records

    private record SeatKey(UUID unitId, SeatType type) {}

    private final boolean useAltAdvancedMedical;
    private final boolean useImplants;
    private final boolean techsUseAdministration;
    private Predicate<Person> isTechBusy = person -> false;
    private final Map<UUID, List<CrewSeat>> seatsByUnit = new HashMap<>();
    private final Map<UUID, StagedAssignment> changes = new LinkedHashMap<>();
    /** How each seat's head count would change: +1 for each staged arrival, -1 for each staged departure. */
    private final Map<SeatKey, Integer> seatChanges = new HashMap<>();
    private final Map<UUID, StagedTech> techChanges = new LinkedHashMap<>();
    private final Map<UUID, StagedTempCrew> tempCrewChanges = new LinkedHashMap<>();
    /** Each step of history is the list of reverts its edits need, applied last first. */
    private final Deque<List<Runnable>> history = new ArrayDeque<>();

    /**
     * @param useAltAdvancedMedical the campaign's alternative advanced medical option, which affects who can drive
     * @param useImplants           the campaign's implants option, which affects who can drive
     */
    public AssignmentPlan(final boolean useAltAdvancedMedical, final boolean useImplants) {
        this(useAltAdvancedMedical, useImplants, false);
    }

    /**
     * @param useAltAdvancedMedical  the campaign's alternative advanced medical option, which affects who can drive
     * @param useImplants            the campaign's implants option, which affects who can drive
     * @param techsUseAdministration the campaign's option that lets a tech's administration skill add to their daily
     *                               tech time
     */
    public AssignmentPlan(final boolean useAltAdvancedMedical, final boolean useImplants,
          final boolean techsUseAdministration) {
        this.useAltAdvancedMedical = useAltAdvancedMedical;
        this.useImplants = useImplants;
        this.techsUseAdministration = techsUseAdministration;
    }

    /**
     * @param isTechBusy says whether a tech is tied up, for example on a refit, and so cannot take on more units
     */
    public void setTechBusy(final Predicate<Person> isTechBusy) {
        this.isTechBusy = Objects.requireNonNull(isTechBusy);
    }

    // region Reading the plan

    /**
     * @param unit the unit
     *
     * @return the unit's seats as they are now, before any staged change
     */
    public List<CrewSeat> seatsOf(final Unit unit) {
        return seatsByUnit.computeIfAbsent(unit.getId(), id -> CrewSeatLayout.forUnit(unit));
    }

    /**
     * @param unit the unit
     * @param type the seat
     *
     * @return how many more people the seat could take once the staged changes are applied; negative if more are
     *       staged into it than it holds
     */
    public int openSeats(final Unit unit, final SeatType type) {
        for (CrewSeat seat : seatsOf(unit)) {
            if (seat.type() == type) {
                return seat.openSeats() - seatChanges.getOrDefault(new SeatKey(unit.getId(), type), 0);
            }
        }
        return 0;
    }

    /**
     * @param unit the unit
     *
     * @return how many more people all of the unit's seats could take once the staged changes are applied
     */
    public int openSeats(final Unit unit) {
        int open = 0;
        for (CrewSeat seat : seatsOf(unit)) {
            open += Math.max(0, openSeats(unit, seat.type()));
        }
        return open;
    }

    /**
     * @param unit the unit
     *
     * @return how many real people will crew the unit once the staged changes are applied
     */
    public int plannedRealCrew(final Unit unit) {
        int crew = 0;
        for (CrewSeat seat : seatsOf(unit)) {
            crew += seat.capacity() - openSeats(unit, seat.type());
        }
        return Math.max(0, crew);
    }

    /**
     * @param person the person
     *
     * @return the unit the person will crew once the staged changes are applied, or {@code null} for none
     */
    public @Nullable Unit plannedUnitOf(final Person person) {
        final StagedAssignment change = changes.get(person.getId());
        return (change == null) ? person.getUnit() : change.toUnit();
    }

    /**
     * @param person the person
     *
     * @return the person's staged crew move, or {@code null} if they have none
     */
    public @Nullable StagedAssignment changeFor(final Person person) {
        return changes.get(person.getId());
    }

    /**
     * @return the staged crew moves, in the order they were first staged
     */
    public List<StagedAssignment> changes() {
        return List.copyOf(changes.values());
    }

    /**
     * @return the staged tech changes, in the order they were first staged
     */
    public List<StagedTech> techChanges() {
        return List.copyOf(techChanges.values());
    }

    /**
     * @return the staged temporary crew changes, in the order they were first staged
     */
    public List<StagedTempCrew> tempCrewChanges() {
        return List.copyOf(tempCrewChanges.values());
    }

    /** @return how many changes of every kind are staged */
    public int size() {
        return changes.size() + techChanges.size() + tempCrewChanges.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    /**
     * Whether a person could be staged into a seat right now: they qualify for it, it has room once the staged changes
     * are counted, the unit is available, both are at the same location, and it is not the unit they will already
     * crew.
     *
     * @param person the person
     * @param unit   the unit
     * @param type   the seat
     *
     * @return {@code true} if staging them there would succeed
     */
    public boolean canStage(final Person person, final Unit unit, final SeatType type) {
        return !Objects.equals(plannedUnitOf(person), unit)
                     && unit.isAvailable()
                     && (openSeats(unit, type) > 0)
                     && SeatEligibility.canTake(person, unit, type, useAltAdvancedMedical, useImplants)
                     && LocationUtils.areSameEffectiveLocation(unit, person);
    }

    /**
     * @param person the person
     * @param unit   the unit
     *
     * @return the first of the unit's seats, in display order, the person could be staged into, or {@code null} if
     *       none
     */
    public @Nullable SeatType seatFor(final Person person, final Unit unit) {
        for (CrewSeat seat : seatsOf(unit)) {
            if (canStage(person, unit, seat.type())) {
                return seat.type();
            }
        }
        return null;
    }

    /**
     * @param person the person
     *
     * @return {@code true} if the person will crew a unit once the plan is applied and can be taken out of it now
     */
    public boolean canUnassign(final Person person) {
        final Unit unit = plannedUnitOf(person);
        return (unit != null) && !person.isDeployed() && unit.isAvailable();
    }

    // endregion Reading the plan

    // region Reading techs

    /**
     * @param unit the unit
     *
     * @return the tech who will maintain the unit once the staged changes are applied, or {@code null} for none
     */
    public @Nullable Person plannedTechOf(final Unit unit) {
        final StagedTech change = techChanges.get(unit.getId());
        return (change == null) ? unit.getTech() : change.toTech();
    }

    /**
     * @param unit the unit
     *
     * @return the unit's staged tech change, or {@code null} if it has none
     */
    public @Nullable StagedTech techChangeFor(final Unit unit) {
        return techChanges.get(unit.getId());
    }

    /**
     * @param tech a tech
     *
     * @return the units the tech will maintain once the staged changes are applied
     */
    public List<Unit> plannedTechUnits(final Person tech) {
        final List<Unit> units = new ArrayList<>();
        for (Unit unit : tech.getTechUnits()) {
            final StagedTech change = techChanges.get(unit.getId());
            if ((change == null) || tech.equals(change.toTech())) {
                units.add(unit);
            }
        }
        for (StagedTech change : techChanges.values()) {
            if (tech.equals(change.toTech()) && !units.contains(change.unit())) {
                units.add(change.unit());
            }
        }
        return units;
    }

    /**
     * @param tech a tech
     *
     * @return the minutes of daily maintenance the tech's units will take once the staged changes are applied
     */
    public int plannedTechMinutes(final Person tech) {
        int minutes = tech.getMaintenanceTimeUsing();
        for (StagedTech change : techChanges.values()) {
            final int unitMinutes = change.unit().getMaintenanceTime();
            if (tech.equals(change.fromTech()) && !tech.equals(change.toTech())) {
                minutes -= unitMinutes;
            } else if (tech.equals(change.toTech()) && !tech.equals(change.fromTech())) {
                minutes += unitMinutes;
            }
        }
        return minutes;
    }

    /**
     * @param tech a tech
     *
     * @return the tech's daily tech time
     */
    public int techMinutesAvailable(final Person tech) {
        return tech.getDailyAvailableTechTime(techsUseAdministration);
    }

    /**
     * Whether a tech could be staged to maintain a unit right now: the unit needs maintenance and is available, the
     * tech can work on it, is at its location and is not tied up, the tech is not already staged to it, and their daily
     * time would cover it.
     *
     * @param tech a tech
     * @param unit the unit
     *
     * @return {@code true} if staging them there would succeed
     */
    public boolean canStageTech(final Person tech, final Unit unit) {
        return !tech.equals(plannedTechOf(unit))
                     && unit.isAvailable()
                     && SeatEligibility.canTech(tech, unit)
                     && !isTechBusy.test(tech)
                     && LocationUtils.areSameEffectiveLocation(unit, tech)
                     && ((plannedTechMinutes(tech) + unit.getMaintenanceTime()) <= techMinutesAvailable(tech));
    }

    // endregion Reading techs

    // region Reading temporary crew

    /**
     * @param unit the unit
     * @param role a temporary crew role
     *
     * @return the unit's temporary crew of that role once the staged changes are applied
     */
    public int plannedTempCrew(final Unit unit, final PersonnelRole role) {
        final StagedTempCrew change = tempCrewChanges.get(unit.getId());
        if ((change != null) && change.after().containsKey(role)) {
            return change.after().get(role);
        }
        return unit.getTempCrewByPersonnelRole(role);
    }

    /**
     * @param unit the unit
     *
     * @return all the unit's temporary crew once the staged changes are applied
     */
    public int plannedTotalTempCrew(final Unit unit) {
        final StagedTempCrew change = tempCrewChanges.get(unit.getId());
        return unit.getTotalTempCrew() + ((change == null) ? 0 : change.change());
    }

    /**
     * @param unit the unit
     * @param seat one of its seats
     *
     * @return the temporary crew counted against the seat once the staged changes are applied, where the unit keeps
     *       them per seat; see {@link CrewSeat}
     */
    public int plannedTempCrew(final Unit unit, final CrewSeat seat) {
        return (seat.tempRole() == null) ? 0 : plannedTempCrew(unit, seat.tempRole());
    }

    /**
     * @param role          a temporary crew role
     * @param availablePool the campaign's free temporary crew of each role, before any staged change
     *
     * @return the free temporary crew of that role once the staged changes are applied
     */
    public int plannedTempCrewPool(final PersonnelRole role, final ToIntFunction<PersonnelRole> availablePool) {
        int pool = availablePool.applyAsInt(role);
        for (StagedTempCrew change : tempCrewChanges.values()) {
            pool -= change.after().getOrDefault(role, 0) - change.before().getOrDefault(role, 0);
        }
        return pool;
    }

    // endregion Reading temporary crew

    // region Changing crew

    /**
     * Stages each person, in the order given, into the first unit in the order given that has a seat they can take.
     * The whole call is one step of history.
     *
     * @param people the people, best first if the caller wants the best people to get the first units
     * @param units  the units, in the order they should be filled
     *
     * @return what was staged and who was left over
     */
    public StageResult stage(final List<Person> people, final List<Unit> units) {
        final List<Runnable> reverts = new ArrayList<>();
        final List<StagedAssignment> staged = new ArrayList<>();
        final List<Person> leftOver = new ArrayList<>();

        for (Person person : people) {
            boolean placed = false;
            for (Unit unit : units) {
                final SeatType seat = seatFor(person, unit);
                if (seat != null) {
                    // A null change means the person went back into the seat they hold now, cancelling their change
                    final StagedAssignment change = put(person, unit, seat, reverts);
                    if (change != null) {
                        staged.add(change);
                    }
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                leftOver.add(person);
            }
        }

        record(reverts);
        return new StageResult(staged, leftOver);
    }

    /**
     * Stages one person into one seat, as one step of history.
     *
     * @param person the person
     * @param unit   the unit
     * @param type   the seat
     *
     * @return {@code true} if the person was staged; {@code false} if {@link #canStage} says they cannot be
     */
    public boolean stage(final Person person, final Unit unit, final SeatType type) {
        if (!canStage(person, unit, type)) {
            return false;
        }
        final List<Runnable> reverts = new ArrayList<>();
        put(person, unit, type, reverts);
        record(reverts);
        return true;
    }

    /**
     * Stages taking each person out of the unit they will crew, as one step of history. People who will crew no unit,
     * or are deployed, are left out.
     *
     * @param people the people
     *
     * @return what was staged, and who could not be taken out
     */
    public StageResult unassign(final List<Person> people) {
        final List<Runnable> reverts = new ArrayList<>();
        final List<StagedAssignment> staged = new ArrayList<>();
        final List<Person> leftOver = new ArrayList<>();
        for (Person person : people) {
            if (canUnassign(person)) {
                final StagedAssignment change = put(person, null, null, reverts);
                if (change != null) {
                    staged.add(change);
                }
            } else {
                leftOver.add(person);
            }
        }
        record(reverts);
        return new StageResult(staged, leftOver);
    }

    /**
     * Stages taking every crew member out of each unit, and, if asked, its tech too, as one step of history.
     *
     * @param units      the units
     * @param alsoTechs  {@code true} to also leave each unit without a tech
     *
     * @return how many changes were staged
     */
    public int clearUnits(final List<Unit> units, final boolean alsoTechs) {
        final List<Runnable> reverts = new ArrayList<>();
        int staged = 0;
        for (Unit unit : units) {
            if (!unit.isAvailable()) {
                continue;
            }
            for (Person person : plannedCrewOf(unit)) {
                if (canUnassign(person) && (put(person, null, null, reverts) != null)) {
                    staged++;
                }
            }
            if (alsoTechs && (plannedTechOf(unit) != null) && (putTech(unit, null, reverts) != null)) {
                staged++;
            }
        }
        record(reverts);
        return staged;
    }

    /**
     * @param unit the unit
     *
     * @return the people who will crew the unit once the staged changes are applied
     */
    public List<Person> plannedCrewOf(final Unit unit) {
        final List<Person> crew = new ArrayList<>();
        for (CrewSeat seat : seatsOf(unit)) {
            for (Person person : seat.occupants()) {
                if (unit.equals(plannedUnitOf(person)) && !crew.contains(person)) {
                    crew.add(person);
                }
            }
        }
        for (StagedAssignment change : changes.values()) {
            if (unit.equals(change.toUnit()) && !crew.contains(change.person())) {
                crew.add(change.person());
            }
        }
        return crew;
    }

    /**
     * Takes a person's staged crew move out of the plan, as one step of history, so they stay where they are now. This
     * is refused when the seat they would go back to has been staged to someone else in the meantime.
     *
     * @param person the person
     *
     * @return {@code true} if a change was removed
     */
    public boolean remove(final Person person) {
        final StagedAssignment change = changes.get(person.getId());
        if (change == null) {
            return false;
        }
        if ((change.fromUnit() != null) && (change.fromSeat() != null)
                  && (openSeats(change.fromUnit(), change.fromSeat()) < 1)) {
            return false;
        }
        final List<Runnable> reverts = new ArrayList<>();
        replace(person, change, null, reverts);
        record(reverts);
        return true;
    }

    // endregion Changing crew

    // region Changing techs

    /**
     * Stages one tech to maintain one unit, replacing any tech it has, as one step of history.
     *
     * @param tech the tech
     * @param unit the unit
     *
     * @return {@code true} if the tech was staged; {@code false} if {@link #canStageTech} says they cannot be
     */
    public boolean stageTech(final Person tech, final Unit unit) {
        if (!canStageTech(tech, unit)) {
            return false;
        }
        final List<Runnable> reverts = new ArrayList<>();
        putTech(unit, tech, reverts);
        record(reverts);
        return true;
    }

    /**
     * Gives each unit without a tech the first tech, in the order given, who can take it on, as one step of history.
     * Units that will already have a tech are left alone, so best-first techs fill units until their time runs out.
     *
     * @param techs the techs, best first
     * @param units the units, in the order they should be given techs
     *
     * @return what was staged, and the units left without a tech
     */
    public TechStageResult stageTechs(final List<Person> techs, final List<Unit> units) {
        final List<Runnable> reverts = new ArrayList<>();
        final List<StagedTech> staged = new ArrayList<>();
        final List<Unit> leftOver = new ArrayList<>();
        for (Unit unit : units) {
            if ((plannedTechOf(unit) != null) || !SeatEligibility.takesTech(unit)) {
                continue;
            }
            Person chosen = null;
            for (Person tech : techs) {
                if (canStageTech(tech, unit)) {
                    chosen = tech;
                    break;
                }
            }
            if (chosen == null) {
                leftOver.add(unit);
            } else {
                final StagedTech change = putTech(unit, chosen, reverts);
                if (change != null) {
                    staged.add(change);
                }
            }
        }
        record(reverts);
        return new TechStageResult(staged, leftOver);
    }

    /**
     * Stages leaving each unit, or every unit a tech maintains, without a tech, as one step of history.
     *
     * @param units the units
     *
     * @return how many changes were staged
     */
    public int unassignTechs(final List<Unit> units) {
        final List<Runnable> reverts = new ArrayList<>();
        int staged = 0;
        for (Unit unit : units) {
            if (unit.isAvailable() && (plannedTechOf(unit) != null) && (putTech(unit, null, reverts) != null)) {
                staged++;
            }
        }
        record(reverts);
        return staged;
    }

    /**
     * Stages taking each person out of their unit and off every unit they maintain, as one step of history: the old
     * right-click "None".
     *
     * @param people the people
     *
     * @return how many changes were staged
     */
    public int unassignEverything(final List<Person> people) {
        final List<Runnable> reverts = new ArrayList<>();
        int staged = 0;
        for (Person person : people) {
            if (canUnassign(person) && (put(person, null, null, reverts) != null)) {
                staged++;
            }
            for (Unit unit : plannedTechUnits(person)) {
                if (unit.isAvailable() && (putTech(unit, null, reverts) != null)) {
                    staged++;
                }
            }
        }
        record(reverts);
        return staged;
    }

    /**
     * Takes a unit's staged tech change out of the plan, as one step of history, so its tech stays as it is now. This
     * is refused when the unit's current tech no longer has the time to take it back.
     *
     * @param unit the unit
     *
     * @return {@code true} if a change was removed
     */
    public boolean removeTechChange(final Unit unit) {
        final StagedTech change = techChanges.get(unit.getId());
        if (change == null) {
            return false;
        }
        final Person fromTech = change.fromTech();
        if ((fromTech != null)
                  && ((plannedTechMinutes(fromTech) + unit.getMaintenanceTime()) > techMinutesAvailable(fromTech))) {
            return false;
        }
        final List<Runnable> reverts = new ArrayList<>();
        replaceTech(unit, change, null, reverts);
        record(reverts);
        return true;
    }

    // endregion Changing techs

    // region Changing temporary crew

    /**
     * Stages filling each unit with temporary crew of its driver role, up to full strength, as one step of history. As
     * the Hangar's "Fill with temp crew" requires, a unit needs at least one real crew member, counting staged
     * arrivals, and the campaign's pool must have temporary crew of that role to spare; units are filled in order until
     * the pool runs out.
     *
     * @param units         the units
     * @param availablePool the campaign's free temporary crew of each role, before any staged change; 0 for a role
     *                      the campaign does not use temporary crew for
     *
     * @return what was staged, and the units that could not be filled
     */
    public TempCrewResult fillTempCrew(final List<Unit> units, final ToIntFunction<PersonnelRole> availablePool) {
        final List<Runnable> reverts = new ArrayList<>();
        final List<StagedTempCrew> staged = new ArrayList<>();
        final List<Unit> leftOver = new ArrayList<>();
        for (Unit unit : units) {
            final PersonnelRole role = unit.getDriverRole();
            final int realCrew = plannedRealCrew(unit);
            if (!unit.isAvailable() || (role == null) || (realCrew == 0)) {
                leftOver.add(unit);
                continue;
            }
            final int current = plannedTempCrew(unit, role);
            final int otherTemp = plannedTotalTempCrew(unit) - current;
            final int wanted = Math.max(0, unit.getFullCrewSize() - realCrew - otherTemp);
            final int added = Math.min(wanted - current, plannedTempCrewPool(role, availablePool));
            if (added <= 0) {
                leftOver.add(unit);
                continue;
            }
            final Map<PersonnelRole, Integer> after = new EnumMap<>(PersonnelRole.class);
            after.put(role, current + added);
            final StagedTempCrew change = putTempCrew(unit, after, reverts);
            if (change != null) {
                staged.add(change);
            }
        }
        record(reverts);
        return new TempCrewResult(staged, leftOver);
    }

    /**
     * Stages removing every temporary crew member from each unit, as one step of history.
     *
     * @param units the units
     *
     * @return what was staged, and the units with no temporary crew to remove
     */
    public TempCrewResult removeTempCrew(final List<Unit> units) {
        final List<Runnable> reverts = new ArrayList<>();
        final List<StagedTempCrew> staged = new ArrayList<>();
        final List<Unit> leftOver = new ArrayList<>();
        for (Unit unit : units) {
            if (!unit.isAvailable() || (plannedTotalTempCrew(unit) == 0)) {
                leftOver.add(unit);
                continue;
            }
            final Map<PersonnelRole, Integer> after = new EnumMap<>(PersonnelRole.class);
            for (PersonnelRole role : PersonnelRole.values()) {
                if (plannedTempCrew(unit, role) > 0) {
                    after.put(role, 0);
                }
            }
            final StagedTempCrew change = putTempCrew(unit, after, reverts);
            if (change != null) {
                staged.add(change);
            }
        }
        record(reverts);
        return new TempCrewResult(staged, leftOver);
    }

    /**
     * Takes a unit's staged temporary crew change out of the plan, as one step of history.
     *
     * @param unit the unit
     *
     * @return {@code true} if a change was removed
     */
    public boolean removeTempCrewChange(final Unit unit) {
        final StagedTempCrew change = tempCrewChanges.get(unit.getId());
        if (change == null) {
            return false;
        }
        final List<Runnable> reverts = new ArrayList<>();
        tempCrewChanges.remove(unit.getId());
        reverts.add(() -> tempCrewChanges.put(unit.getId(), change));
        record(reverts);
        return true;
    }

    // endregion Changing temporary crew

    // region History

    /**
     * Reverts the most recent step of history.
     *
     * @return {@code true} if there was a step to revert
     */
    public boolean undo() {
        final List<Runnable> reverts = history.poll();
        if (reverts == null) {
            return false;
        }
        for (int i = reverts.size() - 1; i >= 0; i--) {
            reverts.get(i).run();
        }
        return true;
    }

    /** Throws away every staged change and all history. */
    public void clear() {
        changes.clear();
        seatChanges.clear();
        techChanges.clear();
        tempCrewChanges.clear();
        history.clear();
    }

    /**
     * Forgets the seats read so far, so they are read again from the units. Use this if the campaign may have changed
     * while the plan was open.
     */
    public void refreshSeats() {
        seatsByUnit.clear();
    }

    private void record(final List<Runnable> reverts) {
        if (!reverts.isEmpty()) {
            history.push(reverts);
        }
    }

    // endregion History

    // region Edits

    /**
     * Records a person's move into a seat, or out of their unit when {@code unit} is {@code null}, replacing any move
     * they already had.
     *
     * @return the new change, or {@code null} if the move leaves them where they are now
     */
    private @Nullable StagedAssignment put(final Person person, final @Nullable Unit unit,
          final @Nullable SeatType type, final List<Runnable> reverts) {
        final StagedAssignment before = changes.get(person.getId());
        final Unit fromUnit = person.getUnit();
        final SeatType fromSeat = currentSeatOf(person);
        final boolean staysPut = Objects.equals(unit, fromUnit) && (type == fromSeat);
        final StagedAssignment after = staysPut ? null :
                                             new StagedAssignment(person, fromUnit, fromSeat, unit, type);
        replace(person, before, after, reverts);
        return after;
    }

    /** Swaps a person's staged move, and records how to swap it back. */
    private void replace(final Person person, final @Nullable StagedAssignment before,
          final @Nullable StagedAssignment after, final List<Runnable> reverts) {
        if (before != null) {
            unapply(before);
        }
        if (after != null) {
            apply(after);
            changes.put(person.getId(), after);
        } else {
            changes.remove(person.getId());
        }
        reverts.add(() -> {
            if (after != null) {
                unapply(after);
            }
            if (before != null) {
                apply(before);
                changes.put(person.getId(), before);
            } else {
                changes.remove(person.getId());
            }
        });
    }

    /**
     * Records who will maintain a unit, replacing any tech change it already had.
     *
     * @return the new change, or {@code null} if the unit keeps the tech it has now
     */
    private @Nullable StagedTech putTech(final Unit unit, final @Nullable Person tech, final List<Runnable> reverts) {
        final StagedTech before = techChanges.get(unit.getId());
        final Person fromTech = unit.getTech();
        final StagedTech after = Objects.equals(tech, fromTech) ? null : new StagedTech(unit, fromTech, tech);
        replaceTech(unit, before, after, reverts);
        return after;
    }

    private void replaceTech(final Unit unit, final @Nullable StagedTech before, final @Nullable StagedTech after,
          final List<Runnable> reverts) {
        if (after != null) {
            techChanges.put(unit.getId(), after);
        } else {
            techChanges.remove(unit.getId());
        }
        reverts.add(() -> {
            if (before != null) {
                techChanges.put(unit.getId(), before);
            } else {
                techChanges.remove(unit.getId());
            }
        });
    }

    /**
     * Records a unit's temporary crew of some roles, replacing any temporary crew change it already had for those
     * roles.
     *
     * @return the new change, or {@code null} if the unit's temporary crew end up as they are now
     */
    private @Nullable StagedTempCrew putTempCrew(final Unit unit, final Map<PersonnelRole, Integer> counts,
          final List<Runnable> reverts) {
        final StagedTempCrew before = tempCrewChanges.get(unit.getId());
        final Map<PersonnelRole, Integer> fromCounts = new EnumMap<>(PersonnelRole.class);
        final Map<PersonnelRole, Integer> toCounts = new EnumMap<>(PersonnelRole.class);
        if (before != null) {
            fromCounts.putAll(before.before());
            toCounts.putAll(before.after());
        }
        for (Map.Entry<PersonnelRole, Integer> entry : counts.entrySet()) {
            fromCounts.putIfAbsent(entry.getKey(), unit.getTempCrewByPersonnelRole(entry.getKey()));
            toCounts.put(entry.getKey(), entry.getValue());
        }
        toCounts.entrySet().removeIf(entry -> entry.getValue().equals(fromCounts.get(entry.getKey())));
        fromCounts.keySet().retainAll(toCounts.keySet());

        final StagedTempCrew after = toCounts.isEmpty() ? null : new StagedTempCrew(unit, fromCounts, toCounts);
        if (after != null) {
            tempCrewChanges.put(unit.getId(), after);
        } else {
            tempCrewChanges.remove(unit.getId());
        }
        reverts.add(() -> {
            if (before != null) {
                tempCrewChanges.put(unit.getId(), before);
            } else {
                tempCrewChanges.remove(unit.getId());
            }
        });
        return after;
    }

    /**
     * @return the seat the person holds now, before any staged change, or {@code null} if they hold none
     */
    private @Nullable SeatType currentSeatOf(final Person person) {
        final Unit unit = person.getUnit();
        if (unit == null) {
            return null;
        }
        for (CrewSeat seat : seatsOf(unit)) {
            if (seat.occupants().contains(person)) {
                return seat.type();
            }
        }
        return null;
    }

    private void apply(final StagedAssignment change) {
        if ((change.toUnit() != null) && (change.toSeat() != null)) {
            adjust(change.toUnit(), change.toSeat(), 1);
        }
        if ((change.fromUnit() != null) && (change.fromSeat() != null)) {
            adjust(change.fromUnit(), change.fromSeat(), -1);
        }
    }

    private void unapply(final StagedAssignment change) {
        if ((change.toUnit() != null) && (change.toSeat() != null)) {
            adjust(change.toUnit(), change.toSeat(), -1);
        }
        if ((change.fromUnit() != null) && (change.fromSeat() != null)) {
            adjust(change.fromUnit(), change.fromSeat(), 1);
        }
    }

    private void adjust(final Unit unit, final SeatType type, final int amount) {
        seatChanges.merge(new SeatKey(unit.getId(), type), amount, Integer::sum);
    }

    // endregion Edits

    // region Validation

    /**
     * Checks every staged crew move against the campaign as it is now.
     *
     * @return the problems found, at most one per change; empty if every crew move can be applied
     */
    public List<Problem> validate() {
        final List<Problem> problems = new ArrayList<>();
        for (StagedAssignment change : changes.values()) {
            final Reason reason = problemWith(change);
            if (reason != null) {
                problems.add(new Problem(change, reason));
            }
        }
        return problems;
    }

    /**
     * @param change a staged crew move
     *
     * @return why it cannot be applied, or {@code null} if it can
     */
    public @Nullable Reason problemWith(final StagedAssignment change) {
        final Person person = change.person();
        final Unit unit = change.toUnit();
        if (!Objects.equals(person.getUnit(), change.fromUnit())) {
            return Reason.OUT_OF_DATE;
        } else if (unit == null) {
            // Taking someone out of their unit only needs them, and the unit they leave, to be free to act
            if (person.isDeployed()) {
                return Reason.NOT_ASSIGNABLE;
            }
            return ((change.fromUnit() != null) && !change.fromUnit().isAvailable()) ? Reason.UNIT_UNAVAILABLE : null;
        } else if (!SeatEligibility.isAssignable(person)) {
            return Reason.NOT_ASSIGNABLE;
        } else if (!SeatEligibility.canTake(person, unit, change.toSeat(), useAltAdvancedMedical, useImplants)) {
            return Reason.NOT_QUALIFIED;
        } else if (!unit.isAvailable()) {
            return Reason.UNIT_UNAVAILABLE;
        } else if (!LocationUtils.areSameEffectiveLocation(unit, person)) {
            return Reason.DIFFERENT_LOCATION;
        } else if (openSeats(unit, change.toSeat()) < 0) {
            return Reason.SEAT_FULL;
        }
        return null;
    }

    /**
     * Checks every staged tech change against the campaign as it is now.
     *
     * @return the problems found, at most one per change
     */
    public List<TechProblem> validateTechs() {
        final List<TechProblem> problems = new ArrayList<>();
        for (StagedTech change : techChanges.values()) {
            final Reason reason = problemWith(change);
            if (reason != null) {
                problems.add(new TechProblem(change, reason));
            }
        }
        return problems;
    }

    /**
     * @param change a staged tech change
     *
     * @return why it cannot be applied, or {@code null} if it can
     */
    public @Nullable Reason problemWith(final StagedTech change) {
        final Unit unit = change.unit();
        final Person tech = change.toTech();
        if (!Objects.equals(unit.getTech(), change.fromTech())) {
            return Reason.OUT_OF_DATE;
        } else if (!unit.isAvailable()) {
            return Reason.UNIT_UNAVAILABLE;
        } else if (tech == null) {
            return null;
        } else if (!SeatEligibility.isAssignable(tech) || isTechBusy.test(tech)) {
            return Reason.NOT_ASSIGNABLE;
        } else if (!SeatEligibility.canTech(tech, unit)) {
            return Reason.NOT_QUALIFIED;
        } else if (!LocationUtils.areSameEffectiveLocation(unit, tech)) {
            return Reason.DIFFERENT_LOCATION;
        } else if (plannedTechMinutes(tech) > techMinutesAvailable(tech)) {
            return Reason.NO_TECH_TIME;
        }
        return null;
    }

    /**
     * Checks every staged temporary crew change against the campaign as it is now.
     *
     * @return the problems found, at most one per change
     */
    public List<TempCrewProblem> validateTempCrew() {
        final List<TempCrewProblem> problems = new ArrayList<>();
        for (StagedTempCrew change : tempCrewChanges.values()) {
            final Reason reason = problemWith(change);
            if (reason != null) {
                problems.add(new TempCrewProblem(change, reason));
            }
        }
        return problems;
    }

    /**
     * @param change a staged temporary crew change
     *
     * @return why it cannot be applied, or {@code null} if it can
     */
    public @Nullable Reason problemWith(final StagedTempCrew change) {
        final Unit unit = change.unit();
        for (Map.Entry<PersonnelRole, Integer> entry : change.before().entrySet()) {
            if (unit.getTempCrewByPersonnelRole(entry.getKey()) != entry.getValue()) {
                return Reason.OUT_OF_DATE;
            }
        }
        return unit.isAvailable() ? null : Reason.UNIT_UNAVAILABLE;
    }

    // endregion Validation
}
