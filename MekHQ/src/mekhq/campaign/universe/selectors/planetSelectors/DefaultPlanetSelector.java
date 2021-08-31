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
package mekhq.campaign.universe.selectors.planetSelectors;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * Selects a {@link Planet} using either the specified planet or the current campaign's planet
 */
public class DefaultPlanetSelector extends AbstractPlanetSelector {
    //region Variable Declarations
    private Planet planet;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a new DefaultPlanetSelector that uses {@link CurrentLocation#getPlanet()} to produce
     * the planet.
     */
    public DefaultPlanetSelector() {
        this(null);
    }

    /**
     * Creates a new DefaultPlanetSelector that always selects a specific planet.
     * @param planet The {@link Planet} to use.
     */
    public DefaultPlanetSelector(final @Nullable Planet planet) {
        setPlanet(planet);
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable Planet getPlanet() {
        return planet;
    }

    public void setPlanet(final @Nullable Planet planet) {
        this.planet = planet;
    }
    //endregion Getters/Setters

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign) {
        return (getPlanet() == null) ? campaign.getLocation().getPlanet() : getPlanet();
    }

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign, final @Nullable Faction faction) {
        return selectPlanet(campaign);
    }
}
