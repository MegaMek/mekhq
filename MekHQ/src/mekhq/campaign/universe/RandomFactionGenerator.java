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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OptionsChangedEvent;

/**
 * @author Neoancient
 *
 * Uses Factions and Planets to weighted lists of potential employers
 * and enemies for contract generation. Also finds a suitable planet
 * for the action.
 *
 */

/* TODO: Account for the de facto alliance of the invading Clans and the
 * Fortress Republic in a way that doesn't involve hard-coding them here.
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

    private static final Date FORTRESS_REPUBLIC = new Date (new GregorianCalendar(3135,10,1).getTimeInMillis());

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

    public void startup(Campaign c) {
        borderTracker.setDate(Utilities.getDateTimeDay(c.getCalendar()));
        final Planet location = c.getLocation().getCurrentPlanet();
        borderTracker.setRegionCenter(location.getX(), location.getY());
        borderTracker.setRegionRadius(c.getCampaignOptions().getSearchRadius());
        MekHQ.registerHandler(borderTracker);
        MekHQ.registerHandler(this);
        for (Faction f : Faction.getFactions()) {
            if (factionHints.isDeepPeriphery(f)) {
                borderTracker.setBorderSize(f, BORDER_RANGE_DEEP_PERIPHERY);
            }
        }
    }

    public void setDate(GregorianCalendar calendar) {
        borderTracker.setDate(Utilities.getDateTimeDay(calendar));
    }

    public void setDate(DateTime when) {
        borderTracker.setDate(when);
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

    private Date currentDate() {
        return borderTracker.getLastUpdated().toDate();
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
            if (f.getShortName().equals("ROS") && currentDate().after(FORTRESS_REPUBLIC)) {
                continue;
            }

            retVal.add(f.getShortName());
            /* Add factions which do not control any planets to the employer list */
            factionHints.getContainedFactions(f, currentDate())
            .forEach(cf -> retVal.add(cf.getShortName()));
        }
        //Add rebels and pirates
        retVal.add("REB");
        retVal.add("PIR");
        return retVal;
    }

    /**
     * Builds map of potential employers weighted by number of systems controled within the search area
     *
     * @return Map used to select employer
     */
    protected WeightedMap<Faction> buildEmployerMap() {
        WeightedMap<Faction> retVal = new WeightedMap<>();
        for (Faction f : borderTracker.getFactionsInRegion()) {

            if (f.isClan() || FactionHints.isEmptyFaction(f)) {
                continue;
            }
            if (f.getShortName().equals("ROS") && currentDate().after(FORTRESS_REPUBLIC)) {
                continue;
            }

            int weight = borderTracker.getBorders(f).getPlanets().size();
            retVal.add(weight, f);

            /* Add factions which do not control any planets to the employer list */
            for (Faction cfaction : factionHints.getContainedFactions(f, currentDate())) {
                if (null != cfaction) {
                    if (!cfaction.isClan()) {
                        weight = (int) Math.floor((borderTracker.getBorders(f).getPlanets().size()
                                * factionHints.getAltLocationFraction(f, cfaction, currentDate())) + 0.5);
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
        WeightedMap<Faction> employers = buildEmployerMap();
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
        final String METHOD_NAME = "getEnemy(String,boolean)"; //$NON-NLS-1$

        Faction employerFaction = Faction.getFaction(employer);
        if (null == employerFaction) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME,
                    "Could not find enemy for " + employer); //$NON-NLS-1$
            return "PIR";
        } else {
            return getEnemy(employerFaction, useRebels);
        }
    }

    /**
     * Selects an enemy faction for the given employer, weighted by length of shared border and
     * diplomatic relations. Factions at war or designated as rivals are twice as likely (cumulative)
     * to be chosen as opponents. Allied factions are ignored except for Clans, which halves
     * the weight for that option.
     *
     * @param employer  The faction offering the contract
     * @param useRebels Whether to include rebels as a possible opponent
     * @return          The faction to use as the opfor.
     */
    public String getEnemy(Faction employer, boolean useRebels) {
        final String METHOD_NAME = "getEnemy(Faction,boolean)"; //$NON-NLS-1$

        /* Rebels occur on a 1-4 (d20) on nearly every enemy chart */
        if (useRebels && (Compute.randomInt(5) == 0)) {
            return "REB";
        }

        Faction enemy = null;
        if (!borderTracker.getFactionsInRegion().contains(employer)) {
            employer = factionHints.getContainedFactionHost(employer, currentDate());
        }
        if (null != employer) {
            WeightedMap<Faction> enemyMap = buildEnemyMap(employer);
            enemy = enemyMap.randomItem();
        }
        if (null != enemy) {
            return enemy.getShortName();
        }
        // Fallback; there are always pirates.
        MekHQ.getLogger().error(getClass(), METHOD_NAME,
                "Could not find enemy for " + employer.getShortName()); //$NON-NLS-1$
        return "PIR";
    }

    /**
     * Builds a map of potential enemies keyed to cumulative weight
     *
     * @param employer
     * @return
     */
    protected WeightedMap<Faction> buildEnemyMap(Faction employer) {
        WeightedMap<Faction> enemyMap = new WeightedMap<>();
        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy)
                    || enemy.getShortName().equals("CLAN")) {
                continue;
            }
            int totalCount = borderTracker.getBorderPlanets(employer, enemy).size();
            double count = totalCount;
            // Split the border between main controlling faction and any contained factions.
            for (Faction cFaction : factionHints.getContainedFactions(employer, currentDate())) {
                if ((null == cFaction)
                        || !factionHints.isContainedFactionOpponent(enemy, cFaction,
                                employer, currentDate())) {
                    continue;
                }
                if (factionHints.isNeutral(cFaction, enemy, currentDate())
                        || factionHints.isNeutral(enemy, cFaction, currentDate())) {
                    continue;
                }
                double cfCount = totalCount;
                if (factionHints.getAltLocationFraction(employer, cFaction, currentDate()) > 0.0) {
                    cfCount = totalCount * factionHints.getAltLocationFraction(employer, cFaction, currentDate());
                    count -= cfCount;
                }
                cfCount = adjustBorderWeight(cfCount, employer, enemy, currentDate());
                enemyMap.add((int) Math.floor(cfCount + 0.5), cFaction);
            }
            count = adjustBorderWeight(count, employer, enemy, currentDate());
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
            if (f.getShortName().equals("ROS") && currentDate().after(FORTRESS_REPUBLIC)) {
                continue;
            }
            /* Add factions which do not control any planets to the employer list */
            for (Faction cfaction : factionHints.getContainedFactions(f, currentDate())) {
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
        Faction employer = Faction.getFaction(employerName);
        if (null == employer) {
            MekHQ.getLogger().warning(getClass(), "getEnemyList(String)", //$NON-NLS-1$
                    "Unknown faction key: " + employerName); //$NON-NLS-1$
            return Collections.emptyList();
        }
        return getEnemyList(Faction.getFaction(employerName));
    }

    /**
     * Constructs a list of a faction's potential enemies based on common borders.
     * @param employerName The employer faction
     * @return             A list of faction that share a border
     */
    public List<String> getEnemyList(Faction employer) {
        Set<Faction> list = new HashSet<>();
        Faction outer = factionHints.getContainedFactionHost(employer, currentDate());
        for (Faction enemy : borderTracker.getFactionsInRegion()) {
            if (FactionHints.isEmptyFaction(enemy)) {
                continue;
            }
            if (enemy.equals(employer) && !factionHints.isAtWarWith(enemy,
                    enemy, currentDate())) {
                continue;
            }
            if (factionHints.isAlliedWith(employer, enemy, currentDate())
                    && !employer.isClan() && !enemy.isClan()) {
                continue;
            }
            if (factionHints.isNeutral(employer, enemy, currentDate())
                    || factionHints.isNeutral(enemy, employer, currentDate())) {
                continue;
            }
            Faction useBorder = employer;
            if (null != outer) {
                if (!factionHints.isContainedFactionOpponent(outer, employer, enemy, currentDate())) {
                    continue;
                }
                useBorder = outer;
            }

            if (!borderTracker.getBorderPlanets(useBorder, enemy).isEmpty()) {
                list.add(enemy);
                for (Faction cf : factionHints.getContainedFactions(enemy, currentDate())) {
                    if ((null != cf)
                            && factionHints.isContainedFactionOpponent(enemy, cf, employer, currentDate())) {
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
    protected double adjustBorderWeight(double count, Faction f,
            Faction enemy, Date date) {
        final Date TUKKAYID = new Date (new GregorianCalendar(3052,5,20).getTimeInMillis());

        if (factionHints.isNeutral(f, enemy, currentDate())
                || factionHints.isNeutral(enemy, f, currentDate())) {
            return 0;
        }
        if (!f.isClan() && factionHints.isAlliedWith(f, enemy, date)) {
            return 0;
        }
        if (f.isClan() && enemy.isClan() &&
                (factionHints.isAlliedWith(f, enemy, date) ||
                        (date.before(TUKKAYID) && (borderTracker.getCenterY() < 600)))) {
            /* Treat invading Clans as allies in the Inner Sphere */
            count /= 4.0;
        }
        if (factionHints.isAtWarWith(f, enemy, date) && (f != enemy)) {
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
     * @param attacker
     * @param defender
     * @return          The planetId of the chosen planet, or null if there are no target candidates
     */
    @Nullable public String getMissionTarget(String attacker, String defender) {
        final String METHOD_NAME = "getMissionTarget(String, String)"; // $NON-NLS-1$
        Faction f1 = Faction.getFaction(attacker);
        Faction f2 = Faction.getFaction(defender);
        if (null == f1) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME,
                    "Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
        if (null == f2) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME,
                    "Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
        List<Planet> planetList = getMissionTargetList(f1, f2);
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
    public List<Planet> getMissionTargetList(String attackerKey, String defenderKey) {
        final String METHOD_NAME = "getMissionTargetList(String, String)"; //$NON-NLS-1$
        Faction attacker = Faction.getFaction(attackerKey);
        Faction defender = Faction.getFaction(defenderKey);
        if (null == attacker) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME,
                    "Non-existent faction key: " + attackerKey); //$NON-NLS-1$
        }
        if (null == defender) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME,
                    "Non-existent faction key: " + defenderKey); //$NON-NLS-1$
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
     * @param attackerKey   The attacking faction
     * @param defenderKey   The defending faction
     * @return              A list of potential mission targets
     */
    public List<Planet> getMissionTargetList(Faction attacker, Faction defender) {
        if (!borderTracker.getFactionsInRegion().contains(attacker)) {
            attacker = factionHints.getContainedFactionHost(attacker, currentDate());
        }
        if (!borderTracker.getFactionsInRegion().contains(defender)) {
            defender = factionHints.getContainedFactionHost(defender, currentDate());
        }
        if ((null == attacker) || (null == defender)) {
            return Collections.emptyList();
        }
        // Locate rebels on any of the attacker's planet
        if (defender.getShortName().equals("REB")) {
            return new ArrayList<>(borderTracker.getBorders(attacker).getPlanets());
        }

        Set<Planet> planetSet = new HashSet<>(borderTracker.getBorderPlanets(attacker, defender));
        // Locate missions by or against pirates on border worlds
        if (attacker.getShortName().equals("PIR") || defender.getShortName().equals("PIR")) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                planetSet.addAll(borderTracker.getBorderPlanets(f, attacker));
                planetSet.addAll(borderTracker.getBorderPlanets(attacker, f));
            }
        }
        /* No border with defender found among systems controlled by
         * attacker; check for presence of attacker and defender
         * in systems controlled by other factions.
         */
        if (planetSet.isEmpty()) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                for (Faction cf : factionHints.getContainedFactions(f, currentDate())) {
                    if (cf.equals(attacker)
                            && factionHints.isContainedFactionOpponent(f,
                                    cf, defender, currentDate())) {
                        planetSet.addAll(borderTracker.getBorderPlanets(f, defender));
                    }
                    if (cf.equals(defender)
                            && factionHints.isContainedFactionOpponent(f,
                                    cf, attacker, currentDate())) {
                        planetSet.addAll(borderTracker.getBorderPlanets(attacker, f));
                    }
                }
            }
        }
        return new ArrayList<>(planetSet);
    }


    /**
     * Constructs a table of values each with a weight that makes them more or less likely to be
     * selected at random
     *
     * @param <T> The values in the table
     */
    static class WeightedMap<T> extends TreeMap<Integer, T> {

        private static final long serialVersionUID = -568712793616821291L;

        void add(int weight, T item) {
            if (weight > 0) {
                if (!isEmpty()) {
                    put(lastKey() + weight, item);
                } else {
                    put(weight, item);
                }
            }
        }

        T randomItem() {
            if (isEmpty()) {
                return null;
            }
            int random = Compute.randomInt(lastKey()) + 1;
            return ceilingEntry(random).getValue();
        }
    }

}
