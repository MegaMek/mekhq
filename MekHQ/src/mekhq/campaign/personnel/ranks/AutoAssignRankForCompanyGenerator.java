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
package mekhq.campaign.personnel.ranks;

import static java.lang.Math.max;

import megamek.codeUtilities.StringUtility;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

/**
 * Assigns ranks to unit crew members based on their role: commander (highest rank), squad leaders (descending from just
 * below commander), and regular crew (lowest available rank).
 *
 * <p>This is only used by the Company Generator, currently, as it is a very basic implementation. If anyone wants
 * to use it as a base to create a fully fledged autoRanks system, the playerbase would love it.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class AutoAssignRankForCompanyGenerator {
    private static final String NO_RANK = "None";
    private static final String MISSING_RANK = "-";

    private static final int CLAN_RANK = 4;
    private static final int COMSTAR_RANK = 4;

    private static final int MIN_RANK_INDEX = 0;
    private static final int NORMAL_RANK_INDEX = 0;
    private static final int COMMANDER_RANK_INDEX = 12;
    private static final int LEADER_RANK_INDEX = COMMANDER_RANK_INDEX - 1;

    /**
     * Assigns ranks to all crew members of the given unit based on their role.
     *
     * <p>Roles are assigned in the following priority order:
     * <ol>
     *   <li><b>Commander</b> – receives {@link #COMMANDER_RANK_INDEX}. If that rank is
     *       unavailable in the active rank system, the commander is treated as a squad leader.</li>
     *   <li><b>Squad leaders</b> – the first {@code n} non-commander crew members, where
     *       {@code n} is determined by {@link #countSquadLeaders(Unit)}. Each receives the
     *       highest available rank at or below {@link #LEADER_RANK_INDEX}.</li>
     *   <li><b>Regular crew</b> – all remaining members receive the lowest available rank,
     *       starting from {@link #NORMAL_RANK_INDEX}.</li>
     * </ol>
     *
     * <p>Rank lookups are cached where possible to avoid redundant scans across crew members
     * with the same role.
     *
     * @param unit    the unit whose crew will have ranks assigned; must not be {@code null}
     * @param faction the faction we're using to generate ranks
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static void assignRanks(Unit unit, Faction faction) {
        int leaderCount = countSquadLeaders(unit);
        int nextLeaderRank = LEADER_RANK_INDEX; // Cached so we don't re-scan for every crew member
        int cachedNormalRank = NORMAL_RANK_INDEX;

        boolean isClan = faction.isClan();
        boolean isComStarOrWoB = faction.isComStarOrWoB();

        Person unitCommander = unit.getCommander();
        tryAssignCommanderRank(unitCommander);

        for (Person person : unit.getCrew()) {
            if (!hasNoRank(person)) {
                continue;
            }

            // Clan Override
            if (isClan) {
                if (leaderCount > 0) { // This is a conventional infantry squad
                    person.setRank(CLAN_RANK);
                } // No ranks for non-military castes

                continue;
            }

            // ComStar Override
            if (isComStarOrWoB) {
                person.setRank(COMSTAR_RANK); // Everyone is an acolyte
                continue;
            }

            // Normal
            if (leaderCount <= 0) {
                // Regular crew member
                cachedNormalRank = assignNormalRank(person, cachedNormalRank);
                continue;
            }

            // Squad leader (or commander whose rank assignment failed)
            leaderCount--;
            nextLeaderRank = assignLeaderRank(person, nextLeaderRank);
        }
    }

    /**
     * Returns the number of squad leaders needed (squad count minus the overall commander).
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int countSquadLeaders(Unit unit) {
        Entity entity = unit.getEntity();
        if (entity != null && entity.isConventionalInfantry()) {
            return max(0, ((ConvInfantry) entity).getSquadCount() - 1);
        }
        return 0;
    }

    /**
     * Tries to assign exactly {@link #COMMANDER_RANK_INDEX} to {@code person}.
     *
     * <p>Returns {@code true} if the rank system recognized it (i.e., not "None").</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static boolean tryAssignCommanderRank(Person person) {
        person.setRank(COMMANDER_RANK_INDEX);
        return !hasNoRank(person);
    }

    /**
     * Assigns the highest available rank at or below {@code startIndex} and returns the index actually assigned (for
     * the next leader to start with it).
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int assignLeaderRank(Person person, int startIndex) {
        int rankIndex = startIndex;
        person.setRank(rankIndex);

        boolean noRank = hasNoRank(person);
        while (rankIndex > MIN_RANK_INDEX && noRank) {
            rankIndex--;
            person.setRank(rankIndex);

            noRank = hasNoRank(person);
        }

        return rankIndex; // next leader starts here too
    }

    /**
     * Assigns the lowest available rank at or above {@code cachedIndex}, caching the result so later calls skip
     * straight to the known-good rank.
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int assignNormalRank(Person person, int cachedIndex) {
        person.setRank(cachedIndex);

        boolean noRank = hasNoRank(person);
        while (cachedIndex < COMMANDER_RANK_INDEX && noRank) {
            cachedIndex++;
            person.setRank(cachedIndex);

            noRank = hasNoRank(person);
        }

        return cachedIndex;
    }

    /**
     * Checks whether a character has no named rank.
     *
     * <p>A rank is considered 'named' if it isn't {@code null}, blank, or equal to {@link #NO_RANK} or
     * {@link #MISSING_RANK}</p>
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static boolean hasNoRank(Person person) {
        String rankName = person.getRankName();
        return StringUtility.isNullOrBlank(rankName) ||
                     rankName.equalsIgnoreCase(NO_RANK) ||
                     rankName.equalsIgnoreCase(MISSING_RANK);
    }
}
