/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.rating.CamOpsReputation;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

public class CrimeRating {
    private static final MMLogger logger = MMLogger.create(CrimeRating.class);

    /**
     * Calculates the crime rating for a given campaign.
     *
     * @param campaign the campaign for which to calculate the crime rating
     *
     * @return the calculated crime rating
     */
    protected static Map<String, Integer> calculateCrimeRating(Campaign campaign) {
        Map<String, Integer> crimeRating = new HashMap<>();

        crimeRating.put("piracy", campaign.getCrimePirateModifier());
        crimeRating.put("other", campaign.getRawCrimeRating());

        int adjustedCrimeRating = campaign.getAdjustedCrimeRating();
        crimeRating.put("total", adjustedCrimeRating);

        logger.debug("Crime Rating = {}",
              crimeRating.entrySet().stream()
                    .map(entry -> String.format("%s: %d\n", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining()));

        return crimeRating;
    }
}
