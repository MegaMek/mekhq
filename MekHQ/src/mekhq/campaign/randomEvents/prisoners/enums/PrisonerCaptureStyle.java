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
package mekhq.campaign.randomEvents.prisoners.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public enum PrisonerCaptureStyle {
    //region Enum Declarations
    NONE, CAMPAIGN_OPERATIONS, MEKHQ;
    //endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources.PrisonerCaptureStyle";

    //region Getters
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    public String getTooltip() {
        final String RESOURCE_KEY = name() + ".tooltip";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isCampaignOperations() {
        return this == CAMPAIGN_OPERATIONS;
    }

    public boolean isMekHQ() {
        return this == MEKHQ;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return getLabel();
    }
}
