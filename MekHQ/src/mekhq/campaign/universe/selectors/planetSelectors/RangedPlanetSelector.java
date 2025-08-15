/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.selectors.planetSelectors;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

/**
 * An implementation of {@link AbstractPlanetSelector} which chooses a planet from a range of planets and a
 * {@link Faction}.
 */
public class RangedPlanetSelector extends AbstractPlanetSelector {
    //region Variable Declarations
    /**
     * The current date when the values were cached.
     */
    private LocalDate cachedDate;

    /**
     * The current {@link Planet} when the values were cached.
     */
    private Planet cachedPlanet;

    /**
     * {@link Faction} keyed {@link WeightedDoubleMap} for potential {@link Planet}s to generate
     */
    private Map<Faction, WeightedDoubleMap<Planet>> cachedPlanets;
    //endregion Variable Declarations

    //region Constructors
    public RangedPlanetSelector(final RandomOriginOptions options) {
        super(options);
    }
    //endregion Constructors

    //endregion Getters/Setters
    public @Nullable LocalDate getCachedDate() {
        return cachedDate;
    }

    public void setCachedDate(final @Nullable LocalDate cachedDate) {
        this.cachedDate = cachedDate;
    }

    public @Nullable Planet getCachedPlanet() {
        return cachedPlanet;
    }

    public void setCachedPlanet(final @Nullable Planet cachedPlanet) {
        this.cachedPlanet = cachedPlanet;
    }

    public @Nullable Map<Faction, WeightedDoubleMap<Planet>> getCachedPlanets() {
        return cachedPlanets;
    }

    public void setCachedPlanets(final @Nullable Map<Faction, WeightedDoubleMap<Planet>> cachedPlanets) {
        this.cachedPlanets = cachedPlanets;
    }
    //endregion Getters/Setters

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign) {
        return selectPlanet(campaign, campaign.getFaction());
    }

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign, final Faction faction) {
        final Planet planet = getOptions().determinePlanet(campaign.getCurrentSystem().getPrimaryPlanet());
        if ((getCachedPlanets() == null)
                  || !getCachedPlanets().containsKey(faction)
                  || !planet.equals(getCachedPlanet())
                  || (getCachedDate() == null) || campaign.getLocalDate().isAfter(getCachedDate())) {
            createLookupMap(campaign, faction, planet);
        }

        return getCachedPlanets().get(faction).randomItem();
    }

    /**
     * Clears the cache associated with per-faction planet probabilities.
     */
    @Override
    public void clearCache() {
        super.clearCache();
        setCachedDate(null);
        setCachedPlanet(null);
        setCachedPlanets(null);
    }

    private void createLookupMap(final Campaign campaign, final Faction faction,
          final Planet centralPlanet) {
        final LocalDate now = campaign.getLocalDate();

        final PlanetarySystem currentSystem = centralPlanet.getParentSystem();

        final WeightedDoubleMap<Planet> planets = new WeightedDoubleMap<>();
        final List<PlanetarySystem> systems = Systems.getInstance().getNearbySystems(currentSystem,
              getOptions().getOriginSearchRadius());

        double total = 0.0;
        for (final PlanetarySystem system : systems) {
            final double distance = system.getDistanceTo(currentSystem);
            if (faction.isMercenary() || system.getFactionSet(now).contains(faction)) {
                if (!getOptions().isExtraRandomOrigin()) {
                    final Planet planet = system.getPrimaryPlanet();
                    final Long pop = planet.getPopulation(now);
                    if ((pop != null) && (pop > 0.0)) {
                        total += 100.0 * Math.log10(pop) / (1.0 + distance * getOptions().getOriginDistanceScale());
                        planets.add(total, planet);
                    }
                } else {
                    for (final Planet planet : system.getPlanets()) {
                        final Long pop = planet.getPopulation(now);
                        if ((pop != null) && (pop > 0.0)) {
                            total += 100.0 * Math.log10(pop) / (1.0 + distance * getOptions().getOriginDistanceScale());
                            planets.add(total, planet);
                        }
                    }
                }
            }
        }

        if (planets.isEmpty()) {
            planets.add(1.0, currentSystem.getPrimaryPlanet());
        }

        setCachedDate(now);
        setCachedPlanet(centralPlanet);
        if (getCachedPlanets() == null) {
            setCachedPlanets(new HashMap<>());
        }
        getCachedPlanets().put(faction, planets);
    }
}
