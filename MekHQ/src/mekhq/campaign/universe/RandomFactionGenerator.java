/*
 * RandomFactionGenerator.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OptionsChangedEvent;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Neoancient
 *
 * Uses Factions and Planets to weighted lists of potential employers
 * and enemies for contract generation. Also finds a suitable planet
 * for the action.
 * TODO : Account for the de facto alliance of the invading Clans and the
 * TODO : Fortress Republic in a way that doesn't involve hard-coding them here.
 */
public class RandomFactionGenerator {
    /* When checking for potential enemies, count the planets controlled
     * by potentially hostile factions within a certain number of jumps of
     * friendly worlds; the number is based on the region of space.
     */
    private static final int BORDER_RANGE_IS = 60;
    private static final int BORDER_RANGE_CLAN = 90;
    private static final int BORDER_RANGE_NEAR_PERIPHERY = 90;
    private static final int BORDER_RANGE_DEEP_PERIPHERY = 210; //a bit more than this distance between HL and NC

    private static final LocalDate FORTRESS_REPUBLIC = LocalDate.of(3135, Month.NOVEMBER, 1);

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
        borderTracker.setDefaultBorderSize(BORDER_RANGE_IS, BORDER_RANGE_NEAR_PERIPHERY, BORDER_RANGE_CLAN);
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
        borderTracker.setRegionRadius(c.getCampaignOptions().getSearchRadius());
        MekHQ.registerHandler(borderTracker);
        MekHQ.registerHandler(this);
        for (Faction f : Factions.getInstance().getFactions()) {
            if (factionHints.isDeepPeriphery(f)) {
                borderTracker.setBorderSize(f, BORDER_RANGE_DEEP_PERIPHERY);
            }
        }
    }

    public void setDate(LocalDate date) {
        borderTracker.setDate(date);
    }

    public void setSearchCenter(double x, double y) {
        borderTracker.setRegionCenter(x, y);
    }

    public void setSearchCenter(Planet p) {
        borderTracker.setRegionCenter(p.getX(), p.getY());
    }

    public void setSearchRadius(double radius) {
        borderTracker.setRegionRadius(radius);
    }

    public FactionHints getFactionHints() {
        return factionHints;
    }

    @Subscribe
    public void handleCampaignOptionsChanged(OptionsChangedEvent event) {
        borderTracker.setRegionRadius(event.getOptions().getSearchRadius());
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

            if (FactionHints.isEmptyFaction(f)
                    || f.getShortName().equals("CLAN")) {
                continue;
            }
            if (f.getShortName().equals("ROS") && getCurrentDate().isAfter(FORTRESS_REPUBLIC)) {
                continue;
            }

            retVal.add(f.getShortName());
            /* Add factions which do not control any planets to the employer list */
            factionHints.getContainedFactions(f, getCurrentDate())
                    .forEach(cf -> retVal.add(cf.getShortName()));
        }
        //Add rebels and pirates
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
                        weight = (int) Math.floor((borderTracker.getBorders(f).getSystems().size()
                                * factionHints.getAltLocationFraction(f, cfaction, getCurrentDate())) + 0.5);
                        retVal.add(weight, f);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * Selects a faction from those with a presence in the region weighted by number of systems controlled.
     * Excludes Clan factions and non-faction place holders (unknown, abandoned, none).
     *
     * @return A faction to use as the employer for a contract.
     */
    public String getEmployer() {
        WeightedIntMap<Faction> employers = buildEmployerMap();
        Faction f = employers.randomItem();
        if (null != f) {
            return f.getShortName();
        }
        return null;
    }

    /**
     * Selects an enemy faction for the given employer, weighted by length of shared border and
     * diplomatic relations. Factions at war or designated as rivals are twice as likely (cumulative)
     * to be chosen as opponents. Allied factions are ignored except for Clans, which halves
     * the weight for that option.
     *
     * @param employer  The shortName of the faction offering the contract
     * @param useRebels Whether to include rebels as a possible opponent
     * @return          The shortName of the faction to use as the opfor.
     */
    public String getEnemy(String employer, boolean useRebels) {
        Faction employerFaction = Factions.getInstance().getFaction(employer);
        if (null == employerFaction) {
            MekHQ.getLogger().error("Could not find enemy for " + employer); //$NON-NLS-1$
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
     * Selects an enemy faction for the given employer, weighted by length of shared border and
     * diplomatic relations. Factions at war or designated as rivals are twice as likely (cumulative)
     * to be chosen as opponents. Allied factions are ignored except for Clans, which halves
     * the weight for that option.
     *
     * @param employer  The faction offering the contract
     * @param useRebels Whether to include rebels as a possible opponent
     * @param useMercs  Whether to include MERC as a possible opponent. Note, don't do this when
     * first generating contract, as contract generation relies on the opfor having planets
     * @return          The faction to use as the opfor.
     */
    public String getEnemy(Faction employer, boolean useRebels, boolean useMercs) {
        String employerName = employer != null ? employer.getShortName() : "no employer supplied or faction does not exist";

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

        MekHQ.getLogger().error("Could not find enemy for " + employerName); //$NON-NLS-1$

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
     * @return         The weight map of potential enemies
     */
    protected WeightedIntMap<Faction> buildEnemyMap(Faction employer) {
        WeightedIntMap<Faction> enemyMap = new WeightedIntMap<>();
        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy)
                    || enemy.getShortName().equals("CLAN")) {
                continue;
            }
            int totalCount = borderTracker.getBorderSystems(employer, enemy).size();
            double count = totalCount;
            // Split the border between main controlling faction and any contained factions.
            for (Faction cFaction : factionHints.getContainedFactions(employer, getCurrentDate())) {
                if ((null == cFaction)
                        || !factionHints.isContainedFactionOpponent(enemy, cFaction,
                                employer, getCurrentDate())) {
                    continue;
                }
                if (factionHints.isNeutral(cFaction, enemy, getCurrentDate())
                        || factionHints.isNeutral(enemy, cFaction, getCurrentDate())) {
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
     * @param employerName The shortName of the employer faction
     * @return             A list of faction that share a border
     */
    public List<String> getEnemyList(String employerName) {
        Faction employer = Factions.getInstance().getFaction(employerName);
        if (null == employer) {
            MekHQ.getLogger().warning("Unknown faction key: " + employerName); //$NON-NLS-1$
            return Collections.emptyList();
        }
        return getEnemyList(Factions.getInstance().getFaction(employerName));
    }

    /**
     * Constructs a list of a faction's potential enemies based on common borders.
     * @param employer     The employer faction
     * @return             A list of faction that share a border
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
            if (factionHints.isAlliedWith(employer, enemy, getCurrentDate())
                    && !employer.isClan() && !enemy.isClan()) {
                continue;
            }
            if (factionHints.isNeutral(employer, enemy, getCurrentDate())
                    || factionHints.isNeutral(enemy, employer, getCurrentDate())) {
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
                    if ((null != cf)
                            && factionHints.isContainedFactionOpponent(enemy, cf, employer, getCurrentDate())) {
                        list.add(cf);
                    }
                }
            }
        }
        return list.stream().map(Faction::getShortName).collect(Collectors.toList());
    }

    /**
     * Applies modifiers to the border size (measured by number of planets within a certain proximity
     * to one or more of the attacker's planets) based on diplomatic stance (e.g. war, rivalry, alliance).
     *
     * @param count  The number of planets
     * @param f      The attacking faction
     * @param enemy  The defending faction
     * @param date   The current campaign date
     * @return       An adjusted weight
     */
    protected double adjustBorderWeight(double count, Faction f, Faction enemy, LocalDate date) {
        final LocalDate TUKKAYID = LocalDate.of(3052, Month.JUNE, 20);

        if (factionHints.isNeutral(f, enemy, getCurrentDate())
                || factionHints.isNeutral(enemy, f, getCurrentDate())) {
            return 0;
        }
        if (!f.isClan() && factionHints.isAlliedWith(f, enemy, date)) {
            return 0;
        }
        if (f.isClan() && enemy.isClan() &&
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
        /* This is pretty hacky, but ComStar does not have many targets
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
     * @param attacker  The faction key of the attacker
     * @param defender  The faction key of the defender
     * @return          The planetId of the chosen planet, or null if there are no target candidates
     */
    @Nullable public String getMissionTarget(String attacker, String defender) {
        Faction f1 = Factions.getInstance().getFaction(attacker);
        Faction f2 = Factions.getInstance().getFaction(defender);
        if (null == f1) {
            MekHQ.getLogger().error("Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
        if (null == f2) {
            MekHQ.getLogger().error("Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
        List<PlanetarySystem> planetList = getMissionTargetList(f1, f2);
        if (planetList.size() > 0) {
            return Utilities.getRandomItem(planetList).getId();
        }
        return null;
    }

    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's
     * planets.
     *
     * @param attackerKey   The attacking faction's shortName
     * @param defenderKey   The defending faction's shortName
     * @return              A list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(String attackerKey, String defenderKey) {
        Faction attacker = Factions.getInstance().getFaction(attackerKey);
        Faction defender = Factions.getInstance().getFaction(defenderKey);
        if (null == attacker) {
            MekHQ.getLogger().error("Non-existent faction key: " + attackerKey); //$NON-NLS-1$
        }
        if (null == defender) {
            MekHQ.getLogger().error("Non-existent faction key: " + defenderKey); //$NON-NLS-1$
        }
        if ((null != attacker) && (null != defender)) {
            return getMissionTargetList(attacker, defender);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's
     * planets.
     *
     * @param attacker   The attacking faction
     * @param defender   The defending faction
     * @return              A list of potential mission targets
     */
    public List<PlanetarySystem> getMissionTargetList(Faction attacker, Faction defender) {
        // If the attacker or defender are not in the set of factions that control planets,
        // and they are not rebels or pirates, they will be a faction contained within another
        // (e.g. Nova Cat in the Draconis Combine, or Wolf-in-Exile in Lyran space
        if (!borderTracker.getFactionsInRegion().contains(attacker)
                && !attacker.is(Faction.Tag.PIRATE)) {
            attacker = factionHints.getContainedFactionHost(attacker, getCurrentDate());
        }
        if (!borderTracker.getFactionsInRegion().contains(defender)
                && !defender.is(Faction.Tag.PIRATE)
                && !defender.is(Faction.Tag.REBEL)) {
            defender = factionHints.getContainedFactionHost(defender, getCurrentDate());
        }
        if ((null == attacker) || (null == defender)) {
            return Collections.emptyList();
        }
        // Locate rebels on any of the attacker's planet
        if (defender.is(Faction.Tag.REBEL)) {
            return new ArrayList<>(borderTracker.getBorders(attacker).getSystems());
        }

        Set<PlanetarySystem> planetSet = new HashSet<>(borderTracker.getBorderSystems(attacker, defender));
        // If mission is against generic pirates (those that don't control any systems),
        // add all border systems as possible locations
        if ((attacker.is(Faction.Tag.PIRATE) && !borderTracker.getFactionsInRegion().contains(attacker))
                || (defender.is(Faction.Tag.PIRATE) && !borderTracker.getFactionsInRegion().contains(defender))) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                planetSet.addAll(borderTracker.getBorderSystems(f, attacker));
                planetSet.addAll(borderTracker.getBorderSystems(attacker, f));
            }
        }
        /* No border with defender found among systems controlled by
         * attacker; check for presence of attacker and defender
         * in systems controlled by other factions.
         */
        if (planetSet.isEmpty()) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                for (Faction cf : factionHints.getContainedFactions(f, getCurrentDate())) {
                    if (cf.equals(attacker)
                            && factionHints.isContainedFactionOpponent(f, cf, defender, getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(f, defender));
                    }
                    if (cf.equals(defender)
                            && factionHints.isContainedFactionOpponent(f, cf, attacker, getCurrentDate())) {
                        planetSet.addAll(borderTracker.getBorderSystems(attacker, f));
                    }
                }
            }
        }
        return new ArrayList<>(planetSet);
    }
}
