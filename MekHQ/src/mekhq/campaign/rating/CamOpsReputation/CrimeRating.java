/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
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
