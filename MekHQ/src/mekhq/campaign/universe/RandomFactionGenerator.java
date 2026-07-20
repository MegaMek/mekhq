/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static mekhq.campaign.universe.Faction.BANDIT_CASTE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.CLAN_FACTION_CODE;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.mission.contractGeneration.GlobalEmployerTableValue;
import mekhq.campaign.mission.newContract.EnemySelectionProfile;
import mekhq.campaign.mission.newContract.MissionLocationProfile;
import mekhq.campaign.mission.newContract.targetFinder.MissionTargetFinder;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.factionHints.FactionHints;

/**
 * Uses Factions and Planets to weighted lists of potential employers and enemies for contract generation. Also finds a
 * suitable planet for the action.
 *
 * @author Neoancient
 */
public class RandomFactionGenerator {
    // TODO : Account for the moratorium on trials for the Clans during the early Clan Invasion
    private static final MMLogger LOGGER = MMLogger.create(RandomFactionGenerator.class);

    private static RandomFactionGenerator randomFactionGenerator = null;

    private FactionBorderTracker borderTracker;
    private FactionHints factionHints;
    private final MissionTargetFinder missionTargetFinder;

    /**
     * Constructs a generator with a default {@link FactionBorderTracker} and the shared {@link FactionHints} instance.
     */
    public RandomFactionGenerator() {
        this(null, null);
    }

    /**
     * Constructs a generator with the given border tracker and faction hints, falling back to a default border tracker
     * and the shared {@link FactionHints} instance when either argument is {@code null}.
     *
     * @param borderTracker the border tracker to use, or {@code null} to create a default one
     * @param factionHints  the faction hints to use, or {@code null} to use {@link FactionHints#getInstance()}
     */
    public RandomFactionGenerator(FactionBorderTracker borderTracker, FactionHints factionHints) {
        this.borderTracker = borderTracker;
        this.factionHints = factionHints;
        if (null == borderTracker) {
            initDefaultBorderTracker();
        }
        if (null == factionHints) {
            this.factionHints = FactionHints.getInstance();
        }
        missionTargetFinder = new MissionTargetFinder(this.borderTracker, this.factionHints);
    }

    /**
     * Builds the default {@link FactionBorderTracker} used when no tracker is supplied, configured with this
     * generator's standard day/distance thresholds and border sizes.
     */
    private void initDefaultBorderTracker() {
        borderTracker = new FactionBorderTracker();
        borderTracker.setDayThreshold(30);
        borderTracker.setDistanceThreshold(100);
        borderTracker.setDefaultBorderSize(MHQConstants.FACTION_GENERATOR_BORDER_RANGE_IS,
              MHQConstants.FACTION_GENERATOR_BORDER_RANGE_NEAR_PERIPHERY,
              MHQConstants.FACTION_GENERATOR_BORDER_RANGE_CLAN);
    }

    /**
     * @return the shared {@link RandomFactionGenerator} instance, creating a default one on first access
     */
    public static RandomFactionGenerator getInstance() {
        if (randomFactionGenerator == null) {
            randomFactionGenerator = new RandomFactionGenerator();
        }
        return randomFactionGenerator;
    }

    /**
     * Replaces the shared {@link RandomFactionGenerator} instance, e.g. for testing.
     *
     * @param instance the instance to use as the new shared instance
     */
    public static void setInstance(RandomFactionGenerator instance) {
        randomFactionGenerator = instance;
    }

    /**
     * Initializes the border tracker for the given campaign: sets its date to the campaign's current date, centers its
     * search region on the campaign's current system with a radius from the campaign options, registers it as an event
     * handler, and widens the border size for deep periphery factions.
     *
     * @param campaign the campaign to initialize the border tracker for
     */
    public void startup(Campaign campaign) {
        borderTracker.setDate(campaign.getLocalDate());
        final PlanetarySystem location = campaign.getCurrentLocation().getCurrentSystem();
        borderTracker.setRegionCenter(location.getX(), location.getY());
        borderTracker.setRegionRadius(campaign.getCampaignOptions().getContractSearchRadius());
        MekHQ.registerHandler(borderTracker);
        for (final Faction faction : Factions.getInstance().getFactions()) {
            if (faction.isDeepPeriphery()) {
                borderTracker.setBorderSize(faction, MHQConstants.FACTION_GENERATOR_BORDER_RANGE_DEEP_PERIPHERY);
            }
        }
    }

    /**
     * Updates the border tracker's current date.
     *
     * @param date the new current date
     */
    public void setDate(LocalDate date) {
        borderTracker.setDate(date);
    }

    /**
     * @return the {@link FactionHints} used by this generator
     */
    public FactionHints getFactionHints() {
        return factionHints;
    }

    /**
     * Checks whether the given faction has any territory to its name: systems it controls directly anywhere on the map,
     * or failing that, a contained-faction host that does (e.g. the Star League's member states while the League itself
     * holds almost nothing directly).
     *
     * @param faction the faction to check
     * @param date    the date to check faction control and the contained-faction relationship against
     *
     * @return {@code true} if the faction controls at least one known system on the given date, or is hosted by a
     *       faction that does
     */
    public boolean hasAnyTerritory(Faction faction, LocalDate date) {
        if (borderTracker.controlsAnySystem(faction, date)) {
            return true;
        }
        for (Faction host : factionHints.getContainedFactionHosts(faction, date)) {
            if (borderTracker.controlsAnySystem(host, date)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unregisters the border tracker as an event handler.
     */
    public void dispose() {
        MekHQ.unregisterHandler(borderTracker);
    }

    /**
     * @return the border tracker's current date
     */
    private LocalDate getCurrentDate() {
        return borderTracker.getLastUpdated();
    }

    /**
     * @return A set of faction keys for all factions that have a presence within the search area.
     */
    @Deprecated(since = "0.51.01")
    public Set<String> getCurrentFactions() {
        Set<String> retVal = new TreeSet<>();
        LocalDate currentDate = getCurrentDate();
        for (Faction faction : borderTracker.getFactionsInRegion()) {
            if (!isEligibleFaction(faction, currentDate) || faction.getShortName().equals(CLAN_FACTION_CODE)) {
                continue;
            }

            retVal.add(faction.getShortName());
            /* Add factions which do not control any planets to the employer list */
            for (Faction containedFaction : factionHints.getContainedFactions(faction, currentDate)) {
                if (containedFaction != null && containedFaction.validIn(currentDate)) {
                    retVal.add(containedFaction.getShortName());
                }
            }
        }
        // Add rebels and pirates
        retVal.add(REBEL_FACTION_CODE);
        retVal.add(PIRATE_FACTION_CODE);
        return retVal;
    }

    /**
     * Determines whether the given faction should be excluded from employer selection: {@code null} or ineligible
     * factions (see {@link #isEligibleFaction}), factions filtered out by the employer-type power tier, the mercenary
     * faction itself, and Clans (aside from the CW/CSF mercenary-use exceptions) are all excluded.
     *
     * @param faction             the candidate faction to check
     * @param currentDate         the date to check faction eligibility against
     * @param currentYear         the year of {@code currentDate}
     * @param employerType        the type of employer to return, or {@code null} for no power-tier filtering
     * @param isMercenaryCampaign if the player campaign is considered part of the 'mercenary' faction
     *
     * @return {@code true} if the faction should be excluded from employer selection
     */
    private static boolean checkForEarlyExit(@Nullable Faction faction, LocalDate currentDate, int currentYear,
          @Nullable GlobalEmployerTableValue employerType, boolean isMercenaryCampaign) {
        if ((faction == null) || !isEligibleFaction(faction, currentDate)) {
            return true;
        }

        if (applyFactionFilter(faction, employerType)) {
            return true;
        }

        if (isMercenaryCampaign && !faction.isUsesMercenaries(currentYear)) {
            return true;
        }

        // We don't add mercenary employers here, they're handled explicitly elsewhere
        return faction.getShortName().equals(MERCENARY_FACTION_CODE);
    }

    /**
     * Checks whether the given faction should be filtered out based on the requested employer power tier.
     *
     * @param faction      the candidate faction to check
     * @param employerType the type of employer to return, or {@code null} for no power-tier filtering
     *
     * @return {@code true} if the faction does not match the requested employer type and should be filtered out
     */
    private static boolean applyFactionFilter(Faction faction, @Nullable GlobalEmployerTableValue employerType) {
        if (employerType == null) {
            return false;
        }

        return !GlobalEmployerTableValue.getFactionTableType(faction).equals(employerType);
    }

    /**
     * Checks the baseline eligibility criteria shared across faction-candidate filtering: whether the faction is a
     * placeholder ("empty") faction, whether it's valid (not extinct or not yet formed) on the given date, and whether
     * it's the Republic of the Sphere during the Fortress Republic period. Callers layer their own additional,
     * context-specific exclusions (employer power tier, mercenary usage, alliances, neutrality, etc.) on top of this.
     *
     * @param faction the candidate faction to check
     * @param date    the date to check eligibility against
     *
     * @return {@code true} if the faction passes these baseline criteria
     */
    private static boolean isEligibleFaction(Faction faction, LocalDate date) {
        // Skip factions whose stated active years don't include the current date. Stale planet ownership data can
        // otherwise leak extinct factions (e.g. ARC after 3028) into the pool.
        return !FactionHints.isEmptyFaction(faction) &&
                     faction.validIn(date) &&
                     !Faction.isDuringFortressRepublic(faction.getShortName(), date, null);
    }

    /**
     * Selects a random employer faction from those controlling (or hosted by a controller of) systems within the search
     * area around the given location, with no employer-type filtering. Equivalent to calling
     * {@link #getRandomEmployerFaction(ILocation, LocalDate, GlobalEmployerTableValue, boolean)} with a {@code null}
     * employer type.
     *
     * @param location the location to check
     * @param date     the date to check faction control and eligibility against
     *
     * @return a random eligible employer faction for the area, or {@code null} if the location has no system, or no
     *       eligible faction currently controls anything in the area
     */
    @Deprecated(since = "0.51.01", forRemoval = true)
    public @Nullable Faction getEmployerFaction(ILocation location, LocalDate date) {
        return getRandomEmployerFaction(location, date, null, true);
    }

    /**
     * Returns a randomly selected faction that can act as an employer based on the provided location, date, employer
     * type, and campaign type.
     *
     * <p>The method calculates potential employer factions by considering nearby planetary systems and
     * filtering them based on the given criteria. It then assigns weights to the factions and performs a weighted
     * random selection.</p>
     *
     * @param location            The current location used to determine the system context for employer selection.
     * @param date                The date to use for filtering factions by temporal availability.
     * @param employerType        The type of employer being considered, which may further filter faction selection. Can
     *                            be null if no specific type is required.
     * @param isMercenaryCampaign A flag indicating if the selection is being made in the context of a mercenary
     *                            campaign. May alter filtering and weighting logic.
     *
     * @return A randomly selected {@code Faction} that can act as an employer under the provided criteria, or
     *       {@code null} if the location has no current system or no eligible faction controls anything in range.
     */
    public @Nullable Faction getRandomEmployerFaction(ILocation location, LocalDate date,
          @Nullable GlobalEmployerTableValue employerType, boolean isMercenaryCampaign) {
        PlanetarySystem system = location.getCurrentSystem();
        if (system == null) {
            return null;
        }

        Map<Faction, Integer> counts = countRegionalPresence(system, date);

        int currentYear = date.getYear();
        Map<Faction, Integer> finalWeights = new LinkedHashMap<>();
        for (Map.Entry<Faction, Integer> entry : counts.entrySet()) {
            Faction faction = entry.getKey();
            int weight = entry.getValue();

            if (checkForEarlyExit(faction, date, currentYear, employerType, isMercenaryCampaign)) {
                continue;
            }

            finalWeights.merge(faction, weight, Integer::sum);

            for (Faction containedFaction : factionHints.getContainedFactions(faction, date)) {
                if (!checkForEarlyExit(containedFaction, date, currentYear, employerType, isMercenaryCampaign)) {
                    double fractionalPresence = factionHints.getAltLocationFraction(faction, containedFaction, date);
                    int adjustedWeight = (int) ceil(weight * fractionalPresence);
                    finalWeights.merge(containedFaction, adjustedWeight, Integer::sum);
                }
            }
        }

        WeightedIntMap<Faction> weightedEmployers = new WeightedIntMap<>();
        for (Map.Entry<Faction, Integer> entry : finalWeights.entrySet()) {
            weightedEmployers.add(entry.getValue(), entry.getKey());
        }
        return weightedEmployers.randomItem();
    }

    /**
     * Counts how many systems within this generator's search radius of {@code origin} each faction controls, the shared
     * base weighting for both employer selection and the enemy-candidate pool.
     *
     * @param origin the system to center the search on
     * @param date   the date to check faction control against
     *
     * @return a map of each faction present in range to the number of systems it controls there
     */
    private Map<Faction, Integer> countRegionalPresence(PlanetarySystem origin, LocalDate date) {
        Map<Faction, Integer> counts = new HashMap<>();
        for (PlanetarySystem nearbySystem : borderTracker.systemsNear(origin, borderTracker.getRadius())) {
            for (Faction faction : nearbySystem.getFactionSet(date)) {
                counts.merge(faction, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Selects a random enemy faction for {@code employer} within the area around {@code location}, weighted by regional
     * presence and diplomatic stance (see {@link #buildEnemyMap}).
     *
     * @param isCovert whether this is a covert operation, where allies become rare, low-chance targets instead of
     *                 always being excluded
     * @param location the location to center the search on
     * @param date     the date to check faction control and diplomatic relations against
     * @param employer the employer faction, or {@code null} to skip straight to the REBEL fallback
     *
     * @return a randomly selected enemy faction, or the INDEPENDENT faction if none could be found
     */
    public Faction getRandomEnemy(boolean isCovert, ILocation location, LocalDate date, @Nullable Faction employer) {
        if (employer == null) {
            return rebelFallback("No employer supplied or faction does not exist. Returning REBEL");
        }

        WeightedIntMap<Faction> enemyMap = buildEnemyMap(isCovert, location, date, employer);
        Faction enemy = enemyMap.randomItem();
        if (null != enemy) {
            return enemy;
        }

        return rebelFallback("Could not find enemy for employerName {}. Returning REBEL",
              employer.getShortName());
    }

    /**
     * Selects an enemy faction for {@code employer} by delegating to the given contract-type enemy-selection profile
     * (see {@link EnemySelectionProfile#selectEnemy(RandomFactionGenerator, ILocation, LocalDate, Faction)}): synthetic
     * enemies (pirates, rebels, raiders) resolve directly, war-based profiles prefer the employer's actual belligerents
     * before falling back to the standard pool, and covert profiles run the standard pool under covert rules (see
     * {@link #getRandomEnemy(boolean, ILocation, LocalDate, Faction)}).
     *
     * @param location the location to center any pool-based search on
     * @param date     the date to check faction control and diplomatic relations against
     * @param employer the employer faction, or {@code null} to skip straight to the INDEPENDENT fallback
     * @param profile  the enemy-selection profile for the contract's type
     *
     * @return the selected enemy faction, or the INDEPENDENT faction if none could be found
     */
    public Faction getRandomEnemy(ILocation location, LocalDate date, @Nullable Faction employer,
          EnemySelectionProfile profile) {
        if (employer == null) {
            return rebelFallback("No employer supplied or faction does not exist. Returning REBEL");
        }

        return profile.selectEnemy(this, location, date, employer);
    }

    /**
     * @param faction the faction to check
     *
     * @return {@code true} if the faction is one of the "aggregate" factions &mdash; pirates, the Bandit Caste, rebels,
     *       mercenaries &mdash; whose innumerable independent bands, cells, and companies routinely fight each other
     *       without any specific war ever being declared between them. Unlike a real government's civil war, this
     *       doesn't need a {@code factionHints} war record: it's true of the faction unconditionally.
     */
    private static boolean isSelfConflictingFaction(Faction faction) {
        return faction.isAggregate() && (faction.isPirate() || faction.isRebel() || faction.isMercenary());
    }

    /**
     * Picks a faction the employer is at war with, falling back to the standard pool when the employer isn't at war
     * with anyone.
     *
     * @param location the location to center the fallback pool search on
     * @param date     the date to check diplomatic relations against
     * @param employer the employer faction
     *
     * @return the selected enemy faction
     */
    public Faction atWarEnemy(ILocation location, LocalDate date, Faction employer) {
        List<Faction> belligerents = findEnemiesAtWarWith(employer, date);
        if (!belligerents.isEmpty()) {
            return ObjectUtility.getRandomItem(belligerents);
        }
        return getRandomEnemy(false, location, date, employer);
    }

    /**
     * Picks a faction at war with the employer that occupies a world recently taken from it (within the search area,
     * using the same lookback window as {@link MissionLocationProfile#OCCUPIED_TERRITORY}'s location tier), then any
     * war partner, then the standard pool.
     *
     * @param location the location to center the search on
     * @param date     the date to check faction control and diplomatic relations against
     * @param employer the employer faction
     *
     * @return the selected enemy faction
     */
    public Faction occupyingPowerEnemy(ILocation location, LocalDate date, Faction employer) {
        List<Faction> belligerents = findEnemiesAtWarWith(employer, date);
        if (belligerents.isEmpty()) {
            return getRandomEnemy(false, location, date, employer);
        }

        List<Faction> occupiers = new ArrayList<>();
        for (Faction belligerent : belligerents) {
            // A civil-war employer can be its own belligerent, but its long-held worlds are not "conquests from
            // itself" - self only qualifies via the plain belligerents tier below.
            if (!belligerent.equals(employer) && holdsRecentConquestFrom(belligerent, employer, location, date)) {
                occupiers.add(belligerent);
            }
        }
        if (!occupiers.isEmpty()) {
            return ObjectUtility.getRandomItem(occupiers);
        }
        return ObjectUtility.getRandomItem(belligerents);
    }

    /**
     * Finds every faction the employer is at war with on the given date that also controls at least one known system,
     * applying the same basic eligibility rules as the standard pool (real, currently valid, not
     * Fortress-Republic-locked). The employer itself qualifies either when it's one of the aggregate factions that are
     * always at war with themselves (see {@link #isSelfConflictingFaction}), or when factionHints records a real
     * government at war with itself, which is how a civil war is represented. Serves both the
     * {@link EnemySelectionProfile#AT_WAR}-family preferences and {@link #buildEnemyMap}'s guarantee that a war partner
     * is a valid target regardless of local presence. War relationships are sparse, so the war check runs first (cheap,
     * over the whole faction roster) before the territory scan (only for the handful of actual war partners).
     *
     * @param employer the employer faction
     * @param date     the date to check diplomatic relations and faction control against
     *
     * @return the employer's territorial war partners, possibly empty
     */
    private List<Faction> findEnemiesAtWarWith(Faction employer, LocalDate date) {
        List<Faction> belligerents = new ArrayList<>();
        for (Faction faction : Factions.getInstance().getFactions()) {
            if (!isEligibleFaction(faction, date)) {
                continue;
            }
            boolean atWar = faction.equals(employer) ?
                                  isSelfConflictingFaction(employer) || factionHints.isAtWarWith(employer, employer,
                                        date) :
                                  factionHints.isAtWarWith(employer, faction, date);
            if (atWar && borderTracker.controlsAnySystem(faction, date)) {
                belligerents.add(faction);
            }
        }
        return belligerents;
    }

    /**
     * Checks whether {@code occupier} currently holds a system within the search radius that {@code formerOwner} held
     * {@value MissionLocationProfile#OCCUPIED_TERRITORY_LOOKBACK_YEARS} years ago, stopping at the first match.
     *
     * @param occupier    the faction suspected of occupying the former owner's territory
     * @param formerOwner the faction whose lost worlds are being looked for
     * @param location    the location to center the search on
     * @param date        the current date; the "before" ownership check uses the shared lookback window
     *
     * @return {@code true} if at least one such occupied system exists in range
     */
    private boolean holdsRecentConquestFrom(Faction occupier, Faction formerOwner, ILocation location,
          LocalDate date) {
        PlanetarySystem origin = location.getCurrentSystem();
        if (origin == null) {
            return false;
        }

        LocalDate beforeConquest = date.minusYears(MissionLocationProfile.OCCUPIED_TERRITORY_LOOKBACK_YEARS);
        for (PlanetarySystem system : borderTracker.systemsNear(origin, borderTracker.getRadius())) {
            if (system.getFactionSet(date).contains(occupier) &&
                      system.getFactionSet(beforeConquest).contains(formerOwner)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs the given warning and returns the REBEL faction, used as {@link #getRandomEnemy}'s fallback when no employer
     * is supplied or no valid enemy candidate could be found.
     *
     * @param message the warning message (may contain {@code {}} placeholders)
     * @param args    arguments for the message's placeholders
     *
     * @return the faction faction
     */
    private Faction rebelFallback(String message, Object... args) {
        LOGGER.warn(message, args);
        return Factions.getInstance().getFaction(REBEL_FACTION_CODE);
    }

    /**
     * Builds a weighted pool of enemy candidates for {@code employer} within the search area around {@code location}.
     * Each faction's weight starts at the number of systems it controls in range; factions at war with the employer are
     * added regardless of area presence, as long as they control territory anywhere; allies (direct or through a shared
     * ally, see {@link #performIsAllyCheck}) are excluded; and remaining weights are adjusted for diplomatic stance
     * (see {@link #adjustEnemyWeight}). A pirate employer bypasses all diplomatic checks. The employer itself is a
     * valid candidate only when factionHints records it at war with itself &mdash; a civil war.
     *
     * @param isCovert whether allies should be rare, low-chance targets instead of always excluded
     * @param location the location to center the search on
     * @param date     the date to check faction control and diplomatic relations against
     * @param employer the employer faction
     *
     * @return a weighted map of enemy candidates, empty if {@code location} has no current system
     */
    protected WeightedIntMap<Faction> buildEnemyMap(boolean isCovert, ILocation location, LocalDate date,
          Faction employer) {
        WeightedIntMap<Faction> enemyMap = new WeightedIntMap<>();
        PlanetarySystem system = location.getCurrentSystem();
        if (system == null) {
            return enemyMap;
        }

        Map<Faction, Integer> counts = countRegionalPresence(system, date);

        String employerShortName = employer.getShortName();
        boolean isPirateEmployer = employerShortName.equals(PIRATE_FACTION_CODE) ||
                                         employerShortName.equals(BANDIT_CASTE_FACTION_CODE);

        // A faction at war with the employer is always a valid target, even one with no systems in the search
        // area, as long as it controls territory somewhere on the map.
        Set<Faction> candidates = new HashSet<>(counts.keySet());
        if (!isPirateEmployer) {
            candidates.addAll(findEnemiesAtWarWith(employer, date));
        }

        // This only depends on the date, not on any individual candidate, so compute them once per call rather
        // than once per candidate in adjustEnemyWeight.
        boolean isDuringJihad = date.isAfter(MHQConstants.JIHAD_START) && date.isBefore(MHQConstants.NOMINAL_JIHAD_END);

        for (Faction enemy : candidates) {
            if (!isEligibleFaction(enemy, date)) {
                continue;
            }

            // A neutral faction is one that is never normally considered a valid target under any circumstances and
            // therefore is entirely skipped.
            boolean isNeutral = factionHints.isNeutral(employer, enemy, date);
            if (isNeutral) {
                continue;
            }

            if (isPirateEmployer) {
                enemyMap.add(1, enemy);
                continue;
            }

            // A faction is never its own enemy, with two exceptions: an aggregate faction whose bands, cells, or
            // companies fight each other constantly with no war ever formally declared (see
            // isSelfConflictingFaction), or factionHints explicitly recording it at war with itself, which is how a
            // civil war is represented for a real government (e.g. the Ghost Bear Civil War).
            if (enemy.equals(employer) &&
                      !isSelfConflictingFaction(employer) &&
                      !factionHints.isAtWarWith(employer, employer, date)) {
                continue;
            }

            // Factions that don't have a direct alliance record but share a common ally (e.g. two member states of
            // the same superpower, each individually allied with it but not with each other) should still be treated
            // as allies, unless factionHints directly contradicts that by recording them as at war with each other.
            boolean isAlly = performIsAllyCheck(date, employer, enemy);

            // During covert operations, allies may sometimes attack each other. However, this is a very low chance
            if (isCovert && isAlly) {
                enemyMap.add(1, enemy);
                continue;
            }

            if (isAlly) {
                continue;
            }

            double weight = adjustEnemyWeight(counts.getOrDefault(enemy, 0),
                  employer,
                  enemy,
                  date,
                  isDuringJihad);
            if (weight > 0) {
                enemyMap.add((int) Math.round(weight), enemy);
            }
        }

        return enemyMap;
    }

    /**
     * Checks whether two factions should be treated as allies: directly, or through a shared ally (e.g. two member
     * states of the same superpower). An explicit war record between them overrides either kind of alliance &mdash; a
     * formal pact that hasn't been expunged from the data doesn't stop a shooting war (e.g. the Star League and the
     * Amaris-held Terran Hegemony, nominally allied for the whole League era but at war from the coup on).
     *
     * @param date     the date to check diplomatic relations against
     * @param employer one faction
     * @param enemy    the other faction
     *
     * @return {@code true} if the two factions should be treated as allied
     */
    private boolean performIsAllyCheck(LocalDate date, Faction employer, Faction enemy) {
        if (factionHints.isAtWarWith(employer, enemy, date)) {
            return false;
        }
        return factionHints.isAlliedWith(employer, enemy, date) ||
                     factionHints.isAlliedThroughSharedAlly(employer, enemy, date);
    }

    /**
     * @return A set of keys for all current factions in the space that are potential employers.
     */
    @Deprecated(since = "0.51.01")
    public Set<String> getEmployerSet() {
        Set<String> set = new HashSet<>();
        LocalDate currentDate = getCurrentDate();
        for (Faction faction : borderTracker.getFactionsInRegion()) {
            // Skip factions whose stated active years don't include the current date.
            // Stale planet ownership data can otherwise leak extinct factions (e.g. ARC after 3028) into the pool.
            if (!faction.validIn(currentDate)) {
                continue;
            }
            if (Faction.isDuringFortressRepublic(faction.getShortName(), currentDate, null)) {
                continue;
            }
            if (!faction.isClan() && !FactionHints.isEmptyFaction(faction)) {
                set.add(faction.getShortName());
            }
            /* Add factions which do not control any planets to the employer list */
            for (Faction containedFaction : factionHints.getContainedFactions(faction, currentDate)) {
                if (!containedFaction.isClan() && containedFaction.validIn(currentDate)) {
                    set.add(containedFaction.getShortName());
                }
            }
        }
        return set;
    }

    /**
     * Constructs a list of a faction's potential enemies based on common borders.
     *
     * @param employerName The shortName of the employer faction
     *
     * @return A list of faction that share a border
     */
    @Deprecated(since = "0.51.01")
    public List<String> getEnemyList(String employerName) {
        Faction employer = Factions.getInstance().getFaction(employerName);
        if (null == employer) {
            LOGGER.warn("Unknown faction key: {}", employerName);
            return Collections.emptyList();
        }
        return getEnemyList(Factions.getInstance().getFaction(employerName));
    }

    /**
     * Constructs a list of a faction's potential enemies based on common borders.
     *
     * @param employer The employer faction
     *
     * @return A list of faction that share a border
     */
    @Deprecated(since = "0.51.01")
    public List<String> getEnemyList(Faction employer) {
        Set<Faction> list = new HashSet<>();
        LocalDate currentDate = getCurrentDate();
        Faction outer = factionHints.getContainedFactionHost(employer, currentDate);
        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy)) {
                continue;
            }

            if (enemy.isAggregate()) {
                if (!enemy.isMercenary() && !enemy.isPirate()) {
                    continue;
                }
            }

            // Skip extinct factions; stale planet ownership data can otherwise leak them into the enemy list.
            if (!enemy.validIn(currentDate)) {
                continue;
            }
            if (enemy.equals(employer) && !factionHints.isAtWarWith(enemy, enemy, currentDate)) {
                continue;
            }
            if (factionHints.isAlliedWith(employer, enemy, currentDate) && !employer.isClan() && !enemy.isClan()) {
                continue;
            }
            if (factionHints.isNeutral(employer, enemy, currentDate) ||
                      factionHints.isNeutral(enemy, employer, currentDate)) {
                continue;
            }
            Faction useBorder = employer;
            if (null != outer) {
                if (!factionHints.isContainedFactionOpponent(outer, employer, enemy, currentDate)) {
                    continue;
                }
                useBorder = outer;
            }

            if (!borderTracker.getBorderSystems(useBorder, enemy).isEmpty()) {
                list.add(enemy);
                for (Faction containedFaction : factionHints.getContainedFactions(enemy, currentDate)) {
                    if ((null != containedFaction) && containedFaction.validIn(currentDate) &&
                              factionHints.isContainedFactionOpponent(enemy, containedFaction, employer, currentDate)) {
                        list.add(containedFaction);
                    }
                }
            }
        }
        return list.stream().map(Faction::getShortName).collect(Collectors.toList());
    }

    /**
     * Applies diplomatic-stance multipliers to an enemy candidate's base area-presence weight (see
     * {@link #buildEnemyMap(boolean, ILocation, LocalDate, Faction)}). Factions at war with the employer are floored to
     * a weight of at least 10 before quadrupling, so a belligerent with no systems in the search area is still a valid,
     * heavily-weighted, pickable target (e.g. distant warring factions during the early Clan Invasion or the
     * Reunification Wars).
     *
     * @param count         The candidate's base weight (number of systems it controls in the search area)
     * @param employer      The attacking faction
     * @param enemy         The defending faction
     * @param date          The date to check diplomatic relations against
     * @param isDuringJihad Whether {@code date} falls within the Jihad
     *
     * @return The adjusted weight
     */
    protected double adjustEnemyWeight(int count, Faction employer, Faction enemy, LocalDate date,
          boolean isDuringJihad) {
        double weight = count;
        if (factionHints.isAtWarWith(employer, enemy, date)) {
            // minimum 'count' enforced for warring factions. This allows us to better ensure fighting between warring
            // factions that are not near each other. For example, the early years of the Clan Invasion, or the
            // Reunification Wars.
            weight = max(10.0, weight);

            weight *= 4.0;
        }

        if (factionHints.isRivalOf(employer, enemy, date)) {
            weight *= 2.0;
        }

        if (isDuringJihad && enemy.isWoB()) {
            weight *= 2.0;
        }

        /*
         * This is pretty hacky, but ComStar does not have many targets
         * and tends to fight the Clans too much between Tukayyid and
         * the Jihad.
         */
        if (employer.getShortName().equals(COMSTAR_FACTION_CODE) && enemy.isClan()) {
            weight /= 12.0;
        }

        return weight;
    }

    /**
     * Selects a random planet from a list of potential targets based on the attacking and defending factions, with no
     * contract-type location preference. Equivalent to calling
     * {@link #getMissionTarget(String, String, ILocation, MissionLocationProfile)} with
     * {@link MissionLocationProfile#DEFAULT}.
     *
     * @param attacker The faction key of the attacker
     * @param defender The faction key of the defender
     * @param location the location to center the search on, scoped by this generator's configured search radius (see
     *                 {@link #getMissionTargetList(Faction, Faction, ILocation)})
     *
     * @return The planetId of the chosen planet, or null if there are no target candidates
     */
    @Nullable
    public String getMissionTarget(String attacker, String defender, ILocation location) {
        return getMissionTarget(attacker, defender, location, MissionLocationProfile.DEFAULT);
    }

    /**
     * Selects a random planet from a list of potential targets based on the attacking and defending factions, applying
     * the given contract-type location profile to both the candidate search (see
     * {@link MissionTargetFinder#find(Faction, Faction, ILocation, LocalDate, MissionLocationProfile)}) and the final
     * pick: for {@linkplain MissionLocationProfile#isPopulationWeighted() population-weighted} profiles the pick is
     * weighted toward populous, well-connected worlds instead of being uniformly random.
     *
     * @param attacker The faction key of the attacker
     * @param defender The faction key of the defender
     * @param location the location to center the search on, scoped by this generator's configured search radius (see
     *                 {@link #getMissionTargetList(Faction, Faction, ILocation)})
     * @param profile  the location profile for the contract's type
     *
     * @return The planetId of the chosen planet, or null if there are no target candidates
     */
    @Nullable
    public String getMissionTarget(String attacker, String defender, ILocation location,
          MissionLocationProfile profile) {
        Faction attackerFaction = Factions.getInstance().getFaction(attacker);
        Faction defenderFaction = Factions.getInstance().getFaction(defender);
        if (null == attackerFaction) {
            LOGGER.error("Non-existent faction key: {}", attacker);
            return null;
        }
        if (null == defenderFaction) {
            LOGGER.error("Non-existent faction key: {}", defender);
            return null;
        }
        List<PlanetarySystem> planetList = getMissionTargetList(attackerFaction, defenderFaction, location, profile);
        if (planetList.isEmpty()) {
            return null;
        }
        if (profile.isPopulationWeighted()) {
            return pickPopulationWeighted(planetList, profile).getId();
        }
        return ObjectUtility.getRandomItem(planetList).getId();
    }

    /**
     * Weight bonus for a system with a major (A- or B-rated) HPG station in the population-weighted pick: a hub world
     * matters beyond its raw head count.
     */
    private static final int MAJOR_HPG_WEIGHT_BONUS = 3;

    /**
     * Per-point weight bonus for a system's industrial capacity (see {@link #industrialWeight}), applied for
     * {@linkplain MissionLocationProfile#isIndustriallyWeighted() industrially-weighted} profiles. Scaled up from the
     * raw 0-8 industrial score so a heavily industrialized world competes with a populous one rather than being drowned
     * out by the population term's log scale.
     */
    private static final int INDUSTRIAL_WEIGHT_MULTIPLIER = 2;

    /**
     * Picks a candidate weighted by {@link #populationWeight}, plus {@link #industrialWeight} for
     * {@linkplain MissionLocationProfile#isIndustriallyWeighted() industrially-weighted} profiles, so the most valuable
     * world is favored without making it the only possible outcome.
     *
     * @param candidates the candidate systems; must not be empty
     * @param profile    the location profile driving this pick, used to decide whether industrial capacity counts
     *
     * @return the chosen system
     */
    private PlanetarySystem pickPopulationWeighted(List<PlanetarySystem> candidates, MissionLocationProfile profile) {
        LocalDate now = getCurrentDate();
        WeightedIntMap<PlanetarySystem> weightedCandidates = new WeightedIntMap<>();
        for (PlanetarySystem system : candidates) {
            weightedCandidates.add(missionTargetWeight(system, now, profile), system);
        }
        PlanetarySystem chosen = weightedCandidates.randomItem();
        return (chosen != null) ? chosen : ObjectUtility.getRandomItem(candidates);
    }

    /**
     * Combines {@link #populationWeight} with {@link #industrialWeight} for
     * {@linkplain MissionLocationProfile#isIndustriallyWeighted() industrially-weighted} profiles, into the total
     * weight {@link #pickPopulationWeighted} uses for a single candidate.
     *
     * @param system  the system to score
     * @param when    the date to check population, HPG, and USILR rating against
     * @param profile the location profile driving this pick, used to decide whether industrial capacity counts
     *
     * @return the system's total weight for this profile, at least 1
     */
    static int missionTargetWeight(PlanetarySystem system, LocalDate when, MissionLocationProfile profile) {
        int weight = populationWeight(system, when);
        if (profile.isIndustriallyWeighted()) {
            weight += industrialWeight(system, when) * INDUSTRIAL_WEIGHT_MULTIPLIER;
        }
        return weight;
    }

    /**
     * Scores a system's value as a high-profile mission target. Population is scored on a log10 scale (a
     * billion-population world weighs ~10, a thousand-person outpost ~4) so major worlds are favored without drowning
     * out everything else, plus {@value #MAJOR_HPG_WEIGHT_BONUS} for an A/B-rated HPG. A system with no data at all
     * still gets a weight of 1, so it remains pickable.
     *
     * @param system the system to score
     * @param when   the date to check population and HPG rating against
     *
     * @return the system's weight, at least 1
     */
    static int populationWeight(PlanetarySystem system, LocalDate when) {
        int weight = 1;
        long population = system.getPopulation(when);
        if (population > 0) {
            weight += (int) Math.log10(population);
        }
        HPGRating hpg = system.getHPG(when);
        if ((hpg != null) && (hpg.compareTo(HPGRating.B) >= 0)) {
            weight += MAJOR_HPG_WEIGHT_BONUS;
        }
        return weight;
    }

    /**
     * Scores a system's industrial capacity from its USILR rating (see {@link SocioIndustrialData}): its industry and
     * output ratings, the two fields that directly measure production capacity rather than self-sufficiency. Each
     * rating contributes 0 (F, no capacity) to 4 (A, fully developed), for a combined range of 0-8 &mdash; a target
     * worth sabotaging, stealing secrets from, or conquering and holding for its factories.
     *
     * @param system the system to score
     * @param when   the date to check the USILR rating against
     *
     * @return the system's industrial score, from 0 to 8
     */
    static int industrialWeight(PlanetarySystem system, LocalDate when) {
        SocioIndustrialData socioIndustrial = system.getSocioIndustrial(when);
        return ratingScore(socioIndustrial.industry) + ratingScore(socioIndustrial.output);
    }

    /**
     * @param rating a USILR rating, where {@link PlanetaryRating#A} is best and {@link PlanetaryRating#F} is worst
     *
     * @return the rating's contribution to {@link #industrialWeight}, from 0 (F) to 4 (A)
     */
    private static int ratingScore(PlanetaryRating rating) {
        return PlanetaryRating.F.getIndex() - rating.getIndex();
    }

    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's planets.
     *
     * @param attackerKey The attacking faction's shortName
     * @param defenderKey The defending faction's shortName
     * @param location    the location to center the search on, scoped by this generator's configured search radius (see
     *                    {@link #getMissionTargetList(Faction, Faction, ILocation)})
     *
     * @return A list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(String attackerKey, String defenderKey, ILocation location) {
        Faction attacker = Factions.getInstance().getFaction(attackerKey);
        Faction defender = Factions.getInstance().getFaction(defenderKey);
        if (null == attacker) {
            LOGGER.error("Non-existent faction key (attacker): {}", attackerKey);
        }
        if (null == defender) {
            LOGGER.error("Non-existent faction key (defender): {}", defenderKey);
        }
        if ((null != attacker) && (null != defender)) {
            return getMissionTargetList(attacker, defender, location);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Builds a list of potential mission-target planets near {@code location}, generally on the shared border between
     * attacker and defender, with dedicated tiers for factions whose territory doesn't work like a normal nation's
     * (pirates, ComStar, rebels). Outside of those special cases, only planets the defender actually owns are ever
     * valid targets. See {@link MissionTargetFinder} for the full selection logic.
     *
     * @param attacker the attacking faction
     * @param defender the defending faction
     * @param location the location to center the search on
     *
     * @return a list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(Faction attacker, Faction defender, ILocation location) {
        return getMissionTargetList(attacker, defender, location, MissionLocationProfile.DEFAULT);
    }

    /**
     * As {@link #getMissionTargetList(Faction, Faction, ILocation)}, but applying the given contract-type location
     * profile's preferred tier before the default border-based search (see
     * {@link MissionTargetFinder#find(Faction, Faction, ILocation, LocalDate, MissionLocationProfile)}).
     *
     * @param attacker the attacking faction
     * @param defender the defending faction
     * @param location the location to center the search on
     * @param profile  the location profile for the contract's type
     *
     * @return a list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(Faction attacker, Faction defender, ILocation location,
          MissionLocationProfile profile) {
        return missionTargetFinder.find(attacker, defender, location, getCurrentDate(), profile);
    }
}
