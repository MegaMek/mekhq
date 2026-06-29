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

import megamek.logging.MMLogger;
import mekhq.campaign.randomEvents.personalities.Aggression;
import org.jspecify.annotations.NonNull;

public enum IndependentEmployerTableValue {
    NOBLE("NOBLE", Integer.MIN_VALUE, 3),
    PLANETARY_GOVERNMENT("PLANETARY_GOVERNMENT", 4, 5),
    MERCENARY("MERCENARY", 6, 6),
    MAJOR_PERIPHERY("MAJOR_PERIPHERY", 7, 8),
    MINOR_PERIPHERY("MINOR_PERIPHERY", 9, 10),
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

    public static IndependentEmployerTableValue getEmployerForRoll(int roll) {
        for (IndependentEmployerTableValue employer : values()) {
            if (employer.isWithinRange(roll)) {
                return employer;
            }
        }
        LOGGER.warn("Roll {} is outside of any employer range. Returning PLANETARY_GOVERNMENT", roll);

        return PLANETARY_GOVERNMENT;
    }

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

        MMLogger logger = MMLogger.create(Aggression.class);
        logger.error("Unknown IndependentEmployerTableValue ordinal: {} - returning {}.",
              text,
              PLANETARY_GOVERNMENT.lookupName);

        return PLANETARY_GOVERNMENT;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
