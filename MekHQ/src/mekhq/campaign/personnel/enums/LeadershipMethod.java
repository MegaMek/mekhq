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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum LeadershipMethod {
    REGULAR("LeadershipMethod.REGULAR.text", "LeadershipMethod.REGULAR.toolTipText"),
    GREEN("LeadershipMethod.GREEN.text", "LeadershipMethod.GREEN.toolTipText"),
    ELITE("LeadershipMethod.ELITE.text", "LeadershipMethod.ELITE.toolTipText"),
    FAMILY("LeadershipMethod.FAMILY.text", "LeadershipMethod.FAMILY.toolTipText"),
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

    public boolean isRegular() {
        return this == REGULAR;
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
