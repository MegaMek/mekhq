/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
