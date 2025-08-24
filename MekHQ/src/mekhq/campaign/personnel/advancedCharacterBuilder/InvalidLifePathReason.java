/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.utilities.MHQInternationalization.getTextAt;

public enum InvalidLifePathReason {
    MISSING_CATEGORIES("MISSING_CATEGORIES"),
    MISSING_FACTION("MISSING_FACTION"),
    MISSING_LIFE_STAGE("MISSING_LIFE_STAGE"),
    MISSING_NAME("MISSING_NAME"),
    MISSING_SOURCE("MISSING_SOURCE"),
    NO_FLEXIBLE_PICKS("NO_FLEXIBLE_PICKS"),
    MIN_YEAR_ABOVE_MAX_YEAR("MIN_YEAR_ABOVE_MAX_YEAR"),
    TOO_MANY_FLEXIBLE_PICKS("TOO_MANY_FLEXIBLE_PICKS");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.InvalidLifePathReason";

    private final String lookupName;

    InvalidLifePathReason(String lookupName) {
        this.lookupName = lookupName;
    }

    public String getLookupName() {
        return lookupName;
    }

    public String getDisplayName() {
        return getTextAt(RESOURCE_BUNDLE, "InvalidLifePathReason." + lookupName + ".displayName");
    }

    public String getDescription() {
        return getTextAt(RESOURCE_BUNDLE, "InvalidLifePathReason." + lookupName + ".description");
    }
}
