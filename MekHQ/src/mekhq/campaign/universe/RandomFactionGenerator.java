/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.MHQConstants.FORTRESS_REPUBLIC;
import static mekhq.campaign.universe.Faction.COMSTAR_FACTION_CODE;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.factionHints.FactionHints;

/**
 * @author Neoancient
 *       <p>
 *       Uses Factions and Planets to weighted lists of potential employers and enemies for contract generation. Also
 *       finds a suitable planet for the action.
 *                                                                                     TODO : Account for the de facto alliance of the invading Clans and the
 *                                                                                     TODO : Fortress Republic in a way that doesn't involve hard-coding them here.
 */
public class RandomFactionGenerator {
    private static final MMLogger LOGGER = MMLogger.create(RandomFactionGenerator.class);

    private static RandomFactionGenerator rfg = null;

    private FactionBorderTracker borderTracker;
    private FactionHints factionHints;

    public RandomFactionGenerator() {
        this(null, null);
    }

    public RandomFactionGenerator(FactionBorderTracker borderTracker,
          FactionHints factionHints) {
        this.borderTracker = borderTracker;
        this.factionHints = factionHints;
        if (null == borderTracker) {
            initDefaultBorderTracker();
        }
        if (null == factionHints) {
            this.factionHints = FactionHints.getInstance();
        }
    }

    private void initDefaultBorderTracker() {
        borderTracker = new FactionBorderTracker();
        borderTracker.setDayThreshold(30);
        borderTracker.setDistanceThreshold(100);
        borderTracker.setDefaultBorderSize(MHQConstants.FACTION_GENERATOR_BORDER_RANGE_IS,
              MHQConstants.FACTION_GENERATOR_BORDER_RANGE_NEAR_PERIPHERY,
              MHQConstants.FACTION_GENERATOR_BORDER_RANGE_CLAN);
    }

    public static RandomFactionGenerator getInstance() {
        if (rfg == null) {
            rfg = new RandomFactionGenerator();
        }
        return rfg;
    }

    public static void setInstance(RandomFactionGenerator instance) {
        rfg = instance;
    }

    public void startup(Campaign c) {
        borderTracker.setDate(c.getLocalDate());
        final PlanetarySystem location = c.getLocation().getCurrentSystem();
        borderTracker.setRegionCenter(location.getX(), location.getY());
        borderTracker.setRegionRadius(c.getCampaignOptions().getContractSearchRadius());
        MekHQ.registerHandler(borderTracker);
        MekHQ.registerHandler(this);
        for (final Faction faction : Factions.getInstance().getFactions()) {
            if (faction.isDeepPeriphery()) {
                borderTracker.setBorderSize(faction, MHQConstants.FACTION_GENERATOR_BORDER_RANGE_DEEP_PERIPHERY);
            }
        }
    }

    public void setDate(LocalDate date) {
        borderTracker.setDate(date);
    }

    public FactionHints getFactionHints() {
        return factionHints;
    }

    public void dispose() {
        MekHQ.unregisterHandler(borderTracker);
        MekHQ.unregisterHandler(this);
    }

    private LocalDate getCurrentDate() {
        return borderTracker.getLastUpdated();
    }

    /**
     * @return A set of faction keys for all factions that have a presence within the search area.
     */
    public Set<String> getCurrentFactions() {
        Set<String> retVal = new TreeSet<>();
        for (Faction f : borderTracker.getFactionsInRegion()) {

            if (FactionHints.isEmptyFaction(f) ||
                      f.getShortName().equals("CLAN")) {
                continue;
            }
            if (f.getShortName().equals("ROS") && getCurrentDate().isAfter(MHQConstants.FORTRESS_REPUBLIC)) {
                continue;
            }

            retVal.add(f.getShortName());
            /* Add factions which do not control any planets to the employer list */
            factionHints.getContainedFactions(f, getCurrentDate()).forEach(cf -> retVal.add(cf.getShortName()));
        }
        // Add rebels and pirates
        retVal.add("REB");
        retVal.add(PIRATE_FACTION_CODE);
        return retVal;
    }

    /**
     * Builds map of potential employers weighted by number of systems controlled within the search area
     *
     * @return Map used to select employer
     */
    protected WeightedIntMap<Faction> buildEmployerMap() {
        WeightedIntMap<Faction> retVal = new WeightedIntMap<>();
        for (Faction f : borderTracker.getFactionsInRegion()) {

            if (f.isClan() || FactionHints.isEmptyFaction(f)) {
                continue;
            }
            if (f.getShortName().equals("ROS") && getCurrentDate().isAfter(FORTRESS_REPUBLIC)) {
                continue;
            }

            int weight = borderTracker.getBorders(f).getSystems().size();
            retVal.add(weight, f);

            /* Add factions which do not control any planets to the employer list */
            for (Faction cfaction : factionHints.getContainedFactions(f, getCurrentDate())) {
                if (null != cfaction) {
                    if (!cfaction.isClan()) {
                        weight = (int) Math.floor((borderTracker.getBorders(f).getSystems().size() *
                                                         factionHints.getAltLocationFraction(f,
                                                               cfaction,
                                                               getCurrentDate())) + 0.5);
                        retVal.add(weight, f);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Selects a Faction from those with a presence in the region weighted by number of systems controlled. Excludes
     * Clan Factions and non-faction placeholders (unknown, abandoned, none).
     *
     * @return A Faction to use as the employer for a contract.
     */
    public @Nullable Faction getEmployerFaction() {
        return buildEmployerMap().randomItem();
    }

    /**
     * @since 0.50.04
     * @deprecated use {@link #getEmployerFaction()} instead
     */
    @Deprecated(since = "0.50.04")
    public String getEmployer() {
        WeightedIntMap<Faction> employers = buildEmployerMap();
        Faction f = employers.randomItem();
        if (null != f) {
            return f.getShortName();
        }
        return null;
    }

    /**
     * Selects an enemy faction for the given employer, weighted by length of shared border and diplomatic relations.
     * Factions at war or designated as rivals are twice as likely (cumulative) to be chosen as opponents. Allied
     * factions are ignored except for Clans, which halves the weight for that option.
     *
     * @param employer  The shortName of the faction offering the contract
     * @param useRebels Whether to include rebels as a possible opponent
     *
     * @return The shortName of the faction to use as the op for.
     */
    public String getEnemy(String employer, boolean useRebels) {
        Faction employerFaction = Factions.getInstance().getFaction(employer);
        if (null == employerFaction) {
            LOGGER.error("Could not find enemy for employer: {}", employer);
            return PIRATE_FACTION_CODE;
        } else {
            return getEnemy(employerFaction, useRebels);
        }
    }

    /**
     * Pick an enemy faction, possibly rebels or mercenaries, given an employer.
     */
    public String getEnemy(Faction employer, boolean useRebels) {
        return getEnemy(employer, useRebels, true);
    }

    /**
     * Selects an enemy faction for the given employer, weighted by length of shared border and diplomatic relations.
     * Factions at war or designated as rivals are twice as likely (cumulative) to be chosen as opponents. Allied
     * factions are ignored except for Clans, which halves the weight for that option.
     *
     * @param employer  The faction offering the contract
     * @param useRebels Whether to include rebels as a possible opponent
     * @param useMercs  Whether to include MERC as a possible opponent. Note, don't do this when first generating
     *                  contract, as contract generation relies on the op for having planets
     *
     * @return The faction to use as the op for.
     */
    public String getEnemy(Faction employer, boolean useRebels, boolean useMercs) {
        String employerName = employer != null ?
                                    employer.getShortName() :
                                    "no employer supplied or faction does not exist";

        /* Rebels occur on a 1-4 (d20) on nearly every enemy chart */
        if (useRebels && (Compute.randomInt(5) == 0)) {
            return "REB";
        }

        Faction enemy = null;
        if (!borderTracker.getFactionsInRegion().contains(employer)) {
            employer = factionHints.getContainedFactionHost(employer, getCurrentDate());
        }
        if (null != employer) {
            employerName = employer.getShortName();
            WeightedIntMap<Faction> enemyMap = buildEnemyMap(employer);

            if (useMercs) {
                appendMercsToEnemyMap(enemyMap);
            }

            enemy = enemyMap.randomItem();
        }
        if (null != enemy) {
            return enemy.getShortName();
        }

        LOGGER.error("Could not find enemy for employerName {}", employerName);

        // Fallback; there are always pirates.
        return PIRATE_FACTION_CODE;
    }

    /**
     * Appends the mercenary faction to the given enemy map with an approximate weight equal to 10% of the total enemy
     * weight, but only if the enemy map contains at least one non-Clan faction.
     *
     * <p>The method first checks if any faction in the enemy map returns {@code false} from {@link Faction#isClan()}.
     * If all factions are Clan, no action is taken. Otherwise, the mercenary faction is added to the enemy map with a
     * weight based on the sum of existing weights (at least 1).</p>
     *
     * <p>The check for non-Clan factions is to help avoid a situation where we have mercenary companies popping up
     * in Clan-space.</p>
     *
     * @param enemyMap the {@link WeightedIntMap} of {@link Faction} to which the mercenary faction may be appended
     */
    protected void appendMercsToEnemyMap(WeightedIntMap<Faction> enemyMap) {
        boolean hasNonClan = false;
        for (Faction faction : enemyMap.values()) {
            if (!faction.isClan()) {
                hasNonClan = true;
                break;
            }
        }

        if (!hasNonClan) {
            return;
        }

        int mercWeight = 0;
        for (int key : enemyMap.keySet()) {
            mercWeight += key;
        }

        enemyMap.add(Math.max(1, (mercWeight / 10)), Factions.getInstance().getFaction(MERCENARY_FACTION_CODE));
    }

    /**
     * Builds a map of potential enemies keyed to cumulative weight
     *
     * @param employer The employer faction
     *
     * @return The weight map of potential enemies
     */
    protected WeightedIntMap<Faction> buildEnemyMap(Faction employer) {
        WeightedIntMap<Faction> enemyMap = new WeightedIntMap<>();

        // If the employer is a pirate, or comstar return all border factions as "enemies"
        String employerShortName = employer.getShortName();
        if (employerShortName.equals(PIRATE_FACTION_CODE) || employerShortName.equals(COMSTAR_FACTION_CODE)) {
            for (Faction enemy : borderTracker.getFactionsInRegion()) {
                if (FactionHints.isEmptyFaction(enemy) ||
                          enemy.getShortName().equals("CLAN")) {
                    continue;
                }
                enemyMap.add(1, enemy); // weight (1) can be adjusted as needed
            }
            return enemyMap;
        }

        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy) ||
                      enemy.getShortName().equals("CLAN")) {
                continue;
            }

            if (enemy.getShortName().equals("ROS") && getCurrentDate().isAfter(FORTRESS_REPUBLIC)) {
                continue;
            }

            int totalCount = borderTracker.getBorderSystems(employer, enemy).size();
            double count = totalCount;
            // Split the border between main controlling faction and any contained factions.
            for (Faction cFaction : factionHints.getContainedFactions(employer, getCurrentDate())) {
                if ((null == cFaction) ||
                          !factionHints.isContainedFactionOpponent(enemy, cFaction, employer, getCurrentDate())) {
                    continue;
                }

                if (cFaction.getShortName().equals("ROS") && getCurrentDate().isAfter(FORTRESS_REPUBLIC)) {
                    continue;
                }

                if (factionHints.isNeutral(cFaction, enemy, getCurrentDate()) ||
                          factionHints.isNeutral(enemy, cFaction, getCurrentDate())) {
                    continue;
                }
                double cfCount = totalCount;
                if (factionHints.getAltLocationFraction(employer, cFaction, getCurrentDate()) > 0.0) {
                    cfCount = totalCount * factionHints.getAltLocationFraction(employer, cFaction, getCurrentDate());
                    count -= cfCount;
                }
                cfCount = adjustBorderWeight(cfCount, employer, enemy, getCurrentDate());
                enemyMap.add((int) Math.floor(cfCount + 0.5), cFaction);
            }
            count = adjustBorderWeight(count, employer, enemy, getCurrentDate());
            enemyMap.add((int) Math.floor(count + 0.5), enemy);
        }

        return enemyMap;
    }

    /**
     * @return A set of keys for all current factions in the space that are potential employers.
     */
    public Set<String> getEmployerSet() {
        Set<String> set = new HashSet<>();
        for (Faction f : borderTracker.getFactionsInRegion()) {
            if (!f.isClan() && !FactionHints.isEmptyFaction(f)) {
                set.add(f.getShortName());
            }
            if (f.getShortName().equals("ROS") && getCurrentDate().isAfter(FORTRESS_REPUBLIC)) {
                continue;
            }
            /* Add factions which do not control any planets to the employer list */
            for (Faction cfaction : factionHints.getContainedFactions(f, getCurrentDate())) {
                if (!cfaction.isClan()) {
                    set.add(cfaction.getShortName());
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
    public List<String> getEnemyList(Faction employer) {
        Set<Faction> list = new HashSet<>();
        Faction outer = factionHints.getContainedFactionHost(employer, getCurrentDate());
        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy)) {
                continue;
            }
            if (enemy.equals(employer) && !factionHints.isAtWarWith(enemy, enemy, getCurrentDate())) {
                continue;
            }
            if (factionHints.isAlliedWith(employer, enemy, getCurrentDate()) && !employer.isClan() && !enemy.isClan()) {
                continue;
            }
            if (factionHints.isNeutral(employer, enemy, getCurrentDate()) ||
                      factionHints.isNeutral(enemy, employer, getCurrentDate())) {
                continue;
            }
            Faction useBorder = employer;
            if (null != outer) {
                if (!factionHints.isContainedFactionOpponent(outer, employer, enemy, getCurrentDate())) {
                    continue;
                }
                useBorder = outer;
            }

            if (!borderTracker.getBorderSystems(useBorder, enemy).isEmpty()) {
                list.add(enemy);
                for (Faction cf : factionHints.getContainedFactions(enemy, getCurrentDate())) {
                    if ((null != cf) &&
                              factionHints.isContainedFactionOpponent(enemy, cf, employer, getCurrentDate())) {
                        list.add(cf);
                    }
                }
            }
        }
        return list.stream().map(Faction::getShortName).collect(Collectors.toList());
    }

    /**
     * Applies modifiers to the border size (measured by number of planets within a certain proximity to one or more of
     * the attacker's planets) based on diplomatic stance (e.g. war, rivalry, alliance).
     *
     * @param count    The number of planets
     * @param employer The attacking faction
     * @param enemy    The defending faction
     * @param date     The current campaign date
     *
     * @return An adjusted weight
     */
    protected double adjustBorderWeight(double count, Faction employer, Faction enemy, LocalDate date) {
        boolean isBeforeTukayyid = date.isBefore(MHQConstants.BATTLE_OF_TUKAYYID);
        boolean isAfterFirstWaveBegins = date.isAfter(MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS);
        boolean isDuringClanInvasionHeight = isBeforeTukayyid && isAfterFirstWaveBegins;
        List<String> innerSphereClanWarCombatants = List.of("FC", "FRR", "DC");

        if (factionHints.isNeutral(employer, enemy, getCurrentDate()) ||
                  factionHints.isNeutral(enemy, employer, getCurrentDate())) {
            return 0;
        }
        if (!employer.isClan() && factionHints.isAlliedWith(employer, enemy, date)) {
            return 0;
        }
        if (employer.isClan() &&
                  enemy.isClan() &&
                  (factionHints.isAlliedWith(employer, enemy, date) ||
                         (isDuringClanInvasionHeight && (borderTracker.getCenterY() < 600)))) {
            /* Treat invading Clans as allies in the Inner Sphere */
            count /= 4.0;
        }
        if (factionHints.isAtWarWith(employer, enemy, date) && !employer.equals(enemy)) {
            count *= 2.0;
        }
        if (factionHints.isRivalOf(employer, enemy, date)) {
            count *= 2.0;
        }
        if (innerSphereClanWarCombatants.contains(employer.getShortName()) &&
                  enemy.isClan() &&
                  isDuringClanInvasionHeight) {
            count *= 2.0;
        }
        /*
         * This is pretty hacky, but ComStar does not have many targets
         * and tends to fight the Clans too much between Tukayyid and
         * the Jihad.
         */
        if (employer.getShortName().equals("CS") && enemy.isClan()) {
            count /= 12.0;
        }
        return count;
    }

    /**
     * Selects a random planet from a list of potential targets based on the attacking and defending factions.
     *
     * @param attacker The faction key of the attacker
     * @param defender The faction key of the defender
     *
     * @return The planetId of the chosen planet, or null if there are no target candidates
     */
    @Nullable
    public String getMissionTarget(String attacker, String defender) {
        Faction f1 = Factions.getInstance().getFaction(attacker);
        Faction f2 = Factions.getInstance().getFaction(defender);
        if (null == f1) {
            LOGGER.error("Non-existent faction key: {}", attacker);
            return null;
        }
        if (null == f2) {
            LOGGER.error("Non-existent faction key: {}", attacker);
            return null;
        }
        List<PlanetarySystem> planetList = getMissionTargetList(f1, f2);
        if (!planetList.isEmpty()) {
            return ObjectUtility.getRandomItem(planetList).getId();
        }
        return null;
    }

    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's planets.
     *
     * @param attackerKey The attacking faction's shortName
     * @param defenderKey The defending faction's shortName
     *
     * @return A list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(String attackerKey, String defenderKey) {
        Faction attacker = Factions.getInstance().getFaction(attackerKey);
        Faction defender = Factions.getInstance().getFaction(defenderKey);
        if (null == attacker) {
            LOGGER.error("Non-existent faction key (attacker): {}", attackerKey);
        }
        if (null == defender) {
            LOGGER.error("Non-existent faction key (defender): {}", defenderKey);
        }
        if ((null != attacker) && (null != defender)) {
            return getMissionTargetList(attacker, defender);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's planets.
     *
     * @param attacker The attacking faction
     * @param defender The defending faction
     *
     * @return A list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(Faction attacker, Faction defender) {
        boolean attackerIsPirate = attacker.isPirate();
        boolean attackerIsMerc = attacker.isMercenary();
        boolean attackerIsComStar = attacker.isComStar();
        boolean attackerHasNoPlanets = !borderTracker.getFactionsInRegion().contains(attacker);

        boolean defenderIsPirate = defender.isPirate();
        boolean defenderIsMerc = defender.isMercenary();
        boolean defenderIsComStar = defender.isComStar();
        boolean defenderHasNoPlanets = !borderTracker.getFactionsInRegion().contains(defender);

        // Faction host logic
        if (attackerHasNoPlanets && !attackerIsPirate && !attackerIsMerc && !attackerIsComStar) {
            attacker = factionHints.getContainedFactionHost(attacker, getCurrentDate());
        }
        if (defenderHasNoPlanets && !defender.isRebelOrPirate() && !defender.isMercenary() && !defenderIsComStar) {
            defender = factionHints.getContainedFactionHost(defender, getCurrentDate());
        }

        if (attacker == null || defender == null) {
            return Collections.emptyList();
        }

        // Special cases for pirates, mercenaries, and ComStar
        if (attackerIsPirate || attackerIsMerc || attackerIsComStar) {
            FactionBorders defenderBorders = borderTracker.getBorders(defender);
            return (defenderBorders == null) ? Collections.emptyList() : new ArrayList<>(defenderBorders.getSystems());
        }
        if (defender.isRebel()) {
            FactionBorders attackerBorders = borderTracker.getBorders(attacker);
            return (attackerBorders == null) ? Collections.emptyList() : new ArrayList<>(attackerBorders.getSystems());
        }

        // Main border calculation
        Set<PlanetarySystem> planetSet = new HashSet<>(borderTracker.getBorderSystems(attacker, defender));
        // Border systems in the case of pirates/mercenaries/ComStar with no planetary regions
        if ((defenderIsPirate || defenderIsMerc || defenderIsComStar) && defenderHasNoPlanets) {
            for (Faction regionalFaction : borderTracker.getFactionsInRegion()) {
                planetSet.addAll(borderTracker.getBorderSystems(regionalFaction, attacker));
                planetSet.addAll(borderTracker.getBorderSystems(attacker, regionalFaction));
            }
        }

        // Check contained factions if nothing found
        if (planetSet.isEmpty()) {
            for (Faction regionalFaction : borderTracker.getFactionsInRegion()) {
                for (Faction hintFaction : factionHints.getContainedFactions(regionalFaction, getCurrentDate())) {
                    if (hintFaction.equals(attacker) &&
                              factionHints.isContainedFactionOpponent(regionalFaction,
                                    hintFaction,
                                    defender,
                                    getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(regionalFaction, defender));
                    } else if (hintFaction.equals(defender) &&
                                     factionHints.isContainedFactionOpponent(regionalFaction,
                                           hintFaction,
                                           attacker,
                                           getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(attacker, regionalFaction));
                    }
                }
            }
        }

        return new ArrayList<>(planetSet);
    }
}
