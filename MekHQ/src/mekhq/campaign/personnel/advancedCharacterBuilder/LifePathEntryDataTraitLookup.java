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

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Enumerates the supported trait types for {@link LifePath} objects, providing lookup functionality by name for use in
 * the advanced character builder.
 *
 * <p>Each trait is identified by a unique string, which is used for matching and data serialization.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum LifePathEntryDataTraitLookup {
    BLOODMARK("BLOODMARK"),
    CONNECTIONS("CONNECTIONS"),
    DEPENDENTS("DEPENDENTS"), // character creation only
    ENEMY("ENEMY"),
    EQUIPPED("EQUIPPED"),
    EXTRA_INCOME("EXTRA_INCOME"),
    LOST_LIMB("LOST_LIMB"), // character creation only
    OWNED_VEHICLE("OWNED_VEHICLE"), // character creation only
    PROPERTY("PROPERTY"),
    REPUTATION("REPUTATION"),
    TITLE("TITLE"),
    UNLUCKY("UNLUCKY"),
    WEALTH("WEALTH");

    private final static String RESOURCE_BUNDLE = "mekhq.resources.LifePathEntryDataTraitLookup";
    private static final MMLogger LOGGER = MMLogger.create(LifePathEntryDataTraitLookup.class);

    public static final int MINIMUM_DEPENDENTS = -Integer.MAX_VALUE;
    public static final int MAXIMUM_DEPENDENTS = 0;
    public static final int MINIMUM_EQUIPPED = -1;
    public static final int MAXIMUM_EQUIPPED = 8;
    public static final int MINIMUM_LOST_LIMB = -5;
    public static final int MAXIMUM_LOST_LIMB = 0;
    public static final int MINIMUM_OWNED_VEHICLE = 0;
    public static final int MAXIMUM_OWNED_VEHICLE = 12;

    private final String lookupName;

    /**
     * Constructs a {@link LifePathEntryDataTraitLookup} enumerated value with the provided name.
     *
     * @param lookupName the unique string identifier for this trait
     *
     * @author Illiani
     * @since 0.50.07
     */
    LifePathEntryDataTraitLookup(String lookupName) {
        this.lookupName = lookupName;
    }

    /**
     * Returns the string identifier of this trait, used for lookup purposes.
     *
     * @return the lookup name
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLookupName() {
        return lookupName;
    }

    /**
     * Returns the display name for this object by looking up the ".label" key in the resource bundle associated with
     * this class.
     *
     * @return the localized display name for this object
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getDisplayName() {
        return getTextAt(RESOURCE_BUNDLE, lookupName + ".label");
    }

    /**
     * Returns the description for this object by looking up the ".description" key in the resource bundle associated
     * with this class.
     *
     * @return the localized description for this object
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getDescription() {
        return getTextAt(RESOURCE_BUNDLE, lookupName + ".description");
    }

    /**
     * Resolves a {@link LifePathEntryDataTraitLookup} from a lookup string, performing a case-insensitive match.
     *
     * @param lookup the string lookup key (case-insensitive)
     *
     * @return the matching {@link LifePathEntryDataTraitLookup}, or {@code null} if not found or if input is
     *       {@code null}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable LifePathEntryDataTraitLookup fromLookupName(String lookup) {
        if (lookup == null) {
            LOGGER.warn("Null lookup passed to LifePathEntryDataTraitLookup#fromLookupName");
            return null;
        }

        for (LifePathEntryDataTraitLookup type : values()) {
            if (type.lookupName.equalsIgnoreCase(lookup)) {
                return type;
            }
        }

        LOGGER.warn("Unknown lookup name: {}", lookup);
        return null;
    }

    @Override
    public String toString() {
        return getLookupName();
    }
}
