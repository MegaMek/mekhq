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
package mekhq.campaign.mission.mission.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import org.jspecify.annotations.NonNull;

public enum GlobalEmployerTableValue {
    INDEPENDENT("INDEPENDENT", Integer.MIN_VALUE, 5),
    MINOR_POWER("MINOR_POWER", 6, 7),
    MAJOR_POWER("MAJOR_POWER", 8, 10),
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

    public String getTooltip() {
        return tooltip;
    }

    public String getLabel() {
        return label;
    }

    public int getLowerBand() {
        return lowerBand;
    }

    public int getUpperBand() {
        return upperBand;
    }

    public boolean isWithinRange(int value) {
        return value >= lowerBand && value <= upperBand;
    }

    public static GlobalEmployerTableValue getEmployerForRoll(int roll) {
        for (GlobalEmployerTableValue employer : values()) {
            if (employer.isWithinRange(roll)) {
                return employer;
            }
        }
        LOGGER.warn("Roll {} is outside of any employer range. Returning MAJOR_POWER", roll);

        return MAJOR_POWER;
    }

    public @Nullable GlobalEmployerTableValue getNextLowestEmployerType() {
        return switch (this) {
            case INDEPENDENT -> null;
            case MINOR_POWER -> INDEPENDENT;
            case MAJOR_POWER -> MINOR_POWER;
            case SUPER_POWER -> MAJOR_POWER;
        };
    }

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

    @Override
    public String toString() {
        return getLabel();
    }
}
