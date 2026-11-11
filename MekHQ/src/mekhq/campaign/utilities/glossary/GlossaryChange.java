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
package mekhq.campaign.utilities.glossary;

import megamek.Version;
import mekhq.MHQConstants;

/**
 * How recently a glossary or documentation entry was added or updated, measured against the running build and the last
 * core release.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum GlossaryChange {
    /** Added or updated in the build the player is running. */
    THIS_BUILD,
    /** Added or updated after the last core release, but before this build. */
    SINCE_LAST_CORE,
    /** Unchanged since the last core release. */
    UNCHANGED;

    /**
     * @return {@code true} if the entry changed since the last core release, in this build or an earlier one
     */
    public boolean isChanged() {
        return this != UNCHANGED;
    }

    /**
     * Classifies an entry against the running build and the last core release.
     *
     * @param entryVersion the version the entry was added or last updated in
     *
     * @return how recently the entry changed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static GlossaryChange of(Version entryVersion) {
        return of(entryVersion, MHQConstants.VERSION, MHQConstants.LAST_MILESTONE);
    }

    /**
     * Classifies an entry against the given build and core release.
     *
     * @param entryVersion   the version the entry was added or last updated in
     * @param currentVersion the build the player is running
     * @param lastCore       the last core release
     *
     * @return how recently the entry changed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static GlossaryChange of(Version entryVersion, Version currentVersion, Version lastCore) {
        // An entry tagged with the running build, or a later one, counts as this build's.
        if (!entryVersion.isLowerThan(currentVersion)) {
            return THIS_BUILD;
        }
        return entryVersion.isHigherThan(lastCore) ? SINCE_LAST_CORE : UNCHANGED;
    }
}
