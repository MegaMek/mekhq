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

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.crewAssignment.AssignmentPlan;
import mekhq.campaign.unit.crewAssignment.CrewSeat;

/**
 * How a unit's seats would be filled once the staged plan is applied: by real people, by temporary ("blob") crew, or
 * not at all.
 *
 * <p>Real people always take precedence: each person staged into a seat that temporary crew hold replaces one of them
 * when the plan is applied, so temporary crew only count toward the seats real people leave free. A vessel or infantry
 * unit keeps its temporary crew per seat; a vehicle keeps one shared pool, which counts toward whichever seats are
 * left.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewCoverage {
    /**
     * One seat's coverage.
     *
     * @param seat      the seat as it is now
     * @param real      real people in it once the plan is applied
     * @param temp      temporary crew still holding places in it once the plan is applied
     * @param uncovered places neither real people nor temporary crew fill
     */
    record SeatCoverage(CrewSeat seat, int real, int temp, int uncovered) {}

    /**
     * A whole unit's coverage.
     *
     * @param seats     each seat's coverage, in display order
     * @param capacity  how many places the unit has
     * @param real      real people crewing it once the plan is applied
     * @param temp      temporary crew still crewing it once the plan is applied, per seat and shared together
     * @param pooled    the part of {@code temp} from a shared pool, not tied to any one seat (vehicles)
     * @param uncovered places neither real people nor temporary crew fill
     */
    record UnitCoverage(List<SeatCoverage> seats, int capacity, int real, int temp, int pooled, int uncovered) {
        /** @return how well the unit is crewed, for its status ring and the status filters */
        CrewAssignmentState.Crewing crewing() {
            if ((real == 0) && (temp == 0) && (capacity > 0)) {
                return CrewAssignmentState.Crewing.EMPTY;
            } else if (uncovered > 0) {
                return CrewAssignmentState.Crewing.PARTIAL;
            }
            return (temp > 0) ? CrewAssignmentState.Crewing.TEMP : CrewAssignmentState.Crewing.FULL;
        }
    }

    private CrewCoverage() {
    }

    /**
     * @param unit the unit
     * @param plan the staged plan
     *
     * @return how the unit's seats would be filled once the plan is applied
     */
    static UnitCoverage of(Unit unit, AssignmentPlan plan) {
        List<SeatCoverage> seats = new ArrayList<>();
        int capacity = 0;
        int real = 0;
        int seatTemp = 0;
        int seatTempHeld = 0;
        int open = 0;
        for (CrewSeat seat : plan.seatsOf(unit)) {
            int seatReal = Math.max(0, seat.capacity() - Math.max(0, plan.openSeats(unit, seat.type())));
            // Counts staged temporary crew fills and removals, not just the temporary crew aboard now
            int seatTempPlanned = plan.plannedTempCrew(unit, seat);
            int temp = Math.min(seatTempPlanned, Math.max(0, seat.capacity() - seatReal));
            seats.add(new SeatCoverage(seat, seatReal, temp, Math.max(0, seat.capacity() - seatReal - temp)));
            capacity += seat.capacity();
            real += seatReal;
            seatTemp += temp;
            seatTempHeld += seatTempPlanned;
            open += Math.max(0, seat.capacity() - seatReal - temp);
        }

        // Temporary crew the unit keeps outside any one seat, such as a vehicle's shared pool, cover what is left
        int pooled = Math.min(Math.max(0, plan.plannedTotalTempCrew(unit) - seatTempHeld), open);
        if (pooled > 0) {
            seats = sharePool(seats, pooled);
        }
        return new UnitCoverage(List.copyOf(seats), capacity, real, seatTemp + pooled, pooled, open - pooled);
    }

    /** Spreads a shared pool of temporary crew over the seats with places left, first seat first. */
    private static List<SeatCoverage> sharePool(List<SeatCoverage> seats, int pooled) {
        List<SeatCoverage> shared = new ArrayList<>();
        int left = pooled;
        for (SeatCoverage seat : seats) {
            int taken = Math.min(left, seat.uncovered());
            left -= taken;
            shared.add(new SeatCoverage(seat.seat(), seat.real(), seat.temp() + taken, seat.uncovered() - taken));
        }
        return shared;
    }
}
