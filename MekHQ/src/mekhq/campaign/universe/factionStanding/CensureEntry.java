/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionCensureLevel.MAX_CENSURE_SEVERITY;

import java.time.LocalDate;

/**
 * Represents the record of a specific censure event for a faction member, capturing both the severity of the censure
 * and the date it was issued.
 *
 * <p>
 * Provides methods to determine if the censure has expired or can be escalated, based on pre-defined expiry and
 * cooldown periods.
 * </p>
 *
 * @param level     the {@link FactionCensureLevel} representing the severity of the censure
 * @param issueDate the {@link LocalDate} the censure was issued
 *
 * @author Illiani
 * @since 0.50.07
 */
public record CensureEntry(FactionCensureLevel level, LocalDate issueDate) {
    /** The number of months after which a censure expires. */
    static final int EXPIRY_PERIOD = 12;

    /** The minimum number of months that must pass before a censure is eligible for escalation. */
    static final int COOLDOWN_PERIOD = 6;

    /**
     * Determines whether this censure has expired according to the specified date.
     *
     * <p>A censure is considered expired if more than {@link #EXPIRY_PERIOD} months have elapsed since it was
     * issued.</p>
     *
     * @param today the date to check expiration against
     *
     * @return {@code true} if the censure is expired, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean hasExpired(LocalDate today) {
        LocalDate expiryDate = issueDate.plusMonths(EXPIRY_PERIOD);
        return today.isAfter(expiryDate);
    }

    /**
     * Determines whether the censure can be escalated based on its severity and issue date.
     *
     * <p>Escalation is possible if the severity has not reached the maximum allowed and at least six months have
     * passed
     * since issuance.</p>
     *
     * @param today the current campaign date
     *
     * @return {@code true} if the censure can be escalated, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean canEscalate(LocalDate today) {
        if (level.getSeverity() >= MAX_CENSURE_SEVERITY) {
            return false;
        }
        LocalDate cooldownDate = issueDate.plusMonths(COOLDOWN_PERIOD);
        return today.isAfter(cooldownDate);
    }
}
