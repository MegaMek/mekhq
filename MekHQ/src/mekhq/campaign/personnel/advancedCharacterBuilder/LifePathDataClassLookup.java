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

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Enumerates the valid class lookup types for {@link LifePathEntryData} in the advanced character builder.
 *
 * <p>Each entry represents a category or type for life path data classification (e.g., trait, skill, faction, etc.),
 * and the enum provides methods for retrieving types by name.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum LifePathDataClassLookup {
    ATOW_TRAIT("ATOW_TRAIT"),
    FACTION_CODE("FACTION_CODE"),
    LIFE_PATH("LIFE_PATH"),
    LIFE_PATH_CATEGORY("LIFE_PATH_CATEGORY"),
    SKILL("SKILL"),
    SKILL_ATTRIBUTE("SKILL_ATTRIBUTE"),
    SPA("SPA");

    private static final MMLogger LOGGER = MMLogger.create(LifePathDataClassLookup.class);

    private final String lookupName;

    /**
     * Constructs a {@link LifePathDataClassLookup} element with the given string lookup name.
     *
     * @param lookupName the unique lookup name for this data class
     *
     * @author Illiani
     * @since 0.50.07
     */
    LifePathDataClassLookup(String lookupName) {
        this.lookupName = lookupName;
    }

    /**
     * Gets the unique string identifier for this class lookup type.
     *
     * @return the lookup name for this class type
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * Resolves a {@link LifePathDataClassLookup} by its lookup name, case-insensitively.
     *
     * @param lookup the lookup name to search for
     *
     * @return the matching {@link LifePathDataClassLookup}, or {@code null} if none match or {@code lookup} is
     *       {@code null}.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable LifePathDataClassLookup fromLookupName(String lookup) {
        if (lookup == null) {
            LOGGER.warn("Null lookup passed to LifePathDataClassLookup#fromLookupName");
            return null;
        }
        for (LifePathDataClassLookup type : values()) {
            if (type.lookupName.equalsIgnoreCase(lookup)) {
                return type;
            }
        }

        LOGGER.warn("Unknown lookup name: {}", lookup);
        return null;
    }
}
