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

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Enumeration of the distinct life stages in A Time of War character generation.
 *
 * <p>Each stage has an associated order (for sequence and sorting) and a lookup name (for parsing and display
 * purposes).</p>
 *
 * <p>Use the {@link #fromLookupName(String)}, {@link #fromOrder(int)}, and {@link #fromString(String)} methods to
 * retrieve enum values based on textual or numeric input.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum ATOWLifeStage {
    AFFILIATION(0, "AFFILIATION"),
    CLAN_CASTE(1, "CLAN_CASTE"),
    SUPPLEMENTAL(2, "SUPPLEMENTAL"),
    EARLY_CHILDHOOD(3, "EARLY_CHILDHOOD"),
    LATE_CHILDHOOD(4, "LATE_CHILDHOOD"),
    HIGHER_EDUCATION(5, "HIGHER_EDUCATION"),
    REAL_LIFE(6, "REAL_LIFE");

    final static String RESOURCE_BUNDLE = "mekhq.resources.ATOWLifeStage";

    private static final MMLogger LOGGER = MMLogger.create(ATOWLifeStage.class);

    private final int order;
    private final String lookupName;

    /**
     * Constructs an {@link ATOWLifeStage} enum constant.
     *
     * @param order      the order of this life stage in the life path sequence
     * @param lookupName a string used for programmatic lookup and parsing
     *
     * @author Illiani
     * @since 0.50.07
     */
    ATOWLifeStage(int order, String lookupName) {
        this.order = order;
        this.lookupName = lookupName;
    }

    /**
     * Gets the stage's order for sequencing or sorting purposes.
     *
     * @return the defined order for this stage
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gets the lookup name for use in parsing, serialization, or display.
     *
     * @return the lookup string for this stage
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
     * Looks up a {@link ATOWLifeStage} by its lookup name, ignoring case.
     *
     * @param lookup the lookup name to search for (case-insensitive)
     *
     * @return the matching ATOWLifeStage, or {@code null} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable ATOWLifeStage fromLookupName(String lookup) {
        if (lookup == null) {
            LOGGER.warn("Null lookup name passed to ATOWLifeStage#fromLookupName");
            return null;
        }

        for (ATOWLifeStage stage : values()) {
            if (stage.lookupName.equalsIgnoreCase(lookup)) {
                return stage;
            }
        }

        LOGGER.warn("Unknown lookup name: {}", lookup);
        return null;
    }

    /**
     * Looks up a {@link ATOWLifeStage} by its order index.
     *
     * @param order the integer order of the desired stage
     *
     * @return the matching {@link ATOWLifeStage}, or {@code null} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable ATOWLifeStage fromOrder(int order) {
        for (ATOWLifeStage stage : values()) {
            if (stage.order == order) {
                return stage;
            }
        }

        LOGGER.warn("Unknown order: {}", order);
        return null;
    }

    /**
     * Attempts to look up a life stage from text input, first matching by lookup name, then (if not found) by parsing
     * the provided text as an integer order.
     *
     * @param text the input text, which may be a lookup name or an integer order
     *
     * @return the matching {@link ATOWLifeStage}, or {@code null} if no match is found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable ATOWLifeStage fromString(String text) {
        if (text == null) {
            LOGGER.warn("Null text passed to ATOWLifeStage#fromString");
            return null;
        }

        ATOWLifeStage stage = fromLookupName(text);
        if (stage != null) {
            return stage;
        }

        stage = fromOrder(MathUtility.parseInt(text, -1));
        if (stage != null) {
            return stage;
        }

        LOGGER.warn("Unknown ATOWLifeStage: {}", text);
        return null;
    }

    @Override
    public String toString() {
        return getLookupName();
    }
}
