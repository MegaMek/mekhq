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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Problem;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.Reason;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedAssignment;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTech;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.StagedTempCrew;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TechProblem;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan.TempCrewProblem;

/**
 * Applies an {@link AssignmentPlan} to the campaign. This is the only part of crew assignment that changes units and
 * people; it shows no dialogs.
 *
 * <p>Every change is checked again first, and any that can no longer be applied is skipped. The rest are applied in
 * this order:</p>
 * <ol>
 *     <li>Everyone changing seat, or leaving their unit, leaves their old seat, so a swap or a chain of moves never
 *     finds its seat still taken.</li>
 *     <li>Temporary crew are filled or removed. A fill was sized for the crew the unit will end up with, so it comes
 *     after the departures and before the arrivals, and never leaves the unit over strength.</li>
 *     <li>Everyone moving into a seat takes it. The seat is checked for room first; if it has none, or the unit
 *     refuses them, they are put back in the seat they left, so nobody is left without a seat by a failed
 *     assignment. A person taking a place temporary crew hold replaces one of them.</li>
 *     <li>Techs change. A unit whose new tech is refused keeps the tech it had.</li>
 * </ol>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CrewAssignmentService {
    private static final MMLogger LOGGER = MMLogger.create(CrewAssignmentService.class);

    /**
     * What applying a plan did.
     *
     * @param applied         the crew moves that were applied, in order
     * @param skipped         the crew moves that were not, and why
     * @param techsApplied    the tech changes that were applied
     * @param techsSkipped    the tech changes that were not, and why
     * @param tempCrewApplied the temporary crew changes that were applied
     * @param tempCrewSkipped the temporary crew changes that were not, and why
     */
    public record CommitReport(List<StagedAssignment> applied, List<Problem> skipped, List<StagedTech> techsApplied,
          List<TechProblem> techsSkipped, List<StagedTempCrew> tempCrewApplied, List<TempCrewProblem> tempCrewSkipped) {
        public CommitReport {
            applied = List.copyOf(applied);
            skipped = List.copyOf(skipped);
            techsApplied = List.copyOf(techsApplied);
            techsSkipped = List.copyOf(techsSkipped);
            tempCrewApplied = List.copyOf(tempCrewApplied);
            tempCrewSkipped = List.copyOf(tempCrewSkipped);
        }

        /** @return how many changes of every kind were applied */
        public int appliedCount() {
            return applied.size() + techsApplied.size() + tempCrewApplied.size();
        }

        /** @return how many changes of every kind were skipped */
        public int skippedCount() {
            return skipped.size() + techsSkipped.size() + tempCrewSkipped.size();
        }
    }

    private CrewAssignmentService() {
    }

    /**
     * Applies the plan, then empties it.
     *
     * @param plan         the staged changes
     * @param useTransfers the campaign's transfers option: when on, a move between units is logged as one
     *                     reassignment rather than a removal and a new assignment
     * @param today        the campaign date, used as the recruitment date of anyone who has none yet
     *
     * @return what was applied and what was skipped
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static CommitReport commit(final AssignmentPlan plan, final boolean useTransfers, final LocalDate today) {
        final List<Problem> skipped = new ArrayList<>(plan.validate());
        final Set<StagedAssignment> blocked = new HashSet<>();
        for (Problem problem : skipped) {
            blocked.add(problem.change());
        }

        final List<StagedAssignment> toApply = new ArrayList<>();
        for (StagedAssignment change : plan.changes()) {
            if (!blocked.contains(change)) {
                toApply.add(change);
            }
        }
        // Checked now, before anything changes, as they were staged against the campaign as it is now
        final List<TechProblem> techsSkipped = new ArrayList<>(plan.validateTechs());
        final List<TempCrewProblem> tempCrewSkipped = new ArrayList<>(plan.validateTempCrew());

        // 1. Everyone leaves their old seat first. Only a move to another unit counts as a transfer: leaving a unit
        // altogether is always logged as a removal
        final List<StagedAssignment> applied = new ArrayList<>();
        for (StagedAssignment change : toApply) {
            if (change.fromUnit() != null) {
                change.fromUnit().remove(change.person(), !(useTransfers && !change.isRemoval()));
            }
            if (change.isRemoval()) {
                applied.add(change);
            }
        }

        // 2. Temporary crew
        final List<StagedTempCrew> tempCrewApplied = new ArrayList<>();
        for (StagedTempCrew change : plan.tempCrewChanges()) {
            if (tempCrewSkipped.stream().noneMatch(problem -> problem.change().equals(change))) {
                change.after().forEach((role, count) -> change.unit().setTempCrew(role, count));
                tempCrewApplied.add(change);
            }
        }

        // 3. Everyone moving into a seat takes it
        for (StagedAssignment change : toApply) {
            if (change.isRemoval()) {
                continue;
            }
            final Person person = change.person();
            final boolean isTransfer = useTransfers && change.isReassignment();
            if (hasRoom(change.toUnit(), change.toSeat())) {
                seat(person, change.toUnit(), change.toSeat(), change.fromUnit(), isTransfer);
            }

            if (change.toUnit().equals(person.getUnit())) {
                if (person.getRecruitment() == null) {
                    person.setRecruitment(today);
                }
                applied.add(change);
            } else {
                LOGGER.warn("Could not seat {} as {} of {}; returning them to their previous seat",
                      person.getFullName(), change.toSeat(), change.toUnit().getName());
                skipped.add(new Problem(change, Reason.REFUSED));
                if ((change.fromUnit() != null) && (change.fromSeat() != null)) {
                    seat(person, change.fromUnit(), change.fromSeat(), null, false);
                }
            }
        }

        // 4. Techs
        final List<StagedTech> techsApplied = new ArrayList<>();
        for (StagedTech change : plan.techChanges()) {
            if (techsSkipped.stream().anyMatch(problem -> problem.change().equals(change))) {
                continue;
            }
            if (applyTech(change)) {
                techsApplied.add(change);
            } else {
                techsSkipped.add(new TechProblem(change, Reason.REFUSED));
            }
        }

        plan.clear();
        plan.refreshSeats();
        return new CommitReport(applied, skipped, techsApplied, techsSkipped, tempCrewApplied, tempCrewSkipped);
    }

    /**
     * Replaces a unit's tech. If the unit refuses the new tech, it is given back the tech it had.
     *
     * @return {@code true} if the unit ends up with the staged tech
     */
    private static boolean applyTech(final StagedTech change) {
        final Unit unit = change.unit();
        final Person fromTech = change.fromTech();
        final Person toTech = change.toTech();
        if (fromTech != null) {
            unit.remove(fromTech, true);
        }
        if (toTech == null) {
            return true;
        }
        unit.setTech(toTech);
        if (toTech.equals(unit.getTech())) {
            return true;
        }
        LOGGER.warn("Could not make {} the tech of {}; returning it to its previous tech", toTech.getFullName(),
              unit.getName());
        if (fromTech != null) {
            unit.setTech(fromTech);
        }
        return false;
    }

    private static boolean hasRoom(final Unit unit, final SeatType type) {
        final CrewSeat seat = CrewSeatLayout.seatOf(unit, type);
        return (seat != null) && !seat.isFull();
    }

    /**
     * Puts a person in a seat through the {@link Unit} method for that seat.
     *
     * @param person     the person
     * @param unit       the unit
     * @param type       the seat
     * @param oldUnit    the unit they left, for the formation change log, or {@code null}
     * @param isTransfer {@code true} to log the move as a reassignment
     */
    private static void seat(final Person person, final Unit unit, final SeatType type,
          final @Nullable Unit oldUnit, final boolean isTransfer) {
        switch (type) {
            case PILOT, SOLDIER -> unit.addPilotOrSoldier(person, oldUnit, isTransfer);
            case DRIVER -> unit.addDriver(person, isTransfer);
            case GUNNER -> unit.addGunner(person, isTransfer);
            case CREW -> unit.addVesselCrew(person, isTransfer);
            case NAVIGATOR -> unit.setNavigator(person, isTransfer);
            case TECH_OFFICER, CONSOLE_COMMANDER -> unit.setTechOfficer(person, isTransfer);
        }
    }
}
