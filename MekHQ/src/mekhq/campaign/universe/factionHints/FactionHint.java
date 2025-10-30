/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionHints;

import java.time.LocalDate;

import megamek.common.annotations.Nullable;

/**
 * Each participant in a war or an alliance has one instance of this class for each of the other factions involved.
 */
public class FactionHint {
    private final String name;
    private final LocalDate start;
    private final LocalDate end;

    public FactionHint(final String name, final @Nullable LocalDate start, final @Nullable LocalDate end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    /**
     * Checks if a given date falls within this object's date range.
     *
     * <p>The date is considered in range if:</p>
     *
     * <ul>
     *   <li>It is on or after the start date (or start is null)</li>
     *   <li>AND it is on or before the end date (or end is null)</li>
     * </ul>
     *
     * <p>A {@code null} start or end date means that bound is unbounded (open-ended).</p>
     *
     * @param date the date to check
     *
     * @return {@code true} if the date is within the range (inclusive)
     */
    public boolean isInDateRange(final LocalDate date) {
        return ((start == null) || !date.isBefore(start)) && ((end == null) || !date.isAfter(end));
    }

    /**
     * Checks if this object's start date is today.
     *
     * @param today the current date to check against
     *
     * @return {@code true} if the start date is defined and equals today
     */
    public boolean hintStartsToday(final LocalDate today) {
        return (start != null) && start.isEqual(today);
    }

    /**
     * Checks if this object's end date is today.
     *
     * @param today the current date to check against
     *
     * @return {@code true} if the end date is defined and equals today
     */
    public boolean hintEndsToday(final LocalDate today) {
        return (end != null) && end.isEqual(today);
    }

    @Override
    public String toString() {
        return name;
    }
}
