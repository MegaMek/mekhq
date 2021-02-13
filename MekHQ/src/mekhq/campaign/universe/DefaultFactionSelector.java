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
 * Selects a {@link Faction} object.
 */
public class DefaultFactionSelector extends AbstractFactionSelector {
    private String factionCode;

    /**
     * Creates a new DefaultFactionSelector class which uses
     * {@link Campaign#getFaction()} to select the faction.
     */
    public DefaultFactionSelector() {
    }

    /**
     * Creates a new DefaultFactionSelector using the specified
     * faction.
     * @param factionCode The short name of the {@link Faction}.
     */
    public DefaultFactionSelector(String factionCode) {
        this.factionCode = factionCode;
    }

    @Override
    public Faction selectFaction(Campaign campaign) {
        if (factionCode != null) {
            return Factions.getInstance().getFaction(factionCode);
        } else {
            return campaign.getFaction();
        }
    }
}
