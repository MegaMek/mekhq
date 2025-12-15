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

import static mekhq.campaign.universe.factionStanding.FactionAccoladeLevel.LETTER_FROM_HEAD_OF_STATE;

import java.time.LocalDate;

import megamek.logging.MMLogger;

/**
 * Represents the record of a specific accolade event, capturing both the recognition level of the accolade and the date
 * it was issued.
 *
 * <p>Provides methods to determine if the accolade has expired or can be improved, based on pre-defined expiry and
 * cooldown periods.</p>
 *
 * @param level     the {@link FactionAccoladeLevel} representing the level of the accolade
 * @param issueDate the {@link LocalDate} the accolade was issued
 *
 * @author Illiani
 * @since 0.50.07
 */
public record AccoladeEntry(FactionAccoladeLevel level, LocalDate issueDate) {
    private static final MMLogger LOGGER = MMLogger.create(AccoladeEntry.class);

    /** The minimum number of months that must pass before an accolade is eligible for improvement. */
    static final int COOLDOWN_PERIOD = 6;

    /**
     * Determines whether the accolade can be improved based on its recognition level and issue date.
     *
     * <p>Improvement is possible if the recognition level has not reached the maximum allowed and at least six months
     * have passed since issuance.</p>
     *
     * @param today                  the current campaign date
     * @param currentFactionStanding the current Faction Standing held with the accolade giving faction
     *
     * @return {@code true} if the accolade can be improved, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean canImprove(LocalDate today, FactionStandingLevel currentFactionStanding) {
        if (level.getRecognition() >= LETTER_FROM_HEAD_OF_STATE.getRecognition()) {
            LOGGER.debug("Accolade questline concluded");
            return false;
        }

        LocalDate cooldownDate = issueDate.plusMonths(COOLDOWN_PERIOD);
        boolean isOffCooldown = !today.isBefore(cooldownDate);
        if (!isOffCooldown) {
            LOGGER.debug("Accolade on cooldown. Last accolade date: {}. Level: {}. Cooldown expires: {}",
                  issueDate, level.getRecognition(), cooldownDate);
            return false;
        }

        int currentStandingLevel = currentFactionStanding.getStandingLevel();
        int requiredStandingLevel = level.getRequiredStandingLevel();
        boolean standingRequirementsMet = currentStandingLevel >= requiredStandingLevel;
        if (!standingRequirementsMet) {
            LOGGER.debug("Current standing level below required standing level");
            return false;
        }

        return true;
    }
}
