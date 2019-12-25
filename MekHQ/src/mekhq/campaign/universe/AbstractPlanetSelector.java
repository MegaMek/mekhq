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
 * Represents a class which selects a {@link Planet}
 * from a {@link Campaign} and optionally a {@link Faction}.
 */
public abstract class AbstractPlanetSelector {

    /**
     * Select a {@Planet} for a {@link Campaign}.
     * @param campaign The {@link Campaign} to use when selecting a planet.
     * @return A {@link Planet} or {@code null}.
     */
    @Nullable
    public abstract Planet selectPlanet(Campaign campaign);

    /**
     * Select a {@Planet} for a {@link Campaign} and optional {@link} Faction.
     * @param campaign The {@link Campaign} to use when selecting a planet.
     * @param faction An optional {@link Faction} to use when selecting a planet.
     * @return A {@link Planet} or {@code null}.
     */
    @Nullable
    public abstract Planet selectPlanet(Campaign campaign, @Nullable Faction faction);

    /**
     * Clears any cache associated with planet selection.
     */
    public void clearCache() {
    }
}
