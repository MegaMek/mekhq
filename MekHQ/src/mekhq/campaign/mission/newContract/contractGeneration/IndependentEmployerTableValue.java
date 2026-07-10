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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import megamek.logging.MMLogger;
import org.jspecify.annotations.NonNull;

/**
 * The sub-classification of an {@link GlobalEmployerTableValue#INDEPENDENT} employer, identifying what kind of
 * independent party is offering the contract &mdash; from a landed {@link #NOBLE} up to a {@link #CORPORATION}. Each
 * entry owns a contiguous band of the employer-determination roll, and the bands tile the entire integer range.
 */
public enum IndependentEmployerTableValue {
    /** A landed noble or house lord hiring on their personal authority. */
    NOBLE("NOBLE", Integer.MIN_VALUE, 3),
    /** The ruling government of a single world, acting on its own behalf. */
    PLANETARY_GOVERNMENT("PLANETARY_GOVERNMENT", 4, 5),
    /** Another mercenary command subcontracting the work. */
    MERCENARY("MERCENARY", 6, 6),
    /** A major Periphery state beyond the borders of the Inner Sphere. */
    MAJOR_PERIPHERY("MAJOR_PERIPHERY", 7, 8),
    /** A minor Periphery realm or independent world on the fringe of known space. */
    MINOR_PERIPHERY("MINOR_PERIPHERY", 9, 10),
    /** A megacorporation or interstellar business concern. */
    CORPORATION("CORPORATION", 11, Integer.MAX_VALUE);

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final int lowerBand;
    private final int upperBand;

    private final static MMLogger LOGGER = MMLogger.create(IndependentEmployerTableValue.class);
    private final String RESOURCE_BUNDLE = "mekhq.resources.IndependentEmployerTableValue";

    IndependentEmployerTableValue(final String lookupName, final int lowerBand, final int upperBand) {
        this.lookupName = lookupName;
        this.label = generateName(lookupName);
        this.tooltip = generateTooltip(lookupName);
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
    }

    private @NonNull String generateTooltip(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "IndependentEmployerTableValue." + lookupName + ".tooltip");
    }

    private @NonNull String generateName(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "IndependentEmployerTableValue." + lookupName + ".name");
    }

    /**
     * @return the localized tooltip describing this independent-employer type
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * @return the localized, human-readable display label for this independent-employer type
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the inclusive lower bound of this type's roll band
     */
    public int getLowerBand() {
        return lowerBand;
    }

    /**
     * @return the inclusive upper bound of this type's roll band
     */
    public int getUpperBand() {
        return upperBand;
    }

    /**
     * Determines whether the given roll falls within this type's band.
     *
     * @param value the roll to test
     *
     * @return {@code true} if the roll is within this type's inclusive band
     */
    public boolean isWithinRange(int value) {
        return value >= lowerBand && value <= upperBand;
    }

    /**
     * Maps a roll to the independent-employer type whose band contains it. Because the bands tile the entire integer
     * range, every roll resolves to a type; {@link #PLANETARY_GOVERNMENT} is returned as a defensive fallback.
     *
     * @param roll the employer-determination roll
     *
     * @return the matching independent-employer type
     */
    public static IndependentEmployerTableValue getEmployerForRoll(int roll) {
        for (IndependentEmployerTableValue employer : values()) {
            if (employer.isWithinRange(roll)) {
                return employer;
            }
        }
        LOGGER.warn("Roll {} is outside of any employer range. Returning PLANETARY_GOVERNMENT", roll);

        return PLANETARY_GOVERNMENT;
    }

    /**
     * Resolves an {@link IndependentEmployerTableValue} from a string. The text is matched, in order, against the enum
     * name (case-insensitive, spaces treated as underscores), the internal lookup name, and finally the ordinal index.
     *
     * @param text the text to parse
     *
     * @return the matching independent-employer type, or {@link #PLANETARY_GOVERNMENT} if none matches
     */
    public static IndependentEmployerTableValue fromString(String text) {
        try {
            return IndependentEmployerTableValue.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        for (IndependentEmployerTableValue value : values()) {
            if (value.lookupName.equals(text)) {
                return value;
            }
        }

        try {
            return IndependentEmployerTableValue.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        LOGGER.error("Unknown IndependentEmployerTableValue ordinal: {} - returning {}.",
              text,
              PLANETARY_GOVERNMENT.lookupName);

        return PLANETARY_GOVERNMENT;
    }

    /**
     * @return the localized display label, so the enum renders sensibly in UI components
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
