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
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * Selects a {@link Faction} object.
 */
public class DefaultFactionSelector extends AbstractFactionSelector {
    //region Variable Declarations
    private Faction faction;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a new DefaultFactionSelector class which uses
     * {@link Campaign#getFaction()} to select the faction.
     */
    public DefaultFactionSelector() {

    }

    /**
     * Creates a new DefaultFactionSelector using the specified faction.
     * @param factionCode The short name of the {@link Faction}.
     */
    @Deprecated // Replaced with it being based on Faction
    public DefaultFactionSelector(final @Nullable String factionCode) {
        this((factionCode == null) ? null : Factions.getInstance().getFaction(factionCode));
    }

    /**
     * Creates a new DefaultFactionSelector using the specified faction
     * @param faction The {@link Faction}.
     */
    public DefaultFactionSelector(final @Nullable Faction faction) {
        setFaction(faction);
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable Faction getFaction() {
        return faction;
    }

    public void setFaction(final @Nullable Faction faction) {
        this.faction = faction;
    }
    //endregion Getters/Setters

    @Override
    public Faction selectFaction(final Campaign campaign) {
        return (getFaction() == null) ? campaign.getFaction() : getFaction();
    }
}
