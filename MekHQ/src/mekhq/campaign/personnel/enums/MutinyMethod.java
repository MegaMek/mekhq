/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;

public enum MutinyMethod {
    CAMPAIGN_OPERATIONS("MutinyMethod.CAMPAIGN_OPERATIONS.text", "MutinyMethod.CAMPAIGN_OPERATIONS.toolTipText"),
    ADVANCED_MUTINIES("MutinyMethod.ADVANCED_MUTINIES.text", "MutinyMethod.ADVANCED_MUTINIES.toolTipText");

    private final String name;
    private final String toolTipText;

    MutinyMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isCampaignOperations() {
        return this == CAMPAIGN_OPERATIONS;
    }

    public boolean isAdvancedMutinies() {
        return this == ADVANCED_MUTINIES;
    }

    @Override
    public String toString() {
        return name;
    }
}
