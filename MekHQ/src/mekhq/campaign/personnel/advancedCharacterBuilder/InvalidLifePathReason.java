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

/**
 * Why a Life Path cannot be used.
 *
 * <p>Each value names a mistake an author can make that the record's own constructor does not reject, and carries
 * the display name and description shown to the author. {@link LifePathValidator} produces them.</p>
 *
 * @since 0.50.11
 */
public enum InvalidLifePathReason {
    MIN_YEAR_ABOVE_MAX_YEAR("MIN_YEAR_ABOVE_MAX_YEAR"),
    MISSING_CATEGORIES("MISSING_CATEGORIES"),
    MISSING_FACTION("MISSING_FACTION"),
    MISSING_LIFE_STAGE("MISSING_LIFE_STAGE"),
    MISSING_NAME("MISSING_NAME"),
    MISSING_SOURCE("MISSING_SOURCE"),
    NO_FLEXIBLE_PICKS("NO_FLEXIBLE_PICKS"),
    MATCHING_UUID_REQUIREMENT("MATCHING_UUID_REQUIREMENT"),
    MATCHING_UUID_EXCLUSION("MATCHING_UUID_EXCLUSION"),
    CATEGORY_NONE_NOT_ALONE("CATEGORY_NONE_NOT_ALONE"),
    CONTRADICTORY_REQUIREMENT_EXCLUSION("CONTRADICTORY_REQUIREMENT_EXCLUSION"),
    TOO_MANY_FLEXIBLE_PICKS("TOO_MANY_FLEXIBLE_PICKS");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.InvalidLifePathReason";

    private final String lookupName;

    InvalidLifePathReason(String lookupName) {
        this.lookupName = lookupName;
    }

    /**
     * Returns the identifier used to look this reason's strings up.
     *
     * @return the lookup name
     *
     * @since 0.50.11
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * Returns the short name shown to the author.
     *
     * @return the display name
     *
     * @since 0.50.11
     */
    public String getDisplayName() {
        return getTextAt(RESOURCE_BUNDLE, "InvalidLifePathReason." + lookupName + ".displayName");
    }

    /**
     * Returns the sentence explaining what the author needs to change.
     *
     * @return the description
     *
     * @since 0.50.11
     */
    public String getDescription() {
        return getTextAt(RESOURCE_BUNDLE, "InvalidLifePathReason." + lookupName + ".description");
    }
}
