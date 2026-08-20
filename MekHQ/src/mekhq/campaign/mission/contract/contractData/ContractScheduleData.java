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
     * Returns a copy with {@code startDate} replaced.
     *
     * <p><b>{@code lengthInMonths} is recomputed from the resulting dates</b>, so it always describes the span the
     * contract actually runs for rather than a separately agreed figure. Moving only the start date therefore shortens
     * or lengthens the contract; a caller that means to shift the whole contract unchanged must move both endpoints (as
     * chaining {@code withStartDate(...).withEndDate(...)} does, where the final recomputed length comes back
     * identical). The length is left untouched when either resulting date is absent, since there is no span to
     * measure.</p>
     *
     * @param startDate the replacement start date
     *
     * @return a copy of this schedule with {@code startDate} replaced and the length recomputed
     */
    public ContractScheduleData withStartDate(LocalDate startDate) {
        return new ContractScheduleData(startDate, endDate, monthsBetween(startDate, endDate, lengthInMonths));
    }

    /**
     * Returns a copy with {@code endDate} replaced. As with {@link #withStartDate(LocalDate)}, the length is recomputed
     * from the resulting dates - pushing the end date back is what lengthens (or an extension shortens) the contract.
     *
     * @param endDate the replacement end date
     *
     * @return a copy of this schedule with {@code endDate} replaced and the length recomputed
     */
    public ContractScheduleData withEndDate(LocalDate endDate) {
        return new ContractScheduleData(startDate, endDate, monthsBetween(startDate, endDate, lengthInMonths));
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
