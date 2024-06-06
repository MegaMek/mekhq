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

public enum TurnoverFrequency {
    NEVER("TurnoverFrequency.NEVER.text", "TurnoverFrequency.NEVER.toolTipText"),
    WEEKLY("TurnoverFrequency.WEEKLY.text", "TurnoverFrequency.WEEKLY.toolTipText"),
    MONTHLY("TurnoverFrequency.MONTHLY.text", "TurnoverFrequency.MONTHLY.toolTipText"),
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

    public boolean isAnnually() {
        return this == ANNUALLY;
    }

    @Override
    public String toString() {
        return name;
    }
}
