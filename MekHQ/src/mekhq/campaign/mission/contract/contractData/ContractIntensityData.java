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

import mekhq.campaign.mission.contract.AbstractContract;

/**
 * How large, hard, and demanding a contract's combat is, and how it is paced across the months it runs.
 *
 * <p>Groups the contract's scenario-generation parameters in one place. Immutable like the other contract data
 * records; the {@code with*} methods return a copy with one field replaced, so a caller adjusts a single parameter
 * without disturbing the rest (as {@link AbstractContract}'s scalar setters do).</p>
 *
 * @param scale                  the support-point/Battle-Value scale the contract is sized at
 * @param requiredVictoryPoints  the victory points needed to win the contract
 * @param trackCount             the number of StratCon tracks the contract runs
 * @param monthlyTrackCounts     the scenario schedule: how those tracks are spread across the contract's months, rolled
 *                               from the Hot Spots Draconis Reach Track Intensity Tables. Entry {@code i} is the number
 *                               of tracks that fall in month {@code i} (zero-based), so the entries sum to
 *                               {@code trackCount}. Never {@code null}; empty until a schedule has been generated.
 *                               StratCon consumes it to stagger strategic-objective scenario spawns across the
 *                               contract's run (see {@code StratConContractInitializer}); because the schedule spans
 *                               the source table's native length rather than the contract's, that consumer folds any
 *                               overflow tail into the final month.
 */
public record ContractIntensityData(int scale, int requiredVictoryPoints, int trackCount,
      List<Integer> monthlyTrackCounts) {
    public ContractIntensityData {
        // Defensive copy so the schedule is genuinely immutable regardless of the list the caller passed.
        monthlyTrackCounts = List.copyOf(monthlyTrackCounts);
    }

    /**
     * returns a zeroed instance with an empty schedule, the starting state before any parameter is determined
     */
    public ContractIntensityData() {
        this(0, 0, 0, List.of());
    }

    /** @return a copy with {@code scale} replaced */
    public ContractIntensityData withScale(int scale) {
        return new ContractIntensityData(scale, requiredVictoryPoints, trackCount,
              monthlyTrackCounts);
    }

    /** @return a copy with {@code requiredVictoryPoints} replaced */
    public ContractIntensityData withRequiredVictoryPoints(int requiredVictoryPoints) {
        return new ContractIntensityData(scale, requiredVictoryPoints, trackCount,
              monthlyTrackCounts);
    }

    /** @return a copy with {@code trackCount} replaced */
    public ContractIntensityData withTrackCount(int trackCount) {
        return new ContractIntensityData(scale, requiredVictoryPoints, trackCount,
              monthlyTrackCounts);
    }

    /** @return a copy with the scenario schedule ({@code monthlyTrackCounts}) replaced */
    public ContractIntensityData withMonthlyTrackCounts(List<Integer> monthlyTrackCounts) {
        return new ContractIntensityData(scale, requiredVictoryPoints, trackCount,
              monthlyTrackCounts);
    }

    /**
     * @param lengthInMonths the number of months to span
     *
     * @return an empty schedule of the given length - every month holds zero tracks, as a contract with no tracks does
     */
    public static List<Integer> emptySchedule(int lengthInMonths) {
        return Collections.nCopies(lengthInMonths, 0);
    }

    /** @return the number of months the scenario schedule covers */
    public int scheduleLengthInMonths() {
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

    /** @return the total number of tracks across all months of the scenario schedule */
    public int totalScheduledTracks() {
        return monthlyTrackCounts.stream().mapToInt(Integer::intValue).sum();
    }
}
