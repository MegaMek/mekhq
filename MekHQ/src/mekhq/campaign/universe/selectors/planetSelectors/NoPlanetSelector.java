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
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * Does not select a {@link Planet}.
 */
public class NoPlanetSelector extends AbstractPlanetSelector {
    // region Constructors
    public NoPlanetSelector(final RandomOriginOptions options) {
        super(options);
    }
    // endregion Constructors

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign) {
        return null;
    }

    @Override
    public @Nullable Planet selectPlanet(final Campaign campaign, final @Nullable Faction faction) {
        return null;
    }
}
