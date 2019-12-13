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

import mekhq.campaign.Campaign;

/**
 * Represents a class which selects {@link Faction} objects.
 */
public abstract class AbstractFactionSelector {
    /**
     * Selects a {@link Faction} for a {@link Campaign}.
     * @param campaign The {@link Campaign} within which this {@link Faction}
     *                 exists.
     * @return A {@link Faction} selected for {@code campaign}.
     */
    public abstract Faction selectFaction(Campaign campaign);
}
