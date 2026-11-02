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
import static mekhq.campaign.personnel.ranks.Rank.RE_MIN;
import static mekhq.campaign.personnel.ranks.Rank.RO_MAX;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.StringUtility;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;

/**
 * Utility class responsible for automatically assigning ranks to personnel. It can apply a faction's rank system and
 * select a valid rank for an individual, or assign missing ranks across a {@link Unit}'s crew.
 *
 * <p>When assigning a unit's crew, rank assignment follows a hierarchy: the unit commander receives the highest
 * rank index, squad leaders (if applicable) receive an intermediate rank, and remaining crew members are assigned the
 * next available rank below the leaders. Clan and ComStar/Word of Blake factions use simplified, flat rank assignments
 * rather than the standard graduated system.</p>
 *
 * <p>Only crew members who do not already hold a rank are affected; existing ranks
 * are always preserved.</p>
 *
 * <p>Everyone this class gives a rank to is first put on the faction's rank system. A rank is an index into a
 * table, and the table is the person's own, so a Draconis Combine crew left on a mercenary campaign's default table
 * would read index 12 as "Sergeant" where the Combine's table says "Gunsho".</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class AutomaticRankAssigner {
    private static final MMLogger LOGGER = MMLogger.create(AutomaticRankAssigner.class);

    private static final String NO_RANK = "None";
    private static final String MISSING_RANK = "-";

    private static final int CLAN_RANK = 4;
    private static final int COMSTAR_RANK = 4;

    private static final int COMMANDER_RANK_INDEX = 12;
    private static final int LEADER_STARTING_RANK_INDEX = COMMANDER_RANK_INDEX - 1;
    private static final int NORMAL_STARTING_RANK_INDEX = LEADER_STARTING_RANK_INDEX;

    /**
     * Assigns ranks to all unranked crew members of the given {@link Unit}, respecting faction-specific rules and
     * distributing ranks by experience level.
     *
     * <p>The assignment process proceeds as follows:</p>
     * <ol>
     *   <li>The crew is sorted in descending order of experience so that more experienced personnel receive higher
     *   ranks.</li>
     *   <li>The unit commander is always assigned {@link #COMMANDER_RANK_INDEX}.</li>
     *   <li>For Clan factions, conventional infantry members receive {@link #CLAN_RANK}; non-military-caste members
     *   receive no rank.</li>
     *   <li>For ComStar and Word of Blake factions, every crew member receives {@link #COMSTAR_RANK} (Acolyte).</li>
     *   <li>For all other factions, a number of crew members equal to the squad leader count (or 1, if not a
     *   conventional infantry unit) receive a leader-level rank, and the remaining crew receive the next rank below.</li>
     * </ol>
     *
     * <p>Personnel who already hold a rank are skipped entirely.</p>
     *
     * @param campaign the active {@link Campaign}, used to retrieve campaign options and determine experience levels
     * @param unit     the {@link Unit} whose crew will be ranked
     * @param faction  the {@link Faction} governing the unit, used to apply faction-specific rank rules
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static void assignRanks(Campaign campaign, Unit unit, Faction faction) {
        Entity entity = unit.getEntity();
        boolean isConventionalInfantry = entity != null && unit.getEntity().isConventionalInfantry();
        int leaderCount = countSquadLeaders(entity);

        int cachedLeaderRank = LEADER_STARTING_RANK_INDEX; // Cached so we don't re-scan for every crew member
        int cachedNormalRank = NORMAL_STARTING_RANK_INDEX;

        List<Person> crew = unit.getCrew();
        crew = sortCrew(campaign, crew);

        boolean factionIsClan = faction.isClan();
        boolean factionIsComStarOrWoB = faction.isComStarOrWoB();

        Person unitCommander = null;
        if (!factionIsClan && !factionIsComStarOrWoB) {
            unitCommander = unit.getCommander();
            if ((unitCommander == null) && !crew.isEmpty()) {
                unitCommander = crew.getFirst();
            }
        }

        putRankedCrewOnFactionRankSystem(unit, crew, unitCommander, faction);

        if (unitCommander != null) {
            unitCommander.setRank(COMMANDER_RANK_INDEX);
        }

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

            // Everyone else
            cachedLeaderRank = assignNormalRank(person, cachedNormalRank);
        }
    }

    /**
     * Puts everyone about to be ranked on the faction's rank system, so the indices assigned below are read from the
     * faction's table. Crew who already hold a rank are not about to be ranked and keep their own system.
     *
     * @param unit          the unit being ranked, named in the log
     * @param crew          the unit's crew
     * @param unitCommander the crew member who is given the commander's rank whether ranked already or not, or
     *                      {@code null} when the faction gives no commander's rank
     * @param faction       the faction whose rank system the crew adopt
     */
    private static void putRankedCrewOnFactionRankSystem(Unit unit, List<Person> crew, @Nullable Person unitCommander,
          Faction faction) {
        List<Person> aboutToBeRanked = new ArrayList<>();
        for (Person person : crew) {
            if ((person == unitCommander) || hasNoRank(person)) {
                aboutToBeRanked.add(person);
            }
        }
        int moved = putOnFactionRankSystem(aboutToBeRanked, faction);
        LOGGER.debug("[AutoRank] unit '{}': {} of the {} crew about to be ranked moved to the rank system of {}",
              unit.getName(), moved, aboutToBeRanked.size(), faction.getShortName());
    }

    /**
     * Puts people on the faction's rank system, as the Command Generator does for everyone it creates, so the rank
     * system the roster offers for them is the same as for the rest of the command. Nobody is given a rank; a person's
     * existing rank index is kept and read from the new table.
     *
     * @param people  the people to move
     * @param faction the faction whose rank system they adopt, or {@code null} to leave them as they are
     *
     * @return how many people changed rank system; {@code 0} when they were on it already, no faction was given or
     *       the faction's rank system is missing or invalid, which is logged
     */
    public static int putOnFactionRankSystem(Collection<Person> people, @Nullable Faction faction) {
        RankValidator rankValidator = new RankValidator();
        RankSystem rankSystem = usableRankSystemOf(faction, rankValidator);
        if (rankSystem == null) {
            return 0;
        }
        int moved = 0;
        for (Person person : people) {
            if (adoptRankSystem(person, rankSystem, rankValidator)) {
                moved++;
            }
        }
        return moved;
    }

    /**
     * The faction's rank system, when it is one personnel can be put on.
     *
     * @param faction       the faction, or {@code null} for none
     * @param rankValidator the validator that decides whether the system is usable
     *
     * @return the faction's rank system, or {@code null} when there is no faction or its rank system is missing or
     *       invalid; the reason is logged
     */
    private static @Nullable RankSystem usableRankSystemOf(@Nullable Faction faction, RankValidator rankValidator) {
        if (faction == null) {
            LOGGER.debug("[AutoRank] no faction given; personnel keep the rank system they were created with");
            return null;
        }
        RankSystem rankSystem = faction.getRankSystem();
        if (!rankValidator.validate(rankSystem, false)) {
            LOGGER.warn("[AutoRank] faction {} names rank system '{}', which is missing or invalid; personnel keep the"
                        + " rank system they were created with, so their rank names are read from that table",
                  faction.getShortName(), faction.getRankSystemCode());
            return null;
        }
        return rankSystem;
    }

    /**
     * Puts the person on the given rank system, unless they are on it already.
     *
     * @param person        the person
     * @param rankSystem    the rank system to adopt
     * @param rankValidator the validator that checks the person's rank still exists on the new system
     *
     * @return {@code true} when the person's rank system changed; {@code false} when they were on it already
     */
    private static boolean adoptRankSystem(Person person, RankSystem rankSystem, RankValidator rankValidator) {
        if (rankSystem.equals(person.getRankSystem())) {
            return false;
        }
        person.setRankSystem(rankValidator, rankSystem);
        return true;
    }

    /**
     * Sorts the provided crew list in descending order of experience level, so that the most experienced personnel
     * appear first and receive the highest available ranks.
     *
     * @param campaign the active {@link Campaign}, used to access {@link CampaignOptions} and the current date for
     *                 experience-level calculations
     * @param crew     the list of {@link Person} objects to sort; the original list is not mutated — a new reversed
     *                 list is returned
     *
     * @return a new {@link List} containing the same crew members ordered from most experienced to least experienced
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static List<Person> sortCrew(Campaign campaign, List<Person> crew) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean campaignIsClan = campaign.getPlayerForce().isClanForce();
        LocalDate today = campaign.getLocalDate();

        crew.sort(Comparator.comparing(
              person -> person.getExperienceLevel(campaignOptions, campaignIsClan, today, false, true)
        ));

        // Sorter is going to sort 0 -> n, so we need to reverse it
        return crew.reversed();
    }

    /**
     * Determines the number of squad leader slots available for the given {@link Entity}.
     *
     * <p>For conventional infantry, the number of leaders is one fewer than the squad count (the platoon commander
     * occupies the top slot separately). All other entity types, and a {@code null} entity, yield a minimum of
     * {@code 1} leader slot.</p>
     *
     * @param entity the {@link Entity} to inspect, or {@code null} if no entity is associated with the unit
     *
     * @return the number of squad leader rank slots to fill; always at least {@code 1}
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
     * Attempts to assign the highest valid rank at or below {@code startIndex} to the given {@link Person}.
     *
     * <p>Beginning at {@code startIndex}, the method decrements the candidate rank index until it finds a rank that
     * resolves to a non-empty, non-placeholder name, or until {@link Rank#RE_MIN} is reached. This handles gaps in rank
     * tables where certain indices map to missing or blank entries.</p>
     *
     * <p>If {@code startIndex} is already at or below {@link Rank#RE_MIN}, no assignment is attempted and
     * {@link Rank#RE_MIN} is returned immediately.</p>
     *
     * @param person     the {@link Person} to receive the rank assignment
     * @param startIndex the highest rank index to try first; the method will walk downward from this value if
     *                   necessary
     *
     * @return the rank index that was ultimately set on the person, or {@link Rank#RE_MIN} if no valid rank could be
     *       found
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int assignNormalRank(Person person, int startIndex) {
        if (startIndex <= RE_MIN) {
            return RE_MIN;
        }

        int rankIndex = startIndex;
        person.setRank(rankIndex);

        boolean noRank = hasNoRank(person);
        while (rankIndex > RE_MIN && noRank) {
            rankIndex--;
            person.setRank(rankIndex);

            noRank = hasNoRank(person);
        }

        return rankIndex;
    }

    /**
     * Assigns the lowest valid rank to the given person, starting from the specified index and ascending until a valid
     * rank is found or {@link Rank#RO_MAX} is reached.
     *
     * @param person     the {@link Person} to assign a rank to; must not be {@code null}
     * @param startIndex the rank value to begin searching from (inclusive)
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static void assignAscendingRank(@Nullable Person person, int startIndex) {
        if (person == null) {
            return;
        }

        int rankIndex = startIndex;
        person.setRank(rankIndex);

        boolean noRank = hasNoRank(person);
        while (rankIndex <= RO_MAX && noRank) {
            rankIndex++;
            person.setRank(rankIndex);

            noRank = hasNoRank(person);
        }
    }

    /**
     * Returns {@code true} if the given {@link Person} currently holds no meaningful rank.
     *
     * <p>A person is considered unranked when their rank name is {@code null}, blank, equal to {@link #NO_RANK}
     * ({@code "None"}), or equal to {@link #MISSING_RANK} ({@code "-"}), all compared case-insensitively.</p>
     *
     * @param person the {@link Person} whose rank is to be checked
     *
     * @return {@code true} if the person has no meaningful rank; {@code false} otherwise
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

    public static void assignRankSystemFromFaction(@Nullable Person person, int rankLevel) {
        if (person == null) {
            return;
        }

        final RankValidator rankValidator = new RankValidator();
        RankSystem rankSystem = usableRankSystemOf(person.getOriginFaction(), rankValidator);
        if (rankSystem == null) {
            return;
        }
        adoptRankSystem(person, rankSystem, rankValidator);

        AutomaticRankAssigner.assignAscendingRank(person, rankLevel);
    }
}
