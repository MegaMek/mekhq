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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

/**
 * Assigns ranks to unit crew members based on their role: commander (highest rank), squad leaders (descending from just
 * below commander), and regular crew (next lowest available rank).
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
    private static final int COMMANDER_RANK_INDEX = 12;
    private static final int LEADER_RANK_INDEX = COMMANDER_RANK_INDEX - 1;
    private static final int NORMAL_RANK_INDEX = LEADER_RANK_INDEX;

    /**
     * Assigns ranks to all crew members of the given unit based on their role.
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
    public static void assignRanks(Campaign campaign, Unit unit, Faction faction) {
        Entity entity = unit.getEntity();
        boolean isConventionalInfantry = entity != null && unit.getEntity().isConventionalInfantry();
        int leaderCount = countSquadLeaders(entity);

        int cachedLeaderRank = LEADER_RANK_INDEX; // Cached so we don't re-scan for every crew member
        int cachedNormalRank = NORMAL_RANK_INDEX;

        List<Person> crew = unit.getCrew();
        crew = sortCrew(campaign, crew);

        Person unitCommander = unit.getCommander();
        unitCommander.setRank(COMMANDER_RANK_INDEX);

        boolean factionIsClan = faction.isClan();
        boolean factionIsComStarOrWoB = faction.isComStarOrWoB();
        for (Person person : crew) {
            // Skip anyone who already has a rank
            if (!hasNoRank(person)) {
                continue;
            }

            // Clan Override
            if (factionIsClan) {
                if (isConventionalInfantry) { // This is a conventional infantry unit
                    person.setRank(CLAN_RANK);
                } // No ranks for non-military castes

                continue;
            }

            // ComStar Override
            if (factionIsComStarOrWoB) {
                person.setRank(COMSTAR_RANK); // Everyone is an acolyte
                continue;
            }

            // Squad leader, or commander if normal assignment failed
            if (leaderCount > 0) {
                leaderCount--;
                cachedLeaderRank = assignNormalRank(person, cachedLeaderRank);

                // If we were successful in assigning a rank, we set up the cachedNormalRank to be the rank below the leader
                if (!hasNoRank(person)) {
                    cachedNormalRank = cachedLeaderRank - 1;
                }
                continue;
            }


            cachedLeaderRank = assignNormalRank(person, cachedNormalRank);
        }
    }

    private static List<Person> sortCrew(Campaign campaign, List<Person> crew) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean campaignIsClan = campaign.isClanCampaign();
        LocalDate today = campaign.getLocalDate();

        crew.sort(Comparator.comparing(
              person -> person.getExperienceLevel(campaignOptions, campaignIsClan, today, false, true)
        ));

        // Sorter is going to sort 0 -> n, so we need to reverse it
        return crew.reversed();
    }

    /**
     * Returns the number of squad leaders needed (squad count minus the overall commander).
     *
     * <p>If the unit doesn't use squads, we're going to pick just 1 character to be the unit's sub-commander.</p>
     *
     * @param entity the unit whose squad leaders we're counting; may be {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int countSquadLeaders(@Nullable Entity entity) {
        int minimumReturnValue = 1;

        if (entity == null) {
            return minimumReturnValue;
        }

        if (entity.isConventionalInfantry()) {
            return max(minimumReturnValue, ((ConvInfantry) entity).getSquadCount() - 1);
        }

        return minimumReturnValue;
    }

    /**
     * Assigns the highest available rank at or below {@code startIndex} and returns the index actually assigned (for
     * the next character to start with it).
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int assignNormalRank(Person person, int startIndex) {
        if (startIndex <= MIN_RANK_INDEX) {
            return MIN_RANK_INDEX;
        }

        int rankIndex = startIndex;
        person.setRank(rankIndex);

        boolean noRank = hasNoRank(person);
        while (rankIndex > MIN_RANK_INDEX && noRank) {
            rankIndex--;
            person.setRank(rankIndex);

            noRank = hasNoRank(person);
        }

        return rankIndex;
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
