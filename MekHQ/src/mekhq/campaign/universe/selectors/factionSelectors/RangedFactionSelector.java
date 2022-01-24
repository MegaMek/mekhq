/*
 * Copyright (C) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.selectors.factionSelectors;

import megamek.common.annotations.Nullable;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.Faction.Tag;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An implementation of {@link AbstractFactionSelector} which chooses
 * a faction from a defined range of planets.
 */
public class RangedFactionSelector extends AbstractFactionSelector {
    //region Variable Declarations
    /**
     * The current date of the {@link Campaign} when the values were cached.
     */
    private LocalDate cachedDate;

    /**
     * The {@link Planet} when the values were cached.
     */
    private Planet cachedPlanet;

    /**
     * This map stores weighted factions
     */
    private WeightedDoubleMap<Faction> cachedFactions;
    //endregion Variable Declarations

    //region Constructors
    public RangedFactionSelector(final RandomOriginOptions options) {
        super(options);
        setCachedDate(null);
        setCachedPlanet(null);
        setCachedFactions(null);
    }
    //endregion Constructors

    //region Getters/Setters
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

    public @Nullable WeightedDoubleMap<Faction> getCachedFactions() {
        return cachedFactions;
    }

    public void setCachedFactions(final @Nullable WeightedDoubleMap<Faction> cachedFactions) {
        this.cachedFactions = cachedFactions;
    }
    //endregion Getters/Setters

    @Override
    public Faction selectFaction(final Campaign campaign) {
        final Planet planet = getOptions().determinePlanet(campaign.getCurrentSystem().getPrimaryPlanet());
        if ((getCachedFactions() == null)
                || !planet.equals(getCachedPlanet())
                || (getCachedDate() == null) || campaign.getLocalDate().isAfter(getCachedDate())) {
            createLookupMap(campaign, planet);
        }

        return getCachedFactions().randomItem();
    }

    /**
     * Clears the cache associated with faction probabilities.
     */
    @Override
    public void clearCache() {
        super.clearCache();
        setCachedDate(null);
        setCachedPlanet(null);
        setCachedFactions(null);
    }

    /**
     * Creates the cached {@link Faction} lookup map.
     */
    private void createLookupMap(final Campaign campaign, final Planet centralPlanet) {
        final PlanetarySystem currentSystem = centralPlanet.getParentSystem();

        final LocalDate now = campaign.getLocalDate();
        final boolean isClan = campaign.getFaction().isClan();

        final Map<Faction, Double> weights = new HashMap<>();
        Systems.getInstance().visitNearbySystems(currentSystem, getOptions().getOriginSearchRadius(),
                planetarySystem -> {
            Planet planet = planetarySystem.getPrimaryPlanet();
            Long pop = planet.getPopulation(now);
            if ((pop == null) || (pop <= 0)) {
                return;
            }

            final double distance = planetarySystem.getDistanceTo(currentSystem);

            // Weight the faction by the planet's population divided by its
            // distance from our current system. The scaling factor is used
            // to affect the 'spread'.
            final double delta = Math.log10(pop) / (1.0 + distance * getOptions().getOriginDistanceScale());
            for (Faction faction : planetarySystem.getFactionSet(now)) {
                if (faction.is(Tag.ABANDONED) || faction.is(Tag.HIDDEN) || faction.is(Tag.SPECIAL)
                        || faction.isMercenary()) {
                    continue;
                }

                if (faction.is(Tag.INACTIVE) && !faction.isComStar()) {
                    // Skip INACTIVE factions [excepting ComStar]
                    continue;
                }

                if (faction.isClan() && !(isClan || getOptions().isAllowClanOrigins())) {
                    continue;
                }

                // each faction on planet is given even weighting here, and then faction tag
                // weights are applied before calculating the probabilities
                weights.compute(faction, (f, total) -> total != null ? total + delta : delta);
            }
        });

        final Faction mercenaries = Factions.getInstance().getFaction("MERC");
        final WeightedDoubleMap<Faction> factions = new WeightedDoubleMap<>();
        if (weights.isEmpty()) {
            // If we have no valid factions use the campaign's faction ...
            if (!isClan) {
                // ... and if we're not a clan faction, we can have mercs too.
                factions.add(1.0, mercenaries);
            }
            factions.add(2.0, campaign.getFaction());

            setCachedDate(now);
            setCachedPlanet(centralPlanet);
            setCachedFactions(factions);
            return;
        }

        final Set<Faction> enemies = getEnemies(campaign);

        //
        // Convert the tallied per-faction weights into a TreeMap
        // to perform roulette randomization
        //

        double total = 0.0;
        for (final Map.Entry<Faction, Double> entry : weights.entrySet()) {
            // Only take factions which are not actively fighting us
            if (!enemies.contains(entry.getKey())) {
                total += entry.getValue() * getFactionWeight(entry.getKey());
                factions.add(total, entry.getKey());
            }
        }

        if (factions.isEmpty()) {
            // If we have no valid factions use the campaign's faction ...
            if (!isClan) {
                // ... and if we're not a clan faction, we can have mercs too.
                factions.add(1.0, mercenaries);
            }
            factions.add(2.0, campaign.getFaction());
        } else {
            if (!isClan) {
                // There is a good chance they're a merc if we're not a clan faction!
                // The 1.0 prevents no weight calculations
                factions.add((total == 0.0) ? 1.0 : total + total * getFactionWeight(mercenaries), mercenaries);
            } else {
                // There is a lopsided chance they're from the campaign faction if it is a clan faction.
                factions.add((total == 0.0) ? 1.0 : total + (15 * total), campaign.getFaction());
            }
        }

        setCachedDate(now);
        setCachedPlanet(centralPlanet);
        setCachedFactions(factions);
    }

    /**
     * Gets the set of enemies for the {@link Campaign}.
     * @param campaign The current campaign.
     * @return A set of current enemies for the {@link Campaign}.
     */
    private Set<Faction> getEnemies(final Campaign campaign) {
        return campaign.getActiveAtBContracts().stream().map(AtBContract::getEnemy).collect(Collectors.toSet());
    }

    /**
     * Gets a weight to apply to a {@link Faction}. This is based on the tags assigned to the
     * faction.
     *
     * @param faction The {@link Faction} to get a weight when calculating probabilities.
     * @return A weight to apply to the given {@link Faction}.
     */
    private double getFactionWeight(final Faction faction) {
        if (faction.isComStarOrWoB()) {
            return 0.05;
        } else if (faction.isMercenary() || faction.isMajorOrSuperPower()) {
            return 1.0;
        } else if (faction.isMinorPower()) {
            return 0.5;
        } else if (faction.isSmall()) {
            return 0.2;
        } else if (faction.isRebelOrPirate() || faction.is(Tag.CHAOS) || faction.is(Tag.TRADER)) {
            return 0.05;
        } else if (faction.isClan()) {
            return 0.01;
        } else {
            // Don't include any of the other tags like hidden or inactive
            return 0.0;
        }
    }
}
