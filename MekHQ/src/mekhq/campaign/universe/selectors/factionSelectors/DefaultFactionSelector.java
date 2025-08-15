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
package mekhq.campaign.universe.selectors.factionSelectors;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;

/**
 * Selects a {@link Faction} object.
 */
public class DefaultFactionSelector extends AbstractFactionSelector {
    //region Variable Declarations
    private Faction faction;
    //endregion Variable Declarations

    //region Constructors

    /**
     * Creates a new DefaultFactionSelector class which uses {@link Campaign#getFaction()} to select the faction.
     *
     * @param options the {@link RandomOriginOptions} to use in faction selection
     */
    public DefaultFactionSelector(final RandomOriginOptions options) {
        super(options);
    }

    /**
     * Creates a new DefaultFactionSelector using the specified faction
     *
     * @param options the {@link RandomOriginOptions} to use in faction selection
     * @param faction The {@link Faction}.
     */
    public DefaultFactionSelector(final RandomOriginOptions options, final @Nullable Faction faction) {
        super(options);
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
    public @Nullable Faction selectFaction(final Campaign campaign) {
        return (getFaction() == null) ? campaign.getFaction() : getFaction();
    }
}
