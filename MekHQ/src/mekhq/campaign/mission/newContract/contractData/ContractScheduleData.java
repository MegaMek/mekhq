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
package mekhq.campaign.mission.newContract.contractData;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.annotation.Nullable;

/**
 * When a contract runs.
 *
 * <p>Either endpoint may be absent: a contract can be held without a settled schedule, and a save need not carry both
 * dates. {@link #isActiveOn(LocalDate)} treats a missing endpoint as unbounded on that side, matching the convention
 * used when sorting missions - an active contract with no start date is treated as starting today.</p>
 *
 * @param startDate      the day the contract begins, or {@code null} when not yet settled
 * @param endDate        the day the contract ends, or {@code null} when open-ended
 * @param lengthInMonths the contract's length in months
 */
public record ContractScheduleData(@Nullable LocalDate startDate,
      @Nullable LocalDate endDate,
      int lengthInMonths
) {
    /**
     * Copies {@code existingData}, replacing either endpoint with the one supplied; a {@code null} argument keeps the
     * existing value.
     *
     * <p><b>{@code lengthInMonths} is recomputed from the resulting dates</b>, so it always describes the span the
     * contract actually runs for rather than a separately agreed figure. That is what makes an extension work: pushing
     * the end date back lengthens the contract. It also means moving only the start date shortens or lengthens it, so a
     * caller that means to shift the whole contract must pass both dates - as {@code setStartAndEndDate} does, where
     * the recomputed length comes back identical.</p>
     *
     * <p>The length is left untouched when either resulting date is absent, since there is no span to measure.</p>
     *
     * @param existingData the schedule to copy
     * @param newStartDate the replacement start date, or {@code null} to keep the existing one
     * @param newEndDate   the replacement end date, or {@code null} to keep the existing one
     */
    public ContractScheduleData(ContractScheduleData existingData, @Nullable LocalDate newStartDate,
          @Nullable LocalDate newEndDate) {
        this(orElse(newStartDate, existingData.startDate),
              orElse(newEndDate, existingData.endDate),
              monthsBetween(orElse(newStartDate, existingData.startDate),
                    orElse(newEndDate, existingData.endDate),
                    existingData.lengthInMonths));
    }

    private static @Nullable LocalDate orElse(final @Nullable LocalDate replacement,
          final @Nullable LocalDate existing) {
        return (replacement == null) ? existing : replacement;
    }

    private static int monthsBetween(final @Nullable LocalDate startDate, final @Nullable LocalDate endDate,
          final int fallback) {
        if ((startDate == null) || (endDate == null)) {
            return fallback;
        }

        return (int) ChronoUnit.MONTHS.between(startDate, endDate);
    }

    /**
     * @param date the day to test
     *
     * @return whether the contract runs on {@code date}, inclusive of both endpoints. An absent start date counts as
     *       already begun and an absent end date as not yet over, so a contract with neither is active on any day.
     */
    public boolean isActiveOn(LocalDate date) {
        final boolean begun = (startDate == null) || !date.isBefore(startDate);
        final boolean notOver = (endDate == null) || !date.isAfter(endDate);
        return begun && notOver;
    }
}
