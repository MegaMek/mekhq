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

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import org.jspecify.annotations.NonNull;

/**
 * The top-level classification of a contract employer by political scale, from {@link #INDEPENDENT} minor players up to
 * galactic {@link #SUPER_POWER}s. Each entry owns a contiguous band of the employer-determination roll (the bands tile
 * the entire integer range), and the tiers form an ordered ladder used to step down to a lesser employer when one
 * cannot be found.
 */
public enum GlobalEmployerTableValue {
    /** Independent states or factions with minimal influence on galactic affairs. */
    INDEPENDENT("INDEPENDENT", Integer.MIN_VALUE, 5),
    /** Periphery states or minor players on the galactic stage. */
    MINOR_POWER("MINOR_POWER", 6, 7),
    /** The great houses or factions with significant spheres of influence. */
    MAJOR_POWER("MAJOR_POWER", 8, 10),
    /** The Star League, FedCom Alliance, or similar superpowers. */
    SUPER_POWER("SUPER_POWER", 11, Integer.MAX_VALUE);

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final int lowerBand;
    private final int upperBand;

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GlobalEmployerTableValue";
    private static final MMLogger LOGGER = MMLogger.create(GlobalEmployerTableValue.class);

    GlobalEmployerTableValue(final String lookupName, final int lowerBand, final int upperBand) {
        this.lookupName = lookupName;
        this.label = generateLabel(lookupName);
        this.tooltip = generateTooltip(lookupName);
        this.lowerBand = lowerBand;
        this.upperBand = upperBand;
    }

    private @NonNull String generateTooltip(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "GlobalEmployerTableValue." + lookupName + ".tooltip");
    }

    private @NonNull String generateLabel(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "GlobalEmployerTableValue." + lookupName + ".name");
    }

    /**
     * @return the localized tooltip describing this employer tier
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * @return the localized, human-readable display label for this employer tier
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the inclusive lower bound of this tier's roll band
     */
    public int getLowerBand() {
        return lowerBand;
    }

    /**
     * @return the inclusive upper bound of this tier's roll band
     */
    public int getUpperBand() {
        return upperBand;
    }

    /**
     * Determines whether the given roll falls within this tier's band.
     *
     * @param value the roll to test
     *
     * @return {@code true} if the roll is within this tier's inclusive band
     */
    public boolean isWithinRange(int value) {
        return value >= lowerBand && value <= upperBand;
    }

    /**
     * Maps a roll to the employer tier whose band contains it. Because the tier bands tile the entire integer range,
     * every roll resolves to a tier; {@link #MAJOR_POWER} is returned as a defensive fallback.
     *
     * @param roll the employer-determination roll
     *
     * @return the matching employer tier
     */
    public static GlobalEmployerTableValue getEmployerForRoll(int roll) {
        for (GlobalEmployerTableValue employer : values()) {
            if (employer.isWithinRange(roll)) {
                return employer;
            }
        }
        LOGGER.warn("Roll {} is outside of any employer range. Returning MAJOR_POWER", roll);

        return MAJOR_POWER;
    }

    /**
     * Steps one rung down the employer ladder, used to retry employer generation with a lesser power when no faction
     * can be found at the current tier.
     *
     * @return the next-lowest employer tier, or {@code null} if this is already {@link #INDEPENDENT}
     */
    public @Nullable GlobalEmployerTableValue getNextLowestEmployerType() {
        return switch (this) {
            case INDEPENDENT -> null;
            case MINOR_POWER -> INDEPENDENT;
            case MAJOR_POWER -> MINOR_POWER;
            case SUPER_POWER -> MAJOR_POWER;
        };
    }

    /**
     * Classifies a faction into its employer tier based on its political power. Factions that match no power tier are
     * treated as {@link #INDEPENDENT} so nothing slips through.
     *
     * @param faction the faction to classify
     *
     * @return the employer tier corresponding to the faction's power
     */
    public static GlobalEmployerTableValue getFactionTableType(Faction faction) {
        if (faction.isMinorPower()) {
            return MINOR_POWER;
        } else if (faction.isMajorPower()) {
            return MAJOR_POWER;
        } else if (faction.isSuperPower()) {
            return SUPER_POWER;
        } else {
            // We use independent here as a catch net so nothing slips through
            return INDEPENDENT;
        }
    }

    /**
     * Resolves a {@link GlobalEmployerTableValue} from a string. The text is matched, in order, against the enum name
     * (case-insensitive, spaces treated as underscores), the internal lookup name, and finally the ordinal index.
     *
     * @param text the text to parse
     *
     * @return the matching employer tier, or {@link #MAJOR_POWER} if none matches
     */
    public static GlobalEmployerTableValue fromString(String text) {
        try {
            return GlobalEmployerTableValue.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        for (GlobalEmployerTableValue value : values()) {
            if (value.lookupName.equals(text)) {
                return value;
            }
        }

        try {
            return GlobalEmployerTableValue.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        LOGGER.error("Unknown GlobalEmployerTableValue ordinal: {} - returning {}.", text, MAJOR_POWER.lookupName);

        return MAJOR_POWER;
    }

    /**
     * @return the localized display label, so the enum renders sensibly in UI components
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
