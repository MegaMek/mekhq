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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How work times (repairs, refits, reloads) are shown to the player. The choice is a client display option, so each
 * player can pick their own without changing the campaign.
 */
public enum WorkTimeDisplay {
    /** The largest unit of time and the one below it, for example {@code 3 months 5 days} or {@code 2 h 30 min}. */
    AUTOMATIC,
    /** Every unit of time that applies, for example {@code 1 year 2 months 5 days 3 h 10 min}. */
    FULL,
    /** Minutes of work only, for example {@code 28,800 min}. */
    MINUTES;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    /**
     * @return the tooltip that explains this choice
     */
    public String getToolTipText() {
        return getTextAt(RESOURCE_BUNDLE, "WorkTimeDisplay." + name() + ".toolTipText");
    }

    @Override
    public String toString() {
        return getTextAt(RESOURCE_BUNDLE, "WorkTimeDisplay." + name() + ".text");
    }
}
