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
     *
     * @param campaign The {@link Campaign} within which this {@link Faction} exists.
     *
     * @return A {@link Faction} selected for {@code campaign}.
     */
    public abstract @Nullable Faction selectFaction(Campaign campaign);

    /**
     * Clears any cache associated with faction selection.
     */
    public void clearCache() {

    }
}
