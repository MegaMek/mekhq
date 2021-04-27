/*
 * Copyright (C) 2019 - The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Faction.Tag;

/**
 * An implementation of {@link AbstractFactionSelector} which chooses
 * a faction from a defined range of planets.
 */
public class RangedFactionSelector extends AbstractFactionSelector {
    /**
     * The range around {@link Campaign#getCurrentSystem()} to search
     * for factions.
     */
    private final int range;

    /**
     * The current date of the {@link Campaign} when the values were
     * cached.
     */
    private LocalDate cachedDate;

    /**
     * The current {@link PlanetarySystem} of the {@link Campaign} when
     * the values were cached.
     */
    private PlanetarySystem cachedSystem;

    /**
     * This map stores weights for a faction. Each weight should
     * be cumulative. That is, if two factions have equal weights,
     * you could express this with weights of 1.0 and 2.0 or 5.0 and 10.0.
     * This way, when selecting a faction at random using the weights
     * you can create a random double between 0.0 and the largest value
     * in the map, then select the key equal to or greater than the value.
     */
    private TreeMap<Double, Faction> cachedFactions;

    /**
     * A scale to apply to planetary distances.
     */
    private double distanceScale = 0.6;

    /**
     * Creates a new {@code RangedFactionSelector} with the given range.
     * @param range The range around the current location ({@link Campaign#getCurrentSystem()})
     *              from which to select factions.
     */
    public RangedFactionSelector(int range) {
        this.range = range;
    }


    /**
     * Gets a scale to apply to planetary distances.
     * @return The scaling factor to apply to planetary distances.
     */
    public double getDistanceScale() {
        return distanceScale;
    }

    /**
     * Sets the scale to apply to planetary distances.
     * @param distanceScale A scaling factor--ideally between 0.1 and 2.0--
     *                      to apply to planetary distances during weighting.
     *                      Values above 1.0 prefer the current location,
     *                      while values closer to 0.1 spread out the faction
     *                      selection.
     */
    public void setDistanceScale(double distanceScale) {
        this.distanceScale = distanceScale;
        clearCache();
    }

    @Override
    public Faction selectFaction(Campaign campaign) {
        if ((cachedFactions == null)
                || !cachedSystem.equals(campaign.getCurrentSystem())
                || campaign.getLocalDate().isAfter(cachedDate)) {
            createLookupMap(campaign);
        }

        double random = ThreadLocalRandom.current().nextDouble(cachedFactions.lastKey());
        return cachedFactions.ceilingEntry(random).getValue();
    }

    /**
     * Clears the cache associated with faction probabilities.
     */
    @Override
    public void clearCache() {
        super.clearCache();
        cachedDate = null;
        cachedFactions = null;
    }

    /**
     * Creates the cached {@link Faction} lookup map.
     */
    private void createLookupMap(Campaign campaign) {
        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        LocalDate now = campaign.getLocalDate();
        boolean isClan = campaign.getFaction().isClan();

        Map<Faction, Double> weights = new HashMap<>();
        Systems.getInstance().visitNearbySystems(currentSystem, range, planetarySystem -> {
            Planet planet = planetarySystem.getPrimaryPlanet();
            Long pop = planet.getPopulation(now);
            if ((pop == null) || (pop <= 0)) {
                return;
            }

            double distance = planetarySystem.getDistanceTo(currentSystem);

            // Weight the faction by the planet's population divided by its
            // distance from our current system. The scaling factor is used
            // to affect the 'spread'.
            double delta = Math.log10(pop) / (1 + distance * distanceScale);
            for (Faction faction : planetarySystem.getFactionSet(now)) {
                if (faction.is(Tag.ABANDONED) || faction.is(Tag.HIDDEN) || faction.is(Tag.SPECIAL)
                        || faction.is(Tag.MERC)) {
                    continue;
                }

                if (faction.is(Tag.INACTIVE) && !faction.isComStar()) {
                    // Skip INACTIVE factions [excepting ComStar]
                    continue;
                }

                if (faction.isClan() && !(isClan || isAllowClan())) {
                    continue;
                }

                // each faction on planet is given even weighting here, and then faction tag
                // weights are applied before calculating the probabilities
                weights.compute(faction, (f, total) -> total != null ? total + delta : delta);
            }
        });

        Faction mercenaries = Factions.getInstance().getFaction("MERC");
        TreeMap<Double, Faction> factions = new TreeMap<>();
        if (weights.isEmpty()) {
            // If we have no valid factions use the campaign's faction ...
            if (!isClan) {
                // ... and if we're not a clan faction, we can have mercs too.
                factions.put(1.0, mercenaries);
            }
            factions.put(2.0, campaign.getFaction());

            cachedDate = now;
            cachedSystem = currentSystem;
            cachedFactions = factions;
            return;
        }

        Set<Faction> enemies = getEnemies(campaign);

        //
        // Convert the tallied per-faction weights into a TreeMap
        // to perform roulette randomization
        //

        double total = 0.0;
        for (Map.Entry<Faction, Double> entry : weights.entrySet()) {
            Faction faction = entry.getKey();

            // Only take factions which are not actively fighting us
            if (!enemies.contains(faction)) {
                double factionWeight = getFactionWeight(faction);
                total += entry.getValue() * factionWeight;
                factions.put(total, faction);
            }
        }

        if (factions.isEmpty()) {
            // If we have no valid factions use the campaign's faction ...
            if (!isClan) {
                // ... and if we're not a clan faction, we can have mercs too.
                factions.put(1.0, mercenaries);
            }
            factions.put(2.0, campaign.getFaction());
        } else {
            if (!isClan) {
                // There is a good chance they're a merc if we're not a clan faction!
                // The 1.0 prevents no weight calculations
                factions.put((total == 0.0) ? 1.0 : total + total * getFactionWeight(mercenaries), mercenaries);
            } else {
                // There is a lopsided chance they're from the campaign faction if it is a clan faction.
                factions.put((total == 0.0) ? 1.0 : total + (15 * total), campaign.getFaction());
            }
        }

        cachedDate = now;
        cachedSystem = currentSystem;
        cachedFactions = factions;
    }

    /**
     * Gets the set of enemies for the {@link Campaign}.
     * @param campaign The current campaign.
     * @return A set of current enemies for the {@link Campaign}.
     */
    private Set<Faction> getEnemies(Campaign campaign) {
        Set<Faction> enemies = new HashSet<>();
        for (Contract contract : campaign.getActiveContracts()) {
            if (contract instanceof AtBContract) {
                enemies.add(Factions.getInstance().getFaction(((AtBContract)contract).getEnemyCode()));
            }
        }
        return enemies;
    }

    /**
     * Gets a weight to apply to a {@link Faction}. This is based on the
     * tags assigned to the faction.
     *
     * @param faction The {@link Faction} to get a weight when calculating
     *                probabilities.
     * @return A weight to apply to the given {@link Faction}.
     */
    private double getFactionWeight(Faction faction) {
        if (faction.isComStar() || faction.getShortName().equals("WOB")) {
            return 0.05;
        } else if (faction.is(Tag.MERC) || faction.is(Tag.SUPER) || faction.is(Tag.MAJOR)) {
            return 1.0;
        } else if (faction.is(Tag.MINOR)) {
            return 0.5;
        } else if (faction.is(Tag.SMALL)) {
            return 0.2;
        } else if (faction.is(Tag.REBEL) || faction.is(Tag.CHAOS)
                    || faction.is(Tag.TRADER) || faction.is(Tag.PIRATE)) {
            return 0.05;
        } else if (faction.is(Tag.CLAN)) {
            return 0.01;
        } else {
            // Don't include any of the other tags like hidden or inactive
            return 0.0;
        }
    }
}
