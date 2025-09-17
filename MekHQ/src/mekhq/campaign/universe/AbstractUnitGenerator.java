/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import megamek.client.generator.RandomUnitGenerator;
import megamek.common.enums.SkillLevel;
import megamek.common.loaders.MekSummary;
import megamek.logging.MMLogger;
import mekhq.campaign.rating.IUnitRating;

/**
 * Base class for unit generators containing common functionality. Currently, only turret-related code.
 *
 * @author NickAragua
 */
public abstract class AbstractUnitGenerator implements IUnitGenerator {
    private static final MMLogger logger = MMLogger.create(AbstractUnitGenerator.class);

    private Map<Integer, String> ratRatingMappings = null;
    private final TreeSet<Integer> turretRatYears = new TreeSet<>();
    private final Map<Integer, Map<String, String>> turretRatNames = new HashMap<>();

    /**
     * Worker function to initialize the mapping between a numeric quality rating level and an alphabetic one (such as
     * one used in the RATs)
     */
    private void initializeRatRatingMappings() {
        // TODO : Switch this with a call to a new IUnitRating array
        if (ratRatingMappings == null) {
            ratRatingMappings = new HashMap<>();
            ratRatingMappings.put(IUnitRating.DRAGOON_ASTAR, "A");
            ratRatingMappings.put(IUnitRating.DRAGOON_A, "A");
            ratRatingMappings.put(IUnitRating.DRAGOON_B, "B");
            ratRatingMappings.put(IUnitRating.DRAGOON_C, "C");
            ratRatingMappings.put(IUnitRating.DRAGOON_D, "D");
            ratRatingMappings.put(IUnitRating.DRAGOON_F, "F");
        }
    }

    /**
     * Generates a list of turrets given a skill level, quality and year
     *
     * @param num         How many turrets to generate
     * @param skill       The skill level of the turret operator
     * @param quality     The quality level of the turret
     * @param currentYear The current year
     *
     * @return List of turrets
     */
    @Override
    public List<MekSummary> generateTurrets(int num, SkillLevel skill, int quality, int currentYear) {
        Integer ratYear;

        // less dirty hack
        // we loop through the names of available turret RATs
        // and pick the latest one
        // turret rat file names appear to follow the pattern of "Turrets YYYY Q"
        // where YYYY is the four-digit year
        // and Q is the quality level of the force.
        // This way, as long as the turret RAT names follow the above-described pattern,
        // we can handle any number of them.
        initializeRatRatingMappings();

        for (Iterator<String> rats = RandomUnitGenerator.getInstance().getRatList(); rats.hasNext(); ) {
            String currentName = rats.next();
            if (currentName.contains("Turrets")) {
                String turretQuality = currentName.substring(currentName.length() - 1);
                int year = Integer.parseInt(currentName.replaceAll("\\D", ""));

                turretRatYears.add(year);

                if (!turretRatNames.containsKey(year)) {
                    turretRatNames.put(year, new HashMap<>());
                }

                turretRatNames.get(year).put(turretQuality, currentName);
            }
        }

        // We don't have rats for *every* year, so we find the nearest previous one. If
        // there is no
        // RAT for the current or previous year, use the earliest available.
        // If there are no turret RATs, return an empty list
        if (turretRatYears.isEmpty()) {
            logger.warn("No turret RATs found.");
            return Collections.emptyList();
        } else if (currentYear < turretRatYears.first()) {
            logger.warn("Earliest turret RAT is later than campaign year.");
            ratYear = turretRatYears.first();
        } else {
            ratYear = turretRatYears.floor(currentYear);
        }

        // now that we have the year, we need to determine which turret RAT we're going
        // to use
        String ratName = turretRatNames.get(ratYear).get(ratRatingMappings.get(quality));

        RandomUnitGenerator.getInstance().setChosenRAT(ratName);
        return RandomUnitGenerator.getInstance().generate(num);
    }
}
