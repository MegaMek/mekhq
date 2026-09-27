/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How capable a cast member with no stats is, used when they take part in an opposed check. Each rating stands for a
 * target number on 2d6.
 */
public enum NpcRating {
    ULTRA_GREEN("ULTRA_GREEN", 10),
    GREEN("GREEN", 9),
    REGULAR("REGULAR", 8),
    VETERAN("VETERAN", 7),
    ELITE("ELITE", 6),
    HEROIC("HEROIC", 5),
    LEGENDARY("LEGENDARY", 4);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String label;
    private final int targetNumber;

    NpcRating(String lookupName, int targetNumber) {
        this.label = getTextAt(RESOURCE_BUNDLE, "NpcRating." + lookupName + ".label");
        this.targetNumber = targetNumber;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @return the target number a character of this rating rolls against, before modifiers
     */
    public int getTargetNumber() {
        return targetNumber;
    }

    @Override
    public String toString() {
        return label + " · " + targetNumber + "+";
    }
}
