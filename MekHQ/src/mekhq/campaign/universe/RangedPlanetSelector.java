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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction.Tag;

/**
 * An implementation of {@link AbstractPlanetSelector} which chooses
 * a planet from a range of planets and a {@link Faction}.
 */
public class RangedPlanetSelector extends AbstractPlanetSelector {
    /**
     * The range around {@link Campaign#getCurrentSystem()} to search
     * for planets.
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
     * This map stores cached weights for a planet keyed by faction.
     * Each weight should be cumulative. That is, if two planets have
     * equal weights, you could express this with weights of 1.0 and 2.0
     * or 5.0 and 10.0. This way, when selecting a planet at random using
     * the weights you can create a random double between 0.0 and the largest
     * value in the map, then select the key equal to or greater than the value.
     */
    private Map<Faction, TreeMap<Double, Planet>> cachedPlanets;

    /**
     * A scale to apply to planetary distances.
     */
    private double distanceScale = 0.6;

    /**
     * A value indicating if extra randomness should be
     * used when selecting planets. Currently, this is implemented
     * by including planets within systems, rather than just
     * the primary planet for a system.
     */
    private boolean isExtraRandom;

    /**
     * Creates a new {@code RangedPlanetSelector} with a given range.
     * @param range The range to use when selecting planets.
     */
    public RangedPlanetSelector(int range) {
        this(range, /*isExtraRandom*/false);
    }

    /**
     * Creates a new {@code RandomPlanetSelector} with a given range
     * and a value indicating if {@link Planet} selection should be extra
     * random.
     * @param range The range to use when selecting planets.
     * @param isExtraRandom A value indicating whether or not to use
     *                      planets within a system, effectively
     *                      producing a more random origin planet.
     */
    public RangedPlanetSelector(int range, boolean isExtraRandom) {
        this.range = range;
        this.isExtraRandom = isExtraRandom;
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

    /**
     * Gets a value indicating if extra randomness should be
     * used when selecting planets. Currently, this is implemented
     * by including planets within systems, rather than just
     * the primary planet for a system.
     * @return A value indicating if extra randomness should be
     *         used during planet selection.
     */
    public boolean isExtraRandom() {
        return isExtraRandom;
    }

    /**
     * Sets a value indicating if extra randomness should be
     * used when selecting planets. Currently, this is implemented
     * by including planets within systems, rather than just
     * the primary planet for a system.
     * @param b A value indicating if extra randomness should be
     *          used during planet selection.
     */
    public void setExtraRandom(boolean b) {
        isExtraRandom = b;
        clearCache();
    }

    @Override
    public Planet selectPlanet(Campaign campaign) {
        return selectPlanet(campaign, campaign.getFaction());
    }

    @Override
    public Planet selectPlanet(Campaign campaign, Faction faction) {
        if ((cachedPlanets == null)
                || !cachedPlanets.containsKey(faction)
                || !cachedSystem.equals(campaign.getCurrentSystem())
                || campaign.getLocalDate().isAfter(cachedDate)) {
            createLookupMap(campaign, faction);
        }

        TreeMap<Double, Planet> planets = cachedPlanets.get(faction);
        double random = ThreadLocalRandom.current().nextDouble(planets.lastKey());
        return planets.ceilingEntry(random).getValue();
    }

    /**
     * Clears the cache associated with per-faction planet probabilities.
     */
    @Override
    public void clearCache() {
        super.clearCache();
        cachedDate = null;
        cachedSystem = null;
        cachedPlanets = null;
    }

    private void createLookupMap(Campaign campaign, Faction faction) {
        LocalDate now = campaign.getLocalDate();

        PlanetarySystem currentSystem = campaign.getCurrentSystem();

        TreeMap<Double, Planet> planets = new TreeMap<>();
        List<PlanetarySystem> systems = Systems.getInstance().getNearbySystems(campaign.getCurrentSystem(), range);

        double total = 0.0;
        for (PlanetarySystem system : systems) {
            double distance = system.getDistanceTo(currentSystem);
            if (faction.is(Tag.MERC) || system.getFactionSet(now).contains(faction)) {
                if (!isExtraRandom) {
                    Planet planet = system.getPrimaryPlanet();
                    Long pop = planet.getPopulation(now);
                    if (pop != null && pop > 0) {
                        total += 100.0 * Math.log10(pop) / (1 + distance * distanceScale);
                        planets.put(total, planet);
                    }
                } else {
                    for (Planet planet : system.getPlanets()) {
                        Long pop = planet.getPopulation(now);
                        if (pop != null && pop > 0) {
                            total += 100.0 * Math.log10(pop) / (1 + distance * distanceScale);
                            planets.put(total, planet);
                        }
                    }
                }
            }
        }

        if (planets.isEmpty()) {
            planets.put(1.0, currentSystem.getPrimaryPlanet());
        }

        cachedDate = now;
        cachedSystem = currentSystem;
        if (cachedPlanets == null) {
            cachedPlanets = new HashMap<>();
        }

        cachedPlanets.put(faction, planets);
    }
}
