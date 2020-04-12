/*
 * Copyright (C) 2019 MegaMek team
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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;

/**
 * Selects planets using the default MekHQ logic.
 */
public class DefaultPlanetSelector extends AbstractPlanetSelector {

    private Planet selectedPlanet;

    /**
     * Creates a new DefaultPlanetSelector that uses
     * {@link Campaign#getCurrentSystem()} to produce
     * the planet.
     */
    public DefaultPlanetSelector() {
    }

    /**
     * Creates a new DefaultPlanetSelector that always
     * selects a specific planet.
     * @param planet The {@link Planet} to use.
     */
    public DefaultPlanetSelector(Planet planet) {
        selectedPlanet = planet;
    }

    @Override
    @Nullable
    public Planet selectPlanet(Campaign campaign) {
        if (selectedPlanet != null) {
            return selectedPlanet;
        }

        return campaign.getCurrentSystem().getPrimaryPlanet();
    }

    @Override
    @Nullable
    public Planet selectPlanet(Campaign campaign, @Nullable Faction faction) {
        return selectPlanet(campaign);
    }
}
