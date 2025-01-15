/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.universe.Faction;

/**
 * Represents a class which selects {@link Faction} objects.
 */
public abstract class AbstractFactionSelector {
    //region Variable Declarations
    private RandomOriginOptions options;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractFactionSelector(final RandomOriginOptions options) {
        setOptionsDirect(options);
    }
    //endregion Constructors

    //region Getters/Setters
    public RandomOriginOptions getOptions() {
        return options;
    }

    public void setOptions(final RandomOriginOptions options) {
        setOptionsDirect(options);
        clearCache();
    }

    public void setOptionsDirect(final RandomOriginOptions options) {
        this.options = options;
    }
    //endregion Getters/Setters

    /**
     * Selects a {@link Faction} for a {@link Campaign}.
     * @param campaign The {@link Campaign} within which this {@link Faction} exists.
     * @return A {@link Faction} selected for {@code campaign}.
     */
    public abstract @Nullable Faction selectFaction(Campaign campaign);

    /**
     * Clears any cache associated with faction selection.
     */
    public void clearCache() {

    }
}
