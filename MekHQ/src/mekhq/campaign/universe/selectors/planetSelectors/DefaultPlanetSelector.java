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

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
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
     * Creates a new DefaultPlanetSelector that uses the central planet to produce the planet.
     */
    public DefaultPlanetSelector(final RandomOriginOptions options) {
        this(options, null);
    }

    /**
     * Creates a new DefaultPlanetSelector that always selects a specific planet.
     *
     * @param planet The {@link Planet} to use.
     */
    public DefaultPlanetSelector(final RandomOriginOptions options, final @Nullable Planet planet) {
        super(options);
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
        return (getPlanet() == null) ? getOptions().determinePlanet(campaign.getLocation().getPlanet()) : getPlanet();
    }

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign, final @Nullable Faction faction) {
        return selectPlanet(campaign);
    }
}
