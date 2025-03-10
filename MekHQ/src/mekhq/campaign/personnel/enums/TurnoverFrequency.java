/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

public enum TurnoverFrequency {
    NEVER("TurnoverFrequency.NEVER.text", "TurnoverFrequency.NEVER.toolTipText"),
    WEEKLY("TurnoverFrequency.WEEKLY.text", "TurnoverFrequency.WEEKLY.toolTipText"),
    MONTHLY("TurnoverFrequency.MONTHLY.text", "TurnoverFrequency.MONTHLY.toolTipText"),
    QUARTERLY("TurnoverFrequency.QUARTERLY.text", "TurnoverFrequency.QUARTERLY.toolTipText"),
    ANNUALLY("TurnoverFrequency.ANNUALLY.text", "TurnoverFrequency.ANNUALLY.toolTipText");

    private final String name;
    private final String toolTipText;

    TurnoverFrequency(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isNever() {
        return this == NEVER;
    }

    public boolean isWeekly() {
        return this == WEEKLY;
    }

    public boolean isMonthly() {
        return this == MONTHLY;
    }

    public boolean isQuarterly() {
        return this == QUARTERLY;
    }

    public boolean isAnnually() {
        return this == ANNUALLY;
    }

    @Override
    public String toString() {
        return name;
    }
}
