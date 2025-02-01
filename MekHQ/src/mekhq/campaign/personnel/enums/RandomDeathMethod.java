/*
 * Copyright (c) 2020-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.death.AbstractDeath;
import mekhq.campaign.personnel.death.DisabledRandomDeath;
import mekhq.campaign.personnel.death.RandomDeath;

import java.util.ResourceBundle;

public enum RandomDeathMethod {
    //region Enum Declarations
    NONE("RandomDeathMethod.NONE.text", "RandomDeathMethod.NONE.toolTipText"),
    RANDOM("RandomDeathMethod.RANDOM.text", "RandomDeathMethod.RANDOM.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    RandomDeathMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isRandom() {
        return this == RANDOM;
    }
    //endregion Boolean Comparison Methods

    public AbstractDeath getMethod(final CampaignOptions options) {
        return getMethod(options, true);
    }

    public AbstractDeath getMethod(final CampaignOptions options, final boolean initializeCauses) {
        return switch (this) {
            case RANDOM -> new RandomDeath(options, initializeCauses);
            case NONE -> new DisabledRandomDeath(options, initializeCauses);
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
