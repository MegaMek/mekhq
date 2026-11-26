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
import mekhq.campaign.enums.DragoonRating;

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

    private final TreeSet<Integer> gunEmplacementRatYears = new TreeSet<>();
    private final Map<Integer, Map<String, String>> gunEmplacementRatNames = new HashMap<>();

    private static final long RAT_LOAD_WAIT_LIMIT_MILLISECONDS = 10000;
    private static final long RAT_LOAD_POLL_INTERVAL_MILLISECONDS = 50;

    /**
     * Returns the shared {@link RandomUnitGenerator} once its RATs have finished loading.
     *
     * <p>The first call to {@link RandomUnitGenerator#getInstance()} in a session only starts loading the RATs on a
     * background thread. Reading the RAT list straight away returns an incomplete, usually empty, list, which caused
     * the first turret or gun emplacement force generated in each session to come out empty.</p>
     *
     * @return the shared {@link RandomUnitGenerator}, which may still be loading if the wait limit was reached
     */
    private static RandomUnitGenerator getLoadedRandomUnitGenerator() {
        RandomUnitGenerator randomUnitGenerator = RandomUnitGenerator.getInstance();
        long waitLimit = System.currentTimeMillis() + RAT_LOAD_WAIT_LIMIT_MILLISECONDS;

        while (!randomUnitGenerator.isInitialized() && (System.currentTimeMillis() < waitLimit)) {
            try {
                Thread.sleep(RAT_LOAD_POLL_INTERVAL_MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!randomUnitGenerator.isInitialized()) {
            logger.warn("RATs had not finished loading after {}ms; the available RAT list may be incomplete.",
                  RAT_LOAD_WAIT_LIMIT_MILLISECONDS);
        }

        return randomUnitGenerator;
    }

    /**
     * Worker function to initialize the mapping between a numeric quality rating level and an alphabetic one (such as
     * one used in the RATs)
     */
    private void initializeRatRatingMappings() {
        // TODO : Switch this with a call to a new IUnitRating array
        if (ratRatingMappings == null) {
            ratRatingMappings = new HashMap<>();
            ratRatingMappings.put(DragoonRating.DRAGOON_ASTAR.getRating(), "A");
            ratRatingMappings.put(DragoonRating.DRAGOON_A.getRating(), "A");
            ratRatingMappings.put(DragoonRating.DRAGOON_B.getRating(), "B");
            ratRatingMappings.put(DragoonRating.DRAGOON_C.getRating(), "C");
            ratRatingMappings.put(DragoonRating.DRAGOON_D.getRating(), "D");
            ratRatingMappings.put(DragoonRating.DRAGOON_F.getRating(), "F");
        }
    }

    /**
     * Generates a list of turrets given a skill level, quality and year
     *
     * @param unitCount   How many turrets to generate
     * @param skill       The skill level of the turret operator
     * @param quality     The quality level of the turret
     * @param currentYear The current year
     *
     * @return List of turrets
     */
    @Override
    public List<MekSummary> generateTurrets(int unitCount, SkillLevel skill, int quality, int currentYear) {
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

        RandomUnitGenerator randomUnitGenerator = getLoadedRandomUnitGenerator();

        for (Iterator<String> ratNameIterator = randomUnitGenerator.getRatList(); ratNameIterator.hasNext(); ) {
            String currentName = ratNameIterator.next();
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

        randomUnitGenerator.setChosenRAT(ratName);
        return randomUnitGenerator.generate(unitCount);
    }

    /**
     * Generates a list of TO:AR Advanced Building gun emplacements given a skill level, quality and year
     *
     * @param unitCount   How many gun emplacements to generate
     * @param skill       The skill level of the gun emplacement operator
     * @param quality     The quality level of the gun emplacement
     * @param currentYear The current year
     *
     * @return List of gun emplacements
     */
    @Override
    public List<MekSummary> generateGunEmplacements(int unitCount, SkillLevel skill, int quality, int currentYear) {
        Integer ratYear;

        // Copied from turrets for the time being
        // less dirty hack
        // we loop through the names of available gun emplacement RATs
        // and pick the latest one
        // gunEmplacement rat file names appear to follow the pattern of "GunEmplacements YYYY Q"
        // where YYYY is the four-digit year
        // and Q is the quality level of the force.
        // This way, as long as the gunEmplacement RAT names follow the above-described pattern,
        // we can handle any number of them.
        initializeRatRatingMappings();

        RandomUnitGenerator randomUnitGenerator = getLoadedRandomUnitGenerator();

        for (Iterator<String> ratNameIterator = randomUnitGenerator.getRatList(); ratNameIterator.hasNext(); ) {
            String currentName = ratNameIterator.next();
            if (currentName.contains("Gun Emplacements")) {
                String gunEmplacementQuality = currentName.substring(currentName.length() - 1);
                int year = Integer.parseInt(currentName.replaceAll("\\D", ""));

                gunEmplacementRatYears.add(year);

                if (!gunEmplacementRatNames.containsKey(year)) {
                    gunEmplacementRatNames.put(year, new HashMap<>());
                }

                gunEmplacementRatNames.get(year).put(gunEmplacementQuality, currentName);
            }
        }

        // We don't have rats for *every* year, so we find the nearest previous one. If
        // there is no
        // RAT for the current or previous year, use the earliest available.
        // If there are no gunEmplacement RATs, return an empty list
        if (gunEmplacementRatYears.isEmpty()) {
            logger.warn("No gunEmplacement RATs found.");
            return Collections.emptyList();
        } else if (currentYear < gunEmplacementRatYears.first()) {
            logger.warn("Earliest gunEmplacement RAT is later than campaign year.");
            ratYear = gunEmplacementRatYears.first();
        } else {
            ratYear = gunEmplacementRatYears.floor(currentYear);
        }

        // now that we have the year, we need to determine which gunEmplacement RAT we're going
        // to use
        String ratName = gunEmplacementRatNames.get(ratYear).get(ratRatingMappings.get(quality));

        randomUnitGenerator.setChosenRAT(ratName);
        return randomUnitGenerator.generate(unitCount);
    }
}
