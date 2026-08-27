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
package mekhq.campaign.mission.contract.contractData;

import java.util.Collections;
import java.util.List;

/**
 * How a contract's StratCon tracks are spread across the months it runs.
 *
 * <p>Rolled from the CamOps Track Intensity Tables (Campaign Operations, "Track Intensity Tables"): the contract's
 * length and its track count pick a table cell, and the cell distributes those tracks over the contract's months. Entry
 * {@code i} of {@link #monthlyTrackCounts()} is the number of tracks that fall in month {@code i} (zero-based), so the
 * entries always sum to the contract's total track count.</p>
 *
 * <p>Nothing consumes this schedule yet - it is generated and stored so a later feature can drive scenario cadence
 * from
 * it.</p>
 *
 * @param monthlyTrackCounts the per-month track counts, one entry per contract month, in order
 */
public record ContractScenarioSchedule(List<Integer> monthlyTrackCounts) {
    public ContractScenarioSchedule {
        // Defensive copy so the schedule is genuinely immutable regardless of the list the caller passed.
        monthlyTrackCounts = List.copyOf(monthlyTrackCounts);
    }

    /**
     * @param lengthInMonths the number of months to span
     *
     * @return an empty schedule of the given length - every month holds zero tracks, as a contract with no tracks does
     */
    public static ContractScenarioSchedule empty(int lengthInMonths) {
        return new ContractScenarioSchedule(Collections.nCopies(lengthInMonths, 0));
    }

    /** @return the number of months this schedule covers */
    public int lengthInMonths() {
        return monthlyTrackCounts.size();
    }

    /**
     * @param monthIndex the zero-based month to read
     *
     * @return the number of tracks that fall in that month
     */
    public int tracksInMonth(int monthIndex) {
        return monthlyTrackCounts.get(monthIndex);
    }

    /** @return the total number of tracks across all months */
    public int totalTrackCount() {
        return monthlyTrackCounts.stream().mapToInt(Integer::intValue).sum();
    }
}
