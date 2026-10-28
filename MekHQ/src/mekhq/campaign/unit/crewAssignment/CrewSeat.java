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

import java.util.List;
import java.util.Objects;

import mekhq.campaign.personnel.Person;

/**
 * One group of crew seats on a unit, as it stands right now: its type, how many people it holds, who holds them, and
 * how many temporary ("blob") crew are counted against it.
 *
 * <p>Temporary crew are reported only where the unit keeps them per seat (vessel pilots, gunners and crew, and
 * infantry). A vehicle's temporary crew are one pool shared by its driver, gunner and crew seats, so they are not
 * split between seats here. A real person may always take a seat a temporary crew member holds: the unit releases one
 * temporary crew member when that happens.</p>
 *
 * @param type      the kind of seat
 * @param capacity  how many people the seat holds
 * @param occupants the real people in the seat now
 * @param tempCrew  the temporary crew counted against this seat
 *
 * @author Illiani
 * @since 0.51.01
 */
public record CrewSeat(SeatType type, int capacity, List<Person> occupants, int tempCrew) {
    public CrewSeat {
        Objects.requireNonNull(type);
        occupants = List.copyOf(occupants);
    }

    /**
     * @return how many more real people the seat can take; temporary crew do not reduce this
     */
    public int openSeats() {
        return Math.max(0, capacity - occupants.size());
    }

    /**
     * @return {@code true} if no more real people can take this seat
     */
    public boolean isFull() {
        return openSeats() == 0;
    }
}
