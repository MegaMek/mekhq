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

/**
 * Applies an {@link AssignmentPlan} to the campaign. This is the only part of crew assignment that changes units and
 * people; it shows no dialogs.
 *
 * <p>Every change is checked again first, and any that can no longer be applied is skipped. Then every person who is
 * changing seat leaves their old one, and only after that does anyone take a new seat, so a swap or a chain of moves
 * never finds its seat still taken. Before each person takes a seat, the seat is checked for room; if it has none, or
 * the unit refuses them, they are put back in the seat they left, so nobody is left without a seat by a failed
 * assignment.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CrewAssignmentService {
    private static final MMLogger LOGGER = MMLogger.create(CrewAssignmentService.class);

    /**
     * What applying a plan did.
     *
     * @param applied the changes that were applied, in order
     * @param skipped the changes that were not, and why
     */
    public record CommitReport(List<StagedAssignment> applied, List<Problem> skipped) {
        public CommitReport {
            applied = List.copyOf(applied);
            skipped = List.copyOf(skipped);
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

        // Everyone leaves their old seat first
        for (StagedAssignment change : toApply) {
            if (change.fromUnit() != null) {
                change.fromUnit().remove(change.person(), !useTransfers);
            }
        }

        final List<StagedAssignment> applied = new ArrayList<>();
        for (StagedAssignment change : toApply) {
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

        plan.clear();
        plan.refreshSeats();
        return new CommitReport(applied, skipped);
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
