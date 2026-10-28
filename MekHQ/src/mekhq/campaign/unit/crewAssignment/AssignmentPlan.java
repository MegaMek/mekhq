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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.annotation.Nullable;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * A set of crew assignments the player has staged but not yet applied. Nothing here touches a real {@link Unit} or
 * {@link Person}: the plan keeps its own count of how each seat would change, so it can be built up, undone and
 * thrown away freely. {@link CrewAssignmentService#commit} applies it.
 *
 * <p>Each person has at most one staged change. Staging someone who already has one replaces it, so the change always
 * runs from where they are now to where they will end up. Staging someone back into the seat they hold now cancels
 * their change.</p>
 *
 * <p>Every action (a bulk stage, a single stage, a removal) is one step of history, so {@link #undo()} reverts a whole
 * bulk assignment at once.</p>
 *
 * <p>The plan reads each unit's seats once and keeps them, because the campaign is not expected to change while the
 * plan is open. Call {@link #refreshSeats()} if it may have.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class AssignmentPlan {
    /**
     * One staged change.
     *
     * @param person   the person being moved
     * @param fromUnit the unit they crew now, or {@code null} if they are unassigned
     * @param fromSeat the seat they hold now, or {@code null} if they are unassigned
     * @param toUnit   the unit they will crew
     * @param toSeat   the seat they will hold
     */
    public record StagedAssignment(Person person, @Nullable Unit fromUnit, @Nullable SeatType fromSeat, Unit toUnit,
          SeatType toSeat) {
        /**
         * @return {@code true} if this change moves the person out of a seat they hold now
         */
        public boolean isReassignment() {
            return fromUnit != null;
        }
    }

    /**
     * What a bulk stage did.
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
     * Something that would stop a staged change from being applied.
     *
     * @param change the change
     * @param reason why it cannot be applied
     */
    public record Problem(StagedAssignment change, Reason reason) {}

    /** Why a staged change cannot be applied. */
    public enum Reason {
        /** The person is no longer active, employed and free, or has been deployed. */
        NOT_ASSIGNABLE,
        /** The person is not qualified for the seat. */
        NOT_QUALIFIED,
        /** The unit is deployed, refitting, mothballed or not present. */
        UNIT_UNAVAILABLE,
        /** The person and the unit are not at the same location. */
        DIFFERENT_LOCATION,
        /** More people are staged into the seat than it holds. */
        SEAT_FULL,
        /** The person has changed unit since the change was staged. */
        OUT_OF_DATE,
        /**
         * The seat had no room, or the unit refused the person, when the change was applied. The person was returned
         * to the seat they held before.
         */
        REFUSED
    }

    private record SeatKey(UUID unitId, SeatType type) {}

    private record Edit(Person person, @Nullable StagedAssignment before, @Nullable StagedAssignment after) {}

    private final boolean useAltAdvancedMedical;
    private final boolean useImplants;
    private final Map<UUID, List<CrewSeat>> seatsByUnit = new HashMap<>();
    private final Map<UUID, StagedAssignment> changes = new LinkedHashMap<>();
    /** How each seat's head count would change: +1 for each staged arrival, -1 for each staged departure. */
    private final Map<SeatKey, Integer> seatChanges = new HashMap<>();
    private final Deque<List<Edit>> history = new ArrayDeque<>();

    /**
     * @param useAltAdvancedMedical the campaign's alternative advanced medical option, which affects who can drive
     * @param useImplants           the campaign's implants option, which affects who can drive
     */
    public AssignmentPlan(final boolean useAltAdvancedMedical, final boolean useImplants) {
        this.useAltAdvancedMedical = useAltAdvancedMedical;
        this.useImplants = useImplants;
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
     * @return the person's staged change, or {@code null} if they have none
     */
    public @Nullable StagedAssignment changeFor(final Person person) {
        return changes.get(person.getId());
    }

    /**
     * @return the staged changes, in the order they were first staged
     */
    public List<StagedAssignment> changes() {
        return List.copyOf(changes.values());
    }

    public int size() {
        return changes.size();
    }

    public boolean isEmpty() {
        return changes.isEmpty();
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

    // endregion Reading the plan

    // region Changing the plan

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
        final List<Edit> edits = new ArrayList<>();
        final List<StagedAssignment> staged = new ArrayList<>();
        final List<Person> leftOver = new ArrayList<>();

        for (Person person : people) {
            boolean placed = false;
            for (Unit unit : units) {
                final SeatType seat = seatFor(person, unit);
                if (seat != null) {
                    // A null change means the person went back into the seat they hold now, cancelling their change
                    final StagedAssignment change = put(person, unit, seat, edits);
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

        if (!edits.isEmpty()) {
            history.push(edits);
        }
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
        final List<Edit> edits = new ArrayList<>();
        put(person, unit, type, edits);
        history.push(edits);
        return true;
    }

    /**
     * Takes a person's staged change out of the plan, as one step of history, so they stay where they are now. This is
     * refused when the seat they would go back to has been staged to someone else in the meantime.
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
        unapply(change);
        changes.remove(person.getId());
        final List<Edit> edits = new ArrayList<>();
        edits.add(new Edit(person, change, null));
        history.push(edits);
        return true;
    }

    /**
     * Reverts the most recent step of history.
     *
     * @return {@code true} if there was a step to revert
     */
    public boolean undo() {
        final List<Edit> edits = history.poll();
        if (edits == null) {
            return false;
        }
        for (int i = edits.size() - 1; i >= 0; i--) {
            final Edit edit = edits.get(i);
            if (edit.after() != null) {
                unapply(edit.after());
            }
            if (edit.before() != null) {
                apply(edit.before());
                changes.put(edit.person().getId(), edit.before());
            } else {
                changes.remove(edit.person().getId());
            }
        }
        return true;
    }

    /** Throws away every staged change and all history. */
    public void clear() {
        changes.clear();
        seatChanges.clear();
        history.clear();
    }

    /**
     * Forgets the seats read so far, so they are read again from the units. Use this if the campaign may have changed
     * while the plan was open.
     */
    public void refreshSeats() {
        seatsByUnit.clear();
    }

    /**
     * Records a person's move into a seat, replacing any change they already had.
     *
     * @return the new change, or {@code null} if the move puts them back where they are now
     */
    private @Nullable StagedAssignment put(final Person person, final Unit unit, final SeatType type,
          final List<Edit> edits) {
        final StagedAssignment before = changes.get(person.getId());
        if (before != null) {
            unapply(before);
        }

        final Unit fromUnit = person.getUnit();
        final SeatType fromSeat = currentSeatOf(person);
        StagedAssignment after = null;
        if (unit.equals(fromUnit) && (type == fromSeat)) {
            changes.remove(person.getId());
        } else {
            after = new StagedAssignment(person, fromUnit, fromSeat, unit, type);
            apply(after);
            changes.put(person.getId(), after);
        }
        edits.add(new Edit(person, before, after));
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
        adjust(change.toUnit(), change.toSeat(), 1);
        if ((change.fromUnit() != null) && (change.fromSeat() != null)) {
            adjust(change.fromUnit(), change.fromSeat(), -1);
        }
    }

    private void unapply(final StagedAssignment change) {
        adjust(change.toUnit(), change.toSeat(), -1);
        if ((change.fromUnit() != null) && (change.fromSeat() != null)) {
            adjust(change.fromUnit(), change.fromSeat(), 1);
        }
    }

    private void adjust(final Unit unit, final SeatType type, final int amount) {
        seatChanges.merge(new SeatKey(unit.getId(), type), amount, Integer::sum);
    }

    // endregion Changing the plan

    // region Validation

    /**
     * Checks every staged change against the campaign as it is now.
     *
     * @return the problems found, at most one per change; empty if the whole plan can be applied
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
     * @param change a staged change
     *
     * @return why it cannot be applied, or {@code null} if it can
     */
    public @Nullable Reason problemWith(final StagedAssignment change) {
        final Person person = change.person();
        final Unit unit = change.toUnit();
        if (!Objects.equals(person.getUnit(), change.fromUnit())) {
            return Reason.OUT_OF_DATE;
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

    // endregion Validation
}
