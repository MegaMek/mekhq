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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Holds data for Faction Standing ultimatum event.
 *
 * @param name                the name of the ultimatum.
 * @param date                the date of the ultimatum. Stored as a {@link String} for ease of loading
 * @param affectedFactionCode the code for the faction affected by the ultimatum
 * @param challenger          information about initiating the challenger
 * @param incumbent           information about the faction leader being challenged
 * @param isViolentTransition {@code true} if the transition is violent
 *
 * @author Illiani
 * @since 0.50.07
 */
public record FactionStandingUltimatumData(
      String name,
      String date,
      String affectedFactionCode,
      FactionStandingAgitatorData challenger,
      FactionStandingAgitatorData incumbent,
      boolean isViolentTransition
) {
    /**
     * Returns the date as a {@link LocalDate}, parsed from the date string.
     *
     * @return {@link LocalDate} representation of the date.
     *
     * @throws DateTimeParseException if the date format is invalid
     * @author Illiani
     * @since 0.50.07
     */
    public LocalDate getDate() {
        return LocalDate.parse(date);
    }
}

