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
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.time.LocalDate;
import java.time.Month;
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

/**
 * @author Neoancient
 *       <p>
 *       Uses Factions and Planets to weighted lists of potential employers and enemies for contract generation. Also
 *       finds a suitable planet for the action.
 *                         TODO : Account for the de facto alliance of the invading Clans and the
 *                         TODO : Fortress Republic in a way that doesn't involve hard-coding them here.
 */
public class RandomFactionGenerator {
    private static final MMLogger LOGGER = MMLogger.create(RandomFactionGenerator.class);

    private static RandomFactionGenerator rfg = null;

    private FactionBorderTracker borderTracker;
    private FactionHints factionHints;

    public RandomFactionGenerator() {
        this(null, null);
    }

    public RandomFactionGenerator(FactionBorderTracker borderTracker, FactionHints factionHints) {
        this.borderTracker = borderTracker;
        this.factionHints = factionHints;
        if (null == borderTracker) {
            initDefaultBorderTracker();
        }
        if (null == factionHints) {
            this.factionHints = FactionHints.defaultFactionHints();
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

            if (FactionHints.isEmptyFaction(f) || f.getShortName().equals("CLAN")) {
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
        retVal.add("PIR");
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
            return "PIR";
        } else {
            return getEnemy(employerFaction, useRebels);
        }
    }

    /**
     * Pick an enemy faction, possibly rebels, given an employer.
     */
    public String getEnemy(Faction employer, boolean useRebels) {
        return getEnemy(employer, useRebels, false);
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
        return "PIR";
    }

    /**
     * Appends MERC faction to the given enemy map, with approximately a 10% probability
     */
    protected void appendMercsToEnemyMap(WeightedIntMap<Faction> enemyMap) {
        int mercWeight = 0;
        for (int key : enemyMap.keySet()) {
            mercWeight += key;
        }

        enemyMap.add(Math.max(1, (mercWeight / 10)), Factions.getInstance().getFaction("MERC"));
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

        // If the employer is a pirate, return all border factions as "enemies"
        if (employer.getShortName().equals(PIRATE_FACTION_CODE)) {
            for (Faction enemy : borderTracker.getFactionsInRegion()) {
                if (FactionHints.isEmptyFaction(enemy) || enemy.getShortName().equals("CLAN")) {
                    continue;
                }
                enemyMap.add(1, enemy); // weight (1) can be adjusted as needed
            }
            return enemyMap;
        }

        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy) || enemy.getShortName().equals("CLAN")) {
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
     * @param count The number of planets
     * @param f     The attacking faction
     * @param enemy The defending faction
     * @param date  The current campaign date
     *
     * @return An adjusted weight
     */
    protected double adjustBorderWeight(double count, Faction f, Faction enemy, LocalDate date) {
        final LocalDate TUKKAYID = LocalDate.of(3052, Month.JUNE, 20);

        if (factionHints.isNeutral(f, enemy, getCurrentDate()) || factionHints.isNeutral(enemy, f, getCurrentDate())) {
            return 0;
        }
        if (!f.isClan() && factionHints.isAlliedWith(f, enemy, date)) {
            return 0;
        }
        if (f.isClan() &&
                  enemy.isClan() &&
                  (factionHints.isAlliedWith(f, enemy, date) ||
                         (date.isBefore(TUKKAYID) && (borderTracker.getCenterY() < 600)))) {
            /* Treat invading Clans as allies in the Inner Sphere */
            count /= 4.0;
        }
        if (factionHints.isAtWarWith(f, enemy, date) && !f.equals(enemy)) {
            count *= 2.0;
        }
        if (factionHints.isRivalOf(f, enemy, date)) {
            count *= 2.0;
        }
        /*
         * This is pretty hacky, but ComStar does not have many targets
         * and tends to fight the Clans too much between Tukayyid and
         * the Jihad.
         */
        if (f.getShortName().equals("CS") && enemy.isClan()) {
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
        // If the attacker or defender are not in the set of factions that control planets, and they are not rebels
        // or pirates, they will be a faction contained within another (e.g. Nova Cat in the Draconis Combine, or
        // Wolf-in-Exile in Lyran space
        if (!borderTracker.getFactionsInRegion().contains(attacker) && !attacker.isPirate()) {
            attacker = factionHints.getContainedFactionHost(attacker, getCurrentDate());
        }

        if (!borderTracker.getFactionsInRegion().contains(defender) && !defender.isRebelOrPirate()) {
            defender = factionHints.getContainedFactionHost(defender, getCurrentDate());
        }

        if ((null == attacker) || (null == defender)) {
            return Collections.emptyList();
        }

        // If the attacker is a pirate, any planet controlled by the defender is viable
        if (attacker.isPirate()) {
            final FactionBorders defenderBorders = borderTracker.getBorders(defender);
            return (defenderBorders == null) ? new ArrayList<>() : new ArrayList<>(defenderBorders.getSystems());
        }

        // Locate rebels on any of the attacker's planet
        if (defender.isRebel()) {
            final FactionBorders factionBorders = borderTracker.getBorders(attacker);
            return (factionBorders == null) ? new ArrayList<>() : new ArrayList<>(factionBorders.getSystems());
        }

        Set<PlanetarySystem> planetSet = new HashSet<>(borderTracker.getBorderSystems(attacker, defender));
        // If mission is against generic pirates (those that don't control any systems),
        // add all border systems as possible locations
        if ((attacker.isPirate() && !borderTracker.getFactionsInRegion().contains(attacker)) ||
                  (defender.isPirate() && !borderTracker.getFactionsInRegion().contains(defender))) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                planetSet.addAll(borderTracker.getBorderSystems(f, attacker));
                planetSet.addAll(borderTracker.getBorderSystems(attacker, f));
            }
        }

        /*
         * No border with defender found among systems controlled by
         * attacker; check for presence of attacker and defender
         * in systems controlled by other factions.
         */
        if (planetSet.isEmpty()) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                for (Faction cf : factionHints.getContainedFactions(f, getCurrentDate())) {
                    if (cf.equals(attacker) &&
                              factionHints.isContainedFactionOpponent(f, cf, defender, getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(f, defender));
                    }
                    if (cf.equals(defender) &&
                              factionHints.isContainedFactionOpponent(f, cf, attacker, getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(attacker, f));
                    }
                }
            }
        }
        return new ArrayList<>(planetSet);
    }
}
