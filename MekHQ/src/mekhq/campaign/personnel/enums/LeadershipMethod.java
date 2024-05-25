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

public enum LeadershipMethod {
    MERCENARY("LeadershipMethod.MERCENARY.text", "LeadershipMethod.MERCENARY.toolTipText"),
    FAMILY("LeadershipMethod.FAMILY.text", "LeadershipMethod.FAMILY.toolTipText"),
    ELITE("LeadershipMethod.ELITE.text", "LeadershipMethod.ELITE.toolTipText"),
    GREEN("LeadershipMethod.GREEN.text", "LeadershipMethod.GREEN.toolTipText"),
    IRON_FIST("LeadershipMethod.IRON_FIST.text", "LeadershipMethod.IRON_FIST.toolTipText");

    private final String name;
    private final String toolTipText;

    LeadershipMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public boolean isMercenary() {
        return this == MERCENARY;
    }

    public boolean isElite() {
        return this == ELITE;
    }

    public boolean isFamily() {
        return this == FAMILY;
    }

    public boolean isIronFist() {
        return this == IRON_FIST;
    }

    @Override
    public String toString() {
        return name;
    }
}
