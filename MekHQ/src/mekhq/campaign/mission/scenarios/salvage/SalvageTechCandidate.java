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
package mekhq.campaign.mission.scenarios.salvage;

import java.util.Locale;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;

/**
 * A tech who could join a salvage operation, with the facts the planner filters and sorts them by worked out once.
 *
 * @param tech          the tech
 * @param skillLevel    the tech's skill level in the tech role used for salvage
 * @param isInjured     {@code true} if the tech has injuries (or, under standard medical rules, hits)
 * @param isPregnant    {@code true} if the tech is pregnant
 * @param hasTechUnits  {@code true} if the tech is assigned to units as their tech
 * @param minutesLeft   the tech's remaining work time today, in minutes
 * @param searchText    lower-case text the planner's search matches against: name, rank, roles, and tech units
 *
 * @author Illiani
 * @since 0.51.01
 */
public record SalvageTechCandidate(Person tech, SkillLevel skillLevel, boolean isInjured, boolean isPregnant,
      boolean hasTechUnits, int minutesLeft, String searchText) {

    /**
     * @param query the player's search text
     *
     * @return {@code true} if the tech matches the search; a blank search matches everyone
     */
    public boolean matches(String query) {
        String trimmedQuery = query.trim().toLowerCase(Locale.ROOT);
        return trimmedQuery.isEmpty() || searchText.contains(trimmedQuery);
    }
}
